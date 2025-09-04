package com.hurtec.obd2.diagnostics.ui.dashboard.adapter

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.databinding.ItemGaugeCardBinding
import com.hurtec.obd2.diagnostics.ui.dashboard.model.GaugeData
import com.hurtec.obd2.diagnostics.ui.dashboard.model.GaugeStatus

/**
 * RecyclerView adapter for displaying gauge cards in a modern grid layout
 */
class GaugeAdapter(
    private val gaugeList: List<GaugeData>,
    private val onGaugeClick: (GaugeData) -> Unit
) : RecyclerView.Adapter<GaugeAdapter.GaugeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GaugeViewHolder {
        val binding = ItemGaugeCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GaugeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GaugeViewHolder, position: Int) {
        holder.bind(gaugeList[position])
    }

    override fun getItemCount(): Int = gaugeList.size

    inner class GaugeViewHolder(
        private val binding: ItemGaugeCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gaugeData: GaugeData) {
            with(binding) {
                // Set gauge title and unit
                tvGaugeTitle.text = gaugeData.title
                tvUnit.text = gaugeData.unit
                tvCurrentValue.text = gaugeData.getFormattedValue()

                // Configure speedometer
                gaugeSpeedometer.apply {
                    minSpeed = gaugeData.minValue
                    maxSpeed = gaugeData.maxValue
                    unit = gaugeData.unit

                    // Animate to current value
                    animateToValue(gaugeData.currentValue)
                }

                // Update status indicator
                statusIndicator.backgroundTintList = ContextCompat.getColorStateList(
                    binding.root.context,
                    when (gaugeData.getGaugeStatus()) {
                        GaugeStatus.NORMAL -> R.color.gauge_normal
                        GaugeStatus.WARNING -> R.color.gauge_warning
                        GaugeStatus.CRITICAL -> R.color.gauge_critical
                    }
                )

                // Set click listener
                root.setOnClickListener {
                    // Add haptic feedback
                    root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onGaugeClick(gaugeData)
                }

                // Add subtle animation on bind
                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((adapterPosition * 50).toLong())
                    .start()
            }
        }

        private fun animateToValue(targetValue: Float) {
            val currentValue = binding.gaugeSpeedometer.speed
            
            ValueAnimator.ofFloat(currentValue, targetValue).apply {
                duration = 1000
                addUpdateListener { animator ->
                    val animatedValue = animator.animatedValue as Float
                    binding.gaugeSpeedometer.speedTo(animatedValue)
                    binding.tvCurrentValue.text = String.format("%.0f", animatedValue)
                }
                start()
            }
        }
    }
}
