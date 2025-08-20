package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.data.UserDataManager
import com.example.komunikav2.data.ChatMessage
import com.example.komunikav2.services.NearbyConnectionService
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.*
import com.example.komunikav2.services.VideoCatalog
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeafMultiPhoneChatScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    val userType = userDataManager.getUserType()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    val messages by nearbyService.messages.collectAsState()
    
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Track which messages have video cards visible
    var messagesWithVideoCards by remember { mutableStateOf(setOf<String>()) }
    
    // Track vocabulary modal visibility
    var showVocabularyModal by remember { mutableStateOf(false) }
    
    // Track prediction message
    var predictionMessage by remember { mutableStateOf("") }
    
    // Track currently selected category for visual indication
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Listen for incoming predictions from hearing users
    val incomingPredictions by nearbyService.incomingPredictions.collectAsState()
    
    // Update prediction message when incoming predictions arrive
    LaunchedEffect(incomingPredictions) {
        incomingPredictions?.let { prediction ->
            predictionMessage = prediction.prediction
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Don't disconnect here as we want to maintain the connection
            // The connection will be managed by the service
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
            TopBar(
                title = stringResource(R.string.multi_phone_title),
                onBackClick = { 
                    nearbyService.resetForReconnection()
                    navController.popBackStack() 
                },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                trailingContent = {
                    MultiPhoneUserDropdown(
                        key = connectedUsers.size, // Force recomposition when user count changes
                        connectedUsers = connectedUsers,
                        onUserClick = { user ->
                            // TODO: Handle user selection
                            println("Selected user: ${user?.name}")
                        }
                    )
                }
            )
            
            UserTypeIndicator(userType)
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp, bottom = 8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    val multiPhoneMessage = MultiPhoneChatMessage(
                        id = message.id,
                        text = message.text,
                        isIncoming = message.isIncoming,
                        timestamp = dateFormat.format(Date(message.timestamp)),
                        avatar = message.senderAvatar,
                        senderName = message.senderName,
                        showVideoCard = messagesWithVideoCards.contains(message.id),
                        videoUris = if (messagesWithVideoCards.contains(message.id)) VideoCatalog.splitInputToUris(message.text) else emptyList()
                    )
                    
                    MultiPhoneChatMessage(
                        message = multiPhoneMessage,
                        onMessageClick = { clickedMessage ->
                            val wasVideoCardVisible = messagesWithVideoCards.contains(clickedMessage.id)
                            messagesWithVideoCards = if (wasVideoCardVisible) {
                                messagesWithVideoCards - clickedMessage.id
                            } else {
                                setOf(clickedMessage.id)
                            }
                            
                            // Auto-scroll to keep the clicked message visible when video card appears
                            if (!wasVideoCardVisible) {
                                val messageIndex = messages.indexOfFirst { it.id == clickedMessage.id }
                                if (messageIndex != -1) {
                                    val reversedIndex = messages.size - 1 - messageIndex
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(reversedIndex)
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            DeafActionButtons(
                onVocabularyClick = {
                    showVocabularyModal = true
                },
                onSignLanguageRecognitionClick = {
                    navController.navigate(Screen.SignLanguageRecognition.route)
                }
            )
        }
        
        // Vocabulary Modal
        if (showVocabularyModal) {
            VocabularyModal(
                onDismiss = { 
                    // Pause prediction on hearing side when modal is closed
                    nearbyService.sendPredictionPause(true)
                    showVocabularyModal = false 
                },
                onCategoryClick = { categoryKey ->
                    // Send category change to hearing users for camera prediction
                    nearbyService.sendCategoryChange(categoryKey)
                    // Resume prediction upon selecting a category
                    nearbyService.sendPredictionPause(false)
                    // Update selected category for visual indication
                    selectedCategory = categoryKey
                    // Note: Modal stays open, no navigation - just model loading
                    android.util.Log.d("DeafMultiPhoneChatScreen", "Category clicked: $categoryKey, sending to hearing users")
                },
                onWrongSignClick = {
                    // Remove the last word from the current prediction message
                    val words = predictionMessage.trim().split("\\s+".toRegex())
                    predictionMessage = if (words.size > 1) {
                        words.dropLast(1).joinToString(" ")
                    } else {
                        "" // If only one word, clear it
                    }
                    // Send wrong sign signal to hearing users to remove their last prediction
                    nearbyService.sendWrongSignSignal()
                    android.util.Log.d("DeafMultiPhoneChatScreen", "Delete clicked - removed last word and sent signal")
                },
                onSendMessage = { message ->
                    nearbyService.sendMessage(message)
                    // Send signal to hearing users to clear their prediction display
                    nearbyService.sendPredictionSentSignal()
                    // Clear the prediction text in the vocabulary modal
                    predictionMessage = ""
                    android.util.Log.d("DeafMultiPhoneChatScreen", "Prediction sent to chat - cleared modal and signaling hearing users")
                },
                predictionMessage = predictionMessage,
                onPredictionMessageChange = { message ->
                    predictionMessage = message
                },
                selectedCategory = selectedCategory
            )
        }
    }
} 