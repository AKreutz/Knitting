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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.akreutz.knitting.data.PatternType
import com.akreutz.knitting.ui.theme.Spacing

/** Colors a pattern cell can be painted with. Index 0 is empty; the stored cell string holds indices. */
private val PatternPalette = listOf(
    null,
    Color(0xFFB5493B),
    Color(0xFFE0A526),
    Color(0xFF6E8B5B),
    Color(0xFF3F5A7A),
)

/** Laid over the cells of completed colorwork rows so they read as done while keeping their colors. */
private val CompletedRowCover = Color.Black.copy(alpha = 0.5f)

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

/**
 * Positions along one axis of the grid. The divider lines take up room of their own between the
 * cells, wider for heavier lines, so a line never covers the inside of a cell. Cells are all the
 * same size and scale with zoom, while the dividers keep their width.
 *
 * Boundary `b` is the line before cell `b`, and boundary [count] is the one after the last cell.
 */
private class AxisGeometry(val count: Int, lineWidth: (boundary: Int) -> Float) {
    val lineWidths = FloatArray(count + 1) { lineWidth(it) }

    /** Combined width of the lines up to and including each boundary. */
    private val linesUpTo = FloatArray(count + 1).also {
        var sum = 0f
        for (boundary in 0..count) {
            sum += lineWidths[boundary]
            it[boundary] = sum
        }
    }

    /** Combined width of all the lines. */
    val linesTotal = linesUpTo[count]

    /** Length of the whole axis for cells of size [cell]. */
    fun extent(cell: Float) = count * cell + linesTotal

    fun cellStart(index: Int, cell: Float) = index * cell + linesUpTo[index]

    fun lineStart(boundary: Int, cell: Float) = boundary * cell + linesUpTo[boundary] - lineWidths[boundary]

    /** The last cell starting at or before [position], or -1 if the position is before the first cell. */
    private fun cellAtOrBefore(position: Float, cell: Float): Int {
        var found = -1
        for (index in 0 until count) {
            if (cellStart(index, cell) <= position) found = index else break
        }
        return found
    }

    /** The cell containing [position], or null when it falls on a line or outside the grid. */
    fun cellAt(position: Float, cell: Float): Int? {
        val index = cellAtOrBefore(position, cell).takeIf { it >= 0 } ?: return null
        return index.takeIf { position < cellStart(it, cell) + cell }
    }

    /** [position] as a fractional cell index, so the same spot can be found again after the cell size changes. */
    fun fractionAt(position: Float, cell: Float): Float {
        val index = cellAtOrBefore(position, cell).coerceAtLeast(0)
        return index + (position - cellStart(index, cell)) / cell
    }

    /** The inverse of [fractionAt]. */
    fun positionOf(fraction: Float, cell: Float): Float {
        val index = kotlin.math.floor(fraction).toInt().coerceIn(0, count - 1)
        return cellStart(index, cell) + (fraction - index) * cell
    }

    /** The cells that overlap a viewport of [viewport] pixels when the grid is shifted by [offset]. */
    fun visibleCells(offset: Float, viewport: Float, cell: Float): IntRange =
        cellAtOrBefore(-offset, cell).coerceAtLeast(0)..cellAtOrBefore(-offset + viewport, cell)
}

/** Space reserved left of and below the viewport for the row and column numbers. */
private val MinAxisGutter = 12.dp
private val AxisLabelHeight = 16.dp
private val AxisPadding = 4.dp
private val AxisLabelOverhang = 12.dp

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

/** Names of the cable stitches, indexed by the value stored in a cell. Index 0 is the default. */
private val CableStitchLabels = listOf(
    R.string.stitch_knit,
    R.string.stitch_purl,
    R.string.stitch_cross_left,
    R.string.stitch_cross_right,
)

/**
 * Draws a cross stitch ([stitch] 2 is cross left `\`, 3 is cross right `/`) corner to corner of the
 * rectangle, clipped to it so the ends sit exactly on the corners. The rectangle is one cell, or
 * several neighboring cells that share a single diagonal.
 */
private fun DrawScope.drawCross(
    stitch: Int,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    cellPx: Float,
) {
    val width = maxOf(1.25.dp.toPx(), cellPx * 0.07f)
    clipRect(left, top, right, bottom) {
        if (stitch == 2) {
            drawLine(color, Offset(left, top), Offset(right, bottom), width)
        } else {
            drawLine(color, Offset(left, bottom), Offset(right, top), width)
        }
    }
}

/**
 * Draws cable stitch [stitch] in the square cell at [topLeft]: v knit, centered dot purl, \ cross left,
 * / cross right. Neighboring crosses are drawn together by the grid, so a grid only calls this for
 * knit and purl.
 */
private fun DrawScope.drawStitch(stitch: Int, topLeft: Offset, cellPx: Float, color: Color) {
    val inset = cellPx * 0.32f
    val left = topLeft.x + inset
    val right = topLeft.x + cellPx - inset
    val top = topLeft.y + inset
    val bottom = topLeft.y + cellPx - inset
    val center = Offset(topLeft.x + cellPx / 2f, topLeft.y + cellPx / 2f)
    val width = maxOf(1.25.dp.toPx(), cellPx * 0.07f)
    when (stitch) {
        1 -> drawCircle(color, radius = maxOf(1.5.dp.toPx(), cellPx * 0.08f), center = center)
        2, 3 -> drawCross(stitch, topLeft.x, topLeft.y, topLeft.x + cellPx, topLeft.y + cellPx, color, cellPx)
        else -> {
            val point = Offset(center.x, bottom)
            drawLine(color, Offset(left, top), point, width, cap = StrokeCap.Round)
            drawLine(color, point, Offset(right, top), width, cap = StrokeCap.Round)
        }
    }
}

/**
 * A rows × columns grid the user paints by tapping or dragging, with a palette underneath: colors
 * for colorwork patterns, stitch symbols for cables. A grid that is not [editable] is only shown,
 * without the palette and the Edit button. The bottom [completedRows] rows, which are knitted first,
 * are marked as done: colorwork cells are dimmed, cable cells have their colors swapped.
 */
@Composable
internal fun PatternGrid(
    rows: Int,
    columns: Int,
    patternType: PatternType?,
    storedCells: String?,
    onCellsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    completedRows: Int = 0,
) {
    val cables = patternType == PatternType.Cables
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
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // The row numbers get just the room their widest number needs, so the whole grid, numbers
    // included, starts at the card's left content edge.
    val gutterPx = with(density) {
        maxOf(MinAxisGutter.toPx(), textMeasurer.measure("$rows", labelStyle).size.width + AxisPadding.toPx())
    }
    val gutter = with(density) { gutterPx.toDp() }
    val usableWidthPx = (availableWidthPx - gutterPx).coerceAtLeast(0f)
    // Dividers have a fixed width of their own, so the cells share what is left of the viewport.
    val columnAxis = remember(columns, density) {
        AxisGeometry(columns) { with(density) { LineWeight.of(it, columns).width.toPx() } }
    }
    val rowAxis = remember(rows, density) {
        AxisGeometry(rows) { with(density) { LineWeight.of(it, rows).width.toPx() } }
    }
    val baseCellPx = ((usableWidthPx - columnAxis.linesTotal) / columns)
        .coerceIn(with(density) { MinCellSize.toPx() }, with(density) { MaxCellSize.toPx() })
    // While editing, the viewport is sized from the unzoomed grid, so pinching never resizes the layout.
    val editWidth = minOf(columnAxis.extent(baseCellPx), usableWidthPx)
    val editHeight = minOf(rowAxis.extent(baseCellPx), with(density) { MaxViewportHeight.toPx() })
    // The zoom at which the whole grid fits in the editing viewport. A saved grid always uses it,
    // which is computed rather than stored so it is right from the first frame and follows resizes.
    val fitZoom = minOf(
        (editWidth - columnAxis.linesTotal) / (baseCellPx * columns),
        (editHeight - rowAxis.linesTotal) / (baseCellPx * rows),
        1f,
    ).coerceAtLeast(MinZoom)
    val cellPx = baseCellPx * if (saved) fitZoom else zoom
    // A saved grid has no use for spare space, so its viewport hugs it and the axis numbers
    // stay right next to its edges.
    val viewWidth = if (saved) columnAxis.extent(cellPx) else editWidth
    val viewHeight = if (saved) rowAxis.extent(cellPx) else editHeight

    // Pan is stored unclamped and clamped on use, so it stays valid when sizes change.
    // A grid smaller than the viewport can sit anywhere inside it.
    fun clampedPan(offset: Offset, cell: Float = cellPx): Offset {
        val slackX = viewWidth - columnAxis.extent(cell)
        val slackY = viewHeight - rowAxis.extent(cell)
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
        pan = Offset((editWidth - columnAxis.extent(cellPx)) / 2f, (editHeight - rowAxis.extent(cellPx)) / 2f)
        saved = false
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        // The width is measured with onSizeChanged rather than BoxWithConstraints, because the
        // project card asks its content for intrinsic sizes, which subcomposition can't answer.
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { availableWidthPx = it.width }) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val emptyColor = MaterialTheme.colorScheme.surface
            // The card's own text color, the slightly brown one the project name uses.
            val stitchColor = LocalContentColor.current
            val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

            // Zooms around the pinch centroid so the cell under the fingers stays put.
            val transform by rememberUpdatedState { centroid: Offset, panChange: Offset, zoomChange: Float ->
                val newZoom = (zoom * zoomChange).coerceIn(MinZoom, MaxZoom)
                val newCellPx = baseCellPx * newZoom
                val inGrid = centroid - currentPan()
                val spotX = columnAxis.fractionAt(inGrid.x, cellPx)
                val spotY = rowAxis.fractionAt(inGrid.y, cellPx)
                val newSpot = Offset(columnAxis.positionOf(spotX, newCellPx), rowAxis.positionOf(spotY, newCellPx))
                pan = clampedPan(centroid - newSpot + panChange, newCellPx)
                zoom = newZoom
            }

            fun paint(position: Offset) {
                val inGrid = position - currentPan()
                val column = columnAxis.cellAt(inGrid.x, cellPx) ?: return
                val row = rowAxis.cellAt(inGrid.y, cellPx) ?: return
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
                                .size(gutter, with(density) { viewHeight.toDp() })
                                .clipToBounds(),
                        ) {
                            val offset = currentPan()
                            val sample = textMeasurer.measure("00", labelStyle).size
                            val step = labelStep(cellPx, sample.height * 1.3f)
                            for (row in rowAxis.visibleCells(offset.y, this.size.height, cellPx)) {
                                // Rows are numbered from the bottom up.
                                val number = rows - row
                                if (number % step != 0) continue
                                val text = textMeasurer.measure("$number", labelStyle)
                                drawText(
                                    text,
                                    topLeft = Offset(
                                        this.size.width - text.size.width - AxisPadding.toPx(),
                                        offset.y + rowAxis.cellStart(row, cellPx) + cellPx / 2f - text.size.height / 2f,
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
                            val visibleColumns = columnAxis.visibleCells(offset.x, this.size.width, cellPx)
                            val visibleRows = rowAxis.visibleCells(offset.y, this.size.height, cellPx)
                            for (row in visibleRows) {
                                for (column in visibleColumns) {
                                    val topLeft = Offset(
                                        columnAxis.cellStart(column, cellPx),
                                        rowAxis.cellStart(row, cellPx),
                                    ) + offset
                                    val value = cells[row * columns + column].digitToInt()
                                    val done = row >= rows - completedRows
                                    if (cables) {
                                        drawRect(if (done) stitchColor else emptyColor, topLeft, cell)
                                        if (value < 2) {
                                            drawStitch(value, topLeft, cellPx, if (done) emptyColor else stitchColor)
                                        }
                                    } else {
                                        drawRect(PatternPalette.getOrNull(value) ?: emptyColor, topLeft, cell)
                                        if (done) drawRect(CompletedRowCover, topLeft, cell)
                                    }
                                }
                            }
                            // Lines on every 5th and 10th boundary are heavier so cells are easy to count.
                            // Boundaries are counted from the bottom right, like the axis numbers.
                            // Each line fills the gap left for it between the cells, and they're drawn
                            // lightest first so a heavy line is never covered by a thin one.
                            val gridWidth = columnAxis.extent(cellPx)
                            val gridHeight = rowAxis.extent(cellPx)
                            for (weight in LineWeight.entries) {
                                val lineColor = if (weight == LineWeight.Thin) gridColor else markerColor
                                for (column in visibleColumns.first..visibleColumns.last + 1) {
                                    if (LineWeight.of(column, columns) != weight) continue
                                    val x = offset.x + columnAxis.lineStart(column, cellPx)
                                    drawRect(
                                        lineColor,
                                        Offset(x, offset.y),
                                        Size(columnAxis.lineWidths[column], gridHeight),
                                    )
                                }
                                for (row in visibleRows.first..visibleRows.last + 1) {
                                    if (LineWeight.of(row, rows) != weight) continue
                                    val y = offset.y + rowAxis.lineStart(row, cellPx)
                                    drawRect(lineColor, Offset(offset.x, y), Size(gridWidth, rowAxis.lineWidths[row]))
                                }
                            }
                            if (cables) {
                                // A run of neighboring cells with the same cross shares one diagonal, from
                                // a corner of the first cell to the opposite corner of the last. It's drawn
                                // over the dividers so it stays unbroken across them.
                                for (row in visibleRows) {
                                    val top = offset.y + rowAxis.cellStart(row, cellPx)
                                    var start = 0
                                    while (start < columns) {
                                        val stitch = cells[row * columns + start]
                                        if (stitch < '2') {
                                            start++
                                            continue
                                        }
                                        var end = start
                                        while (end + 1 < columns && cells[row * columns + end + 1] == stitch) end++
                                        if (end >= visibleColumns.first && start <= visibleColumns.last) {
                                            drawCross(
                                                stitch = stitch.digitToInt(),
                                                left = offset.x + columnAxis.cellStart(start, cellPx),
                                                top = top,
                                                right = offset.x + columnAxis.cellStart(end, cellPx) + cellPx,
                                                bottom = top + cellPx,
                                                color = if (row >= rows - completedRows) emptyColor else stitchColor,
                                                cellPx = cellPx,
                                            )
                                        }
                                        start = end + 1
                                    }
                                }
                            }
                        }
                    }
                    // Column numbers, aligned with the columns of the grid above them.
                    Row {
                        // The canvas reaches past the viewport on both sides so a number centered on an
                        // edge column isn't cut off; only columns centered inside the viewport get one.
                        Spacer(Modifier.width(gutter - AxisLabelOverhang))
                        Canvas(
                            modifier = Modifier
                                .size(with(density) { viewWidth.toDp() } + AxisLabelOverhang * 2, AxisLabelHeight)
                                .clipToBounds(),
                        ) {
                            val offset = currentPan()
                            val overhang = AxisLabelOverhang.toPx()
                            val sample = textMeasurer.measure("00", labelStyle).size
                            val step = labelStep(cellPx, sample.width * 1.3f)
                            for (column in columnAxis.visibleCells(offset.x, viewWidth, cellPx)) {
                                // Columns are numbered from right to left.
                                val number = columns - column
                                if (number % step != 0) continue
                                val center = offset.x + columnAxis.cellStart(column, cellPx) + cellPx / 2f
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
        if (editable) Row(
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
            if (cables) {
                // Cable charts are painted with stitch symbols instead of colors.
                CableStitchLabels.forEachIndexed { index, labelRes ->
                    val stitchColor = LocalContentColor.current
                    ToolSwatch(
                        enabled = !saved,
                        selected = selectedTool == index,
                        background = MaterialTheme.colorScheme.surface,
                        label = stringResource(labelRes),
                        onClick = { selectedTool = index },
                    ) {
                        Canvas(Modifier.size(24.dp)) { drawStitch(index, Offset.Zero, this.size.width, stitchColor) }
                    }
                }
            } else {
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
