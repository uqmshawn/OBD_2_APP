package com.hurtec.obd2.diagnostics.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show success messages
    if (uiState.exportSuccess) {
        LaunchedEffect(uiState.exportSuccess) {
            // In a real app, you'd show a snackbar here
            viewModel.clearExportSuccess()
        }
    }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Connection Settings
        item {
            SettingsSection(
                title = "Connection",
                icon = Icons.Default.Bluetooth
            ) {
                SwitchSetting(
                    title = "Auto Connect",
                    description = "Automatically connect to last used adapter",
                    checked = uiState.autoConnect,
                    onCheckedChange = viewModel::updateAutoConnect
                )
            }
        }

        // Display Settings
        item {
            SettingsSection(
                title = "Display",
                icon = Icons.Default.Phone
            ) {
                SwitchSetting(
                    title = "Keep Screen On",
                    description = "Prevent screen from turning off during diagnostics",
                    checked = uiState.keepScreenOn,
                    onCheckedChange = viewModel::updateKeepScreenOn
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DropdownSetting(
                    title = "Theme",
                    description = "Choose app appearance",
                    currentValue = uiState.theme.displayName,
                    options = AppTheme.values().map { it.displayName },
                    onValueChange = { displayName ->
                        val theme = AppTheme.values().find { it.displayName == displayName }
                        theme?.let { viewModel.updateTheme(it) }
                    }
                )
            }
        }

        // Units Settings
        item {
            SettingsSection(
                title = "Units",
                icon = Icons.Default.Straighten
            ) {
                DropdownSetting(
                    title = "Temperature",
                    description = "Temperature display unit",
                    currentValue = uiState.temperatureUnit.displayName,
                    options = TemperatureUnit.values().map { it.displayName },
                    onValueChange = { displayName ->
                        val unit = TemperatureUnit.values().find { it.displayName == displayName }
                        unit?.let { viewModel.updateTemperatureUnit(it) }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DropdownSetting(
                    title = "Distance",
                    description = "Distance and speed display unit",
                    currentValue = uiState.distanceUnit.displayName,
                    options = DistanceUnit.values().map { it.displayName },
                    onValueChange = { displayName ->
                        val unit = DistanceUnit.values().find { it.displayName == displayName }
                        unit?.let { viewModel.updateDistanceUnit(it) }
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DropdownSetting(
                    title = "Pressure",
                    description = "Pressure display unit",
                    currentValue = uiState.pressureUnit.displayName,
                    options = PressureUnit.values().map { it.displayName },
                    onValueChange = { displayName ->
                        val unit = PressureUnit.values().find { it.displayName == displayName }
                        unit?.let { viewModel.updatePressureUnit(it) }
                    }
                )
            }
        }

        // Data Settings
        item {
            SettingsSection(
                title = "Data Management",
                icon = Icons.Default.Storage
            ) {
                SwitchSetting(
                    title = "Enable Logging",
                    description = "Log diagnostic data for analysis",
                    checked = uiState.enableLogging,
                    onCheckedChange = viewModel::updateEnableLogging
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SliderSetting(
                    title = "Data Retention",
                    description = "Keep data for ${uiState.dataRetentionDays} days",
                    value = uiState.dataRetentionDays.toFloat(),
                    valueRange = 7f..365f,
                    steps = 51,
                    onValueChange = { viewModel.updateDataRetentionDays(it.toInt()) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DropdownSetting(
    title: String,
    description: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = { expanded = true }
            ) {
                Text(currentValue)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
