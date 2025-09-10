package com.example.kotlingcspractice.Telemetry

import android.util.Log
import com.divpundir.mavlink.adapters.coroutines.asCoroutine
import com.divpundir.mavlink.adapters.coroutines.tryConnect
import com.divpundir.mavlink.adapters.coroutines.trySendUnsignedV2
import com.divpundir.mavlink.api.MavFrame
import com.divpundir.mavlink.api.wrap
import com.divpundir.mavlink.connection.StreamState
import com.divpundir.mavlink.connection.tcp.TcpClientMavConnection
import com.divpundir.mavlink.definitions.common.*
import com.divpundir.mavlink.definitions.minimal.*
import com.example.kotlingcspractice.Telemetry.AppScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MavlinkTelemetryRepository {
    private const val host: String = "10.0.2.2"
    private const val port: Int = 5762
    private val gcsSystemId: UByte = 200u
    private val gcsComponentId: UByte = 1u
    private val _state = MutableStateFlow(TelemetryState())
    val state: StateFlow<TelemetryState> = _state.asStateFlow()

    private var fcuSystemId: UByte = 0u
    private var fcuComponentId: UByte = 0u

    // Diagnostic info
    private val _lastFailure = MutableStateFlow<Throwable?>(null)
    val lastFailure: StateFlow<Throwable?> = _lastFailure.asStateFlow()

    // Connection
    private val connection = TcpClientMavConnection(host, port, CommonDialect).asCoroutine()

    fun start() {
        val scope = AppScope

        suspend fun reconnect(scope: kotlinx.coroutines.CoroutineScope) {
            while (scope.isActive) {
                try {
                    if (connection.tryConnect(scope)) {
                        return // Exit on successful connection
                    }
                } catch (e: Exception) {
                    Log.e("MavlinkRepo", "Connection attempt failed", e)
                    _lastFailure.value = e
                }
                delay(1000)
            }
        }

        // Manage connection state + reconnects
        scope.launch {
            reconnect(this) // Initial connection attempt
            connection.streamState.collect { st ->
                when (st) {
                    is StreamState.Active -> {
                        if (!state.value.connected) {
                            Log.i("MavlinkRepo", "Connection Active")
                            _state.update { it.copy(connected = true) }
                        }
                    }
                    is StreamState.Inactive -> {
                        if (state.value.connected) {
                            Log.i("MavlinkRepo", "Connection Inactive, reconnecting...")
                            _state.update { it.copy(connected = false, fcuDetected = false) }
                            reconnect(this)
                        }
                    }
                }
            }
        }

        // Send GCS heartbeat
        scope.launch {
            val heartbeat = Heartbeat(
                type = MavType.GCS.wrap(),
                autopilot = MavAutopilot.INVALID.wrap(),
                baseMode = emptyList<MavModeFlag>().wrap(),
                customMode = 0u,
                mavlinkVersion = 3u
            )
            while (isActive) {
                if (state.value.connected) {
                    try {
                        connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, heartbeat)
                    } catch (e: Exception) {
                        Log.e("MavlinkRepo", "Failed to send heartbeat", e)
                        _lastFailure.value = e
                    }
                }
                delay(1000)
            }
        }

        // Shared message stream
        val mavFrameStream = connection.mavFrame
            .shareIn(scope, SharingStarted.Eagerly, replay = 0)

        // Log raw messages
        scope.launch {
            mavFrameStream.collect {
                Log.d("MavlinkRepo", "Frame: ${it.message.javaClass.simpleName} (sysId=${it.systemId}, compId=${it.componentId})")
            }
        }

        // Detect FCU
        scope.launch {
            mavFrameStream
                .filter { it.message is Heartbeat && (it.message as Heartbeat).type != MavType.GCS.wrap() }
                .collect {
                    if (!state.value.fcuDetected) {
                        fcuSystemId = it.systemId
                        fcuComponentId = it.componentId
                        Log.i("MavlinkRepo", "FCU detected sysId=$fcuSystemId compId=$fcuComponentId")
                        _state.update { it.copy(fcuDetected = true) }

                        // Set message intervals
                        launch {
                            suspend fun setMessageRate(messageId: UInt, hz: Float) {
                                val intervalUsec = if (hz <= 0f) 0f else (1_000_000f / hz)
                                val cmd = CommandLong(
                                    targetSystem = fcuSystemId,
                                    targetComponent = fcuComponentId,
                                    command = MavCmd.SET_MESSAGE_INTERVAL.wrap(),
                                    confirmation = 0u,
                                    param1 = messageId.toFloat(),
                                    param2 = intervalUsec,
                                    param3 = 0f,
                                    param4 = 0f,
                                    param5 = 0f,
                                    param6 = 0f,
                                    param7 = 0f
                                )
                                try {
                                    connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, cmd)
                                } catch (e: Exception) {
                                    Log.e("MavlinkRepo", "Failed to send SET_MESSAGE_INTERVAL", e)
                                    _lastFailure.value = e
                                }
                            }

                            setMessageRate(1u, 1f)   // SYS_STATUS
                            setMessageRate(24u, 1f)  // GPS_RAW_INT
                            setMessageRate(33u, 5f)  // GLOBAL_POSITION_INT
                            setMessageRate(74u, 5f)  // VFR_HUD
                            setMessageRate(147u, 1f) // BATTERY_STATUS
                        }
                    }
                }
        }

        // Collectors

        // VFR_HUD
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<VfrHud>()
                .collect { hud ->
                    _state.update {
                        it.copy(
                            altitudeMsl = hud.alt,
                            airspeed = hud.airspeed.takeIf { v -> v > 0f },
                            groundspeed = hud.groundspeed.takeIf { v -> v > 0f }
                        )
                    }
                }
        }

        // GLOBAL_POSITION_INT
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GlobalPositionInt>()
                .collect { gp ->
                    val altAMSLm = gp.alt / 1000f
                    val relAltM = gp.relativeAlt / 1000f
                    val lat = gp.lat.takeIf { it != Int.MIN_VALUE }?.let { it / 10_000_000.0 }
                    val lon = gp.lon.takeIf { it != Int.MIN_VALUE }?.let { it / 10_000_000.0 }
                    _state.update {
                        it.copy(
                            altitudeMsl = altAMSLm,
                            altitudeRelative = relAltM,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                }
        }

        // BATTERY_STATUS
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<BatteryStatus>()
                .collect { b ->
                    val currentA = if (b.currentBattery.toInt() == -1) null else b.currentBattery / 100f
                    _state.update { it.copy(currentA = currentA) }
                }
        }

        // HEARTBEAT
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<Heartbeat>()
                .collect { hb ->
                    val armed = hb.baseMode.any { it == MavModeFlag.SAFETY_ARMED }

                    // ArduPilot custom modes
                    val mode = when (hb.customMode) {
                        0u -> "Stabilize"
                        1u -> "Acro"
                        2u -> "Alt Hold"
                        3u -> "Auto"
                        4u -> "Guided"
                        5u -> "Loiter"
                        6u -> "RTL"
                        7u -> "Circle"
                        9u -> "Land"
                        11u -> "Drift"
                        13u -> "Sport"
                        14u -> "Flip"
                        15u -> "AutoTune"
                        16u -> "Pos Hold"
                        17u -> "Brake"
                        18u -> "Throw"
                        19u -> "Avoid_ADSB"
                        20u -> "Guided_NoGPS"
                        21u -> "Smart_RTL"
                        22u -> "FlowHold"
                        23u -> "Follow"
                        24u -> "ZigZag"
                        25u -> "SystemID"
                        26u -> "AutoRotate"
                        27u -> "Auto_RTL"
                        else -> "Unknown"
                    }
                    _state.update { it.copy(armed = armed, mode = mode) }
                }
        }


        // SYS_STATUS
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<SysStatus>()
                .collect { s ->
                    val vBatt = if (s.voltageBattery.toUInt() == 0xFFFFu) null else s.voltageBattery.toFloat() / 1000f
                    val pct = if (s.batteryRemaining.toInt() == -1) null else s.batteryRemaining.toInt()
                    val armable = s.onboardControlSensorsPresent.any { it == MavSysStatus.SENSOR_3D_GYRO } &&
                            s.onboardControlSensorsEnabled.any { it == MavSysStatus.SENSOR_3D_GYRO } &&
                            s.onboardControlSensorsHealth.any { it == MavSysStatus.SENSOR_3D_GYRO }
                    _state.update { it.copy(voltage = vBatt, batteryPercent = pct, armable = armable) }
                }
        }

        // GPS_RAW_INT
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GpsRawInt>()
                .collect { gps ->
                    val sats = gps.satellitesVisible.toInt().takeIf { it >= 0 }
                    val hdop = if (gps.eph.toUInt() == 0xFFFFu) null else gps.eph.toFloat() / 100f
                    _state.update { it.copy(sats = sats, hdop = hdop) }
                }
        }
    }

    private suspend fun sendCommand(command: MavCmd, param1: Float = 0f, param2: Float = 0f, param3: Float = 0f, param4: Float = 0f, param5: Float = 0f, param6: Float = 0f, param7: Float = 0f) {
        val commandLong = CommandLong(
            targetSystem = fcuSystemId,
            targetComponent = fcuComponentId,
            command = command.wrap(),
            confirmation = 0u,
            param1 = param1,
            param2 = param2,
            param3 = param3,
            param4 = param4,
            param5 = param5,
            param6 = param6,
            param7 = param7
        )
        try {
            connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, commandLong)
        } catch (e: Exception) {
            Log.e("MavlinkRepo", "Failed to send command", e)
            _lastFailure.value = e
        }
    }

    suspend fun arm() {
        if (state.value.armable) {
            sendCommand(MavCmd.COMPONENT_ARM_DISARM, 1f)
        } else {
            Log.w("MavlinkRepo", "Arm command rejected, vehicle not armable")
        }
    }

    suspend fun disarm() {
        sendCommand(MavCmd.COMPONENT_ARM_DISARM, 0f)
    }

    suspend fun changeMode(mode: MavMode) {
        sendCommand(MavCmd.DO_SET_MODE, mode.value.toFloat(), 0f)
    }

    suspend fun takeoff(altitude: Float) {
        sendCommand(MavCmd.NAV_TAKEOFF, -1f, 0f, 0f, 0f, 0f, 0f, altitude)
    }

    suspend fun land() {
        sendCommand(MavCmd.NAV_LAND)
    }
}
