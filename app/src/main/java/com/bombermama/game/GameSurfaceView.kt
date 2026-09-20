package com.bombermama.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bombermama.audio.AudioEngine
import com.bombermama.audio.MusicEngine
import com.bombermama.core.BonusPickup
import com.bombermama.core.Bomb
import com.bombermama.core.Direction
import com.bombermama.core.Enemy
import com.bombermama.core.GameEngine
import com.bombermama.core.GameState
import com.bombermama.core.Levels
import com.bombermama.core.Player
import com.bombermama.core.ProgressStore
import com.bombermama.core.Tile
import com.bombermama.input.TouchInputController
import com.bombermama.render.Palette
import com.bombermama.render.SpriteFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * Игровое поле: SurfaceView с собственным потоком рендера.
 *
 * Архитектура:
 *  - [GameEngine] (чистый Kotlin) — вся логика, тестируется на JVM;
 *  - [GameRenderer] — отрисовка спрайтов на Canvas;
 *  - [TouchInputController] — сенсорное управление;
 *  - [AudioEngine]/[MusicEngine] — звук.
 *
 * Поток использует фиксированный шаг симуляции с аккумулятором:
 * логика всегда просчитывается одинаковыми порциями (1/120 c),
 * поэтому скорость игры не зависит от FPS устройства.
 */
class GameSurfaceView(
    context: Context,
    private val levelNumber: Int,
    private val store: ProgressStore,
    private val audio: AudioEngine,
    private val music: MusicEngine
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    interface GameCallback {
        fun onWin(level: Int, score: Int)
        fun onGameOver(level: Int, score: Int)
    }

    /** Колбэк финиша уровня. Устанавливается активностью. */
    @Volatile
    var callback: GameCallback? = null

    /** Контроллер ввода (открыт для тестирования разметки зон). */
    val input: TouchInputController = TouchInputController()

    @Volatile
    private var running = false
    private var renderThread: Thread? = null
    private val surfaceReady = AtomicBoolean(false)

    val engine: GameEngine = GameEngine(
        spec = Levels.byNumber(levelNumber) ?: Levels.first,
        seed = (Levels.byNumber(levelNumber) ?: Levels.first).seed,
        onWin = { callback?.onWin(it.level, it.score) },
        onGameOver = { callback?.onGameOver(it.level, it.score) }
    )

    private val renderer = GameRenderer(SpriteFactory())

    // Фиксированный шаг симуляции.
    private val fixedDt = 1f / 120f
    private var accumulator = 0f
    private var lastTimeNanos = 0L

    // Зритель: плавный интерполяционный кадр.
    private var prevPlayerX = engine.player.x
    private var prevPlayerY = engine.player.y
    private var renderAlpha = 1f

    // Звук: тикание бомбы.
    private var lastTickFuse = 0f

    // Размеры экрана в пикселях.
    private var viewWidth = 0
    private var viewHeight = 0

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady.set(true)
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        renderer.onSizeChanged(width, height, engine.field.width, engine.field.height)
        layoutTouchZones()
    }

    /**
     * Зоны ввода пересчитываются при любой смене размеров View.
     * На устройстве сюда приходит и surfaceChanged, и onLayout —
     * оба пути должны давать корректную разметку.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!changed) return
        viewWidth = right - left
        viewHeight = bottom - top
        if (viewWidth > 0 && viewHeight > 0) {
            renderer.onSizeChanged(viewWidth, viewHeight, engine.field.width, engine.field.height)
            layoutTouchZones()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady.set(false)
        stopThread()
    }

    private fun startThread() {
        if (running) return
        running = true
        renderThread = Thread(this, "GameRenderThread").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun stopThread() {
        running = false
        try {
            renderThread?.join(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        renderThread = null
    }

    /**
     * Ставим игру на паузу: поток продолжает работать (чтобы рисовать
     * оверлей паузы), но симуляция останавливается — engine.update
     * не вызывается, поэтому таймеры, враги и бомбы заморожены.
     */
    fun pauseGame() {
        engine.pause()
        audio.stopAll()
    }

    fun resumeGame() {
        engine.resume()
        lastTimeNanos = System.nanoTime()
    }

    override fun run() {
        Looper.prepare()
        lastTimeNanos = System.nanoTime()
        while (running) {
            if (!surfaceReady.get()) {
                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    return
                }
                continue
            }
            val now = System.nanoTime()
            var frameNanos = (now - lastTimeNanos).toFloat() / 1_000_000f
            lastTimeNanos = now
            // Защита от "провала" после паузы/сворачивания.
            frameNanos = frameNanos.coerceIn(0f, 100f)
            val dt = min(frameNanos / 1000f, MAX_FRAME_SEC)

            // Тонкая логика/толстый рендер: логика — мелкими фиксированными шагами.
            accumulator += dt
            var steps = 0
            while (accumulator >= fixedDt && steps < MAX_STEPS) {
                step(fixedDt)
                accumulator -= fixedDt
                steps++
            }
            if (steps >= MAX_STEPS) {
                // Отстаём — сбрасываем аккумулятор, чтобы не было спирали смерти.
                accumulator = 0f
            }
            renderAlpha = (accumulator / fixedDt).coerceIn(0f, 1f)

            drawFrame()
        }
    }

    private var bombHoldTimer = 0f

    private fun step(dt: Float) {
        // Ввод → движок.
        engine.inputDirection = input.direction
        if (input.bombJustPressed) {
            input.consumeBombPress()
            engine.bombRequested = true
            bombHoldTimer = 0f
            audio.play("bomb_place")
        }
        // Удержание кнопки бомбы — повторная установка с задержкой.
        if (input.isBombHold()) {
            bombHoldTimer += dt
            if (bombHoldTimer >= BOMB_REPEAT_DELAY) {
                bombHoldTimer = 0f
                engine.bombRequested = true
                audio.play("bomb_place")
            }
        } else {
            bombHoldTimer = 0f
        }

        // Запоминаем позиции до шага для интерполяции рендера.
        prevPlayerX = engine.player.x
        prevPlayerY = engine.player.y

        engine.update(dt)

        // Звуковые события, которые движок "накопил".
        pumpAudioEvents()

        // Тикание бомбы.
        val activeBomb = engine.bombs.firstOrNull { it.isActive }
        if (activeBomb != null) {
            // Учащение тиков по мере сгорания фитиля.
            val total = Bomb.FUSE_TIME
            val ratio = (total - activeBomb.fuse) / total
            val period = (0.55f - ratio * 0.4f).coerceAtLeast(0.08f)
            if (activeBomb.age - lastTickFuse >= period || lastTickFuse == 0f) {
                lastTickFuse = activeBomb.age
                audio.playTick(0.85f + ratio * 1.3f)
            }
        } else {
            lastTickFuse = 0f
        }
    }

    /**
     * Звуковые реакции на события в движке.
     * Движок не знает про Android-аудио, поэтому здесь мы сравниваем
     * состояния "до/после" и воспроизводим нужные звуки.
     */
    private var prevBombCount = 0
    private var prevExplosionCount = 0
    private var prevEnemyCount = 0
    private var prevLives = 0
    private var prevBlockCount = 0
    private var prevBonusCount = 0
    private var prevState: GameState = GameState.PLAYING
    private var winSoundPlayed = false
    private var loseSoundPlayed = false

    private fun pumpAudioEvents() {
        // Появление нового взрыва.
        val explosions = engine.explosions.size
        if (explosions > prevExplosionCount) {
            audio.play("explosion")
        }
        prevExplosionCount = explosions

        // Уничтожен враг.
        val aliveEnemies = engine.enemies.count { !it.dead }
        if (aliveEnemies < prevEnemyCount) {
            audio.play("enemy_kill")
        }
        prevEnemyCount = aliveEnemies

        // Подобран бонус.
        if (engine.bonuses.size < prevBonusCount) {
            audio.play("bonus")
        }
        prevBonusCount = engine.bonuses.size

        // Получен урон.
        if (engine.player.lives < prevLives) {
            audio.play("hurt")
        }
        prevLives = engine.player.lives

        // Разрушен блок.
        var blocks = 0
        for (y in 0 until engine.field.height)
            for (x in 0 until engine.field.width)
                if (engine.field[x, y] == Tile.BLOCK) blocks++
        if (blocks < prevBlockCount) {
            audio.play("block_break")
        }
        prevBlockCount = blocks

        // Победа / поражение.
        when (engine.state) {
            GameState.WIN -> {
                if (!winSoundPlayed) {
                    winSoundPlayed = true
                    music.stop()
                    audio.play("win")
                    store.recordLevelScore(engine.spec.number, engine.player.score)
                    store.unlockLevel(engine.spec.number + 1)
                }
            }
            GameState.GAME_OVER -> {
                if (!loseSoundPlayed) {
                    loseSoundPlayed = true
                    music.stop()
                    audio.play("game_over")
                    store.recordLevelScore(engine.spec.number, engine.player.score)
                }
            }
            else -> {}
        }
        if (engine.state != prevState && engine.state == GameState.PAUSED) {
            audio.play("pause")
        }
        prevState = engine.state
        prevBombCount = engine.bombs.size
    }

    private fun drawFrame() {
        val c = holder.lockHardwareCanvas() ?: return
        try {
            renderer.draw(
                canvas = c,
                engine = engine,
                prevPlayerX = prevPlayerX,
                prevPlayerY = prevPlayerY,
                alpha = renderAlpha,
                input = input,
                width = viewWidth,
                height = viewHeight
            )
        } catch (e: Throwable) {
            Log.w(TAG, "draw error", e)
        } finally {
            try {
                holder.unlockCanvasAndPost(c)
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Разметка сенсорных зон: D-pad слева, кнопка бомбы справа.
     * Не у самых краёв экрана — чтобы пальцу было удобно.
     */
    private fun layoutTouchZones() {
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()
        val dpadSize = min(w, h) * 0.42f
        // D-pad в левой-нижней области.
        val dpadLeft = max(w * 0.04f, 16f)
        val dpadBottom = h - max(h * 0.06f, 16f)
        input.dpadZone = RectF(
            dpadLeft,
            dpadBottom - dpadSize,
            dpadLeft + dpadSize,
            dpadBottom
        )
        // Кнопка бомбы в правой-нижней области.
        val bombSize = min(w, h) * 0.26f
        val bombRight = w - max(w * 0.04f, 16f)
        input.bombZone = RectF(
            bombRight - bombSize,
            dpadBottom - bombSize,
            bombRight,
            dpadBottom
        )
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        return input.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun release() {
        stopThread()
        audio.stopAll()
    }

    companion object {
        private const val TAG = "GameSurfaceView"
        private const val MAX_STEPS = 8
        private const val MAX_FRAME_SEC = 0.25f
        private const val BOMB_REPEAT_DELAY = 0.45f
    }
}
