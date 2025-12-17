package com.example.kotlingcspractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.kotlingcspractice.telemetry.TelemetryViewModel
import com.example.kotlingcspractice.ui.navigation.AppNavigation
import com.example.kotlingcspractice.ui.theme.KotlinGCSPracticeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotlinGCSPracticeTheme {
                val navController = rememberNavController()
                val viewModel: TelemetryViewModel = viewModel()

                AppNavigation(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
