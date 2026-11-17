package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

object VideoAudioHelper {

    fun mediaAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
    }

    fun requestMediaAudioFocus(context: Context): () -> Unit {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = runCatching {
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mediaAudioAttributes())
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener { /* handled by VideoView/AudioTrack */ }
                    .build()
            }.getOrNull() ?: return {}
            runCatching { audioManager.requestAudioFocus(request) }
            return { runCatching { audioManager.abandonAudioFocusRequest(request) } }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            @Suppress("DEPRECATION")
            return { runCatching { audioManager.abandonAudioFocus(null) } }
        }
    }

    /**
     * Generates a gentle, non-intrusive ambient chord pad for sample catalog videos
     * that do not yet carry a local file audio stream. Safely no-ops in JVM/Robolectric
     * environments where native AudioTrack initialization is unavailable.
     */
    suspend fun playAmbientSoundtrack(
        seedKey: String,
        playbackSpeed: Float
    ) = withContext(Dispatchers.IO) {
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return@withContext
        }

        val sampleRate = 22050
        val minBufBytes = runCatching {
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        }.getOrElse { -1 }

        if (minBufBytes <= 0) return@withContext

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return@withContext

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track.release() }
            return@withContext
        }

        // Choose a warm major/minor 7th chord root frequency based on video/short ID
        val rootFrequencies = doubleArrayOf(196.00, 220.00, 246.94, 261.63, 293.66, 329.63)
        val idx = abs(seedKey.hashCode()) % rootFrequencies.size
        val root = rootFrequencies[idx]
        val fifth = root * 1.498
        val seventh = root * 1.888

        val chunkSize = 1024
        val buffer = ShortArray(chunkSize)
        var phaseRoot = 0.0
        var phaseFifth = 0.0
        var phaseSeventh = 0.0
        var lfoPhase = 0.0
        var fadeGain = 0.0

        val speed = playbackSpeed.coerceIn(0.5f, 2.0f).toDouble()

        try {
            track.play()
            while (currentCoroutineContext().isActive) {
                for (i in 0 until chunkSize) {
                    fadeGain = (fadeGain + 0.00025).coerceAtMost(1.0)
                    val lfo = 0.65 + 0.35 * sin(lfoPhase)
                    val wave = (
                        0.50 * sin(phaseRoot) +
                            0.30 * sin(phaseFifth) +
                            0.20 * sin(phaseSeventh)
                        ) * lfo * fadeGain

                    // Comfortable ambient level (~14% max amplitude)
                    buffer[i] = (wave * 4400.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    phaseRoot += 2.0 * PI * root * speed / sampleRate
                    phaseFifth += 2.0 * PI * fifth * speed / sampleRate
                    phaseSeventh += 2.0 * PI * seventh * speed / sampleRate
                    lfoPhase += 2.0 * PI * 0.35 * speed / sampleRate

                    if (phaseRoot > 2.0 * PI) phaseRoot -= 2.0 * PI
                    if (phaseFifth > 2.0 * PI) phaseFifth -= 2.0 * PI
                    if (phaseSeventh > 2.0 * PI) phaseSeventh -= 2.0 * PI
                    if (lfoPhase > 2.0 * PI) lfoPhase -= 2.0 * PI
                }
                val writeStartNs = System.nanoTime()
                val written = track.write(buffer, 0, chunkSize)
                if (written < 0) break
                if (System.nanoTime() - writeStartNs < 5_000_000L) {
                    kotlinx.coroutines.delay(40L)
                }
            }
        } catch (_: Throwable) {
            // Safe guard for lifecycle interruption or host audio unavailability
        } finally {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
                track.stop()
            }
            runCatching { track.release() }
        }
    }
}
