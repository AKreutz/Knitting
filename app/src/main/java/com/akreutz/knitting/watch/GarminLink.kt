package com.akreutz.knitting.watch

import android.content.Context
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.ConnectIQ.IQConnectType
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus
import com.garmin.android.connectiq.ConnectIQ.IQSdkErrorStatus
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The id of the watch app; the watch app's manifest must use the same one. */
const val WATCH_APP_ID = "e142bb0b6df0473083015a609fa1a268"

private const val SEND_TIMEOUT_MS = 10_000L

/**
 * Talks to the watch app through the Garmin Connect app on the phone. Collecting [incoming] opens the connection and
 * keeps it open until the collector is cancelled; [WatchStatus] reports how it is going.
 */
class GarminLink(context: Context) : WatchLink {
    private class Session(val connectIq: ConnectIQ, val device: IQDevice)

    private val context = context.applicationContext
    private val app = IQApp(WATCH_APP_ID)

    @Volatile
    private var session: Session? = null

    override val incoming: Flow<Any?> = callbackFlow {
        val connectIq = ConnectIQ.getInstance(context, IQConnectType.WIRELESS)
        WatchStatus.set(WatchConnection.Connecting)
        // No automatic dialogs: a service has no window to show them in, so problems go to [WatchStatus] instead.
        connectIq.initialize(
            context,
            false,
            object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    try {
                        val device = findDevice(connectIq)
                        if (device == null) {
                            WatchStatus.set(WatchConnection.NoWatch)
                            close()
                            return
                        }
                        connectIq.registerForDeviceEvents(device) { _, status -> WatchStatus.set(status.toConnection()) }
                        connectIq.registerForAppEvents(device, app) { _, _, message, status ->
                            if (status == IQMessageStatus.SUCCESS) trySend(message)
                        }
                        session = Session(connectIq, device)
                        WatchStatus.set(connectIq.getDeviceStatus(device).toConnection())
                    } catch (e: Exception) {
                        WatchStatus.set(WatchConnection.Error)
                        close()
                    }
                }

                override fun onInitializeError(status: IQSdkErrorStatus) {
                    WatchStatus.set(
                        when (status) {
                            IQSdkErrorStatus.GCM_NOT_INSTALLED -> WatchConnection.GarminConnectMissing
                            IQSdkErrorStatus.GCM_UPGRADE_NEEDED -> WatchConnection.GarminConnectOutdated
                            else -> WatchConnection.Error
                        },
                    )
                    close()
                }

                override fun onSdkShutDown() {
                    session = null
                }
            },
        )
        awaitClose {
            session = null
            try {
                connectIq.unregisterAllForEvents()
                connectIq.shutdown(context)
            } catch (e: Exception) {
                // Never initialized or already shut down; nothing is left to release.
            }
        }
    }

    override suspend fun send(message: Map<String, Any>) {
        val session = session ?: throw IOException("Garmin Connect is not ready")
        val result = withTimeoutOrNull(SEND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                try {
                    session.connectIq.sendMessage(session.device, app, message) { _, _, status ->
                        if (status == IQMessageStatus.SUCCESS) continuation.resume(Unit)
                        else continuation.resumeWithException(IOException("Message to the watch failed: $status"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
        if (result == null) throw IOException("The watch did not confirm the message")
    }

    /** The paired watch, preferring one that is connected right now. */
    private fun findDevice(connectIq: ConnectIQ): IQDevice? {
        val devices = connectIq.knownDevices
        return devices.firstOrNull { connectIq.getDeviceStatus(it) == IQDeviceStatus.CONNECTED } ?: devices.firstOrNull()
    }

    private fun IQDeviceStatus.toConnection() = when (this) {
        IQDeviceStatus.CONNECTED -> WatchConnection.Connected
        IQDeviceStatus.NOT_CONNECTED -> WatchConnection.WatchOffline
        else -> WatchConnection.NoWatch
    }
}
