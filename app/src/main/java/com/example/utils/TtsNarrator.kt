package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsNarrator(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsNarrator", "Language is not supported or missing data. Falling back to US English.")
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
        } else {
            Log.e("TtsNarrator", "TTS Initialization failed!")
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("TtsNarrator", "TTS not fully initialized yet. Queueing speaking...")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VisionAiNarrationId")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
