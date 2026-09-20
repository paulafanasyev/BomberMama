package com.bombermama

import android.app.Application
import com.bombermama.audio.AudioEngine
import com.bombermama.audio.MusicEngine
import com.bombermama.save.SharedProgressStore

/**
 * Глобальный контекст игры: аудио-движки и хранилище прогресса.
 *
 * Живут в Application, чтобы пережить смену активностей
 * (например, переход из игры в меню) без повторной инициализации.
 */
class BomberMamaApp : Application() {

    val audio: AudioEngine by lazy {
        AudioEngine().apply {
            preGenerate()
            setEnabled(progress.soundEnabled)
        }
    }

    val music: MusicEngine by lazy {
        MusicEngine().apply {
            setEnabled(progress.musicEnabled)
        }
    }

    val progress: SharedProgressStore by lazy {
        SharedProgressStore(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Гарантируем, что синглтоны инициализируются в главном потоке.
        @Suppress("UNUSED_EXPRESSION")
        progress
    }
}
