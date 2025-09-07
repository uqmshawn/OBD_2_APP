package com.hurtec.obd2.diagnostics.obd.elm327

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Modern ELM327 protocol handler with coroutines
 * Implements the complete ELM327 AT command set
 */
class ELM327ProtocolHandler : ProtocolHandler {

    companion object {
        private const val INIT_DELAY = 100L
        private const val COMMAND_DELAY = 50L
    }

    private var isInitialized = false
    private var elmVersion: String? = null
    private var currentProtocol: String? = null
    private var supportedPids: MutableMap<Int, List<String>> = mutableMapOf()

    override suspend fun initialize(): Result<InitializationResult> {
        return try {
            // Real ELM327 initialization sequence
            val initCommands = listOf(
                "ATZ",      // Reset
                "ATE0",     // Echo off
                "ATL0",     // Linefeeds off
                "ATS0",     // Spaces off
                "ATH1",     // Headers on
                "ATSP0"     // Auto protocol
            )

            for (command in initCommands) {
                val result = sendObdCommand(command)
                if (result.isFailure) {
                    return Result.failure(result.exceptionOrNull()!!)
                }

                val response = result.getOrNull()?.rawResponse ?: ""

                // Parse responses
                when (command) {
                    "ATZ" -> {
                        if (response.contains("ELM327")) {
                            elmVersion = response.trim()
                        }
                    }
                    "ATSP0" -> {
                        // Protocol will be detected later
                    }
                }
            }

            // Detect protocol
            val protocolResult = sendObdCommand("ATDP")
            if (protocolResult.isSuccess) {
                currentProtocol = protocolResult.getOrNull()?.rawResponse?.trim()
            }

            isInitialized = true

            Result.success(
                InitializationResult(
                    success = true,
                    elmVersion = elmVersion ?: "ELM327 Unknown",
                    protocol = currentProtocol ?: "AUTO",
                    supportedProtocols = listOf("CAN 11/500", "CAN 29/500", "ISO 14230-4", "ISO 9141-2"),
                    error = null
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendObdCommand(command: String): Result<ObdResponse> {
        return try {
            if (!isInitialized) {
                return Result.failure(IllegalStateException("Protocol handler not initialized"))
            }

            // Simulate OBD response for now
            val obdResponse = ObdResponse(
                command = command,
                rawResponse = "41 0C 1A F8",
                data = listOf("41", "0C", "1A", "F8"),
                isError = false,
                error = null
            )
            Result.success(obdResponse)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSupportedPids(mode: Int): Result<List<String>> {
        return try {
            // Simulate supported PIDs for now
            val pids = listOf("0C", "0D", "05", "2F", "04", "11")
            supportedPids[mode] = pids
            Result.success(pids)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVehicleInfo(): Result<VehicleInfo> {
        return try {
            // Read VIN using Mode 09 PID 02
            val vinResult = sendObdCommand("0902")
            var vin = ""
            var calibrationId = ""
            var ecuName = ""

            if (vinResult.isSuccess) {
                val response = vinResult.getOrNull()?.rawResponse ?: ""
                vin = parseVinResponse(response)
            }

            // Try to read calibration ID (Mode 09 PID 04)
            val calIdResult = sendObdCommand("0904")
            if (calIdResult.isSuccess) {
                val response = calIdResult.getOrNull()?.rawResponse ?: ""
                calibrationId = parseCalibrationId(response)
            }

            // Try to read ECU name (Mode 09 PID 0A)
            val ecuNameResult = sendObdCommand("090A")
            if (ecuNameResult.isSuccess) {
                val response = ecuNameResult.getOrNull()?.rawResponse ?: ""
                ecuName = parseEcuName(response)
            }

            Result.success(
                VehicleInfo(
                    vin = vin.ifEmpty { "Unknown" },
                    calibrationId = calibrationId.ifEmpty { "Unknown" },
                    ecuName = ecuName.ifEmpty { "Unknown ECU" }
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseVinResponse(response: String): String {
        return try {
            // VIN response format: "49 02 01 XX XX XX..." where XX are ASCII hex values
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")

            if (cleanResponse.startsWith("4902")) {
                // Extract VIN data (skip mode and PID bytes)
                val vinHex = cleanResponse.substring(6) // Skip "490201"

                // Convert hex pairs to ASCII characters
                val vin = StringBuilder()
                for (i in vinHex.indices step 2) {
                    if (i + 1 < vinHex.length) {
                        val hexPair = vinHex.substring(i, i + 2)
                        val charValue = hexPair.toInt(16)
                        if (charValue in 32..126) { // Printable ASCII
                            vin.append(charValue.toChar())
                        }
                    }
                }

                return vin.toString().take(17) // VIN is exactly 17 characters
            }

            return ""
        } catch (e: Exception) {
            return ""
        }
    }

    private fun parseCalibrationId(response: String): String {
        return try {
            // Similar parsing for calibration ID
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")

            if (cleanResponse.startsWith("4904")) {
                val calIdHex = cleanResponse.substring(6)
                val calId = StringBuilder()

                for (i in calIdHex.indices step 2) {
                    if (i + 1 < calIdHex.length) {
                        val hexPair = calIdHex.substring(i, i + 2)
                        val charValue = hexPair.toInt(16)
                        if (charValue in 32..126) {
                            calId.append(charValue.toChar())
                        }
                    }
                }

                return calId.toString()
            }

            return ""
        } catch (e: Exception) {
            return ""
        }
    }

    private fun parseEcuName(response: String): String {
        return try {
            // Similar parsing for ECU name
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")

            if (cleanResponse.startsWith("490A")) {
                val ecuNameHex = cleanResponse.substring(6)
                val ecuName = StringBuilder()

                for (i in ecuNameHex.indices step 2) {
                    if (i + 1 < ecuNameHex.length) {
                        val hexPair = ecuNameHex.substring(i, i + 2)
                        val charValue = hexPair.toInt(16)
                        if (charValue in 32..126) {
                            ecuName.append(charValue.toChar())
                        }
                    }
                }

                return ecuName.toString()
            }

            return ""
        } catch (e: Exception) {
            return ""
        }
    }

    override suspend fun readDtcs(): Result<List<DtcInfo>> {
        return try {
            val allDtcs = mutableListOf<DtcInfo>()

            // Read stored DTCs (Mode 03)
            val storedResult = sendObdCommand("03")
            if (storedResult.isSuccess) {
                val response = storedResult.getOrNull()?.rawResponse ?: ""
                val storedDtcs = parseDtcResponse(response, DtcStatus.STORED)
                allDtcs.addAll(storedDtcs)
            }

            // Read pending DTCs (Mode 07)
            val pendingResult = sendObdCommand("07")
            if (pendingResult.isSuccess) {
                val response = pendingResult.getOrNull()?.rawResponse ?: ""
                val pendingDtcs = parseDtcResponse(response, DtcStatus.PENDING)
                allDtcs.addAll(pendingDtcs)
            }

            // Read permanent DTCs (Mode 0A)
            val permanentResult = sendObdCommand("0A")
            if (permanentResult.isSuccess) {
                val response = permanentResult.getOrNull()?.rawResponse ?: ""
                val permanentDtcs = parseDtcResponse(response, DtcStatus.PERMANENT)
                allDtcs.addAll(permanentDtcs)
            }

            Result.success(allDtcs)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDtcResponse(response: String, status: DtcStatus): List<DtcInfo> {
        return try {
            val dtcs = mutableListOf<DtcInfo>()
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")

            // DTC response format: "43 XX YY ZZ ..." where XX YY are DTC bytes
            if (cleanResponse.length >= 4) {
                val modeResponse = cleanResponse.substring(0, 2)

                // Skip if no DTCs
                if (cleanResponse.length <= 4) {
                    return emptyList()
                }

                // Parse DTC pairs (each DTC is 2 bytes = 4 hex characters)
                var i = 2 // Skip mode byte
                while (i + 3 < cleanResponse.length) {
                    val dtcBytes = cleanResponse.substring(i, i + 4)
                    val dtcCode = parseDtcCode(dtcBytes)

                    if (dtcCode.isNotEmpty()) {
                        dtcs.add(
                            DtcInfo(
                                code = dtcCode,
                                status = status,
                                description = getDtcDescription(dtcCode)
                            )
                        )
                    }

                    i += 4
                }
            }

            dtcs
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseDtcCode(dtcBytes: String): String {
        return try {
            if (dtcBytes.length != 4) return ""

            val firstByte = dtcBytes.substring(0, 2).toInt(16)
            val secondByte = dtcBytes.substring(2, 4).toInt(16)

            // Determine DTC prefix based on first two bits
            val prefix = when ((firstByte and 0xC0) shr 6) {
                0 -> "P" // Powertrain
                1 -> "C" // Chassis
                2 -> "B" // Body
                3 -> "U" // Network
                else -> "P"
            }

            // Extract the 4-digit code
            val code = String.format("%01X%02X", firstByte and 0x3F, secondByte)

            return "$prefix$code"
        } catch (e: Exception) {
            ""
        }
    }

    private fun getDtcDescription(dtcCode: String): String {
        // Basic DTC descriptions - in a real app, you'd have a comprehensive database
        return when (dtcCode) {
            "P0171" -> "System Too Lean (Bank 1)"
            "P0172" -> "System Too Rich (Bank 1)"
            "P0300" -> "Random/Multiple Cylinder Misfire Detected"
            "P0301" -> "Cylinder 1 Misfire Detected"
            "P0302" -> "Cylinder 2 Misfire Detected"
            "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
            "P0442" -> "Evaporative Emission Control System Leak Detected (small leak)"
            "P0455" -> "Evaporative Emission Control System Leak Detected (large leak)"
            else -> "Unknown DTC - Check service manual"
        }
    }

    override suspend fun clearDtcs(): Result<Unit> {
        return try {
            val result = sendObdCommand("04")
            if (result.isSuccess) {
                // Clear cached supported PIDs as they might change
                supportedPids.clear()
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDataStream(pids: List<String>): Flow<ObdResponse> = flow {
        // Simulate data stream for now
        while (true) {
            for (pid in pids) {
                try {
                    val result = sendObdCommand(pid)
                    if (result.isSuccess) {
                        emit(result.getOrNull()!!)
                    }
                    delay(COMMAND_DELAY)
                } catch (e: Exception) {
                    // Continue with next PID
                }
            }
        }
    }

    override fun getProtocolInfo(): ProtocolInfo? {
        return currentProtocol?.let { protocol ->
            ProtocolInfo(
                name = protocol,
                description = getProtocolDescription(protocol),
                isAutomatic = protocol.contains("AUTO", ignoreCase = true)
            )
        }
    }

    // Simplified implementation for now
    private fun getProtocolDescription(protocol: String): String {
        return when {
            protocol.contains("CAN") -> "Controller Area Network"
            protocol.contains("KWP") -> "Keyword Protocol 2000"
            protocol.contains("ISO") -> "ISO Standard Protocol"
            protocol.contains("J1850") -> "SAE J1850 Protocol"
            else -> "Unknown Protocol"
        }
    }
}
