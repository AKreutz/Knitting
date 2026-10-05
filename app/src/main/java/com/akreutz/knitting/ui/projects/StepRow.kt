package com.akreutz.knitting.ui.projects

import androidx.annotation.PluralsRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.ui.theme.Spacing

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
                if (hasGrid && rows != null && columns != null) {
                    // The grid is painted while planning the project; once it is in progress it is only shown.
                    PatternGrid(
                        rows = rows,
                        columns = columns,
                        patternType = step.patternType,
                        storedCells = step.patternCells,
                        onCellsChange = { onPatternCellsChange?.invoke(step, it) },
                        editable = !inProgress,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
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

/** How much a tap on a step's counter adds or removes. */
internal fun Step.increment(): Int = if (type == StepType.CastOn) 10 else 1

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
    val unit = step.unitRes()
    return if (step.type == StepType.Pattern) {
        listOfNotNull(
            step.patternType?.let { stringResource(it.labelRes()) },
            stringResource(R.string.pattern_size, step.patternRows ?: 0, step.patternColumns ?: 0),
            step.patternRepeats?.let { pluralStringResource(R.plurals.pattern_repeats_count, it, it) },
        ).joinToString(" · ")
    } else {
        listOfNotNull(
            step.rowPattern?.let { stringResource(it.labelRes()) },
            (target ?: step.targetRows)?.let { pluralStringResource(unit ?: R.plurals.rows_count, it, it) },
        ).joinToString(" · ").ifEmpty { null }
    }
}

@Composable
internal fun StepCounter(
    step: Step,
    onProgressChange: ((Step, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val target = step.progressTarget() ?: return
    if (step.type == StepType.Special) {
        val done = step.progress >= target
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (done) R.string.step_done else R.string.step_not_done),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (onProgressChange != null) {
                FilledTonalButton(onClick = { onProgressChange(step, if (done) 0 else target) }) {
                    Text(stringResource(if (done) R.string.undo else R.string.mark_done))
                }
            }
        }
        return
    }
    val unit = step.unitRes() ?: return
    val increment = step.increment()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.step_progress,
                step.progress,
                pluralStringResource(unit, target, target),
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (onProgressChange != null) {
            FilledTonalButton(
                onClick = { onProgressChange(step, step.progress - increment) },
                enabled = step.progress > 0,
            ) {
                Text(stringResource(R.string.remove_count, increment))
            }
            FilledTonalButton(
                onClick = { onProgressChange(step, step.progress + increment) },
                enabled = step.progress < target,
                modifier = Modifier.padding(start = Spacing.sm),
            ) {
                Text(stringResource(R.string.add_count, increment))
            }
        }
    }
}
