package com.hurtec.obd2.diagnostics.ui.dashboard.model

import androidx.annotation.ColorRes

/**
 * Data class representing a gauge display item
 */
data class GaugeData(
    val title: String,
    val minValue: Float,
    val maxValue: Float,
    var currentValue: Float,
    val unit: String,
    @ColorRes val colorRes: Int,
    val isActive: Boolean = true
) {
    
    /**
     * Get the percentage value for the gauge (0-100)
     */
    fun getPercentage(): Float {
        return if (maxValue > minValue) {
            ((currentValue - minValue) / (maxValue - minValue) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }
    }
    
    /**
     * Get formatted display value
     */
    fun getFormattedValue(): String {
        return when {
            currentValue >= 1000 -> String.format("%.1fk", currentValue / 1000)
            currentValue % 1 == 0f -> String.format("%.0f", currentValue)
            else -> String.format("%.1f", currentValue)
        }
    }
    
    /**
     * Determine gauge status based on value ranges
     */
    fun getGaugeStatus(): GaugeStatus {
        val percentage = getPercentage()
        return when {
            percentage < 30f -> GaugeStatus.NORMAL
            percentage < 70f -> GaugeStatus.WARNING
            else -> GaugeStatus.CRITICAL
        }
    }
}

/**
 * Enum representing gauge status levels
 */
enum class GaugeStatus {
    NORMAL,
    WARNING,
    CRITICAL
}
