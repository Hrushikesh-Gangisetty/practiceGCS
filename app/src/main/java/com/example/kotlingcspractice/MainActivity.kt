package com.example.kotlingcspractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlingcspractice.Design.TelemetryOverlay
import com.example.kotlingcspractice.Telemetry.TelemetryViewModel
import com.example.kotlingcspractice.ui.theme.KotlinGCSPracticeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm : TelemetryViewModel = TelemetryViewModel()
            val state = vm.telemetry.collectAsStateWithLifecycle()
            TelemetryOverlay(
                state = state.value,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}
