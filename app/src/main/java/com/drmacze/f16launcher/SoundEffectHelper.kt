package com.drmacze.f16launcher

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

/**
 * Synthesized sound effects for DLavie launcher.
 *
 * Two flavours:
 *  - playShinyChime() → cinematic splash sound (low drone + soft pad + subtle sparkle).
 *  - playSoftTick()   → lightweight button-press tick.
 *
 * Pure math, no external audio file needed. Must be called from IO dispatcher.
 */
object SoundEffectHelper {

    private const val SAMPLE_RATE = 44100
    private const val DURATION_SEC = 2.3  // cinematic duration (was 1.1 for old chime)

    /**
     * Cinematic splash sound — low drone + soft pad + subtle sparkle.
     * Lebih elegant & tidak annoying dibanding chime lama (880/1760/2640Hz).
     *
     * Layers:
     *  - 110Hz (A2) drone — body, slow attack, with slight detune for warmth
     *  - 220Hz (A3) — warmth
     *  - 440Hz (A4) — soft pad, slow vibrato
     *  - 880Hz (A5) — subtle sparkle, very low amplitude, short (first 1 second only)
     *
     * Envelope: slow attack 300ms, sustain 500ms, long release 1500ms.
     * Simple one-pole lowpass filter for warmer sound.
     * Total: ~2.3 seconds.
     *
     * Suspends until playback finishes.
     */
    suspend fun playShinyChime() = withContext(Dispatchers.IO) {
        try {
            val samples = generateCinematicSound()
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

            // Write PCM samples (halve amplitude to avoid clipping from layered sum)
            val pcm16 = ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE * 0.5f).toInt().toShort() }
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
     * Generate the cinematic sound:
     *  - Drone (110Hz) — main body, slight detune for warmth
     *  - Warmth (220Hz) — adds body
     *  - Pad (440Hz) — soft, slow vibrato
     *  - Sparkle (880Hz) — only in first 1 second, very low amplitude
     *  - Master envelope: attack 300ms (quadratic), sustain 500ms, exponential release 1500ms
     *  - One-pole lowpass filter for warmer overall tone
     */
    private fun generateCinematicSound(): FloatArray {
        val totalSamples = (SAMPLE_RATE * DURATION_SEC).toInt()
        val out = FloatArray(totalSamples)

        // Frequencies (low = cinematic, high = sparkle)
        val droneFreq = 110.0   // A2 — body
        val warmthFreq = 220.0  // A3 — warmth
        val padFreq = 440.0     // A4 — soft pad
        val sparkleFreq = 880.0 // A5 — subtle sparkle

        // Envelope timing
        val attackEnd = 0.3     // 300ms attack
        val sustainEnd = 0.8    // 500ms sustain
        // Release: 0.8 to 2.3 (1500ms)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Master envelope: attack-sustain-release
            val env: Double = when {
                t < attackEnd -> {
                    val p = t / attackEnd
                    p * p  // quadratic attack
                }
                t < sustainEnd -> 1.0  // sustain
                else -> {
                    val releaseProgress = (t - sustainEnd) / (DURATION_SEC - sustainEnd)
                    exp(-releaseProgress * 3.0)  // exponential release
                }
            }

            // Layer 1: Drone (110Hz) — main body, slight detune for warmth
            val drone = sin(2 * PI * droneFreq * t) * 0.5
            val droneDetune = sin(2 * PI * droneFreq * 1.005 * t) * 0.3  // slight detune

            // Layer 2: Warmth (220Hz) — adds body
            val warmth = sin(2 * PI * warmthFreq * t) * 0.25

            // Layer 3: Pad (440Hz) — soft, slow vibrato
            val vibrato = 1.0 + 0.005 * sin(2 * PI * 4.0 * t)  // 4Hz vibrato
            val pad = sin(2 * PI * padFreq * t * vibrato) * 0.15

            // Layer 4: Sparkle (880Hz) — only in first 1 second, very low amplitude
            val sparkleEnv = if (t < 1.0) exp(-t * 2.0) else 0.0
            val sparkle = sin(2 * PI * sparkleFreq * t) * 0.08 * sparkleEnv

            // Sum & apply envelope (master gain 0.3 to leave headroom)
            val sample = (drone + droneDetune + warmth + pad + sparkle) * env * 0.3
            out[i] = sample.toFloat().coerceIn(-1f, 1f)
        }

        // Simple one-pole lowpass filter — smooth out high frequencies for warmer sound
        val filtered = FloatArray(totalSamples)
        var prev = 0.0
        val filterCoeff = 0.85  // higher = smoother (more lowpass)
        for (i in 0 until totalSamples) {
            prev = prev * filterCoeff + out[i].toDouble() * (1.0 - filterCoeff)
            filtered[i] = prev.toFloat().coerceIn(-1f, 1f)
        }

        return filtered
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
