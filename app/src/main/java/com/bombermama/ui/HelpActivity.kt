package com.bombermama.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bombermama.BomberMamaApp
import com.bombermama.R

/**
 * Экран помощи: цель, управление, бонусы, враги.
 */
class HelpActivity : AppCompatActivity() {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        app.audio.setEnabled(app.progress.soundEnabled)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (app.progress.soundEnabled) app.audio.play("button")
            finish()
        }
    }
}
