package com.rocket.deadlinelock

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

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

        // Ноты для зловещей мелодии (низкие частоты)
        // C2, D#2, F#2, G2
        val notes = doubleArrayOf(65.41, 77.78, 92.50, 98.00) 
        var currentNote = 0
        var noteTimer = 0.0

        while (running) {
            for (i in chunk.indices) {
                val t = sampleIndex.toDouble() / sampleRate
                
                // Смена ноты каждые 1.5 секунды
                noteTimer += 1.0 / sampleRate
                if (noteTimer > 1.5) {
                    noteTimer = 0.0
                    currentNote = (currentNote + 1) % notes.size
                }

                val freq = notes[currentNote]
                
                // Основной тон (мелодия)
                val tone = sin(twoPi * freq * t)
                
                // Гармоника для "металлического" звука
                val harm = sin(twoPi * freq * 2.0 * t) * 0.5
                
                // Затухание (эффект эха)
                val envelope = exp(-noteTimer * 1.5)

                // Фоновый гул (очень низкий)
                val drone = sin(twoPi * 30.0 * t) * 0.4

                // Периодический громкий удар (каждые 3 секунды)
                val beat = if (t % 3.0 < 0.15) sin(twoPi * 40.0 * t) * 0.8 else 0.0

                val s = (drone + (tone + harm) * envelope * 0.5 + beat).coerceIn(-1.0, 1.0)
                chunk[i] = (s * Short.MAX_VALUE).toInt().toShort()
                sampleIndex++
            }
            track.write(chunk, 0, chunk.size)
        }
        try { track.stop() } catch (_: Exception) {}
        track.release()
    }
}
