package com.example.komunikav2.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class HandLandmarkerHelper(
    private val context: Context,
    private val runningMode: RunningMode,
    private val handLandmarkerHelperListener: LandmarkerListener
) {
    
    private var handLandmarker: HandLandmarker? = null
    private var isFrontCamera = false
    private var signLanguagePredictor: SignLanguagePredictor? = null
    private var landmarkBuffer: MutableList<List<Float>> = mutableListOf()
    private var currentCategory: String? = null
    private var isPredicting = false
    
    companion object {
        private const val TAG = "HandLandmarkerHelper"
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
        val inputImage: MPImage,
        val inferenceTime: Long
    )
    
    init {
        setupHandLandmarker()
        signLanguagePredictor = SignLanguagePredictor(context)
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
                .setRunningMode(runningMode)
                .setResultListener { result, image ->
                    val inferenceTime = SystemClock.uptimeMillis()
                    handLandmarkerHelperListener.onResults(ResultBundle(result, image, inferenceTime))
                    
                    if (result.landmarks().isNotEmpty()) {
                        processLandmarksForPrediction(result.landmarks()[0])
                    }
                }
                .setErrorListener { error ->
                    handLandmarkerHelperListener.onError(error.message ?: "Unknown error", -1)
                }
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError("Failed to setup HandLandmarker: ${e.message}", -1)
        }
    }
    
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
        val frameTime = SystemClock.uptimeMillis()
        
        try {
            // Convert ImageProxy to Bitmap without consuming the buffer
            val bitmap = imageProxyToBitmap(imageProxy, isFrontCamera)
            val mpImage = BitmapImageBuilder(bitmap).build()
            detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError("Error processing image: ${e.message}", -1)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy, isFrontCamera: Boolean): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Create copies of the buffer data to avoid interfering with preview
        val yCopy = ByteArray(ySize)
        val uCopy = ByteArray(uSize)
        val vCopy = ByteArray(vSize)
        
        yBuffer.get(yCopy)
        vBuffer.get(vCopy)
        uBuffer.get(uCopy)
        
        // Restore buffer positions for preview
        yBuffer.rewind()
        vBuffer.rewind()
        uBuffer.rewind()

        val nv21 = ByteArray(ySize + uSize + vSize)
        System.arraycopy(yCopy, 0, nv21, 0, ySize)
        System.arraycopy(vCopy, 0, nv21, ySize, vSize)
        System.arraycopy(uCopy, 0, nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Apply rotation and front camera flip
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f)
            }
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        try {
            handLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError("Error in detection: ${e.message}", -1)
        }
    }
    
    fun loadModelsAndLabels(category: String) {
        currentCategory = category
        landmarkBuffer.clear()
        isPredicting = false
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = signLanguagePredictor?.loadModel(category) ?: false
                if (success) {
                    Log.d(TAG, "Model loaded successfully for category: $category")
                    handLandmarkerHelperListener.onPrediction("Model loaded for $category")
                } else {
                    Log.e(TAG, "Failed to load model for category: $category")
                    handLandmarkerHelperListener.onPrediction("Failed to load model for $category")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model for category: $category", e)
                handLandmarkerHelperListener.onPrediction("Error loading model for $category")
            }
        }
    }
    
    private fun processLandmarksForPrediction(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        if (currentCategory == null || signLanguagePredictor?.isModelLoaded() != true) {
            return
        }
        
        val landmarkData = landmarks.flatMap { landmark ->
            listOf(landmark.x(), landmark.y(), landmark.z())
        }
        
        landmarkBuffer.add(landmarkData)
        
        if (landmarkBuffer.size > BUFFER_SIZE) {
            landmarkBuffer.removeAt(0)
        }
        
        if (landmarkBuffer.size == BUFFER_SIZE && !isPredicting) {
            makePrediction()
        }
    }
    
    private fun makePrediction() {
        if (isPredicting) return
        
        isPredicting = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = signLanguagePredictor?.predict(landmarkBuffer.toList())
                if (result != null && result.confidence >= PREDICTION_THRESHOLD) {
                    handLandmarkerHelperListener.onPrediction("${result.prediction} (${(result.confidence * 100).toInt()}%)")
                } else if (result != null) {
                    handLandmarkerHelperListener.onPrediction("Low confidence: ${result.prediction} (${(result.confidence * 100).toInt()}%)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error making prediction", e)
            } finally {
                isPredicting = false
            }
        }
    }
    
    fun unloadModels() {
        try {
            handLandmarker?.close()
            handLandmarker = null
            signLanguagePredictor?.unloadModel()
            currentCategory = null
            landmarkBuffer.clear()
            isPredicting = false
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError("Error unloading models: ${e.message}", -1)
        }
    }
    
    fun clearHandLandmarker() {
        try {
            handLandmarker?.close()
            handLandmarker = null
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError("Error clearing hand landmarker: ${e.message}", -1)
        }
    }
} 