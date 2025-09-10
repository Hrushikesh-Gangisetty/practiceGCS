package com.example.kotlingcspractice.Design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kotlingcspractice.Telemetry.TelemetryState

import kotlin.math.round

@Composable
fun TelemetryOverlay(
    state: TelemetryState,
    modifier: Modifier = Modifier,
    onArmClick: () -> Unit,
    onDisarmClick: () -> Unit,
    onChangeModeClick: () -> Unit,
    onTakeoffClick: () -> Unit
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val connText = if (state.connected) "Connected" else "Disconnected"
                val connColor = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(connText, color = connColor)
            }

            TelemetryRow("Mode", state.mode ?: "—")
            TelemetryRow("Armed", if (state.armed) "Yes" else "No")
            TelemetryRow("Armable", if (state.armable) "Yes" else "No")
            TelemetryRow("Altitude (MSL)", state.altitudeMsl?.let { "${fmt(it)} m" } ?: "—")
            TelemetryRow("Altitude (Rel)", state.altitudeRelative?.let { "${fmt(it)} m" } ?: "—")
            TelemetryRow("Airspeed", state.airspeed?.let { "${fmt(it)} m/s" } ?: "—")
            TelemetryRow("Groundspeed", state.groundspeed?.let { "${fmt(it)} m/s" } ?: "—")
            TelemetryRow("Voltage", state.voltage?.let { "${fmt(it)} V" } ?: "—")
            TelemetryRow("Battery", state.batteryPercent?.let { "$it%" } ?: "—")
            TelemetryRow("Current", state.currentA?.let { "${fmt(it)} A" } ?: "—")
            TelemetryRow("Satellites", state.sats?.toString() ?: "—")
            TelemetryRow("HDOP", state.hdop?.let { fmt(it) } ?: "—")
            TelemetryRow("Latitude", state.latitude?.let { fmt(it.toFloat(), 6) } ?: "—")
            TelemetryRow("Longitude", state.longitude?.let { fmt(it.toFloat(), 6) } ?: "—")

            // Control Buttons
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Controls",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // First row of buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onChangeModeClick() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Change Mode")
                }

                Button(
                    onClick = { onArmClick() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Arm")
                }
            }

            // Second row of buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDisarmClick() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disarm")
                }

                Button(
                    onClick = { onTakeoffClick() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Takeoff")
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun fmt(v: Float, places: Int = 2): String {
    val factor = 10f.pow(places)
    return (kotlin.math.round(v * factor) / factor).toString()
}

private fun Float.pow(p: Int): Float = Math.pow(this.toDouble(), p.toDouble()).toFloat()