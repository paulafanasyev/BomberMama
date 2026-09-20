package com.bombermama.game

import android.graphics.RectF
import android.view.MotionEvent
import com.bombermama.core.Direction
import com.bombermama.input.TouchInputController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Тесты сенсорного управления на JVM (через Robolectric).
 * Проверяем: нажатие, удержание, диагональную проекцию, мёртвую зону.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TouchInputControllerTest {

    private lateinit var controller: TouchInputController

    @Before
    fun setUp() {
        controller = TouchInputController()
        // D-pad 200..600 по X, 400..800 по Y. Центр (400, 600).
        controller.dpadZone = RectF(200f, 400f, 600f, 800f)
        controller.bombZone = RectF(1000f, 600f, 1200f, 800f)
    }

    private fun motion(action: Int, x: Float, y: Float, pid: Int = 0): MotionEvent {
        val props = MotionEvent.PointerProperties().apply {
            id = pid
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        }
        return MotionEvent.obtain(
            0L, System.currentTimeMillis(), action, 1,
            arrayOf(props), arrayOf(coords), 0, 0, 1f, 1f,
            0, 0, 0, 0
        )
    }

    /**
     * Жест "нажал и перетащил": D-pad плавающий, поэтому направление
     * определяется смещением пальца от точки первого касания.
     */
    private fun drag(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        controller.onTouchEvent(motion(MotionEvent.ACTION_DOWN, fromX, fromY))
        controller.onTouchEvent(motion(MotionEvent.ACTION_MOVE, toX, toY))
    }

    @Test
    fun `drag right moves right`() {
        drag(400f, 600f, 550f, 600f)
        assertEquals(Direction.RIGHT, controller.direction)
    }

    @Test
    fun `drag left moves left`() {
        drag(400f, 600f, 250f, 600f)
        assertEquals(Direction.LEFT, controller.direction)
    }

    @Test
    fun `drag up moves up`() {
        drag(400f, 600f, 400f, 450f)
        assertEquals(Direction.UP, controller.direction)
    }

    @Test
    fun `drag down moves down`() {
        drag(400f, 600f, 400f, 750f)
        assertEquals(Direction.DOWN, controller.direction)
    }

    @Test
    fun `dead zone yields no direction`() {
        // Крошечное смещение внутри мёртвой зоны — героиня не двигается.
        drag(400f, 600f, 405f, 603f)
        assertEquals(Direction.NONE, controller.direction)
    }

    @Test
    fun `diagonal drag projects to dominant axis`() {
        // Сильнее по X, чем по Y → RIGHT.
        drag(400f, 600f, 520f, 660f)
        assertEquals("горизонталь доминирует", Direction.RIGHT, controller.direction)
    }

    @Test
    fun `release stops movement`() {
        drag(400f, 600f, 550f, 600f)
        assertEquals(Direction.RIGHT, controller.direction)
        controller.onTouchEvent(motion(MotionEvent.ACTION_UP, 550f, 600f))
        assertEquals(Direction.NONE, controller.direction)
    }

    @Test
    fun `tap without drag yields no direction`() {
        controller.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 550f, 600f))
        assertEquals("floating stick waits for movement", Direction.NONE, controller.direction)
    }

    @Test
    fun `bomb button press and hold`() {
        controller.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 1100f, 700f))
        assertTrue("bomb pressed", controller.bombPressed)
        assertTrue("bomb just pressed", controller.bombJustPressed)
        controller.consumeBombPress()
        assertFalse("just-pressed consumed", controller.bombJustPressed)
        // Удержание продолжает считаться нажатым.
        assertTrue(controller.isBombHold())
    }

    @Test
    fun `bomb button release clears state`() {
        controller.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 1100f, 700f))
        controller.onTouchEvent(motion(MotionEvent.ACTION_UP, 1100f, 700f))
        assertFalse(controller.bombPressed)
        assertFalse(controller.isBombHold())
    }

    @Test
    fun `touch outside zones is ignored`() {
        val handled = controller.onTouchEvent(motion(MotionEvent.ACTION_DOWN, 50f, 50f))
        assertFalse("outside zones not handled", handled)
        assertEquals(Direction.NONE, controller.direction)
    }
}
