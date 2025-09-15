package com.hurtec.obd2.diagnostics.ui.screens.diagnostics

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    navController: NavController,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Vehicle Diagnostics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scan and clear diagnostic trouble codes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = viewModel::loadDtcs,
                enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isLoading) "Scanning..." else "Scan DTCs")
            }

            OutlinedButton(
                onClick = viewModel::clearDtcs,
                enabled = !uiState.isClearing && uiState.dtcs.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isClearing) "Clearing..." else "Clear DTCs")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Clear success message
        if (uiState.clearSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "DTCs cleared successfully!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Simple DTC List
        if (uiState.dtcs.isNotEmpty()) {
            Text(
                text = "Found ${uiState.dtcs.size} Diagnostic Trouble Codes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.dtcs) { dtc ->
                    SimpleDtcCard(dtc = dtc)
                }
            }
        } else if (!uiState.isLoading) {
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
                        .padding(32.dp),
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
                        text = "No DTCs Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your vehicle has no diagnostic trouble codes. Click 'Scan DTCs' to check for new codes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
private fun SimpleDtcCard(dtc: DtcInfo) {
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = dtc.code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (dtc.status) {
                    DtcStatus.STORED -> MaterialTheme.colorScheme.onErrorContainer
                    DtcStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                    DtcStatus.PERMANENT -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            val description = dtc.description ?: getDtcDescription(dtc.code)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = when (dtc.status) {
                    DtcStatus.STORED -> MaterialTheme.colorScheme.onErrorContainer
                    DtcStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                    DtcStatus.PERMANENT -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Status: ${dtc.status.name}",
                style = MaterialTheme.typography.labelSmall,
                color = when (dtc.status) {
                    DtcStatus.STORED -> MaterialTheme.colorScheme.onErrorContainer
                    DtcStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                    DtcStatus.PERMANENT -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}
