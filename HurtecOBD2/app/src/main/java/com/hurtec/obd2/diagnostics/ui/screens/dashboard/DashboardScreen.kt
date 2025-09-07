package com.hurtec.obd2.diagnostics.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hurtec.obd2.diagnostics.ui.theme.*
import kotlin.math.*

/**
 * Modern Dashboard Screen with animated gauges
 * Replaces the old fragment-based dashboard with Jetpack Compose
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with connection status
        DashboardHeader(
            connectionState = uiState.connectionState,
            onConnectClick = { navController.navigate("connection") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Bar
        QuickActionsBar(
            onScanCodes = { navController.navigate("diagnostics") },
            onClearCodes = { /* TODO: Clear codes */ },
            onRecordTrip = { navController.navigate("trips") },
            onFreezeFrame = { navController.navigate("livedata") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gauges grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.gaugeData) { gauge ->
                AnimatedGaugeCard(
                    gauge = gauge,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.CONNECTED -> StatusConnected.copy(alpha = 0.1f)
                ConnectionState.CONNECTING -> StatusWarning.copy(alpha = 0.1f)
                ConnectionState.ERROR -> StatusError.copy(alpha = 0.1f)
                else -> StatusDisconnected.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hurtec OBD-II",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            ConnectionState.CONNECTED -> Icons.Filled.CheckCircle
                            ConnectionState.CONNECTING -> Icons.Filled.Sync
                            ConnectionState.ERROR -> Icons.Filled.Error
                            else -> Icons.Filled.BluetoothDisabled
                        },
                        contentDescription = null,
                        tint = when (connectionState) {
                            ConnectionState.CONNECTED -> StatusConnected
                            ConnectionState.CONNECTING -> StatusWarning
                            ConnectionState.ERROR -> StatusError
                            else -> StatusDisconnected
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = connectionState.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> StatusConnected
                            ConnectionState.CONNECTING -> StatusWarning
                            ConnectionState.ERROR -> StatusError
                            else -> StatusDisconnected
                        }
                    )
                }
            }
            
            if (connectionState != ConnectionState.CONNECTED) {
                FilledTonalButton(
                    onClick = onConnectClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
fun AnimatedGaugeCard(
    gauge: GaugeData,
    modifier: Modifier = Modifier
) {
    // Animate the gauge value
    val animatedValue by animateFloatAsState(
        targetValue = gauge.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gauge_value"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gauge background and arc
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawGauge(
                    value = animatedValue,
                    minValue = gauge.minValue,
                    maxValue = gauge.maxValue,
                    color = gauge.color,
                    warningThreshold = gauge.warningThreshold,
                    criticalThreshold = gauge.criticalThreshold
                )
            }
            
            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = gauge.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = String.format("%.1f", animatedValue),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = gauge.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Draw the gauge arc and indicators
 */
fun DrawScope.drawGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    color: Color,
    warningThreshold: Float? = null,
    criticalThreshold: Float? = null
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = minOf(size.width, size.height) / 2 - 20.dp.toPx()
    val strokeWidth = 12.dp.toPx()
    
    val startAngle = 135f
    val sweepAngle = 270f
    
    // Background arc
    drawArc(
        color = GaugeBackground,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
    
    // Value arc
    val valueAngle = ((value - minValue) / (maxValue - minValue)) * sweepAngle
    val gaugeColor = when {
        criticalThreshold != null && value >= criticalThreshold -> GaugeCritical
        warningThreshold != null && value >= warningThreshold -> GaugeWarning
        else -> color
    }
    
    drawArc(
        color = gaugeColor,
        startAngle = startAngle,
        sweepAngle = valueAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

/**
 * Connection states
 */
enum class ConnectionState(val displayName: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    ERROR("Connection Error")
}

/**
 * Gauge data model
 */
data class GaugeData(
    val id: String,
    val name: String,
    val value: Float,
    val unit: String,
    val minValue: Float,
    val maxValue: Float,
    val color: Color,
    val warningThreshold: Float? = null,
    val criticalThreshold: Float? = null
)

@Composable
fun QuickActionsBar(
    onScanCodes: () -> Unit,
    onClearCodes: () -> Unit,
    onRecordTrip: () -> Unit,
    onFreezeFrame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.BugReport,
                    label = "Scan Codes",
                    onClick = onScanCodes
                )

                QuickActionButton(
                    icon = Icons.Default.Clear,
                    label = "Clear Codes",
                    onClick = onClearCodes
                )

                QuickActionButton(
                    icon = Icons.Default.DirectionsCar,
                    label = "Trip History",
                    onClick = onRecordTrip
                )

                QuickActionButton(
                    icon = Icons.Default.Timeline,
                    label = "Live Data",
                    onClick = onFreezeFrame
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
