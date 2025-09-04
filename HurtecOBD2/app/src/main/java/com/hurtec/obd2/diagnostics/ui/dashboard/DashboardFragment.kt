package com.hurtec.obd2.diagnostics.ui.dashboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.databinding.FragmentDashboardBinding
import com.hurtec.obd2.diagnostics.service.ConnectionState
import com.hurtec.obd2.diagnostics.service.ObdService
import com.hurtec.obd2.diagnostics.ui.dashboard.adapter.GaugeAdapter
import com.hurtec.obd2.diagnostics.ui.dashboard.model.GaugeData
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var gaugeAdapter: GaugeAdapter
    private val gaugeDataList = mutableListOf<GaugeData>()

    // OBD Service connection
    private var obdService: ObdService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ObdService.ObdBinder
            obdService = binder.getService()
            isServiceBound = true
            observeObdData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            obdService = null
            isServiceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGauges()
        setupClickListeners()
        updateConnectionStatus(false)
        // Temporarily disable service binding to prevent crashes
        // bindToObdService()
    }

    private fun setupGauges() {
        // Initialize gauge data
        gaugeDataList.apply {
            clear()
            add(GaugeData("Engine RPM", 0f, 8000f, 0f, "RPM", R.color.hurtec_neon_cyan))
            add(GaugeData("Vehicle Speed", 0f, 200f, 0f, "km/h", R.color.hurtec_neon_blue))
            add(GaugeData("Engine Temp", 0f, 120f, 0f, "°C", R.color.gauge_warning))
            add(GaugeData("Fuel Level", 0f, 100f, 0f, "%", R.color.gauge_normal))
        }

        // Setup RecyclerView with GridLayoutManager
        gaugeAdapter = GaugeAdapter(gaugeDataList) { gaugeData ->
            onGaugeClicked(gaugeData)
        }

        binding.rvGauges.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = gaugeAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            onConnectClicked()
        }

        binding.btnScanCodes.setOnClickListener {
            onScanCodesClicked()
        }

        binding.fabSettings.setOnClickListener {
            onSettingsClicked()
        }
    }

    private fun onConnectClicked() {
        // TODO: Implement connection logic
        updateConnectionStatus(true)
        simulateGaugeData()
    }

    private fun onScanCodesClicked() {
        // TODO: Navigate to diagnostics fragment
    }

    private fun onSettingsClicked() {
        // TODO: Navigate to settings fragment
    }

    private fun onGaugeClicked(gaugeData: GaugeData) {
        // TODO: Show detailed view or navigate to data fragment
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        binding.tvConnectionStatus.apply {
            if (isConnected) {
                text = "Connected to OBD-II"
                setTextColor(resources.getColor(R.color.status_connected, null))
            } else {
                text = "Ready to Connect"
                setTextColor(resources.getColor(R.color.status_disconnected, null))
            }
        }
    }

    private fun simulateGaugeData() {
        // Simulate some gauge data for demonstration
        gaugeDataList[0].currentValue = 2500f // RPM
        gaugeDataList[1].currentValue = 65f   // Speed
        gaugeDataList[2].currentValue = 85f   // Temperature
        gaugeDataList[3].currentValue = 75f   // Fuel

        gaugeAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Temporarily disable service unbinding
        // if (isServiceBound) {
        //     requireContext().unbindService(serviceConnection)
        //     isServiceBound = false
        // }
        _binding = null
    }

    private fun bindToObdService() {
        val intent = Intent(requireContext(), ObdService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeObdData() {
        obdService?.let { service ->
            // Observe connection state
            lifecycleScope.launch {
                service.connectionState.collect { state ->
                    updateConnectionStatus(state == ConnectionState.CONNECTED)
                }
            }

            // Observe OBD data and update gauges
            lifecycleScope.launch {
                service.obdData.collect { obdData ->
                    updateGaugesWithObdData(obdData)
                }
            }
        }
    }

    private fun updateGaugesWithObdData(obdData: Map<String, com.hurtec.obd2.diagnostics.obd.ObdDataPoint>) {
        // Update RPM gauge
        obdData["ENGINE_RPM"]?.let { dataPoint ->
            gaugeDataList.find { it.title == "Engine RPM" }?.let { gauge ->
                gauge.currentValue = dataPoint.value
                gaugeAdapter.notifyDataSetChanged()
            }
        }

        // Update Speed gauge
        obdData["VEHICLE_SPEED"]?.let { dataPoint ->
            gaugeDataList.find { it.title == "Speed" }?.let { gauge ->
                gauge.currentValue = dataPoint.value
                gaugeAdapter.notifyDataSetChanged()
            }
        }

        // Update Temperature gauge
        obdData["ENGINE_TEMP"]?.let { dataPoint ->
            gaugeDataList.find { it.title == "Temperature" }?.let { gauge ->
                gauge.currentValue = dataPoint.value
                gaugeAdapter.notifyDataSetChanged()
            }
        }

        // Update Fuel gauge
        obdData["FUEL_LEVEL"]?.let { dataPoint ->
            gaugeDataList.find { it.title == "Fuel Level" }?.let { gauge ->
                gauge.currentValue = dataPoint.value
                gaugeAdapter.notifyDataSetChanged()
            }
        }
    }
}
