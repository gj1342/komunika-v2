package com.example.komunikav2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.komunikav2.R
import com.example.komunikav2.data.UserDataManager
import com.example.komunikav2.navigation.Screen
import com.example.komunikav2.ui.components.EditProfileDialog
import com.example.komunikav2.ui.components.FeatureButton
import com.example.komunikav2.ui.components.NavigationButton
import com.example.komunikav2.ui.components.ProfileSection

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userDataManager = remember { UserDataManager(context) }
    
    var userName by remember { mutableStateOf(userDataManager.getUserName()) }
    var userAvatar by remember { mutableStateOf(userDataManager.getUserAvatar()) }
    var userType by remember { mutableStateOf(userDataManager.getUserType()) }
    var showEditDialog by remember { mutableStateOf(false) }
    
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp)
        ) {
            ProfileSection(
                userName = userName,
                userAvatar = userAvatar,
                userType = userType,
                onEditName = {
                    showEditDialog = true
                },
                onUserTypeChange = { newType ->
                    userType = newType
                    userDataManager.setUserType(newType)
                },
                onHelpClick = {
                    // TODO: Implement help functionality
                }
            )
            
            NavigationButton(
                onClick = {
                    navController.navigate(Screen.Navigation.route)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureButton(
                title = stringResource(R.string.singlephone),
                subtitle = stringResource(R.string.singlephone_subtitle),
                iconResId = R.drawable.single_phone,
                backgroundColor = Color(0xFFFF5722),
                onClick = {
                    navController.navigate(Screen.Singlephone.route)
                }
            )
            
            FeatureButton(
                title = stringResource(R.string.multiphone),
                subtitle = stringResource(R.string.multiphone_subtitle),
                iconResId = R.drawable.multi_phone,
                backgroundColor = Color(0xFFD32F2F),
                onClick = {
                    navController.navigate(Screen.Multiphone.route)
                }
            )
            
            FeatureButton(
                title = stringResource(R.string.vocabulary_button),
                subtitle = stringResource(R.string.vocabulary_subtitle),
                iconResId = R.drawable.vocabulary,
                backgroundColor = Color(0xFF388E3C),
                onClick = {
                    navController.navigate(Screen.Vocabulary.route)
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        if (showEditDialog) {
            EditProfileDialog(
                currentName = userName,
                currentAvatar = userAvatar,
                onDismiss = {
                    showEditDialog = false
                },
                onSave = { newName, newAvatar ->
                    userName = newName
                    userAvatar = newAvatar
                    userDataManager.saveUserProfile(newName, newAvatar)
                    showEditDialog = false
                }
            )
        }
    }
} 