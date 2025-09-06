package com.hurtec.obd2.diagnostics.ui.screens.livedata

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * Live Data Monitoring Screen with real-time graphs and data logging
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    navController: NavController,
    viewModel: LiveDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Data Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (uiState.isRecording) "Recording..." else "Monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isRecording) 
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recording control
                IconButton(
                    onClick = { 
                        if (uiState.isRecording) viewModel.stopRecording() 
                        else viewModel.startRecording() 
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (uiState.isRecording) "Stop recording" else "Start recording",
                        tint = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                
                // Export data
                IconButton(
                    onClick = { viewModel.exportData() }
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export data"
                    )
                }
                
                // Settings
                IconButton(
                    onClick = { /* TODO: Open settings */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Parameter selection chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.availableParameters) { parameter ->
                FilterChip(
                    onClick = { viewModel.toggleParameter(parameter.id) },
                    label = { Text(parameter.name) },
                    selected = parameter.id in uiState.selectedParameters,
                    leadingIcon = if (parameter.id in uiState.selectedParameters) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Main content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real-time graphs
            items(uiState.selectedParameters) { parameterId ->
                val parameter = uiState.availableParameters.find { it.id == parameterId }
                val data = uiState.parameterData[parameterId] ?: emptyList()
                
                parameter?.let {
                    LiveDataGraph(
                        parameter = it,
                        data = data,
                        isRealTime = true
                    )
                }
            }
            
            // Data table
            if (uiState.showDataTable) {
                item {
                    DataTable(
                        parameters = uiState.selectedParameters.mapNotNull { id ->
                            uiState.availableParameters.find { it.id == id }
                        },
                        data = uiState.recentData
                    )
                }
            }
        }
    }
}

@Composable
fun LiveDataGraph(
    parameter: LiveDataParameter,
    data: List<DataPoint>,
    isRealTime: Boolean = false
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = parameter.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "${data.lastOrNull()?.value?.let { "%.1f".format(it) } ?: "--"} ${parameter.unit}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.isNotEmpty()) {
                        Text(
                            text = "Min: ${"%.1f".format(data.minOf { it.value })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Max: ${"%.1f".format(data.maxOf { it.value })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (data.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val width = size.width
                        val height = size.height
                        val padding = 20f
                        
                        val minValue = data.minOf { it.value }
                        val maxValue = data.maxOf { it.value }
                        val valueRange = maxValue - minValue
                        
                        if (valueRange > 0) {
                            val path = Path()
                            
                            data.forEachIndexed { index, point ->
                                val x = padding + (index.toFloat() / (data.size - 1)) * (width - 2 * padding)
                                val y = height - padding - ((point.value - minValue) / valueRange) * (height - 2 * padding)
                                
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            
                            drawPath(
                                path = path,
                                color = parameter.color,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Draw data points
                            data.forEachIndexed { index, point ->
                                val x = padding + (index.toFloat() / (data.size - 1)) * (width - 2 * padding)
                                val y = height - padding - ((point.value - minValue) / valueRange) * (height - 2 * padding)
                                
                                drawCircle(
                                    color = parameter.color,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                } else {
                    // No data placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataTable(
    parameters: List<LiveDataParameter>,
    data: List<Map<String, Float>>
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
            Text(
                text = "Recent Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Table header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                parameters.forEach { parameter ->
                    Text(
                        text = parameter.shortName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Table data
            data.takeLast(10).forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${10 - index}s ago",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    
                    parameters.forEach { parameter ->
                        Text(
                            text = row[parameter.id]?.let { "%.1f".format(it) } ?: "--",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (index < data.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
