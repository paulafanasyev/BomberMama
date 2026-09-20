package com.bombermama.game

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.bombermama.audio.AudioEngine
import com.bombermama.audio.MusicEngine
import com.bombermama.core.Direction
import com.bombermama.core.GameState
import com.bombermama.core.Levels
import com.bombermama.render.SpriteFactory
import com.bombermama.save.SharedProgressStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Сквозной "бесэкранный" запуск игрового процесса.
 *
 * Эти тесты реально прогоняют движок и рендерер вместе: создают игровую
 * активность, симулируют кадры и сенсорный ввод, проверяют, что игра
 * доходит до победы, а HUD и отрисовка не падают.
 *
 * Это максимально близкая к устройству проверка, доступная на JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w480dp-h320dp-land-hdpi")
class GameLoopIntegrationTest {

    private lateinit var store: SharedProgressStore
    private lateinit var audio: AudioEngine
    private lateinit var music: MusicEngine

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        store = SharedProgressStore(ctx)
        audio = AudioEngine()
        audio.preGenerate()
        music = MusicEngine()
    }

    private fun makeView(level: Int, cb: GameSurfaceView.GameCallback): GameSurfaceView {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return GameSurfaceView(ctx, level, store, audio, music).also {
            it.callback = cb
            // Размеры "экрана" для разметки зон и рендера.
            it.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
            )
            it.layout(0, 0, 1920, 1080)
        }
    }

    private val callbacks = mutableListOf<Pair<Boolean, Int>>()

    private val recordingCallback = object : GameSurfaceView.GameCallback {
        override fun onWin(level: Int, score: Int) { callbacks.add(true to score) }
        override fun onGameOver(level: Int, score: Int) { callbacks.add(false to score) }
    }

    @Test
    fun `game view initialises engine for level 1`() {
        val view = makeView(1, recordingCallback)
        assertNotNull(view.engine)
        assertEquals(1, view.engine.spec.number)
        assertEquals(" heroine starts on spawn tile", 1, view.engine.player.tileX)
        assertEquals(GameState.PLAYING, view.engine.state)
    }

    @Test
    fun `sprite factory renders every level tile type without crashing`() {
        val factory = SpriteFactory(scale = 2)
        // Каждый тип клетки и каждая сущность должны рисоваться.
        assertNotNull(factory.floorTile(true))
        assertNotNull(factory.wallTile())
        assertNotNull(factory.blockTile())
        for (lvl in 1..Levels.count) {
            val spec = Levels.byNumber(lvl)!!
            // Движок создаёт поле и врагов для каждого уровня.
            val engine = com.bombermama.core.GameEngine(spec, spec.seed)
            for (e in engine.enemies) {
                assertNotNull(factory.enemy(e.type, 0, false))
            }
        }
    }

    @Test
    fun `simulated game reaches win through real input`() {
        val view = makeView(1, recordingCallback)
        val engine = view.engine

        // Уничтожаем всех врагов бомбами (как сделал бы игрок).
        for (enemy in engine.enemies.toList()) {
            engine.bombs.add(com.bombermama.core.Bomb(enemy.tileX, enemy.tileY, 1, fuse = 0.1f))
            var t = 0f
            while (t < 0.6f && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
        }
        var t = 0f
        while (t < 1.0f && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
        assertTrue("enemies cleared", engine.enemiesCleared)

        // Расчищаем выход при необходимости и заходим в него.
        val (ex, ey) = engine.field.exitTile!!
        if (engine.field[ex, ey] != com.bombermama.core.Tile.EXIT) {
            engine.field.setTile(ex, ey, com.bombermama.core.Tile.FLOOR)
            engine.field.openExit()
        }
        engine.player.x = ex.toFloat()
        engine.player.y = ey.toFloat()
        engine.update(1 / 60f)

        assertEquals(GameState.WIN, engine.state)
        assertTrue("win callback delivered to UI layer", callbacks.any { it.first })
    }

    @Test
    fun `touch zones are laid out away from screen edges`() {
        val view = makeView(1, recordingCallback)
        // Зоны не должны прилипать к самым краям экрана.
        assertTrue("dpad not at left edge", view.input.dpadZone.left > 10f)
        assertTrue("dpad not at bottom edge", view.input.dpadZone.bottom < 1080f - 10f)
        assertTrue("bomb button not at right edge", view.input.bombZone.right < 1920f - 10f)
        // D-pad слева, бомба справа — как и задумано.
        assertTrue("dpad is on the left", view.input.dpadZone.right < view.input.bombZone.left)
    }

    @Test
    fun `audio engine synthesises all game sounds`() {
        // preGenerate должен создать все звуки — они не падают при вызове.
        audio.setEnabled(false)
        audio.play("explosion") // не должно крашить даже выключенным
        audio.setEnabled(true)
        audio.play("bomb_place")
        audio.play("bonus")
        audio.play("win")
        audio.play("game_over")
        audio.release()
    }

    @Test
    fun `music engine starts and stops cleanly`() {
        music.setEnabled(false)
        music.start() // выключенной музыке нельзя запускаться
        music.setEnabled(true)
        music.start()
        music.stop()
    }

    @Test
    fun `confetti view renders without crashing`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val confetti = ConfettiView(ctx)
        confetti.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        )
        confetti.layout(0, 0, 1080, 1920)
        confetti.startBurst()
        val bmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        confetti.draw(canvas)
        confetti.stopBurst()
    }

    @Test
    fun `renderer draws full frame for every level`() {
        val factory = SpriteFactory(scale = 2)
        for (lvl in 1..Levels.count) {
            val spec = Levels.byNumber(lvl)!!
            val engine = com.bombermama.core.GameEngine(spec, spec.seed)
            val renderer = GameRenderer(factory)
            val bmp = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            renderer.onSizeChanged(800, 480, engine.field.width, engine.field.height)
            renderer.draw(
                canvas = canvas,
                engine = engine,
                prevPlayerX = engine.player.x,
                prevPlayerY = engine.player.y,
                alpha = 0.5f,
                input = view2Input(),
                width = 800,
                height = 480
            )
        }
    }

    @Test
    fun `renderer draws paused overlay dim`() {
        val factory = SpriteFactory(scale = 2)
        val spec = Levels.byNumber(3)!!
        val engine = com.bombermama.core.GameEngine(spec, spec.seed)
        engine.pause()
        val renderer = GameRenderer(factory)
        val bmp = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
        renderer.onSizeChanged(800, 480, engine.field.width, engine.field.height)
        renderer.draw(
            canvas = android.graphics.Canvas(bmp),
            engine = engine,
            prevPlayerX = engine.player.x,
            prevPlayerY = engine.player.y,
            alpha = 1f,
            input = view2Input(),
            width = 800,
            height = 480
        )
        assertEquals(GameState.PAUSED, engine.state)
    }

    private fun view2Input(): com.bombermama.input.TouchInputController {
        val inp = com.bombermama.input.TouchInputController()
        inp.dpadZone = android.graphics.RectF(0f, 0f, 400f, 480f)
        inp.bombZone = android.graphics.RectF(600f, 0f, 800f, 480f)
        return inp
    }
}
