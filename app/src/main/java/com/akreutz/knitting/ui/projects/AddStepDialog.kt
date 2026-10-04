package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme

/** What the user entered in [AddStepDialog]; only the fields that apply to [type] are set. */
data class NewStep(
    val name: String,
    val type: StepType,
    val targetRows: Int? = null,
    val stitchCount: Int? = null,
    val method: String? = null,
    val needleSize: String? = null,
    val shapingCount: Int? = null,
    val pattern: String? = null,
    val patternRows: Int? = null,
    val patternColumns: Int? = null,
)

/** Largest pattern grid the user can create, so cells stay tappable. */
internal const val MAX_PATTERN_SIZE = 50

@StringRes
internal fun StepType.labelRes(): Int = when (this) {
    StepType.CastOn -> R.string.step_type_cast_on
    StepType.Increases -> R.string.step_type_increases
    StepType.Decreases -> R.string.step_type_decreases
    StepType.Stockinette -> R.string.step_type_stockinette
    StepType.Pattern -> R.string.step_type_pattern
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddStepDialog(
    onDismiss: () -> Unit,
    onConfirm: (NewStep) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(StepType.Stockinette) }
    var targetRows by rememberSaveable { mutableStateOf("") }
    var stitchCount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("") }
    var needleSize by rememberSaveable { mutableStateOf("") }
    var shapingCount by rememberSaveable { mutableStateOf("") }
    var pattern by rememberSaveable { mutableStateOf("") }
    var gridRows by rememberSaveable { mutableStateOf("") }
    var gridColumns by rememberSaveable { mutableStateOf("") }

    val rows = targetRows.toIntOrNull()
    val stitches = stitchCount.toIntOrNull()
    val shaping = shapingCount.toIntOrNull()
    val gridSize = MAX_PATTERN_SIZE.let { max ->
        val r = gridRows.toIntOrNull()?.takeIf { it in 1..max }
        val c = gridColumns.toIntOrNull()?.takeIf { it in 1..max }
        if (r != null && c != null) r to c else null
    }
    val newStep = when (type) {
        StepType.Pattern -> gridSize?.let { (r, c) ->
            NewStep(name = name, type = type, patternRows = r, patternColumns = c)
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
        else -> rows?.takeIf { it > 0 }?.let { NewStep(name = name, type = type, targetRows = it) }
    }

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
                Text(
                    text = stringResource(R.string.step_type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepType.entries.forEach { option ->
                        FilterChip(
                            selected = option == type,
                            onClick = { type = option },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
                if (type == StepType.Pattern) {
                    NumberField(
                        value = gridRows,
                        onValueChange = { gridRows = it },
                        labelRes = R.string.step_pattern_rows,
                    )
                    NumberField(
                        value = gridColumns,
                        onValueChange = { gridColumns = it },
                        labelRes = R.string.step_pattern_columns,
                    )
                    Text(
                        text = stringResource(R.string.step_pattern_size_hint, MAX_PATTERN_SIZE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
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
                    NumberField(
                        value = targetRows,
                        onValueChange = { targetRows = it },
                        labelRes = R.string.step_target_rows,
                    )
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

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

@Preview
@Composable
private fun AddStepDialogPreview() {
    KnittingTheme {
        AddStepDialog(onDismiss = {}, onConfirm = {})
    }
}
