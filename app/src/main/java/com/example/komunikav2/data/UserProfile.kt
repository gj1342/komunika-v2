package com.example.komunikav2.data

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val avatar: String,
    val userType: String,
    val serviceId: String
) 