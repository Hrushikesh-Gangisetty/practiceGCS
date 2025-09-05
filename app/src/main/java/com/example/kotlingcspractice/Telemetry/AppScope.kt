package com.example.kotlingcspractice.Telemetry



import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppScope : CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.IO
}
