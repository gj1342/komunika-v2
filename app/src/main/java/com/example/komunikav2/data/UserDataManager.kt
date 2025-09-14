package com.example.komunikav2.data

import android.content.Context
import android.content.SharedPreferences

class UserDataManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_profile",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_UPLOADED_AVATAR_URI = "uploaded_avatar_uri"
        private const val DEFAULT_NAME = "NAME"
        private const val DEFAULT_AVATAR = "👓"
        private const val DEFAULT_USER_TYPE = "Deaf"
    }
    
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
    }
    
    fun setUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    fun getUserAvatar(): String {
        return sharedPreferences.getString(KEY_USER_AVATAR, DEFAULT_AVATAR) ?: DEFAULT_AVATAR
    }
    
    fun setUserAvatar(avatar: String) {
        sharedPreferences.edit().putString(KEY_USER_AVATAR, avatar).apply()
    }
    
    fun getUserType(): String {
        return sharedPreferences.getString(KEY_USER_TYPE, DEFAULT_USER_TYPE) ?: DEFAULT_USER_TYPE
    }
    
    fun setUserType(userType: String) {
        sharedPreferences.edit().putString(KEY_USER_TYPE, userType).apply()
    }
    
    fun saveUserProfile(name: String, avatar: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_AVATAR, avatar)
            .apply()
    }
    
    fun getUploadedAvatarUri(): String? {
        return sharedPreferences.getString(KEY_UPLOADED_AVATAR_URI, null)
    }
    
    fun setUploadedAvatarUri(uri: String) {
        sharedPreferences.edit().putString(KEY_UPLOADED_AVATAR_URI, uri).apply()
    }
} 