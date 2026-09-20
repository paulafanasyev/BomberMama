package com.bombermama.core

/**
 * Главная героиня — Мама.
 *
 * @param lives оставшиеся жизни
 * @param maxBombs сколько бомб можно установить одновременно
 * @param fireRange радиус взрыва в клетках
 * @param speed скорость в клетках в секунду
 */
class Player(
    startX: Float,
    startY: Float,
    var lives: Int = 3,
    var maxBombs: Int = 1,
    var fireRange: Int = 1,
    var speed: Float = 3.6f
) : GridEntity(startX, startY) {

    var score: Int = 0
        private set

    /** Неуязвимость после получения урона (сек). */
    var invulnerableTime: Float = 0f
        private set

    /** Победная анимация. */
    var winTime: Float = 0f
        private set

    /** Анимация поражения. */
    var dead: Boolean = false
        private set

    fun addScore(points: Int) {
        score += points
    }

    fun addInvulnerable(seconds: Float) {
        invulnerableTime = maxOf(invulnerableTime, seconds)
    }

    fun tickInvulnerable(dt: Float) {
        if (invulnerableTime > 0) invulnerableTime -= dt
        if (winTime > 0) winTime -= dt
    }

    fun setWin() {
        winTime = 2.5f
    }

    fun setDead() {
        dead = true
    }

    fun applyBonus(type: BonusType, maxLives: Int) {
        when (type) {
            BonusType.FIRE -> fireRange++
            BonusType.BOMB -> maxBombs++
            BonusType.SPEED -> speed = (speed + 0.7f).coerceAtMost(MAX_SPEED)
            BonusType.LIFE -> lives = (lives + 1).coerceAtMost(maxLives)
            BonusType.STAR -> addScore(type.score)
        }
        addScore(type.score / 5)
    }

    override fun animPhase(time: Float): Float {
        return if (isMoving) (x + y + time * 6f) % 1f else 0f
    }

    companion object {
        const val MAX_SPEED = 6.5f
        const val START_LIVES = 3
        const val MAX_LIVES = 6
    }
}
