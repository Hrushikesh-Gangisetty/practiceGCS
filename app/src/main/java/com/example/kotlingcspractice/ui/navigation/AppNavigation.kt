package com.example.kotlingcspractice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.kotlingcspractice.telemetry.TelemetryViewModel
import com.example.kotlingcspractice.ui.screens.QuickActionsScreen
import com.example.kotlingcspractice.ui.screens.TelemetryDataScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TelemetryData.route,
        modifier = modifier
    ) {
        composable(Screen.TelemetryData.route) {
            TelemetryDataScreen(
                viewModel = viewModel,
                onNavigateToQuickActions = {
                    navController.navigate(Screen.QuickActions.route)
                }
            )
        }

        composable(Screen.QuickActions.route) {
            QuickActionsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

