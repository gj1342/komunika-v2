package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.*
import com.example.komunikav2.services.HandLandmarkerService
import com.example.komunikav2.services.NearbyConnectionService
import androidx.compose.runtime.collectAsState

@Composable
fun SignLanguageRecognitionScreen(navController: NavController) {
    var prediction by remember { mutableStateOf("") }
    val context = LocalContext.current
    val handLandmarkerService = remember { HandLandmarkerService(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    val handLandmarks by handLandmarkerService.handLandmarks.collectAsState()
    val isHandDetected by handLandmarkerService.isHandDetected.collectAsState()
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.cloud_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                title = stringResource(R.string.multi_phone_title),
                onBackClick = { navController.popBackStack() },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                trailingContent = {
                    MultiPhoneUserDropdown(
                        key = connectedUsers.size, // Force recomposition when user count changes
                        connectedUsers = connectedUsers,
                        onUserClick = { user ->
                            // TODO: Handle user selection
                            println("Selected user: ${user.name}")
                        }
                    )
                }
            )
            
            // Screen Title
            Text(
                text = "SIGN LANGUAGE RECOGNITION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            // Camera Preview
            SignLanguageCameraPreview()
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Prediction Input
            PredictionInput(
                prediction = if (isHandDetected) "Hand detected - analyzing sign..." else prediction,
                onSendClick = {
                    // TODO: Handle send prediction
                    if (prediction.isNotBlank()) {
                        // Send the prediction
                        prediction = ""
                    }
                }
            )
            
            // Action Buttons
            SignLanguageActionButtons(
                onVocabularyClick = {
                    navController.navigate(Screen.Vocabulary.route)
                },
                onWrongSignClick = {
                    // TODO: Handle wrong sign feedback
                }
            )
        }
    }
} 