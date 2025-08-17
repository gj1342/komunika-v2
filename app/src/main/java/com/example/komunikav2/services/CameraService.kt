package com.example.komunikav2.services

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

class CameraService(private val context: Context) {
    
    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()
    
    private val _currentCamera = MutableStateFlow(CameraType.FRONT)
    val currentCamera: StateFlow<CameraType> = _currentCamera.asStateFlow()
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var currentPreviewView: PreviewView? = null
    private var handLandmarkerService: HandLandmarkerService? = null
    
    enum class CameraType {
        FRONT, BACK
    }
    
    fun initializeCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: Executor,
        handLandmarkerService: HandLandmarkerService? = null,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Prevent multiple initializations
        if (_isCameraActive.value) {
            android.util.Log.d("CameraService", "Camera already active, skipping initialization")
            onSuccess()
            return
        }
        
        android.util.Log.d("CameraService", "Initializing camera...")
        currentPreviewView = previewView
        this.handLandmarkerService = handLandmarkerService
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupCamera(previewView, lifecycleOwner, executor)
                _isCameraActive.value = true
                android.util.Log.d("CameraService", "Camera initialized successfully")
                onSuccess()
            } catch (e: Exception) {
                _isCameraActive.value = false
                android.util.Log.e("CameraService", "Camera initialization failed", e)
                onError(e)
            }
        }, executor)
    }
    
    private fun setupCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: Executor
    ) {
        val cameraProvider = cameraProvider ?: return
        
        android.util.Log.d("CameraService", "Setting up camera...")
        
        // Update camera type in HandLandmarkerService
        handLandmarkerService?.setCameraType(_currentCamera.value == CameraType.FRONT)
        
        // Unbind any existing use cases first
        try {
            cameraProvider.unbindAll()
            android.util.Log.d("CameraService", "Unbound existing use cases")
        } catch (e: Exception) {
            android.util.Log.w("CameraService", "Error unbinding existing use cases", e)
        }
        
        preview = Preview.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        android.util.Log.d("CameraService", "Preview configured")
        
        val imageAnalyzer = handLandmarkerService?.createImageAnalysis() ?: ImageAnalysis.Builder()
            .setTargetRotation(previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        android.util.Log.d("CameraService", "Image analyzer configured")
        
        val cameraSelector = when (_currentCamera.value) {
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        android.util.Log.d("CameraService", "Using camera: ${_currentCamera.value}")
        
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!,
                imageAnalyzer
            )
            android.util.Log.d("CameraService", "Camera bound to lifecycle successfully")
        } catch (e: Exception) {
            android.util.Log.e("CameraService", "Failed to bind camera to lifecycle", e)
            throw e
        }
    }
    
    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        executor: Executor
    ) {
        _currentCamera.value = when (_currentCamera.value) {
            CameraType.FRONT -> CameraType.BACK
            CameraType.BACK -> CameraType.FRONT
        }
        
        // Update camera type in HandLandmarkerService
        handLandmarkerService?.setCameraType(_currentCamera.value == CameraType.FRONT)
        
        if (_isCameraActive.value && currentPreviewView != null) {
            // Reset camera active state to force reinitialization
            _isCameraActive.value = false
            setupCamera(currentPreviewView!!, lifecycleOwner, executor)
            _isCameraActive.value = true
        }
    }
    
    fun releaseCamera() {
        try {
            cameraProvider?.unbindAll()
            _isCameraActive.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCameraProvider(): ProcessCameraProvider? {
        return cameraProvider
    }
    
    fun getCurrentCameraSelector(): CameraSelector {
        return when (_currentCamera.value) {
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
} 