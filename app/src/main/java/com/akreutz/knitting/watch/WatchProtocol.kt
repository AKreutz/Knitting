package com.akreutz.knitting.watch

import com.akreutz.knitting.data.CurrentCounter
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget

/** What the watch asks of the phone. The phone always acts on its own current step, never on one the watch names. */
sealed interface WatchCommand {
    /** One tap forward ([delta] = 1) or back (-1) on the current step. */
    data class Step(val delta: Int) : WatchCommand

    /** Asks for the current state, e.g. when the watch app starts. */
    data object Sync : WatchCommand
}

/**
 * The messages exchanged with the watch app: small maps of strings and ints, as Connect IQ limits message size.
 *
 * Watch to phone: `{"cmd": "inc" | "dec" | "sync"}`.
 * Phone to watch: `{"p": project, "s": step, "t": step type, "prog": n, "max": n}`, plus `"row"` and `"rows"` for
 * pattern steps and `"cm": 1` when the counts are millimeters; `{"idle": 1}` when there is nothing to count.
 */
object WatchProtocol {
    private const val MAX_NAME_LENGTH = 40

    /** Accepts the payload as the Connect IQ SDK delivers it: a list holding the map, or the map itself. */
    fun parseCommand(message: Any?): WatchCommand? {
        val map = (if (message is List<*>) message.firstOrNull() else message) as? Map<*, *> ?: return null
        return when (map["cmd"]) {
            "inc" -> WatchCommand.Step(1)
            "dec" -> WatchCommand.Step(-1)
            "sync" -> WatchCommand.Sync
            else -> null
        }
    }

    fun encode(current: CurrentCounter?): Map<String, Any> {
        val step = current?.step ?: return mapOf("idle" to 1)
        val message = mutableMapOf<String, Any>(
            "p" to current.project.name.take(MAX_NAME_LENGTH),
            "s" to step.name.take(MAX_NAME_LENGTH),
            "t" to step.type.name,
            "prog" to step.progress,
            "max" to (step.progressTarget() ?: 0),
        )
        if (step.type == StepType.Pattern) {
            message["row"] = step.patternRow
            message["rows"] = step.patternRows ?: 0
        }
        if (step.trackInCm) message["cm"] = 1
        return message
    }
}
