package com.akreutz.knitting.ui.projects

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.ui.theme.Spacing

/** One step on a project card: a summary line, plus type-specific tracking once the project is in progress. */
@Composable
internal fun StepRow(
    step: Step,
    inProgress: Boolean,
    onProgressChange: ((Step, Int) -> Unit)?,
    onPatternCellsChange: ((Step, String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rows = step.patternRows
    val columns = step.patternColumns
    if (step.type == StepType.Pattern && rows != null && columns != null) {
        // The grid can be painted both while planning the project and while knitting it.
        Column(modifier = modifier.fillMaxWidth()) {
            Text(text = summaryLine(step), style = MaterialTheme.typography.bodyMedium)
            PatternGrid(
                rows = rows,
                columns = columns,
                storedCells = step.patternCells,
                onCellsChange = { onPatternCellsChange?.invoke(step, it) },
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    } else if (inProgress && step.progressTarget() != null) {
        StepCounter(step, onProgressChange, modifier)
    } else {
        Text(
            text = summaryLine(step),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}

/** Plural of the unit a step's counter counts, or null for steps without a counter. */
@PluralsRes
private fun Step.unitRes(): Int? = when (type) {
    StepType.CastOn -> R.plurals.stitches_count
    StepType.Increases -> R.plurals.increases_count
    StepType.Decreases -> R.plurals.decreases_count
    StepType.Stockinette -> R.plurals.rows_count
    else -> null
}

/** How much a tap on a step's counter adds or removes. */
private fun Step.increment(): Int = if (type == StepType.CastOn) 10 else 1

/** The step's entered details that are worth showing under its name while it is being worked on. */
private fun Step.details(): String = when (type) {
    StepType.CastOn -> listOfNotNull(method, needleSize)
    StepType.Increases, StepType.Decreases -> listOfNotNull(pattern)
    else -> emptyList()
}.joinToString(" · ")

@Composable
private fun summaryLine(step: Step): String {
    val target = step.progressTarget() ?: step.targetRows
    val unit = step.unitRes() ?: R.plurals.rows_count
    val detail = if (step.type == StepType.Pattern) {
        stringResource(R.string.pattern_size, step.patternRows ?: 0, step.patternColumns ?: 0)
    } else {
        target?.let { pluralStringResource(unit, it, it) }
    }
    return listOfNotNull(step.name, stringResource(step.type.labelRes()), detail).joinToString(" · ")
}

@Composable
private fun StepCounter(
    step: Step,
    onProgressChange: ((Step, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val target = step.progressTarget() ?: return
    val unit = step.unitRes() ?: return
    val increment = step.increment()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = step.name, style = MaterialTheme.typography.titleSmall)
        val details = step.details()
        if (details.isNotEmpty()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
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
}
