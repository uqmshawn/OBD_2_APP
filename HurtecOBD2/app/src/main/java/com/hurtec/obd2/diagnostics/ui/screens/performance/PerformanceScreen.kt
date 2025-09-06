package com.hurtec.obd2.diagnostics.ui.screens.performance

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * Performance Monitoring Screen with acceleration tests and fuel economy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    navController: NavController,
    viewModel: PerformanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Text(
                text = "Performance Monitor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            // Performance Tests
            PerformanceTestsCard(
                uiState = uiState,
                onStart060Test = { viewModel.start060Test() },
                onStartQuarterMileTest = { viewModel.startQuarterMileTest() },
                onStopTest = { viewModel.stopCurrentTest() }
            )
        }
        
        item {
            // Trip Computer
            TripComputerCard(
                tripData = uiState.currentTrip,
                onResetTrip = { viewModel.resetTrip() },
                onStartTrip = { viewModel.startTrip() }
            )
        }
        
        item {
            // Fuel Economy
            FuelEconomyCard(
                fuelData = uiState.fuelEconomy,
                onResetFuelData = { viewModel.resetFuelEconomy() }
            )
        }
        
        item {
            // Power/Torque Estimates
            PowerTorqueCard(
                powerData = uiState.powerEstimates
            )
        }
        
        item {
            // Performance History
            PerformanceHistoryCard(
                history = uiState.performanceHistory
            )
        }
    }
}

@Composable
fun PerformanceTestsCard(
    uiState: PerformanceUiState,
    onStart060Test: () -> Unit,
    onStartQuarterMileTest: () -> Unit,
    onStopTest: () -> Unit
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
                text = "Acceleration Tests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current test display
            if (uiState.currentTest != null) {
                CurrentTestDisplay(
                    test = uiState.currentTest,
                    onStop = onStopTest
                )
            } else {
                // Test buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TestButton(
                        title = "0-60 mph",
                        subtitle = "Acceleration Test",
                        icon = Icons.Default.Speed,
                        onClick = onStart060Test
                    )
                    
                    TestButton(
                        title = "1/4 Mile",
                        subtitle = "Quarter Mile",
                        icon = Icons.Default.Timeline,
                        onClick = onStartQuarterMileTest
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Best times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BestTimeDisplay(
                    label = "Best 0-60",
                    time = uiState.best060Time,
                    unit = "sec"
                )
                
                BestTimeDisplay(
                    label = "Best 1/4 Mile",
                    time = uiState.bestQuarterMileTime,
                    unit = "sec"
                )
            }
        }
    }
}

@Composable
fun CurrentTestDisplay(
    test: PerformanceTest,
    onStop: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = test.type.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Timer display
        Text(
            text = "%.2f".format(test.currentTime),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "seconds",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current speed
        Text(
            text = "${test.currentSpeed.toInt()} mph",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop Test")
        }
    }
}

@Composable
fun TestButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BestTimeDisplay(
    label: String,
    time: Float?,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = time?.let { "%.2f $unit".format(it) } ?: "--",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TripComputerCard(
    tripData: TripData,
    onResetTrip: () -> Unit,
    onStartTrip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = "Trip Computer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    if (!tripData.isActive) {
                        TextButton(onClick = onStartTrip) {
                            Text("Start Trip")
                        }
                    }
                    
                    TextButton(onClick = onResetTrip) {
                        Text("Reset")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trip metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripMetric(
                    label = "Distance",
                    value = "%.1f".format(tripData.distance),
                    unit = "miles"
                )
                
                TripMetric(
                    label = "Duration",
                    value = formatDuration(tripData.duration),
                    unit = ""
                )
                
                TripMetric(
                    label = "Avg Speed",
                    value = "%.1f".format(tripData.averageSpeed),
                    unit = "mph"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripMetric(
                    label = "Max Speed",
                    value = "%.1f".format(tripData.maxSpeed),
                    unit = "mph"
                )
                
                TripMetric(
                    label = "Fuel Used",
                    value = "%.2f".format(tripData.fuelUsed),
                    unit = "gal"
                )
                
                TripMetric(
                    label = "Trip MPG",
                    value = "%.1f".format(tripData.fuelEconomy),
                    unit = "mpg"
                )
            }
        }
    }
}

@Composable
fun TripMetric(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FuelEconomyCard(
    fuelData: FuelEconomyData,
    onResetFuelData: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    text = "Fuel Economy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                TextButton(onClick = onResetFuelData) {
                    Text("Reset")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FuelMetric(
                    label = "Current MPG",
                    value = "%.1f".format(fuelData.currentMpg),
                    unit = "mpg",
                    color = MaterialTheme.colorScheme.primary
                )
                
                FuelMetric(
                    label = "Average MPG",
                    value = "%.1f".format(fuelData.averageMpg),
                    unit = "mpg",
                    color = MaterialTheme.colorScheme.secondary
                )
                
                FuelMetric(
                    label = "Range",
                    value = "${fuelData.estimatedRange.toInt()}",
                    unit = "miles",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun FuelMetric(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun PowerTorqueCard(
    powerData: PowerEstimates
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Power & Torque Estimates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PowerMetric(
                    label = "Horsepower",
                    value = "${powerData.horsepower.toInt()}",
                    unit = "hp"
                )
                
                PowerMetric(
                    label = "Torque",
                    value = "${powerData.torque.toInt()}",
                    unit = "lb-ft"
                )
                
                PowerMetric(
                    label = "Power/Weight",
                    value = "%.2f".format(powerData.powerToWeight),
                    unit = "hp/lb"
                )
            }
        }
    }
}

@Composable
fun PowerMetric(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun PerformanceHistoryCard(
    history: List<PerformanceRecord>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (history.isEmpty()) {
                Text(
                    text = "No performance records yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                history.take(5).forEach { record ->
                    PerformanceRecordItem(record = record)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PerformanceRecordItem(
    record: PerformanceRecord
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = record.testType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = formatDate(record.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "%.2f sec".format(record.time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
