package com.hurtec.obd2.diagnostics.ui.screens.system

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * System information and performance monitoring screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfoScreen(
    navController: NavController,
    viewModel: SystemInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val memoryInfo by viewModel.memoryInfo.collectAsState()
    val hardwareCapabilities by viewModel.hardwareCapabilities.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Memory Usage Card
        item {
            MemoryUsageCard(
                memoryInfo = memoryInfo,
                onOptimize = viewModel::optimizeMemory
            )
        }

        // Hardware Capabilities Card
        item {
            HardwareCapabilitiesCard(
                capabilities = hardwareCapabilities,
                onStartMonitoring = viewModel::startHardwareMonitoring,
                onStopMonitoring = viewModel::stopHardwareMonitoring
            )
        }

        // Performance Metrics
        item {
            PerformanceMetricsCard(
                metrics = uiState.performanceMetrics
            )
        }

        // System Details
        item {
            SystemDetailsCard(
                details = uiState.systemDetails
            )
        }

        // Actions
        item {
            SystemActionsCard(
                onForceGC = viewModel::forceGarbageCollection,
                onClearCache = viewModel::clearCache,
                onOptimizePerformance = viewModel::optimizePerformance
            )
        }
    }
}

@Composable
fun MemoryUsageCard(
    memoryInfo: com.hurtec.obd2.diagnostics.utils.MemoryInfo,
    onOptimize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (memoryInfo.isLowMemory) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Memory Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (memoryInfo.isLowMemory) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Low Memory",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memory progress bar
            MemoryProgressBar(
                percentage = memoryInfo.memoryPercentage,
                isLowMemory = memoryInfo.isLowMemory
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Memory details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Used: ${formatBytes(memoryInfo.usedMemory)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Available: ${formatBytes(memoryInfo.availableMemory)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column {
                    Text(
                        text = "Heap: ${formatBytes(memoryInfo.heapUsed)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Native: ${formatBytes(memoryInfo.nativeHeapAllocated)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (memoryInfo.isLowMemory || memoryInfo.memoryPercentage > 80) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onOptimize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimize Memory")
                }
            }
        }
    }
}

@Composable
fun MemoryProgressBar(
    percentage: Int,
    isLowMemory: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "memory_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background
            drawRect(
                color = Color.Gray.copy(alpha = 0.3f),
                size = size
            )

            // Progress
            val progressColor = when {
                isLowMemory -> Color.Red
                percentage > 80 -> Color(0xFFFF9800) // Orange
                percentage > 60 -> Color.Yellow
                else -> Color.Green
            }

            drawRect(
                color = progressColor,
                size = Size(size.width * animatedProgress, size.height)
            )
        }

        // Percentage text
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HardwareCapabilitiesCard(
    capabilities: com.hurtec.obd2.diagnostics.hardware.HardwareCapabilities,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hardware Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware features grid
            val features = listOf(
                "Bluetooth" to capabilities.hasAccelerometer,
                "GPS" to capabilities.hasGPS,
                "Accelerometer" to capabilities.hasAccelerometer,
                "Gyroscope" to capabilities.hasGyroscope,
                "Magnetometer" to capabilities.hasMagnetometer,
                "Barometer" to capabilities.hasBarometer,
                "Vibrator" to capabilities.hasVibrator
            )

            features.chunked(2).forEach { rowFeatures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowFeatures.forEach { (name, available) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (available) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (available) Color.Green else Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device info
            Text(
                text = "Device: ${capabilities.deviceModel}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Android: ${capabilities.androidVersion}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "CPU Cores: ${capabilities.cpuCores}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Memory: ${capabilities.totalMemoryMB} MB",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Monitoring controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartMonitoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Monitoring")
                }
                
                OutlinedButton(
                    onClick = onStopMonitoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Monitoring")
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricsCard(
    metrics: PerformanceMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("FPS", "${metrics.fps}")
                MetricItem("Frame Time", "${metrics.frameTimeMs}ms")
                MetricItem("CPU Usage", "${metrics.cpuUsage}%")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SystemDetailsCard(
    details: SystemDetails
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            details.items.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun SystemActionsCard(
    onForceGC: () -> Unit,
    onClearCache: () -> Unit,
    onOptimizePerformance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val actions = listOf(
                Triple("Force GC", Icons.Default.CleaningServices, onForceGC),
                Triple("Clear Cache", Icons.Default.ClearAll, onClearCache),
                Triple("Optimize", Icons.Default.Speed, onOptimizePerformance)
            )

            actions.forEach { (title, icon, action) ->
                OutlinedButton(
                    onClick = action,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Helper function
private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.1f %s".format(size, units[unitIndex])
}

// Data classes will be moved to ViewModel file
