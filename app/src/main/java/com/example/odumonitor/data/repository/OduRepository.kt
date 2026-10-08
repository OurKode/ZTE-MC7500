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

        val caPrimaryParts = p.lteCaString?.split(";")?.firstOrNull()?.split(",")?.map { it.trim() }
        val lteEarfcnInt = p.lteActionChannel?.toIntOrNull()
            ?: caPrimaryParts?.getOrNull(3)?.toIntOrNull()
        val lteFreq = lteEarfcnInt?.let { com.example.odumonitor.util.FrequencyConverter.convert4gEarfcnToMhz(it) }
        val rawLteBw = p.lteBandwidth ?: caPrimaryParts?.getOrNull(4)
        val lteBwFormatted = rawLteBw?.let { if (it.endsWith("MHz", true)) it else "${it} MHz" }

        val nrArfcnInt = p.nr5gActionChannel?.toIntOrNull()
        val nrFreq = nrArfcnInt?.let { com.example.odumonitor.util.FrequencyConverter.convert5gArfcnToMhz(it) }

        val cpuTempFormatted = bundle.thermal?.cpussTemp?.let { if (it.isNotBlank() && it != "0") "$it°C" else null }

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
            lteEarfcn = lteEarfcnInt,
            lteDlFreqMhz = lteFreq,
            lteBandwidth = lteBwFormatted,

            nrRsrp = p.nr5gRsrp ?: 0,
            nrRsrq = p.nr5gRsrq ?: 0,
            nrSinr = nrSinrVal,
            nrBand = p.nr5gActionBand ?: "-",
            nrPci = p.nr5gPci ?: 0,
            nrCellId = p.nr5gCellId ?: 0L,
            nrArfcn = nrArfcnInt,
            nrDlFreqMhz = nrFreq,
            nrBandwidth = p.nr5gBandwidth?.let { if (it.endsWith("MHz", true)) it else "${it} MHz" },

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
            simPhoneNumber = bundle.simInfo?.msisdn,

            // Deep Diagnostics Telemetry
            cpuTemp = cpuTempFormatted,
            deviceUptimeSeconds = bundle.deviceInfo?.deviceUptime,
            softwareVersion = bundle.deviceInfo?.softwareVersion ?: bundle.deviceInfo?.waInnerVersion,

            // Radio Selection & Cell Locks
            netSelect = p.netSelect,
            lteBandLock = p.lteBandLock,
            lockLteCell = p.lockLteCell,
            lockNrCell = p.lockNrCell
        )
    }

    suspend fun setNetworkSelect(mode: String): Result<Boolean> {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        return apiService.executeAuthenticatedCommand(
            creds = creds,
            service = "zte_nwinfo_api",
            method = "nwinfo_set_netselect",
            paramsJson = """{"net_select":"$mode"}""",
            tag = "nwinfo_set_netselect"
        )
    }

    suspend fun set4gBandLock(maskDecimalString: String): Result<Boolean> {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        return apiService.executeAuthenticatedCommand(
            creds = creds,
            service = "zte_nwinfo_api",
            method = "nwinfo_set_gwl_bandlock",
            paramsJson = """{"is_gw_band":"0","gw_band_mask":"0","is_lte_band":"1","lte_band_mask":"$maskDecimalString"}""",
            tag = "nwinfo_set_gwl_bandlock"
        )
    }

    suspend fun set5gBandLock(bandsCommaSeparated: String): Result<Boolean> {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        return apiService.executeAuthenticatedCommand(
            creds = creds,
            service = "zte_nwinfo_api",
            method = "nwinfo_set_nrbandlock",
            paramsJson = """{"nr5g_type":"SA","nr5g_band":"$bandsCommaSeparated"}""",
            tag = "nwinfo_set_nrbandlock"
        )
    }

    suspend fun lock4gCell(pci: Int, earfcn: Int): Result<Boolean> {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        return apiService.executeAuthenticatedCommand(
            creds = creds,
            service = "zte_nwinfo_api",
            method = "nwinfo_lock_lte_cell",
            paramsJson = """{"lock_lte_pci":"$pci","lock_lte_earfcn":"$earfcn"}""",
            tag = "nwinfo_lock_lte_cell"
        )
    }

    suspend fun unlock4gCell(): Result<Boolean> {
        return lock4gCell(0, 0)
    }

    suspend fun lock5gCell(pci: Int, earfcnOrArfcn: Int, bandNumber: Int): Result<Boolean> {
        val creds = widgetPrefs?.getRouterCredentials() ?: RouterCredentials()
        return apiService.executeAuthenticatedCommand(
            creds = creds,
            service = "zte_nwinfo_api",
            method = "nwinfo_lock_nr_cell",
            paramsJson = """{"lock_nr_pci":"$pci","lock_nr_earfcn":"$earfcnOrArfcn","lock_nr_cell_band":"$bandNumber"}""",
            tag = "nwinfo_lock_nr_cell"
        )
    }

    suspend fun unlock5gCell(): Result<Boolean> {
        return lock5gCell(0, 0, 0)
    }
}
