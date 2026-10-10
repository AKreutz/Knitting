package com.akreutz.knitting

import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepCounter
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.counterAfterPatternRowStep
import com.akreutz.knitting.data.counterAfterSettingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterRepositoryTest {
    private fun pattern(progress: Int = 0, patternRow: Int = 0, trackInCm: Boolean = false) = Step(
        projectId = 1,
        name = "Pattern",
        type = StepType.Pattern,
        patternRows = 4,
        patternRepeats = 3,
        trackInCm = trackInCm,
        targetMm = 100,
        progress = progress,
        patternRow = patternRow,
    )

    private fun plainRows(progress: Int = 0) = Step(
        projectId = 1, name = "Body", type = StepType.PlainRows, targetRows = 10, progress = progress,
    )

    @Test
    fun settingProgressClampsToTarget() {
        assertEquals(StepCounter(10, 0), plainRows().counterAfterSettingProgress(99))
        assertEquals(StepCounter(0, 0), plainRows(5).counterAfterSettingProgress(-3))
    }

    @Test
    fun settingPatternRepeatsRestartsRowCount() {
        assertEquals(StepCounter(2, 0), pattern(progress = 1, patternRow = 3).counterAfterSettingProgress(2))
    }

    @Test
    fun settingCentimetersKeepsPatternRow() {
        val step = pattern(patternRow = 2, trackInCm = true)
        assertEquals(StepCounter(30, 2), step.counterAfterSettingProgress(30))
    }

    @Test
    fun lastRowOfRepeatCompletesIt() {
        assertEquals(StepCounter(2, 0), pattern(progress = 1, patternRow = 3).counterAfterPatternRowStep(1))
    }

    @Test
    fun steppingBackCrossesIntoPreviousRepeat() {
        assertEquals(StepCounter(0, 3), pattern(progress = 1, patternRow = 0).counterAfterPatternRowStep(-1))
    }

    @Test
    fun rowsStopAtTheEnds() {
        assertEquals(StepCounter(0, 0), pattern().counterAfterPatternRowStep(-1))
        assertEquals(StepCounter(3, 0), pattern(progress = 3).counterAfterPatternRowStep(1))
    }

    @Test
    fun rowsCycleWhenTrackingCentimeters() {
        val step = pattern(progress = 20, patternRow = 3, trackInCm = true)
        assertEquals(StepCounter(20, 0), step.counterAfterPatternRowStep(1))
        assertEquals(StepCounter(20, 3), pattern(progress = 20, trackInCm = true).counterAfterPatternRowStep(-1))
    }

    @Test
    fun nonPatternStepsHaveNoRowStep() {
        assertNull(plainRows().counterAfterPatternRowStep(1))
    }
}
