package com.akreutz.knitting.watch

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How far the link to the watch has got; shown in the watch menu and in the service notification. */
enum class WatchConnection {
    Off,
    Connecting,
    Connected,

    /** The watch is paired but out of Bluetooth range or switched off. */
    WatchOffline,
    NoWatch,
    GarminConnectMissing,
    GarminConnectOutdated,
    Error,
    ;

    /** The service cannot recover from this on its own and stops itself. */
    val isFailure get() = this == NoWatch || this == GarminConnectMissing || this == GarminConnectOutdated || this == Error
}

/** The link's current state, shared between the service that owns the link and the screen that shows it. */
object WatchStatus {
    private val state = MutableStateFlow(WatchConnection.Off)
    val connection: StateFlow<WatchConnection> = state

    fun set(connection: WatchConnection) {
        state.value = connection
    }
}

/** Whether the user wants the watch to control the counters, and the service that does it. */
object WatchSettings {
    private const val PREFS = "watch"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        apply(context)
    }

    /** Starts or stops the service to match the setting; call from the foreground, where Android allows starting it. */
    fun apply(context: Context) {
        val intent = Intent(context, WatchCounterService::class.java)
        if (isEnabled(context)) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
    }
}
