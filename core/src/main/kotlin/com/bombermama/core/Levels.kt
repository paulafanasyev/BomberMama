package com.bombermama.core

/**
 * Список уровней игры. Минимум 20 уровней с разной архитектурой поля,
 * нарастающей сложностью и разными комбинациями врагов.
 *
 * Уровни 1–5:   обучение механике (мало врагов, простые схемы).
 * Уровни 6–10:  больше врагов, появляются быстрые враги.
 * Уровни 11–15: более сложная архитектура поля (комнаты, лабиринты, спираль).
 * Уровни 16–24: сложные комбинации врагов, охотники, плотные блоки, лимиты времени.
 */
object Levels {

    val all: List<LevelSpec> = buildList {
        // ===================== 1–5: ОБУЧЕНИЕ =====================
        add(LevelSpec(1, 13, 11, WallScheme.CLASSIC_PILLARS, 0.35f,
            mapOf(EnemyType.SLOW to 2), seed = 101, timeLimit = 180))

        add(LevelSpec(2, 13, 11, WallScheme.OPEN, 0.45f,
            mapOf(EnemyType.SLOW to 3), seed = 202, timeLimit = 170))

        add(LevelSpec(3, 15, 11, WallScheme.CORRIDORS_H, 0.40f,
            mapOf(EnemyType.SLOW to 4), seed = 303, timeLimit = 170))

        add(LevelSpec(4, 13, 13, WallScheme.CHECKERBOARD, 0.35f,
            mapOf(EnemyType.SLOW to 3, EnemyType.FAST to 1), seed = 404, timeLimit = 160))

        add(LevelSpec(5, 15, 13, WallScheme.CROSS, 0.45f,
            mapOf(EnemyType.SLOW to 4, EnemyType.FAST to 1), seed = 505, timeLimit = 160))

        // ===================== 6–10: БОЛЬШЕ ВРАГОВ =====================
        add(LevelSpec(6, 15, 13, WallScheme.CORRIDORS_V, 0.50f,
            mapOf(EnemyType.SLOW to 3, EnemyType.FAST to 2), seed = 606, timeLimit = 150))

        add(LevelSpec(7, 15, 13, WallScheme.CLASSIC_PILLARS, 0.55f,
            mapOf(EnemyType.FAST to 3, EnemyType.CHANGER to 1), seed = 707, timeLimit = 150))

        add(LevelSpec(8, 17, 13, WallScheme.ROOMS, 0.45f,
            mapOf(EnemyType.SLOW to 4, EnemyType.CHANGER to 2), seed = 808, timeLimit = 140))

        add(LevelSpec(9, 15, 15, WallScheme.MAZE_L, 0.40f,
            mapOf(EnemyType.FAST to 3, EnemyType.CHANGER to 2), seed = 909, timeLimit = 140))

        add(LevelSpec(10, 17, 15, WallScheme.CHECKERBOARD, 0.50f,
            mapOf(EnemyType.SLOW to 4, EnemyType.FAST to 2, EnemyType.CHANGER to 2), seed = 1010, timeLimit = 130))

        // ============= 11–15: СЛОЖНАЯ АРХИТЕКТУРА ПОЛЯ =============
        add(LevelSpec(11, 17, 15, WallScheme.SPIRAL, 0.45f,
            mapOf(EnemyType.CHANGER to 3, EnemyType.FAST to 2), seed = 1111, timeLimit = 140))

        add(LevelSpec(12, 15, 15, WallScheme.DOUBLE_CROSS, 0.55f,
            mapOf(EnemyType.SLOW to 3, EnemyType.HUNTER to 1), seed = 1212, timeLimit = 130))

        add(LevelSpec(13, 19, 15, WallScheme.CORRIDORS_H, 0.50f,
            mapOf(EnemyType.FAST to 4, EnemyType.HUNTER to 1), seed = 1313, timeLimit = 130))

        add(LevelSpec(14, 17, 17, WallScheme.ROOMS, 0.55f,
            mapOf(EnemyType.CHANGER to 3, EnemyType.HUNTER to 2), seed = 1414, timeLimit = 120))

        add(LevelSpec(15, 19, 17, WallScheme.MAZE_L, 0.50f,
            mapOf(EnemyType.SLOW to 4, EnemyType.FAST to 3, EnemyType.HUNTER to 1), seed = 1515, timeLimit = 120))

        // ============= 16–24: СЛОЖНЫЕ КОМБИНАЦИИ =============
        add(LevelSpec(16, 19, 17, WallScheme.SPIRAL, 0.55f,
            mapOf(EnemyType.FAST to 3, EnemyType.CHANGER to 3, EnemyType.HUNTER to 1), seed = 1616, timeLimit = 115))

        add(LevelSpec(17, 19, 19, WallScheme.DOUBLE_CROSS, 0.60f,
            mapOf(EnemyType.SLOW to 4, EnemyType.HUNTER to 2), seed = 1717, timeLimit = 115))

        add(LevelSpec(18, 21, 17, WallScheme.CHECKERBOARD, 0.55f,
            mapOf(EnemyType.FAST to 4, EnemyType.HUNTER to 2), seed = 1818, timeLimit = 110))

        add(LevelSpec(19, 21, 19, WallScheme.CROSS, 0.60f,
            mapOf(EnemyType.CHANGER to 4, EnemyType.HUNTER to 2), seed = 1919, timeLimit = 105))

        add(LevelSpec(20, 21, 19, WallScheme.MAZE_L, 0.60f,
            mapOf(EnemyType.SLOW to 4, EnemyType.FAST to 3, EnemyType.CHANGER to 3, EnemyType.HUNTER to 2),
            seed = 2020, timeLimit = 100))

        // Финальные уровни — самая сложная комбинация.
        add(LevelSpec(21, 21, 19, WallScheme.SPIRAL, 0.62f,
            mapOf(EnemyType.FAST to 4, EnemyType.HUNTER to 3), seed = 2121, timeLimit = 100))

        add(LevelSpec(22, 23, 19, WallScheme.ROOMS, 0.60f,
            mapOf(EnemyType.CHANGER to 4, EnemyType.HUNTER to 3), seed = 2222, timeLimit = 95))

        add(LevelSpec(23, 23, 21, WallScheme.DOUBLE_CROSS, 0.65f,
            mapOf(EnemyType.SLOW to 5, EnemyType.FAST to 4, EnemyType.HUNTER to 3), seed = 2323, timeLimit = 95))

        add(LevelSpec(24, 23, 21, WallScheme.MAZE_L, 0.65f,
            mapOf(EnemyType.SLOW to 4, EnemyType.FAST to 4, EnemyType.CHANGER to 4, EnemyType.HUNTER to 4),
            seed = 2424, timeLimit = 90))
    }

    /** Количество уровней в игре. */
    val count: Int get() = all.size

    /** Получить уровень по номеру (1-based). */
    fun byNumber(number: Int): LevelSpec? = all.firstOrNull { it.number == number }

    /** Первый уровень. */
    val first: LevelSpec get() = all.first()

    /** Последний уровень. */
    val last: LevelSpec get() = all.last()

    /**
     * Для уровней за пределами кампании (если кто-то захочет расширить)
     * генерируется бесконечный режим с нарастающей сложностью.
     */
    fun endless(number: Int): LevelSpec {
        val clamped = number.coerceAtLeast(LevelSpec.MAX_LEVEL + 1)
        val idx = (clamped - 1) % WallScheme.values().size
        val scheme = WallScheme.values()[idx]
        return LevelSpec(
            number = clamped,
            width = (15 + (clamped % 9)).coerceAtMost(25),
            height = (13 + (clamped % 8)).coerceAtMost(23),
            wallScheme = scheme,
            blockDensity = (0.45f + (clamped % 10) * 0.02f).coerceAtMost(0.7f),
            enemies = mapOf(
                EnemyType.SLOW to (2 + clamped % 3),
                EnemyType.FAST to (1 + clamped % 3),
                EnemyType.CHANGER to (1 + clamped % 2),
                EnemyType.HUNTER to (1 + clamped % 3)
            ),
            seed = 9000L + clamped,
            timeLimit = 100
        )
    }
}
