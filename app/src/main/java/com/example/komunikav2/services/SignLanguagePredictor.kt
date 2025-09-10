package com.example.komunikav2.services

import android.content.Context
import android.util.Log
import android.os.Build
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.io.IOException

class SignLanguagePredictor(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var currentModel: String? = null
    private var currentLabels: List<String> = emptyList()
    private var inputTensorShape: IntArray? = null
    private var outputTensorShape: IntArray? = null
    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: ByteBuffer? = null
    private var useNnapiIfAvailable: Boolean = false
    private val isDebug: Boolean by lazy {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    companion object {
        private const val TAG = "SignLanguagePredictor"
        private const val SEQUENCE_LENGTH = 30
        private const val NUM_KEYPOINTS = 126
        private const val FLOAT_SIZE = 4
        private const val MODEL_PATH_PREFIX = "models/"
        private const val LABELS_PATH_PREFIX = "labels/"
        
        private val MODEL_MAPPING = mapOf(
            "alphabets" to "alphabets.tflite",
            "calendar" to "calendar.tflite",
            "colors" to "colors.tflite",
            "family" to "family.tflite",
            "gender" to "gender.tflite", 
            "greetings" to "greetings.tflite",
            "numbers1-10" to "1_to_10.tflite",
            "numbers11-19" to "11_to_19.tflite",
            "numbers20-100" to "20_to_100.tflite",
            "people" to "people.tflite",
            "places" to "places.tflite",
            "questions" to "questions.tflite",
            "time" to "time.tflite",
            "pronouns" to "pronouns.tflite",
            "survival" to "survival.tflite",
            "verbs" to "verbs.tflite",
            "facial_expressions" to "facial_expression.tflite",
            "money_matters" to "money_matters.tflite",
            "food" to "food.tflite",
            "adjectives_and_adverbs" to "adjectives_adverbs.tflite"
        )
        
        private val AVAILABLE_CATEGORIES = setOf(
            "alphabets", "calendar", "colors", "family", "gender", "greetings", "numbers1-10", "numbers11-19", "numbers20-100",
            "people", "places", "questions", "time", "pronouns", "survival", "verbs", "facial_expressions", "money_matters", "food", "adjectives_and_adverbs"
        )
        
        private val LABELS_MAPPING = mapOf(
            "alphabets" to "alphabets_labels.txt",
            "calendar" to "calendar_labels.txt",
            "colors" to "colors_labels.txt",
            "family" to "family_labels.txt",
            "gender" to "gender_labels.txt",
            "greetings" to "greetings_labels.txt",
            "numbers1-10" to "numbers1-10_labels.txt",
            "numbers11-19" to "numbers11-19_labels.txt",
            "numbers20-100" to "numbers20-100_labels.txt",
            "people" to "people_labels.txt",
            "places" to "places_labels.txt",
            "questions" to "questions_labels.txt",
            "time" to "time_labels.txt",
            "pronouns" to "pronouns_labels.txt",
            "survival" to "survival_labels.txt",
            "verbs" to "verbs_labels.txt",
            "facial_expressions" to "facial_expression_labels.txt",
            "money_matters" to "money_matters_labels.txt",
            "food" to "food_labels.txt",
            "adjectives_and_adverbs" to "adjectives_adverbs_labels.txt"
        )
    }
    
    data class PredictionResult(
        val prediction: String,
        val confidence: Float,
        val category: String
    )
    
    suspend fun loadModel(category: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!AVAILABLE_CATEGORIES.contains(category)) {
                Log.w(TAG, "No model available for category: $category")
                return@withContext false
            }
            
            val modelFileName = MODEL_MAPPING[category]
            val labelsFileName = LABELS_MAPPING[category]
            
            if (modelFileName == null || labelsFileName == null) {
                Log.e(TAG, "No model or labels found for category: $category")
                return@withContext false
            }
            
            if (currentModel == category && interpreter != null) {
                Log.d(TAG, "Model already loaded for category: $category")
                return@withContext true
            }
            
            unloadModel()
            
            val modelPath = MODEL_PATH_PREFIX + modelFileName
            val labelsPath = LABELS_PATH_PREFIX + labelsFileName
            
            Log.d(TAG, "Loading model: $modelPath")
            Log.d(TAG, "Loading labels: $labelsPath")
            
            val modelBuffer = loadModelFile(modelPath)
            val labels = loadLabelsFile(labelsPath)
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            
            interpreter = try {
                Interpreter(modelBuffer, options)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create interpreter with XNNPACK, trying without: ${e.message}")
                val fallbackOptions = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                Interpreter(modelBuffer, fallbackOptions)
            }
            currentLabels = labels
            currentModel = category
            inputTensorShape = interpreter?.getInputTensor(0)?.shape()
            outputTensorShape = interpreter?.getOutputTensor(0)?.shape()

            val expectedInputSize = inputTensorShape?.fold(1) { acc, dim -> acc * dim } ?: 0
            val expectedOutputSize = outputTensorShape?.fold(1) { acc, dim -> acc * dim } ?: 0

            inputBuffer = ByteBuffer.allocateDirect(expectedInputSize * FLOAT_SIZE).apply {
                order(ByteOrder.nativeOrder())
            }
            outputBuffer = ByteBuffer.allocateDirect(expectedOutputSize * FLOAT_SIZE).apply {
                order(ByteOrder.nativeOrder())
            }
            
            if (isDebug) {
                Log.d(TAG, "Model loaded successfully for category: $category")
                Log.d(TAG, "Labels loaded: ${labels.size} labels")
                Log.d(TAG, "Input shape: ${inputTensorShape?.contentToString()}")
                Log.d(TAG, "Output shape: ${outputTensorShape?.contentToString()}")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model for category $category", e)
            false
        }
    }
    
    suspend fun predict(landmarks: List<List<Float>>): PredictionResult? = withContext(Dispatchers.IO) {
        try {
            val interpreter = interpreter ?: run {
                Log.e(TAG, "No model loaded")
                return@withContext null
            }
            
            if (currentLabels.isEmpty()) {
                Log.e(TAG, "No labels loaded")
                return@withContext null
            }
            
            if (isDebug) {
                Log.d(TAG, "Predicting with ${landmarks.size} frames, ${landmarks.firstOrNull()?.size ?: 0} landmarks per frame")
            }
            
            val inputShape = inputTensorShape ?: interpreter.getInputTensor(0).shape().also { inputTensorShape = it }
            val expectedInputSize = inputShape.fold(1) { acc, dim -> acc * dim }
            
            if (isDebug) {
                Log.d(TAG, "Model input shape: ${inputShape.contentToString()}, expected size: $expectedInputSize")
                Log.d(TAG, "Our data size: ${landmarks.size * (landmarks.firstOrNull()?.size ?: 0)}")
                Log.d(TAG, "Landmarks frame count: ${landmarks.size}")
                Log.d(TAG, "Landmarks per frame: ${landmarks.firstOrNull()?.size ?: 0}")
                Log.d(TAG, "Total coordinates: ${landmarks.flatten().size}")
            }
            
            val inputData = preprocessLandmarks(landmarks)
            
            val outputShape = outputTensorShape ?: interpreter.getOutputTensor(0).shape().also { outputTensorShape = it }
            val actualOutputSize = outputShape.fold(1) { acc, dim -> acc * dim }
            
            if (isDebug) {
                Log.d(TAG, "Model output shape: ${outputShape.contentToString()}, size: $actualOutputSize")
                Log.d(TAG, "Labels count: ${currentLabels.size}")
            }
            
            val outBuffer = outputBuffer?.also { it.clear() } ?: ByteBuffer.allocateDirect(actualOutputSize * FLOAT_SIZE).apply {
                order(ByteOrder.nativeOrder())
                outputBuffer = this
            }
            
            interpreter.run(inputData, outBuffer)
            
            val predictions = FloatArray(actualOutputSize)
            outBuffer.rewind()
            outBuffer.asFloatBuffer().get(predictions)
            
            // Use the minimum of actual output size and labels size for safety
            val validPredictionCount = minOf(actualOutputSize, currentLabels.size)
            val maxIndex = (0 until validPredictionCount).maxByOrNull { predictions[it] } ?: 0
            val confidence = predictions[maxIndex]
            val prediction = currentLabels.getOrNull(maxIndex) ?: "Unknown"
            
            if (isDebug) {
                Log.d(TAG, "Raw predictions (first 5): ${predictions.take(5).joinToString()}")
                Log.d(TAG, "Valid prediction count: $validPredictionCount")
                Log.d(TAG, "Max confidence index: $maxIndex")
                Log.d(TAG, "Prediction: $prediction (confidence: $confidence)")
            }
            
            PredictionResult(
                prediction = prediction,
                confidence = confidence,
                category = currentModel ?: "unknown"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during prediction", e)
            null
        }
    }
    
    private fun preprocessLandmarks(landmarks: List<List<Float>>): ByteBuffer {
        val buf = inputBuffer?.also { it.clear() } ?: ByteBuffer.allocateDirect(SEQUENCE_LENGTH * NUM_KEYPOINTS * FLOAT_SIZE).apply {
            order(ByteOrder.nativeOrder())
            inputBuffer = this
        }
        
        val paddedLandmarks = padOrTruncateSequence(landmarks)
        
        for (frame in paddedLandmarks) {
            for (coordinate in frame) {
                buf.putFloat(coordinate)
            }
        }
        
        buf.rewind()
        return buf
    }
    
    private fun padOrTruncateSequence(landmarks: List<List<Float>>): List<List<Float>> {
        return when {
            landmarks.size > SEQUENCE_LENGTH -> {
                landmarks.takeLast(SEQUENCE_LENGTH)
            }
            landmarks.size < SEQUENCE_LENGTH -> {
                val padding = List(SEQUENCE_LENGTH - landmarks.size) { 
                    List(NUM_KEYPOINTS) { 0.0f } 
                }
                padding + landmarks  // Zeros first, then data (exactly like Python reference)
            }
            else -> landmarks
        }
    }
    
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        return try {
            Log.d(TAG, "Loading model from assets: $modelPath")
            FileUtil.loadMappedFile(context, modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model file: $modelPath", e)
            throw e
        }
    }
    
    private fun loadLabelsFile(labelsPath: String): List<String> {
        return try {
            val labels = context.assets.open(labelsPath).bufferedReader().readLines()
            labels.filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading labels file: $labelsPath", e)
            emptyList()
        }
    }
    
    fun unloadModel() {
        try {
            interpreter?.close()
            interpreter = null
            currentModel = null
            currentLabels = emptyList()
            Log.d(TAG, "Model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
        }
    }
    
    fun isModelLoaded(): Boolean {
        return interpreter != null && currentModel != null
    }
    
    fun getCurrentCategory(): String? {
        return currentModel
    }
    
    fun getAvailableCategories(): List<String> {
        return AVAILABLE_CATEGORIES.toList()
    }
    
    fun isCategoryAvailable(category: String): Boolean {
        return AVAILABLE_CATEGORIES.contains(category)
    }

    fun setUseNnapiIfAvailable(enabled: Boolean) {
        useNnapiIfAvailable = enabled
    }
    
    fun setInferenceOptimization(optimization: InferenceOptimization) {
        when (optimization) {
            InferenceOptimization.SPEED -> {
                useNnapiIfAvailable = false
            }
            InferenceOptimization.BALANCED -> {
                useNnapiIfAvailable = false
            }
            InferenceOptimization.ACCURACY -> {
                useNnapiIfAvailable = false
            }
        }
        Log.d(TAG, "Inference optimization set to: $optimization")
    }
    
    enum class InferenceOptimization {
        SPEED, BALANCED, ACCURACY
    }
}
