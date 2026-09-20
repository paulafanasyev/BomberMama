package com.bombermama.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

/**
 * Оригинальный 8-битный саундтрек.
 *
 * Музыка генерируется процедурно: мелодия, бас и барабаны синтезируются
 * как волны и смешиваются в один PCM-буфер. Никаких внешних файлов.
 * Мелодия написана специально для BomberMama (не заимствована).
 */
class MusicEngine {

    @Volatile
    private var playing: Boolean = false
    private var track: AudioTrack? = null
    @Volatile
    private var enabled: Boolean = true

    private val sampleRate = 22050

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }

    val isEnabled: Boolean get() = enabled

    /** Темп мелодии: 1.0 = нормально, >1 быстрее. */
    private val tempo: Double = 1.0

    private data class Note(val freq: Float, val duration: Double, val volume: Double)

    // Главная мелодия (до-мажор, бодрая).
    // Ноты в Гц.
    private val melody: List<Note> = listOf(
        Note(523.25f, 0.22, 0.30), // C5
        Note(659.25f, 0.22, 0.30), // E5
        Note(783.99f, 0.22, 0.30), // G5
        Note(1046.5f, 0.22, 0.30), // C6
        Note(783.99f, 0.22, 0.30), // G5
        Note(659.25f, 0.22, 0.30), // E5
        Note(523.25f, 0.44, 0.30), // C5
        Note(0f, 0.22, 0.0),       // пауза
        Note(587.33f, 0.22, 0.30), // D5
        Note(698.46f, 0.22, 0.30), // F5
        Note(880.0f, 0.22, 0.30),  // A5
        Note(1174.7f, 0.22, 0.30), // D6
        Note(880.0f, 0.22, 0.30),  // A5
        Note(698.46f, 0.22, 0.30), // F5
        Note(587.33f, 0.44, 0.30), // D5
        Note(0f, 0.22, 0.0),       // пауза
        Note(523.25f, 0.22, 0.30), // C5
        Note(783.99f, 0.22, 0.30), // G5
        Note(1046.5f, 0.22, 0.30), // C6
        Note(1318.5f, 0.22, 0.30), // E6
        Note(1046.5f, 0.22, 0.30), // C6
        Note(783.99f, 0.22, 0.30), // G5
        Note(523.25f, 0.44, 0.30), // C5
        Note(0f, 0.22, 0.0),       // пауза
        Note(493.88f, 0.22, 0.30), // B4
        Note(587.33f, 0.22, 0.30), // D5
        Note(659.25f, 0.22, 0.30), // E5
        Note(783.99f, 0.22, 0.30), // G5
        Note(880.0f, 0.22, 0.30),  // A5
        Note(1046.5f, 0.22, 0.30), // C6
        Note(523.25f, 0.66, 0.32)  // C5 (каденция)
    )

    // Бас: тоника и доминанта.
    private val bass: List<Note> = listOf(
        Note(130.81f, 0.44, 0.32), // C3
        Note(130.81f, 0.44, 0.32),
        Note(196.0f, 0.44, 0.32),  // G3
        Note(196.0f, 0.44, 0.32),
        Note(130.81f, 0.44, 0.32), // C3
        Note(130.81f, 0.44, 0.32),
        Note(174.61f, 0.44, 0.32), // F3
        Note(196.0f, 0.44, 0.32)   // G3
    )

    private fun generateLoop(): ShortArray {
        val totalSec = melody.sumOf { it.duration } / tempo
        val n = (sampleRate * totalSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        var bassPhase = 0.0
        var melodyPos = 0
        var bassPos = 0
        var melodyLeft = (melody[0].duration / tempo * sampleRate).toInt()
        var bassLeft = (bass[0].duration / tempo * sampleRate).toInt()
        var currentMelody = melody[0]
        var currentBass = bass[0]
        var drumPhase = 0
        for (i in 0 until n) {
            // Мелодия: треугольная волна (мягче квадрата).
            val mVol = currentMelody.volume
            val mSample = if (mVol > 0.0) triangle(phase) * mVol else 0.0
            phase += 2 * PI * currentMelody.freq / sampleRate

            // Бас: квадрат.
            val bSample = if (currentBass.volume > 0.0) square(bassPhase) * currentBass.volume * 0.6 else 0.0
            bassPhase += 2 * PI * currentBass.freq / sampleRate

            // Барабаны на каждую долю.
            drumPhase++
            val drum = if (i % (sampleRate / 2) < 120) {
                drumNoise(i) * 0.18
            } else 0.0

            val v = mSample * 0.7 + bSample * 0.5 + drum
            out[i] = (v.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()

            melodyLeft--
            if (melodyLeft <= 0) {
                melodyPos = (melodyPos + 1) % melody.size
                currentMelody = melody[melodyPos]
                melodyLeft = (currentMelody.duration / tempo * sampleRate).toInt()
                phase = 0.0
            }
            bassLeft--
            if (bassLeft <= 0) {
                bassPos = (bassPos + 1) % bass.size
                currentBass = bass[bassPos]
                bassLeft = (currentBass.duration / tempo * sampleRate).toInt()
                bassPhase = 0.0
            }
        }
        return out
    }

    private var noiseState = 12345
    private fun drumNoise(i: Int): Double {
        noiseState = noiseState * 1103515245 + 12345 + i
        return ((noiseState ushr 16).toFloat() / 32768f - 0.5f).toDouble()
    }

    private fun square(phase: Double): Double = if (sin(phase) >= 0.0) 1.0 else -1.0
    private fun triangle(phase: Double): Double {
        val p = (phase / (2 * PI)) % 1.0
        return if (p < 0.5) (4 * p - 1.0) else (3.0 - 4 * p)
    }

    fun start() {
        if (!enabled || playing) return
        try {
            val loop = generateLoop()
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = maxOf(minBuf, loop.size * 2)
            val t = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufSize,
                AudioTrack.MODE_STATIC,
                0
            )
            if (t.state != AudioTrack.STATE_INITIALIZED) {
                t.release()
                return
            }
            t.write(loop, 0, loop.size)
            t.setLoopPoints(0, loop.size, -1)
            track = t
            playing = true
            t.play()
        } catch (e: Throwable) {
            Log.w(TAG, "music start failed", e)
        }
    }

    fun stop() {
        playing = false
        try {
            track?.let {
                it.stop()
                it.release()
            }
        } catch (_: Throwable) {
        }
        track = null
    }

    companion object {
        private const val TAG = "MusicEngine"
    }
}
