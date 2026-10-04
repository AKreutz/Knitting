package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme

/**
 * The project form, used to create and to edit a project. Steps are managed here too: [existingSteps]
 * can be removed and new ones added, and all changes are handed to [onConfirm] together.
 */
@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, addedSteps: List<NewStep>, removedSteps: List<Step>) -> Unit,
    initialName: String = "",
    initialDescription: String = "",
    existingSteps: List<Step> = emptyList(),
    onDelete: (() -> Unit)? = null,
    @StringRes titleRes: Int = R.string.add_project_title,
    @StringRes confirmRes: Int = R.string.add,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var addedSteps by remember { mutableStateOf(emptyList<NewStep>()) }
    var removedSteps by remember { mutableStateOf(emptyList<Step>()) }
    var showAddStep by rememberSaveable { mutableStateOf(false) }
    val keptSteps = existingSteps - removedSteps.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.project_description_optional)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.project_steps),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                keptSteps.forEach { step ->
                    FormStepRow(step.name, step.type, onRemove = { removedSteps = removedSteps + step })
                }
                addedSteps.forEachIndexed { index, step ->
                    FormStepRow(
                        name = step.name.trim(),
                        type = step.type,
                        onRemove = { addedSteps = addedSteps.filterIndexed { i, _ -> i != index } },
                    )
                }
                TextButton(onClick = { showAddStep = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_step))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description, addedSteps, removedSteps) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )

    if (showAddStep) {
        AddStepDialog(
            stepNumber = keptSteps.size + addedSteps.size + 1,
            onDismiss = { showAddStep = false },
            onConfirm = {
                addedSteps = addedSteps + it
                showAddStep = false
            },
        )
    }
}

@Composable
private fun FormStepRow(name: String, type: StepType, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$name · ${stringResource(type.labelRes())}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.remove_step),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
fun AddProjectDialogPreview() {
    KnittingTheme {
        AddProjectDialog(onDismiss = {}, onConfirm = { _, _, _, _ -> })
    }
}
