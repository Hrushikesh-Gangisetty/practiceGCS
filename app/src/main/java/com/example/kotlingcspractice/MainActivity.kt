package com.example.kotlingcspractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlingcspractice.Design.TelemetryOverlay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotlingcspractice.telemetry.TelemetryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TelemetryViewModel = viewModel()
            val state = vm.telemetry.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            LaunchedEffect(state.value.connected) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (state.value.connected) "Connected" else "Disconnected"
                    )
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { _ ->
                TelemetryOverlay(
                    state = state.value,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
