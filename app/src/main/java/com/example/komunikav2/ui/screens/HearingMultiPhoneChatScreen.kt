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
import androidx.compose.runtime.collectAsState
import com.example.komunikav2.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HearingMultiPhoneChatScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    
    var currentMessage by remember { mutableStateOf("") }
    
    val userType = userDataManager.getUserType()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    val messages by nearbyService.messages.collectAsState()
    
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Track which messages have video cards visible
    var messagesWithVideoCards by remember { mutableStateOf(setOf<String>()) }
    
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
                            println("Selected user: ${user.name}")
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
                           showVideoCard = messagesWithVideoCards.contains(message.id)
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
                    // TODO: Implement voice input
                },
                onCameraClick = {
                    navController.navigate(Screen.SignLanguageRecognition.route)
                }
            )
        }
    }
} 