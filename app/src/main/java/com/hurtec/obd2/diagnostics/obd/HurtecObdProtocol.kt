package com.hurtec.obd2.diagnostics.obd

import java.util.logging.Logger

/**
 * Simplified OBD-II protocol handler for Hurtec OBD-II
 * Handles parsing of common OBD-II PIDs and responses
 */
class HurtecObdProtocol {
    
    companion object {
        private val TAG = "HurtecObdProtocol"
        private val logger = Logger.getLogger(TAG)
    }

    /**
     * Parse OBD-II response and convert to data points
     */
    fun parseResponse(response: String): Map<String, ObdDataPoint> {
        val result = mutableMapOf<String, ObdDataPoint>()
        
        try {
            val cleanResponse = response.trim().replace(">", "").replace("\r", "").replace("\n", "")
            
            if (cleanResponse.length < 4) return result
            
            // Extract service and PID
            val service = cleanResponse.substring(0, 2)
            val pid = cleanResponse.substring(2, 4)
            val data = if (cleanResponse.length > 4) cleanResponse.substring(4) else ""
            
            when (service) {
                "41" -> parseService01Response(pid, data, result)
                "42" -> parseService02Response(pid, data, result)
                "43" -> parseService03Response(data, result)
                // Add more services as needed
            }
            
        } catch (e: Exception) {
            logger.warning("Error parsing OBD response '$response': ${e.message}")
        }
        
        return result
    }

    /**
     * Parse Service 01 (Live Data) responses
     */
    private fun parseService01Response(pid: String, data: String, result: MutableMap<String, ObdDataPoint>) {
        when (pid) {
            "0C" -> parseEngineRPM(data, result)
            "0D" -> parseVehicleSpeed(data, result)
            "05" -> parseEngineCoolantTemp(data, result)
            "2F" -> parseFuelTankLevel(data, result)
            "04" -> parseEngineLoad(data, result)
            "06" -> parseShortTermFuelTrim(data, result)
            "0B" -> parseIntakeManifoldPressure(data, result)
            "0F" -> parseIntakeAirTemp(data, result)
            "10" -> parseMassAirFlow(data, result)
            "11" -> parseThrottlePosition(data, result)
            // Add more PIDs as needed
        }
    }

    /**
     * Parse Service 02 (Freeze Frame Data) responses
     */
    private fun parseService02Response(pid: String, data: String, result: MutableMap<String, ObdDataPoint>) {
        // Similar to Service 01 but for freeze frame data
        parseService01Response(pid, data, result)
    }

    /**
     * Parse Service 03 (Diagnostic Trouble Codes) responses
     */
    private fun parseService03Response(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val numCodes = data.substring(0, 2).toInt(16)
            result["DTC_COUNT"] = ObdDataPoint("Trouble Codes", numCodes.toFloat(), "codes")
            
            // Parse individual DTCs if present
            var index = 2
            for (i in 0 until numCodes) {
                if (index + 4 <= data.length) {
                    val dtcCode = parseDTC(data.substring(index, index + 4))
                    result["DTC_$i"] = ObdDataPoint("DTC $i", 0f, dtcCode)
                    index += 4
                }
            }
        }
    }

    /**
     * Parse Engine RPM (PID 0C)
     */
    private fun parseEngineRPM(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 4) {
            val a = data.substring(0, 2).toInt(16)
            val b = data.substring(2, 4).toInt(16)
            val rpm = ((a * 256) + b) / 4.0f
            result["ENGINE_RPM"] = ObdDataPoint("Engine RPM", rpm, "RPM")
        }
    }

    /**
     * Parse Vehicle Speed (PID 0D)
     */
    private fun parseVehicleSpeed(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val speed = data.substring(0, 2).toInt(16).toFloat()
            result["VEHICLE_SPEED"] = ObdDataPoint("Vehicle Speed", speed, "km/h")
        }
    }

    /**
     * Parse Engine Coolant Temperature (PID 05)
     */
    private fun parseEngineCoolantTemp(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val temp = data.substring(0, 2).toInt(16) - 40f
            result["ENGINE_TEMP"] = ObdDataPoint("Engine Temperature", temp, "°C")
        }
    }

    /**
     * Parse Fuel Tank Level (PID 2F)
     */
    private fun parseFuelTankLevel(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val level = (data.substring(0, 2).toInt(16) * 100) / 255f
            result["FUEL_LEVEL"] = ObdDataPoint("Fuel Level", level, "%")
        }
    }

    /**
     * Parse Engine Load (PID 04)
     */
    private fun parseEngineLoad(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val load = (data.substring(0, 2).toInt(16) * 100) / 255f
            result["ENGINE_LOAD"] = ObdDataPoint("Engine Load", load, "%")
        }
    }

    /**
     * Parse Short Term Fuel Trim (PID 06)
     */
    private fun parseShortTermFuelTrim(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val trim = (data.substring(0, 2).toInt(16) - 128) * 100 / 128f
            result["FUEL_TRIM"] = ObdDataPoint("Fuel Trim", trim, "%")
        }
    }

    /**
     * Parse Intake Manifold Pressure (PID 0B)
     */
    private fun parseIntakeManifoldPressure(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val pressure = data.substring(0, 2).toInt(16).toFloat()
            result["INTAKE_PRESSURE"] = ObdDataPoint("Intake Pressure", pressure, "kPa")
        }
    }

    /**
     * Parse Intake Air Temperature (PID 0F)
     */
    private fun parseIntakeAirTemp(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val temp = data.substring(0, 2).toInt(16) - 40f
            result["INTAKE_TEMP"] = ObdDataPoint("Intake Air Temp", temp, "°C")
        }
    }

    /**
     * Parse Mass Air Flow (PID 10)
     */
    private fun parseMassAirFlow(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 4) {
            val a = data.substring(0, 2).toInt(16)
            val b = data.substring(2, 4).toInt(16)
            val maf = ((a * 256) + b) / 100f
            result["MAF"] = ObdDataPoint("Mass Air Flow", maf, "g/s")
        }
    }

    /**
     * Parse Throttle Position (PID 11)
     */
    private fun parseThrottlePosition(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val position = (data.substring(0, 2).toInt(16) * 100) / 255f
            result["THROTTLE_POS"] = ObdDataPoint("Throttle Position", position, "%")
        }
    }

    /**
     * Parse Diagnostic Trouble Code
     */
    private fun parseDTC(hexCode: String): String {
        if (hexCode.length != 4) return "INVALID"
        
        val firstByte = hexCode.substring(0, 2).toInt(16)
        val secondByte = hexCode.substring(2, 4).toInt(16)
        
        val firstChar = when ((firstByte and 0xC0) shr 6) {
            0 -> 'P' // Powertrain
            1 -> 'C' // Chassis
            2 -> 'B' // Body
            3 -> 'U' // Network
            else -> 'P'
        }
        
        val secondChar = (firstByte and 0x30) shr 4
        val thirdChar = firstByte and 0x0F
        val fourthChar = (secondByte and 0xF0) shr 4
        val fifthChar = secondByte and 0x0F
        
        return "$firstChar$secondChar$thirdChar$fourthChar$fifthChar"
    }
}
