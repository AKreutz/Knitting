package com.akreutz.knitting.ui.projects

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.MM_PER_CM
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import com.akreutz.knitting.ui.theme.Spacing
import java.text.NumberFormat

/**
 * One step on a project card. Collapsed, it shows the type icon, name, type and size or target;
 * tapping it expands the step's details and its type-specific tracking, which is a counter once
 * the project is in progress.
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
    /** Where this step sits in a tree of steps: a branch leads to it from a line along the left edge. */
    branch: StepBranch? = null,
) {
    var expanded by rememberSaveable(step.id) { mutableStateOf(false) }
    val rows = step.patternRows
    val columns = step.patternColumns
    val hasGrid = step.type == StepType.Pattern && rows != null && columns != null
    val hasCounter = inProgress && step.progressTarget() != null
    val shade = step.color?.let { yarnColorOrNull(it) }
    val details = step.targetDetails() + step.detailItems()
    val description = details.firstOrNull { it.labelRes == null }
    val labeledDetails = details.filter { it.labelRes != null }
    val hasContent = hasGrid || hasCounter || details.isNotEmpty()

    // The branch meets the header row at its middle, so the header's height places it.
    var headerHeight by remember { mutableIntStateOf(0) }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val treeModifier = if (branch == null) {
        Modifier
    } else {
        Modifier
            .drawBehind {
                val x = TREE_LINE_X.toPx()
                val branchY = Spacing.sm.toPx() + headerHeight / 2f
                val stroke = TREE_LINE_WIDTH.toPx()
                // The trunk comes down from the row above and carries on to the next one, unless this is the last.
                drawLine(lineColor, Offset(x, 0f), Offset(x, if (branch.isLast) branchY else size.height), stroke)
                drawLine(lineColor, Offset(x, branchY), Offset(TREE_GUTTER.toPx() - TREE_BRANCH_GAP.toPx(), branchY), stroke)
            }
            .padding(start = TREE_GUTTER, top = Spacing.sm, bottom = Spacing.sm)
    }
    Column(modifier = modifier.fillMaxWidth().then(treeModifier)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeight = it.height }
                .clickable(enabled = hasContent) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DuotoneIcon(step.type.icon(), modifier = Modifier.padding(end = Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                // A known color is shown as a dot beside the text; any other is written out after the type.
                Text(
                    text = listOfNotNull(stringResource(step.type.labelRes()), step.color.takeIf { shade == null })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (step.color != null && shade != null) {
                YarnBall(shade, description = step.color, modifier = Modifier.padding(horizontal = Spacing.sm))
            }
            if (hasContent) {
                ExpandChevron(expanded)
            }
        }
        AnimatedVisibility(visible = expanded && hasContent) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                description?.let { Text(text = it.value, style = MaterialTheme.typography.bodyMedium) }
                if (labeledDetails.isNotEmpty()) {
                    DetailChips(labeledDetails)
                }
                if (hasCounter) {
                    StepCounter(step, onProgressChange)
                }
                if (hasGrid) {
                    // Framed so the grid reads as its own area; it is painted while planning, and in
                    // progress only editable where the caller allows it.
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
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
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                }
            }
        }
    }
}

/** How a step hangs off the tree's trunk: the last one ends it, like "└" instead of "├". */
internal enum class StepBranch(val isLast: Boolean) {
    Inner(isLast = false),
    Last(isLast = true),
}

/** The branch for the step at [index] of [count]. */
internal fun stepBranchAt(index: Int, count: Int): StepBranch =
    if (index == count - 1) StepBranch.Last else StepBranch.Inner

/**
 * Room left of the content for the trunk and the branches. The trunk's outer edge lines up with the
 * drawn edge of an icon, which sits [ICON_GLYPH_INSET] inside the icon's box, not with the row's edge.
 */
private val TREE_GUTTER = 20.dp
private val TREE_BRANCH_GAP = 6.dp
private val TREE_LINE_WIDTH = 2.dp
private val ICON_GLYPH_INSET = 2.dp
private val TREE_LINE_X = ICON_GLYPH_INSET + TREE_LINE_WIDTH / 2

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

/** One entered detail of a step; a null [labelRes] marks free text that has no label, like a description. */
internal data class StepDetail(@StringRes val labelRes: Int?, val value: String)

/** The details the user entered for the step's type. The color is not among them: it is shown with the step's type. */
internal fun Step.detailItems(): List<StepDetail> = when (type) {
    StepType.CastOn -> listOfNotNull(
        method?.let { StepDetail(R.string.detail_method, it) },
        needleSize?.let { StepDetail(R.string.detail_needle_size, it) },
    )
    StepType.Increases, StepType.Decreases -> listOfNotNull(pattern?.let { StepDetail(R.string.detail_shaping_pattern, it) })
    StepType.Special -> listOfNotNull(description?.let { StepDetail(null, it) })
    else -> emptyList()
}

/** The entered details and, unless [withColor] is off, the color in one line, for places with no room to label them. */
internal fun Step.details(withColor: Boolean = true): String =
    (detailItems().map { it.value } + listOfNotNull(color.takeIf { withColor })).joinToString(" · ")

/** A step's labeled details as small chips that wrap onto further lines when they do not fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailChips(details: List<StepDetail>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        details.forEach { detail ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                    detail.labelRes?.let {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = detail.value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** The stitch pattern or pattern type and the target of a step, as details to show once it is expanded. */
@Composable
private fun Step.targetDetails(): List<StepDetail> {
    val target = progressTarget()
    val kind = if (type == StepType.Pattern) {
        patternType?.let { StepDetail(R.string.step_pattern_type, stringResource(it.labelRes())) }
    } else {
        rowPattern?.let { StepDetail(R.string.step_row_pattern, stringResource(it.labelRes())) }
    }
    // A step that is only checked off has no amount to show.
    val amount = (target ?: targetRows)?.takeIf { unitRes() != null }
        ?.let { StepDetail(if (trackInCm) R.string.detail_target_length else R.string.detail_target_count, amountText(it)) }
    return listOfNotNull(kind, amount)
}

/** The label on a counter's buttons, like "+1", "−1" or "Mark done", in the font of the app's titles. */
@Composable
internal fun CounterButtonLabel(text: String) {
    Text(text = text, fontFamily = MaterialTheme.typography.titleLarge.fontFamily)
}

/** A changing count slides up into place while the old value fades out. */
internal fun <S> AnimatedContentTransitionScope<S>.countTransition(): ContentTransform =
    (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith fadeOut(tween(100))

@Composable
internal fun StepCounter(
    step: Step,
    onProgressChange: ((Step, Int) -> Unit)?,
    modifier: Modifier = Modifier,
    buttonColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
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
                FilledTonalButton(onClick = { change(if (done) 0 else target) }, colors = buttonColors) {
                    CounterButtonLabel(stringResource(if (done) R.string.undo else R.string.mark_done))
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
                colors = buttonColors,
            ) {
                CounterButtonLabel(stringResource(R.string.remove_count, label(decrement)))
            }
            FilledTonalButton(
                onClick = { change(step.progress + increment) },
                enabled = step.progress < target,
                colors = buttonColors,
                modifier = Modifier.padding(start = Spacing.sm),
            ) {
                CounterButtonLabel(stringResource(R.string.add_count, label(increment)))
            }
        }
    }
}
