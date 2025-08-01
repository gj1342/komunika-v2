package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.FeatureButton
import com.example.komunikav2.ui.components.TopBar

@Composable
fun NavigationScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                title = stringResource(R.string.navigation),
                onBackClick = { navController.popBackStack() }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FeatureButton(
                    title = stringResource(R.string.singlephone),
                    subtitle = stringResource(R.string.singlephone_subtitle),
                    iconResId = R.drawable.single_phone,
                    backgroundColor = Color(0xFFFF5722),
                    onClick = {
                        navController.navigate(Screen.Singlephone.route)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FeatureButton(
                    title = stringResource(R.string.multiphone),
                    subtitle = stringResource(R.string.multiphone_subtitle),
                    iconResId = R.drawable.multi_phone,
                    backgroundColor = Color(0xFFD32F2F),
                    onClick = {
                        navController.navigate(Screen.Multiphone.route)
                    }
                )
            }
        }
    }
} 