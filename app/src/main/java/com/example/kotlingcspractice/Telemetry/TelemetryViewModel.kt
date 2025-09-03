package com.example.kotlingcspractice.Telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow

class TelemetryViewModel(
    private val repo: MavlinkTelemetryRepository = MavlinkTelemetryRepository()
) : ViewModel(){
    val telemetry : StateFlow<TelemetryState> get() = repo.state
    init {
        repo.start(viewModelScope)
    }
}