package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.data.UserDataManager
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.InstructionalContent
import com.example.komunikav2.ui.components.ServerIdInput
import com.example.komunikav2.ui.components.ServiceStatusImage
import com.example.komunikav2.ui.components.StartButton
import com.example.komunikav2.ui.components.TopBar

@Composable
fun ConnectionScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    var serverId by remember { mutableStateOf("") }
    
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
                title = stringResource(R.string.multi_phone_title),
                onBackClick = { navController.popBackStack() },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ServiceStatusImage()
                
                Spacer(modifier = Modifier.height(32.dp))
                
                InstructionalContent()
                
                Spacer(modifier = Modifier.height(48.dp))
                
                ServerIdInput(
                    value = serverId,
                    onValueChange = { serverId = it }
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                StartButton(
                    onClick = {
                        if (serverId.isNotBlank()) {
                            val userType = userDataManager.getUserType()
                            if (userType.lowercase() == "non deaf") {
                                navController.navigate(Screen.MultiphoneChat.route)
                            } else {
                                // Deaf users navigate to deaf-specific chat screen
                                navController.navigate(Screen.DeafMultiphoneChat.route)
                            }
                        }
                    }
                )
            }
        }
    }
} 