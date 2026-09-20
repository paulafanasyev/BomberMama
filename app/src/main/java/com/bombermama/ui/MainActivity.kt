package com.bombermama.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bombermama.BomberMamaApp
import com.bombermama.R
import com.bombermama.game.GameActivity

/**
 * Главное меню. Все кнопки реально работают.
 */
class MainActivity : AppCompatActivity() {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app.audio.setEnabled(app.progress.soundEnabled)

        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            playClick()
            // Новая игра начинается с первого уровня, но прогресс сохраняется.
            startActivity(GameActivity.intent(this, levelNumber = 1))
        }
        findViewById<Button>(R.id.btnLevels).setOnClickListener {
            playClick()
            startActivity(Intent(this, LevelsActivity::class.java))
        }
        findViewById<Button>(R.id.btnRecords).setOnClickListener {
            playClick()
            startActivity(Intent(this, RecordsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            playClick()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnHelp).setOnClickListener {
            playClick()
            startActivity(Intent(this, HelpActivity::class.java))
        }
        findViewById<Button>(R.id.btnExit).setOnClickListener {
            playClick()
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        // Меню — только музыка, без эффектов.
        if (app.progress.musicEnabled) {
            app.music.setEnabled(true)
            app.music.start()
        } else {
            app.music.stop()
        }
    }

    override fun onPause() {
        super.onPause()
        app.music.stop()
    }

    private fun playClick() {
        if (app.progress.soundEnabled) {
            app.audio.play("button")
        }
    }
}
