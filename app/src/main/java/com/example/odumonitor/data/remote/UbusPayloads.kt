package com.example.odumonitor.data.remote

/**
 * Request bodies for the zte_nwinfo_api write methods, kept as pure functions so
 * the exact byte-for-byte payload can be asserted in unit tests. These are the
 * only place where a key name is allowed to be written; the vendor script
 * (zte-script-ng.js) is the source of truth for every key below.
 */
object UbusPayloads {

    /** nwinfo_set_netselect: net_select is the numeric bearer mode as a string. */
    fun networkSelect(mode: String): String = """{"net_select":"$mode"}"""

    /** nwinfo_set_gwl_bandlock: LTE band mask, decimal string, GW side disabled. */
    fun lock4gBands(maskDecimalString: String): String =
        """{"is_gw_band":"0","gw_band_mask":"0","is_lte_band":"1","lte_band_mask":"$maskDecimalString"}"""

    /** nwinfo_set_nrbandlock: nr5g_type is ignored by the firmware, SA is sent anyway. */
    fun lock5gBands(bandsCommaSeparated: String): String =
        """{"nr5g_type":"SA","nr5g_band":"$bandsCommaSeparated"}"""

    /** nwinfo_lock_lte_cell: PCI and EARFCN as strings. Zero means unlock. */
    fun lock4gCell(pci: Int, earfcn: Int): String =
        """{"lock_lte_pci":"$pci","lock_lte_earfcn":"$earfcn"}"""

    /**
     * nwinfo_lock_nr_cell: the firmware accepts lock_nr_pci / lock_nr_earfcn /
     * lock_nr_cell_band as strings. The nr5g_* key names used by the router UI
     * itself are not what the vendor script sends and are not accepted here.
     */
    fun lock5gCell(pci: Int, earfcn: Int, band: Int): String =
        """{"lock_nr_pci":"$pci","lock_nr_earfcn":"$earfcn","lock_nr_cell_band":"$band"}"""
}
