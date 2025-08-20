package com.example.komunikav2.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import android.util.Log

class HandLandmarkerService(
    private val context: Context,
    private var isFrontCamera: Boolean = true
) {
    
    private var handLandmarker: HandLandmarker? = null
    private var signLanguagePredictor: SignLanguagePredictor? = null
    private var isReleased = false
    private val _handLandmarks = MutableStateFlow<List<HandLandmarkerResult>>(emptyList())
    val handLandmarks: StateFlow<List<HandLandmarkerResult>> = _handLandmarks.asStateFlow()
    
    private val _isHandDetected = MutableStateFlow(false)
    val isHandDetected: StateFlow<Boolean> = _isHandDetected.asStateFlow()
    
    private val _handBoundingBox = MutableStateFlow<HandBoundingBox?>(null)
    val handBoundingBox: StateFlow<HandBoundingBox?> = _handBoundingBox.asStateFlow()
    
    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()
    
    private val _prediction = MutableStateFlow("")
    val prediction: StateFlow<String> = _prediction.asStateFlow()
    
    private var lastProcessTime = 0L
    private val minProcessInterval = 33L
    private var landmarkBuffer: MutableList<List<Float>> = mutableListOf()
    private var currentCategory: String? = null
    private var isPredicting = false
    private var lastPredictionTime = 0L
    private val minPredictionInterval = 2000L // 2 seconds between predictions
    
    // Sentence building components
    private val predictionHistory = mutableListOf<String>()
    private val sentenceBuilder = StringBuilder()
    private var handLandmarkerListener: LandmarkerListener? = null
    
    companion object {
        private const val BUFFER_SIZE = 30
        private const val PREDICTION_THRESHOLD = 0.3f
    }
    
    interface LandmarkerListener {
        fun onResults(resultBundle: ResultBundle)
        fun onError(error: String, errorCode: Int)
        fun onPrediction(prediction: String)
    }
    
    data class ResultBundle(
        val results: HandLandmarkerResult,
        val inputImageHeight: Int,
        val inputImageWidth: Int
    )
    
    data class HandBoundingBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    init {
        setupHandLandmarker()
        signLanguagePredictor = SignLanguagePredictor(context)
    }
    
    fun setListener(listener: LandmarkerListener?) {
        handLandmarkerListener = listener
    }
    
    fun clearPrediction() {
        predictionHistory.clear()
        sentenceBuilder.clear()
        landmarkBuffer.clear()
        isPredicting = false
        _prediction.value = ""
        Log.d("HandLandmarkerService", "Prediction cleared - history, sentence, buffer and state reset")
    }
    
    fun removeLastPrediction() {
        if (predictionHistory.isNotEmpty()) {
            val removedPrediction = predictionHistory.removeLastOrNull()
            
            // Rebuild sentence without the last prediction
            sentenceBuilder.clear()
            if (predictionHistory.isNotEmpty()) {
                sentenceBuilder.append(predictionHistory.joinToString(" "))
            }
            
            val currentSentence = sentenceBuilder.toString().trim()
            
            // Force update the StateFlow even if empty
            _prediction.value = currentSentence
            
            Log.d("HandLandmarkerService", "Removed last prediction: '$removedPrediction'")
            Log.d("HandLandmarkerService", "Updated sentence: '$currentSentence'")
            Log.d("HandLandmarkerService", "Prediction StateFlow updated to: '${_prediction.value}'")
        } else {
            Log.d("HandLandmarkerService", "No predictions to remove")
            // Ensure StateFlow is empty
            _prediction.value = ""
        }
        
        // Clear buffer and reset state to prevent continuing with old data
        landmarkBuffer.clear()
        isPredicting = false
        
        // Reset prediction timing to allow immediate new predictions
        lastPredictionTime = 0L
    }
    
    fun getCurrentSentence(): String {
        return sentenceBuilder.toString().trim()
    }
    
    fun getPredictionHistory(): List<String> {
        return predictionHistory.toList()
    }
    
    fun rebuildSentenceFromHistory(history: List<String>) {
        predictionHistory.clear()
        sentenceBuilder.clear()
        
        predictionHistory.addAll(history)
        sentenceBuilder.append(history.joinToString(" "))
        
        val currentSentence = sentenceBuilder.toString().trim()
        _prediction.value = currentSentence
        
        Log.d("HandLandmarkerService", "Rebuilt sentence from history: $currentSentence")
    }
    
    private fun setupHandLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath("hand_landmarker.task")
                .build()
            
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.7f)
                .setMinTrackingConfidence(0.7f)
                .setMinHandPresenceConfidence(0.7f)
                .setNumHands(2)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, image ->
                    processHandLandmarkerResult(result, image)
                }
                .setErrorListener { error ->
                    error.printStackTrace()
                }
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun processHandLandmarkerResult(
        result: HandLandmarkerResult,
        image: MPImage
    ) {
        _handLandmarks.value = listOf(result)
        _isHandDetected.value = result.landmarks().isNotEmpty()
        
        Log.d("HandLandmarkerService", "Hand detection result: ${result.landmarks().size} hands detected")
        
        // Notify listener about results
        handLandmarkerListener?.onResults(ResultBundle(result, image.height, image.width))
        
        if (result.landmarks().isNotEmpty()) {
            calculateBoundingBox(result, image.width, image.height)
            Log.d("HandLandmarkerService", "Processing landmarks for prediction")
            
            // Combine all detected hands into single landmark list
            val allLandmarks = result.landmarks().flatMap { it }
            processLandmarksForPrediction(allLandmarks)
        } else {
            _handBoundingBox.value = null
            Log.d("HandLandmarkerService", "No hands detected")
        }
    }
    
    private fun calculateBoundingBox(
        result: HandLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int
    ) {
        if (result.landmarks().isEmpty()) return
        
        val landmarks = result.landmarks()[0] // First hand
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        for (landmark in landmarks) {
            minX = minOf(minX, landmark.x())
            minY = minOf(minY, landmark.y())
            maxX = maxOf(maxX, landmark.x())
            maxY = maxOf(maxY, landmark.y())
        }
        
        // Convert to pixel coordinates
        val left = minX * imageWidth
        val top = minY * imageHeight
        val right = maxX * imageWidth
        val bottom = maxY * imageHeight
        
        _handBoundingBox.value = HandBoundingBox(left, top, right, bottom)
    }
    
    fun createImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(
                    java.util.concurrent.Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    detectLiveStream(imageProxy, isFrontCamera)
                }
            }
    }
    
    fun setCameraType(isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
    }
    

    
    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        try {
            val landmarker = handLandmarker
            if (landmarker == null) {
                Log.w("HandLandmarkerService", "HandLandmarker is null in detectAsync")
                return
            }
            
            // Additional validation for MPImage
            if (mpImage.width <= 0 || mpImage.height <= 0) {
                Log.w("HandLandmarkerService", "Invalid MPImage dimensions: ${mpImage.width}x${mpImage.height}")
                return
            }
            
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e("HandLandmarkerService", "Error in detectAsync", e)
        }
    }
    
    fun loadModelsAndLabels(category: String) {
        currentCategory = category
        landmarkBuffer.clear()
        isPredicting = false
        lastPredictionTime = 0L // Reset prediction cooldown for new category
        
        // Only clear the landmark buffer, preserve the built sentence
        // clearPrediction() - removed to keep existing sentence
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isAvailable = signLanguagePredictor?.isCategoryAvailable(category) ?: false
                if (!isAvailable) {
                    handLandmarkerListener?.onError("Model not available for $category", 2)
                    return@launch
                }
                
                val success = signLanguagePredictor?.loadModel(category) ?: false
                if (success) {
                    Log.d("HandLandmarkerService", "Model loaded successfully for $category")
                } else {
                    handLandmarkerListener?.onError("Failed to load model for $category", 3)
                }
            } catch (e: Exception) {
                handLandmarkerListener?.onError("Error loading model for $category: ${e.message}", 4)
            }
        }
    }
    
    private fun processLandmarksForPrediction(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        if (currentCategory == null || signLanguagePredictor?.isModelLoaded() != true) {
            Log.d("HandLandmarkerService", "Skipping prediction: category=$currentCategory, modelLoaded=${signLanguagePredictor?.isModelLoaded()}")
            return
        }
        
        // Process landmarks for proper two-hand format
        val landmarkData = processLandmarksForTwoHands(landmarks)
        
        landmarkBuffer.add(landmarkData)
        Log.d("HandLandmarkerService", "Added landmark frame with ${landmarkData.size} coordinates. Buffer size: ${landmarkBuffer.size}/$BUFFER_SIZE")
        
        // Only predict when buffer is exactly full
        if (landmarkBuffer.size == BUFFER_SIZE && !isPredicting) {
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime - lastPredictionTime >= minPredictionInterval) {
                Log.d("HandLandmarkerService", "Buffer full, triggering prediction")
                lastPredictionTime = currentTime
                makePrediction()
                // Clear buffer after prediction for fresh start
                landmarkBuffer.clear()
            } else {
                val timeRemaining = minPredictionInterval - (currentTime - lastPredictionTime)
                Log.d("HandLandmarkerService", "Prediction cooldown active, ${timeRemaining}ms remaining")
            }
        }
    }
    
    private fun processLandmarksForTwoHands(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): List<Float> {
        // Check if we have enough landmarks for two hands (42 landmarks = 21 per hand)
        val hasTwoHands = landmarks.size >= 42
        
        if (hasTwoHands) {
            // Extract landmarks for both hands
            val hand1Landmarks = landmarks.take(21)
            val hand2Landmarks = landmarks.drop(21).take(21)
            
            val hand1Data = hand1Landmarks.flatMap { landmark ->
                listOf(landmark.x(), landmark.y(), landmark.z())
            }
            val hand2Data = hand2Landmarks.flatMap { landmark ->
                listOf(landmark.x(), landmark.y(), landmark.z())
            }
            
            Log.d("HandLandmarkerService", "Processing two hands: ${hand1Landmarks.size} + ${hand2Landmarks.size} landmarks")
            return hand1Data + hand2Data
        } else {
            // Single hand - pad with zeros for second hand
            val hand1Data = landmarks.flatMap { landmark ->
                listOf(landmark.x(), landmark.y(), landmark.z())
            }
            val hand2Data = List(63) { 0.0f }
            
            Log.d("HandLandmarkerService", "Processing single hand: ${landmarks.size} landmarks, padding second hand")
            return hand1Data + hand2Data
        }
    }
    
    private fun makePrediction() {
        if (isPredicting) return
        
        isPredicting = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("HandLandmarkerService", "Making prediction with ${landmarkBuffer.size} frames")
                val result = signLanguagePredictor?.predict(landmarkBuffer.toList())
                Log.d("HandLandmarkerService", "Prediction result: $result")
                
                if (result != null) {
                    val predictionText = result.prediction
                    val confidence = result.confidence
                    
                    Log.d("HandLandmarkerService", "Prediction: $predictionText (confidence: ${(confidence * 100).toInt()}%)")
                    
                    if (confidence >= PREDICTION_THRESHOLD) {
                        // High confidence - add to sentence
                        predictionHistory.add(predictionText)
                        sentenceBuilder.append(" $predictionText ")
                        val currentSentence = sentenceBuilder.toString().trim()
                        
                        _prediction.value = currentSentence
                        handLandmarkerListener?.onPrediction(predictionText)
                        
                        Log.d("HandLandmarkerService", "High confidence - added to sentence: $predictionText")
                        Log.d("HandLandmarkerService", "Current sentence: $currentSentence")
                    } else {
                        // Low confidence - show but don't add to sentence
                        _prediction.value = predictionText
                        handLandmarkerListener?.onPrediction(predictionText)
                        
                        Log.d("HandLandmarkerService", "Low confidence - showing but not adding: $predictionText")
                    }
                } else {
                    Log.d("HandLandmarkerService", "No prediction result")
                    handLandmarkerListener?.onPrediction("")
                }
            } catch (e: Exception) {
                Log.e("HandLandmarkerService", "Error during prediction", e)
                handLandmarkerListener?.onError("Prediction error: ${e.message}", 1)
            } finally {
                isPredicting = false
            }
        }
    }
    
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
        val currentTime = SystemClock.uptimeMillis()
        _frameCount.value += 1
        
        // Limit processing rate for smooth performance
        if (currentTime - lastProcessTime < minProcessInterval) {
            imageProxy.close()
            return
        }
        
        lastProcessTime = currentTime
        
        try {
            // Check if service is released or HandLandmarker is unavailable
            if (isReleased || handLandmarker == null) {
                Log.w("HandLandmarkerService", "Service released or HandLandmarker null, skipping frame")
                return
            }
            
            // Validate imageProxy
            if (imageProxy.planes.size < 3) {
                Log.w("HandLandmarkerService", "Invalid imageProxy: insufficient planes")
                return
            }
            
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            // Validate buffer sizes
            if (ySize <= 0 || uSize <= 0 || vSize <= 0) {
                Log.w("HandLandmarkerService", "Invalid buffer sizes: y=$ySize, u=$uSize, v=$vSize")
                return
            }

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val out = ByteArrayOutputStream()
            val success = yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 85, out)
            
            if (!success) {
                Log.w("HandLandmarkerService", "Failed to compress YUV to JPEG")
                return
            }
            
            val imageBytes = out.toByteArray()
            if (imageBytes.isEmpty()) {
                Log.w("HandLandmarkerService", "Empty image bytes after compression")
                return
            }
            
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Validate bitmap
            if (bitmap == null || bitmap.isRecycled) {
                Log.w("HandLandmarkerService", "Invalid bitmap: null or recycled")
                return
            }
            
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                Log.w("HandLandmarkerService", "Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                bitmap.recycle()
                return
            }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f)
                }
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            
            // Clean up original bitmap if different from rotated
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            
            // Validate rotated bitmap
            if (rotatedBitmap == null || rotatedBitmap.isRecycled) {
                Log.w("HandLandmarkerService", "Invalid rotated bitmap")
                return
            }
            
            try {
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                detectAsync(mpImage, currentTime)
            } finally {
                // Clean up bitmap after creating MPImage
                rotatedBitmap.recycle()
            }

        } catch (e: Exception) {
            Log.e("HandLandmarkerService", "Error in detectLiveStream", e)
        } finally {
            imageProxy.close()
        }
    }
    
    fun updateCameraType(isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
    }
    
    fun release() {
        try {
            isReleased = true
            handLandmarker?.close()
            handLandmarker = null
            signLanguagePredictor?.unloadModel()
            signLanguagePredictor = null
            currentCategory = null
            landmarkBuffer.clear()
            isPredicting = false
            Log.d("HandLandmarkerService", "Service released successfully")
        } catch (e: Exception) {
            Log.e("HandLandmarkerService", "Error during release", e)
        }
    }
} 