package com.example.kotlingcspractice.ui.navigation

sealed class Screen(val route: String) {
    object TelemetryData : Screen("telemetry_data")
    object QuickActions : Screen("quick_actions")
}

