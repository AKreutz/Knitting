package com.akreutz.knitting.watch

import kotlinx.coroutines.flow.Flow

/** A connection to the watch app; the Garmin Connect IQ implementation is the only part that needs Garmin's SDK. */
interface WatchLink {
    /** Raw messages from the watch app, in the form [WatchProtocol.parseCommand] accepts. */
    val incoming: Flow<Any?>

    /** Sends [message] to the watch app; throws if the watch is unreachable. */
    suspend fun send(message: Map<String, Any>)
}
