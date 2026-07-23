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
