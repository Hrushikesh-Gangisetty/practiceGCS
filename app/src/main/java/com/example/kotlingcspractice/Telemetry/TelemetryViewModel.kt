package com.example.kotlingcspractice.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlingcspractice.Telemetry.MavlinkTelemetryRepository
import com.example.kotlingcspractice.Telemetry.TelemetryState
import com.example.kotlingcspractice.Telemetry.MavMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TelemetryViewModel : ViewModel() {

    private val repo = MavlinkTelemetryRepository

    // Expose as a StateFlow for Compose to observe
    val telemetry: StateFlow<TelemetryState> = repo.state


    init {
        // Start the MAVLink telemetry collection
        repo.start()
    }

    fun arm() {
        viewModelScope.launch {
            repo.arm()
        }
    }

    fun disarm() {
        viewModelScope.launch {
            repo.disarm()
        }
    }

    fun changeMode() {
        viewModelScope.launch {
            repo.changeMode(MavMode(3u)) // 3u corresponds to "Auto" mode
        }
    }

    fun takeoff() {
        viewModelScope.launch {
            repo.takeoff(10f)
        }
    }

    fun land() {
        viewModelScope.launch {
            repo.land()
        }
    }
}
