package com.example.komunikav2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.komunikav2.ui.screens.ConnectionScreen
import com.example.komunikav2.ui.screens.HomeScreen
import com.example.komunikav2.ui.screens.HearingMultiPhoneChatScreen
import com.example.komunikav2.ui.screens.DeafMultiPhoneChatScreen
import com.example.komunikav2.ui.screens.SignLanguageRecognitionScreen
import com.example.komunikav2.ui.screens.NavigationScreen
import com.example.komunikav2.ui.screens.SinglePhoneScreen
import com.example.komunikav2.ui.screens.SplashScreen
import com.example.komunikav2.ui.screens.VocabularyScreen
import com.example.komunikav2.ui.screens.CategoryScreen
import com.example.komunikav2.ui.screens.NumbersScreen

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
            NavigationScreen(navController = navController)
        }
        
        composable(route = Screen.Singlephone.route) {
            SinglePhoneScreen(navController = navController)
        }
        
        composable(route = Screen.Multiphone.route) {
            ConnectionScreen(navController = navController)
        }
        
        composable(route = Screen.MultiphoneChat.route) {
             HearingMultiPhoneChatScreen(navController = navController)
        }
                
        composable(route = Screen.DeafMultiphoneChat.route) {
            DeafMultiPhoneChatScreen(navController = navController)
        }
        
        composable(route = Screen.Vocabulary.route) {
            VocabularyScreen(navController = navController)
        }
        
        composable(
            route = Screen.Category.route,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CategoryScreen(navController = navController, category = category)
        }
        
        composable(route = Screen.Numbers.route) {
            NumbersScreen(navController = navController)
        }
        
        composable(route = Screen.SignLanguageRecognition.route) {
            SignLanguageRecognitionScreen(navController = navController)
        }
    }
} 