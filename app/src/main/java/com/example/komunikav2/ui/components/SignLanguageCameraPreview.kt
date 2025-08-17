package com.example.komunikav2.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.komunikav2.services.CameraService
import com.example.komunikav2.services.HandLandmarkerHelper
import com.example.komunikav2.ui.components.HandLandmarkOverlay

@Composable
fun SignLanguageCameraPreview(
    modifier: Modifier = Modifier,
    handLandmarkerService: com.example.komunikav2.services.HandLandmarkerService? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val cameraService = remember { CameraService(context) }
    var isHandDetected by remember { mutableStateOf(false) }
    var handLandmarkerResult by remember { mutableStateOf<com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult?>(null) }
    
    val currentCamera by cameraService.currentCamera.collectAsState()
    val isCameraActive by cameraService.isCameraActive.collectAsState()
    
    // Use the provided HandLandmarkerService or create a new one
    val landmarkerService = handLandmarkerService ?: remember { 
        com.example.komunikav2.services.HandLandmarkerService(context) 
    }
    
    // Collect hand detection state from the service
    val isHandDetectedFromService by landmarkerService.isHandDetected.collectAsState()
    val handLandmarksFromService by landmarkerService.handLandmarks.collectAsState()
    
    LaunchedEffect(isHandDetectedFromService) {
        isHandDetected = isHandDetectedFromService
    }
    
    LaunchedEffect(handLandmarksFromService) {
        if (handLandmarksFromService.isNotEmpty()) {
            handLandmarkerResult = handLandmarksFromService.first()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraService.releaseCamera()
            landmarkerService.release()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    // Initialize camera only once
                    if (!isCameraActive) {
                        cameraService.initializeCamera(
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner,
                            executor = ContextCompat.getMainExecutor(context),
                            handLandmarkerService = landmarkerService,
                            onSuccess = {
                                // Camera initialized successfully
                            },
                            onError = { e ->
                                e.printStackTrace()
                            }
                        )
                    }
                }
            )
            
            // Hand landmark overlay
            AndroidView(
                factory = { context ->
                    HandLandmarkOverlay(context).apply {
                        // Set initial state
                        if (isHandDetected && handLandmarkerResult != null) {
                            setResults(handLandmarkerResult!!)
                        } else {
                            clear()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { overlay ->
                    if (isHandDetected && handLandmarkerResult != null) {
                        overlay.setResults(handLandmarkerResult!!)
                    } else {
                        overlay.clear()
                    }
                }
            )
            

            
            // Camera switch button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { 
                            cameraService.switchCamera(
                                lifecycleOwner = lifecycleOwner,
                                executor = ContextCompat.getMainExecutor(context)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentCamera == CameraService.CameraType.FRONT) 
                            Icons.Default.CameraRear else Icons.Default.CameraFront,
                        contentDescription = if (currentCamera == CameraService.CameraType.FRONT) 
                            "Switch to back camera" else "Switch to front camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Permission request or fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission required",
                    color = Color.Gray
                )
            }
        }
    }
} 