package com.example.komunikav2.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Navigation : Screen("navigation")
    object Singlephone : Screen("singlephone")
    object Multiphone : Screen("multiphone")
    object Vocabulary : Screen("vocabulary")
} 