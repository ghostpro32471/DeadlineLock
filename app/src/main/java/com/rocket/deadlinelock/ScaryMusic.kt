package com.rocket.deadlinelock

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class ScaryMusic {
    @Volatile private var running = false
    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true
        worker = thread(name = "scary-music") { loop() }
    }

    fun stop() {
        running = false
        worker?.join(1000)
        worker = null
    }

    private fun loop() {
        val sampleRate = 22050
        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(bufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()
        val chunk = ShortArray(2048)
        var sampleIndex = 0L
        val twoPi = 2.0 * PI

        while (running) {
            for (i in chunk.indices) {
                val t = sampleIndex.toDouble() / sampleRate
                val drone = sin(twoPi * 55.0 * t) * 0.45 + sin(twoPi * 55.6 * t) * 0.45
                val pulse = if (sin(twoPi * 2.0 * t) > 0.85) 1.0 else 0.0
                val squeal = sin(twoPi * 1864.0 * t) * 0.10 * pulse
                val beatPhase = t % 2.0
                val thump = if (beatPhase < 0.12)
                    sin(twoPi * 40.0 * beatPhase) * (1.0 - beatPhase / 0.12) * 0.6 else 0.0
                val s = (drone + squeal + thump).coerceIn(-1.0, 1.0)
                chunk[i] = (s * Short.MAX_VALUE).toInt().toShort()
                sampleIndex++
            }
            track.write(chunk, 0, chunk.size)
        }
        try { track.stop() } catch (_: Exception) {}
        track.release()
    }
}
