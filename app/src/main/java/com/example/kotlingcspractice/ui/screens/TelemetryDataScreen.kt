package com.example.kotlingcspractice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlingcspractice.Telemetry.TelemetryState
import com.example.kotlingcspractice.telemetry.TelemetryViewModel
import com.example.kotlingcspractice.ui.components.TelemetryRow
import com.example.kotlingcspractice.ui.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryDataScreen(
    viewModel: TelemetryViewModel,
    onNavigateToQuickActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetryState by viewModel.telemetry.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Telemetry Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(telemetryState)

            // Telemetry Data Card
            TelemetryDataCard(telemetryState)

            // Quick Actions Button
            Button(
                onClick = onNavigateToQuickActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Quick Actions Page", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: TelemetryState) {
    val isConnected = state.connected
    val statusColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935) // Green : Red
    val backgroundColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // Light green : Light red

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator circle
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = statusColor
                ) {}

                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isConnected) "Connected" else "Disconnected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun TelemetryDataCard(state: TelemetryState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Flight Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            TelemetryRow(
                label = "Arm Status",
                value = formatBooleanYesNo(state.armed)
            )

            TelemetryRow(
                label = "Latitude",
                value = formatNullableDouble(state.latitude)
            )

            TelemetryRow(
                label = "Longitude",
                value = formatNullableDouble(state.longitude)
            )

            TelemetryRow(
                label = "Altitude (Rel)",
                value = formatNullableFloat(state.altitudeRelative, "m")
            )

            HorizontalDivider()

            Text(
                text = "Power Systems",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            TelemetryRow(
                label = "Battery",
                value = state.batteryPercent?.let { "$it%" } ?: "—"
            )

            TelemetryRow(
                label = "Voltage",
                value = formatNullableFloat(state.voltage, "V")
            )

            HorizontalDivider()

            Text(
                text = "GPS Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            TelemetryRow(
                label = "HDOP",
                value = formatNullableFloat(state.hdop)
            )

            TelemetryRow(
                label = "Satellite Count",
                value = formatNullableInt(state.sats)
            )

            HorizontalDivider()

            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            TelemetryRow(
                label = "Mode",
                value = state.mode ?: "—"
            )
        }
    }
}
