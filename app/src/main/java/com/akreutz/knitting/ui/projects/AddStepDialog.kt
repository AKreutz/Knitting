package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.MM_PER_CM
import com.akreutz.knitting.data.PatternType
import com.akreutz.knitting.data.RowPattern
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme
import com.akreutz.knitting.ui.theme.Spacing

/** What the user entered in [AddStepDialog]; only the fields that apply to [type] are set. */
data class NewStep(
    val name: String,
    val type: StepType,
    val color: String? = null,
    val targetRows: Int? = null,
    val stitchCount: Int? = null,
    val method: String? = null,
    val needleSize: String? = null,
    val description: String? = null,
    val shapingCount: Int? = null,
    val pattern: String? = null,
    val patternType: PatternType? = null,
    val rowPattern: RowPattern? = null,
    val patternRows: Int? = null,
    val patternColumns: Int? = null,
    val patternRepeats: Int? = null,
    val trackInCm: Boolean = false,
    val targetMm: Int? = null,
)

internal fun Step.toNewStep() = NewStep(
    name = name,
    type = type,
    color = color,
    targetRows = targetRows,
    stitchCount = stitchCount,
    method = method,
    needleSize = needleSize,
    description = description,
    shapingCount = shapingCount,
    pattern = pattern,
    patternType = patternType,
    rowPattern = rowPattern,
    patternRows = patternRows,
    patternColumns = patternColumns,
    patternRepeats = patternRepeats,
    trackInCm = trackInCm,
    targetMm = targetMm,
)

/** Largest pattern grid the user can create, so cells stay tappable. */
internal const val MAX_PATTERN_SIZE = 50

@StringRes
internal fun PatternType.labelRes(): Int = when (this) {
    PatternType.Cables -> R.string.pattern_type_cables
    PatternType.Colorwork -> R.string.pattern_type_colorwork
}

@StringRes
internal fun RowPattern.labelRes(): Int = when (this) {
    RowPattern.AllKnit -> R.string.row_pattern_all_knit
    RowPattern.AllPurl -> R.string.row_pattern_all_purl
    RowPattern.Ribbing1x1 -> R.string.row_pattern_ribbing_1x1
    RowPattern.Ribbing2x2 -> R.string.row_pattern_ribbing_2x2
}

@StringRes
internal fun StepType.labelRes(): Int = when (this) {
    StepType.CastOn -> R.string.step_type_cast_on
    StepType.Increases -> R.string.step_type_increases
    StepType.Decreases -> R.string.step_type_decreases
    StepType.PlainRows -> R.string.step_type_plain_rows
    StepType.Pattern -> R.string.step_type_pattern
    StepType.Special -> R.string.step_type_special
}

@Composable
fun AddStepDialog(
    stepNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (NewStep) -> Unit,
    /** Set to edit an existing step: the form starts with its values. */
    initial: NewStep? = null,
    @StringRes titleRes: Int = R.string.add_step_title,
    @StringRes confirmRes: Int = R.string.add,
) {
    val defaultName = stringResource(R.string.step_default_name, stepNumber)
    var name by rememberSaveable { mutableStateOf(initial?.name ?: defaultName) }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: StepType.PlainRows) }
    var color by rememberSaveable { mutableStateOf(initial?.color.orEmpty()) }
    var targetRows by rememberSaveable { mutableStateOf(initial?.targetRows?.toString().orEmpty()) }
    var stitchCount by rememberSaveable { mutableStateOf(initial?.stitchCount?.toString().orEmpty()) }
    var method by rememberSaveable { mutableStateOf(initial?.method.orEmpty()) }
    var needleSize by rememberSaveable { mutableStateOf(initial?.needleSize.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var shapingCount by rememberSaveable { mutableStateOf(initial?.shapingCount?.toString().orEmpty()) }
    var pattern by rememberSaveable { mutableStateOf(initial?.pattern.orEmpty()) }
    var patternType by rememberSaveable { mutableStateOf(initial?.patternType ?: PatternType.Cables) }
    var rowPattern by rememberSaveable { mutableStateOf(initial?.rowPattern ?: RowPattern.AllKnit) }
    var gridRows by rememberSaveable { mutableStateOf(initial?.patternRows?.toString().orEmpty()) }
    var gridColumns by rememberSaveable { mutableStateOf(initial?.patternColumns?.toString().orEmpty()) }
    var patternRepeats by rememberSaveable { mutableStateOf(initial?.patternRepeats?.toString() ?: "1") }

    var trackInCm by rememberSaveable { mutableStateOf(initial?.trackInCm ?: false) }
    var targetCm by rememberSaveable { mutableStateOf(initial?.targetMm?.let(::mmToText).orEmpty()) }

    val rows = targetRows.toIntOrNull()
    val repeats = patternRepeats.toIntOrNull()
    val lengthMm = textToMm(targetCm)?.takeIf { it > 0 }
    val stitches = stitchCount.toIntOrNull()
    val shaping = shapingCount.toIntOrNull()
    val gridSize = MAX_PATTERN_SIZE.let { max ->
        val r = gridRows.toIntOrNull()?.takeIf { it in 1..max }
        val c = gridColumns.toIntOrNull()?.takeIf { it in 1..max }
        if (r != null && c != null) r to c else null
    }
    val newStep = when (type) {
        StepType.Pattern -> gridSize?.let { (r, c) ->
            val length = if (trackInCm) lengthMm else repeats?.takeIf { it > 0 }
            length?.let {
                NewStep(
                    name = name,
                    type = type,
                    patternType = patternType,
                    patternRows = r,
                    patternColumns = c,
                    patternRepeats = if (trackInCm) null else it,
                    trackInCm = trackInCm,
                    targetMm = if (trackInCm) it else null,
                )
            }
        }
        StepType.Increases, StepType.Decreases -> shaping?.takeIf { it > 0 }?.let {
            NewStep(
                name = name,
                type = type,
                shapingCount = it,
                pattern = pattern.trim().ifEmpty { null },
            )
        }
        StepType.CastOn -> stitches?.takeIf { it > 0 }?.let {
            NewStep(
                name = name,
                type = type,
                stitchCount = it,
                method = method.trim().ifEmpty { null },
                needleSize = needleSize.trim().ifEmpty { null },
            )
        }
        StepType.Special -> NewStep(
            name = name,
            type = type,
            description = description.trim().ifEmpty { null },
        )
        else -> (if (trackInCm) lengthMm else rows?.takeIf { it > 0 })?.let {
            NewStep(
                name = name,
                type = type,
                targetRows = if (trackInCm) null else it,
                rowPattern = rowPattern.takeIf { type == StepType.PlainRows },
                trackInCm = trackInCm,
                targetMm = if (trackInCm) it else null,
            )
        }
    }?.copy(color = color.trim().ifEmpty { null })

    val hasLength = type == StepType.Pattern || type == StepType.PlainRows

    FormDialog(
        title = stringResource(titleRes),
        confirmLabel = stringResource(confirmRes),
        confirmEnabled = name.isNotBlank() && newStep != null,
        onConfirm = { newStep?.let(onConfirm) },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.step_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FormSection(stringResource(R.string.step_type)) {
            StepTypeGrid(selected = type, onSelect = { type = it })
        }
        FormSection(stringResource(R.string.section_details)) {
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                value = color,
                onValueChange = { color = it },
                label = { Text(stringResource(R.string.step_color_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when (type) {
                StepType.Pattern -> {
                    EnumDropdown(
                        labelRes = R.string.step_pattern_type,
                        selected = patternType,
                        options = PatternType.entries,
                        optionLabelRes = PatternType::labelRes,
                        onSelect = { patternType = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Entered as "rows × columns", side by side.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NumberField(
                            value = gridRows,
                            onValueChange = { gridRows = it },
                            labelRes = R.string.step_pattern_rows,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = Spacing.sm),
                        )
                        NumberField(
                            value = gridColumns,
                            onValueChange = { gridColumns = it },
                            labelRes = R.string.step_pattern_columns,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.step_pattern_size_hint, MAX_PATTERN_SIZE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StepType.Increases, StepType.Decreases -> {
                    val increases = type == StepType.Increases
                    NumberField(
                        value = shapingCount,
                        onValueChange = { shapingCount = it },
                        labelRes = if (increases) R.string.step_increase_count else R.string.step_decrease_count,
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = {
                            Text(
                                stringResource(
                                    if (increases) {
                                        R.string.step_increase_pattern_optional
                                    } else {
                                        R.string.step_decrease_pattern_optional
                                    },
                                ),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                StepType.CastOn -> {
                    NumberField(
                        value = stitchCount,
                        onValueChange = { stitchCount = it },
                        labelRes = R.string.step_stitch_count,
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = method,
                        onValueChange = { method = it },
                        label = { Text(stringResource(R.string.step_method_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = needleSize,
                        onValueChange = { needleSize = it },
                        label = { Text(stringResource(R.string.step_needle_size_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                StepType.Special -> OutlinedTextField(
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.step_description_optional)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                StepType.PlainRows -> EnumDropdown(
                    labelRes = R.string.step_row_pattern,
                    selected = rowPattern,
                    options = RowPattern.entries,
                    optionLabelRes = RowPattern::labelRes,
                    onSelect = { rowPattern = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (hasLength) {
            FormSection(stringResource(R.string.section_length)) {
                CmToggle(trackInCm) { trackInCm = it }
                if (trackInCm) {
                    DecimalField(
                        value = targetCm,
                        onValueChange = { targetCm = it },
                        labelRes = R.string.step_target_cm,
                    )
                } else if (type == StepType.Pattern) {
                    NumberField(
                        value = patternRepeats,
                        onValueChange = { patternRepeats = it },
                        labelRes = R.string.step_pattern_repeats,
                    )
                } else {
                    NumberField(
                        value = targetRows,
                        onValueChange = { targetRows = it },
                        labelRes = R.string.step_target_rows,
                    )
                }
            }
        }
    }
}

internal fun StepType.icon(): ImageVector = when (this) {
    StepType.CastOn -> Icons.Filled.LinearScale
    StepType.Increases -> Icons.Filled.AddCircle
    StepType.Decreases -> Icons.Filled.RemoveCircle
    StepType.PlainRows -> Icons.Filled.Reorder
    StepType.Pattern -> Icons.Filled.Grid4x4
    StepType.Special -> Icons.Filled.AutoAwesome
}

/** All step types as a grid of selectable tiles, three per row. */
@Composable
private fun StepTypeGrid(selected: StepType, onSelect: (StepType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StepType.entries.chunked(3).forEach { rowTypes ->
            // Tiles in a row share the height of the tallest one, so a label that wraps does not make them uneven.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                rowTypes.forEach { option ->
                    val isSelected = option == selected
                    Surface(
                        selected = isSelected,
                        onClick = { onSelect(option) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
                        ) {
                            DuotoneIcon(option.icon())
                            Text(
                                text = stringResource(option.labelRes()),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    @StringRes labelRes: Int,
    selected: T,
    options: List<T>,
    optionLabelRes: (T) -> Int,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = stringResource(optionLabelRes(selected)),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(labelRes)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(optionLabelRes(option))) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Switches a plain rows or pattern step between tracking its length in rows or repeats and in centimeters. */
@Composable
private fun CmToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(R.string.step_track_in_cm),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Millimeters as the centimeter text the user edits, e.g. 25 -> "2.5". */
private fun mmToText(mm: Int): String =
    if (mm % MM_PER_CM == 0) "${mm / MM_PER_CM}" else "${mm / MM_PER_CM}.${mm % MM_PER_CM}"

/** The millimeters of a centimeter text such as "2.5", or null when it is not a number. */
private fun textToMm(text: String): Int? =
    text.toBigDecimalOrNull()?.movePointRight(1)?.toInt()

/** Like [NumberField], but accepts one decimal place, for lengths in centimeters. */
@Composable
private fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // A comma is accepted as the decimal separator; at most one decimal place is kept.
            val text = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
            val whole = text.substringBefore('.').take(4)
            val fraction = text.substringAfter('.', "").filter(Char::isDigit).take(1)
            onValueChange(if ('.' in text) "$whole.$fraction" else whole)
        },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AddStepDialogPreview() {
    KnittingTheme {
        AddStepDialog(stepNumber = 1, onDismiss = {}, onConfirm = {})
    }
}
