package com.hurtec.obd2.diagnostics.ui.bluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.databinding.ItemBluetoothDeviceBinding

/**
 * Adapter for displaying Bluetooth devices in RecyclerView
 */
class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDeviceItem>,
    private val onDeviceClick: (BluetoothDeviceItem) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemBluetoothDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(
        private val binding: ItemBluetoothDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDeviceItem) {
            binding.apply {
                tvDeviceName.text = device.name
                tvDeviceAddress.text = device.address

                // Set device type icon
                val iconRes = when {
                    device.isObdDevice -> R.drawable.ic_car_24
                    device.isPaired -> R.drawable.ic_bluetooth_connected_24
                    else -> R.drawable.ic_bluetooth_24
                }
                ivDeviceIcon.setImageResource(iconRes)

                // Set status indicators
                if (device.isPaired) {
                    tvPairedStatus.text = "Paired"
                    tvPairedStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.status_connected)
                    )
                } else {
                    tvPairedStatus.text = "Available"
                    tvPairedStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.hurtec_secondary)
                    )
                }

                if (device.isObdDevice) {
                    tvObdIndicator.text = "OBD-II Device"
                    tvObdIndicator.setTextColor(
                        ContextCompat.getColor(root.context, R.color.hurtec_neon_cyan)
                    )
                } else {
                    tvObdIndicator.text = ""
                }

                // Set click listener
                root.setOnClickListener {
                    onDeviceClick(device)
                }

                // Highlight OBD devices
                if (device.isObdDevice) {
                    cardDevice.strokeColor = ContextCompat.getColor(root.context, R.color.hurtec_neon_cyan)
                    cardDevice.strokeWidth = 2
                } else {
                    cardDevice.strokeColor = ContextCompat.getColor(root.context, R.color.hurtec_surface_variant)
                    cardDevice.strokeWidth = 1
                }
            }
        }
    }
}
