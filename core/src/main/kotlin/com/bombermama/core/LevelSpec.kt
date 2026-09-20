package com.bombermama.core

/**
 * Описание одного уровня. Уровни задаются декларативно и легко расширяются.
 *
 * Карта уровня составляется из схем стен и блоков, что позволяет создавать
 * существенно разные архитектуры поля, а не просто "те же карты с переставленными
 * блоками".
 *
 * @param width     ширина поля в клетках
 * @param height    высота поля в клетках
 * @param wallScheme схема расположения неразрушаемых стен
 * @param blockDensity плотность разрушаемых блоков (0..1) на свободных клетках
 * @param enemies   список типов врагов и их количество
 * @param seed      зерно генератора (фиксированное => уровень всегда одинаковый)
 * @param timeLimit ограничение времени в секундах (0 = без лимита)
 */
data class LevelSpec(
    val number: Int,
    val width: Int,
    val height: Int,
    val wallScheme: WallScheme,
    val blockDensity: Float,
    val enemies: Map<EnemyType, Int>,
    val seed: Long,
    val timeLimit: Int = 0
) {
    /** Общее количество врагов на уровне. */
    val totalEnemies: Int get() = enemies.values.sum()

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 24
    }
}

/**
 * Схема расположения неразрушаемых стен.
 * Каждая схема даёт свою архитектуру поля.
 */
enum class WallScheme {
    /** Классическая раскладка: опоры каждые 2 клетки. */
    CLASSIC_PILLARS,

    /** Поперечные стены с проходами (коридоры). */
    CORRIDORS_H,

    /** Продольные стены с проходами. */
    CORRIDORS_V,

    /** Комнаты 3x3 с дверными проёмами. */
    ROOMS,

    /** Крестовина в центре + диагональные опоры. */
    CROSS,

    /** Шахматный порядок опор (более плотный). */
    CHECKERBOARD,

    /** Лабиринт из Г-образных стен. */
    MAZE_L,

    /** Открытое поле с редкими одиночными опорами. */
    OPEN,

    /** Спираль стен вокруг центра. */
    SPIRAL,

    /** Двойной крест — сложная комбинация. */
    DOUBLE_CROSS
}
