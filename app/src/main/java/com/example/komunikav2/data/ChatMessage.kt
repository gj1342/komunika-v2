package com.example.komunikav2.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val timestamp: Long,
    val isIncoming: Boolean = false
) 