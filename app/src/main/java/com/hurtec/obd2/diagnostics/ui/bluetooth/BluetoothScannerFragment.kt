package com.hurtec.obd2.diagnostics.ui.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.databinding.FragmentBluetoothScannerBinding

/**
 * Fragment for scanning and selecting Bluetooth OBD-II devices
 */
class BluetoothScannerFragment : Fragment() {

    private var _binding: FragmentBluetoothScannerBinding? = null
    private val binding get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private val discoveredDevices = mutableListOf<BluetoothDeviceItem>()

    // Callback for device selection
    var onDeviceSelected: ((BluetoothDevice) -> Unit)? = null

    // Broadcast receiver for Bluetooth discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { addDevice(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnScan.text = "Scan for Devices"
                    binding.btnScan.isEnabled = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnScan.text = "Scanning..."
                    binding.btnScan.isEnabled = false
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        setupRecyclerView()
        setupClickListeners()
        loadPairedDevices()
        registerBluetoothReceiver()
    }

    private fun setupRecyclerView() {
        deviceAdapter = BluetoothDeviceAdapter(discoveredDevices) { device ->
            onDeviceSelected?.invoke(device.bluetoothDevice)
        }

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            startDiscovery()
        }

        binding.btnRefresh.setOnClickListener {
            refreshDevices()
        }
    }

    private fun loadPairedDevices() {
        if (!hasBluetoothPermission()) {
            showMessage("Bluetooth permission required", true)
            return
        }

        bluetoothAdapter?.bondedDevices?.forEach { device ->
            addDevice(device, isPaired = true)
        }
    }

    private fun startDiscovery() {
        if (!hasBluetoothPermission()) {
            showMessage("Bluetooth permission required", true)
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            showMessage("Please enable Bluetooth", true)
            return
        }

        // Clear previous discoveries (keep paired devices)
        discoveredDevices.removeAll { !it.isPaired }
        deviceAdapter.notifyDataSetChanged()

        // Start discovery
        bluetoothAdapter?.startDiscovery()
    }

    private fun refreshDevices() {
        discoveredDevices.clear()
        deviceAdapter.notifyDataSetChanged()
        loadPairedDevices()
    }

    private fun addDevice(device: BluetoothDevice, isPaired: Boolean = false) {
        // Check if device already exists
        val existingDevice = discoveredDevices.find { it.bluetoothDevice.address == device.address }
        if (existingDevice != null) return

        val deviceName = if (hasBluetoothPermission()) {
            device.name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }

        val deviceItem = BluetoothDeviceItem(
            bluetoothDevice = device,
            name = deviceName,
            address = device.address,
            isPaired = isPaired || (device.bondState == BluetoothDevice.BOND_BONDED),
            isObdDevice = isLikelyObdDevice(deviceName)
        )

        discoveredDevices.add(deviceItem)
        
        // Sort devices: OBD devices first, then paired, then by name
        discoveredDevices.sortWith(compareBy<BluetoothDeviceItem> { !it.isObdDevice }
            .thenBy { !it.isPaired }
            .thenBy { it.name })
        
        deviceAdapter.notifyDataSetChanged()
    }

    private fun isLikelyObdDevice(deviceName: String): Boolean {
        val obdKeywords = listOf(
            "obd", "elm", "327", "obdii", "obd2", "scan", "diag", "auto", "car", "vehicle"
        )
        return obdKeywords.any { keyword ->
            deviceName.lowercase().contains(keyword)
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }
        requireContext().registerReceiver(discoveryReceiver, filter)
    }

    private fun showMessage(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.status_error))
        } else {
            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.status_connected))
        }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        bluetoothAdapter?.cancelDiscovery()
        _binding = null
    }
}

/**
 * Data class representing a Bluetooth device item
 */
data class BluetoothDeviceItem(
    val bluetoothDevice: BluetoothDevice,
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val isObdDevice: Boolean
)
