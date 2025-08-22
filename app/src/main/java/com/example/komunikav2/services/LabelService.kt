package com.example.komunikav2.services

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LabelService(private val context: Context) {
    
    suspend fun loadLabelsForCategory(category: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${category}_labels.txt"
            val inputStream = context.assets.open("labels/$fileName")
            val labels = inputStream.bufferedReader().use { reader ->
                reader.readLines().filter { it.isNotBlank() }
            }
            inputStream.close()
            labels
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getCategoryTitle(category: String): String {
        return when (category) {
            "alphabets" -> "ALPHABETS"
            "greetings" -> "GREETINGS"
            "family" -> "FAMILY"
            "people" -> "PEOPLE"
            "places" -> "PLACES"
            "time" -> "TIME"
            "numbers1-10" -> "NUMBERS 1-10"
            "numbers11-19" -> "NUMBERS 11-19"
            "numbers20-100" -> "NUMBERS 20-100"
            "gender" -> "GENDER"
            "questions" -> "QUESTIONS"
            else -> category.uppercase()
        }
    }
}
