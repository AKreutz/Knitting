package com.akreutz.knitting.web

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom

/** Whether the web server is serving; shown in the web menu and in the service notification. */
enum class WebConnection { Off, Running, Error }

/** The server's current state, shared between the service that owns the server and the screen that shows it. */
object WebStatus {
    private val state = MutableStateFlow(WebConnection.Off)
    val connection: StateFlow<WebConnection> = state

    fun set(connection: WebConnection) {
        state.value = connection
    }
}

/** Whether the user wants the counters reachable from a browser, the PIN that protects them, and the service. */
object WebSettings {
    const val PORT = 8765
    private const val PREFS = "web"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PIN = "pin"
    private const val PIN_DIGITS = 4

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        apply(context)
    }

    /** The PIN the browser must send; made up on first use and kept so a bookmarked address keeps working. */
    fun pin(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // A PIN from before the length changed no longer fits and is replaced.
        prefs.getString(KEY_PIN, null)?.takeIf { it.length == PIN_DIGITS }?.let { return it }
        return newPin(context)
    }

    /** Replaces the PIN, which locks out every browser that knew the old one. */
    fun newPin(context: Context): String {
        val random = SecureRandom()
        val pin = (1..PIN_DIGITS).joinToString("") { random.nextInt(10).toString() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PIN, pin).apply()
        return pin
    }

    /** The phone's address on the local network, or null when it is not on one. */
    fun localAddress(): String? = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress

    /** Starts or stops the service to match the setting; call from the foreground, where Android allows starting it. */
    fun apply(context: Context) {
        val intent = Intent(context, WebCounterService::class.java)
        if (isEnabled(context)) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
    }
}
