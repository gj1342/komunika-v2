package com.example.komunikav2.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
        ) {}
    }
} 

@OptIn(UnstableApi::class)
@Composable
fun VideoCardPlayer(uris: List<Uri>) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    LaunchedEffect(uris) {
        player.clearMediaItems()
        uris.forEach { uri ->
            player.addMediaItem(MediaItem.fromUri(uri))
        }
        if (uris.isNotEmpty()) {
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                controllerAutoShow = false
                controllerHideOnTouch = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(32.dp))
    )
}