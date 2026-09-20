package com.bombermama.core

/**
 * Направления движения на сетке.
 */
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    NONE(0, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
        else -> NONE
    }

    companion object {
        val MOVING = listOf(UP, DOWN, LEFT, RIGHT)
    }
}
