package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.ui.theme.Spacing
import java.util.concurrent.TimeUnit

/**
 * The card for a project being knitted: overall progress up top, the step to work on right now
 * front and center with its counter or pattern grid, and the remaining steps tucked away below.
 */
@Composable
internal fun InProgressProjectCard(
    project: Project,
    steps: List<Step>,
    modifier: Modifier = Modifier,
    onResetClick: (() -> Unit)? = null,
    onFinishClick: (() -> Unit)? = null,
    onStepProgressChange: ((Step, Int) -> Unit)? = null,
    onPatternCellsChange: ((Step, String) -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val countable = steps.filter { it.progressTarget() != null }
    val doneCount = countable.count { it.isDone() }
    val current = steps.currentStep()
    // With no current step everything is done, so every step counts as previous.
    val currentIndex = steps.indexOfFirst { it.id == current?.id }.takeIf { it >= 0 } ?: steps.size
    val previous = steps.take(currentIndex)
    val next = steps.drop(currentIndex + 1)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Same status-colored strip as the basic project card.
            Box(
                modifier = Modifier
                    .width(Spacing.accentStrip)
                    .fillMaxHeight()
                    .background(project.status.accentColor()),
            )
            Column(modifier = Modifier.weight(1f).padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = project.name, style = MaterialTheme.typography.titleLarge)
                        dayCount(project)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                project.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                if (countable.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { doneCount / countable.size.toFloat() },
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                    Text(
                        text = stringResource(R.string.steps_done_summary, doneCount, countable.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                CollapsibleSteps(
                    titleRes = R.string.previous_steps,
                    steps = previous,
                    stateKey = "${project.id}-previous",
                    onProgressChange = onStepProgressChange,
                    onPatternCellsChange = onPatternCellsChange,
                )
                if (steps.isNotEmpty()) {
                    CurrentStepPanel(
                        step = current,
                        onProgressChange = onStepProgressChange,
                        onPatternCellsChange = onPatternCellsChange,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }
                CollapsibleSteps(
                    titleRes = R.string.next_steps,
                    steps = next,
                    stateKey = "${project.id}-next",
                    onProgressChange = onStepProgressChange,
                    onPatternCellsChange = onPatternCellsChange,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(onClick = { onResetClick?.invoke() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reset))
                    }
                    Button(
                        onClick = { onFinishClick?.invoke() },
                        enabled = doneCount == countable.size,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.finish))
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStepPanel(
    step: Step?,
    onProgressChange: ((Step, Int) -> Unit)?,
    onPatternCellsChange: ((Step, String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (step == null) {
                Text(
                    text = stringResource(R.string.all_steps_done),
                    style = MaterialTheme.typography.titleMedium,
                )
                return@Column
            }
            Text(
                text = stringResource(R.string.current_step).uppercase(),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = "${step.name} · ${stringResource(step.type.labelRes())}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            val details = step.details()
            if (details.isNotEmpty()) {
                Text(text = details, style = MaterialTheme.typography.bodySmall)
            }
            val target = step.progressTarget()
            if (target != null) {
                LinearProgressIndicator(
                    progress = { (step.progress / target.toFloat()).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.onSecondary,
                    trackColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
                StepCounter(step, onProgressChange, modifier = Modifier.padding(top = Spacing.xs))
            }
            val rows = step.patternRows
            val columns = step.patternColumns
            if (step.type == StepType.Pattern && rows != null && columns != null) {
                // The panel's content color is light on its colored background; the grid has its own
                // light cells, so its symbols keep the card's regular text color.
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    PatternGrid(
                        rows = rows,
                        columns = columns,
                        patternType = step.patternType,
                        storedCells = step.patternCells,
                        onCellsChange = { onPatternCellsChange?.invoke(step, it) },
                        editable = false,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSteps(
    @StringRes titleRes: Int,
    steps: List<Step>,
    stateKey: String,
    onProgressChange: ((Step, Int) -> Unit)?,
    onPatternCellsChange: ((Step, String) -> Unit)?,
) {
    var expanded by rememberSaveable(stateKey) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(top = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes, steps.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.collapse_step else R.string.expand_step),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column {
            steps.forEach { step ->
                StepRow(
                    step = step,
                    inProgress = true,
                    onProgressChange = onProgressChange,
                    onPatternCellsChange = onPatternCellsChange,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}

private fun Step.isDone(): Boolean = progressTarget()?.let { progress >= it } ?: false

/**
 * The step to work on next: the first counted step that is not finished, else the first pattern
 * step without a repeat count (it is reference material and never blocks progress), else null
 * when all is done.
 */
private fun List<Step>.currentStep(): Step? =
    firstOrNull { it.progressTarget() != null && !it.isDone() }
        ?: firstOrNull { it.progressTarget() == null && it.type == StepType.Pattern }

@Composable
private fun dayCount(project: Project): String? {
    val startedAt = project.startedAt ?: return null
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startedAt).toInt() + 1
    return stringResource(R.string.day_count, days)
}
