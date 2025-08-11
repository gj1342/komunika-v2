package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.TopBar
import com.example.komunikav2.ui.components.VocabularyCategoryButton

@Composable
fun VocabularyScreen(navController: NavController) {
    val vocabularyCategories = listOf(
        VocabularyCategory(R.drawable.greetings, R.string.greetings, R.color.button_light_blue, "greetings"),
        VocabularyCategory(R.drawable.wh_questions, R.string.wh_questions, R.color.button_orange, "questions"),
        VocabularyCategory(R.drawable.gender, R.string.gender, R.color.button_blue, "gender"),
        VocabularyCategory(R.drawable.survival, R.string.survival, R.color.button_red, "survival"),
        VocabularyCategory(R.drawable.facial_expressions, R.string.facial_expressions, R.color.button_yellow, "facial_expressions"),
        VocabularyCategory(R.drawable.calendar, R.string.calendar, R.color.button_purple, "calendar"),
        VocabularyCategory(R.drawable.time, R.string.time, R.color.button_light_blue, "time"),
        VocabularyCategory(R.drawable.money_matters, R.string.money_matters, R.color.button_green, "money_matters"),
        VocabularyCategory(R.drawable.numbers, R.string.numbers, R.color.button_teal, "numbers"),
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.cloud_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                title = stringResource(R.string.vocabulary),
                onBackClick = { navController.navigateUp() },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Row 1: 4 standard buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vocabularyCategories.take(4).forEach { category ->
                            VocabularyCategoryButton(
                                iconResId = category.iconResId,
                                text = stringResource(id = category.textResId),
                                backgroundColor = colorResource(id = category.colorResId),
                                onClick = { 
                                    if (category.categoryKey == "numbers") {
                                        navController.navigate(Screen.Numbers.route)
                                    } else {
                                        navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    // Row 2: 4 standard buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vocabularyCategories.drop(4).take(4).forEach { category ->
                            VocabularyCategoryButton(
                                iconResId = category.iconResId,
                                text = stringResource(id = category.textResId),
                                backgroundColor = colorResource(id = category.colorResId),
                                onClick = { 
                                    if (category.categoryKey == "numbers") {
                                        navController.navigate(Screen.Numbers.route)
                                    } else {
                                        navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    // Row 3: 4 standard buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vocabularyCategories.drop(8).take(4).forEach { category ->
                            VocabularyCategoryButton(
                                iconResId = category.iconResId,
                                text = stringResource(id = category.textResId),
                                backgroundColor = colorResource(id = category.colorResId),
                                onClick = { 
                                    if (category.categoryKey == "numbers") {
                                        navController.navigate(Screen.Numbers.route)
                                    } else {
                                        navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    // Row 4: 4 standard buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vocabularyCategories.drop(12).take(4).forEach { category ->
                            VocabularyCategoryButton(
                                iconResId = category.iconResId,
                                text = stringResource(id = category.textResId),
                                backgroundColor = colorResource(id = category.colorResId),
                                onClick = { 
                                    if (category.categoryKey == "numbers") {
                                        navController.navigate(Screen.Numbers.route)
                                    } else {
                                        navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    // Row 5: 2 wide buttons (Adjectives & Adverbs, Alphabets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        vocabularyCategories.drop(16).take(2).forEach { category ->
                            VocabularyCategoryButton(
                                iconResId = category.iconResId,
                                text = stringResource(id = category.textResId),
                                backgroundColor = colorResource(id = category.colorResId),
                                onClick = { 
                                    if (category.categoryKey == "numbers") {
                                        navController.navigate(Screen.Numbers.route)
                                    } else {
                                        navController.navigate(Screen.Category.createRoute(category.categoryKey))
                                    }
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

data class VocabularyCategory(
    val iconResId: Int,
    val textResId: Int,
    val colorResId: Int,
    val categoryKey: String
) 