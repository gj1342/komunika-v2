package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.komunikav2.R

@Composable
fun VideoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // If you have a video/image resource, use it here. Otherwise, keep the white background.
            // Example (uncomment and replace R.drawable.sample_image if you have an image):
            // Image(
            //     painter = painterResource(id = R.drawable.sample_image),
            //     contentDescription = "Video Preview",
            //     modifier = Modifier.fillMaxSize(),
            //     contentScale = ContentScale.Crop
            // )
        }
    }
} 