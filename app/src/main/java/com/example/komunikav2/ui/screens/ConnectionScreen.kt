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
import com.example.komunikav2.data.UserProfile
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.services.NearbyConnectionService
import com.example.komunikav2.ui.components.ConnectionStatusIndicator
import com.example.komunikav2.ui.components.InstructionalContent
import com.example.komunikav2.ui.components.ServerIdInput
import com.example.komunikav2.ui.components.ServiceStatusImage
import com.example.komunikav2.ui.components.StartButton
import com.example.komunikav2.ui.components.TopBar
import androidx.compose.runtime.collectAsState
import java.util.*

@Composable
fun ConnectionScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    val nearbyService = remember { NearbyConnectionService.getInstance(context) }
    var serverId by remember { mutableStateOf("") }
    
    val connectionState by nearbyService.connectionState.collectAsState()
    val connectedUsers by nearbyService.connectedUsers.collectAsState()
    
    DisposableEffect(Unit) {
        onDispose {
            // Don't disconnect here as we want to maintain the connection
            // The connection will be managed by the service
        }
    }
    
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
                    connectionState = connectionState,
                    onStartClick = {
                        if (serverId.isNotBlank()) {
                            val userProfile = UserProfile(
                                id = UUID.randomUUID().toString(),
                                name = userDataManager.getUserName(),
                                avatar = userDataManager.getUserAvatar(),
                                userType = userDataManager.getUserType(),
                                serviceId = serverId
                            )
                            
                            nearbyService.startAdvertisingAndDiscovery(userProfile)
                        }
                    },
                    onCancelClick = {
                        nearbyService.resetForReconnection()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ConnectionStatusIndicator(
                    connectionState = connectionState,
                    connectedUsersCount = connectedUsers.size
                )
                
                if (connectionState == NearbyConnectionService.ConnectionState.CONNECTED && connectedUsers.isNotEmpty()) {
                    LaunchedEffect(connectedUsers) {
                        val userType = userDataManager.getUserType()
                        if (userType.lowercase() == "non deaf") {
                            navController.navigate(Screen.MultiphoneChat.route)
                        } else {
                            navController.navigate(Screen.DeafMultiphoneChat.route)
                        }
                    }
                }
            }
        }
    }
} 