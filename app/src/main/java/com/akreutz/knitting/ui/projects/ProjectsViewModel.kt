package com.akreutz.knitting.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.knitting.data.KnittingDatabase
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import com.akreutz.knitting.data.Step
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
        targetRows = targetRows,
        stitchCount = stitchCount,
        method = method,
        needleSize = needleSize,
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

    fun setStepProgress(step: Step, progress: Int) {
        val max = step.progressTarget() ?: return
        viewModelScope.launch { dao.updateStepProgress(step.id, progress.coerceIn(0, max)) }
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
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.updateDetails(project.id, trimmedName, description.trim().ifEmpty { null })
            removedSteps.forEach { dao.deleteStep(it) }
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
