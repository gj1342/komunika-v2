package com.example.komunikav2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.komunikav2.R

@Composable
fun ServiceStatusImage() {
    Image(
        painter = painterResource(id = R.drawable.wifi_bluetooth_location),
        contentDescription = stringResource(R.string.wifi_bluetooth_location_description),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 32.dp),
        contentScale = ContentScale.Fit
    )
} 