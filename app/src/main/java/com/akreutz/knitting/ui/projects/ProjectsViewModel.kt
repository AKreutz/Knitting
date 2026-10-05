package com.akreutz.knitting.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.knitting.data.KnittingDatabase
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.progressTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = KnittingDatabase.get(application).projectDao()

    val projects: StateFlow<List<Project>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stepsByProject: StateFlow<Map<Long, List<Step>>> = dao.observeAllSteps()
        .map { steps -> steps.groupBy { it.projectId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private fun NewStep.toStep(projectId: Long) = Step(
        projectId = projectId,
        name = name.trim(),
        type = type,
        color = color,
        targetRows = targetRows,
        stitchCount = stitchCount,
        method = method,
        needleSize = needleSize,
        description = description,
        shapingCount = shapingCount,
        pattern = pattern,
        patternType = patternType,
        rowPattern = rowPattern,
        patternRows = patternRows,
        patternColumns = patternColumns,
        patternRepeats = patternRepeats,
    )

    fun setPatternCells(step: Step, cells: String) {
        viewModelScope.launch { dao.updatePatternCells(step.id, cells) }
    }

    /** Gives [target] its own copy of [source]'s grid: type, size and painted cells. */
    fun copyPatternGrid(target: Step, source: Step) {
        val rows = source.patternRows ?: return
        val columns = source.patternColumns ?: return
        viewModelScope.launch {
            dao.updatePatternGrid(target.id, source.patternType, rows, columns, source.patternCells)
        }
    }

    fun setStepProgress(step: Step, progress: Int) {
        val max = step.progressTarget() ?: return
        val clamped = progress.coerceIn(0, max)
        viewModelScope.launch {
            // Changing the repeat count by hand restarts the row count within the repeat.
            if (step.type == StepType.Pattern) dao.updatePatternProgress(step.id, clamped, 0)
            else dao.updateStepProgress(step.id, clamped)
        }
    }

    /** Moves a pattern step one row forward or back; finishing a repeat's last row completes that repeat. */
    fun stepPatternRow(step: Step, delta: Int) {
        val rows = step.patternRows ?: return
        val repeats = step.progressTarget() ?: return
        // Linear position over all rows of all repeats keeps the wrap-around arithmetic in one place.
        val position = (step.progress * rows + step.patternRow + delta).coerceIn(0, repeats * rows)
        viewModelScope.launch { dao.updatePatternProgress(step.id, position / rows, position % rows) }
    }

    fun setStatus(project: Project, status: ProjectStatus) {
        // Starting stamps today's date once and finishing does the same for completedAt.
        // Resetting to Created clears both; reopening a finished project clears completedAt.
        val now = System.currentTimeMillis()
        val startedAt = when (status) {
            ProjectStatus.Created -> null
            ProjectStatus.InProgress -> project.startedAt ?: now
            ProjectStatus.Finished -> project.startedAt
        }
        val completedAt = if (status == ProjectStatus.Finished) project.completedAt ?: now else null
        viewModelScope.launch {
            dao.updateStatus(project.id, status, startedAt, completedAt)
            // A reset starts the project over, so its step counters go back to 0 too.
            if (status == ProjectStatus.Created) dao.resetStepProgress(project.id)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { dao.delete(project) }
    }

    fun editProject(
        project: Project,
        name: String,
        description: String,
        addedSteps: List<NewStep> = emptyList(),
        removedSteps: List<Step> = emptyList(),
        editedSteps: List<Pair<Step, NewStep>> = emptyList(),
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.updateDetails(project.id, trimmedName, description.trim().ifEmpty { null })
            removedSteps.forEach { dao.deleteStep(it) }
            editedSteps.forEach { (old, new) ->
                // The painted cells only stay valid while the grid keeps its type and size.
                val sameGrid = old.type == StepType.Pattern && new.type == StepType.Pattern &&
                    old.patternType == new.patternType &&
                    old.patternRows == new.patternRows &&
                    old.patternColumns == new.patternColumns
                val updated = new.toStep(project.id).copy(
                    id = old.id,
                    patternCells = old.patternCells.takeIf { sameGrid },
                )
                // Keep the counters, but never beyond the edited targets (e.g. fewer repeats than done).
                val progress = old.progress.coerceIn(0, updated.progressTarget() ?: 0)
                val patternRows = updated.patternRows
                val patternRow = if (updated.type == StepType.Pattern && patternRows != null && progress < (updated.progressTarget() ?: 0)) {
                    old.patternRow.coerceIn(0, patternRows - 1)
                } else {
                    0
                }
                dao.updateStep(updated.copy(progress = progress, patternRow = patternRow))
            }
            addedSteps.forEach { dao.insertStep(it.toStep(project.id)) }
        }
    }

    fun addProject(name: String, description: String, steps: List<NewStep> = emptyList()) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val projectId = dao.insert(
                Project(
                    name = trimmedName,
                    description = description.trim().ifEmpty { null },
                ),
            )
            steps.forEach { dao.insertStep(it.toStep(projectId)) }
        }
    }
}
