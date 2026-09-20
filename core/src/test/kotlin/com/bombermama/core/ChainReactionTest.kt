package com.bombermama.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты цепных реакций: взрыв A → бомба B → бомба C.
 * Проверяем, что нет двойного урона, зависших бомб и бесконечных циклов.
 */
class ChainReactionTest {

    private fun flatEngine(level: Int = 1): GameEngine {
        val spec = Levels.byNumber(level)!!
        val engine = GameEngine(spec, spec.seed)
        // Полностью свободное поле для предсказуемых тестов.
        for (y in 0 until engine.field.height)
            for (x in 0 until engine.field.width)
                engine.field.setTile(x, y, Tile.FLOOR)
        // Оставляем обрамление стенами, чтобы не убежать за край.
        for (x in 0 until engine.field.width) {
            engine.field.setTile(x, 0, Tile.WALL)
            engine.field.setTile(x, engine.field.height - 1, Tile.WALL)
        }
        for (y in 0 until engine.field.height) {
            engine.field.setTile(0, y, Tile.WALL)
            engine.field.setTile(engine.field.width - 1, y, Tile.WALL)
        }
        return engine
    }

    private fun tickUntil(engine: GameEngine, seconds: Float) {
        var t = 0f
        val step = 1 / 60f
        while (t < seconds) { engine.update(step); t += step }
    }

    @Test
    fun `chain reaction detonates second bomb immediately`() {
        val engine = flatEngine()
        engine.player.fireRange = 3
        // Бомба A в (5,5), бомба B в (7,5) — в радиусе A.
        engine.bombs.add(Bomb(5, 5, fireRange = 3))
        engine.bombs.add(Bomb(7, 5, fireRange = 3))

        // Взрываем A вручную (через окончание фитиля).
        tickUntil(engine, Bomb.FUSE_TIME + 0.3f)

        assertEquals("all bombs detonated", 0, engine.bombs.size)
        // Оба центра должны быть охвачены взрывом.
        assertTrue(engine.explosions.any { it.tileX == 5 && it.tileY == 5 })
        assertTrue(engine.explosions.any { it.tileX == 7 && it.tileY == 5 })
    }

    @Test
    fun `long chain A to B to C terminates`() {
        val engine = flatEngine()
        engine.player.fireRange = 5
        engine.bombs.add(Bomb(3, 5, fireRange = 5))
        engine.bombs.add(Bomb(5, 5, fireRange = 5))
        engine.bombs.add(Bomb(7, 5, fireRange = 5))

        val start = System.currentTimeMillis()
        tickUntil(engine, Bomb.FUSE_TIME + 0.5f)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("chain must terminate quickly (under 2s), took ${elapsed}ms", elapsed < 2000)
        assertEquals("no lingering bombs", 0, engine.bombs.size)
        assertTrue(engine.explosions.any { it.tileX == 3 && it.tileY == 5 })
        assertTrue(engine.explosions.any { it.tileX == 5 && it.tileY == 5 })
        assertTrue(engine.explosions.any { it.tileX == 7 && it.tileY == 5 })
    }

    @Test
    fun `no double damage - overlapping chain explosions cost one life only`() {
        val engine = flatEngine()
        engine.player.lives = 3
        engine.player.x = 9f; engine.player.y = 5f
        engine.player.fireRange = 8
        // Две бомбы так, что игрок попадает в зону обоих взрывов.
        engine.bombs.add(Bomb(1, 5, fireRange = 10))
        engine.bombs.add(Bomb(3, 5, fireRange = 10))

        tickUntil(engine, Bomb.FUSE_TIME + 0.6f)

        // Игрок должен потерять ровно одну жизнь (неуязвимость после первого урона).
        assertEquals("exactly one life lost", 2, engine.player.lives)
    }

    @Test
    fun `no duplicate explosion segments on same tile`() {
        val engine = flatEngine()
        engine.player.fireRange = 5
        engine.bombs.add(Bomb(5, 5, fireRange = 5))
        engine.bombs.add(Bomb(5, 7, fireRange = 5)) // пересекается с первой

        tickUntil(engine, Bomb.FUSE_TIME + 0.4f)

        // На одной клетке не должно быть двух сегментов.
        val seen = mutableSetOf<Pair<Int, Int>>()
        for (e in engine.explosions) {
            val key = e.tileX to e.tileY
            assertTrue("duplicate explosion at $key", seen.add(key))
        }
    }

    @Test
    fun `chain reaction blocked by wall does not propagate`() {
        val engine = flatEngine()
        engine.player.fireRange = 5
        // Стена между A и B.
        engine.field.setTile(6, 5, Tile.WALL)
        engine.bombs.add(Bomb(5, 5, fireRange = 5))
        engine.bombs.add(Bomb(7, 5, fireRange = 5))

        tickUntil(engine, Bomb.FUSE_TIME + 0.4f)

        // B не должна взорваться от A (стена блокирует), но может взорваться
        // сама по окончанию своего фитиля. Проверяем, что взрыв не дошёл через стену.
        assertFalse(engine.explosions.any { it.tileX == 6 && it.tileY == 5 })
    }

    @Test
    fun `no lingering bombs after massive chain`() {
        val engine = flatEngine()
        engine.player.fireRange = 12
        // Ряд из 10 бомб.
        for (i in 0 until 10) {
            engine.bombs.add(Bomb(2 + i, 5, fireRange = 12))
        }
        tickUntil(engine, Bomb.FUSE_TIME + 1.0f)
        assertEquals("all bombs cleared", 0, engine.bombs.size)
    }

    @Test
    fun `explosion does not pass through wall diagonally`() {
        val engine = flatEngine()
        engine.player.fireRange = 3
        engine.bombs.add(Bomb(5, 5, fireRange = 3))
        engine.field.setTile(6, 5, Tile.WALL)
        engine.field.setTile(5, 6, Tile.WALL)
        tickUntil(engine, Bomb.FUSE_TIME + 0.4f)
        // Ни одна из клеток за стенами не должна взорваться.
        assertFalse(engine.explosions.any { it.tileX == 7 && it.tileY == 5 })
        assertFalse(engine.explosions.any { it.tileX == 5 && it.tileY == 7 })
    }
}
