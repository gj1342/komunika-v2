package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.komunikav2.R
import com.example.komunikav2.services.NearbyConnectionService

@Composable
fun StartButton(
    connectionState: NearbyConnectionService.ConnectionState,
    onStartClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isActive = connectionState != NearbyConnectionService.ConnectionState.DISCONNECTED && 
                   connectionState != NearbyConnectionService.ConnectionState.ERROR
    
    val backgroundColor = if (isActive) Color(0xFFD32F2F) else colorResource(R.color.button_blue)
    val buttonText = if (isActive) "Cancel" else stringResource(R.string.start)
    val onClick = if (isActive) onCancelClick else onStartClick
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buttonText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.white)
        )
    }
} 