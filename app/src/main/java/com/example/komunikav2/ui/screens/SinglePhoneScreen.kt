package com.example.komunikav2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.komunikav2.R
import com.example.komunikav2.ui.components.ChatMessageItem
import com.example.komunikav2.ui.components.ChatMessage
import com.example.komunikav2.ui.components.LatestText
import com.example.komunikav2.ui.components.MessageInput
import com.example.komunikav2.ui.components.TopBar
import com.example.komunikav2.ui.components.VideoCard
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.Image

@Composable
fun SinglePhoneScreen(navController: NavController) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
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
                title = stringResource(R.string.single_phone),
                onBackClick = { navController.navigateUp() },
                onClearClick = { showClearConfirmation = true }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                VideoCard()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val latestMessage = messages.lastOrNull()?.text
                if (latestMessage != null && latestMessage.isNotBlank()) {
                    LatestText(text = latestMessage)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            text = message.text,
                            isUser = message.isUser
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                MessageInput(
                    value = messageText,
                    onValueChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotEmpty()) {
                            messages = messages + ChatMessage(messageText, true)
                            messageText = ""
                        }
                    }
                )
            }
        }
        
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = {
                    Text(
                        text = stringResource(R.string.clear_chat_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.clear_chat_message)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            messages = listOf()
                            messageText = ""
                            showClearConfirmation = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.clear),
                            color = colorResource(R.color.error_red)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmation = false }
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = colorResource(R.color.dark_gray)
                        )
                    }
                }
            )
        }
    }
} 