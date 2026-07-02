package com.drmacze.f16launcher

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.pow

/**
 * Synthesized elegant chime / shimmer sound effect.
 *
 * Generates a layered bell-like tone with ADSR envelope using additive synthesis:
 *  - Two higher harmonic partials for "shimmer"
 *  - Lower fundamental for body
 *  - Exponential decay envelope (bell-like)
 *  - Slight frequency sweep upward for "rising sparkle" feel
 *
 * No external audio file needed — pure math, ~0.9 second playback.
 *
 * Must be called from IO dispatcher (audio write is blocking).
 */
object SoundEffectHelper {

    private const val SAMPLE_RATE = 44100
    private const val DURATION_SEC = 1.1

    /**
     * Play the "shiny" chime — a layered bell tone with rising sweep.
     * Suspends until playback finishes.
     */
    suspend fun playShinyChime() = withContext(Dispatchers.IO) {
        try {
            val samples = generateShinyChime()
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Write PCM samples
            val pcm16 = ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE * 0.6f).toInt().toShort() }
            audioTrack.write(pcm16, 0, pcm16.size)
            audioTrack.setNotificationMarkerPosition(pcm16.size)
            audioTrack.play()

            // Wait for playback to finish (DURATION_SEC)
            kotlinx.coroutines.delay((DURATION_SEC * 1000).toLong() + 100)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Throwable) { /* silent fail — sound is non-critical */ }
    }

    /**
     * Generate the layered shimmer chime:
     *  - Fundamental 880Hz (A5) with bell envelope
     *  - 1760Hz (A6) overtone, lower amplitude, longer decay
     *  - 2640Hz (E7) sparkle partial, very short
     *  - Slight upward pitch sweep 880 → 920Hz over first 200ms
     *  - Master envelope: quick attack 20ms, exponential decay ~1.1s
     */
    private fun generateShinyChime(): FloatArray {
        val totalSamples = (SAMPLE_RATE * DURATION_SEC).toInt()
        val out = FloatArray(totalSamples)

        // Frequencies
        val f0 = 880.0   // fundamental (A5)
        val f1 = 1760.0  // octave (A6)
        val f2 = 2640.0  // sparkle (E7-ish)
        val sweepEnd = 200.0 / 1000.0  // 200ms sweep duration

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Pitch sweep (upward 880 → 920 in 200ms, then steady)
            val currentF0 = if (t < sweepEnd) {
                f0 + (920.0 - f0) * (t / sweepEnd)
            } else 920.0

            // Master envelope: attack 20ms, then exponential decay
            val attack = 0.02
            val env: Double = if (t < attack) {
                t / attack  // linear attack
            } else {
                exp(-(t - attack) * 3.5)  // exponential decay, ~1.1s total
            }

            // Layered oscillators
            val osc0 = sin(2 * PI * currentF0 * t)                          // body
            val osc1 = sin(2 * PI * f1 * t) * 0.5                           // octave
            val osc2 = sin(2 * PI * f2 * t) * 0.25 * exp(-t * 6.0)          // sparkle (short)

            // Soft tremolo for "shimmer" feel
            val tremolo = 1.0 + 0.05 * sin(2 * PI * 18.0 * t)

            // Final sample
            val sample = (osc0 + osc1 + osc2) * env * tremolo * 0.33
            out[i] = sample.toFloat().coerceIn(-1f, 1f)
        }
        return out
    }

    /** Play a softer "tick" sound for button presses (optional, lightweight). */
    suspend fun playSoftTick() = withContext(Dispatchers.IO) {
        try {
            val dur = 0.08
            val samples = (SAMPLE_RATE * dur).toInt()
            val out = FloatArray(samples)
            for (i in 0 until samples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 30.0)
                out[i] = (sin(2 * PI * 1200.0 * t) * env * 0.2).toFloat()
            }
            val pcm16 = ShortArray(out.size) { (out[it] * Short.MAX_VALUE).toInt().toShort() }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm16.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm16, 0, pcm16.size)
            track.play()
            kotlinx.coroutines.delay(120)
            track.stop()
            track.release()
        } catch (_: Throwable) { }
    }
}
