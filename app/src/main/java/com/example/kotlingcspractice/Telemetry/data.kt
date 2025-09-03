package com.example.kotlingcspractice.Telemetry

data class TelemetryState(

    val connected : Boolean = false,
    //Altitude
    val altitudeMsl: Float? = null,
    val altitudeRelative: Float? = null,
    //Speeds
    val airspeed: Float? = null,
    val groundspeed: Float? = null,
    //Battery
    val volatage: Float? = null,
    val batteryPercent: Int? = null,
    val currentA : Float? = null,
    //Sat count and HDOP
    val sats : Int? = null,
    val hdop : Float? = null,
)