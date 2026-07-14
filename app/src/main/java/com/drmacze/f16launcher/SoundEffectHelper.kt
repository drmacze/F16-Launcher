package com.drmacze.f16launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesized sound effects for DLavie launcher.
 *
 * Two flavours:
 *  - playShinyChime(context) → cinematic splash sound (low drone + soft pad + subtle sparkle).
 *  - playSoftTick()          → lightweight button-press tick.
 *
 * **Bug 5 fix:** `playShinyChime` sekarang pakai MediaPlayer + raw WAV resource
 * (`R.raw.splash_sound`) yang di-generate dari `scripts/gen_splash_wav.py`.
 * MediaPlayer jauh lebih reliable di semua device dibanding AudioTrack.MODE_STATIC
 * (yang sering silent-fail di Android 12+ karena buffer size validation).
 *
 * Fallback: kalau MediaPlayer.create() return null (mis. resource corrupt),
 * pakai AudioTrack MODE_STREAM sebagai backup. Kalau itu juga gagal, silent fail.
 */
object SoundEffectHelper {

    private const val TAG = "SoundEffectHelper"
    private const val SAMPLE_RATE = 44100
    private const val DURATION_SEC = 2.3  // cinematic duration (matches raw WAV length)
    private const val CINEMATIC_DURATION_SEC = 2.3

    /**
     * Cinematic splash sound — primary path pakai MediaPlayer + raw WAV.
     *
     * WAV file di `res/raw/splash_sound.wav` berisi cinematic sound yang sama
     * dengan [generateCinematicSound] (110Hz drone + 220Hz warmth + 440Hz pad
     * + 880Hz sparkle, master envelope + lowpass filter, total 2.3s).
     *
     * Keunggulan MediaPlayer:
     *  - Reliable di semua Android version (minSdk 24+).
     *  - Tidak peduli buffer size, audio routing, atau DOZE mode.
     *  - Built-in volume & focus handling.
     *
     * Fallback: kalau MediaPlayer null, coba AudioTrack MODE_STREAM dengan
     * generated samples (chunked write untuk reliability).
     *
     * Suspends until playback finishes (atau timeout 3s).
     */
    suspend fun playShinyChime(context: Context) = withContext(Dispatchers.IO) {
        // ── Primary path: MediaPlayer + raw WAV ──
        var played = false
        var mp: MediaPlayer? = null
        try {
            mp = MediaPlayer.create(context, R.raw.splash_sound)
            if (mp != null) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setOnCompletionListener { it.release() }
                mp.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what extra=$extra")
                    false  // let OnCompletionListener handle release
                }
                mp.start()
                played = true
                // Wait for playback to finish (WAV duration ~2.3s) + 200ms buffer.
                delay((CINEMATIC_DURATION_SEC * 1000).toLong() + 200)
            } else {
                Log.w(TAG, "MediaPlayer.create returned null — falling back to AudioTrack")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "MediaPlayer failed: ${e.message} — falling back to AudioTrack")
            try { mp?.release() } catch (_: Throwable) {}
        }

        // ── Fallback: AudioTrack MODE_STREAM (chunked write) ──
        if (!played) {
            try {
                playShinyChimeAudioTrack()
            } catch (e: Throwable) {
                Log.w(TAG, "AudioTrack fallback also failed: ${e.message}")
            }
        }
    }

    /**
     * Legacy no-arg entry point — DEPRECATED.
     * Hanya dipanggil dari code lama yang tidak punya Context. No-op + log warning.
     *
     * Caller sebaiknya update ke `playShinyChime(context: Context)`.
     */
    suspend fun playShinyChime() {
        Log.w(TAG, "playShinyChime() called without Context — no-op. Use playShinyChime(context) instead.")
    }

    /**
     * AudioTrack fallback: pakai MODE_STREAM + chunked write (lebih reliable
     * dari MODE_STATIC yang sering silent-fail di Android 12+).
     *
     * Generate cinematic samples on-the-fly, write dalam chunk 1 detik.
     */
    private suspend fun playShinyChimeAudioTrack() {
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
            .setBufferSizeInBytes(SAMPLE_RATE)  // small buffer for streaming (~1s)
            .setTransferMode(AudioTrack.MODE_STREAM)  // STREAM, not STATIC
            .build()

        audioTrack.play()

        // Halve amplitude to avoid clipping from layered sum.
        val pcm16 = ShortArray(samples.size) {
            (samples[it] * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        var written = 0
        val chunkSize = SAMPLE_RATE  // 1 second chunks
        while (written < pcm16.size) {
            val toWrite = minOf(chunkSize, pcm16.size - written)
            audioTrack.write(pcm16, written, toWrite)
            written += toWrite
        }

        // Wait for playback to finish.
        delay((CINEMATIC_DURATION_SEC * 1000).toLong() + 200)
        try { audioTrack.stop() } catch (_: Throwable) {}
        try { audioTrack.release() } catch (_: Throwable) {}
    }

    /**
     * Generate the cinematic sound:
     *  - Drone (110Hz) — main body, slight detune for warmth
     *  - Warmth (220Hz) — adds body
     *  - Pad (440Hz) — soft, slow vibrato
     *  - Sparkle (880Hz) — only in first 1 second, very low amplitude
     *  - Master envelope: attack 300ms (quadratic), sustain 500ms, exponential release 1500ms
     *  - One-pole lowpass filter for warmer overall tone
     *
     * Identik dengan `scripts/gen_splash_wav.py` (raw WAV resource).
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
            delay(120)
            track.stop()
            track.release()
        } catch (e: Throwable) {
            Log.w(TAG, "playSoftTick failed: ${e.message}")
        }
    }
}
