package com.bombermama.screenshot

import com.bombermama.core.Bomb
import com.bombermama.core.BonusType
import com.bombermama.core.Direction
import com.bombermama.core.EnemyType
import com.bombermama.core.GameEngine
import com.bombermama.core.GameState
import com.bombermama.core.Levels
import com.bombermama.core.Tile
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Генератор скриншотов.
 *
 * Использует НАСТОЯЩИЙ GameEngine из core-модуля: создаёт уровень,
 * прогоняет симуляцию и рендерит кадры через SceneRenderer.
 * Каждый кадр — реальное состояние игры, а не мокап.
 */
object ScreenshotMaker {

    private const val W = 1280
    private const val H = 720
    private const val SCALE = 4

    private val outDir = File("/workspace/wise-kalam/screenshots")
    private val sprites = SpriteFactory(SCALE)
    private val renderer = SceneRenderer(sprites)

    fun zones(): TouchZones {
        // Как layoutTouchZones() в GameSurfaceView.
        val dpadSize = min(W, H) * 0.42f
        val dpadLeft = max(W * 0.04f, 16f)
        val dpadBottom = H - max(H * 0.06f, 16f)
        val bombSize = min(W, H) * 0.26f
        val bombRight = W - max(W * 0.04f, 16f)
        return TouchZones(
            dpad = RectF(dpadLeft, dpadBottom - dpadSize, dpadLeft + dpadSize, dpadBottom),
            bomb = RectF(bombRight - bombSize, dpadBottom - bombSize, bombRight, dpadBottom)
        )
    }

    private fun render(engine: GameEngine, name: String, input: TouchZones = zones(), paused: Boolean = false) {
        val canvas = PixCanvas(W, H)
        renderer.onSizeChanged(W, H, engine.field.width, engine.field.height)
        renderer.draw(canvas, engine, input, paused)
        val file = File(outDir, name)
        file.writeBytes(PngEncoder.encode(canvas))
        println("SHOT ${file.name} ${file.length()} bytes")
    }

    // ===================== Сцены =====================

    fun shotLevelStart(level: Int) {
        val spec = Levels.byNumber(level) ?: return
        val engine = GameEngine(spec, spec.seed)
        render(engine, "level_${level}_start.png", zones().copy(direction = Direction.DOWN))
    }

    /** Бомба установлена и тикает перед героиней. */
    fun shotBombPlanted(level: Int) {
        val spec = Levels.byNumber(level) ?: return
        val engine = GameEngine(spec, spec.seed)
        // Подвинем героиню к открытой клетке и поставим бомбу.
        engine.bombRequested = true
        engine.update(0.5f) // бомба установлена и тикает
        render(engine, "level_${level}_bomb.png", zones().copy(direction = Direction.RIGHT))
    }

    /** Момент взрыва: бомба только что взорвалась, крест виден. */
    fun shotExplosion(level: Int) {
        val spec = Levels.byNumber(level) ?: return
        val engine = GameEngine(spec, spec.seed)
        engine.bombRequested = true
        // Прогоняем ровно до взрыва (фитиль 2.2с) и ловим первый кадр.
        var t = 0f
        while (t < Bomb.FUSE_TIME + 0.05f && engine.state == GameState.PLAYING) {
            engine.update(1 / 60f)
            t += 1 / 60f
        }
        engine.update(1 / 60f * 3) // крест взрыва чуть разрастётся
        render(engine, "level_${level}_explosion.png", zones().copy(direction = Direction.DOWN))
    }

    /** Цепная реакция из нескольких бомб. */
    fun shotChain() {
        val spec = Levels.byNumber(6) ?: return
        val engine = GameEngine(spec, spec.seed)
        val p = engine.player
        // Две бомбы в ряд: A взрывает B.
        engine.bombs.add(Bomb(p.tileX + 2, p.tileY, 3, fuse = 0.4f))
        engine.bombs.add(Bomb(p.tileX + 4, p.tileY, 3, fuse = 2.0f))
        var t = 0f
        while (t < 0.6f && engine.state == GameState.PLAYING) {
            engine.update(1 / 60f)
            t += 1 / 60f
        }
        render(engine, "chain_reaction.png", zones().copy(direction = Direction.RIGHT))
    }

    /** Бонусы на поле после разрушения блоков. */
    fun shotBonuses() {
        val spec = Levels.byNumber(3) ?: return
        val engine = GameEngine(spec, spec.seed)
        // Разрушим несколько блоков бомбой с большим радиусом.
        val p = engine.player
        engine.bombs.add(Bomb(p.tileX, p.tileY, 4, fuse = 0.3f))
        var t = 0f
        while (t < 0.5f && engine.state == GameState.PLAYING) {
            engine.update(1 / 60f)
            t += 1 / 60f
        }
        render(engine, "bonuses.png", zones().copy(direction = Direction.DOWN))
    }

    /** Враги разных типов на одном поле. */
    fun shotEnemies() {
        val spec = Levels.byNumber(11) ?: return
        val engine = GameEngine(spec, spec.seed)
        render(engine, "enemies.png", zones().copy(direction = Direction.UP))
    }

    /** Выход открыт — враги зачищены. */
    fun shotExitOpen() {
        val spec = Levels.byNumber(1) ?: return
        val engine = GameEngine(spec, spec.seed)
        for (e in engine.enemies.toList()) {
            engine.bombs.add(Bomb(e.tileX, e.tileY, 1, fuse = 0.1f))
            var t = 0f
            while (t < 0.35f) { engine.update(1 / 60f); t += 1 / 60f }
        }
        var t = 0f
        while (t < 1.2f && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
        // Если выход ещё не открыт, откроем принудительно (как сделал бы солвер).
        if (!engine.enemiesCleared) {
            engine.field.openExit()
        }
        render(engine, "exit_open.png", zones().copy(direction = Direction.DOWN))
    }

    /** Пауза: затемнённый кадр. */
    fun shotPaused() {
        val spec = Levels.byNumber(2) ?: return
        val engine = GameEngine(spec, spec.seed)
        engine.pause()
        render(engine, "paused.png", zones(), paused = true)
    }

    /** Победа: героиня в позе победы. */
    fun shotWin() {
        val spec = Levels.byNumber(5) ?: return
        val engine = GameEngine(spec, spec.seed)
        // Заставим движок перейти в WIN: зачищаем врагов и идём к выходу.
        for (e in engine.enemies.toList()) {
            engine.bombs.add(Bomb(e.tileX, e.tileY, 1, fuse = 0.1f))
            var t = 0f
            while (t < 0.35f) { engine.update(1 / 60f); t += 1 / 60f }
        }
        var t = 0f
        while (t < 1.5f && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
        val (ex, ey) = engine.field.exitTile!!
        engine.player.x = ex.toFloat()
        engine.player.y = ey.toFloat()
        engine.update(1 / 60f)
        if (engine.state != GameState.WIN) {
            engine.field.openExit()
            engine.update(1 / 60f)
        }
        render(engine, "win.png", zones().copy(direction = Direction.DOWN))
    }

    /** Поражение: героиня повержена. */
    fun shotGameOver() {
        val spec = Levels.byNumber(8) ?: return
        val engine = GameEngine(spec, spec.seed)
        // Бомба под героиней — урон до нуля.
        repeat(5) {
            engine.bombs.add(Bomb(engine.player.tileX, engine.player.tileY, 5, fuse = 0.1f))
            var t = 0f
            while (t < 0.4f && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
        }
        render(engine, "game_over.png", zones().copy(direction = Direction.DOWN))
    }

    fun all() {
        outDir.mkdirs()
        menu()
        shotLevelStart(1)
        shotBombPlanted(1)
        shotExplosion(2)
        shotChain()
        shotBonuses()
        shotEnemies()
        shotExitOpen()
        shotPaused()
        shotWin()
        shotGameOver()
    }

    /** Главное меню в фокусе на "НОВАЯ ИГРА". */
    fun menu() {
        val spec = Levels.byNumber(1) ?: return
        val engine = GameEngine(spec, spec.seed)
        val canvas = PixCanvas(W, H)
        renderer.onSizeChanged(W, H, engine.field.width, engine.field.height)
        renderer.drawMenu(canvas, selected = 0)
        val file = File(outDir, "menu.png")
        file.writeBytes(PngEncoder.encode(canvas))
        println("SHOT ${file.name} ${file.length()} bytes")
    }
}

fun main() {
    ScreenshotMaker.all()
    println("DONE")
}
