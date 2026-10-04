package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme

/**
 * The project form, used to create and to edit a project. Steps are managed here too: [existingSteps]
 * can be edited or removed and new ones added, and all changes are handed to [onConfirm] together.
 */
@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        description: String,
        addedSteps: List<NewStep>,
        removedSteps: List<Step>,
        editedSteps: List<Pair<Step, NewStep>>,
    ) -> Unit,
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
    var editedSteps by remember { mutableStateOf(emptyMap<Long, NewStep>()) }
    var showAddStep by rememberSaveable { mutableStateOf(false) }
    var stepBeingEdited by remember { mutableStateOf<StepEdit?>(null) }
    val keptSteps = existingSteps - removedSteps.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                    val shown = editedSteps[step.id] ?: step.toNewStep()
                    FormStepRow(
                        name = shown.name.trim(),
                        type = shown.type,
                        onClick = { stepBeingEdited = StepEdit.Existing(step) },
                        onRemove = { removedSteps = removedSteps + step },
                    )
                }
                addedSteps.forEachIndexed { index, step ->
                    FormStepRow(
                        name = step.name.trim(),
                        type = step.type,
                        onClick = { stepBeingEdited = StepEdit.Added(index) },
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
                onClick = {
                    val edited = keptSteps.mapNotNull { step -> editedSteps[step.id]?.let { step to it } }
                    onConfirm(name, description, addedSteps, removedSteps, edited)
                },
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

    stepBeingEdited?.let { edit ->
        val initial = when (edit) {
            is StepEdit.Existing -> editedSteps[edit.step.id] ?: edit.step.toNewStep()
            is StepEdit.Added -> addedSteps[edit.index]
        }
        AddStepDialog(
            stepNumber = 0,
            initial = initial,
            titleRes = R.string.edit_step_title,
            confirmRes = R.string.save,
            onDismiss = { stepBeingEdited = null },
            onConfirm = { updated ->
                when (edit) {
                    is StepEdit.Existing -> editedSteps = editedSteps + (edit.step.id to updated)
                    is StepEdit.Added -> addedSteps = addedSteps.mapIndexed { i, s -> if (i == edit.index) updated else s }
                }
                stepBeingEdited = null
            },
        )
    }
}

/** The step the form is currently editing: one that already exists, or one added in this form. */
private sealed interface StepEdit {
    data class Existing(val step: Step) : StepEdit
    data class Added(val index: Int) : StepEdit
}

@Composable
private fun FormStepRow(name: String, type: StepType, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        AddProjectDialog(onDismiss = {}, onConfirm = { _, _, _, _, _ -> })
    }
}
