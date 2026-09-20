package com.bombermama.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты генерации поля: стены, блоки, выход, зона старта.
 */
class GameFieldTest {

    @Test
    fun `field has correct dimensions`() {
        val spec = Levels.byNumber(1)!!
        val field = GameField.create(spec, spec.seed)
        assertEquals(spec.width, field.width)
        assertEquals(spec.height, field.height)
    }

    @Test
    fun `border is always walls`() {
        val spec = Levels.byNumber(3)!!
        val field = GameField.create(spec, spec.seed)
        for (x in 0 until field.width) {
            assertEquals(Tile.WALL, field[x, 0])
            assertEquals(Tile.WALL, field[x, field.height - 1])
        }
        for (y in 0 until field.height) {
            assertEquals(Tile.WALL, field[0, y])
            assertEquals(Tile.WALL, field[field.width - 1, y])
        }
    }

    @Test
    fun `start zone is clear for player`() {
        for (n in 1..Levels.count) {
            val spec = Levels.byNumber(n)!!
            val field = GameField.create(spec, spec.seed)
            // Внутренняя зона старта 2x2 (кроме бордюра) должна быть свободна,
            // чтобы героиня могла сразу двигаться и ставить бомбы.
            for (y in 1..2) {
                for (x in 1..2) {
                    assertTrue("Level $n start ($x,$y) blocked", field.isPassable(x, y))
                }
            }
        }
    }

    @Test
    fun `level 1 is solvable-friendly - has blocks and floor`() {
        val spec = Levels.byNumber(1)!!
        val field = GameField.create(spec, spec.seed)
        var blocks = 0
        var walls = 0
        var floors = 0
        for (y in 0 until field.height) for (x in 0 until field.width) {
            when (field[x, y]) {
                Tile.BLOCK -> blocks++
                Tile.WALL -> walls++
                Tile.FLOOR, Tile.EXIT -> floors++
            }
        }
        assertTrue("level 1 must have some blocks", blocks > 5)
        // note: counts must be positive
        assertTrue("level 1 must have floor", floors > 10)
        assertTrue("level 1 must have walls", walls > 4)
    }

    @Test
    fun `different wall schemes produce different maps`() {
        val specs = Levels.all.take(10)
        val maps = specs.map { spec ->
            GameField.create(spec, spec.seed).let { f ->
                (0 until f.height).joinToString("") { y ->
                    (0 until f.width).joinToString("") { x ->
                        when (f[x, y]) { Tile.WALL -> "W"; Tile.BLOCK -> "B"; else -> "." }
                    }
                }
            }
        }
        assertEquals("levels should be distinct", specs.size, maps.toSet().size)
    }

    @Test
    fun `deterministic generation with same seed`() {
        val spec = Levels.byNumber(5)!!
        val a = GameField.create(spec, spec.seed)
        val b = GameField.create(spec, spec.seed)
        for (y in 0 until a.height) for (x in 0 until a.width) {
            assertEquals(a[x, y], b[x, y])
        }
    }

    @Test
    fun `exit exists on every level`() {
        for (n in 1..Levels.count) {
            val spec = Levels.byNumber(n)!!
            val field = GameField.create(spec, spec.seed)
            assertNotNull("level $n has no exit", field.exitTile)
        }
    }

    @Test
    fun `destroyBlock removes block and reports bonus`() {
        val spec = Levels.byNumber(1)!!
        val field = GameField.create(spec, spec.seed)
        // найдём блок
        var bx = -1; var by = -1
        for (y in 1 until field.height - 1) {
            for (x in 1 until field.width - 1) {
                if (field[x, y] == Tile.BLOCK) { bx = x; by = y; break }
            }
            if (bx >= 0) break
        }
        assertTrue("need at least one block", bx >= 0)
        field.destroyBlock(bx, by)
        assertEquals(Tile.FLOOR, field[bx, by])
        // Повторное разрушение пустой клетки — false.
        assertFalse(field.destroyBlock(bx, by))
    }

    @Test
    fun `cannot destroy wall or floor`() {
        val spec = Levels.byNumber(1)!!
        val field = GameField.create(spec, spec.seed)
        assertFalse(field.destroyBlock(0, 0))
    }

    @Test
    fun `all levels have enemies within field capacity`() {
        for (n in 1..Levels.count) {
            val spec = Levels.byNumber(n)!!
            assertTrue("level $n has enemies", spec.totalEnemies > 0)
            // Враги должны помещаться в свободные клетки поля.
            assertTrue("level $n too many enemies", spec.totalEnemies < (spec.width - 2) * (spec.height - 2) - 20)
        }
    }

    @Test
    fun `endless levels are valid`() {
        val spec = Levels.endless(30)
        val field = GameField.create(spec, spec.seed)
        assertTrue(field.width >= 13 && field.height >= 11)
        assertNotNull(field.exitTile)
    }
}
