package io.dodge.android.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.dodge.audio.SoundEffect
import kotlin.math.*

class SynthAudioEngine {
    private val sampleRate = 44100
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(4)

    fun play(sfx: SoundEffect, comboN: Int = 0) {
        when (sfx) {
            SoundEffect.NEAR_MISS -> {
                playTone(880f, 0.1f, WaveForm.SINE, 0.12f)
                playTone(1320f, 0.08f, WaveForm.SINE, 0.08f)
            }
            SoundEffect.COMBO -> {
                playTone(660f + comboN * 80f, 0.15f, WaveForm.TRIANGLE, 0.12f)
            }
            SoundEffect.POWER_UP -> {
                playTone(523f, 0.1f, WaveForm.SINE, 0.15f)
                executor.execute {
                    Thread.sleep(80)
                    playTone(784f, 0.1f, WaveForm.SINE, 0.15f)
                }
                executor.execute {
                    Thread.sleep(160)
                    playTone(1047f, 0.15f, WaveForm.SINE, 0.12f)
                }
            }
            SoundEffect.SHIELD_BREAK -> {
                playNoise(0.3f, 0.2f)
                playTone(220f, 0.3f, WaveForm.SAWTOOTH, 0.1f)
            }
            SoundEffect.DEATH -> {
                playNoise(0.5f, 0.25f)
                playTone(150f, 0.4f, WaveForm.SAWTOOTH, 0.15f)
                executor.execute {
                    Thread.sleep(200)
                    playTone(100f, 0.5f, WaveForm.SAWTOOTH, 0.12f)
                }
            }
            SoundEffect.BOMBER_EXPLODE -> {
                playNoise(0.2f, 0.15f)
                playTone(80f, 0.2f, WaveForm.SQUARE, 0.08f)
            }
            SoundEffect.LASER_CHARGE -> {
                playTone(200f, 0.5f, WaveForm.SAWTOOTH, 0.05f)
            }
            SoundEffect.LASER_FIRE -> {
                playNoise(0.15f, 0.12f)
                playTone(100f, 0.2f, WaveForm.SQUARE, 0.1f)
            }
            SoundEffect.TELEPORT -> {
                playTone(1200f, 0.08f, WaveForm.SINE, 0.1f)
                playTone(600f, 0.1f, WaveForm.SINE, 0.08f)
            }
            SoundEffect.SPIRAL_SPAWN -> {
                playTone(440f, 0.15f, WaveForm.TRIANGLE, 0.06f)
            }
            SoundEffect.SPLIT_ENEMY -> {
                playTone(660f, 0.1f, WaveForm.SQUARE, 0.08f)
                playTone(990f, 0.08f, WaveForm.SQUARE, 0.06f)
            }
        }
    }

    private fun playTone(freq: Float, duration: Float, waveform: WaveForm, volume: Float) {
        executor.execute {
            try {
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val phase = (2.0 * PI * freq * t).toFloat()

                    val sample = when (waveform) {
                        WaveForm.SINE -> sin(phase)
                        WaveForm.TRIANGLE -> (2f / PI.toFloat()) * asin(sin(phase))
                        WaveForm.SQUARE -> if (sin(phase) >= 0) 1f else -1f
                        WaveForm.SAWTOOTH -> 2f * (t * freq - floor(t * freq + 0.5f))
                    }

                    // Exponential decay envelope
                    val envelope = volume * exp(-3f * t / duration).coerceAtLeast(0.001f)
                    buffer[i] = (sample * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    private fun playNoise(duration: Float, volume: Float) {
        executor.execute {
            try {
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                val random = java.util.Random()

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val noise = random.nextFloat() * 2f - 1f
                    val envelope = volume * exp(-3f * t / duration).coerceAtLeast(0.001f)
                    buffer[i] = (noise * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        val bufferSize = buffer.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()

        // Schedule cleanup after playback
        val durationMs = (buffer.size * 1000L) / sampleRate + 100
        executor.execute {
            Thread.sleep(durationMs)
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun shutdown() {
        executor.shutdown()
    }

    enum class WaveForm {
        SINE, TRIANGLE, SQUARE, SAWTOOTH
    }
}
