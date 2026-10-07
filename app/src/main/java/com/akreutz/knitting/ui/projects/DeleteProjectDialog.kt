package com.akreutz.knitting.ui.projects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R

@Composable
fun DeleteProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        icon = Icons.Filled.DeleteForever,
        title = stringResource(R.string.delete_project_title),
        message = stringResource(R.string.delete_project_message, projectName),
        confirmLabel = stringResource(R.string.delete),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
