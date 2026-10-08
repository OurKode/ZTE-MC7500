package com.example.odumonitor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class UbusResponse(
    @SerialName("jsonrpc") val jsonrpc: String? = null,
    @SerialName("id") val id: Int? = null,
    @SerialName("result") val result: JsonArray? = null,
    @SerialName("error") val error: kotlinx.serialization.json.JsonElement? = null
) {
    inline fun <reified T> extractPayload(json: Json): T? {
        return try {
            result?.getOrNull(1)?.let {
                json.decodeFromJsonElement<T>(it)
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class OduNetInfoPayload(
    @SerialName("network_type") val networkType: String = "UNKNOWN",
    @SerialName("network_provider") val networkProvider: String? = null,
    @SerialName("network_provider_fullname") val networkProviderFullname: String? = null,
    @SerialName("signalbar") val signalBar: String? = "0",
    
    // 5G Metrics
    @SerialName("nr5g_rsrp") val nr5gRsrp: Int? = null,
    @SerialName("nr5g_rsrq") val nr5gRsrq: Int? = null,
    @SerialName("nr5g_snr") val nr5gSnr: String? = null,
    @SerialName("nr5g_action_band") val nr5gActionBand: String? = null,
    @SerialName("nr5g_pci") val nr5gPci: Int? = null,
    @SerialName("nr5g_cell_id") val nr5gCellId: Long? = null,
    
    // 4G Metrics
    @SerialName("lte_rsrp") val lteRsrp: Int? = null,
    @SerialName("lte_rsrq") val lteRsrq: Int? = null,
    @SerialName("lte_snr") val lteSnr: String? = null,
    @SerialName("wan_active_band") val wanActiveBand: String? = null,
    @SerialName("cell_id") val cellId: Long? = null,
    @SerialName("lte_pci") val ltePci: Int? = null,
    
    // CA & Neighbors
    @SerialName("lteca_state") val lteCaState: Int? = 0,
    @SerialName("lteca") val lteCaString: String? = null,
    @SerialName("lte_neighbor_cell") val lteNeighborCell: String? = null
)

@Serializable
data class OduRouterStatusPayload(
    @SerialName("current_wan_status") val currentWanStatus: String? = null,
    @SerialName("opms_wan_mode") val opmsWanMode: String? = null,
    @SerialName("mwan_wanlan1_wan_ipaddr") val wanIp: String? = null,
    @SerialName("mwan_wanlan1_ipv6_wan_ipaddr") val wanIpv6: String? = null
)

@Serializable
data class OduTrafficPayload(
    @SerialName("real_tx_speed") val realTxSpeed: Long? = null,
    @SerialName("real_rx_speed") val realRxSpeed: Long? = null,
    @SerialName("day_rx_bytes") val dayRxBytes: Long? = null,
    @SerialName("month_rx_bytes") val monthRxBytes: Long? = null
)

@Serializable
data class OduSimInfoPayload(
    @SerialName("msisdn") val msisdn: String? = null,
    @SerialName("sim_states") val simStates: String? = null,
    @SerialName("sim_iccid") val simIccid: String? = null
)

@Serializable
data class OduUserListNumPayload(
    @SerialName("access_total_num") val accessTotalNum: Int? = null,
    @SerialName("wireless_num") val wirelessNum: Int? = null,
    @SerialName("lan_num") val lanNum: Int? = null
)

@Serializable
data class OduLoginInfoPayload(
    @SerialName("zte_web_sault") val zteWebSault: String? = null,
    @SerialName("login_fail_num") val loginFailNum: Int? = null,
    @SerialName("login_fail_lock_lefttime") val loginFailLockLefttime: Int? = null
)

@Serializable
data class OduLoginResultPayload(
    @SerialName("result") val result: Int? = null,
    @SerialName("ubus_rpc_session") val ubusRpcSession: String? = null,
    @SerialName("timeout") val timeout: Int? = null
)

data class OduTelemetryBundle(
    val netInfo: OduNetInfoPayload,
    val routerStatus: OduRouterStatusPayload? = null,
    val traffic: OduTrafficPayload? = null,
    val simInfo: OduSimInfoPayload? = null,
    val userListNum: OduUserListNumPayload? = null,
    val isLoggedIn: Boolean = false
)

data class OduSignalState(
    val isConnected: Boolean = false,
    val connectionType: String = "Disconnected",
    val provider: String = "-",
    val signalBar: Int = 0,
    
    // Parsed Metrics
    val lteRsrp: Int = 0,
    val lteRsrq: Int = 0,
    val lteSinr: Float = 0f,
    val lteBand: String = "-",
    val ltePci: Int = 0,
    val lteCellId: Long = 0L,
    
    val nrRsrp: Int = 0,
    val nrRsrq: Int = 0,
    val nrSinr: Float = 0f,
    val nrBand: String = "-",
    val nrPci: Int = 0,
    val nrCellId: Long = 0L,
    
    val isCaActive: Boolean = false,
    val caDetails: String = "-",
    val neighborCells: String = "-",
    val errorMessage: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),

    // Authenticated Router Status & Traffic Metrics
    val isRouterLoggedIn: Boolean = false,
    val wanIp: String? = null,
    val wanIpv6: String? = null,
    val wanStatus: String? = null,
    val wanMode: String? = null,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val dayRxBytes: Long = 0L,
    val monthRxBytes: Long = 0L,
    val connectedDevicesCount: Int = 0,
    val simPhoneNumber: String? = null
)
