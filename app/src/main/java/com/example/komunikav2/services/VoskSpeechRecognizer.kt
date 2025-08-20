package com.example.komunikav2.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException

object VoskSpeechRecognizer {
    private const val defaultModelFolder = "model-en-us"
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isLoading = false

    val isListening: Boolean
        get() = speechService != null

    fun startListening(context: Context, onText: (String) -> Unit) {
        if (speechService != null) return
        ensureModelLoaded(context) {
            try {
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(object : RecognitionListener {
                    override fun onResult(hypothesis: String?) {
                        deliver(hypothesis, onText)
                    }
                    override fun onPartialResult(hypothesis: String?) {
                        deliver(hypothesis, onText)
                    }
                    override fun onFinalResult(hypothesis: String?) {
                        deliver(hypothesis, onText)
                        stopListening()
                    }
                    override fun onError(e: Exception?) {
                        Log.e("Vosk", "error", e)
                        stopListening()
                    }
                    override fun onTimeout() {
                        stopListening()
                    }
                })
            } catch (e: IOException) {
                Log.e("Vosk", "start failed", e)
            }
        }
    }

    fun stopListening() {
        try {
            speechService?.stop()
        } catch (_: Exception) {}
        speechService = null
    }

    private fun deliver(hypothesis: String?, onText: (String) -> Unit) {
        if (hypothesis.isNullOrBlank()) return
        val text = try {
            val json = JSONObject(hypothesis)
            json.optString("text").trim()
        } catch (e: Exception) {
            Log.w("Vosk", "parse fail: $hypothesis", e)
            ""
        }
        if (text.isNotBlank()) onText(text)
    }

    private fun ensureModelLoaded(context: Context, onReady: () -> Unit) {
        if (model != null) {
            onReady()
            return
        }
        if (isLoading) return
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assetFolder = resolveModelFolder(context)
                val targetDir = File(context.filesDir, assetFolder)
                if (!targetDir.exists() || targetDir.list()?.isEmpty() != false) {
                    copyAssetDir(context, assetFolder, targetDir)
                }
                model = Model(targetDir.absolutePath)
                onReady()
            } catch (e: Exception) {
                Log.e("Vosk", "model load failed", e)
            } finally {
                isLoading = false
            }
        }
    }

    private fun resolveModelFolder(context: Context): String {
        val roots = context.assets.list("")?.toList().orEmpty()
        if (roots.contains(defaultModelFolder)) return defaultModelFolder
        val candidate = roots.firstOrNull { it.startsWith("model") || it.contains("vosk", ignoreCase = true) || it.contains("en", ignoreCase = true) }
        return candidate ?: defaultModelFolder
    }

    private fun copyAssetDir(context: Context, assetDirName: String, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val items = context.assets.list(assetDirName) ?: return
        for (item in items) {
            val assetPath = "$assetDirName/$item"
            val children = context.assets.list(assetPath)
            if (children?.isNotEmpty() == true) {
                copyAssetDir(context, assetPath, File(targetDir, item))
            } else {
                val outFile = File(targetDir, item)
                if (outFile.exists()) continue
                context.assets.open(assetPath).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}


