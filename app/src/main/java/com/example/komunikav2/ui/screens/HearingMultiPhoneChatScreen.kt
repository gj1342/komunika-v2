package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
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
import com.example.komunikav2.ui.components.MultiPhoneChatMessage
import com.example.komunikav2.ui.components.MultiPhoneMessageInput
import com.example.komunikav2.ui.components.MultiPhoneUserDropdown
import com.example.komunikav2.ui.components.TopBar
import com.example.komunikav2.ui.components.UserTypeIndicator

@Composable
fun HearingMultiPhoneChatScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    
    var currentMessage by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf(
        MultiPhoneChatMessage(
            id = "1",
            text = "Ipsum reprehenderit ea nulla velit dolore laborum in id sint tempor et magna tempor veniam. Pariatur cillum venia dolore",
            isIncoming = true,
            timestamp = "2 mins ago",
            avatar = "👩"
        ),
        MultiPhoneChatMessage(
            id = "2",
            text = "Cupidatat exercitation",
            isIncoming = true,
            timestamp = "2 mins ago",
            avatar = "👩"
        ),
        MultiPhoneChatMessage(
            id = "3",
            text = "Mollit excepteur eiusmod conse",
            isIncoming = false,
            timestamp = "Just now"
        ),
        MultiPhoneChatMessage(
            id = "4",
            text = "Exercitation ca id",
            isIncoming = false,
            timestamp = "Just now"
        )
    )) }
    
    val userType = userDataManager.getUserType()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
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
                       MultiPhoneChatMessage(
                           message = message,
                           onMessageClick = { clickedMessage ->
                               val wasVideoCardVisible = clickedMessage.showVideoCard
                               val updatedMessages = messages.map { msg ->
                                   if (msg.id == clickedMessage.id) {
                                       msg.copy(showVideoCard = !msg.showVideoCard)
                                   } else {
                                       msg.copy(showVideoCard = false)
                                   }
                               }
                               messages = updatedMessages
                               
                               // Auto-scroll to keep the clicked message visible when video card appears
                               if (!wasVideoCardVisible) {
                                   val messageIndex = updatedMessages.indexOfFirst { it.id == clickedMessage.id }
                                   if (messageIndex != -1) {
                                       val reversedIndex = updatedMessages.size - 1 - messageIndex
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
                            val newMessage = MultiPhoneChatMessage(
                                id = System.currentTimeMillis().toString(),
                                text = currentMessage,
                                isIncoming = false,
                                timestamp = "Just now"
                            )
                            messages = messages + newMessage
                            currentMessage = ""
                        }
                },
                onMicClick = {
                    // TODO: Implement voice input
                },
                onCameraClick = {
                    // TODO: Implement camera functionality
                }
            )
        }
    }
} 