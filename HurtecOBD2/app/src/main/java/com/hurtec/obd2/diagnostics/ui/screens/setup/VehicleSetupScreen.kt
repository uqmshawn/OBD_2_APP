package com.hurtec.obd2.diagnostics.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import com.hurtec.obd2.diagnostics.ui.viewmodels.VehicleSetupViewModel

/**
 * Vehicle Setup Screen for adding the first vehicle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSetupScreen(
    navController: NavController,
    viewModel: VehicleSetupViewModel = hiltViewModel()
) {
    var vehicleName by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var engineSize by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Gasoline") }
    var transmission by remember { mutableStateOf("Automatic") }
    var licensePlate by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState

    // Observe ViewModel state
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is VehicleSetupViewModel.UiState.Success -> {
                isLoading = false
                // Navigate to dashboard after successful vehicle addition
                navController.navigate("dashboard") {
                    popUpTo("vehicle_setup") { inclusive = true }
                }
            }
            is VehicleSetupViewModel.UiState.Error -> {
                isLoading = false
                showError = true
                errorMessage = state.message
            }
            is VehicleSetupViewModel.UiState.Loading -> {
                isLoading = true
                showError = false
            }
            else -> {
                isLoading = false
                showError = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Add Your Vehicle",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Let's start by adding your vehicle information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Basic Information Section
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vehicle Name (Required)
        OutlinedTextField(
            value = vehicleName,
            onValueChange = { vehicleName = it },
            label = { Text("Vehicle Name *") },
            placeholder = { Text("My Car") },
            leadingIcon = {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Make and Model Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = make,
                onValueChange = { make = it },
                label = { Text("Make") },
                placeholder = { Text("Toyota") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                placeholder = { Text("Camry") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Year and Engine Size Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = year,
                onValueChange = { if (it.length <= 4) year = it },
                label = { Text("Year") },
                placeholder = { Text("2020") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = engineSize,
                onValueChange = { engineSize = it },
                label = { Text("Engine Size") },
                placeholder = { Text("2.5L") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // VIN with detection button
        OutlinedTextField(
            value = vin,
            onValueChange = { if (it.length <= 17) vin = it.uppercase() },
            label = { Text("VIN (Vehicle Identification Number)") },
            placeholder = { Text("1HGBH41JXMN109186") },
            leadingIcon = {
                Icon(Icons.Default.QrCode, contentDescription = null)
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        // Implement real VIN detection from OBD
                        viewModel.detectVinFromObd { detectedVin ->
                            if (detectedVin.isNotEmpty()) {
                                vin = detectedVin
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Detect VIN from OBD",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("${vin.length}/17 characters • Tap Bluetooth icon to detect from OBD")
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Additional Information Section
        Text(
            text = "Additional Information",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fuel Type and Transmission Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fuel Type Dropdown
            var fuelTypeExpanded by remember { mutableStateOf(false) }
            val fuelTypes = listOf("Gasoline", "Diesel", "Hybrid", "Electric", "Other")
            
            ExposedDropdownMenuBox(
                expanded = fuelTypeExpanded,
                onExpandedChange = { fuelTypeExpanded = !fuelTypeExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fuel Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelTypeExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = fuelTypeExpanded,
                    onDismissRequest = { fuelTypeExpanded = false }
                ) {
                    fuelTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                fuelType = type
                                fuelTypeExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Transmission Dropdown
            var transmissionExpanded by remember { mutableStateOf(false) }
            val transmissionTypes = listOf("Automatic", "Manual", "CVT", "Other")
            
            ExposedDropdownMenuBox(
                expanded = transmissionExpanded,
                onExpandedChange = { transmissionExpanded = !transmissionExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = transmission,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transmission") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transmissionExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = transmissionExpanded,
                    onDismissRequest = { transmissionExpanded = false }
                ) {
                    transmissionTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                transmission = type
                                transmissionExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // License Plate and Color Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = licensePlate,
                onValueChange = { licensePlate = it.uppercase() },
                label = { Text("License Plate") },
                placeholder = { Text("ABC-1234") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color") },
                placeholder = { Text("Silver") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            placeholder = { Text("Additional information about your vehicle...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error Message
        if (showError) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Back")
            }
            
            Button(
                onClick = {
                    if (vehicleName.isBlank()) {
                        showError = true
                        errorMessage = "Vehicle name is required"
                        return@Button
                    }
                    
                    val vehicle = VehicleEntity(
                        name = vehicleName,
                        make = make.takeIf { it.isNotBlank() },
                        model = model.takeIf { it.isNotBlank() },
                        year = year.toIntOrNull(),
                        vin = vin.takeIf { it.isNotBlank() },
                        engineSize = engineSize.takeIf { it.isNotBlank() },
                        fuelType = fuelType,
                        transmission = transmission,
                        licensePlate = licensePlate.takeIf { it.isNotBlank() },
                        color = color.takeIf { it.isNotBlank() },
                        notes = notes.takeIf { it.isNotBlank() },
                        isActive = true
                    )
                    
                    viewModel.addVehicle(vehicle)
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && vehicleName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Vehicle")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Skip Button
        TextButton(
            onClick = { navController.navigate("main") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Skip for now")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
