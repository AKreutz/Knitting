package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.knitting.R
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.ui.theme.KnittingTheme
import com.akreutz.knitting.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun ProjectsScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = viewModel(),
) {
    val projects by viewModel.projects.collectAsState()
    val stepsByProject by viewModel.stepsByProject.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var projectToStartId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToStart = projects.firstOrNull { it.id == projectToStartId }
    var projectToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToDelete = projects.firstOrNull { it.id == projectToDeleteId }
    var projectToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToEdit = projects.firstOrNull { it.id == projectToEditId }

    ProjectsContent(
        projects = projects,
        stepsByProject = stepsByProject,
        onAddClick = { showAddDialog = true },
        onProjectStartClick = { projectToStartId = it.id },
        onProjectEditClick = { projectToEditId = it.id },
        onPatternCellsChange = viewModel::setPatternCells,
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

    if (projectToEdit != null) {
        AddProjectDialog(
            initialName = projectToEdit.name,
            initialDescription = projectToEdit.description.orEmpty(),
            existingSteps = stepsByProject[projectToEdit.id].orEmpty(),
            titleRes = R.string.edit_project_title,
            confirmRes = R.string.save,
            onDismiss = { projectToEditId = null },
            onDelete = {
                projectToDeleteId = projectToEdit.id
                projectToEditId = null
            },
            onConfirm = { name, description, addedSteps, removedSteps ->
                viewModel.editProject(projectToEdit, name, description, addedSteps, removedSteps)
                projectToEditId = null
            },
        )
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, addedSteps, _ ->
                viewModel.addProject(name, description, addedSteps)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ProjectsContent(
    projects: List<Project>,
    stepsByProject: Map<Long, List<Step>>,
    onAddClick: () -> Unit,
    onProjectStartClick: (Project) -> Unit,
    onProjectEditClick: (Project) -> Unit,
    onPatternCellsChange: (Step, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The FAB shrinks to an icon once the list scrolls away from the top.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

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
                state = listState,
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    top = Spacing.lg,
                    end = Spacing.lg,
                    bottom = Spacing.fabClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        steps = stepsByProject[project.id].orEmpty(),
                        modifier = Modifier.animateItem(),
                        onPatternCellsChange = onPatternCellsChange,
                        onStartClick = { onProjectStartClick(project) },
                        onEditClick = { onProjectEditClick(project) },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddClick,
            expanded = fabExpanded,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_project)) },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg),
        )
    }
}

@Composable
internal fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    steps: List<Step> = emptyList(),
    onStartClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onStepProgressChange: ((Step, Int) -> Unit)? = null,
    onPatternCellsChange: ((Step, String) -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // The strip's color mirrors the status chip so a card's progress reads at a glance.
            Box(
                modifier = Modifier
                    .width(Spacing.accentStrip)
                    .fillMaxHeight()
                    .background(project.status.accentColor()),
            )
            Column(modifier = Modifier.weight(1f).padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (onEditClick != null && project.status == ProjectStatus.Created) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_project_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    StatusChip(status = project.status)
                }
                project.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                MetaLine(project)
                if (steps.isNotEmpty()) {
                    var expanded by rememberSaveable(project.id) { mutableStateOf(false) }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.steps_count, steps.size, steps.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.collapse_step else R.string.expand_step,
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            steps.forEach { step ->
                                StepRow(
                                    step = step,
                                    inProgress = project.status == ProjectStatus.InProgress,
                                    onProgressChange = onStepProgressChange,
                                    onPatternCellsChange = onPatternCellsChange,
                                    modifier = Modifier.padding(top = Spacing.sm),
                                )
                            }
                        }
                    }
                }
                if (onStartClick != null && project.status == ProjectStatus.Created) {
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                    ) {
                        Text(stringResource(R.string.start_project_action))
                    }
                }
            }
        }
    }
}

/** One-line summary under the title: dates and the running day for in-progress projects. */
@Composable
private fun MetaLine(project: Project) {
    val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val startedAt = project.startedAt
    val dayCount = if (project.status == ProjectStatus.InProgress && startedAt != null) {
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startedAt).toInt() + 1
        stringResource(R.string.day_count, days)
    } else {
        null
    }
    val parts = listOfNotNull(
        startedAt?.let { stringResource(R.string.started_at, formatter.format(Date(it))) },
        project.completedAt?.let { stringResource(R.string.completed_at, formatter.format(Date(it))) },
        dayCount,
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun StatusChip(
    status: ProjectStatus,
    modifier: Modifier = Modifier,
) {
    // Created stays outlined; the later statuses are filled so progress reads at a glance.
    val (container, content) = when (status) {
        ProjectStatus.Created -> Color.Transparent to MaterialTheme.colorScheme.onSurface
        ProjectStatus.InProgress -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ProjectStatus.Finished -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    }
    Surface(
        modifier = modifier,
        shape = AssistChipDefaults.shape,
        color = container,
        contentColor = content,
        border = if (status == ProjectStatus.Created) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
    ) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}

@Composable
internal fun ProjectStatus.accentColor(): Color = when (this) {
    ProjectStatus.Created -> MaterialTheme.colorScheme.outlineVariant
    ProjectStatus.InProgress -> MaterialTheme.colorScheme.secondary
    ProjectStatus.Finished -> MaterialTheme.colorScheme.primary
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
            stepsByProject = mapOf(1L to listOf(Step(1L, 1L, "Edge", StepType.CastOn, stitchCount = 60, method = "Long-tail", needleSize = "4.0 mm"))),
            onAddClick = {},
            onProjectStartClick = {},
            onProjectEditClick = {},
            onPatternCellsChange = { _, _ -> },
        )
    }
}
