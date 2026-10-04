package com.akreutz.knitting.ui.projects

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.annotation.StringRes
import com.akreutz.knitting.ui.theme.KnittingTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun ProjectsScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = viewModel(),
) {
    val projects by viewModel.projects.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var projectToStartId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToStart = projects.firstOrNull { it.id == projectToStartId }
    var projectToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToDelete = projects.firstOrNull { it.id == projectToDeleteId }

    ProjectsContent(
        projects = projects,
        onAddClick = { showAddDialog = true },
        onProjectClick = { projectToStartId = it.id },
        onProjectLongClick = { projectToDeleteId = it.id },
        onStatusChange = viewModel::setStatus,
        modifier = modifier,
    )

    if (projectToDelete != null) {
        DeleteProjectDialog(
            projectName = projectToDelete.name,
            onDismiss = { projectToDeleteId = null },
            onConfirm = {
                viewModel.deleteProject(projectToDelete)
                projectToDeleteId = null
            },
        )
    }

    if (projectToStart != null) {
        StartProjectDialog(
            projectName = projectToStart.name,
            onDismiss = { projectToStartId = null },
            onConfirm = {
                viewModel.setStatus(projectToStart, ProjectStatus.InProgress)
                projectToStartId = null
            },
        )
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description ->
                viewModel.addProject(name, description)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ProjectsContent(
    projects: List<Project>,
    onAddClick: () -> Unit,
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit,
    onStatusChange: (Project, ProjectStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (projects.isEmpty()) {
            Text(
                text = stringResource(R.string.projects_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                // Bottom padding keeps the last card clear of the FAB.
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onStatusChange = { onStatusChange(project, it) },
                        onClick = if (project.status == ProjectStatus.Created) {
                            { onProjectClick(project) }
                        } else {
                            null
                        },
                        onLongClick = { onProjectLongClick(project) },
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_project_title))
        }
    }
}

@Composable
internal fun ProjectCard(
    project: Project,
    onStatusChange: (ProjectStatus) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = project.name, style = MaterialTheme.typography.titleMedium)
            project.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            project.startedAt?.let { DateLine(R.string.started_at, it) }
            project.completedAt?.let { DateLine(R.string.completed_at, it) }
            StatusChip(
                status = project.status,
                onStatusChange = onStatusChange,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
    // Card(onClick) has no long-click, so clip to the card shape and add the gestures ourselves.
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier
            .clip(CardDefaults.shape)
            .combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Card(modifier = modifier.fillMaxWidth().then(clickModifier), content = content)
}

@Composable
private fun DateLine(@StringRes label: Int, epochMillis: Long) {
    Text(
        text = stringResource(label, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StatusChip(
    status: ProjectStatus,
    onStatusChange: (ProjectStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(stringResource(status.labelRes())) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProjectStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes())) },
                    onClick = {
                        expanded = false
                        onStatusChange(option)
                    },
                )
            }
        }
    }
}

@StringRes
private fun ProjectStatus.labelRes(): Int = when (this) {
    ProjectStatus.Created -> R.string.status_created
    ProjectStatus.InProgress -> R.string.status_in_progress
    ProjectStatus.Finished -> R.string.status_finished
}

@Preview(showBackground = true)
@Composable
private fun ProjectsContentPreview() {
    KnittingTheme {
        ProjectsContent(
            projects = listOf(
                Project(1L, "Winter scarf", "Merino wool, garter stitch"),
                Project(2L, "Baby hat", null),
            ),
            onAddClick = {},
            onProjectClick = {},
            onProjectLongClick = {},
            onStatusChange = { _, _ -> },
        )
    }
}
