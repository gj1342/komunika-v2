package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    
    val isHandDetected by handLandmarkerService.isHandDetected.collectAsState()
    val prediction by handLandmarkerService.prediction.collectAsState()
    val categoryChanges by nearbyService.categoryChanges.collectAsState()
    val wrongSignSignals by nearbyService.wrongSignSignals.collectAsState()
    val predictionSentSignals by nearbyService.predictionSentSignals.collectAsState()
    
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
    LaunchedEffect(wrongSignSignals, selectedUser) {
        wrongSignSignals?.let { signal ->
            if (selectedUser != null && signal.fromUserId == selectedUser.id) {
                android.util.Log.d("HearingCameraPopup", "Delete signal received from ${selectedUser.name} - removing last prediction")
                
                // Set delete in progress flag
                isDeleteInProgress = true
                
                // Remove only the last prediction, keep the rest of the sentence
                handLandmarkerService.removeLastPrediction()
                
                // Show the updated sentence after deletion, or status if empty
                val updatedSentence = handLandmarkerService.getCurrentSentence()
                if (updatedSentence.isNotBlank()) {
                    onPredictionChange(updatedSentence)
                } else {
                    onPredictionChange("Last word deleted. Ready for new gesture...")
                }
                
                // Reset delete flag after a shorter delay since we're showing the sentence
                kotlinx.coroutines.delay(1000)
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
    
    // Only update prediction when user is selected and not during delete operation
    LaunchedEffect(isHandDetected, selectedUser, prediction, isDeleteInProgress) {
        // Don't update UI during delete operation to prevent race conditions
        if (isDeleteInProgress) {
            android.util.Log.d("HearingCameraPopup", "Skipping prediction update - delete in progress")
            return@LaunchedEffect
        }
        
        when {
            selectedUser == null -> onPredictionChange("Select a user to start prediction")
            prediction.isNotBlank() -> {
                onPredictionChange(prediction)
                // Send prediction to selected user
                nearbyService.sendPredictionToUser(selectedUser.id, prediction)
            }
            isHandDetected -> onPredictionChange("Hand detected - analyzing sign...")
            else -> onPredictionChange("User selected. Waiting for ${selectedUser.name} to choose a category...")
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
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = predictionMessage.ifEmpty { "Prediction message here..." },
                        fontSize = 16.sp, // Increased font size
                        color = Color.Black,
                        fontWeight = FontWeight.Medium // Changed font weight
                    )
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerService.release()
        }
    }
}

