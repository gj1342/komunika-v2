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

@Composable
fun SignLanguageRecognitionScreen(navController: NavController) {
    var prediction by remember { mutableStateOf("") }
    val context = LocalContext.current
    val handLandmarkerService = remember { HandLandmarkerService(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    val handLandmarks by handLandmarkerService.handLandmarks.collectAsState()
    val isHandDetected by handLandmarkerService.isHandDetected.collectAsState()
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    
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
        VocabularyCategory(R.drawable.numbers, R.string.numbers, R.color.button_teal, "numbers"),
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
                            println("Selected user: ${user.name}")
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
                    .padding(horizontal = 16.dp)
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
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = colorResource(id = category.colorResId),
                                    onClick = { 
                                        if (category.categoryKey == "numbers") {
                                            navController.navigate(Screen.Numbers.route)
                                        } else {
                                            navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                        }
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF9C4))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isHandDetected) "Hand detected - analyzing sign..." else prediction.ifEmpty { "Prediction message here..." },
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
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
                        // TODO: Handle wrong sign feedback
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
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
                        if (prediction.isNotBlank()) {
                            // TODO: Handle send prediction
                            prediction = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    ),
                    enabled = prediction.isNotBlank()
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