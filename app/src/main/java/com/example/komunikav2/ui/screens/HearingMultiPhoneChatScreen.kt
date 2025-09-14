package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.komunikav2.ui.components.MultiPhoneChatMessage
import com.example.komunikav2.ui.components.MultiPhoneMessageInput
import com.example.komunikav2.ui.components.MultiPhoneUserDropdown
import com.example.komunikav2.ui.components.TopBar
import com.example.komunikav2.ui.components.UserTypeIndicator
import com.example.komunikav2.ui.components.HearingCameraPopup
import androidx.compose.runtime.collectAsState
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.services.VideoCatalog
import java.text.SimpleDateFormat
import com.example.komunikav2.services.VoskSpeechRecognizer
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.*
import com.example.komunikav2.services.TextToSpeechManager

@Composable
fun HearingMultiPhoneChatScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    var currentMessage by remember { mutableStateOf("") }
    var showCameraPopup by remember { mutableStateOf(false) }
    var predictionMessage by remember { mutableStateOf("") }
    var selectedUserForPrediction by remember { mutableStateOf<com.example.komunikav2.data.UserProfile?>(null) }
    var isMicActive by remember { mutableStateOf(false) }
    
    val userType = userDataManager.getUserType()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    val messages by nearbyService.messages.collectAsState()
    
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Track which messages have video cards visible
    var messagesWithVideoCards by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(Unit) {
        TextToSpeechManager.initialize(context)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Don't disconnect here as we want to maintain the connection
            // The connection will be managed by the service
            TextToSpeechManager.shutdown()
        }
    }
    
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            VoskSpeechRecognizer.startListening(context) { text ->
                coroutineScope.launch {
                    currentMessage = (currentMessage + " " + text).trim()
                }
            }
            isMicActive = true
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
                        key = connectedUsers.size,
                        connectedUsers = connectedUsers,
                        selectedUser = selectedUserForPrediction,
                        onUserClick = { user -> 
                            // Send notification to the previously selected user (if any) that they are no longer selected
                            selectedUserForPrediction?.let { previousUser ->
                                nearbyService.sendUserSelectionNotification(previousUser.id, false)
                            }
                            
                            // Update the selected user
                            selectedUserForPrediction = user
                            
                            // Send notification to the newly selected user
                            user?.let { newUser ->
                                nearbyService.sendUserSelectionNotification(newUser.id, true)
                            }
                        }
                    )
                }
            )
            
            UserTypeIndicator(userType)
            
            // Camera Popup
            if (showCameraPopup) {
                HearingCameraPopup(
                    onClose = { 
                        showCameraPopup = false
                    },
                    predictionMessage = predictionMessage,
                    onPredictionChange = { message ->
                        predictionMessage = message
                    },
                    selectedUser = selectedUserForPrediction
                )
            }
            
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

                               if (!wasVideoCardVisible) {
                                  TextToSpeechManager.speak(clickedMessage.text)
                                   val messageIndex = messages.indexOfFirst { it.id == clickedMessage.id }
                                   if (messageIndex != -1) {
                                       val reversedIndex = messages.size - 1 - messageIndex
                                       coroutineScope.launch { listState.animateScrollToItem(reversedIndex) }
                                   }
                               }
                           }
                       )
                   }
               }
            
            MultiPhoneMessageInput(
                message = currentMessage,
                onMessageChange = { currentMessage = it },
                onSendClick = {
                    if (currentMessage.isNotBlank()) {
                        nearbyService.sendMessage(currentMessage)
                        currentMessage = ""
                    }
                },
                onMicClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        val currentlyListening = VoskSpeechRecognizer.isListening
                        if (currentlyListening) {
                            VoskSpeechRecognizer.stopListening()
                            isMicActive = false
                        } else {
                            VoskSpeechRecognizer.startListening(context) { text ->
                                coroutineScope.launch {
                                    currentMessage = (currentMessage + " " + text).trim()
                                }
                            }
                            isMicActive = true
                        }
                    }
                },
                onCameraClick = {
                    showCameraPopup = true
                },
                isMicActive = isMicActive
            )
        }
    }
} 