package com.bombermama.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

/**
 * Процедурный 8-битный звуковой движок.
 *
 * Все звуки генерируются программно (синтез квадратных/треугольных волн
 * с огибающей) — поэтому нам не нужны аудиофайлы, а APK остаётся маленьким.
 * Это оригинальные звуки, а не музыка/эффекты Bomberman.
 *
 * Звуки синтезируются один раз в [preGenerate] и кешируются в памяти,
 * после чего воспроизводятся через AudioTrack без аллокаций в кадре.
 */
class AudioEngine {

    @Volatile
    private var enabled: Boolean = true

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) stopAll()
    }

    val isEnabled: Boolean get() = enabled

    private val sampleRate = 22050

    /** Кеш синтезированных звуков. */
    private val sounds: MutableMap<String, ShortArray> = HashMap()

    /** Звук сейчас играет (для предотвращения наложения тиков). */
    private val playing: MutableMap<String, AudioTrack> = HashMap()

    /**
     * Синтез всех звуков игры. Делается один раз при запуске.
     */
    fun preGenerate() {
        try {
            sounds["bomb_place"] = synthBlip(660f, 0.08, 0.35)
            sounds["tick"] = synthBlip(880f, 0.04, 0.18)
            sounds["explosion"] = synthExplosion()
            sounds["block_break"] = synthNoise(0.18, 0.35, 900.0)
            sounds["bonus"] = synthArpeggio(listOf(523f, 659f, 784f, 1047f), 0.06)
            sounds["enemy_kill"] = synthArpeggio(listOf(392f, 330f, 262f), 0.07)
            sounds["hurt"] = synthSweep(440f, 110f, 0.30)
            sounds["win"] = synthArpeggio(listOf(523f, 659f, 784f, 1047f, 1319f), 0.10)
            sounds["game_over"] = synthArpeggio(listOf(392f, 330f, 262f, 196f), 0.14)
            sounds["button"] = synthBlip(740f, 0.05, 0.25)
            sounds["pause"] = synthBlip(330f, 0.09, 0.3)
            sounds["life"] = synthArpeggio(listOf(784f, 988f, 1319f), 0.08)
        } catch (e: Throwable) {
            Log.w(TAG, "preGenerate failed", e)
        }
    }

    fun play(name: String, rate: Float = 1f) {
        if (!enabled) return
        val data = sounds[name] ?: return
        try {
            playPcm(data, rate, name)
        } catch (e: Throwable) {
            // Аудио не должно крашить игру.
            Log.w(TAG, "play($name) failed", e)
        }
    }

    /** Тик бомбы учащается по мере приближения взрыва. */
    fun playTick(pitch: Float) {
        play("tick", pitch.coerceIn(0.7f, 1.8f))
    }

    private fun playPcm(data: ShortArray, rate: Float, key: String) {
        stop(key)
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, data.size * 2 + 2048)
        val track = try {
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (e: Throwable) {
            Log.w(TAG, "AudioTrack create failed", e)
            return
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }
        try {
            track.write(data, 0, data.size)
        } catch (e: Throwable) {
            track.release()
            return
        }
        playing[key] = track
        track.setNotificationMarkerPosition(data.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                try {
                    t?.stop()
                    t?.release()
                } catch (_: Throwable) {
                }
                synchronized(playing) { playing.remove(key) }
            }

            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
        track.playbackParams = track.playbackParams.apply { pitch = rate.coerceIn(0.5f, 2.0f) }
        try {
            track.play()
        } catch (e: Throwable) {
            track.release()
            synchronized(playing) { playing.remove(key) }
        }
    }

    private fun stop(key: String) {
        synchronized(playing) {
            val t = playing.remove(key) ?: return
            try {
                t.stop()
            } catch (_: Throwable) {
            }
            try {
                t.release()
            } catch (_: Throwable) {
            }
        }
    }

    fun stopAll() {
        synchronized(playing) {
            val it = playing.values.iterator()
            while (it.hasNext()) {
                val t = it.next()
                try {
                    t.stop()
                } catch (_: Throwable) {
                }
                try {
                    t.release()
                } catch (_: Throwable) {
                }
                it.remove()
            }
        }
    }

    fun release() {
        stopAll()
        sounds.clear()
    }

    // =========================================================================
    // Синтез
    // =========================================================================

    private fun samples(durationSec: Double): Int = (sampleRate * durationSec).toInt()

    /** Короткий "бип" с экспоненциальной огибающей. */
    private fun synthBlip(freq: Float, duration: Double, peak: Double): ShortArray {
        val n = samples(duration)
        val out = ShortArray(n)
        var phase = 0.0
        val phaseInc = 2 * PI * freq / sampleRate
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = Math.exp(-t * 6.0) * peak
            out[i] = (square(phase) * env * Short.MAX_VALUE).toInt().toShort()
            phase += phaseInc
        }
        return out
    }

    /** Свип (глиссандо) — для получения урона. */
    private fun synthSweep(fromFreq: Float, toFreq: Float, duration: Double): ShortArray {
        val n = samples(duration)
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val f = fromFreq + (toFreq - fromFreq) * t
            val env = Math.exp(-t * 3.0) * 0.4
            out[i] = (square(phase) * env * Short.MAX_VALUE).toInt().toShort()
            phase += 2 * PI * f / sampleRate
        }
        return out
    }

    /** Взрыв: низкочастотный удар + шум. */
    private fun synthExplosion(): ShortArray {
        val duration = 0.55
        val n = samples(duration)
        val out = ShortArray(n)
        var phase = 0.0
        var noisePhase = 0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            // Низкий бум.
            val env = Math.exp(-t * 4.0)
            val boom = square(phase) * 0.5
            phase += 2 * PI * (90.0 - 60.0 * t) / sampleRate
            // Шумовой хвост.
            noisePhase = noisePhase * 1103515245 + 12345
            val noise = ((noisePhase ushr 16).toFloat() / 32768f - 0.5f) * (0.35 * (1.0 - t * 0.7))
            val v = (boom + noise) * env
            out[i] = (v.coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Цветной шум (для разрушения блоков). */
    private fun synthNoise(duration: Double, peak: Double, cutoff: Double): ShortArray {
        val n = samples(duration)
        val out = ShortArray(n)
        var noisePhase = 1
        var lp = 0.0
        val alpha = cutoff / sampleRate
        for (i in 0 until n) {
            val t = i.toDouble() / n
            noisePhase = noisePhase * 1103515245 + 12345
            val raw = (noisePhase ushr 16).toFloat() / 32768f - 0.5f
            lp += (raw - lp) * alpha
            val env = Math.exp(-t * 5.0) * peak
            out[i] = (lp * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Арпеджио из нот — для бонусов, победы, поражения. */
    private fun synthArpeggio(notes: List<Float>, noteDuration: Double): ShortArray {
        val per = samples(noteDuration)
        val out = ShortArray(per * notes.size)
        var phase = 0.0
        for ((idx, freq) in notes.withIndex()) {
            val phaseInc = 2 * PI * freq / sampleRate
            for (i in 0 until per) {
                val t = i.toDouble() / per
                val env = Math.exp(-t * 3.0) * 0.45
                val pos = idx * per + i
                if (pos < out.size) {
                    out[pos] = (square(phase) * env * Short.MAX_VALUE).toInt().toShort()
                }
                phase += phaseInc
            }
        }
        return out
    }

    private fun square(phase: Double): Double {
        val v = sin(phase)
        return if (v >= 0.0) 1.0 else -1.0
    }

    companion object {
        private const val TAG = "AudioEngine"
    }
}
