package com.bombermama.core

/**
 * Типы бонусов, которые могут выпадать из разрушаемых блоков.
 */
enum class BonusType(val score: Int) {
    /** 🔥 Увеличение радиуса взрыва. */
    FIRE(50),

    /** 💣 Дополнительная бомба. */
    BOMB(50),

    /** 🏃 Увеличение скорости. */
    SPEED(50),

    /** ❤️ Дополнительная жизнь. */
    LIFE(100),

    /** ⭐ Бонус очков. */
    STAR(250);

    companion object {
        /** Доступные в обычной игре бонусы (без секретных). */
        val REGULAR = listOf(FIRE, BOMB, SPEED, LIFE, STAR)
    }
}
