package com.example.kotlingcspractice.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlingcspractice.Telemetry.MavlinkTelemetryRepository
import com.example.kotlingcspractice.Telemetry.TelemetryState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class TelemetryViewModel : ViewModel() {

    private val repo = MavlinkTelemetryRepository

    // Expose as a StateFlow for Compose to observe
    val telemetry: StateFlow<TelemetryState> = repo.state


    init {
        // Start the MAVLink telemetry collection
        repo.start()
    }
}
