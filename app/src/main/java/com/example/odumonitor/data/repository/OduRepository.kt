package com.example.odumonitor.data.repository

import android.content.Context
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.OduDatabaseHelper
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduNetInfoPayload
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.remote.OduApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OduRepository(
    private val context: Context? = null,
    private val apiService: OduApiService = OduApiService()
) {
    private val dbHelper = context?.let { OduDatabaseHelper(it) }
    private val widgetPrefs = context?.let { WidgetPreferences(it) }

    fun getSignalStream(pollingIntervalMs: Long = 3000L): Flow<OduSignalState> = flow {
        while (true) {
            val result = apiService.fetchNetInfo()
            if (result.isSuccess) {
                val payload = result.getOrNull()!!
                val state = mapPayloadToDomain(payload)
                saveAndPruneSignalHistory(state)
                emit(state)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Network Error"
                val offlineState = OduSignalState(
                    isConnected = false,
                    connectionType = "Disconnected",
                    errorMessage = errorMsg,
                    lastUpdated = System.currentTimeMillis()
                )
                emit(offlineState)
            }
            kotlinx.coroutines.delay(pollingIntervalMs)
        }
    }

    suspend fun fetchCurrentSignalOnce(): OduSignalState {
        val result = apiService.fetchNetInfo()
        val state = if (result.isSuccess) {
            mapPayloadToDomain(result.getOrNull()!!)
        } else {
            OduSignalState(
                isConnected = false,
                connectionType = "Disconnected",
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect",
                lastUpdated = System.currentTimeMillis()
            )
        }
        if (state.isConnected) {
            saveAndPruneSignalHistory(state)
        }
        return state
    }

    private fun saveAndPruneSignalHistory(state: OduSignalState) {
        dbHelper?.let { db ->
            db.insertSignal(state)
            val retention = widgetPrefs?.getHistoryRetention() ?: HistoryRetention.TWENTY_FOUR_HOURS
            db.pruneOldHistory(retention)
        }
    }

    fun getHistoryList(): List<OduSignalState> {
        return dbHelper?.getHistory(limit = 300) ?: emptyList()
    }

    fun clearAllHistory(): Int {
        return dbHelper?.clearAllHistory() ?: 0
    }

    fun getHistoryRetention(): HistoryRetention {
        return widgetPrefs?.getHistoryRetention() ?: HistoryRetention.TWENTY_FOUR_HOURS
    }

    fun saveHistoryRetention(retention: HistoryRetention) {
        widgetPrefs?.saveHistoryRetention(retention)
        dbHelper?.pruneOldHistory(retention)
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
}
