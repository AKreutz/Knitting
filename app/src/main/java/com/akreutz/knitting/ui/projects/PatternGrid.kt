package com.akreutz.knitting.ui.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import com.akreutz.knitting.R
import com.akreutz.knitting.ui.theme.Spacing

/** Colors a pattern cell can be painted with. Index 0 is empty; the stored cell string holds indices. */
private val PatternPalette = listOf(
    null,
    Color(0xFFB5493B),
    Color(0xFFE0A526),
    Color(0xFF6E8B5B),
    Color(0xFF3F5A7A),
)

/** Selected-tool value for the pan tool; non-negative values are palette indices. */
private const val PAN_TOOL = -1

/** Cells are drawn between these sizes; grids too large to fit at the minimum scroll by panning. */
private val MinCellSize = 28.dp
private val MaxCellSize = 40.dp

/** Grid line weights, ordered lightest to heaviest. */
private enum class LineWeight(val width: Dp) {
    Thin(0.5.dp),
    Fifth(1.5.dp),
    Tenth(3.dp),
    ;

    companion object {
        /**
         * The weight of the line at [boundary], one of the `count + 1` lines across an axis of
         * [count] cells. Boundaries are counted from the far end (bottom or right), matching the
         * axis numbers; the outer edges are always the heaviest, whatever the grid size.
         */
        fun of(boundary: Int, count: Int): LineWeight {
            if (boundary == 0 || boundary == count) return Tenth
            return when ((count - boundary) % 10) {
                0 -> Tenth
                5 -> Fifth
                else -> Thin
            }
        }
    }
}

/** How far a line at [boundary] moves inward so an outer edge isn't half-clipped by the viewport. */
private fun edgeInset(boundary: Int, count: Int, strokeWidth: Float): Float = when (boundary) {
    0 -> strokeWidth / 2f
    count -> -strokeWidth / 2f
    else -> 0f
}

/** Space reserved left of and below the viewport for the row and column numbers. */
private val AxisGutter = 24.dp
private val AxisLabelHeight = 16.dp
private val AxisPadding = 4.dp
private val AxisLabelOverhang = 12.dp

/** Indices of the cells along one axis that overlap the viewport, partially visible ones included. */
private fun visibleRange(offsetPx: Float, extentPx: Float, cellPx: Float, count: Int): IntRange =
    (-offsetPx / cellPx).toInt().coerceAtLeast(0)..((-offsetPx + extentPx) / cellPx).toInt().coerceAtMost(count - 1)

/**
 * How many cells apart axis numbers go so they don't overlap: every cell when there's room,
 * otherwise the smallest of 2, 5, 10, 20 or 50 that fits.
 */
private fun labelStep(cellPx: Float, labelExtentPx: Float): Int {
    val needed = ceil(labelExtentPx / cellPx).toInt()
    return if (needed <= 1) 1 else listOf(2, 5, 10, 20, 50).firstOrNull { it >= needed } ?: needed
}

private const val MinZoom = 0.1f
private const val MaxZoom = 4f

/** The grid's viewport never grows taller than this, so tall grids pan instead of filling the screen. */
private val MaxViewportHeight = 360.dp

/** A rows × columns grid the user paints by tapping or dragging, with a color palette underneath. */
@Composable
internal fun PatternGrid(
    rows: Int,
    columns: Int,
    storedCells: String?,
    onCellsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = rows * columns
    // Painting edits this local copy so drags stay smooth; it is saved when the gesture ends.
    var cells by remember(storedCells, size) {
        mutableStateOf(storedCells?.takeIf { it.length == size } ?: "0".repeat(size))
    }
    var selectedTool by remember { mutableIntStateOf(PAN_TOOL) }
    var availableWidthPx by remember { mutableIntStateOf(0) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    // Pinch zoom, as a multiplier on the cell size the grid would have at rest.
    var zoom by remember { mutableFloatStateOf(1f) }
    // A saved grid is a static picture, shown whole: the tools are disabled and swipes scroll the app.
    // Grids start out saved; the user presses Edit to paint.
    var saved by remember { mutableStateOf(true) }

    val density = LocalDensity.current
    val gutterPx = with(density) { AxisGutter.toPx() }
    val usableWidthPx = (availableWidthPx - gutterPx).coerceAtLeast(0f)
    val baseCellPx = (usableWidthPx / columns)
        .coerceIn(with(density) { MinCellSize.toPx() }, with(density) { MaxCellSize.toPx() })
    // While editing, the viewport is sized from the unzoomed grid, so pinching never resizes the layout.
    val editWidth = minOf(baseCellPx * columns, usableWidthPx)
    val editHeight = minOf(baseCellPx * rows, with(density) { MaxViewportHeight.toPx() })
    // The zoom at which the whole grid fits in the editing viewport. A saved grid always uses it,
    // which is computed rather than stored so it is right from the first frame and follows resizes.
    val fitZoom = minOf(editWidth / (baseCellPx * columns), editHeight / (baseCellPx * rows), 1f)
        .coerceAtLeast(MinZoom)
    val cellPx = baseCellPx * if (saved) fitZoom else zoom
    // A saved grid has no use for spare space, so its viewport hugs it and the axis numbers
    // stay right next to its edges.
    val viewWidth = if (saved) cellPx * columns else editWidth
    val viewHeight = if (saved) cellPx * rows else editHeight

    // Pan is stored unclamped and clamped on use, so it stays valid when sizes change.
    // A grid smaller than the viewport can sit anywhere inside it.
    fun clampedPan(offset: Offset, cell: Float = cellPx): Offset {
        val slackX = viewWidth - cell * columns
        val slackY = viewHeight - cell * rows
        return Offset(
            offset.x.coerceIn(minOf(0f, slackX), maxOf(0f, slackX)),
            offset.y.coerceIn(minOf(0f, slackY), maxOf(0f, slackY)),
        )
    }

    // Where the grid currently sits: filling the viewport while saved, otherwise wherever the user panned it.
    fun currentPan() = if (saved) Offset.Zero else clampedPan(pan)

    // Starts editing from the view the saved grid was showing, centered in the larger editing viewport.
    fun startEditing() {
        zoom = fitZoom
        pan = Offset((editWidth - cellPx * columns) / 2f, (editHeight - cellPx * rows) / 2f)
        saved = false
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        // The width is measured with onSizeChanged rather than BoxWithConstraints, because the
        // project card asks its content for intrinsic sizes, which subcomposition can't answer.
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { availableWidthPx = it.width }) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val emptyColor = MaterialTheme.colorScheme.surface
            val markerColor = MaterialTheme.colorScheme.onSurfaceVariant
            val textMeasurer = rememberTextMeasurer()
            val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = markerColor)

            // Zooms around the pinch centroid so the cell under the fingers stays put.
            val transform by rememberUpdatedState { centroid: Offset, panChange: Offset, zoomChange: Float ->
                val newZoom = (zoom * zoomChange).coerceIn(MinZoom, MaxZoom)
                val newCellPx = baseCellPx * newZoom
                val gridPoint = (centroid - currentPan()) / cellPx
                pan = clampedPan(centroid - gridPoint * newCellPx + panChange, newCellPx)
                zoom = newZoom
            }

            fun paint(position: Offset) {
                val inGrid = position - currentPan()
                val column = (inGrid.x / cellPx).toInt()
                val row = (inGrid.y / cellPx).toInt()
                if (inGrid.x < 0f || inGrid.y < 0f || column !in 0 until columns || row !in 0 until rows) return
                val index = row * columns + column
                val painted = selectedTool.digitToChar()
                if (cells[index] != painted) cells = cells.replaceRange(index, index + 1, painted.toString())
            }

            if (availableWidthPx > 0) {
                Column {
                    Row {
                        // Row numbers, aligned with the rows of the grid next to them.
                        Canvas(
                            modifier = Modifier
                                .size(AxisGutter, with(density) { viewHeight.toDp() })
                                .clipToBounds(),
                        ) {
                            val offset = currentPan()
                            val sample = textMeasurer.measure("00", labelStyle).size
                            val step = labelStep(cellPx, sample.height * 1.3f)
                            for (row in visibleRange(offset.y, this.size.height, cellPx, rows)) {
                                // Rows are numbered from the bottom up.
                                val number = rows - row
                                if (number % step != 0) continue
                                val text = textMeasurer.measure("$number", labelStyle)
                                drawText(
                                    text,
                                    topLeft = Offset(
                                        this.size.width - text.size.width - AxisPadding.toPx(),
                                        offset.y + (row + 0.5f) * cellPx - text.size.height / 2f,
                                    ),
                                )
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .size(with(density) { viewWidth.toDp() }, with(density) { viewHeight.toDp() })
                                // Canvas doesn't clip on its own, so cells at the edge would spill out of the viewport.
                                .clipToBounds()
                                .then(
                                    if (saved) {
                                        // No pointer input at all, so swipes fall through to the list.
                                        Modifier
                                    } else if (selectedTool == PAN_TOOL) {
                                        // Keyed on the tool only: re-keying on the zoom would restart the
                                        // gesture in the middle of a pinch.
                                        Modifier.pointerInput(PAN_TOOL) {
                                            detectTransformGestures { centroid, panChange, zoomChange, _ ->
                                                transform(centroid, panChange, zoomChange)
                                            }
                                        }
                                    } else {
                                        Modifier
                                            .pointerInput(rows, columns, cellPx, viewWidth, viewHeight) {
                                                detectTapGestures { position ->
                                                    paint(position)
                                                    onCellsChange(cells)
                                                }
                                            }
                                            .pointerInput(rows, columns, cellPx, viewWidth, viewHeight) {
                                                detectDragGestures(
                                                    onDragStart = { paint(it) },
                                                    onDragEnd = { onCellsChange(cells) },
                                                    onDragCancel = { onCellsChange(cells) },
                                                ) { change, _ ->
                                                    change.consume()
                                                    paint(change.position)
                                                }
                                            }
                                    },
                                ),
                        ) {
                            val offset = currentPan()
                            val cell = Size(cellPx, cellPx)
                            // Only the cells inside the viewport are drawn, which matters for large grids.
                            val firstColumn = (-offset.x / cellPx).toInt().coerceAtLeast(0)
                            val lastColumn = ((-offset.x + this.size.width) / cellPx).toInt().coerceAtMost(columns - 1)
                            val firstRow = (-offset.y / cellPx).toInt().coerceAtLeast(0)
                            val lastRow = ((-offset.y + this.size.height) / cellPx).toInt().coerceAtMost(rows - 1)
                            for (row in firstRow..lastRow) {
                                for (column in firstColumn..lastColumn) {
                                    val topLeft = Offset(column * cellPx, row * cellPx) + offset
                                    val color = PatternPalette[cells[row * columns + column].digitToInt()]
                                    drawRect(color ?: emptyColor, topLeft, cell)
                                }
                            }
                            // Lines on every 5th and 10th boundary are heavier so cells are easy to count.
                        // Boundaries are counted from the bottom right, like the axis numbers.
                            // They're drawn lightest first so a heavy line is never covered by a thin one.
                            val top = offset.y + firstRow * cellPx
                            val bottom = offset.y + (lastRow + 1) * cellPx
                            val left = offset.x + firstColumn * cellPx
                            val right = offset.x + (lastColumn + 1) * cellPx
                            for (weight in LineWeight.entries) {
                                val strokeWidth = weight.width.toPx()
                                val lineColor = if (weight == LineWeight.Thin) gridColor else markerColor
                                // The outer edges are drawn half a stroke inward, so the viewport
                                // doesn't clip half of them off.
                                for (column in firstColumn..lastColumn + 1) {
                                    if (LineWeight.of(column, columns) != weight) continue
                                    val x = offset.x + column * cellPx + edgeInset(column, columns, strokeWidth)
                                    drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth)
                                }
                                for (row in firstRow..lastRow + 1) {
                                    if (LineWeight.of(row, rows) != weight) continue
                                    val y = offset.y + row * cellPx + edgeInset(row, rows, strokeWidth)
                                    drawLine(lineColor, Offset(left, y), Offset(right, y), strokeWidth)
                                }
                            }
                        }
                    }
                    // Column numbers, aligned with the columns of the grid above them.
                    Row {
                        // The canvas reaches past the viewport on both sides so a number centered on an
                        // edge column isn't cut off; only columns centered inside the viewport get one.
                        Spacer(Modifier.width(AxisGutter - AxisLabelOverhang))
                        Canvas(
                            modifier = Modifier
                                .size(with(density) { viewWidth.toDp() } + AxisLabelOverhang * 2, AxisLabelHeight)
                                .clipToBounds(),
                        ) {
                            val offset = currentPan()
                            val overhang = AxisLabelOverhang.toPx()
                            val sample = textMeasurer.measure("00", labelStyle).size
                            val step = labelStep(cellPx, sample.width * 1.3f)
                            for (column in visibleRange(offset.x, viewWidth, cellPx, columns)) {
                                // Columns are numbered from right to left.
                                val number = columns - column
                                if (number % step != 0) continue
                                val center = offset.x + (column + 0.5f) * cellPx
                                if (center < 0f || center > viewWidth) continue
                                val text = textMeasurer.measure("$number", labelStyle)
                                drawText(
                                    text,
                                    topLeft = Offset(overhang + center - text.size.width / 2f, AxisPadding.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolSwatch(
                enabled = !saved,
                selected = selectedTool == PAN_TOOL,
                background = MaterialTheme.colorScheme.surfaceVariant,
                label = stringResource(R.string.pattern_pan),
                onClick = { selectedTool = PAN_TOOL },
            ) {
                Icon(
                    Icons.Filled.OpenWith,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            PatternPalette.forEachIndexed { index, color ->
                val label = if (index == 0) {
                    stringResource(R.string.pattern_erase)
                } else {
                    stringResource(R.string.pattern_color, index)
                }
                ToolSwatch(
                    enabled = !saved,
                    selected = selectedTool == index,
                    background = color ?: MaterialTheme.colorScheme.surface,
                    label = label,
                    onClick = { selectedTool = index },
                )
            }
            Spacer(Modifier.weight(1f))
            // Styled like the status chip on the card: outlined for Edit, filled for Save.
            Surface(
                onClick = {
                    if (saved) {
                        startEditing()
                    } else {
                        onCellsChange(cells)
                        saved = true
                    }
                },
                shape = AssistChipDefaults.shape,
                color = if (saved) Color.Transparent else MaterialTheme.colorScheme.primary,
                contentColor = if (saved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                border = if (saved) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            ) {
                Text(
                    text = stringResource(if (saved) R.string.pattern_edit else R.string.pattern_save),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolSwatch(
    enabled: Boolean,
    selected: Boolean,
    background: Color,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .size(32.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                border = if (selected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                },
                shape = CircleShape,
            )
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}
