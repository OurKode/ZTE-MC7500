package com.example.odumonitor.data.repository

import android.content.Context
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.OduDatabaseHelper
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.model.OduTelemetryBundle
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
            val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
            val result = apiService.fetchTelemetry(creds)
            if (result.isSuccess) {
                val bundle = result.getOrNull()!!
                val state = mapPayloadToDomain(bundle)
                saveAndPruneSignalHistory(state)
                emit(state)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Network Error"
                val lastGood = getLastCachedSignal()
                val fallbackState = if (lastGood != null && lastGood.isConnected) {
                    lastGood
                } else {
                    OduSignalState(
                        isConnected = false,
                        connectionType = "Disconnected",
                        errorMessage = errorMsg,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                emit(fallbackState)
            }
            kotlinx.coroutines.delay(pollingIntervalMs)
        }
    }

    suspend fun fetchCurrentSignalOnce(): OduSignalState {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        val result = apiService.fetchTelemetry(creds)
        val state = if (result.isSuccess) {
            val mapped = mapPayloadToDomain(result.getOrNull()!!)
            saveAndPruneSignalHistory(mapped)
            mapped
        } else {
            val lastGood = getLastCachedSignal()
            if (lastGood != null && lastGood.isConnected) {
                lastGood
            } else {
                OduSignalState(
                    isConnected = false,
                    connectionType = "Disconnected",
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect",
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
        return state
    }

    suspend fun testLogin(creds: RouterCredentials): Result<String> {
        return apiService.login(creds.host, creds.username, creds.password)
    }

    fun getLastCachedSignal(): OduSignalState? {
        return dbHelper?.getHistory(limit = 1)?.firstOrNull()
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

    private fun mapPayloadToDomain(bundle: OduTelemetryBundle): OduSignalState {
        val p = bundle.netInfo
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
            lastUpdated = System.currentTimeMillis(),

            // Router Telemetry & Traffic
            isRouterLoggedIn = bundle.isLoggedIn,
            wanIp = bundle.routerStatus?.wanIp,
            wanIpv6 = bundle.routerStatus?.wanIpv6,
            wanStatus = bundle.routerStatus?.currentWanStatus,
            wanMode = bundle.routerStatus?.opmsWanMode,
            downloadSpeedBps = bundle.traffic?.realRxSpeed ?: 0L,
            uploadSpeedBps = bundle.traffic?.realTxSpeed ?: 0L,
            dayRxBytes = bundle.traffic?.dayRxBytes ?: 0L,
            monthRxBytes = bundle.traffic?.monthRxBytes ?: 0L,
            connectedDevicesCount = bundle.userListNum?.accessTotalNum ?: 0,
            simPhoneNumber = bundle.simInfo?.msisdn
        )
    }
}
