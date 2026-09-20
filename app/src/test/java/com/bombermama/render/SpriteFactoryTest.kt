package com.bombermama.render

import com.bombermama.core.BonusType
import com.bombermama.core.Direction
import com.bombermama.core.EnemyType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Тесты генератора спрайтов: все спрайты должны реально создаваться
 * и иметь ненулевые размеры (проверка процедурной пиксельной графики).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SpriteFactoryTest {

    private val factory = SpriteFactory(scale = 4)
    private val expected = 16 * 4 // TILE * scale

    @Test
    fun `tiles have correct size`() {
        assertEquals(expected, factory.floorTile(true).width)
        assertEquals(expected, factory.floorTile(false).height)
        assertEquals(expected, factory.wallTile().width)
        assertEquals(expected, factory.blockTile().height)
    }

    @Test
    fun `bomb sprites exist for all frames`() {
        for (frame in 0..1) {
            val bmp = factory.bomb(frame)
            assertEquals(expected, bmp.width)
            assertEquals(expected, bmp.height)
            assertTrue("bomb frame $frame not blank", bmp.rowBytes > 0)
        }
    }

    @Test
    fun `explosion segments exist for all kinds and axes`() {
        for (kind in 0..2) {
            for (axis in 0..1) {
                for (frame in 0..2) {
                    val bmp = factory.explosion(kind, axis, frame)
                    assertEquals("kind=$kind axis=$axis frame=$frame", expected, bmp.width)
                }
            }
        }
    }

    @Test
    fun `bonus sprites exist for all types`() {
        for (type in BonusType.values()) {
            for (frame in 0..3) {
                val bmp = factory.bonus(type, frame)
                assertEquals(type.name, expected, bmp.width)
            }
        }
    }

    @Test
    fun `exit sprite exists`() {
        for (frame in 0..1) {
            assertEquals(expected, factory.exit(frame).width)
        }
    }

    @Test
    fun `mama sprites exist for all directions and poses`() {
        for (dir in Direction.values()) {
            for (frame in 0..3) {
                for (pose in 0..3) {
                    val bmp = factory.mama(dir, frame, pose)
                    assertEquals("$dir f$frame p$pose", expected, bmp.width)
                    assertEquals(expected, bmp.height)
                }
            }
        }
    }

    @Test
    fun `enemy sprites exist for all types`() {
        for (type in EnemyType.values()) {
            for (frame in 0..3) {
                assertEquals(expected, factory.enemy(type, frame, false).width)
                assertEquals(expected, factory.enemy(type, frame, true).height)
            }
        }
    }

    @Test
    fun `sprite cache returns same instance`() {
        val a = factory.wallTile()
        val b = factory.wallTile()
        assertSame("sprites must be cached", a, b)
    }

    @Test
    fun `mama sprite is not fully transparent`() {
        val bmp = factory.mama(Direction.DOWN, 0, 0)
        var nonTransparent = 0
        for (y in 0 until bmp.height) {
            for (x in 0 until bmp.width) {
                if ((bmp.getPixel(x, y) ushr 24) and 0xFF != 0) nonTransparent++
            }
        }
        assertTrue("heroine must be visible", nonTransparent > 100)
    }

    @Test
    fun `enemy sprite is not fully transparent`() {
        val bmp = factory.enemy(EnemyType.HUNTER, 0, false)
        var nonTransparent = 0
        for (y in 0 until bmp.height step 2) {
            for (x in 0 until bmp.width step 2) {
                if ((bmp.getPixel(x, y) ushr 24) and 0xFF != 0) nonTransparent++
            }
        }
        assertTrue("enemy must be visible", nonTransparent > 40)
    }
}
