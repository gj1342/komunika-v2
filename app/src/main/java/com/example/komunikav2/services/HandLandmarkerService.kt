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
import java.io.ByteArrayOutputStream

class HandLandmarkerService(
    private val context: Context,
    private var isFrontCamera: Boolean = true
) {
    
    private var handLandmarker: HandLandmarker? = null
    private val _handLandmarks = MutableStateFlow<List<HandLandmarkerResult>>(emptyList())
    val handLandmarks: StateFlow<List<HandLandmarkerResult>> = _handLandmarks.asStateFlow()
    
    private val _isHandDetected = MutableStateFlow(false)
    val isHandDetected: StateFlow<Boolean> = _isHandDetected.asStateFlow()
    
    private val _handBoundingBox = MutableStateFlow<HandBoundingBox?>(null)
    val handBoundingBox: StateFlow<HandBoundingBox?> = _handBoundingBox.asStateFlow()
    
    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()
    
    private var lastProcessTime = 0L
    private val minProcessInterval = 100L // Process every 100ms for smooth performance
    
    data class HandBoundingBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    init {
        setupHandLandmarker()
    }
    
    private fun setupHandLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath("hand_landmarker.task")
                .build()
            
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(1)
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
        
        if (result.landmarks().isNotEmpty()) {
            calculateBoundingBox(result, image.width, image.height)
        } else {
            _handBoundingBox.value = null
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
                    processImage(imageProxy)
                }
            }
    }
    
    private fun processImage(imageProxy: ImageProxy) {
        val currentTime = SystemClock.uptimeMillis()
        _frameCount.value += 1
        
        // Limit processing rate for smooth performance
        if (currentTime - lastProcessTime < minProcessInterval) {
            imageProxy.close()
            return
        }
        
        lastProcessTime = currentTime
        
        try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

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
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
            val imageBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f)
                }
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()

            detectAsync(mpImage, currentTime)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
    
    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        try {
            handLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    

    
    fun updateCameraType(isFrontCamera: Boolean) {
        this.isFrontCamera = isFrontCamera
    }
    
    fun release() {
        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 