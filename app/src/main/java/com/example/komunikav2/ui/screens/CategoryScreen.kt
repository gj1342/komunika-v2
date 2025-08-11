package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.services.LabelService
import com.example.komunikav2.ui.components.CategoryButton
import com.example.komunikav2.ui.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    navController: NavController,
    category: String
) {
    val context = LocalContext.current
    val labelService = remember { LabelService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var labels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val categoryTitle = labelService.getCategoryTitle(category)
    
    LaunchedEffect(category) {
        coroutineScope.launch {
            labels = labelService.loadLabelsForCategory(category)
            isLoading = false
        }
    }
    
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
                title = categoryTitle,
                onBackClick = { navController.navigateUp() },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(labels) { label ->
                        CategoryButton(
                            text = label.replace("_", " ").capitalize(),
                            onClick = {
                                navController.navigate(Screen.CategoryFSLVideo.createRoute(category))
                            },
                            modifier = Modifier.height(56.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
