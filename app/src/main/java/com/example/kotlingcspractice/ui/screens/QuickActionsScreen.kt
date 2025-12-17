package com.example.kotlingcspractice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.autoMirrored
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlingcspractice.Telemetry.TelemetryState
import com.example.kotlingcspractice.telemetry.TelemetryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsScreen(
    viewModel: TelemetryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetryState by viewModel.telemetry.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Status Card
            CurrentStatusCard(telemetryState)

            // Action Buttons
            ActionsCard(
                telemetryState = telemetryState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope
            )
        }
    }
}

@Composable
private fun CurrentStatusCard(state: TelemetryState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = state.mode ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Armed:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (state.armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = if (state.armed) "ARMED" else "DISARMED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.armed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionsCard(
    telemetryState: TelemetryState,
    viewModel: TelemetryViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Divider()

            // Arm/Disarm Section
            Text(
                text = "Arming Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (telemetryState.armable && !telemetryState.armed) {
                                viewModel.arm()
                                snackbarHostState.showSnackbar("Arming drone...")
                            } else if (telemetryState.armed) {
                                snackbarHostState.showSnackbar("Drone is already armed")
                            } else {
                                snackbarHostState.showSnackbar("Drone is not armable")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = telemetryState.connected && telemetryState.fcuDetected
                ) {
                    Text("Arm", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (telemetryState.armed) {
                                val altOk = telemetryState.altitudeRelative?.let { it < 0.5f } ?: true
                                if (altOk) {
                                    viewModel.disarm()
                                    snackbarHostState.showSnackbar("Disarming drone...")
                                } else {
                                    snackbarHostState.showSnackbar("Drone must be landed to disarm")
                                }
                            } else {
                                snackbarHostState.showSnackbar("Drone is not armed")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = telemetryState.connected && telemetryState.fcuDetected
                ) {
                    Text("Disarm", style = MaterialTheme.typography.titleMedium)
                }
            }

            Divider()

            // Flight Actions Section
            Text(
                text = "Flight Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = {
                    scope.launch {
                        if (!telemetryState.armed) {
                            snackbarHostState.showSnackbar("Starting takeoff sequence...")
                            viewModel.takeoff()
                        } else {
                            snackbarHostState.showSnackbar("Drone is already armed. Use land command if needed.")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                enabled = telemetryState.connected && telemetryState.fcuDetected && !telemetryState.armed
            ) {
                Text("Takeoff (10m)", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = {
                    scope.launch {
                        if (telemetryState.armed) {
                            snackbarHostState.showSnackbar("Landing...")
                            viewModel.land()
                        } else {
                            snackbarHostState.showSnackbar("Drone is not armed")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                enabled = telemetryState.connected && telemetryState.fcuDetected && telemetryState.armed
            ) {
                Text("Land", style = MaterialTheme.typography.titleMedium)
            }

            Divider()

            // Mode Change Section
            Text(
                text = "Mode Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var showModeDialog by remember { mutableStateOf(false) }

            Button(
                onClick = { showModeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = telemetryState.connected && telemetryState.fcuDetected
            ) {
                Text("Change Mode", style = MaterialTheme.typography.titleMedium)
            }

            if (showModeDialog) {
                ModeSelectionDialog(
                    currentMode = telemetryState.mode ?: "Unknown",
                    onDismiss = { showModeDialog = false },
                    onModeSelected = { mode ->
                        scope.launch {
                            when (mode) {
                                "Stabilize" -> {
                                    snackbarHostState.showSnackbar("Changing to Stabilize mode...")
                                    viewModel.changeToStabilize()
                                }
                                "Guided" -> {
                                    snackbarHostState.showSnackbar("Changing to Guided mode...")
                                    viewModel.changeToGuided()
                                }
                                "Auto" -> {
                                    snackbarHostState.showSnackbar("Changing to Auto mode...")
                                    viewModel.changeToAuto()
                                }
                                "Loiter" -> {
                                    snackbarHostState.showSnackbar("Changing to Loiter mode...")
                                    viewModel.changeToLoiter()
                                }
                            }
                        }
                        showModeDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeSelectionDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Flight Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Current Mode: $currentMode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Divider()
                Text(
                    "Select a new mode:",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onModeSelected("Stabilize") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stabilize Mode")
                }
                TextButton(
                    onClick = { onModeSelected("Guided") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guided Mode")
                }
                TextButton(
                    onClick = { onModeSelected("Auto") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto Mode")
                }
                TextButton(
                    onClick = { onModeSelected("Loiter") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Loiter Mode")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
