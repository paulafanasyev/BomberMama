package com.bombermama.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.bombermama.render.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Праздничная анимация конфетти для экрана победы.
 *
 * Частицы живут в ограниченном пуле (без аллокаций в кадре),
 * падают вниз с лёгким вращением и покачиванием.
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var color: Int = 0,
        var size: Float = 0f,
        var rot: Float = 0f,
        var rotSpeed: Float = 0f,
        var sway: Float = 0f,
        var swayPhase: Float = 0f
    )

    private val colors = intArrayOf(
        Palette.CONFETTI_1, Palette.CONFETTI_2, Palette.CONFETTI_3,
        Palette.CONFETTI_4, Palette.CONFETTI_5
    )

    private val particles = Array(MAX_PARTICLES) { Particle() }
    private var activeCount = 0
    private val paint = Paint().apply { isAntiAlias = true }
    private val random = Random(System.currentTimeMillis())
    private var lastEmit = 0L
    @Volatile
    private var emitting: Boolean = true

    fun startBurst() {
        emitting = true
        activeCount = 0
        // Первый залп — со всей ширины.
        emitBurst(width.toFloat())
    }

    fun stopBurst() {
        emitting = false
    }

    private fun emitBurst(w: Float) {
        var n = 0
        for (i in 0 until MAX_PARTICLES) {
            if (n >= BURST_COUNT) break
            val p = particles[i]
            p.x = random.nextFloat() * w
            p.y = -20f
            p.vx = (random.nextFloat() - 0.5f) * 120f
            p.vy = random.nextFloat() * 160f + 60f
            p.color = colors[random.nextInt(colors.size)]
            p.size = 6f + random.nextFloat() * 8f
            p.rot = random.nextFloat() * 360f
            p.rotSpeed = (random.nextFloat() - 0.5f) * 360f
            p.sway = 20f + random.nextFloat() * 40f
            p.swayPhase = random.nextFloat() * 6.28f
            n++
        }
        activeCount = MAX_PARTICLES
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val now = System.currentTimeMillis()
        if (emitting && now - lastEmit > EMIT_INTERVAL_MS) {
            lastEmit = now
            // Подбрасываем новые частицы сверху.
            for (i in 0 until BURST_COUNT) {
                val p = particles[i]
                p.x = random.nextFloat() * width
                p.y = -20f
                p.vx = (random.nextFloat() - 0.5f) * 140f
                p.vy = random.nextFloat() * 180f + 80f
                p.color = colors[random.nextInt(colors.size)]
                p.size = 6f + random.nextFloat() * 8f
                p.rot = random.nextFloat() * 360f
                p.rotSpeed = (random.nextFloat() - 0.5f) * 400f
                p.sway = 20f + random.nextFloat() * 40f
                p.swayPhase = random.nextFloat() * 6.28f
            }
        }

        val dt = 0.016f
        canvas.save()
        for (i in 0 until MAX_PARTICLES) {
            val p = particles[i]
            if (p.y > height + 40f) continue
            p.x += p.vx * dt + cos(p.swayPhase) * p.sway * dt
            p.y += p.vy * dt
            p.vy += GRAVITY * dt
            p.rot += p.rotSpeed * dt
            p.swayPhase += dt * 3f

            paint.color = p.color
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rot)
            canvas.drawRect(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f, paint)
            canvas.restore()
        }
        canvas.restore()

        if (emitting || particles.any { it.y < height + 40f }) {
            invalidate()
        }
    }

    companion object {
        private const val MAX_PARTICLES = 140
        private const val BURST_COUNT = 26
        private const val GRAVITY = 240f
        private const val EMIT_INTERVAL_MS = 120L
    }
}
