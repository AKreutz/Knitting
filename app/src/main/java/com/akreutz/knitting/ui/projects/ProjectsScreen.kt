package com.akreutz.knitting.ui.projects

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
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
        onCopyGrid = viewModel::copyPatternGrid,
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
            onConfirm = { name, description, addedSteps, removedSteps, editedSteps ->
                viewModel.editProject(projectToEdit, name, description, addedSteps, removedSteps, editedSteps)
                projectToEditId = null
            },
        )
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, addedSteps, _, _ ->
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
    onCopyGrid: (Step, Step) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The FAB shrinks to an icon once the list scrolls away from the top.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        if (projects.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Checkroom,
                title = stringResource(R.string.projects_empty),
                hint = stringResource(R.string.projects_empty_hint),
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
                        onCopyGrid = onCopyGrid,
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
            shape = FloatingActionButtonDefaults.extendedFabShape,
            // Two-toned like the current-step panel: the split brush replaces the flat container color.
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            // The button's own shadow would show through its transparent container as a pale box, so it is
            // flat and the shadow is drawn here, behind the brush.
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg)
                .shadow(6.dp, FloatingActionButtonDefaults.extendedFabShape)
                .background(
                    diagonalSplitBrush(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    FloatingActionButtonDefaults.extendedFabShape,
                ),
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
    onPatternCellsChange: ((Step, String) -> Unit)? = null,
    onCopyGrid: ((target: Step, source: Step) -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        // The strip's color mirrors the status chip so a card's progress reads at a glance. It is
        // drawn behind the content rather than measured, so it follows the card while it animates.
        val accentColor = project.status.accentColor()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(accentColor, size = Size(Spacing.accentStrip.toPx(), size.height))
                }
                .padding(start = Spacing.accentStrip + Spacing.lg, top = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (onEditClick != null && project.status != ProjectStatus.Finished) {
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
                    StepTypeStrip(steps, modifier = Modifier.weight(1f))
                    Text(
                        text = pluralStringResource(R.plurals.steps_count, steps.size, steps.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                    ExpandChevron(expanded)
                }
                AnimatedVisibility(visible = expanded) {
                    // Hung off a trunk line that starts at the summary line's left edge, like a file tree.
                    Column {
                        steps.forEachIndexed { index, step ->
                            StepRow(
                                step = step,
                                // Counting happens on the in-progress screen, not here.
                                inProgress = false,
                                onProgressChange = null,
                                onPatternCellsChange = onPatternCellsChange,
                                gridEditable = true,
                                copyGridSources = steps.filter { it.id != step.id && it.hasGrid() },
                                onCopyGrid = onCopyGrid,
                                branch = stepBranchAt(index, steps.size),
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

/**
 * The type icons of [steps] in order, as many as the available width holds. When they do not all
 * fit, room is kept at the end for a "+N" with the number of steps left out.
 */
@Composable
private fun StepTypeStrip(steps: List<Step>, modifier: Modifier = Modifier) {
    val iconSize = 24.dp
    val gap = Spacing.sm
    val labelStyle = MaterialTheme.typography.labelLarge
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        // Width of [count] icons followed by the "+N" for the rest, with a gap between each item.
        fun widthWithLabel(count: Int): Dp {
            val label = with(density) { textMeasurer.measure("+${steps.size - count}", labelStyle).size.width.toDp() }
            return (iconSize + gap) * count + label
        }
        val allFit = (iconSize + gap) * steps.size - gap <= maxWidth
        val shown = if (allFit) {
            steps.size
        } else {
            (steps.size - 1 downTo 1).firstOrNull { widthWithLabel(it) <= maxWidth } ?: 0
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.take(shown).forEach { DuotoneIcon(it.type.icon(), iconSize = iconSize) }
            if (shown < steps.size) {
                Text(
                    text = "+${steps.size - shown}",
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            onCopyGrid = { _, _ -> },
        )
    }
}
