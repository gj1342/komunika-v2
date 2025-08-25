package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import android.net.Uri
import com.example.komunikav2.services.LabelService
import com.example.komunikav2.services.VideoCatalog
import com.example.komunikav2.ui.components.CategoryFilterButtons
import com.example.komunikav2.ui.components.PlayButton
import com.example.komunikav2.ui.components.TopBar
import com.example.komunikav2.ui.components.VideoCard
import com.example.komunikav2.ui.components.VideoCardPlayer
import kotlinx.coroutines.launch

@Composable
fun CategoryFSLVideoScreen(
    navController: NavController,
    category: String
) {
    val context = LocalContext.current
    val labelService = remember { LabelService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var labels by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var videoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var replayTrigger by remember { mutableStateOf(0) }

    val initialLabel = navController.currentBackStackEntry?.arguments?.getString("label")?.takeIf { it.isNotBlank() }

    val categoryTitle = labelService.getCategoryTitle(category)

    LaunchedEffect(category) {
        coroutineScope.launch {
            labels = labelService.loadLabelsForCategory(category)
            selectedLabel = if (initialLabel != null && labels.contains(initialLabel)) initialLabel else labels.firstOrNull()
            isLoading = false
        }
    }

    LaunchedEffect(selectedLabel) {
        videoUris = selectedLabel?.let { label ->
            VideoCatalog.getUriForVocabularyLabel(category, label)?.let { listOf(it) } ?: emptyList()
        } ?: emptyList()
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
                Spacer(modifier = Modifier.height(16.dp))
                
                CategoryFilterButtons(
                    labels = labels,
                    selectedLabel = selectedLabel ?: "",
                    onLabelSelected = { label ->
                        selectedLabel = label
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (videoUris.isNotEmpty()) {
                            VideoCardPlayer(uris = videoUris, replayTrigger = replayTrigger)
                        } else {
                            VideoCard()
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        PlayButton(
                            onClick = {
                                replayTrigger++
                            }
                        )
                    }
                }
            }
        }
    }
}
