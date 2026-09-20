package com.bombermama.input

import android.view.MotionEvent
import com.bombermama.core.Direction
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Сенсорное управление: виртуальный D-pad слева + кнопка "БОМБА" справа.
 *
 * Особенности:
 *  - поддерживает нажатие, удержание и отпускание;
 *  - диагональные нажатия D-pad'а проецируются на основную ось
 *    (наибольшее смещение), что удобно для клеточного движения;
 *  - плавающий стик: касание в любом месте D-pad-зоны становится центром
 *    (большой палец не обязан попадать в фиксированный круг);
 *  - мёртвая зона, чтобы случайные касания не двигали героиню.
 */
class TouchInputController {

    /** Зона D-pad'а в координатах экрана. */
    var dpadZone: android.graphics.RectF = android.graphics.RectF()

    /** Зона кнопки бомбы. */
    var bombZone: android.graphics.RectF = android.graphics.RectF()

    /** Радиус активной зоны стика вокруг точки касания. */
    private val stickRadius: Float = 120f

    /** Мёртвая зона — меньше неё считаем, что палец неподвижен. */
    private val deadZone: Float = 26f

    /** Текущее направление (0..1 векторы). */
    private var activePointerId: Int = INVALID_POINTER
    private var originX: Float = 0f
    private var originY: Float = 0f

    /** Текущее направление ввода. UI читает его каждый кадр. */
    @Volatile
    var direction: Direction = Direction.NONE
        private set

    @Volatile
    var bombPressed: Boolean = false
        private set

    /** Только что нажата (для одиночного срабатывания). */
    @Volatile
    var bombJustPressed: Boolean = false
        private set

    private var bombPointerId: Int = INVALID_POINTER

    fun setZones(dpad: android.graphics.RectF, bomb: android.graphics.RectF) {
        dpadZone = dpad
        bombZone = bomb
    }

    /**
     * Обработка сенсорного события.
     * @return true, если событие было обработано контроллером.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val x = event.getX(idx)
                val y = event.getY(idx)
                val pid = event.getPointerId(idx)
                if (bombZone.contains(x, y) && bombPointerId == INVALID_POINTER) {
                    bombPointerId = pid
                    bombPressed = true
                    bombJustPressed = true
                    return true
                }
                if (dpadZone.contains(x, y) && activePointerId == INVALID_POINTER) {
                    activePointerId = pid
                    originX = x
                    originY = y
                    updateDirection(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.findPointerIndex(activePointerId) >= 0) {
                    val idx = event.findPointerIndex(activePointerId)
                    updateDirection(event.getX(idx), event.getY(idx))
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = event.getPointerId(event.actionIndex)
                if (pid == activePointerId) {
                    activePointerId = INVALID_POINTER
                    direction = Direction.NONE
                }
                if (pid == bombPointerId) {
                    bombPointerId = INVALID_POINTER
                    bombPressed = false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER
                bombPointerId = INVALID_POINTER
                direction = Direction.NONE
                bombPressed = false
            }
        }
        return false
    }

    private fun updateDirection(x: Float, y: Float) {
        val dx = x - originX
        val dy = y - originY
        if (abs(dx) < deadZone && abs(dy) < deadZone) {
            direction = Direction.NONE
            return
        }
        // Диагональ проецируется на более выраженную ось.
        direction = if (abs(dx) > abs(dy)) {
            if (dx > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0) Direction.DOWN else Direction.UP
        }
    }

    /** Сбрасываем флаг "только что нажата" — движок уже обработал запрос. */
    fun consumeBombPress() {
        bombJustPressed = false
    }

    /** Кнопка бомбы удерживается. */
    fun isBombHold(): Boolean = bombPressed

    fun reset() {
        activePointerId = INVALID_POINTER
        bombPointerId = INVALID_POINTER
        direction = Direction.NONE
        bombPressed = false
        bombJustPressed = false
    }

    companion object {
        private const val INVALID_POINTER = -1
        @Suppress("unused")
        private fun angleOf(dx: Float, dy: Float): Float = atan2(dy, dx)
    }
}
