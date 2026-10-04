package com.akreutz.knitting.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.knitting.R
import com.akreutz.knitting.data.ProjectStatus

@Composable
fun InProgressProjectsScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = viewModel(),
) {
    val projects by viewModel.projects.collectAsState()
    val inProgress = projects.filter { it.status == ProjectStatus.InProgress }
    var projectToResetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToReset = inProgress.firstOrNull { it.id == projectToResetId }

    if (projectToReset != null) {
        ResetProjectDialog(
            projectName = projectToReset.name,
            onDismiss = { projectToResetId = null },
            onConfirm = {
                viewModel.setStatus(projectToReset, ProjectStatus.Created)
                projectToResetId = null
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (inProgress.isEmpty()) {
            Text(
                text = stringResource(R.string.in_progress_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(inProgress, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onStatusChange = { viewModel.setStatus(project, it) },
                        onLongClick = { projectToResetId = project.id },
                    )
                }
            }
        }
    }
}
