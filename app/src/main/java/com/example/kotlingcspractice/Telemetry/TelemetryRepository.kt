package com.example.kotlingcspractice.Telemetry

import com.divpundir.mavlink.adapters.coroutines.asCoroutine
import com.divpundir.mavlink.adapters.coroutines.tryConnect
import com.divpundir.mavlink.adapters.coroutines.trySendUnsignedV2
import com.divpundir.mavlink.api.wrap
import com.divpundir.mavlink.connection.StreamState
import com.divpundir.mavlink.connection.tcp.TcpClientMavConnection
import com.divpundir.mavlink.definitions.common.BatteryStatus
import com.divpundir.mavlink.definitions.common.CommandLong
import com.divpundir.mavlink.definitions.common.CommonDialect
import com.divpundir.mavlink.definitions.common.GlobalPositionInt
import com.divpundir.mavlink.definitions.common.GpsRawInt
import com.divpundir.mavlink.definitions.common.MavCmd
import com.divpundir.mavlink.definitions.common.SysStatus
import com.divpundir.mavlink.definitions.common.VfrHud
import com.divpundir.mavlink.definitions.minimal.Heartbeat
import com.divpundir.mavlink.definitions.minimal.MavAutopilot
import com.divpundir.mavlink.definitions.minimal.MavModeFlag
import android.util.Log
import com.divpundir.mavlink.definitions.minimal.MavType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MavlinkTelemetryRepository(
    private val host : String = "10.0.2.2",
    private val port : Int = 5762,
    private val gcsSystemId : UByte = 200u,
    private val gcsComponentId : UByte = 1u
){
    private val _state = MutableStateFlow(TelemetryState())
    val state : StateFlow<TelemetryState> = _state.asStateFlow()

    private var fcuSystemId: UByte = 0u
    private var fcuComponentId: UByte = 0u

    //Diagnostic info
    val lastFailure : StateFlow<Throwable> get() = _lastFailure.asStateFlow() as StateFlow<Throwable>
    private val _lastFailure = MutableStateFlow<Throwable?>(null)

    //connection

    private val connection = TcpClientMavConnection(host,port, CommonDialect).asCoroutine()

    fun start(scope: CoroutineScope){


        suspend fun reconnect(scope: CoroutineScope) {
            while (scope.isActive) {
                try {
                    if (connection.tryConnect(scope)) {
                        return // Exit on successful connection
                    }
                } catch (e: Exception) {
                    Log.e("MavlinkTelemetryRepository", "Connection attempt failed", e)
                    _lastFailure.value = e
                }
                delay(1000)
            }
        }

        //Keep connected flag in sync and manage reconnects
        scope.launch{
            reconnect(this) // Initial connection attempt
            connection.streamState.collect {
                    st->
                when(st){
                    is StreamState.Active -> {
                        if (!state.value.connected) {
                            Log.i("MavlinkTelemetryRepository", "Connection Active")
                            _state.update { it.copy(connected = true) }
                        }
                    }
                    is StreamState.Inactive -> {
                        if (state.value.connected) {
                            Log.i("MavlinkTelemetryRepository", "Connection Inactive, attempting to reconnect...")
                            _state.update { it.copy(connected = false, fcuDetected = false) }
                            reconnect(this)
                        }
                    }
                }
            }
        }

        // GCS Heartbeat
        scope.launch{
            val heartbeat = Heartbeat(
                type = MavType.GCS.wrap(),
                autopilot = MavAutopilot.INVALID.wrap(),
                baseMode = emptyList<MavModeFlag>().wrap(),
                customMode = 0u,
                mavlinkVersion = 3u
            )
            while(isActive){
                if (state.value.connected) {
                    try {
                        connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, heartbeat)
                    } catch (e: Exception) {
                        Log.e("MavlinkTelemetryRepository", "Failed to send heartbeat", e)
                        _lastFailure.value = e
                        // This might indicate the connection is dead, the streamState collector will handle it
                    }
                }
                delay(1000)
            }
        }

        //Message Rates
        // Collecting the messages from the FCU

        val mavFrameStream = connection.mavFrame
            .shareIn(scope, SharingStarted.Eagerly, replay = 0)

        scope.launch {
            mavFrameStream.collect {
                Log.d("MavlinkTelemetryRepository", "Raw Frame: ${it.message.javaClass.simpleName} (sysId: ${it.systemId}, compId: ${it.componentId})")
            }
        }

        scope.launch {
            mavFrameStream
                .filter { it.message is Heartbeat && (it.message as Heartbeat).type != MavType.GCS }
                .collect{
                    if(!state.value.fcuDetected){
                        fcuSystemId = it.systemId
                        fcuComponentId = it.componentId
                        Log.i("MavlinkTelemetryRepository", "FCU detected with sysId: $fcuSystemId, compId: $fcuComponentId")
                        _state.update { it.copy(fcuDetected = true) }

                        launch {
                            suspend fun setMessageRate(messageId: UInt,hz: Float){
                                val intervalUsec = if(hz <= 0f) 0f else (1_000_000f / hz)
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
                                    Log.e("MavlinkTelemetryRepository", "Failed to send SET_MESSAGE_INTERVAL", e)
                                    _lastFailure.value = e
                                }
                            }
                            //Set rates here
                            setMessageRate(1u,1f) // SYS_STATUS
                            setMessageRate(24u,1f) // GPS_RAW_INT
                            setMessageRate(33u,5f) // GLOBAL_POSITION_INT
                            setMessageRate(74u,5f) // VFR_HUD
                            setMessageRate(147u,1f) // BATTERY_STATUS
                        }
                    }
                }
        }
        //VFR_HUD for alt and speed
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<VfrHud>()
                .collect { hud->
                    Log.d("MavlinkTelemetryRepository", "Received VFR_HUD: $hud")
                    _state.update{
                        it.copy(
                            altitudeMsl = hud.alt,
                            airspeed = hud.airspeed.takeIf { v->v>0f },
                            groundspeed = hud.groundspeed.takeIf { v -> v> 0f }
                        )
                    }
                }
        }

        // GLOBAL_POSITION_INT for relative alt
        scope.launch {
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GlobalPositionInt>()
                .collect{ gp->
                    Log.d("MavlinkTelemetryRepository", "Received GLOBAL_POSITION_INT: $gp")
                    val altAMSLm = gp.alt / 1000f
                    val relAltM = gp.relativeAlt / 1000f
                    _state.update{ it.copy(altitudeMsl = altAMSLm , altitudeRelative = relAltM) }
                }
        }

        // BATTERY_STATUS for battery info
        scope.launch{
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<BatteryStatus>()
                .collect { b ->
                    Log.d("MavlinkTelemetryRepository", "Received BATTERY_STATUS: $b")
                    val currentA =
                        if (b.currentBattery.toInt() == -1) null else b.currentBattery / 100f
                    _state.update{
                        it.copy(currentA = currentA)
                    }
                }
        }

        // SYS_STATUS for voltage and battery percent
        scope.launch{
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<SysStatus>()
                .collect { s ->
                    Log.d("MavlinkTelemetryRepository", "Received SYS_STATUS: $s")
                    val vBatt =
                        if (s.voltageBattery.toUInt() == 0xFFFFu) null else s.voltageBattery.toFloat() / 1000f
                    val pct =
                        if (s.batteryRemaining.toInt() == -1) null else s.batteryRemaining.toInt()
                    _state.update{it.copy(volatage = vBatt , batteryPercent = pct) }

                }
        }

        // GPS_RAW_INT for HDOP and Sat count
        scope.launch{
            mavFrameStream
                .filter { state.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GpsRawInt>()
                .collect { gps ->
                    Log.d("MavlinkTelemetryRepository", "Received GPS_RAW_INT: $gps")
                    val sats = gps.satellitesVisible.toInt().takeIf { it >= 0 }
                    val hdop =
                        if (gps.eph.toUInt() == 0xFFFFu) null else gps.eph.toFloat() / 100f
                    _state.update{it.copy(sats = sats, hdop = hdop) }
                }
        }
    }
}