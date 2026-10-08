package com.example.odumonitor.util

object FrequencyConverter {

    private data class LteBandSpec(
        val band: Int,
        val fDlLow: Float,
        val nOffsDl: Int,
        val nMinDl: Int,
        val nMaxDl: Int
    )

    private val lteBands = listOf(
        LteBandSpec(1, 2110f, 0, 0, 599),
        LteBandSpec(3, 1805f, 1200, 1200, 1949),
        LteBandSpec(4, 2110f, 1950, 1950, 2399),
        LteBandSpec(5, 869f, 2400, 2400, 2649),
        LteBandSpec(7, 2620f, 2750, 2750, 3449),
        LteBandSpec(8, 925f, 3450, 3450, 3799),
        LteBandSpec(20, 791f, 6150, 6150, 6449),
        LteBandSpec(28, 758f, 9210, 9210, 9659),
        LteBandSpec(32, 1452f, 9920, 9920, 10359),
        LteBandSpec(38, 2570f, 37750, 37750, 38249),
        LteBandSpec(40, 2300f, 38650, 38650, 39649),
        LteBandSpec(42, 3400f, 41590, 41590, 43589),
        LteBandSpec(43, 3600f, 43590, 43590, 45589)
    )

    /**
     * Converts a 4G LTE EARFCN into Downlink frequency in MHz (3GPP TS 36.101).
     */
    fun convert4gEarfcnToMhz(earfcn: Int): Float? {
        for (spec in lteBands) {
            if (earfcn in spec.nMinDl..spec.nMaxDl) {
                val freq = spec.fDlLow + 0.1f * (earfcn - spec.nOffsDl)
                return Math.round(freq * 10f) / 10f
            }
        }
        return null
    }

    private data class NrBandSpec(
        val band: Int,
        val nMin: Int,
        val nMax: Int
    )

    private val nrBands = listOf(
        NrBandSpec(1, 422000, 434000),
        NrBandSpec(3, 361000, 376000),
        NrBandSpec(5, 173800, 178800),
        NrBandSpec(7, 524000, 538000),
        NrBandSpec(8, 185000, 192000),
        NrBandSpec(28, 151600, 160600),
        NrBandSpec(40, 460000, 480000),
        NrBandSpec(41, 499200, 537999),
        NrBandSpec(75, 286400, 303400),
        NrBandSpec(78, 620000, 653333),
        NrBandSpec(79, 693334, 733333),
        NrBandSpec(257, 2054167, 2104166),
        NrBandSpec(258, 2016667, 2070833),
        NrBandSpec(260, 2229167, 2279166),
        NrBandSpec(261, 2070833, 2084999)
    )

    private fun arfcnToMHz(n: Int): Float? {
        return when {
            n in 0..599999 -> 0.005f * n
            n in 600000..2016666 -> 3000f + 0.015f * (n - 600000)
            n in 2016667..3279165 -> 24250f + 0.06f * (n - 2016667)
            else -> null
        }
    }

    /**
     * Converts a 5G NR ARFCN into Downlink frequency in MHz (3GPP TS 38.104 global raster).
     */
    fun convert5gArfcnToMhz(arfcn: Int): Float? {
        for (spec in nrBands) {
            if (arfcn in spec.nMin..spec.nMax) {
                val f = arfcnToMHz(arfcn) ?: return null
                return Math.round(f * 10f) / 10f
            }
        }
        val f = arfcnToMHz(arfcn) ?: return null
        return Math.round(f * 10f) / 10f
    }

    /**
     * Formats seconds into a compact uptime string: Xh Yj Zm
     */
    fun formatUptime(seconds: Long): String {
        if (seconds <= 0) return "-"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            days > 0 -> "${days}h ${hours}j ${minutes}m"
            hours > 0 -> "${hours}j ${minutes}m"
            else -> "${minutes}m ${secs}d"
        }
    }

    val SUPPORTED_4G_BANDS = listOf(1, 3, 7, 8, 20, 28, 38, 40, 41, 42, 43)

    fun calculate4gBandMask(bands: Collection<Int>): Long {
        return bands.fold(0L) { mask, band ->
            if (band in 1..62) mask or (1L shl (band - 1)) else mask
        }
    }

    val FULL_4G_MASK: Long = calculate4gBandMask(SUPPORTED_4G_BANDS)

    fun parse4gActiveBands(maskNum: Long): List<Int> {
        val list = mutableListOf<Int>()
        for (b in 1..44) {
            if ((maskNum and (1L shl (b - 1))) != 0L) {
                list.add(b)
            }
        }
        return list
    }

    val FULL_5G_BANDS = listOf("1", "3", "7", "8", "20", "28", "38", "40", "41", "75", "77", "78")

    fun formatBearerModeName(mode: String?): String {
        return when (mode?.trim()) {
            "WL_AND_5G" -> "Auto (5G / 4G / 3G)"
            "LTE_AND_5G" -> "5G NSA + 4G LTE"
            "Only_5G" -> "5G SA Saja"
            "Only_LTE" -> "4G LTE Saja"
            else -> mode?.ifBlank { "Auto" } ?: "Auto"
        }
    }
}
