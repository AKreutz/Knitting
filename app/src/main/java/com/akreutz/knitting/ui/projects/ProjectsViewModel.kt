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

    fun addStep(project: Project, newStep: NewStep) {
        val trimmedName = newStep.name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.insertStep(
                Step(
                    projectId = project.id,
                    name = trimmedName,
                    type = newStep.type,
                    targetRows = newStep.targetRows,
                    stitchCount = newStep.stitchCount,
                    method = newStep.method,
                    needleSize = newStep.needleSize,
                    shapingCount = newStep.shapingCount,
                    pattern = newStep.pattern,
                    patternRows = newStep.patternRows,
                    patternColumns = newStep.patternColumns,
                ),
            )
        }
    }

    fun deleteStep(step: Step) {
        viewModelScope.launch { dao.deleteStep(step) }
    }

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
        viewModelScope.launch { dao.updateStatus(project.id, status, startedAt, completedAt) }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { dao.delete(project) }
    }

    fun editProject(project: Project, name: String, description: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.updateDetails(project.id, trimmedName, description.trim().ifEmpty { null })
        }
    }

    fun addProject(name: String, description: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            dao.insert(
                Project(
                    name = trimmedName,
                    description = description.trim().ifEmpty { null },
                ),
            )
        }
    }
}
