package com.hurtec.obd2.diagnostics.ui.screens.connection

import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionState
import com.hurtec.obd2.diagnostics.obd.communication.DeviceType
import com.hurtec.obd2.diagnostics.obd.communication.ObdDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ConnectionViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {

    @Mock
    private lateinit var communicationManager: CommunicationManager

    private lateinit var viewModel: ConnectionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup mock flows
        whenever(communicationManager.connectionState).thenReturn(
            MutableStateFlow(ConnectionState.DISCONNECTED)
        )
        whenever(communicationManager.connectionType).thenReturn(
            MutableStateFlow(null)
        )
        
        viewModel = ConnectionViewModel(communicationManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() {
        val initialState = viewModel.uiState.value
        
        assertEquals(emptyList(), initialState.devices)
        assertFalse(initialState.isScanning)
        assertEquals(null, initialState.connectingToDevice)
        assertEquals(null, initialState.connectedDevice)
        assertEquals(null, initialState.error)
    }

    @Test
    fun `scanForDevices should update scanning state`() = runTest {
        // Setup mock responses
        whenever(communicationManager.getAvailableBluetoothDevices()).thenReturn(
            listOf(
                ObdDevice(
                    id = "test-bt-1",
                    name = "Test Bluetooth Device",
                    address = "00:11:22:33:44:55",
                    type = DeviceType.BLUETOOTH,
                    isPaired = true
                )
            )
        )
        whenever(communicationManager.getAvailableUsbDevices()).thenReturn(emptyList())

        // Start scanning
        viewModel.scanForDevices()
        
        // Check scanning state
        assertTrue(viewModel.uiState.value.isScanning)
        
        // Advance time to complete scanning
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isScanning)
        assertEquals(1, finalState.devices.size)
        assertEquals("Test Bluetooth Device", finalState.devices.first().name)
    }

    @Test
    fun `connectToDevice should update connecting state`() = runTest {
        val testDevice = ObdDevice(
            id = "test-device",
            name = "Test Device",
            address = "test-address",
            type = DeviceType.BLUETOOTH,
            isPaired = true
        )

        whenever(communicationManager.connectBluetooth(testDevice)).thenReturn(
            Result.success(Unit)
        )

        // Start connection
        viewModel.connectToDevice(testDevice)
        
        // Check connecting state
        assertEquals("test-device", viewModel.uiState.value.connectingToDevice)
        
        // Advance time to complete connection
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.connectingToDevice)
        assertEquals(testDevice, finalState.connectedDevice)
    }

    @Test
    fun `connectToDevice failure should show error`() = runTest {
        val testDevice = ObdDevice(
            id = "test-device",
            name = "Test Device",
            address = "test-address",
            type = DeviceType.BLUETOOTH,
            isPaired = true
        )

        whenever(communicationManager.connectBluetooth(testDevice)).thenReturn(
            Result.failure(Exception("Connection failed"))
        )

        // Start connection
        viewModel.connectToDevice(testDevice)
        
        // Advance time to complete connection attempt
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check error state
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.connectingToDevice)
        assertEquals(null, finalState.connectedDevice)
        assertTrue(finalState.error?.contains("Connection failed") == true)
    }

    @Test
    fun `disconnect should clear connected device`() = runTest {
        // Setup initial connected state
        viewModel.uiState.value.copy(
            connectedDevice = ObdDevice(
                id = "test-device",
                name = "Test Device",
                address = "test-address",
                type = DeviceType.BLUETOOTH,
                isPaired = true
            )
        )

        whenever(communicationManager.disconnect()).thenReturn(Result.success(Unit))

        // Disconnect
        viewModel.disconnect()
        
        // Advance time
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check state
        assertEquals(null, viewModel.uiState.value.connectedDevice)
    }

    @Test
    fun `clearError should remove error message`() {
        // Set error state
        viewModel.uiState.value.copy(error = "Test error")
        
        // Clear error
        viewModel.clearError()
        
        // Check state
        assertEquals(null, viewModel.uiState.value.error)
    }
}
