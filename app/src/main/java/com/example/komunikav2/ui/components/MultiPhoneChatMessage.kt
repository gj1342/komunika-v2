package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MultiPhoneChatMessage(
    val id: String,
    val text: String,
    val isIncoming: Boolean,
    val timestamp: String,
    val avatar: String? = null,
    val senderName: String? = null,
    val showVideoCard: Boolean = false
)

@Composable
fun MultiPhoneChatMessage(
    message: MultiPhoneChatMessage,
    onMessageClick: (MultiPhoneChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onMessageClick(message) },
        horizontalAlignment = if (message.isIncoming) Alignment.Start else Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (message.isIncoming) Arrangement.Start else Arrangement.End
        ) {
            if (message.isIncoming && message.avatar != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.avatar,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(
                horizontalAlignment = if (message.isIncoming) Alignment.Start else Alignment.End
            ) {
                // Show sender name for incoming messages
                if (message.isIncoming && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (message.isIncoming) Color(0xFFE1BEE7) else Color(0xFFE3F2FD)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 16.sp,
                        color = Color.Black,
                        maxLines = 10
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.timestamp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
                   // Show video card if enabled
           if (message.showVideoCard) {
               Spacer(modifier = Modifier.height(16.dp))
               MultiPhoneVideoCard(
                   onPlayClick = {
                       // TODO: Handle video play
                       println("Playing video for message: ${message.id}")
                   },
                   onExitClick = {
                       // Hide the video card
                       onMessageClick(message)
                   }
               )
           }
    }
} 