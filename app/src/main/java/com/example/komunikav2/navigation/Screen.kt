package com.example.komunikav2.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Navigation : Screen("navigation")
    object Singlephone : Screen("singlephone")
    object Multiphone : Screen("multiphone")
    object Connection : Screen("connection")
    object Vocabulary : Screen("vocabulary")
} 