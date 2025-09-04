package com.hurtec.obd2.diagnostics.obd

import java.util.logging.Logger

/**
 * Simplified OBD-II protocol handler for Hurtec OBD-II
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
        }
    }

    /**
     * Parse Service 02 (Freeze Frame Data) responses
     */
    private fun parseService02Response(pid: String, data: String, result: MutableMap<String, ObdDataPoint>) {
        parseService01Response(pid, data, result)
    }

    /**
     * Parse Service 03 (Diagnostic Trouble Codes) responses
     */
    private fun parseService03Response(data: String, result: MutableMap<String, ObdDataPoint>) {
        if (data.length >= 2) {
            val numCodes = data.substring(0, 2).toInt(16)
            result["DTC_COUNT"] = ObdDataPoint("Trouble Codes", numCodes.toFloat(), "codes")
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
}
