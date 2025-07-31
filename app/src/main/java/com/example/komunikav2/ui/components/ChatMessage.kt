package com.example.komunikav2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.komunikav2.R

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatMessageItem(
    text: String,
    isUser: Boolean
) {
    Text(
        text = text,
        color = colorResource(R.color.black),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
} 