package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.komunikav2.services.HandLandmarkerService
import com.example.komunikav2.services.NearbyConnectionService
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay

@Composable
fun HearingCameraPopup(
    onClose: () -> Unit,
    predictionMessage: String = "",
    onPredictionChange: (String) -> Unit = {},
    selectedUser: com.example.komunikav2.data.UserProfile? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val handLandmarkerService = remember { HandLandmarkerService(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    val scrollState = rememberScrollState()
    
    val isHandDetected by handLandmarkerService.isHandDetected.collectAsState()
    val prediction by handLandmarkerService.prediction.collectAsState()
    val categoryChanges by nearbyService.categoryChanges.collectAsState()
    val wrongSignSignals by nearbyService.wrongSignSignals.collectAsState()
    val predictionSentSignals by nearbyService.predictionSentSignals.collectAsState()
    val predictionPauseSignals by nearbyService.predictionPauseSignals.collectAsState()
    
    // Set up the listener for predictions (like SignLanguageRecognitionScreen)
    LaunchedEffect(handLandmarkerService) {
        handLandmarkerService.setListener(object : HandLandmarkerService.LandmarkerListener {
            override fun onResults(resultBundle: HandLandmarkerService.ResultBundle) {
                // Handle hand detection results if needed
            }
            
            override fun onError(error: String, errorCode: Int) {
                android.util.Log.e("HearingCameraPopup", "Error: $error (Code: $errorCode)")
            }
            
            override fun onPrediction(prediction: String) {
                android.util.Log.d("HearingCameraPopup", "New prediction: $prediction")
                // The prediction StateFlow will be updated automatically
            }
        })
    }
    
    // Store the latest category change to apply when user is selected
    var latestCategoryChange by remember { mutableStateOf<com.example.komunikav2.services.CategoryChangeMessage?>(null) }
    
    // Track when delete operation is in progress to prevent race conditions
    var isDeleteInProgress by remember { mutableStateOf(false) }
    
    // Listen for category changes and store them
    LaunchedEffect(categoryChanges) {
        categoryChanges?.let { change ->
            android.util.Log.d("HearingCameraPopup", "Received category change: ${change.category} from user: ${change.fromUserId}")
            latestCategoryChange = change
        }
    }
    
    // Apply category when user is selected (or when category changes for selected user)
    LaunchedEffect(selectedUser, latestCategoryChange) {
        if (selectedUser != null && latestCategoryChange != null) {
            val change = latestCategoryChange!!
            android.util.Log.d("HearingCameraPopup", "Selected user: ${selectedUser.id}, Latest category change from: ${change.fromUserId}")
            
            if (change.fromUserId == selectedUser.id) {
                // Load the new category in the hand landmarker service
                handLandmarkerService.loadModelsAndLabels(change.category)
                android.util.Log.d("HearingCameraPopup", "Loading model for category: ${change.category} by user: ${selectedUser.name}")
            } else {
                android.util.Log.d("HearingCameraPopup", "Category change ignored - not from selected user")
            }
        }
    }
    
    // Handle wrong sign signals from selected user
    LaunchedEffect(wrongSignSignals?.eventId, selectedUser) {
        wrongSignSignals?.let { signal ->
            if (selectedUser != null && signal.fromUserId == selectedUser.id) {
                android.util.Log.d("HearingCameraPopup", "Delete signal received from ${selectedUser.name} - removing last prediction")
                
                // Set delete in progress flag
                isDeleteInProgress = true
                
                // Remove only the last prediction, keep the rest of the sentence
                handLandmarkerService.removeLastPrediction()
                val updatedSentence = handLandmarkerService.getCurrentSentence()
                
                // Also trim the currently displayed text to immediately reflect deletion
                val displayText = predictionMessage.trim()
                val words = if (displayText.isNotEmpty()) displayText.split("\\s+".toRegex()) else emptyList()
                val updatedDisplay = if (words.size > 1) words.dropLast(1).joinToString(" ") else ""
                
                val finalText = when {
                    updatedDisplay.isNotBlank() -> updatedDisplay
                    updatedSentence.isNotBlank() -> updatedSentence
                    else -> "Last word deleted. Ready for new gesture..."
                }
                onPredictionChange(finalText)
                
                // Keep UI guarded while the service suppression window is active
                kotlinx.coroutines.delay(2000)
                isDeleteInProgress = false
            }
        }
    }
    
    // Handle prediction sent signals from selected user (clear display when sent to chat)
    LaunchedEffect(predictionSentSignals, selectedUser) {
        predictionSentSignals?.let { signal ->
            if (selectedUser != null && signal.fromUserId == selectedUser.id) {
                android.util.Log.d("HearingCameraPopup", "Prediction sent signal received from ${selectedUser.name} - clearing display")
                
                // Clear the prediction completely
                handLandmarkerService.clearPrediction()
                onPredictionChange("Prediction sent to chat. Ready for new conversation...")
            }
        }
    }
    
    var isPaused by remember { mutableStateOf(false) }

    // Handle pause/resume from deaf user
    LaunchedEffect(predictionPauseSignals?.eventId, selectedUser) {
        predictionPauseSignals?.let { pause ->
            if (selectedUser != null && pause.fromUserId == selectedUser.id) {
                isPaused = pause.paused
                if (pause.paused) {
                    android.util.Log.d("HearingCameraPopup", "Pausing detection on request from ${selectedUser.name}")
                    handLandmarkerService.clearPrediction()
                    onPredictionChange("Prediction paused by user.")
                } else {
                    android.util.Log.d("HearingCameraPopup", "Resuming detection on request from ${selectedUser.name}")
                    onPredictionChange("")
                }
            }
        }
    }

    // Only update prediction when user is selected and not during delete operation and not paused
    LaunchedEffect(isHandDetected, selectedUser, prediction, isDeleteInProgress, isPaused) {
        // Don't update UI during delete operation to prevent race conditions
        if (isDeleteInProgress) {
            android.util.Log.d("HearingCameraPopup", "Skipping prediction update - delete in progress")
            return@LaunchedEffect
        }
        if (isPaused) {
            onPredictionChange("Prediction paused by user.")
            return@LaunchedEffect
        }
        
        when {
            selectedUser == null -> onPredictionChange("Select a user to start prediction")
            prediction.isNotBlank() -> {
                val cleanPrediction = prediction.replace("_", " ").replace("-", " ")
                onPredictionChange(cleanPrediction)
                // Send prediction to selected user
                nearbyService.sendPredictionToUser(selectedUser.id, prediction)
            }
            isHandDetected -> onPredictionChange("Hand detected - analyzing sign...")
            else -> onPredictionChange("User selected. Waiting for ${selectedUser.name} to choose a category...")
        }
    }
    
    // Autoscroll when prediction message changes - scroll to show the growing sentence
    LaunchedEffect(predictionMessage) {
        if (predictionMessage.isNotBlank()) {
            delay(100) // Small delay to ensure the text is rendered
            // Scroll to the bottom to show the latest additions to the sentence
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Close Button (X icon) - Above camera
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Camera Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            SignLanguageCameraPreview(
                modifier = Modifier.fillMaxSize(),
                handLandmarkerService = if (selectedUser != null) handLandmarkerService else null
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Prediction Message Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Fixed height for scrollability
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFF9C4))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = predictionMessage.ifEmpty { "Prediction message here..." },
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerService.release()
        }
    }
}

