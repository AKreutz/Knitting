package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
    var projectToAddStepId by rememberSaveable { mutableStateOf<Long?>(null) }
    val projectToAddStep = projects.firstOrNull { it.id == projectToAddStepId }
    var stepToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val stepToDelete = stepsByProject.values.flatten().firstOrNull { it.id == stepToDeleteId }
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
        onProjectClick = { projectToStartId = it.id },
        onProjectLongClick = { projectToDeleteId = it.id },
        onProjectEditClick = { projectToEditId = it.id },
        onAddStepClick = { projectToAddStepId = it.id },
        onPatternCellsChange = viewModel::setPatternCells,
        onStepLongClick = { stepToDeleteId = it.id },
        modifier = modifier,
    )

    if (stepToDelete != null) {
        DeleteStepDialog(
            stepName = stepToDelete.name,
            onDismiss = { stepToDeleteId = null },
            onConfirm = {
                viewModel.deleteStep(stepToDelete)
                stepToDeleteId = null
            },
        )
    }

    if (projectToAddStep != null) {
        AddStepDialog(
            stepNumber = (stepsByProject[projectToAddStep.id]?.size ?: 0) + 1,
            onDismiss = { projectToAddStepId = null },
            onConfirm = { newStep ->
                viewModel.addStep(projectToAddStep, newStep)
                projectToAddStepId = null
            },
        )
    }

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
            titleRes = R.string.edit_project_title,
            confirmRes = R.string.save,
            onDismiss = { projectToEditId = null },
            onConfirm = { name, description ->
                viewModel.editProject(projectToEdit, name, description)
                projectToEditId = null
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
    stepsByProject: Map<Long, List<Step>>,
    onAddClick: () -> Unit,
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit,
    onProjectEditClick: (Project) -> Unit,
    onAddStepClick: (Project) -> Unit,
    onPatternCellsChange: (Step, String) -> Unit,
    onStepLongClick: (Step) -> Unit,
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
                        onStepLongClick = onStepLongClick,
                        onAddStepClick = if (project.status == ProjectStatus.Created) {
                            { onAddStepClick(project) }
                        } else {
                            null
                        },
                        onClick = if (project.status == ProjectStatus.Created) {
                            { onProjectClick(project) }
                        } else {
                            null
                        },
                        onLongClick = { onProjectLongClick(project) },
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
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onAddStepClick: (() -> Unit)? = null,
    onStepProgressChange: ((Step, Int) -> Unit)? = null,
    onPatternCellsChange: ((Step, String) -> Unit)? = null,
    onStepLongClick: ((Step) -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    // Card(onClick) has no long-click, so clip to the card shape and add the gestures ourselves.
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier
            .clip(shape)
            .combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Card(modifier = modifier.fillMaxWidth().then(clickModifier), shape = shape) {
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
                    StatusChip(status = project.status, modifier = Modifier.padding(end = Spacing.xs))
                    if (onEditClick != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_project_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                project.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                DatesLine(project)
                steps.forEach { step ->
                    StepRow(
                        step = step,
                        inProgress = project.status == ProjectStatus.InProgress,
                        onProgressChange = onStepProgressChange,
                        onPatternCellsChange = onPatternCellsChange,
                        onLongClick = onStepLongClick,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
                if (onAddStepClick != null) {
                    TextButton(onClick = onAddStepClick, modifier = Modifier.padding(top = Spacing.xs)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.add_step))
                    }
                }
                if (project.status == ProjectStatus.InProgress) {
                    DayCount(project)
                }
            }
        }
    }
}

@Composable
private fun DatesLine(project: Project) {
    val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val parts = listOfNotNull(
        project.startedAt?.let { stringResource(R.string.started_at, formatter.format(Date(it))) },
        project.completedAt?.let { stringResource(R.string.completed_at, formatter.format(Date(it))) },
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

/** How many days the project has been in progress, shown on in-progress cards. */
@Composable
private fun DayCount(project: Project) {
    val startedAt = project.startedAt ?: return
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startedAt).toInt() + 1
    Text(
        text = stringResource(R.string.day_count, days),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs),
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
            onProjectClick = {},
            onProjectLongClick = {},
            onProjectEditClick = {},
            onAddStepClick = {},
            onPatternCellsChange = { _, _ -> },
            onStepLongClick = {},
        )
    }
}
