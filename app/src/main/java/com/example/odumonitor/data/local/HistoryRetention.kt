package com.example.odumonitor.data.local

enum class HistoryRetention(val label: String, val durationMillis: Long) {
    TWENTY_FOUR_HOURS("24 Jam", 24 * 60 * 60 * 1000L),
    ONE_WEEK("Seminggu", 7 * 24 * 60 * 60 * 1000L),
    ONE_MONTH("Sebulan", 30L * 24 * 60 * 60 * 1000L),
    ONE_YEAR("Setahun", 365L * 24 * 60 * 60 * 1000L),
    MANUAL("Manual", -1L);

    companion object {
        fun fromName(name: String?): HistoryRetention {
            return values().firstOrNull { it.name == name } ?: TWENTY_FOUR_HOURS
        }
    }
}

enum class ChartTimeRange(val shortLabel: String, val fullLabel: String, val durationMillis: Long) {
    ONE_MINUTE("1m", "1 Menit", 60 * 1000L),
    FIVE_MINUTES("5m", "5 Menit", 5 * 60 * 1000L),
    FIFTEEN_MINUTES("15m", "15 Menit", 15 * 60 * 1000L),
    ONE_HOUR("1j", "1 Jam", 60 * 60 * 1000L),
    SIX_HOURS("6j", "6 Jam", 6 * 60 * 60 * 1000L),
    TWENTY_FOUR_HOURS("24j", "24 Jam", 24 * 60 * 60 * 1000L);

    companion object {
        fun fromName(name: String?): ChartTimeRange {
            return values().firstOrNull { it.name == name } ?: FIVE_MINUTES
        }
    }
}
