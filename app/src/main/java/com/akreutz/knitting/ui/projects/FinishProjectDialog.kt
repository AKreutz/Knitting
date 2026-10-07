package com.akreutz.knitting.ui.projects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R

@Composable
fun FinishProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        icon = Icons.Filled.Celebration,
        title = stringResource(R.string.finish_project_title),
        message = stringResource(R.string.finish_project_message, projectName),
        confirmLabel = stringResource(R.string.finish),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
