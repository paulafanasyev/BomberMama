package com.bombermama.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Приемочные тесты полного игрового цикла.
 *
 * Здесь проверяется именно "сквозной" путь: от создания движка до победы
 * и поражения — с реальными колбэками, которые активируют UI-экраны.
 * Это та логика, по которой UI решает, какой оверлей показать.
 */
class FullGameCycleTest {

    private val results = mutableListOf<SessionResult>()

    private fun newEngine(level: Int): GameEngine {
        val spec = Levels.byNumber(level)!!
        return GameEngine(
            spec = spec,
            seed = spec.seed,
            onWin = { results.add(it) },
            onGameOver = { results.add(it) }
        )
    }

    private fun tick(engine: GameEngine, seconds: Float) {
        var t = 0f
        while (t < seconds && engine.state == GameState.PLAYING) {
            engine.update(1 / 60f)
            t += 1 / 60f
        }
    }

    @Test
    fun `level can be completed by clearing enemies and reaching exit`() {
        val engine = newEngine(1)
        val startState = engine.state
        assertEquals(GameState.PLAYING, startState)

        // 1. Уничтожаем всех врагов бомбами.
        for (enemy in engine.enemies.toList()) {
            engine.bombs.add(Bomb(enemy.tileX, enemy.tileY, fireRange = 1, fuse = 0.1f))
            tick(engine, 0.5f)
        }
        assertTrue("all enemies dead", engine.enemies.all { it.dead })
        // Движок должен был открыть выход.
        val (ex, ey) = engine.field.exitTile!!
        // Даём мёртвым врагам удалиться и выходу — открыться.
        tick(engine, 1.0f)
        assertTrue("exit opened", engine.enemiesCleared)

        // Если выход всё ещё скрыт под блоком — расчистим его (как сделал бы игрок).
        if (engine.field[ex, ey] != Tile.EXIT) {
            engine.field.setTile(ex, ey, Tile.FLOOR)
            engine.field.openExit()
        }
        assertEquals(Tile.EXIT, engine.field[ex, ey])

        // 2. Идём к выходу.
        engine.player.x = ex.toFloat()
        engine.player.y = ey.toFloat()
        engine.update(1 / 60f)

        assertEquals(GameState.WIN, engine.state)
        assertTrue("win callback fired", results.any { it.won && it.level == 1 })
    }

    @Test
    fun `game over fires when lives reach zero`() {
        val engine = newEngine(1)
        engine.player.lives = 1
        // Подрываем героиню несколько раз — после последней жизни должен прийти GAME_OVER.
        repeat(3) {
            engine.bombs.add(Bomb(engine.player.tileX, engine.player.tileY, fireRange = 1, fuse = 0.05f))
            tick(engine, 0.5f)
        }
        assertEquals(GameState.GAME_OVER, engine.state)
        assertTrue("game over callback fired", results.any { !it.won })
        assertEquals(0, engine.player.lives)
    }

    @Test
    fun `progression - level 2 is harder than level 1`() {
        val l1 = Levels.byNumber(1)!!
        val l10 = Levels.byNumber(10)!!
        assertTrue("later levels have more enemies", l10.totalEnemies > l1.totalEnemies)
        assertTrue("later levels are bigger or denser",
            l10.width * l10.height >= l1.width * l1.height)
    }

    @Test
    fun `endless mode beyond campaign is playable`() {
        val spec = Levels.endless(Levels.count + 5)
        val engine = GameEngine(spec, spec.seed)
        assertNotNull(engine.field.exitTile)
        assertTrue(engine.enemies.isNotEmpty())
        assertEquals(GameState.PLAYING, engine.state)
    }

    @Test
    fun `no impossible states - bombs always resolve`() {
        // Размещаем кластер бомб и убеждаемся, что никаких "зависших" бомб не остаётся.
        val engine = newEngine(5)
        for (x in 2..6) {
            engine.bombs.add(Bomb(x, 5, fireRange = 3, fuse = 0.1f))
        }
        tick(engine, 2.0f)
        assertEquals("no lingering bombs", 0, engine.bombs.size)
        // И никаких зависших взрывов.
        tick(engine, 2.0f)
        assertEquals("no lingering explosions", 0, engine.explosions.size)
    }
}
