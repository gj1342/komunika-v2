package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
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
import com.example.komunikav2.services.NearbyConnectionService

@Composable
fun ConnectionStatusIndicator(
    connectionState: NearbyConnectionService.ConnectionState,
    connectedUsersCount: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, statusText) = when (connectionState) {
        NearbyConnectionService.ConnectionState.DISCONNECTED -> {
            Triple(Color.Gray.copy(alpha = 0.3f), Color.Gray, "Disconnected")
        }
        NearbyConnectionService.ConnectionState.ADVERTISING -> {
            Triple(Color.Blue.copy(alpha = 0.3f), Color.Blue, "Advertising...")
        }
        NearbyConnectionService.ConnectionState.DISCOVERING -> {
            Triple(Color.Yellow.copy(alpha = 0.3f), Color.Yellow, "Discovering...")
        }
        NearbyConnectionService.ConnectionState.CONNECTING -> {
            Triple(Color(0xFFFF9800).copy(alpha = 0.3f), Color(0xFFFF9800), "Connecting...")
        }
        NearbyConnectionService.ConnectionState.CONNECTED -> {
            Triple(Color.Green.copy(alpha = 0.3f), Color.Green, "Connected ($connectedUsersCount users)")
        }
        NearbyConnectionService.ConnectionState.ERROR -> {
            Triple(Color.Red.copy(alpha = 0.3f), Color.Red, "Error")
        }
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
} 