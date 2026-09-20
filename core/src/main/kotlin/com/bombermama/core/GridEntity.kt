package com.bombermama.core

/**
 * Базовый элемент поля с плавным перемещением между клетками.
 *
 * Система координат: позиция (x,y) — это центр сущности в клеточных единицах,
 * где клетка (tx,ty) занимает диапазон [tx, tx+1) по горизонтали и
 * [ty, ty+1) по вертикали; центр клетки — (tx+0.5, ty+0.5).
 * Такая модель делает коллизии и привязку бомб к сетке простыми и надёжными.
 */
abstract class GridEntity(
    var x: Float,
    var y: Float,
    var direction: Direction = Direction.NONE
) {
    /** Горизонтальная координата клетки, в которой находится сущность. */
    val tileX: Int get() = x.toInt().coerceIn(0, 255)

    /** Вертикальная координата клетки, в которой находится сущность. */
    val tileY: Int get() = y.toInt().coerceIn(0, 255)

    val isMoving: Boolean get() = direction != Direction.NONE

    /** Доля продвижения для анимации ног/покачивания. */
    abstract fun animPhase(time: Float): Float
}
