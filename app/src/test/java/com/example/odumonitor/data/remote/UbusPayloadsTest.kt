package com.example.odumonitor.data.remote

import com.example.odumonitor.util.FrequencyConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the write payloads to the contract in the vendor script. A silent rename
 * here is what breaks band lock on real hardware, and it breaks it in a way the
 * UI cannot report, so the key names are asserted literally.
 */
class UbusPayloadsTest {

    private fun fields(payload: String) = Json.parseToJsonElement(payload).jsonObject

    @Test
    fun `5g cell lock uses script key names and string values`() {
        val obj = fields(UbusPayloads.lock5gCell(pci = 412, earfcn = 627264, band = 78))

        assertEquals(setOf("lock_nr_pci", "lock_nr_earfcn", "lock_nr_cell_band"), obj.keys)
        assertEquals("412", obj.getValue("lock_nr_pci").jsonPrimitive.content)
        assertEquals("627264", obj.getValue("lock_nr_earfcn").jsonPrimitive.content)
        assertEquals("78", obj.getValue("lock_nr_cell_band").jsonPrimitive.content)
    }

    @Test
    fun `5g cell unlock sends zeroes on the same payload shape`() {
        val obj = fields(UbusPayloads.lock5gCell(0, 0, 0))

        assertEquals("0", obj.getValue("lock_nr_pci").jsonPrimitive.content)
        assertEquals("0", obj.getValue("lock_nr_earfcn").jsonPrimitive.content)
        assertEquals("0", obj.getValue("lock_nr_cell_band").jsonPrimitive.content)
    }

    @Test
    fun `legacy 5g cell keys must not reappear`() {
        val payload = UbusPayloads.lock5gCell(412, 627264, 78)

        listOf("nr5g_CellLockType", "nr5g_pci", "nr5g_arfcn", "nr5g_band", "nr5g_scs").forEach { key ->
            assert(!payload.contains(key)) { "legacy key $key is still present in $payload" }
        }
    }

    @Test
    fun `4g cell lock sends pci and earfcn as strings`() {
        val obj = fields(UbusPayloads.lock4gCell(pci = 137, earfcn = 1650))

        assertEquals("137", obj.getValue("lock_lte_pci").jsonPrimitive.content)
        assertEquals("1650", obj.getValue("lock_lte_earfcn").jsonPrimitive.content)
    }

    @Test
    fun `4g band lock carries the decimal mask and disables the gw side`() {
        val mask = FrequencyConverter.calculate4gBandMask(listOf(1, 3, 8))
        val obj = fields(UbusPayloads.lock4gBands(mask.toString()))

        assertEquals("0", obj.getValue("is_gw_band").jsonPrimitive.content)
        assertEquals("0", obj.getValue("gw_band_mask").jsonPrimitive.content)
        assertEquals("1", obj.getValue("is_lte_band").jsonPrimitive.content)
        assertEquals(mask.toString(), obj.getValue("lte_band_mask").jsonPrimitive.content)
    }

    @Test
    fun `4g band mask sets one bit per band index`() {
        // Bit n-1 keeps the mask human-checkable: B1 -> 1, B3 -> 4, B40 -> 2^39.
        assertEquals(1L, FrequencyConverter.calculate4gBandMask(listOf(1)))
        assertEquals(4L, FrequencyConverter.calculate4gBandMask(listOf(3)))
        assertEquals(1L or 4L or 128L, FrequencyConverter.calculate4gBandMask(listOf(1, 3, 8)))
        assertEquals(0L, FrequencyConverter.calculate4gBandMask(emptyList()))
    }

    @Test
    fun `4g band mask round trips through the parser`() {
        val bands = listOf(1, 3, 8, 40)
        val mask = FrequencyConverter.calculate4gBandMask(bands)

        assertEquals(bands, FrequencyConverter.parse4gActiveBands(mask).sorted())
    }

    @Test
    fun `5g band lock sends the comma separated band list`() {
        val obj = fields(UbusPayloads.lock5gBands("1,3,28,40,78"))

        assertEquals("SA", obj.getValue("nr5g_type").jsonPrimitive.content)
        assertEquals("1,3,28,40,78", obj.getValue("nr5g_band").jsonPrimitive.content)
    }

    @Test
    fun `network select sends the mode as a string`() {
        val obj = fields(UbusPayloads.networkSelect("9"))

        assertEquals("9", obj.getValue("net_select").jsonPrimitive.content)
    }
}
