package com.example.odumonitor.widget

import android.content.Context
import com.example.odumonitor.R
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository

/**
 * One telemetry read for a widget, contained.
 *
 * A widget that throws shows the launcher's error layout, so nothing is allowed to
 * escape from here. The repository already reports an unreachable ODU as offline
 * state; this catch covers the rest (database, keystore, a mapping bug).
 */
internal suspend fun loadWidgetSignal(context: Context): OduSignalState = runCatching {
    OduRepository(context).fetchCurrentSignalOnce()
}.getOrElse { error ->
    OduSignalState(
        isConnected = false,
        connectionType = context.getString(R.string.repo_disconnected),
        provider = context.getString(R.string.repo_offline_provider),
        errorMessage = error.message,
        lastUpdated = System.currentTimeMillis()
    )
}
