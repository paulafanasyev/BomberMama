package com.bombermama.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bombermama.BomberMamaApp
import com.bombermama.R
import com.bombermama.core.Levels
import com.bombermama.ui.MainActivity

/**
 * Активность игры: содержит [GameSurfaceView] и оверлеи (пауза, победа, поражение).
 *
 * Оверлеи — это настоящие View поверх SurfaceView: на время паузы/финиша
 * симуляция полностью останавливается (см. [GameSurfaceView.pauseGame]).
 */
class GameActivity : AppCompatActivity(), GameSurfaceView.GameCallback {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    private lateinit var gameView: GameSurfaceView
    private var overlay: View? = null
    private var levelNumber: Int = 1
    private var resultShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        levelNumber = intent.getIntExtra(EXTRA_LEVEL, 1)
        setContentView(R.layout.activity_game)

        gameView = findViewById(R.id.gameView)
        gameView.callback = this
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            app.audio.play("pause")
            gameView.pauseGame()
            showPauseOverlay()
        }

        // Музыка в игре — та же мелодия, тише эффектов.
        if (app.progress.musicEnabled) {
            app.music.setEnabled(true)
            app.music.start()
        }
    }

    override fun onResume() {
        super.onResume()
        app.audio.setEnabled(app.progress.soundEnabled)
        gameView.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
        app.music.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.release()
    }

    override fun onBackPressed() {
        // Назад → пауза, а не мгновенный выход из уровня.
        if (overlay == null) {
            app.audio.play("pause")
            gameView.pauseGame()
            showPauseOverlay()
        } else {
            super.onBackPressed()
        }
    }

    // =========================================================================
    // Оверлеи
    // =========================================================================

    private fun showPauseOverlay() {
        if (overlay != null) return
        val v = layoutInflater.inflate(R.layout.overlay_pause, null)
        addContentView(v, fullScreenParams())
        overlay = v

        v.findViewById<Button>(R.id.btnResume).setOnClickListener {
            app.audio.play("button")
            removeOverlay()
            gameView.resumeGame()
        }
        v.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            app.audio.play("button")
            removeOverlay()
            recreateLevel()
        }
        v.findViewById<Button>(R.id.btnMenu).setOnClickListener {
            app.audio.play("button")
            app.music.stop()
            finish()
        }
    }

    private fun showWinOverlay(score: Int) {
        if (resultShown) return
        resultShown = true
        val isFinal = levelNumber >= Levels.count
        val v = layoutInflater.inflate(R.layout.overlay_result, null)
        addContentView(v, fullScreenParams())
        overlay = v

        val confetti = v.findViewById<ConfettiView>(R.id.confetti)
        if (!isFinal) confetti.startBurst()

        v.findViewById<TextView>(R.id.overlayTitle).text =
            if (isFinal) getString(R.string.win_final) else getString(R.string.win_title)
        v.findViewById<TextView>(R.id.overlaySubtitle).text = getString(R.string.win_subtitle)
        v.findViewById<TextView>(R.id.overlayScore).text = getString(R.string.win_score, score)

        val primary = v.findViewById<Button>(R.id.btnPrimary)
        val secondary = v.findViewById<Button>(R.id.btnSecondary)
        val tertiary = v.findViewById<Button>(R.id.btnTertiary)

        if (isFinal) {
            primary.text = getString(R.string.menu_levels)
            primary.setOnClickListener {
                app.audio.play("button")
                app.music.stop()
                finish()
            }
            secondary.visibility = View.GONE
            tertiary.text = getString(R.string.win_menu)
            tertiary.setOnClickListener {
                app.audio.play("button")
                app.music.stop()
                finish()
            }
        } else {
            primary.text = getString(R.string.win_next)
            primary.setOnClickListener {
                app.audio.play("button")
                app.music.stop()
                startActivity(GameActivity.intent(this, levelNumber + 1))
                finish()
            }
            secondary.text = getString(R.string.win_repeat)
            secondary.setOnClickListener {
                app.audio.play("button")
                removeOverlay()
                resultShown = false
                recreateLevel()
            }
            tertiary.text = getString(R.string.win_menu)
            tertiary.setOnClickListener {
                app.audio.play("button")
                app.music.stop()
                finish()
            }
        }
    }

    private fun showLoseOverlay(score: Int) {
        if (resultShown) return
        resultShown = true
        val v = layoutInflater.inflate(R.layout.overlay_result, null)
        addContentView(v, fullScreenParams())
        overlay = v

        v.findViewById<TextView>(R.id.overlayTitle).text = getString(R.string.lose_title)
        v.findViewById<TextView>(R.id.overlaySubtitle).text = ""
        v.findViewById<TextView>(R.id.overlayScore).text = getString(R.string.lose_score, score)

        val primary = v.findViewById<Button>(R.id.btnPrimary)
        primary.text = getString(R.string.lose_retry)
        primary.setOnClickListener {
            app.audio.play("button")
            removeOverlay()
            resultShown = false
            recreateLevel()
        }
        val secondary = v.findViewById<Button>(R.id.btnSecondary)
        secondary.text = getString(R.string.lose_menu)
        secondary.setOnClickListener {
            app.audio.play("button")
            app.music.stop()
            finish()
        }
        val tertiary = v.findViewById<Button>(R.id.btnTertiary)
        tertiary.visibility = View.GONE
    }

    private fun recreateLevel() {
        // Перезапуск уровня: пересоздаём движок через пересоздание активности.
        app.music.stop()
        startActivity(GameActivity.intent(this, levelNumber))
        finish()
    }

    private fun removeOverlay() {
        overlay?.let {
            (it.parent as? android.view.ViewGroup)?.removeView(it)
        }
        overlay = null
    }

    private fun fullScreenParams(): android.view.ViewGroup.LayoutParams =
        android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

    // =========================================================================
    // Колбэки движка
    // =========================================================================

    override fun onWin(level: Int, score: Int) {
        runOnUiThread { showWinOverlay(score) }
    }

    override fun onGameOver(level: Int, score: Int) {
        runOnUiThread { showLoseOverlay(score) }
    }

    companion object {
        private const val EXTRA_LEVEL = "level_number"

        fun intent(context: Context, levelNumber: Int): Intent =
            Intent(context, GameActivity::class.java).apply {
                putExtra(EXTRA_LEVEL, levelNumber)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
