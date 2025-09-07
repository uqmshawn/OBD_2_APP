package com.hurtec.obd2.diagnostics.ui.screens.diagnostics

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionState
import com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo
import com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus
import com.hurtec.obd2.diagnostics.ui.components.*
import com.hurtec.obd2.diagnostics.ui.viewmodels.DiagnosticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    navController: NavController,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Show success message
    if (uiState.clearSuccess) {
        LaunchedEffect(uiState.clearSuccess) {
            // In a real app, you'd show a snackbar here
            viewModel.clearClearSuccess()
        }
    }

    // Show error message
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you'd show a snackbar here
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Vehicle Diagnostics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status check
        if (connectionState != ConnectionState.CONNECTED) {
            ConnectionRequiredCard()
            return@Column
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::scanDtcs,
                enabled = !uiState.isScanning,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isScanning) {
                    WaveLoadingIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isScanning) "Scanning..." else "Scan DTCs")
            }

            OutlinedButton(
                onClick = viewModel::clearDtcs,
                enabled = !uiState.isClearing && uiState.dtcs.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isClearing) {
                    RotatingLoadingIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isClearing) "Clearing..." else "Clear DTCs")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DTC Summary
        if (uiState.dtcs.isNotEmpty()) {
            val activeDtcs = uiState.dtcs.filter { it.status == DtcStatus.STORED }
            val pendingDtcs = uiState.dtcs.filter { it.status == DtcStatus.PENDING }
            val permanentDtcs = uiState.dtcs.filter { it.status == DtcStatus.PERMANENT }

            DtcSummaryCard(
                activeDtcs = activeDtcs.size,
                pendingDtcs = pendingDtcs.size,
                permanentDtcs = permanentDtcs.size
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // DTC List or Empty State
        if (uiState.dtcs.isNotEmpty()) {
            val activeDtcs = uiState.dtcs.filter { it.status == DtcStatus.STORED }
            val pendingDtcs = uiState.dtcs.filter { it.status == DtcStatus.PENDING }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active DTCs
                if (activeDtcs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Codes (${activeDtcs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    itemsIndexed(activeDtcs) { index, dtc ->
                        SlideInFromBottomTransition(visible = true) {
                            AnimatedCard(
                                isVisible = true,
                                delayMillis = index * 150
                            ) {
                                DtcCard(
                                    dtc = dtc,
                                    isLoadingFreezeFrame = false,
                                    onGetFreezeFrame = { /* TODO: Get freeze frame data */ }
                                )
                            }
                        }
                    }
                }

                // Pending DTCs
                if (pendingDtcs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Codes (${pendingDtcs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    itemsIndexed(pendingDtcs) { index, dtc ->
                        SlideInFromBottomTransition(visible = true) {
                            AnimatedCard(
                                isVisible = true,
                                delayMillis = (index + activeDtcs.size) * 150
                            ) {
                                DtcCard(
                                    dtc = dtc,
                                    isLoadingFreezeFrame = false,
                                    onGetFreezeFrame = { /* TODO: Get freeze frame data */ }
                                )
                            }
                        }
                    }
                }
            }
        } else if (!uiState.isScanning) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Diagnostic Trouble Codes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Your vehicle's systems are operating normally",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionRequiredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connection Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Please connect to an OBD-II adapter first",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DtcSummaryCard(
    activeDtcs: Int,
    pendingDtcs: Int,
    permanentDtcs: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DtcSummaryItem(
                count = activeDtcs,
                label = "Active",
                color = MaterialTheme.colorScheme.error
            )

            DtcSummaryItem(
                count = pendingDtcs,
                label = "Pending",
                color = MaterialTheme.colorScheme.tertiary
            )

            DtcSummaryItem(
                count = permanentDtcs,
                label = "Permanent",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun DtcSummaryItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DtcCard(
    dtc: DtcInfo,
    isLoadingFreezeFrame: Boolean,
    onGetFreezeFrame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (dtc.status) {
                DtcStatus.STORED -> MaterialTheme.colorScheme.errorContainer
                DtcStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                DtcStatus.PERMANENT -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dtc.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = getDtcDescription(dtc.code),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = dtc.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (dtc.status) {
                            DtcStatus.STORED -> MaterialTheme.colorScheme.error
                            DtcStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                            DtcStatus.PERMANENT -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                OutlinedButton(
                    onClick = onGetFreezeFrame,
                    enabled = !isLoadingFreezeFrame,
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isLoadingFreezeFrame) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Freeze Frame")
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeFrameDialog(
    dtcCode: String,
    freezeFrameData: Map<String, String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Freeze Frame Data - $dtcCode")
        },
        text = {
            LazyColumn {
                items(freezeFrameData.entries.toList()) { (parameter, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = parameter,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ReadinessMonitorsCard(
    monitors: Map<String, ReadinessStatus>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Readiness Monitors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            monitors.entries.forEach { (monitor, status) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monitor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (status) {
                                ReadinessStatus.READY -> Icons.Default.CheckCircle
                                ReadinessStatus.NOT_READY -> Icons.Default.Warning
                                ReadinessStatus.NOT_APPLICABLE -> Icons.Default.Remove
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (status) {
                                ReadinessStatus.READY -> MaterialTheme.colorScheme.primary
                                ReadinessStatus.NOT_READY -> MaterialTheme.colorScheme.error
                                ReadinessStatus.NOT_APPLICABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = when (status) {
                                ReadinessStatus.READY -> "Ready"
                                ReadinessStatus.NOT_READY -> "Not Ready"
                                ReadinessStatus.NOT_APPLICABLE -> "N/A"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                ReadinessStatus.READY -> MaterialTheme.colorScheme.primary
                                ReadinessStatus.NOT_READY -> MaterialTheme.colorScheme.error
                                ReadinessStatus.NOT_APPLICABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getDtcDescription(code: String): String {
    return when (code) {
        "P0171" -> "System Too Lean (Bank 1)"
        "P0300" -> "Random/Multiple Cylinder Misfire Detected"
        "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
        "P0442" -> "Evaporative Emission Control System Leak Detected (small leak)"
        "P0455" -> "Evaporative Emission Control System Leak Detected (large leak)"
        "P0506" -> "Idle Control System RPM Lower Than Expected"
        "P0507" -> "Idle Control System RPM Higher Than Expected"
        else -> "Unknown diagnostic trouble code"
    }
}
