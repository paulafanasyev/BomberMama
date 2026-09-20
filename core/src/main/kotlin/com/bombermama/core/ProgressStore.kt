package com.bombermama.core

/**
 * Интерфейс системы сохранений. Хранит локально прогресс игрока.
 *
 * Конкретная реализация (Android SharedPreferences) живёт в Android-слое;
 * этот интерфейс позволяет тестировать логику сохранений на JVM.
 */
interface ProgressStore {

    /** Максимальный открытый уровень (1-based). */
    var maxUnlockedLevel: Int

    /** Лучший результат за всё время. */
    var bestScore: Int

    /** Включён ли звук. */
    var soundEnabled: Boolean

    /** Включена ли музыка. */
    var musicEnabled: Boolean

    /** Лучший результат на конкретном уровне. */
    fun bestScoreForLevel(level: Int): Int

    /** Сохранить результат на уровне (только если он лучше предыдущего). */
    fun recordLevelScore(level: Int, score: Int)

    /** Открыть уровень (прогресс). */
    fun unlockLevel(level: Int)

    /** Полный сброс прогресса. */
    fun reset()
}

/**
 * Простая реализация в памяти — используется в тестах на JVM.
 */
class InMemoryProgressStore : ProgressStore {
    private val levelScores = mutableMapOf<Int, Int>()
    override var maxUnlockedLevel: Int = 1
    override var bestScore: Int = 0
    override var soundEnabled: Boolean = true
    override var musicEnabled: Boolean = true

    override fun bestScoreForLevel(level: Int): Int = levelScores[level] ?: 0

    override fun recordLevelScore(level: Int, score: Int) {
        val current = bestScoreForLevel(level)
        if (score > current) levelScores[level] = score
        if (score > bestScore) bestScore = score
    }

    override fun unlockLevel(level: Int) {
        if (level > maxUnlockedLevel) maxUnlockedLevel = level
    }

    override fun reset() {
        levelScores.clear()
        maxUnlockedLevel = 1
        bestScore = 0
        soundEnabled = true
        musicEnabled = true
    }
}
