package com.bombermama.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bombermama.BomberMamaApp
import com.bombermama.R
import com.bombermama.core.Levels

/**
 * Экран рекордов: лучший счёт и количество пройденных уровней.
 */
class RecordsActivity : AppCompatActivity() {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        app.audio.setEnabled(app.progress.soundEnabled)

        updateStats()

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            app.audio.play("button")
            AlertDialog.Builder(this)
                .setMessage(R.string.records_reset_confirm)
                .setPositiveButton(R.string.records_reset_yes) { _, _ ->
                    app.progress.reset()
                    app.audio.play("bonus")
                    updateStats()
                }
                .setNegativeButton(R.string.records_reset_no, null)
                .show()
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            app.audio.play("button")
            finish()
        }
    }

    private fun updateStats() {
        val completed = (1..Levels.count).count { app.progress.bestScoreForLevel(it) > 0 }
        findViewById<TextView>(R.id.tvBestScore).text = app.progress.bestScore.toString()
        findViewById<TextView>(R.id.tvLevelsCompleted).text = "$completed / ${Levels.count}"
    }
}
