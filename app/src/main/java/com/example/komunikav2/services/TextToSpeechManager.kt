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
				// Set slower speech rate for better understanding
				tts?.setSpeechRate(0.7f) // 0.7 = 70% of normal speed (slower)
				isInitialized = true
				onReady?.let { it() }
			}
		}
	}

	fun speak(text: String) {
		if (!isInitialized) return
		tts?.stop()
		// Replace underscores and hyphens with spaces for better speech
		val cleanText = text.replace("_", " ").replace("-", " ")
		tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
	}

	fun setSpeechRate(rate: Float) {
		if (isInitialized) {
			tts?.setSpeechRate(rate)
		}
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
