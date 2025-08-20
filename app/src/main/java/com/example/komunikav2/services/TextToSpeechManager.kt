package com.example.komunikav2.services

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TextToSpeechManager {
	private var tts: TextToSpeech? = null
	var isInitialized: Boolean = false
		private set

	fun initialize(context: Context, locale: Locale = Locale.getDefault(), onReady: (() -> Unit)? = null) {
		if (tts != null) return
		tts = TextToSpeech(context.applicationContext) { status ->
			if (status == TextToSpeech.SUCCESS) {
				tts?.language = locale
				isInitialized = true
				onReady?.let { it() }
			}
		}
	}

	fun speak(text: String) {
		if (!isInitialized) return
		tts?.stop()
		tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
	}

	fun stop() {
		tts?.stop()
	}

	fun shutdown() {
		isInitialized = false
		tts?.stop()
		tts?.shutdown()
		tts = null
	}
}
