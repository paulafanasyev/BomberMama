package com.bombermama.save

import android.content.Context
import android.content.SharedPreferences
import com.bombermama.core.ProgressStore

/**
 * Реализация системы сохранений на SharedPreferences.
 *
 * Прогресс переживает закрытие приложения и перезагрузку устройства.
 * Файл хранится в приватном хранилище приложения — никаких серверов,
 * игра полностью офлайн.
 */
class SharedProgressStore(context: Context) : ProgressStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var maxUnlockedLevel: Int
        get() = prefs.getInt(KEY_MAX_LEVEL, 1)
        set(value) {
            // Открываем все уровни вплоть до указанного.
            prefs.edit().putInt(KEY_MAX_LEVEL, value.coerceAtLeast(1)).apply()
        }

    override var bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)
        set(value) {
            prefs.edit().putInt(KEY_BEST_SCORE, value.coerceAtLeast(0)).apply()
        }

    override var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND, value).apply()
        }

    override var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MUSIC, value).apply()
        }

    override fun bestScoreForLevel(level: Int): Int =
        prefs.getInt("$KEY_LEVEL_SCORE$level", 0)

    override fun recordLevelScore(level: Int, score: Int) {
        val current = bestScoreForLevel(level)
        if (score > current) {
            prefs.edit().putInt("$KEY_LEVEL_SCORE$level", score).apply()
        }
        if (score > bestScore) {
            bestScore = score
        }
    }

    override fun unlockLevel(level: Int) {
        if (level > maxUnlockedLevel) {
            maxUnlockedLevel = level
        }
    }

    override fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "bombermama_progress"
        private const val KEY_MAX_LEVEL = "max_unlocked_level"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_LEVEL_SCORE = "level_score_"
    }
}
