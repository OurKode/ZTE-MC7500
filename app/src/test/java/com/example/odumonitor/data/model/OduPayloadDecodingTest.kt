package com.example.odumonitor.data.model

import com.example.odumonitor.data.remote.OduApiService
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Decodes captured MC7500 responses through the same path production uses:
 * a UBUS envelope -> UbusResponse.extractPayload -> @Serializable payload.
 */
class OduPayloadDecodingTest {

    private val json: Json = OduApiService.SharedJson

    private inline fun <reified T> decodePayload(payloadJson: String): T {
        val envelope = """[{"jsonrpc":"2.0","id":1,"result":[0,$payloadJson]}]"""
        val response = json.decodeFromString<List<UbusResponse>>(envelope).single()
        val decoded = response.extractPayload<T>(json)
        assertNotNull("Payload failed to decode: $payloadJson", decoded)
        return decoded!!
    }

    @Test
    fun trafficPayload_decodesAllVerifiedCounters() {
        val t = decodePayload<OduTrafficPayload>(
            """{"cid":1,"real_time":5438,"real_tx_bytes":196606031,"real_rx_bytes":3244788276,
               "real_tx_speed":20457,"real_rx_speed":463137,"real_max_tx_speed":552330,
               "real_max_rx_speed":4214048,"day_time":65322,"day_tx_bytes":2083622354,
               "day_rx_bytes":45235165638,"day_tx_packets":13157478,"day_rx_packets":37087839,
               "day_tx_drop_packets":26,"day_rx_drop_packets":26,"day_tx_error_packets":0,
               "day_rx_error_packets":34,"month_time":839926,"month_tx_bytes":43620316349,
               "month_rx_bytes":499803094852,"total_tx_bytes":51780098115,"total_rx_bytes":4316739698602}"""
        )

        assertEquals(5438L, t.realTime)
        assertEquals(2083622354L, t.dayTxBytes)
        assertEquals(45235165638L, t.dayRxBytes)
        assertEquals(65322L, t.dayTime)
        assertEquals(34L, t.dayRxErrorPackets)
        assertEquals(26L, t.dayTxDropPackets)
        assertEquals(43620316349L, t.monthTxBytes)
        assertEquals(51780098115L, t.totalTxBytes)
        assertEquals(552330L, t.realMaxTxSpeed)
    }

    @Test
    fun thermalPayload_decodesNumericCpusssTemp() {
        val t = decodePayload<OduThermalPayload>("""{"cpuss_temp":52}""")
        assertEquals("52", t.cpussTemp)
    }

    @Test
    fun deviceInfoPayload_decodesUptimeDeliveredAsString() {
        val d = decodePayload<OduDeviceInfoPayload>(
            """{"device_uptime":"5449","reboot_schedule_enable":"0","reboot_hour1":"2",
               "WatchDog_Setting_Option":"on","wa_inner_version":"",
               "meminfo":{"total":"84600","free":"10044","avaliable":"29896"}}"""
        )
        assertEquals(5449L, d.deviceUptime)
    }

    @Test
    fun userListPayload_decodesAccessTotalNum() {
        val u = decodePayload<OduUserListNumPayload>(
            """{"access_total_num":1,"lan_num":1,"wireless_num":0,"offline_num":0}"""
        )
        assertEquals(1, u.accessTotalNum)
    }

    @Test
    fun netInfoPayload_decodesLiveSample() {
        val p = decodePayload<OduNetInfoPayload>(
            """{"net_select":"LTE_AND_5G","network_type":"ENDC","signalbar":"5",
               "wan_active_band":"LTE BAND 3","network_provider":"XL Axiata",
               "cell_id":56790788,"lte_pci":339,"wan_active_channel":1325,
               "nr5g_pci":217,"nr5g_action_channel":504990,"nr5g_action_band":"n41",
               "lte_band_lock":"0x000000014","nr5g_sa_band_lock":"41,40",
               "nr5g_rsrp":-73,"nr5g_rsrq":-11,"nr5g_snr":"17.5",
               "lte_rsrp":-81,"lte_rsrq":-4,"lte_snr":"17.0"}"""
        )

        assertEquals(-81, p.lteRsrp)
        assertEquals(-73, p.nr5gRsrp)
        assertEquals("17.5", p.nr5gSnr)
        assertEquals("LTE BAND 3", p.wanActiveBand)
        assertEquals("n41", p.nr5gActionBand)
        assertEquals("0x000000014", p.lteBandLock)
        assertEquals("504990", p.nr5gActionChannel)
    }
}
