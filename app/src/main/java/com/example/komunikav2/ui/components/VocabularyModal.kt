package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.komunikav2.R

@Composable
fun VocabularyModal(
    onDismiss: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onWrongSignClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    predictionMessage: String = "",
    onPredictionMessageChange: (String) -> Unit = {},
    selectedCategory: String? = null,
    isModelReady: Boolean = false,
    isHandDetected: Boolean = false
) {
    
    val vocabularyCategories = listOf(
        VocabularyCategory(R.drawable.alphabets, R.string.alphabets, R.color.button_light_blue, "alphabets"),
        VocabularyCategory(R.drawable.greetings, R.string.greetings, R.color.button_light_blue, "greetings"),
        VocabularyCategory(R.drawable.wh_questions, R.string.wh_questions, R.color.button_orange, "questions"),
        VocabularyCategory(R.drawable.gender, R.string.gender, R.color.button_blue, "gender"),
        VocabularyCategory(R.drawable.survival, R.string.survival, R.color.button_red, "survival"),
        VocabularyCategory(R.drawable.facial_expressions, R.string.facial_expressions, R.color.button_yellow, "facial_expressions"),
        VocabularyCategory(R.drawable.calendar, R.string.calendar, R.color.button_purple, "calendar"),
        VocabularyCategory(R.drawable.time, R.string.time, R.color.button_light_blue, "time"),
        VocabularyCategory(R.drawable.money_matters, R.string.money_matters, R.color.button_green, "money_matters"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_1_10, R.color.button_teal, "numbers1-10"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_11_19, R.color.button_teal, "numbers11-19"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers_20_100, R.color.button_teal, "numbers20-100"),
        VocabularyCategory(R.drawable.people, R.string.people, R.color.button_brown_orange, "people"),
        VocabularyCategory(R.drawable.place, R.string.place, R.color.button_pink_purple, "places"),
        VocabularyCategory(R.drawable.family, R.string.family, R.color.button_peach_orange, "family"),
        VocabularyCategory(R.drawable.food, R.string.food, R.color.button_yellow_orange, "food"),
        VocabularyCategory(R.drawable.colors, R.string.colors, R.color.button_light_peach, "colors"),
        VocabularyCategory(R.drawable.pronouns, R.string.pronouns, R.color.button_light_purple, "pronouns"),
        VocabularyCategory(R.drawable.verbs, R.string.verbs, R.color.button_dark_orange_brown, "verbs"),
        VocabularyCategory(R.drawable.adjectives_and_adverbs, R.string.adjectives_adverbs, R.color.button_light_green, "adjectives_and_adverbs"),
        VocabularyCategory(R.drawable.alphabets, R.string.alphabets, R.color.button_light_teal_green, "alphabets")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.vocabulary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Ready/Sign Status Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                                                 containerColor = when {
                             selectedCategory == null -> androidx.compose.ui.graphics.Color(0xFFFF9800).copy(alpha = 0.1f)
                             !isModelReady -> Color.Yellow.copy(alpha = 0.1f)
                             isHandDetected -> Color.Green.copy(alpha = 0.1f)
                             else -> Color.Blue.copy(alpha = 0.1f)
                         }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .                                     background(
                                         when {
                                             selectedCategory == null -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                             !isModelReady -> Color.Yellow
                                             isHandDetected -> Color.Green
                                             else -> Color.Blue
                                         }
                                     )
                            )
                            
                            Text(
                                text = when {
                                    selectedCategory == null -> "Select a category to start"
                                    !isModelReady -> "Loading model..."
                                    isHandDetected -> "Ready to sign"
                                    else -> "Show your hands"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                                                 color = when {
                                     selectedCategory == null -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                     !isModelReady -> Color.Black
                                     isHandDetected -> Color.Green
                                     else -> Color.Blue
                                 }
                            )
                        }
                        
                        if (selectedCategory != null) {
                            Text(
                                text = selectedCategory.replace("_", " ").uppercase(),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Prediction Message Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Prediction Message",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = predictionMessage.ifEmpty { "No prediction available" },
                            fontSize = 16.sp,
                            color = if (predictionMessage.isEmpty()) Color.Gray else Color.Black
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onWrongSignClick() },
                                modifier = Modifier.weight(0.4f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722)
                                )
                            ) {
                                Text(
                                    text = "Delete",
                                    fontSize = 14.sp
                                )
                            }
                            
                            Button(
                                onClick = { 
                                    if (predictionMessage.isNotEmpty()) {
                                        onSendMessage(predictionMessage)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(0.6f),
                                enabled = predictionMessage.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text(
                                    text = "Send",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vocabularyCategories.take(4).forEach { category ->
                                val isSelected = selectedCategory == category.categoryKey
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = if (isSelected) Color.Blue else colorResource(id = category.colorResId),
                                    onClick = { 
                                        onCategoryClick(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vocabularyCategories.drop(4).take(4).forEach { category ->
                                val isSelected = selectedCategory == category.categoryKey
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = if (isSelected) Color.Blue else colorResource(id = category.colorResId),
                                    onClick = { 
                                        onCategoryClick(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vocabularyCategories.drop(8).take(4).forEach { category ->
                                val isSelected = selectedCategory == category.categoryKey
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = if (isSelected) Color.Blue else colorResource(id = category.colorResId),
                                    onClick = { 
                                        onCategoryClick(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vocabularyCategories.drop(12).take(4).forEach { category ->
                                val isSelected = selectedCategory == category.categoryKey
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = if (isSelected) Color.Blue else colorResource(id = category.colorResId),
                                    onClick = { 
                                        onCategoryClick(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vocabularyCategories.drop(16).take(2).forEach { category ->
                                val isSelected = selectedCategory == category.categoryKey
                                VocabularyCategoryButton(
                                    iconResId = category.iconResId,
                                    text = stringResource(id = category.textResId),
                                    backgroundColor = if (isSelected) Color.Blue else colorResource(id = category.colorResId),
                                    onClick = { 
                                        onCategoryClick(category.categoryKey)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class VocabularyCategory(
    val iconResId: Int,
    val textResId: Int,
    val colorResId: Int,
    val categoryKey: String
)
