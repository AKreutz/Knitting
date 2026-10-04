package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import com.akreutz.knitting.ui.theme.KnittingTheme

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    initialName: String = "",
    initialDescription: String = "",
    @StringRes titleRes: Int = R.string.add_project_title,
    @StringRes confirmRes: Int = R.string.add,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
fun AddProjectDialogPreview() {
    KnittingTheme {
        AddProjectDialog(onDismiss = {}, onConfirm = { _, _ -> })
    }
}
