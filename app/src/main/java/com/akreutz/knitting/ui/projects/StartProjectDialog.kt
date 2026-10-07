package com.akreutz.knitting.ui.projects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R

@Composable
fun StartProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        icon = Icons.Filled.PlayArrow,
        title = stringResource(R.string.start_project_title),
        message = stringResource(R.string.start_project_message, projectName),
        confirmLabel = stringResource(R.string.start),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
