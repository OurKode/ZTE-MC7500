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
    @SerialName("result") val result: JsonArray? = null
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
    val lastUpdated: Long = System.currentTimeMillis()
)
