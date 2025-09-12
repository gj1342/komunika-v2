package com.example.komunikav2

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.komunikav2.navigation.NavGraph
import com.example.komunikav2.services.PermissionManager
import com.example.komunikav2.ui.theme.KomunikaV2Theme
import com.example.komunikav2.utils.CompatibilityHelper

class MainActivity : ComponentActivity() {
    
    private val permissionManager by lazy { PermissionManager(this) }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionManager.updatePermissionState()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        CompatibilityHelper.logCompatibilityInfo(this)
        checkAndRequestPermissions()
        
        setContent {
            KomunikaV2Theme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val state = permissionManager.checkAllPermissions()
        
        if (!state.allRequiredGranted) {
            val requiredPermissions = permissionManager.getRequiredPermissions()
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    fun requestPermissions() {
        checkAndRequestPermissions()
    }
    
    fun isNearbyConnectionReady(): Boolean {
        return permissionManager.isNearbyConnectionReady()
    }
}