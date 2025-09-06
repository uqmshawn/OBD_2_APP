package com.hurtec.obd2.diagnostics.obd.data

import com.hurtec.obd2.diagnostics.utils.CrashHandler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Comprehensive unit conversion system for OBD-II data
 */
@Singleton
class UnitConverter @Inject constructor() {
    
    // Conversion mappings
    private val conversionMap = mapOf(
        // Temperature conversions
        "°C" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("°F", { c -> c * 9.0 / 5.0 + 32.0 }),
            UnitSystem.MIXED to ConversionRule("°F", { c -> c * 9.0 / 5.0 + 32.0 })
        ),
        
        // Speed conversions
        "km/h" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("mph", { kmh -> kmh * 0.621371 }),
            UnitSystem.MIXED to ConversionRule("mph", { kmh -> kmh * 0.621371 })
        ),
        
        // Pressure conversions
        "kPa" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("psi", { kpa -> kpa * 0.145038 }),
            UnitSystem.MIXED to ConversionRule("psi", { kpa -> kpa * 0.145038 })
        ),
        
        // Distance conversions
        "km" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("mi", { km -> km * 0.621371 }),
            UnitSystem.MIXED to ConversionRule("mi", { km -> km * 0.621371 })
        ),
        
        // Volume conversions
        "L" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("gal", { l -> l * 0.264172 }),
            UnitSystem.MIXED to ConversionRule("gal", { l -> l * 0.264172 })
        ),
        
        "L/h" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("gal/h", { lh -> lh * 0.264172 }),
            UnitSystem.MIXED to ConversionRule("gal/h", { lh -> lh * 0.264172 })
        ),
        
        "L/100km" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("mpg", { l100km -> if (l100km > 0) 235.214 / l100km else 0.0 }),
            UnitSystem.MIXED to ConversionRule("mpg", { l100km -> if (l100km > 0) 235.214 / l100km else 0.0 })
        ),
        
        // Mass conversions
        "kg" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("lb", { kg -> kg * 2.20462 }),
            UnitSystem.MIXED to ConversionRule("lb", { kg -> kg * 2.20462 })
        ),
        
        "g/s" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("lb/h", { gs -> gs * 7.93664 }),
            UnitSystem.MIXED to ConversionRule("lb/h", { gs -> gs * 7.93664 })
        ),
        
        // Torque conversions
        "Nm" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("lb-ft", { nm -> nm * 0.737562 }),
            UnitSystem.MIXED to ConversionRule("lb-ft", { nm -> nm * 0.737562 })
        ),
        
        // Power conversions
        "kW" to mapOf(
            UnitSystem.IMPERIAL to ConversionRule("hp", { kw -> kw * 1.34102 }),
            UnitSystem.MIXED to ConversionRule("hp", { kw -> kw * 1.34102 })
        )
    )
    
    /**
     * Convert a value from one unit system to another
     */
    fun convert(value: Double, fromUnit: String, toUnitSystem: UnitSystem): Double {
        return try {
            if (toUnitSystem == UnitSystem.METRIC) {
                return value // Already in metric
            }
            
            val conversionRule = conversionMap[fromUnit]?.get(toUnitSystem)
            if (conversionRule != null) {
                val convertedValue = conversionRule.converter(value)
                CrashHandler.logInfo("Converted $value $fromUnit to $convertedValue ${conversionRule.toUnit}")
                convertedValue
            } else {
                CrashHandler.logWarning("No conversion rule found for $fromUnit to $toUnitSystem")
                value // Return original value if no conversion available
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "UnitConverter.convert")
            value
        }
    }
    
    /**
     * Get the converted unit name
     */
    fun getConvertedUnit(fromUnit: String, toUnitSystem: UnitSystem): String {
        return try {
            if (toUnitSystem == UnitSystem.METRIC) {
                return fromUnit // Already in metric
            }
            
            val conversionRule = conversionMap[fromUnit]?.get(toUnitSystem)
            conversionRule?.toUnit ?: fromUnit
        } catch (e: Exception) {
            CrashHandler.handleException(e, "UnitConverter.getConvertedUnit")
            fromUnit
        }
    }
    
    /**
     * Check if conversion is available for a unit
     */
    fun isConversionAvailable(fromUnit: String, toUnitSystem: UnitSystem): Boolean {
        return conversionMap[fromUnit]?.containsKey(toUnitSystem) == true
    }
    
    /**
     * Get all available conversions for a unit
     */
    fun getAvailableConversions(fromUnit: String): Map<UnitSystem, String> {
        return conversionMap[fromUnit]?.mapValues { it.value.toUnit } ?: emptyMap()
    }
    
    /**
     * Convert temperature with proper rounding
     */
    fun convertTemperature(celsius: Double, toUnitSystem: UnitSystem): Double {
        return when (toUnitSystem) {
            UnitSystem.METRIC -> celsius
            UnitSystem.IMPERIAL, UnitSystem.MIXED -> {
                val fahrenheit = celsius * 9.0 / 5.0 + 32.0
                (fahrenheit * 10).roundToInt() / 10.0 // Round to 1 decimal place
            }
        }
    }
    
    /**
     * Convert speed with proper rounding
     */
    fun convertSpeed(kmh: Double, toUnitSystem: UnitSystem): Double {
        return when (toUnitSystem) {
            UnitSystem.METRIC -> kmh
            UnitSystem.IMPERIAL, UnitSystem.MIXED -> {
                val mph = kmh * 0.621371
                (mph * 10).roundToInt() / 10.0 // Round to 1 decimal place
            }
        }
    }
    
    /**
     * Convert pressure with proper rounding
     */
    fun convertPressure(kpa: Double, toUnitSystem: UnitSystem): Double {
        return when (toUnitSystem) {
            UnitSystem.METRIC -> kpa
            UnitSystem.IMPERIAL, UnitSystem.MIXED -> {
                val psi = kpa * 0.145038
                (psi * 100).roundToInt() / 100.0 // Round to 2 decimal places
            }
        }
    }
    
    /**
     * Convert fuel consumption
     */
    fun convertFuelConsumption(l100km: Double, toUnitSystem: UnitSystem): Double {
        return when (toUnitSystem) {
            UnitSystem.METRIC -> l100km
            UnitSystem.IMPERIAL, UnitSystem.MIXED -> {
                if (l100km > 0) {
                    val mpg = 235.214 / l100km
                    (mpg * 10).roundToInt() / 10.0 // Round to 1 decimal place
                } else {
                    0.0
                }
            }
        }
    }
    
    /**
     * Format value with appropriate precision based on unit
     */
    fun formatValue(value: Double, unit: String): String {
        return try {
            when (unit) {
                "°C", "°F" -> String.format("%.1f", value)
                "km/h", "mph" -> String.format("%.0f", value)
                "kPa", "psi" -> String.format("%.2f", value)
                "rpm" -> String.format("%.0f", value)
                "%" -> String.format("%.1f", value)
                "V" -> String.format("%.2f", value)
                "A" -> String.format("%.2f", value)
                "L/h", "gal/h" -> String.format("%.2f", value)
                "L/100km", "mpg" -> String.format("%.1f", value)
                "g/s", "lb/h" -> String.format("%.2f", value)
                "Nm", "lb-ft" -> String.format("%.1f", value)
                "kW", "hp" -> String.format("%.1f", value)
                else -> String.format("%.2f", value)
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "UnitConverter.formatValue")
            value.toString()
        }
    }
    
    /**
     * Get unit category for grouping
     */
    fun getUnitCategory(unit: String): UnitCategory {
        return when (unit) {
            "°C", "°F" -> UnitCategory.TEMPERATURE
            "km/h", "mph" -> UnitCategory.SPEED
            "kPa", "psi" -> UnitCategory.PRESSURE
            "rpm" -> UnitCategory.ROTATION
            "%" -> UnitCategory.PERCENTAGE
            "V" -> UnitCategory.VOLTAGE
            "A" -> UnitCategory.CURRENT
            "L", "gal", "L/h", "gal/h", "L/100km", "mpg" -> UnitCategory.FUEL
            "kg", "lb", "g/s", "lb/h" -> UnitCategory.MASS
            "Nm", "lb-ft" -> UnitCategory.TORQUE
            "kW", "hp" -> UnitCategory.POWER
            "km", "mi" -> UnitCategory.DISTANCE
            "s", "min", "h" -> UnitCategory.TIME
            else -> UnitCategory.OTHER
        }
    }
    
    /**
     * Get all supported units by category
     */
    fun getSupportedUnits(): Map<UnitCategory, List<String>> {
        return mapOf(
            UnitCategory.TEMPERATURE to listOf("°C", "°F"),
            UnitCategory.SPEED to listOf("km/h", "mph"),
            UnitCategory.PRESSURE to listOf("kPa", "psi"),
            UnitCategory.ROTATION to listOf("rpm"),
            UnitCategory.PERCENTAGE to listOf("%"),
            UnitCategory.VOLTAGE to listOf("V"),
            UnitCategory.CURRENT to listOf("A"),
            UnitCategory.FUEL to listOf("L", "gal", "L/h", "gal/h", "L/100km", "mpg"),
            UnitCategory.MASS to listOf("kg", "lb", "g/s", "lb/h"),
            UnitCategory.TORQUE to listOf("Nm", "lb-ft"),
            UnitCategory.POWER to listOf("kW", "hp"),
            UnitCategory.DISTANCE to listOf("km", "mi"),
            UnitCategory.TIME to listOf("s", "min", "h")
        )
    }
}

/**
 * Conversion rule definition
 */
private data class ConversionRule(
    val toUnit: String,
    val converter: (Double) -> Double
)

/**
 * Unit categories for organization
 */
enum class UnitCategory {
    TEMPERATURE,
    SPEED,
    PRESSURE,
    ROTATION,
    PERCENTAGE,
    VOLTAGE,
    CURRENT,
    FUEL,
    MASS,
    TORQUE,
    POWER,
    DISTANCE,
    TIME,
    OTHER
}
