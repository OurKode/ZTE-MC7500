package com.example.odumonitor.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.odumonitor.data.model.OduSignalState

class OduDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "odu_monitor.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_HISTORY = "signal_history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_IS_CONNECTED = "is_connected"
        private const val COLUMN_CONNECTION_TYPE = "connection_type"
        private const val COLUMN_PROVIDER = "provider"
        private const val COLUMN_SIGNAL_BAR = "signal_bar"
        private const val COLUMN_LTE_RSRP = "lte_rsrp"
        private const val COLUMN_LTE_RSRQ = "lte_rsrq"
        private const val COLUMN_LTE_SINR = "lte_sinr"
        private const val COLUMN_LTE_BAND = "lte_band"
        private const val COLUMN_LTE_PCI = "lte_pci"
        private const val COLUMN_LTE_CELL_ID = "lte_cell_id"
        private const val COLUMN_NR_RSRP = "nr_rsrp"
        private const val COLUMN_NR_RSRQ = "nr_rsrq"
        private const val COLUMN_NR_SINR = "nr_sinr"
        private const val COLUMN_NR_BAND = "nr_band"
        private const val COLUMN_NR_PCI = "nr_pci"
        private const val COLUMN_NR_CELL_ID = "nr_cell_id"
        private const val COLUMN_IS_CA_ACTIVE = "is_ca_active"
        private const val COLUMN_CA_DETAILS = "ca_details"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_IS_CONNECTED INTEGER NOT NULL,
                $COLUMN_CONNECTION_TYPE TEXT,
                $COLUMN_PROVIDER TEXT,
                $COLUMN_SIGNAL_BAR INTEGER,
                $COLUMN_LTE_RSRP INTEGER,
                $COLUMN_LTE_RSRQ INTEGER,
                $COLUMN_LTE_SINR REAL,
                $COLUMN_LTE_BAND TEXT,
                $COLUMN_LTE_PCI INTEGER,
                $COLUMN_LTE_CELL_ID INTEGER,
                $COLUMN_NR_RSRP INTEGER,
                $COLUMN_NR_RSRQ INTEGER,
                $COLUMN_NR_SINR REAL,
                $COLUMN_NR_BAND TEXT,
                $COLUMN_NR_PCI INTEGER,
                $COLUMN_NR_CELL_ID INTEGER,
                $COLUMN_IS_CA_ACTIVE INTEGER,
                $COLUMN_CA_DETAILS TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun insertSignal(state: OduSignalState) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, state.lastUpdated)
            put(COLUMN_IS_CONNECTED, if (state.isConnected) 1 else 0)
            put(COLUMN_CONNECTION_TYPE, state.connectionType)
            put(COLUMN_PROVIDER, state.provider)
            put(COLUMN_SIGNAL_BAR, state.signalBar)
            put(COLUMN_LTE_RSRP, state.lteRsrp)
            put(COLUMN_LTE_RSRQ, state.lteRsrq)
            put(COLUMN_LTE_SINR, state.lteSinr)
            put(COLUMN_LTE_BAND, state.lteBand)
            put(COLUMN_LTE_PCI, state.ltePci)
            put(COLUMN_LTE_CELL_ID, state.lteCellId)
            put(COLUMN_NR_RSRP, state.nrRsrp)
            put(COLUMN_NR_RSRQ, state.nrRsrq)
            put(COLUMN_NR_SINR, state.nrSinr)
            put(COLUMN_NR_BAND, state.nrBand)
            put(COLUMN_NR_PCI, state.nrPci)
            put(COLUMN_NR_CELL_ID, state.nrCellId)
            put(COLUMN_IS_CA_ACTIVE, if (state.isCaActive) 1 else 0)
            put(COLUMN_CA_DETAILS, state.caDetails)
        }
        db.insert(TABLE_HISTORY, null, values)
    }

    fun getHistory(limit: Int = 200): List<OduSignalState> {
        val list = mutableListOf<OduSignalState>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC",
            limit.toString()
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val state = OduSignalState(
                    isConnected = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_CONNECTED)) == 1,
                    connectionType = c.getString(c.getColumnIndexOrThrow(COLUMN_CONNECTION_TYPE)) ?: "",
                    provider = c.getString(c.getColumnIndexOrThrow(COLUMN_PROVIDER)) ?: "-",
                    signalBar = c.getInt(c.getColumnIndexOrThrow(COLUMN_SIGNAL_BAR)),
                    lteRsrp = c.getInt(c.getColumnIndexOrThrow(COLUMN_LTE_RSRP)),
                    lteRsrq = c.getInt(c.getColumnIndexOrThrow(COLUMN_LTE_RSRQ)),
                    lteSinr = c.getFloat(c.getColumnIndexOrThrow(COLUMN_LTE_SINR)),
                    lteBand = c.getString(c.getColumnIndexOrThrow(COLUMN_LTE_BAND)) ?: "-",
                    ltePci = c.getInt(c.getColumnIndexOrThrow(COLUMN_LTE_PCI)),
                    lteCellId = c.getLong(c.getColumnIndexOrThrow(COLUMN_LTE_CELL_ID)),
                    nrRsrp = c.getInt(c.getColumnIndexOrThrow(COLUMN_NR_RSRP)),
                    nrRsrq = c.getInt(c.getColumnIndexOrThrow(COLUMN_NR_RSRQ)),
                    nrSinr = c.getFloat(c.getColumnIndexOrThrow(COLUMN_NR_SINR)),
                    nrBand = c.getString(c.getColumnIndexOrThrow(COLUMN_NR_BAND)) ?: "-",
                    nrPci = c.getInt(c.getColumnIndexOrThrow(COLUMN_NR_PCI)),
                    nrCellId = c.getLong(c.getColumnIndexOrThrow(COLUMN_NR_CELL_ID)),
                    isCaActive = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_CA_ACTIVE)) == 1,
                    caDetails = c.getString(c.getColumnIndexOrThrow(COLUMN_CA_DETAILS)) ?: "-",
                    errorMessage = null,
                    lastUpdated = c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                )
                list.add(state)
            }
        }
        return list
    }

    fun pruneOldHistory(retention: HistoryRetention): Int {
        if (retention == HistoryRetention.MANUAL || retention.durationMillis <= 0) return 0
        val cutoffTimestamp = System.currentTimeMillis() - retention.durationMillis
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, "$COLUMN_TIMESTAMP < ?", arrayOf(cutoffTimestamp.toString()))
    }

    fun clearAllHistory(): Int {
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, null, null)
    }
}
