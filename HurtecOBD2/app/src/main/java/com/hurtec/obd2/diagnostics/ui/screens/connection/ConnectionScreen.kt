package com.hurtec.obd2.diagnostics.ui.screens.connection

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
import com.hurtec.obd2.diagnostics.obd.communication.DeviceType
import com.hurtec.obd2.diagnostics.obd.communication.ObdDevice
import com.hurtec.obd2.diagnostics.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    navController: NavController,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you'd show a snackbar here
            // For now, we'll just clear it after showing
            viewModel.clearUiError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "OBD-II Connection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status
        ConnectionStatusCard(
            connectionState = connectionState,
            connectedDevice = uiState.connectedDevice,
            onDisconnect = viewModel::disconnect,
            onTest = viewModel::testConnection
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scan button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = viewModel::scanForDevices,
                enabled = !uiState.isScanning && connectionState != ConnectionState.CONNECTING
            ) {
                if (uiState.isScanning) {
                    ScanningAnimation(
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isScanning) "Scanning..." else "Scan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device list
        if (uiState.hasDevices) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bluetooth devices
                if (uiState.bluetoothDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Bluetooth Devices",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.bluetoothDevices) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = uiState.connectingToDevice == device.id,
                            isConnected = uiState.connectedDevice?.id == device.id,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
                    }
                }

                // USB devices
                if (uiState.usbDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "USB Devices",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.usbDevices) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = uiState.connectingToDevice == device.id,
                            isConnected = uiState.connectedDevice?.id == device.id,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
                    }
                }

                // WiFi devices
                if (uiState.wifiDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "WiFi Devices",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.wifiDevices) { device ->
                        DeviceCard(
                            device = device,
                            isConnecting = uiState.connectingToDevice == device.id,
                            isConnected = uiState.connectedDevice?.id == device.id,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
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
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Tap 'Scan' to search for OBD-II adapters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Error display
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    connectedDevice: ObdDevice?,
    onDisconnect: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "Connected"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.DISCONNECTING -> "Disconnecting..."
                            ConnectionState.ERROR -> "Connection Error"
                            ConnectionState.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    connectedDevice?.let { device ->
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (connectionState == ConnectionState.CONNECTED) {
                    Row {
                        OutlinedButton(
                            onClick = onTest,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Test")
                        }

                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: ObdDevice,
    isConnecting: Boolean,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (device.type) {
                        DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
                        DeviceType.USB -> Icons.Default.Usb
                        DeviceType.WIFI -> Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (device.isPaired) {
                        Text(
                            text = "Paired",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            when {
                isConnected -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    PressAnimationBox(
                        onClick = onConnect
                    ) {
                        Button(
                            onClick = onConnect,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}
