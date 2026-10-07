package com.akreutz.knitting.ui.projects

import androidx.annotation.PluralsRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.MM_PER_CM
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.ui.theme.Spacing
import java.text.NumberFormat

/**
 * One step on a project card. Collapsed, it is a single line with the name and type; tapping it
 * expands the step's size, details and its type-specific tracking, which is a counter once the project is in progress.
 */
@Composable
internal fun StepRow(
    step: Step,
    inProgress: Boolean,
    onProgressChange: ((Step, Int) -> Unit)?,
    onPatternCellsChange: ((Step, String) -> Unit)?,
    modifier: Modifier = Modifier,
    gridEditable: Boolean = !inProgress,
    /** Other pattern steps whose grid can be copied into this one; only offered while the grid is editable. */
    copyGridSources: List<Step> = emptyList(),
    onCopyGrid: ((target: Step, source: Step) -> Unit)? = null,
) {
    var expanded by rememberSaveable(step.id) { mutableStateOf(false) }
    val rows = step.patternRows
    val columns = step.patternColumns
    val hasGrid = step.type == StepType.Pattern && rows != null && columns != null
    val hasCounter = inProgress && step.progressTarget() != null
    val summary = summaryText(step)
    val details = step.details()
    val hasContent = hasGrid || hasCounter || details.isNotEmpty() || summary != null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasContent) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${step.name} · ${stringResource(step.type.labelRes())}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (hasContent) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.collapse_step else R.string.expand_step),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded && hasContent) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val detailLine = listOfNotNull(summary, details.ifEmpty { null }).joinToString(" · ")
                if (detailLine.isNotEmpty()) {
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasCounter) {
                    StepCounter(step, onProgressChange, modifier = Modifier.padding(top = Spacing.xs))
                }
                if (hasGrid) {
                    // The grid is painted while planning; in progress it is only editable where the caller allows it.
                    PatternGrid(
                        rows = rows,
                        columns = columns,
                        patternType = step.patternType,
                        storedCells = step.patternCells,
                        onCellsChange = { onPatternCellsChange?.invoke(step, it) },
                        editable = gridEditable,
                        editActions = if (onCopyGrid != null && copyGridSources.isNotEmpty()) {
                            {
                                CopyGridButton(
                                    sources = copyGridSources,
                                    onSelect = { onCopyGrid(step, it) },
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
            }
        }
    }
}

/** Whether a step is a pattern step with a grid size, i.e. something to show, paint or copy. */
internal fun Step.hasGrid(): Boolean = type == StepType.Pattern && patternRows != null && patternColumns != null

/** A button that opens a menu of the steps whose grid can be copied into the current one. */
@Composable
private fun CopyGridButton(
    sources: List<Step>,
    onSelect: (Step) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.copy_grid_from), modifier = Modifier.padding(start = Spacing.xs))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            sources.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${source.name} · ${stringResource(R.string.pattern_size, source.patternRows ?: 0, source.patternColumns ?: 0)}",
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onSelect(source)
                    },
                )
            }
        }
    }
}

/** Plural of the unit a step's counter counts, or null for steps without a counter. */
@PluralsRes
internal fun Step.unitRes(): Int? = when (type) {
    StepType.CastOn -> R.plurals.stitches_count
    StepType.Increases -> R.plurals.increases_count
    StepType.Decreases -> R.plurals.decreases_count
    StepType.PlainRows -> R.plurals.rows_count
    StepType.Pattern -> R.plurals.pattern_repeats_count
    StepType.Special -> null
}

/** [count] with its unit, e.g. "12 rows" or "2.5 cm"; [count] is in millimeters for centimeter steps. */
@Composable
private fun Step.amountText(count: Int): String {
    val unit = unitRes() ?: return count.toString()
    return if (trackInCm) {
        stringResource(R.string.cm_value, formatCm(count))
    } else {
        pluralStringResource(unit, count, count)
    }
}

/** Millimeters as centimeters in the user's locale, with a decimal only when there is one. */
private fun formatCm(mm: Int): String =
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(mm / MM_PER_CM.toDouble())

/** The size of a regular tap on a step's counter: a whole centimeter, 10 cast-on stitches or a single count. */
private fun Step.chunk(): Int = when {
    trackInCm -> MM_PER_CM
    type == StepType.CastOn -> 10
    else -> 1
}

/** How much a tap on a step's counter adds; less than a full chunk when that is all that is left. */
internal fun Step.increment(): Int {
    val remaining = (progressTarget() ?: 0) - progress
    return if (remaining in 1 until chunk()) remaining else chunk()
}

/** How much a tap on a step's counter removes; a partial chunk (e.g. 2.5 cm or 25 stitches) goes first. */
internal fun Step.decrement(): Int {
    val partial = progress % chunk()
    return if (partial > 0) partial else chunk()
}

/** The step's entered details that are worth showing once it is expanded. */
internal fun Step.details(): String = (when (type) {
    StepType.CastOn -> listOfNotNull(method, needleSize)
    StepType.Increases, StepType.Decreases -> listOfNotNull(pattern)
    StepType.Special -> listOfNotNull(description)
    else -> emptyList()
} + listOfNotNull(color)).joinToString(" · ")

/** The size, stitch pattern or target of a step, shown once it is expanded; null when there is none. */
@Composable
private fun summaryText(step: Step): String? {
    val target = step.progressTarget()
    return if (step.type == StepType.Pattern) {
        listOfNotNull(
            step.patternType?.let { stringResource(it.labelRes()) },
            stringResource(R.string.pattern_size, step.patternRows ?: 0, step.patternColumns ?: 0),
            target?.let { step.amountText(it) },
        ).joinToString(" · ")
    } else {
        listOfNotNull(
            step.rowPattern?.let { stringResource(it.labelRes()) },
            (target ?: step.targetRows)?.let { step.amountText(it) },
        ).joinToString(" · ").ifEmpty { null }
    }
}

/** A changing count slides up into place while the old value fades out. */
internal fun <S> AnimatedContentTransitionScope<S>.countTransition(): ContentTransform =
    (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith fadeOut(tween(100))

@Composable
internal fun StepCounter(
    step: Step,
    onProgressChange: ((Step, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val target = step.progressTarget() ?: return
    val haptics = LocalHapticFeedback.current
    // A light tick for every count, a firmer confirmation when a count reaches its target.
    fun change(newProgress: Int) {
        haptics.performHapticFeedback(
            if (newProgress >= target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick,
        )
        onProgressChange?.invoke(step, newProgress)
    }
    if (step.type == StepType.Special) {
        val done = step.progress >= target
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = done,
                transitionSpec = { countTransition() },
                label = "step done",
                modifier = Modifier.weight(1f),
            ) { isDone ->
                Text(
                    text = stringResource(if (isDone) R.string.step_done else R.string.step_not_done),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (onProgressChange != null) {
                FilledTonalButton(onClick = { change(if (done) 0 else target) }) {
                    Text(stringResource(if (done) R.string.undo else R.string.mark_done))
                }
            }
        }
        return
    }
    if (step.unitRes() == null) return
    val increment = step.increment()
    val decrement = step.decrement()
    // Centimeter steps count in millimeters but their buttons are labeled in centimeters.
    fun label(amount: Int) = if (step.trackInCm) formatCm(amount) else amount.toString()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = stringResource(
                R.string.step_progress,
                if (step.trackInCm) formatCm(step.progress) else step.progress.toString(),
                step.amountText(target),
            ),
            transitionSpec = { countTransition() },
            label = "step progress",
            modifier = Modifier.weight(1f),
        ) { text ->
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
        if (onProgressChange != null) {
            FilledTonalButton(
                onClick = { change(step.progress - decrement) },
                enabled = step.progress > 0,
            ) {
                Text(stringResource(R.string.remove_count, label(decrement)))
            }
            FilledTonalButton(
                onClick = { change(step.progress + increment) },
                enabled = step.progress < target,
                modifier = Modifier.padding(start = Spacing.sm),
            ) {
                Text(stringResource(R.string.add_count, label(increment)))
            }
        }
    }
}
