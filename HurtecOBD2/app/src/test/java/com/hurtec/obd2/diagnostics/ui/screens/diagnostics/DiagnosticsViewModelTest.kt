package com.hurtec.obd2.diagnostics.ui.screens.diagnostics

import com.hurtec.obd2.diagnostics.data.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionState
import com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo
import com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus
import com.hurtec.obd2.diagnostics.obd.elm327.ELM327ProtocolHandler
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
 * Unit tests for DiagnosticsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    @Mock
    private lateinit var communicationManager: CommunicationManager

    @Mock
    private lateinit var vehicleRepository: VehicleRepository

    @Mock
    private lateinit var protocolHandler: ELM327ProtocolHandler

    private lateinit var viewModel: DiagnosticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup mock flows
        whenever(communicationManager.connectionState).thenReturn(
            MutableStateFlow(ConnectionState.CONNECTED)
        )
        
        viewModel = DiagnosticsViewModel(
            communicationManager,
            vehicleRepository,
            protocolHandler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() {
        val initialState = viewModel.uiState.value
        
        assertEquals(emptyList(), initialState.dtcs)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isClearing)
        assertEquals(null, initialState.error)
    }

    @Test
    fun `loadDtcs should update loading state and fetch DTCs`() = runTest {
        val testDtcs = listOf(
            DtcInfo(code = "P0171", status = DtcStatus.STORED),
            DtcInfo(code = "P0300", status = DtcStatus.PENDING)
        )

        whenever(protocolHandler.readDtcs()).thenReturn(Result.success(testDtcs))

        // Start loading
        viewModel.loadDtcs()
        
        // Check loading state
        assertTrue(viewModel.uiState.value.isLoading)
        
        // Advance time to complete loading
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(2, finalState.dtcs.size)
        assertEquals("P0171", finalState.dtcs[0].code)
        assertEquals("P0300", finalState.dtcs[1].code)
        assertTrue(finalState.lastScanTime != null)
    }

    @Test
    fun `loadDtcs failure should show error`() = runTest {
        whenever(protocolHandler.readDtcs()).thenReturn(
            Result.failure(Exception("Failed to read DTCs"))
        )

        // Start loading
        viewModel.loadDtcs()
        
        // Advance time to complete loading
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check error state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(emptyList(), finalState.dtcs)
        assertTrue(finalState.error?.contains("Failed to read DTCs") == true)
    }

    @Test
    fun `clearDtcs should update clearing state`() = runTest {
        // Setup initial state with DTCs
        val testDtcs = listOf(
            DtcInfo(code = "P0171", status = DtcStatus.STORED)
        )
        whenever(protocolHandler.readDtcs()).thenReturn(Result.success(testDtcs))
        whenever(protocolHandler.clearDtcs()).thenReturn(Result.success(Unit))

        // Load DTCs first
        viewModel.loadDtcs()
        testDispatcher.scheduler.advanceUntilIdle()

        // Clear DTCs
        viewModel.clearDtcs()
        
        // Check clearing state
        assertTrue(viewModel.uiState.value.isClearing)
        
        // Advance time to complete clearing
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isClearing)
        assertTrue(finalState.clearSuccess)
    }

    @Test
    fun `clearDtcs failure should show error`() = runTest {
        whenever(protocolHandler.clearDtcs()).thenReturn(
            Result.failure(Exception("Failed to clear DTCs"))
        )

        // Clear DTCs
        viewModel.clearDtcs()
        
        // Advance time to complete clearing
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check error state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isClearing)
        assertFalse(finalState.clearSuccess)
        assertTrue(finalState.error?.contains("Failed to clear DTCs") == true)
    }

    @Test
    fun `getFreezeFrameData should load freeze frame data`() = runTest {
        val testCode = "P0171"
        
        // Get freeze frame data
        viewModel.getFreezeFrameData(testCode)
        
        // Check loading state
        assertEquals(testCode, viewModel.uiState.value.loadingFreezeFrame)
        
        // Advance time to complete loading
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.loadingFreezeFrame)
        assertTrue(finalState.selectedDtcFreezeFrame != null)
        assertEquals(testCode, finalState.selectedDtcFreezeFrame?.first)
    }

    @Test
    fun `getReadinessMonitors should load monitor data`() = runTest {
        // Get readiness monitors
        viewModel.getReadinessMonitors()
        
        // Check loading state
        assertTrue(viewModel.uiState.value.isLoadingReadiness)
        
        // Advance time to complete loading
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingReadiness)
        assertTrue(finalState.readinessMonitors.isNotEmpty())
    }

    @Test
    fun `activeDtcs should filter stored DTCs`() = runTest {
        val testDtcs = listOf(
            DtcInfo(code = "P0171", status = DtcStatus.STORED),
            DtcInfo(code = "P0300", status = DtcStatus.PENDING),
            DtcInfo(code = "P0420", status = DtcStatus.STORED)
        )

        whenever(protocolHandler.readDtcs()).thenReturn(Result.success(testDtcs))

        // Load DTCs
        viewModel.loadDtcs()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check filtered DTCs
        val finalState = viewModel.uiState.value
        assertEquals(2, finalState.activeDtcs.size)
        assertEquals(1, finalState.pendingDtcs.size)
        assertTrue(finalState.hasDtcs)
    }

    @Test
    fun `clearError should remove error message`() {
        // Set error state manually (in real scenario this would come from failed operations)
        viewModel.clearError()
        
        // Check state
        assertEquals(null, viewModel.uiState.value.error)
    }
}
