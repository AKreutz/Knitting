package com.akreutz.knitting.web

import androidx.compose.ui.graphics.Color
import com.akreutz.knitting.data.ActiveProject
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.completedFraction
import com.akreutz.knitting.data.decrement
import com.akreutz.knitting.data.increment
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.data.usesShapingRounds
import com.akreutz.knitting.data.weightedProgress
import com.akreutz.knitting.ui.projects.yarnColorOrNull
import java.util.Locale

/** A tap from the browser on one step's counter. */
data class WebStep(val stepId: Long, val delta: Int)

/**
 * The JSON the browser page and the phone exchange.
 *
 * Phone to browser: `{"project": name, "description": text or null, "progress": 0..1 or null, "steps": [{"id", "name",
 * "type", "prog", "max", "inc", "dec", "current", "done"}]}`, plus
 * `"color"`, `"shade"` (`#rrggbb`, only for a color name the app knows), `"rowPattern"` and `"details"` (`{"k": method, needle,
 * shaping or description, "v": the entered text}`)
 * where the step has them, `"cols"`, `"ptype"` and `"cells"` (as stored in the step) on a pattern step with a grid,
 * `"row"` and `"rows"` on pattern steps, `"round"` and `"every"` (rounds since the last one, and the interval) on
 * increases and decreases worked every few rounds, and `"cm": true` when the counts are millimeters; `{"project": null}` when no
 * project is in progress. `inc` and `dec` are how much one tap adds or removes, as for the watch.
 *
 * Browser to phone: a form body `step=<id>&delta=1|-1`.
 */
object WebProtocol {
    fun encode(active: ActiveProject?): String {
        if (active == null) return """{"project":null}"""
        val steps = active.steps.joinToString(",") { encode(it, it.id == active.current?.id) }
        val description = active.project.description?.let(::quote) ?: "null"
        val progress = active.steps.weightedProgress()?.let { "%.4f".format(Locale.ROOT, it) } ?: "null"
        return """{"project":${quote(active.project.name)},"description":$description,"progress":$progress,"steps":[$steps]}"""
    }

    private fun encode(step: Step, current: Boolean): String {
        val pattern = step.type == StepType.Pattern
        // A tap on a pattern moves a row and on a shaping step with an interval a round, never the count itself.
        val byRow = pattern || step.usesShapingRounds()
        val fields = mutableListOf(
            """"id":${step.id}""",
            """"name":${quote(step.name)}""",
            """"type":${quote(step.type.name)}""",
            """"prog":${step.progress}""",
            """"max":${step.progressTarget() ?: 0}""",
            """"inc":${if (byRow) 1 else step.increment()}""",
            """"dec":${if (byRow) 1 else step.decrement()}""",
            """"current":$current""",
            """"done":${(step.completedFraction() ?: 0f) >= 1f}""",
        )
        if (pattern) {
            val rows = step.patternRows ?: 0
            val columns = step.patternColumns ?: 0
            fields += """"row":${step.patternRow}"""
            fields += """"rows":$rows"""
            if (rows > 0 && columns > 0) {
                fields += """"cols":$columns"""
                step.patternType?.let { fields += """"ptype":${quote(it.name)}""" }
                step.patternCells?.takeIf { it.length == rows * columns }?.let { fields += """"cells":${quote(it)}""" }
            }
        }
        if (step.usesShapingRounds()) {
            fields += """"round":${step.patternRow}"""
            fields += """"every":${step.shapingInterval}"""
        }
        if (step.trackInCm) fields += """"cm":true"""
        step.color?.let { color ->
            fields += """"color":${quote(color)}"""
            yarnColorOrNull(color)?.let { fields += """"shade":"${hex(it)}"""" }
        }
        step.rowPattern?.let { fields += """"rowPattern":${quote(it.name)}""" }
        val details = details(step).map { (key, value) -> """{"k":"$key","v":${quote(value)}}""" }
        if (details.isNotEmpty()) fields += """"details":${details.joinToString(",", "[", "]")}"""
        return fields.joinToString(",", "{", "}")
    }

    /** The texts the user entered for the step's type, with a key the page labels in its own language. */
    private fun details(step: Step): List<Pair<String, String>> = when (step.type) {
        StepType.CastOn -> listOfNotNull(step.method?.let { "method" to it }, step.needleSize?.let { "needle" to it })
        StepType.Increases, StepType.Decreases -> listOfNotNull(step.pattern?.let { "shaping" to it })
        StepType.Special -> listOfNotNull(step.description?.let { "description" to it })
        else -> emptyList()
    }

    fun parseStep(body: String): WebStep? {
        val fields = body.split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        val stepId = fields["step"]?.toLongOrNull() ?: return null
        val delta = when (fields["delta"]) {
            "1" -> 1
            "-1" -> -1
            else -> return null
        }
        return WebStep(stepId, delta)
    }

    /** The color as `#rrggbb`. */
    private fun hex(color: Color): String =
        "#%02x%02x%02x".format(Locale.ROOT, (color.red * 255 + 0.5f).toInt(), (color.green * 255 + 0.5f).toInt(), (color.blue * 255 + 0.5f).toInt())

    private fun quote(text: String): String = buildString {
        append('"')
        for (c in text) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c < ' ' -> append("\\u%04x".format(c.code))
                // Keeps the JSON safe to embed in HTML even though the page only ever reads it with fetch.
                c == '<' -> append("\\u003c")
                else -> append(c)
            }
        }
        append('"')
    }
}
