package com.akreutz.knitting.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/** A step's counters after a change. */
data class StepCounter(val progress: Int, val patternRow: Int)

/**
 * The counters after setting [progress] by hand, or null for steps without a counter.
 * Changing a pattern's repeat count restarts the row count within the repeat; centimeters are independent of the rows.
 */
fun Step.counterAfterSettingProgress(progress: Int): StepCounter? {
    val max = progressTarget() ?: return null
    val clamped = progress.coerceIn(0, max)
    return StepCounter(clamped, if (usesPatternRows()) 0 else patternRow)
}

/**
 * The counters after moving a pattern step [delta] rows, or null if it is not a pattern step.
 * Finishing a repeat's last row completes that repeat.
 */
fun Step.counterAfterPatternRowStep(delta: Int): StepCounter? {
    val rows = patternRows ?: return null
    if (trackInCm) {
        // The length is counted in centimeters by hand, so the rows just cycle through the grid.
        return StepCounter(progress, Math.floorMod(patternRow + delta, rows))
    }
    val repeats = progressTarget() ?: return null
    // Linear position over all rows of all repeats keeps the wrap-around arithmetic in one place.
    val position = (progress * rows + patternRow + delta).coerceIn(0, repeats * rows)
    return StepCounter(position / rows, position % rows)
}

private fun Step.usesPatternRows() = type == StepType.Pattern && !trackInCm

/**
 * The counters after one tap of a remote control (the watch) in direction [delta], or null for steps without a counter.
 * A pattern step moves one row; any other step moves like the + and - buttons of its counter in the app do.
 */
fun Step.counterAfterStepping(delta: Int): StepCounter? =
    if (type == StepType.Pattern) counterAfterPatternRowStep(delta)
    else counterAfterSettingProgress(if (delta > 0) progress + increment() else progress - decrement())

/** The step a remote control works on, together with its project. */
data class CurrentCounter(val project: Project, val step: Step)

/**
 * The first unfinished counted step of the most recent in-progress project, or its last counted step once all are done;
 * null when no project is in progress or none of its steps has a counter.
 */
fun currentCounter(projects: List<Project>, steps: List<Step>): CurrentCounter? {
    val project = projects.firstOrNull { it.status == ProjectStatus.InProgress } ?: return null
    val counted = steps.filter { it.projectId == project.id && it.completedFraction() != null }
    val step = counted.firstOrNull { (it.completedFraction() ?: 1f) < 1f } ?: counted.lastOrNull() ?: return null
    return CurrentCounter(project, step)
}

/**
 * The step a tap in direction [delta] changes: the current step, or, when counting back from the very start of it,
 * the counted step before it, which then becomes the current one again. Null when there is nothing to change.
 */
fun tapTarget(projects: List<Project>, steps: List<Step>, delta: Int): Step? {
    val current = currentCounter(projects, steps)?.step ?: return null
    if (delta >= 0 || current.progress != 0 || current.patternRow != 0) return current
    val counted = steps.filter { it.projectId == current.projectId && it.completedFraction() != null }
    return counted.getOrNull(counted.indexOfFirst { it.id == current.id } - 1)
}

/** Changes step counters in the database; shared by the screens and anything else that drives the counters. */
class CounterRepository(private val dao: ProjectDao) {
    /** The step a remote control works on right now; emits again whenever it or its counters change. */
    val current: Flow<CurrentCounter?> =
        combine(dao.observeAll(), dao.observeAllSteps(), ::currentCounter).distinctUntilChanged()

    /** Moves the current step one tap in direction [delta], reading the database fresh so a stale caller can't misfire. */
    suspend fun stepCurrent(delta: Int) {
        val step = tapTarget(dao.observeAll().first(), dao.observeAllSteps().first(), delta) ?: return
        val counter = step.counterAfterStepping(delta) ?: return
        dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
    }

    suspend fun setProgress(step: Step, progress: Int) {
        val counter = step.counterAfterSettingProgress(progress) ?: return
        if (step.usesPatternRows()) dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
        else dao.updateStepProgress(step.id, counter.progress)
    }

    suspend fun stepPatternRow(step: Step, delta: Int) {
        val counter = step.counterAfterPatternRowStep(delta) ?: return
        dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
    }
}
