package com.akreutz.knitting.watch

import com.akreutz.knitting.data.CurrentCounter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Keeps the watch in step with the counters: pushes every change to it and applies the taps it sends. */
class WatchCounterController(
    private val current: Flow<CurrentCounter?>,
    private val step: suspend (delta: Int) -> Unit,
    private val link: WatchLink,
) {
    /** Runs until cancelled. */
    suspend fun run() = coroutineScope {
        launch { current.collect { push(it) } }
        launch {
            link.incoming.collect { message ->
                when (val command = WatchProtocol.parseCommand(message)) {
                    // The change comes back through [current], which pushes the new state.
                    is WatchCommand.Step -> step(command.delta)
                    WatchCommand.Sync -> push(current.first())
                    null -> Unit
                }
            }
        }
    }

    private suspend fun push(counter: CurrentCounter?) {
        try {
            link.send(WatchProtocol.encode(counter))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The watch is out of reach; it asks for the state again with a sync once it is back.
        }
    }
}
