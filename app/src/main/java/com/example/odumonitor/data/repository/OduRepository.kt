package com.example.odumonitor.data.repository

import com.example.odumonitor.data.model.OduNetInfoPayload
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.remote.OduApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class OduRepository(
    private val apiService: OduApiService = OduApiService()
) {
    var isDemoMode: Boolean = false

    fun getSignalStream(pollingIntervalMs: Long = 3000L): Flow<OduSignalState> = flow {
        while (true) {
            if (isDemoMode) {
                emit(generateDemoState())
            } else {
                val result = apiService.fetchNetInfo()
                if (result.isSuccess) {
                    val payload = result.getOrNull()!!
                    emit(mapPayloadToDomain(payload))
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Network Error"
                    emit(
                        OduSignalState(
                            isConnected = false,
                            connectionType = "Disconnected",
                            errorMessage = errorMsg,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
            kotlinx.coroutines.delay(pollingIntervalMs)
        }
    }

    suspend fun fetchCurrentSignalOnce(): OduSignalState {
        if (isDemoMode) return generateDemoState()
        val result = apiService.fetchNetInfo()
        return if (result.isSuccess) {
            mapPayloadToDomain(result.getOrNull()!!)
        } else {
            OduSignalState(
                isConnected = false,
                connectionType = "Disconnected",
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect",
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun mapPayloadToDomain(p: OduNetInfoPayload): OduSignalState {
        val lteSinrVal = p.lteSnr?.toFloatOrNull() ?: 0f
        val nrSinrVal = p.nr5gSnr?.toFloatOrNull() ?: 0f
        val signalBars = p.signalBar?.toIntOrNull() ?: 0

        return OduSignalState(
            isConnected = true,
            connectionType = p.networkType.ifBlank { "LTE/5G" },
            provider = p.networkProviderFullname ?: p.networkProvider ?: "ZTE MC7500",
            signalBar = signalBars,
            
            lteRsrp = p.lteRsrp ?: 0,
            lteRsrq = p.lteRsrq ?: 0,
            lteSinr = lteSinrVal,
            lteBand = p.wanActiveBand ?: "-",
            ltePci = p.ltePci ?: 0,
            lteCellId = p.cellId ?: 0L,
            
            nrRsrp = p.nr5gRsrp ?: 0,
            nrRsrq = p.nr5gRsrq ?: 0,
            nrSinr = nrSinrVal,
            nrBand = p.nr5gActionBand ?: "-",
            nrPci = p.nr5gPci ?: 0,
            nrCellId = p.nr5gCellId ?: 0L,
            
            isCaActive = (p.lteCaState ?: 0) > 0,
            caDetails = p.lteCaString ?: "-",
            neighborCells = p.lteNeighborCell ?: "-",
            errorMessage = null,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun generateDemoState(): OduSignalState {
        val randomRsrpOffset = Random.nextInt(-4, 4)
        val randomSinrOffset = Random.nextFloat() * 2f - 1f

        return OduSignalState(
            isConnected = true,
            connectionType = "ENDC (5G NSA)",
            provider = "Telkomsel 5G Ultra",
            signalBar = 4,
            
            lteRsrp = -78 + randomRsrpOffset,
            lteRsrq = -10,
            lteSinr = 22.4f + randomSinrOffset,
            lteBand = "LTE Band 3 (1800MHz)",
            ltePci = 339,
            lteCellId = 2260613L,
            
            nrRsrp = -72 + randomRsrpOffset,
            nrRsrq = -8,
            nrSinr = 28.5f + randomSinrOffset,
            nrBand = "n40 (2300MHz)",
            nrPci = 104,
            nrCellId = 884102L,
            
            isCaActive = true,
            caDetails = "LTE B3 (20MHz) + LTE B1 (15MHz) + 5G n40 (100MHz)",
            neighborCells = "PCI:339 (-80dBm); PCI:392 (-88dBm); PCI:367 (-92dBm)",
            errorMessage = null,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
