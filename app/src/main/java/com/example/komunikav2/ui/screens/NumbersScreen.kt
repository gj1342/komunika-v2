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
import com.example.komunikav2.services.LabelService
import com.example.komunikav2.ui.components.CategoryButton
import com.example.komunikav2.ui.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun NumbersScreen(navController: NavController) {
    val context = LocalContext.current
    val labelService = remember { LabelService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var allNumbers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val numbers1_10 = labelService.loadLabelsForCategory("numbers1-10")
            val numbers11_19 = labelService.loadLabelsForCategory("numbers11-19")
            val numbers20_100 = labelService.loadLabelsForCategory("numbers20-100")
            
            allNumbers = numbers1_10 + numbers11_19 + numbers20_100
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
                title = "NUMBERS",
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
                    items(allNumbers) { number ->
                        CategoryButton(
                            text = number.replace("_", " ").capitalize(),
                            onClick = {
                                // TODO: Handle number selection
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
