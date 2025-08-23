package com.example.komunikav2.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun UserSelectionNotification(
    isSelected: Boolean,
    selectedByUserName: String?,
    modifier: Modifier = Modifier
) {
    var showNotification by remember { mutableStateOf(false) }
    
    LaunchedEffect(isSelected) {
        if (isSelected) {
            showNotification = true
            delay(3000) // Show for 3 seconds
            showNotification = false
        }
    }
    
    AnimatedVisibility(
        visible = showNotification,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = selectedByUserName?.let { "Selected by $it" } ?: "You are selected",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
