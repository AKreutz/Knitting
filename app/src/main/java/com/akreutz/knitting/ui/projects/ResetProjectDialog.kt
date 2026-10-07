package com.akreutz.knitting.ui.projects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R

@Composable
fun ResetProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        icon = Icons.Filled.RestartAlt,
        title = stringResource(R.string.reset_project_title),
        message = stringResource(R.string.reset_project_message, projectName),
        confirmLabel = stringResource(R.string.reset),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
