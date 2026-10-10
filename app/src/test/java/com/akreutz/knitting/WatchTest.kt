package com.akreutz.knitting

import com.akreutz.knitting.data.CurrentCounter
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepCounter
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.counterAfterStepping
import com.akreutz.knitting.data.currentCounter
import com.akreutz.knitting.data.tapTarget
import com.akreutz.knitting.watch.WatchCommand
import com.akreutz.knitting.watch.WatchCounterController
import com.akreutz.knitting.watch.WatchLink
import com.akreutz.knitting.watch.WatchProtocol
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchTest {
    private val project = Project(id = 1, name = "Scarf", description = null, status = ProjectStatus.InProgress)

    private fun rows(id: Long, progress: Int, target: Int = 10, projectId: Long = 1) = Step(
        id = id, projectId = projectId, name = "Rows $id", type = StepType.PlainRows, targetRows = target, progress = progress,
    )

    private fun pattern(progress: Int = 0, patternRow: Int = 0, trackInCm: Boolean = false) = Step(
        id = 9, projectId = 1, name = "Lace", type = StepType.Pattern, patternRows = 4, patternRepeats = 3,
        trackInCm = trackInCm, targetMm = 100, progress = progress, patternRow = patternRow,
    )

    @Test
    fun currentStepIsTheFirstUnfinishedOne() {
        val steps = listOf(rows(1, 10), rows(2, 3), rows(3, 0))
        assertEquals(2L, currentCounter(listOf(project), steps)?.step?.id)
    }

    @Test
    fun currentStepIsTheLastOneWhenAllAreDone() {
        val steps = listOf(rows(1, 10), rows(2, 10))
        assertEquals(2L, currentCounter(listOf(project), steps)?.step?.id)
    }

    @Test
    fun noCurrentStepWithoutAnInProgressProject() {
        val created = project.copy(status = ProjectStatus.Created)
        assertNull(currentCounter(listOf(created), listOf(rows(1, 0))))
        assertNull(currentCounter(emptyList(), emptyList()))
    }

    @Test
    fun currentStepIgnoresOtherProjects() {
        val steps = listOf(rows(1, 0, projectId = 2), rows(2, 4))
        assertEquals(2L, currentCounter(listOf(project), steps)?.step?.id)
    }

    @Test
    fun countingBackFromTheStartOfAStepWorksOnThePreviousStep() {
        val steps = listOf(rows(1, 10), rows(2, 0), rows(3, 0))
        assertEquals(1L, tapTarget(listOf(project), steps, -1)?.id)
        assertEquals(2L, tapTarget(listOf(project), steps, 1)?.id)
    }

    @Test
    fun countingBackWithinAStepStaysOnIt() {
        val steps = listOf(rows(1, 10), rows(2, 1))
        assertEquals(2L, tapTarget(listOf(project), steps, -1)?.id)
    }

    @Test
    fun countingBackFromTheFirstStepDoesNothing() {
        assertNull(tapTarget(listOf(project), listOf(rows(1, 0), rows(2, 0)), -1))
    }

    @Test
    fun countingBackFromARepeatsFirstRowStaysOnThePattern() {
        val steps = listOf(rows(1, 10), pattern(progress = 1, patternRow = 0))
        assertEquals(9L, tapTarget(listOf(project), steps, -1)?.id)
        val midRepeat = listOf(rows(1, 10), pattern(progress = 0, patternRow = 2))
        assertEquals(9L, tapTarget(listOf(project), midRepeat, -1)?.id)
    }

    @Test
    fun steppingMovesOneCountOrOneCentimeter() {
        assertEquals(StepCounter(4, 0), rows(1, 3).counterAfterStepping(1))
        assertEquals(StepCounter(0, 0), rows(1, 0).counterAfterStepping(-1))
        val cm = Step(projectId = 1, name = "Body", type = StepType.PlainRows, trackInCm = true, targetMm = 100, progress = 25)
        assertEquals(StepCounter(35, 0), cm.counterAfterStepping(1))
    }

    @Test
    fun steppingCastOnMovesTenStitchesLikeTheButtons() {
        fun castOn(progress: Int) = Step(
            projectId = 1, name = "Cast on", type = StepType.CastOn, stitchCount = 25, progress = progress,
        )
        assertEquals(StepCounter(10, 0), castOn(0).counterAfterStepping(1))
        assertEquals(StepCounter(25, 0), castOn(20).counterAfterStepping(1))
        assertEquals(StepCounter(20, 0), castOn(25).counterAfterStepping(-1))
        assertEquals(StepCounter(10, 0), castOn(20).counterAfterStepping(-1))
    }

    @Test
    fun steppingCentimetersRemovesAPartialCentimeterFirst() {
        fun body(progress: Int) = Step(
            projectId = 1, name = "Body", type = StepType.PlainRows, trackInCm = true, targetMm = 100, progress = progress,
        )
        assertEquals(StepCounter(20, 0), body(25).counterAfterStepping(-1))
        assertEquals(StepCounter(10, 0), body(20).counterAfterStepping(-1))
        assertEquals(StepCounter(100, 0), body(95).counterAfterStepping(1))
    }

    @Test
    fun steppingAPatternMovesOneRow() {
        assertEquals(StepCounter(1, 0), pattern(progress = 0, patternRow = 3).counterAfterStepping(1))
        assertEquals(StepCounter(0, 3), pattern(progress = 1).counterAfterStepping(-1))
    }

    @Test
    fun parsesCommandsAsTheSdkDelivers() {
        assertEquals(WatchCommand.Step(1), WatchProtocol.parseCommand(listOf(mapOf("cmd" to "inc"))))
        assertEquals(WatchCommand.Step(-1), WatchProtocol.parseCommand(mapOf("cmd" to "dec")))
        assertEquals(WatchCommand.Sync, WatchProtocol.parseCommand(listOf(mapOf("cmd" to "sync"))))
        assertNull(WatchProtocol.parseCommand(listOf(mapOf("cmd" to "format"))))
        assertNull(WatchProtocol.parseCommand("inc"))
        assertNull(WatchProtocol.parseCommand(null))
    }

    @Test
    fun encodesStateForTheWatch() {
        assertEquals(mapOf("idle" to 1), WatchProtocol.encode(null))
        assertEquals(
            mapOf("p" to "Scarf", "s" to "Rows 2", "t" to "PlainRows", "prog" to 3, "max" to 10, "inc" to 1, "dec" to 1),
            WatchProtocol.encode(CurrentCounter(project, rows(2, 3))),
        )
        assertEquals(
            mapOf(
                "p" to "Scarf", "s" to "Lace", "t" to "Pattern", "prog" to 1, "max" to 3, "inc" to 1, "dec" to 1,
                "row" to 2, "rows" to 4,
            ),
            WatchProtocol.encode(CurrentCounter(project, pattern(progress = 1, patternRow = 2))),
        )
        assertEquals(1, WatchProtocol.encode(CurrentCounter(project, pattern(trackInCm = true)))["cm"])
    }

    @Test
    fun encodesTheAmountOfEachTapLikeTheButtons() {
        val castOn = Step(projectId = 1, name = "Cast on", type = StepType.CastOn, stitchCount = 25, progress = 25)
        val encoded = WatchProtocol.encode(CurrentCounter(project, castOn))
        assertEquals(10, encoded["inc"])
        assertEquals(5, encoded["dec"])
        val cm = Step(projectId = 1, name = "Body", type = StepType.PlainRows, trackInCm = true, targetMm = 100, progress = 25)
        val encodedCm = WatchProtocol.encode(CurrentCounter(project, cm))
        assertEquals(10, encodedCm["inc"])
        assertEquals(5, encodedCm["dec"])
    }

    @Test
    fun longNamesAreCut() {
        val long = project.copy(name = "x".repeat(100))
        assertEquals(40, (WatchProtocol.encode(CurrentCounter(long, rows(1, 0)))["p"] as String).length)
    }

    private class FakeLink : WatchLink {
        val inbox = Channel<Any?>(Channel.UNLIMITED)
        val sent = Channel<Map<String, Any>>(Channel.UNLIMITED)
        override val incoming: Flow<Any?> = inbox.receiveAsFlow()
        override suspend fun send(message: Map<String, Any>) {
            sent.send(message)
        }
    }

    @Test
    fun controllerPushesChangesAndAppliesTaps() = runBlocking {
        val state = MutableStateFlow<CurrentCounter?>(CurrentCounter(project, rows(1, 3)))
        val taps = mutableListOf<Int>()
        val link = FakeLink()
        val job = launch {
            WatchCounterController(state, { taps += it }, link).run()
        }
        withTimeout(5_000) {
            assertEquals(3, link.sent.receive()["prog"])

            state.value = CurrentCounter(project, rows(1, 4))
            assertEquals(4, link.sent.receive()["prog"])

            link.inbox.send(listOf(mapOf("cmd" to "inc")))
            link.inbox.send(listOf(mapOf("cmd" to "dec")))
            link.inbox.send(listOf(mapOf("cmd" to "sync")))
            assertEquals(4, link.sent.receive()["prog"])
            assertEquals(listOf(1, -1), taps)
        }
        job.cancel()
    }
}
