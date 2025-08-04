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
    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    
    enum class CameraType {
        FRONT, BACK
    }
    
    fun initializeCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: Executor,
        handLandmarkerHelper: HandLandmarkerHelper? = null,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        currentPreviewView = previewView
        this.handLandmarkerHelper = handLandmarkerHelper
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupCamera(previewView, lifecycleOwner, executor)
                _isCameraActive.value = true
                onSuccess()
            } catch (e: Exception) {
                _isCameraActive.value = false
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
        
        preview = Preview.Builder().build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
        
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor) { imageProxy ->
                    handLandmarkerHelper?.detectLiveStream(imageProxy, _currentCamera.value == CameraType.FRONT)
                }
            }
        
        val cameraSelector = when (_currentCamera.value) {
            CameraType.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraType.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview!!,
                imageAnalyzer
            )
        } catch (e: Exception) {
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
        
        if (_isCameraActive.value && currentPreviewView != null) {
            setupCamera(currentPreviewView!!, lifecycleOwner, executor)
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