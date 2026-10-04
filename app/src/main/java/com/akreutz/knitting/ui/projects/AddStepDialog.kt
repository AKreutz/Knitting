package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.PatternType
import com.akreutz.knitting.data.RowPattern
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme

/** What the user entered in [AddStepDialog]; only the fields that apply to [type] are set. */
data class NewStep(
    val name: String,
    val type: StepType,
    val color: String? = null,
    val targetRows: Int? = null,
    val stitchCount: Int? = null,
    val method: String? = null,
    val needleSize: String? = null,
    val shapingCount: Int? = null,
    val pattern: String? = null,
    val patternType: PatternType? = null,
    val rowPattern: RowPattern? = null,
    val patternRows: Int? = null,
    val patternColumns: Int? = null,
    val patternRepeats: Int? = null,
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
}

@Composable
fun AddStepDialog(
    stepNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (NewStep) -> Unit,
) {
    val defaultName = stringResource(R.string.step_default_name, stepNumber)
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var type by rememberSaveable { mutableStateOf(StepType.PlainRows) }
    var color by rememberSaveable { mutableStateOf("") }
    var targetRows by rememberSaveable { mutableStateOf("") }
    var stitchCount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("") }
    var needleSize by rememberSaveable { mutableStateOf("") }
    var shapingCount by rememberSaveable { mutableStateOf("") }
    var pattern by rememberSaveable { mutableStateOf("") }
    var patternType by rememberSaveable { mutableStateOf(PatternType.Cables) }
    var rowPattern by rememberSaveable { mutableStateOf(RowPattern.AllKnit) }
    var gridRows by rememberSaveable { mutableStateOf("") }
    var gridColumns by rememberSaveable { mutableStateOf("") }
    var patternRepeats by rememberSaveable { mutableStateOf("1") }

    val rows = targetRows.toIntOrNull()
    val repeats = patternRepeats.toIntOrNull()
    val stitches = stitchCount.toIntOrNull()
    val shaping = shapingCount.toIntOrNull()
    val gridSize = MAX_PATTERN_SIZE.let { max ->
        val r = gridRows.toIntOrNull()?.takeIf { it in 1..max }
        val c = gridColumns.toIntOrNull()?.takeIf { it in 1..max }
        if (r != null && c != null) r to c else null
    }
    val newStep = when (type) {
        StepType.Pattern -> gridSize?.let { (r, c) ->
            repeats?.takeIf { it > 0 }?.let {
                NewStep(
                    name = name,
                    type = type,
                    patternType = patternType,
                    patternRows = r,
                    patternColumns = c,
                    patternRepeats = it,
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
        else -> rows?.takeIf { it > 0 }?.let {
            NewStep(
                name = name,
                type = type,
                targetRows = it,
                rowPattern = rowPattern.takeIf { type == StepType.PlainRows },
            )
        }
    }?.copy(color = color.trim().ifEmpty { null })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_step_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.step_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EnumDropdown(
                    labelRes = R.string.step_type,
                    selected = type,
                    options = StepType.entries,
                    optionLabelRes = StepType::labelRes,
                    onSelect = { type = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(stringResource(R.string.step_color_optional)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (type == StepType.Pattern) {
                    EnumDropdown(
                        labelRes = R.string.step_pattern_type,
                        selected = patternType,
                        options = PatternType.entries,
                        optionLabelRes = PatternType::labelRes,
                        onSelect = { patternType = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    // Entered as "rows × columns", side by side.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
                            modifier = Modifier.padding(horizontal = 8.dp),
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
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    NumberField(
                        value = patternRepeats,
                        onValueChange = { patternRepeats = it },
                        labelRes = R.string.step_pattern_repeats,
                    )
                } else if (type == StepType.Increases || type == StepType.Decreases) {
                    val increases = type == StepType.Increases
                    NumberField(
                        value = shapingCount,
                        onValueChange = { shapingCount = it },
                        labelRes = if (increases) R.string.step_increase_count else R.string.step_decrease_count,
                    )
                    OutlinedTextField(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                } else if (type == StepType.CastOn) {
                    NumberField(
                        value = stitchCount,
                        onValueChange = { stitchCount = it },
                        labelRes = R.string.step_stitch_count,
                    )
                    OutlinedTextField(
                        value = method,
                        onValueChange = { method = it },
                        label = { Text(stringResource(R.string.step_method_optional)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    OutlinedTextField(
                        value = needleSize,
                        onValueChange = { needleSize = it },
                        label = { Text(stringResource(R.string.step_needle_size_optional)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        if (type == StepType.PlainRows) {
                            EnumDropdown(
                                labelRes = R.string.step_row_pattern,
                                selected = rowPattern,
                                options = RowPattern.entries,
                                optionLabelRes = RowPattern::labelRes,
                                onSelect = { rowPattern = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            )
                        }
                        NumberField(
                            value = targetRows,
                            onValueChange = { targetRows = it },
                            labelRes = R.string.step_target_rows,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { newStep?.let(onConfirm) },
                enabled = name.isNotBlank() && newStep != null,
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp),
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
