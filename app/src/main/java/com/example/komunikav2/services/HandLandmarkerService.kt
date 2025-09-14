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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayOutputStream
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private var frameCounter = 0
    private var slidingWindowStride = 1
    private var landmarkBuffer: ArrayDeque<List<Float>> = ArrayDeque()
    private var currentCategory: String? = null
    private var isPredicting = false
    private var lastPredictionTime = 0L
    private var minPredictionInterval = TARGET_GESTURE_DURATION_MS // Initialize with 1-second target
    private var predictionLockUntilMs: Long = 0L
    @Volatile private var suppressNextPrediction: Boolean = false
    private var predictionJob: Job? = null

    
    // Sentence building components
    private val predictionHistory = mutableListOf<String>()
    private val sentenceBuilder = StringBuilder()
    private var handLandmarkerListener: LandmarkerListener? = null
    private var analyzerExecutor: ExecutorService? = null
    private var lastHandSeenAt = 0L
    private var handAbsentClearDelayMs = HAND_DETECTION_GRACE_PERIOD_MS
    private var lastPredictedClass: String? = null
    private var emaConfidence = 0f
    private var emaAlpha = 0.6f
    private var stableClassCount = 0
    private var requiredStableCount = 1
    
    // 1-second optimization variables
    private var gestureStartTime = 0L
    private var lastGesturePredictionTime = 0L
    private var isGestureInProgress = false
    private var consecutiveHandAbsenceCount = 0
    private var maxConsecutiveHandAbsence = 3
    
    companion object {
        private const val BUFFER_SIZE = 30
        private const val PREDICTION_THRESHOLD = 0.7f
        private const val QUICK_PREDICTION_BUFFER_SIZE = 20
        private const val QUICK_PREDICTION_INTERVAL = 800L
        
        // 1-second optimization constants
        private const val TARGET_GESTURE_DURATION_MS = 1000L
        private const val MIN_GESTURE_DURATION_MS = 800L
        private const val MAX_GESTURE_DURATION_MS = 1500L
        private const val HAND_DETECTION_GRACE_PERIOD_MS = 500L
        private const val MAX_PREDICTION_DELAY_MS = 2000L
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
        analyzerExecutor = Executors.newSingleThreadExecutor()
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
        // Cancel any in-flight prediction immediately
        try {
            predictionJob?.cancel()
        } catch (_: Exception) {}
        predictionJob = null
        isPredicting = false

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

        // Suppress the next prediction updates for a short window while new frames still stream
        suppressNextPrediction = true
                        predictionLockUntilMs = SystemClock.uptimeMillis() + 1200L


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
        
        // Always process landmarks for prediction, even when no hands detected (like Python reference)
        if (result.landmarks().isNotEmpty()) {
            calculateBoundingBox(result, image.width, image.height)
            Log.d("HandLandmarkerService", "Processing landmarks for prediction")

            val hands = result.landmarks()
            
            // Handle front camera mirroring: when image is mirrored, left/right hands are swapped
            // For front camera: first detected hand = right hand (mirrored), second = left hand (mirrored)
            // For back camera: first detected hand = left hand, second = right hand
            val leftHand = if (hands.isNotEmpty()) {
                if (isFrontCamera && hands.size > 1) hands[1] else hands[0]
            } else null
            val rightHand = if (hands.size > 1) {
                if (isFrontCamera) hands[0] else hands[1]
            } else null

            Log.d("HandLandmarkerService", "Front camera: $isFrontCamera, Hands detected: ${hands.size}, Left hand: ${leftHand != null}, Right hand: ${rightHand != null}")
            processLandmarksForPrediction(leftHand, rightHand)
        } else {
            _handBoundingBox.value = null
            if (lastHandSeenAt == 0L) lastHandSeenAt = SystemClock.uptimeMillis()
            val since = SystemClock.uptimeMillis() - lastHandSeenAt
            
            // More graceful buffer clearing - only clear if hands absent for extended period
            if (since > handAbsentClearDelayMs * 2) {
                landmarkBuffer.clear()
                isPredicting = false
                isGestureInProgress = false
                Log.d("HandLandmarkerService", "Extended hand absence, cleared buffer")
            }
        }
    }
    
    private fun processLandmarksForPrediction(
        leftHand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?,
        rightHand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?
    ) {
        if (currentCategory == null || signLanguagePredictor?.isModelLoaded() != true) {
            Log.d("HandLandmarkerService", "Skipping prediction: category=$currentCategory, modelLoaded=${signLanguagePredictor?.isModelLoaded()}")
            return
        }

        val leftData = leftHand?.let { normalizeHandLandmarks(it) } ?: List(63) { 0.0f }
        val rightData = rightHand?.let { normalizeHandLandmarks(it) } ?: List(63) { 0.0f }
        val landmarkData = leftData + rightData

        landmarkBuffer.add(landmarkData)
        Log.d("HandLandmarkerService", "Added landmark frame with ${landmarkData.size} coordinates. Buffer size: ${landmarkBuffer.size}/$QUICK_PREDICTION_BUFFER_SIZE")

        // Use sliding window approach like Python reference
        if (landmarkBuffer.size > QUICK_PREDICTION_BUFFER_SIZE) {
            landmarkBuffer.removeFirst()
        }

        val hasHandsInCurrentFrame = leftHand != null || rightHand != null
        val currentTime = SystemClock.uptimeMillis()
        
        // Update hand detection tracking
        if (hasHandsInCurrentFrame) {
            lastHandSeenAt = currentTime
            consecutiveHandAbsenceCount = 0
            
            // Start gesture timing if not already started
            if (!isGestureInProgress) {
                gestureStartTime = currentTime
                isGestureInProgress = true
                Log.d("HandLandmarkerService", "Gesture started at $currentTime")
            }
        } else {
            consecutiveHandAbsenceCount++
        }
        
        // Check if we should trigger prediction (1-second optimized logic)
        val shouldPredict = shouldTriggerPrediction(currentTime, hasHandsInCurrentFrame)
        
        // Debug logging
        Log.d("HandLandmarkerService", "Buffer: ${landmarkBuffer.size} frames, Hands: $hasHandsInCurrentFrame, Gesture: $isGestureInProgress, ShouldPredict: $shouldPredict")
        
        if (shouldPredict) {
            Log.d("HandLandmarkerService", "1-second optimized prediction triggered")
            lastPredictionTime = currentTime
            lastGesturePredictionTime = currentTime
            isGestureInProgress = false
            makePrediction()
        }
    }

    private fun shouldTriggerPrediction(currentTime: Long, hasHandsInCurrentFrame: Boolean): Boolean {
        // Don't predict if already predicting
        if (isPredicting) return false
        
        // Don't predict if buffer is too small (reduced requirement for faster response)
        if (landmarkBuffer.size < 5) return false
        
        // Check if we have enough data for 1-second models (reduced minimum for faster response)
        val bufferDuration = estimateBufferDuration()
        if (bufferDuration < 500) return false  // Reduced from 800ms to 500ms
        
        // Calculate time since last prediction
        val timeSinceLastPrediction = currentTime - lastPredictionTime
        
        // Case 1: Perfect 1-second gesture (hands detected throughout)
        if (isGestureInProgress && hasHandsInCurrentFrame) {
            val gestureDuration = currentTime - gestureStartTime
            if (gestureDuration >= TARGET_GESTURE_DURATION_MS) {
                Log.d("HandLandmarkerService", "Perfect 1-second gesture detected (${gestureDuration}ms)")
                return true
            }
        }
        
        // Case 2: Hands briefly missed but we have enough data
        val withinGrace = currentTime - lastHandSeenAt <= HAND_DETECTION_GRACE_PERIOD_MS
        if (withinGrace && bufferDuration >= 800 && timeSinceLastPrediction >= minPredictionInterval) {
            Log.d("HandLandmarkerService", "Hands briefly missed but sufficient data available (${bufferDuration}ms)")
            return true
        }
        
        // Case 3: Force prediction if too much time has passed (prevent long delays)
        val maxDelayReached = currentTime - lastGesturePredictionTime >= MAX_PREDICTION_DELAY_MS
        if (maxDelayReached && bufferDuration >= 500) {
            Log.d("HandLandmarkerService", "Max delay reached, forcing prediction (${bufferDuration}ms)")
            return true
        }
        
        // Case 4: Buffer overflow protection (too much data)
        if (bufferDuration >= MAX_GESTURE_DURATION_MS && timeSinceLastPrediction >= minPredictionInterval) {
            Log.d("HandLandmarkerService", "Buffer overflow protection, predicting (${bufferDuration}ms)")
            return true
        }
        
        return false
    }
    
    private fun estimateBufferDuration(): Long {
        // Estimate how much time the buffer represents
        // Assuming ~30fps, each frame is ~33ms
        val estimatedFrameTime = 33L
        return landmarkBuffer.size * estimatedFrameTime
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
        val exec = analyzerExecutor ?: Executors.newSingleThreadExecutor().also { analyzerExecutor = it }
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(exec) { imageProxy ->
                    detectLiveStream(imageProxy, isFrontCamera)
                }
            }
    }
    
    fun setCameraType(isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
    }
    
    fun setPredictionSpeed(speed: PredictionSpeed) {
        when (speed) {
            PredictionSpeed.FAST -> {
                minPredictionInterval = TARGET_GESTURE_DURATION_MS
                slidingWindowStride = 1
                requiredStableCount = 1
            }
            PredictionSpeed.MEDIUM -> {
                minPredictionInterval = TARGET_GESTURE_DURATION_MS
                slidingWindowStride = 1
                requiredStableCount = 2
            }
            PredictionSpeed.SLOW -> {
                minPredictionInterval = TARGET_GESTURE_DURATION_MS
                slidingWindowStride = 2
                requiredStableCount = 2
            }
        }
        Log.d("HandLandmarkerService", "Prediction speed set to: $speed (1-second optimized)")
    }
    
    fun setInferenceOptimization(optimization: SignLanguagePredictor.InferenceOptimization) {
        signLanguagePredictor?.setInferenceOptimization(optimization)
    }
    
    enum class PredictionSpeed {
        FAST, MEDIUM, SLOW
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
            
            // Check if service is released
            if (isReleased) {
                Log.w("HandLandmarkerService", "Service is released, skipping detection")
                return
            }
            
            // Validate MPImage before passing to MediaPipe
            try {
                landmarker.detectAsync(mpImage, frameTime)
            } catch (e: Exception) {
                Log.e("HandLandmarkerService", "MediaPipe detection failed", e)
                // Don't crash the app, just log the error
            }
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
                
                // Unload previous model first to avoid conflicts
                signLanguagePredictor?.unloadModel()
                
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
    
    fun isCategoryAvailable(category: String): Boolean {
        return signLanguagePredictor?.isCategoryAvailable(category) ?: false
    }

    private fun normalizeHandLandmarks(handLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): List<Float> {
        if (handLandmarks.isEmpty()) {
            return List(63) { 0.0f } // 21 landmarks * 3 coordinates
        }
        
        // Get wrist landmark (base of the hand) - index 0
        val wrist = handLandmarks[0]
        
        // Find max distance from wrist to any finger tip for normalization
        val fingerTipIndices = listOf(4, 8, 12, 16, 20) // Finger tips indices
        var maxDist = 0.0f
        
        for (tipIdx in fingerTipIndices) {
            if (tipIdx < handLandmarks.size) {
                val tip = handLandmarks[tipIdx]
                val dx = tip.x() - wrist.x()
                val dy = tip.y() - wrist.y()
                val dz = tip.z() - wrist.z()
                val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                maxDist = kotlin.math.max(maxDist, dist)
            }
        }
        
        // If max_dist is too small, use a default value to avoid division by near-zero
        if (maxDist < 0.001f) {
            maxDist = 0.1f
        }
        
        // Normalize all points relative to wrist and hand size
        val normalized = mutableListOf<Float>()
        for (landmark in handLandmarks) {
            normalized.add((landmark.x() - wrist.x()) / maxDist)
            normalized.add((landmark.y() - wrist.y()) / maxDist)
            normalized.add((landmark.z() - wrist.z()) / maxDist)
        }
        
        return normalized
    }
    
    private fun makePrediction() {
        if (isPredicting) return
        
        isPredicting = true
        predictionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("HandLandmarkerService", "Making prediction with ${landmarkBuffer.size} frames")
                val result = signLanguagePredictor?.predict(landmarkBuffer.toList())
                Log.d("HandLandmarkerService", "Prediction result: $result")
                
                if (suppressNextPrediction) {
                    Log.d("HandLandmarkerService", "Suppressing prediction output due to recent delete action")
                    suppressNextPrediction = false
                    _prediction.value = sentenceBuilder.toString().trim()
                    return@launch
                }

                val now = SystemClock.uptimeMillis()
                if (now < predictionLockUntilMs) {
                    Log.d("HandLandmarkerService", "Prediction lock active, skipping UI update")
                    _prediction.value = sentenceBuilder.toString().trim()
                    return@launch
                }

                if (result != null) {
                    val predictionText = result.prediction.replace("_", " ").replace("-", " ")
                    val confidence = result.confidence
                    
                    Log.d("HandLandmarkerService", "Prediction: $predictionText (confidence: ${(confidence * 100).toInt()}%, threshold: ${(PREDICTION_THRESHOLD * 100).toInt()}%)")


                    
                    if (lastPredictedClass == null || !predictionText.equals(lastPredictedClass, ignoreCase = true)) {
                        lastPredictedClass = predictionText
                        emaConfidence = confidence
                        stableClassCount = 1
                    } else {
                        emaConfidence = emaAlpha * confidence + (1 - emaAlpha) * emaConfidence
                        stableClassCount += 1
                    }

                    val accept = emaConfidence >= PREDICTION_THRESHOLD && stableClassCount >= requiredStableCount
                    if (accept) {

                        predictionHistory.add(predictionText)
                        sentenceBuilder.append(" $predictionText ")
                        val currentSentence = sentenceBuilder.toString().trim()
                        _prediction.value = currentSentence
                        handLandmarkerListener?.onPrediction(predictionText)

                        lastPredictedClass = null
                        stableClassCount = 0
                        emaConfidence = 0f
                    } else {
                        _prediction.value = sentenceBuilder.toString().trim()
                        handLandmarkerListener?.onPrediction(predictionText)
                    }
                } else {
                    Log.d("HandLandmarkerService", "No prediction result")
                    handLandmarkerListener?.onPrediction("")
                }
            } catch (ce: CancellationException) {
                Log.d("HandLandmarkerService", "Prediction job cancelled")
            } catch (e: Exception) {
                Log.e("HandLandmarkerService", "Error during prediction", e)
                handLandmarkerListener?.onError("Prediction error: ${e.message}", 1)
            } finally {
                isPredicting = false
                predictionJob = null
            }
        }
    }
    
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
        val currentTime = SystemClock.uptimeMillis()
        _frameCount.value += 1
        frameCounter += 1
        
        // Limit processing rate for smooth performance
        if (currentTime - lastProcessTime < minProcessInterval) {
            imageProxy.close()
            return
        }
        
        // Process every 3rd frame like Python reference
        if (frameCounter % slidingWindowStride != 0) {
            imageProxy.close()
            return
        }
        
        lastProcessTime = currentTime
        
        try {
            // Check if service is released or HandLandmarker is unavailable
            if (isReleased) {
                Log.w("HandLandmarkerService", "Service is released, skipping frame")
                return
            }
            
            if (handLandmarker == null) {
                Log.w("HandLandmarkerService", "HandLandmarker is null, skipping frame")
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
            
            val rotatedBitmap = try {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } catch (e: Exception) {
                Log.e("HandLandmarkerService", "Error creating rotated bitmap", e)
                bitmap  // Use original bitmap if rotation fails
            }
            
            // Clean up original bitmap if different from rotated
            if (rotatedBitmap != bitmap) {
                try {
                    bitmap.recycle()
                } catch (e: Exception) {
                    Log.w("HandLandmarkerService", "Error recycling original bitmap", e)
                }
            }
            
            // Validate rotated bitmap
            if (rotatedBitmap == null || rotatedBitmap.isRecycled) {
                Log.w("HandLandmarkerService", "Invalid rotated bitmap")
                return
            }
            
            try {
                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                detectAsync(mpImage, currentTime)
            } catch (e: Exception) {
                Log.e("HandLandmarkerService", "Error creating MPImage or detecting", e)
                // Continue processing without crashing
            } finally {
                // Clean up bitmap after creating MPImage
                try {
                    rotatedBitmap.recycle()
                } catch (e: Exception) {
                    Log.w("HandLandmarkerService", "Error recycling bitmap", e)
                }
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
            try {
                analyzerExecutor?.shutdownNow()
            } catch (_: Exception) {}
            analyzerExecutor = null
        } catch (e: Exception) {
            Log.e("HandLandmarkerService", "Error during release", e)
        }
    }
} 