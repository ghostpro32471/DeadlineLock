package com.rocket.deadlinelock

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

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
                // USAGE_ALARM — чтобы музыка играла даже на беззвучном режиме
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
                
                // 1. Тяжелый бас (низкая частота, медленно нарастает)
                val bassFreq = 30.0 + 10.0 * sin(twoPi * 0.1 * t) 
                val bass = sin(twoPi * bassFreq * t) * 0.6
                
                // 2. Резкие высокочастотные всплески (каждые 2 секунды)
                val beatPhase = t % 2.0
                val highSqueal = if (beatPhase < 0.1) {
                    sin(twoPi * 2000.0 * t) * abs(1.0 - beatPhase / 0.1) * 0.3
                } else 0.0

                // 3. Фоновый шум/шипение (эффект старой кассеты/помех)
                val noise = (Math.random() * 2 - 1) * 0.05

                val s = (bass + highSqueal + noise).coerceIn(-1.0, 1.0)
                chunk[i] = (s * Short.MAX_VALUE).toInt().toShort()
                sampleIndex++
            }
            track.write(chunk, 0, chunk.size)
        }
        try { track.stop() } catch (_: Exception) {}
        track.release()
    }
}
