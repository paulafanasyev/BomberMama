package com.bombermama.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bombermama.BomberMamaApp
import com.bombermama.R
import com.bombermama.core.Levels
import com.bombermama.game.GameActivity

/**
 * Экран выбора уровня. Открытые уровни можно проходить в любом порядке.
 */
class LevelsActivity : AppCompatActivity() {

    private val app: BomberMamaApp
        get() = application as BomberMamaApp

    private lateinit var adapter: LevelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        app.audio.setEnabled(app.progress.soundEnabled)

        adapter = LevelsAdapter(
            maxUnlocked = app.progress.maxUnlockedLevel,
            bestScoreFor = { app.progress.bestScoreForLevel(it) }
        ) { level ->
            app.audio.play("button")
            startActivity(GameActivity.intent(this, level))
        }

        val rv = findViewById<RecyclerView>(R.id.levelsGrid)
        rv.layoutManager = GridLayoutManager(this, 4)
        rv.adapter = adapter

        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener {
            app.audio.play("button")
            finish()
        }
    }
}

/**
 * Адаптер сетки уровней.
 * Закрытые уровни — серые с замком, пройденные — с золотой рамкой и звёздами.
 */
class LevelsAdapter(
    private val maxUnlocked: Int,
    private val bestScoreFor: (Int) -> Int,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<LevelsAdapter.VH>() {

    class VH(val text: TextView) : RecyclerView.ViewHolder(text)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level, parent, false) as TextView
        return VH(tv)
    }

    override fun getItemCount(): Int = Levels.count

    override fun onBindViewHolder(holder: VH, position: Int) {
        val level = position + 1
        val unlocked = level <= maxUnlocked
        val score = bestScoreFor(level)
        val ctx = holder.text.context

        holder.text.text = if (unlocked) {
            if (score > 0) "$level ★" else "$level"
        } else {
            ctx.getString(R.string.levels_locked)
        }
        holder.text.isEnabled = unlocked
        holder.text.setBackgroundResource(
            when {
                !unlocked -> R.drawable.cell_locked
                score > 0 -> R.drawable.cell_completed
                else -> R.drawable.cell_available
            }
        )
        holder.text.setOnClickListener {
            if (unlocked) onClick(level)
        }
        holder.text.contentDescription = ctx.getString(R.string.level_select_desc, level)
    }
}
