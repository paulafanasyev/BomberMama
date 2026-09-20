package com.bombermama.core

/**
 * Бонус, лежащий на поле. Появляется после разрушения некоторых блоков.
 */
class BonusPickup(
    val tileX: Int,
    val tileY: Int,
    val type: BonusType
) {
    /** Подобран игроком. */
    var picked: Boolean = false
        private set

    /** Анимация появления/покачивания. */
    var age: Float = 0f
        private set

    fun tick(dt: Float) {
        age += dt
    }

    fun pick() {
        picked = true
    }

    /** Фаза анимации покачивания 0..3. */
    val bobPhase: Int
        get() = ((age / 0.18f).toInt() % 4)
}
