package com.akreutz.knitting.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/** A step's counters after a change. */
data class StepCounter(val progress: Int, val patternRow: Int)

/**
 * The counters after setting [progress] by hand, or null for steps without a counter.
 * Changing a pattern's repeat count or a shaping step's count restarts the row or round count; centimeters are
 * independent of the rows.
 */
fun Step.counterAfterSettingProgress(progress: Int): StepCounter? {
    val max = progressTarget() ?: return null
    val clamped = progress.coerceIn(0, max)
    return StepCounter(clamped, if (usesPatternRows() || usesShapingRounds()) 0 else patternRow)
}

/**
 * The counters after knitting one round ([delta] = 1) of an increases or decreases step, or taking one back (-1);
 * null if the step has no interval. The last round of an interval is the one that makes the increase or decrease.
 */
fun Step.counterAfterShapingRound(delta: Int): StepCounter? {
    if (!usesShapingRounds()) return null
    val interval = shapingInterval ?: return null
    val count = progressTarget() ?: return null
    return when {
        delta > 0 && progress >= count -> StepCounter(progress, 0)
        delta > 0 -> if (patternRow + 1 >= interval) StepCounter(progress + 1, 0) else StepCounter(progress, patternRow + 1)
        patternRow > 0 -> StepCounter(progress, patternRow - 1)
        progress > 0 -> StepCounter(progress - 1, interval - 1)
        else -> StepCounter(0, 0)
    }
}

/** The counters after one row or round in direction [delta] on a pattern or shaping step, or null for any other. */
fun Step.counterAfterRowStep(delta: Int): StepCounter? =
    if (type == StepType.Pattern) counterAfterPatternRowStep(delta) else counterAfterShapingRound(delta)

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
 * A pattern step moves one row and a shaping step with an interval one round; any other step moves like the + and -
 * buttons of its counter in the app do.
 */
fun Step.counterAfterStepping(delta: Int): StepCounter? =
    if (type == StepType.Pattern || usesShapingRounds()) counterAfterRowStep(delta)
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

/** The project a remote control works on with all its counted steps, and the step that is current among them. */
data class ActiveProject(val project: Project, val steps: List<Step>, val current: Step?)

/** The most recent in-progress project, or null when there is none; see [currentCounter] for which step is current. */
fun activeProject(projects: List<Project>, steps: List<Step>): ActiveProject? {
    val project = projects.firstOrNull { it.status == ProjectStatus.InProgress } ?: return null
    val counted = steps.filter { it.projectId == project.id && it.completedFraction() != null }
    return ActiveProject(project, counted, currentCounter(projects, steps)?.step)
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

    /** The active project as a remote control shows it, read fresh from the database. */
    suspend fun active(): ActiveProject? = activeProject(dao.observeAll().first(), dao.observeAllSteps().first())

    /** Moves the step [stepId] of the active project one tap in direction [delta]; other steps are left alone. */
    suspend fun stepStep(stepId: Long, delta: Int) {
        val step = active()?.steps?.firstOrNull { it.id == stepId } ?: return
        val counter = step.counterAfterStepping(delta) ?: return
        dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
    }

    suspend fun setProgress(step: Step, progress: Int) {
        val counter = step.counterAfterSettingProgress(progress) ?: return
        if (step.usesPatternRows() || step.usesShapingRounds()) dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
        else dao.updateStepProgress(step.id, counter.progress)
    }

    /** Moves a pattern step one row, or a shaping step one round. */
    suspend fun stepPatternRow(step: Step, delta: Int) {
        val counter = step.counterAfterRowStep(delta) ?: return
        dao.updatePatternProgress(step.id, counter.progress, counter.patternRow)
    }
}
