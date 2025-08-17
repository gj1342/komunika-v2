package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
import android.util.Log

@Composable
fun SignLanguageRecognitionScreen(navController: NavController) {
    var prediction by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentSentence by remember { mutableStateOf("") }
    var predictionHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val handLandmarkerService = remember { HandLandmarkerService(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    val handLandmarks by handLandmarkerService.handLandmarks.collectAsState()
    val isHandDetected by handLandmarkerService.isHandDetected.collectAsState()
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    
    // Set up the listener
    LaunchedEffect(handLandmarkerService) {
        handLandmarkerService.setListener(object : HandLandmarkerService.LandmarkerListener {
            override fun onResults(resultBundle: HandLandmarkerService.ResultBundle) {
                // Handle hand detection results if needed
            }
            
            override fun onError(error: String, errorCode: Int) {
                Log.e("SignLanguageRecognition", "Error: $error (Code: $errorCode)")
            }
            
            override fun onPrediction(prediction: String) {
                // Update the current sentence from the service
                currentSentence = handLandmarkerService.getCurrentSentence()
                predictionHistory = handLandmarkerService.getPredictionHistory()
                
                Log.d("SignLanguageRecognition", "New prediction: $prediction")
                Log.d("SignLanguageRecognition", "Current sentence: $currentSentence")
                Log.d("SignLanguageRecognition", "Prediction history: $predictionHistory")
            }
        })
    }
    
    // All 18 vocabulary categories
    val vocabularyCategories = listOf(
        VocabularyCategory(R.drawable.greetings, R.string.greetings, R.color.button_light_blue, "greetings"),
        VocabularyCategory(R.drawable.wh_questions, R.string.wh_questions, R.color.button_orange, "questions"),
        VocabularyCategory(R.drawable.gender, R.string.gender, R.color.button_blue, "gender"),
        VocabularyCategory(R.drawable.survival, R.string.survival, R.color.button_red, "survival"),
        VocabularyCategory(R.drawable.facial_expressions, R.string.facial_expressions, R.color.button_yellow, "facial_expressions"),
        VocabularyCategory(R.drawable.calendar, R.string.calendar, R.color.button_purple, "calendar"),
        VocabularyCategory(R.drawable.time, R.string.time, R.color.button_light_blue, "time"),
        VocabularyCategory(R.drawable.money_matters, R.string.money_matters, R.color.button_green, "money_matters"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_1_10, R.color.button_teal, "numbers1-10"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_11_19, R.color.button_teal, "numbers11-19"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_20_100, R.color.button_teal, "numbers20-100"),
        VocabularyCategory(R.drawable.people, R.string.people, R.color.button_brown_orange, "people"),
        VocabularyCategory(R.drawable.place, R.string.place, R.color.button_pink_purple, "places"),
        VocabularyCategory(R.drawable.family, R.string.family, R.color.button_peach_orange, "family"),
        VocabularyCategory(R.drawable.food, R.string.food, R.color.button_yellow_orange, "food"),
        VocabularyCategory(R.drawable.colors, R.string.colors, R.color.button_light_peach, "colors"),
        VocabularyCategory(R.drawable.pronouns, R.string.pronouns, R.color.button_light_purple, "pronouns"),
        VocabularyCategory(R.drawable.verbs, R.string.verbs, R.color.button_dark_orange_brown, "verbs"),
        VocabularyCategory(R.drawable.adjectives_and_adverbs, R.string.adjectives_adverbs, R.color.button_light_green, "adjectives_and_adverbs"),
        VocabularyCategory(R.drawable.alphabets, R.string.alphabets, R.color.button_light_teal_green, "alphabets")
    )
    
    // Handle prediction updates from HandLandmarkerService
    LaunchedEffect(Unit) {
        handLandmarkerService.prediction.collect { newPrediction ->
            prediction = newPrediction
            Log.d("SignLanguageRecognition", "Prediction StateFlow updated: '$newPrediction'")
        }
    }
    
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
            // Header
            TopBar(
                title = stringResource(R.string.multi_phone_title),
                onBackClick = { navController.popBackStack() },
                backgroundColor = Color.Transparent,
                trailingContent = {
                    MultiPhoneUserDropdown(
                        key = connectedUsers.size,
                        connectedUsers = connectedUsers,
                        onUserClick = { user ->
                            // TODO: Handle user selection
                            println("Selected user: ${user?.name}")
                        }
                    )
                }
            )
            
            // Sign Language Recognition Text
            Text(
                text = "SIGN LANGUAGE RECOGNITION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            // Camera Preview
            SignLanguageCameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
                handLandmarkerService = handLandmarkerService
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Vocabulary Categories (Scrollable) - All 18 categories
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Generate rows of 4 buttons each
                vocabularyCategories.chunked(4).forEach { rowCategories ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCategories.forEach { category ->
                                val isModelAvailable = category.categoryKey in listOf(
                                    "family", "gender", "numbers1-10", "numbers11-19", "numbers20-100",
                                    "people", "places", "questions", "time"
                                )
                                val isSelected = selectedCategory == category.categoryKey
                                
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = when {
                                        isSelected -> Color.Blue // Selected state - blue background
                                        isModelAvailable -> colorResource(id = category.colorResId) // Available
                                        else -> Color.Gray.copy(alpha = 0.5f) // Unavailable
                                    },
                                    onClick = { 
                                        selectedCategory = category.categoryKey
                                        Log.d("SignLanguageRecognition", "Selected category: $selectedCategory")
                                        handLandmarkerService.loadModelsAndLabels(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if row has less than 4 items
                            repeat(4 - rowCategories.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // Prediction Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF9C4))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = when {
                                currentSentence.isNotBlank() -> currentSentence
                                prediction.isNotBlank() -> prediction
                                selectedCategory == null -> "Select a category to start recognition"
                                isHandDetected -> "Analyzing..."
                                else -> "Ready for sign recognition"
                            },
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wrong Sign Button
                Button(
                    onClick = {
                        if (predictionHistory.isNotEmpty()) {
                            // Remove the last prediction
                            val newHistory = predictionHistory.dropLast(1)
                            predictionHistory = newHistory
                            
                            // Rebuild the sentence
                            val newSentence = newHistory.joinToString(" ")
                            currentSentence = newSentence
                            
                            // Update the service
                            handLandmarkerService.rebuildSentenceFromHistory(newHistory)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    enabled = predictionHistory.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Wrong Sign",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wrong Sign")
                }
                
                // Send Button
                Button(
                    onClick = {
                        if (currentSentence.isNotBlank()) {
                            // Send the sentence to all connected devices
                            nearbyService.sendMessage(currentSentence)
                            Log.d("SignLanguageRecognition", "Sending sentence to all connected devices: $currentSentence")
                            
                            // Clear the sentence after sending
                            handLandmarkerService.clearPrediction()
                            currentSentence = ""
                            predictionHistory = emptyList()
                            
                            // Release hand landmarker service to stop prediction
                            handLandmarkerService.release()
                            
                            // Navigate back to chat screen
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    ),
                    enabled = currentSentence.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send")
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