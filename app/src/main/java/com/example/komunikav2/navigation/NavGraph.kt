package com.example.komunikav2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.komunikav2.ui.screens.HomeScreen
import com.example.komunikav2.ui.screens.SinglePhoneScreen
import com.example.komunikav2.ui.screens.SplashScreen
import com.example.komunikav2.ui.screens.VocabularyScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(route = Screen.Navigation.route) {
            // TODO: Implement NavigationScreen
        }
        
        composable(route = Screen.Singlephone.route) {
            SinglePhoneScreen(navController = navController)
        }
        
        composable(route = Screen.Multiphone.route) {
            // TODO: Implement MultiphoneScreen
        }
        
        composable(route = Screen.Vocabulary.route) {
            VocabularyScreen(navController = navController)
        }
    }
} 