package com.bombermama.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.bombermama.BomberMamaApp
import com.bombermama.R

/**
 * Настройки: звук и музыка. Состояние немедленно применяется
 * и сохраняется между запусками приложения.
 */
class SettingsActivity : AppCompatActivity() {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val swSound = findViewById<SwitchCompat>(R.id.swSound)
        val swMusic = findViewById<SwitchCompat>(R.id.swMusic)

        swSound.isChecked = app.progress.soundEnabled
        swMusic.isChecked = app.progress.musicEnabled

        swSound.setOnCheckedChangeListener { _, isChecked ->
            app.progress.soundEnabled = isChecked
            app.audio.setEnabled(isChecked)
            if (isChecked) app.audio.play("bonus")
        }
        swMusic.setOnCheckedChangeListener { _, isChecked ->
            app.progress.musicEnabled = isChecked
            app.music.setEnabled(isChecked)
            if (isChecked) {
                app.music.start()
            } else {
                app.music.stop()
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (app.progress.soundEnabled) app.audio.play("button")
            finish()
        }
    }
}
