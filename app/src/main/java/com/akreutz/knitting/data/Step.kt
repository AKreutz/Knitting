package com.akreutz.knitting.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One step of a project. Which of the optional fields are used depends on [type]. */
@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class Step(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val type: StepType,
    /** The yarn color used for this step, e.g. "Forest green"; applies to every step type. */
    val color: String? = null,
    /** Number of rows this step should reach; unused by cast-on steps. */
    val targetRows: Int? = null,
    /** Cast-on: number of stitches to cast on. */
    val stitchCount: Int? = null,
    /** Cast-on: the cast-on method, e.g. "Long-tail". */
    val method: String? = null,
    /** Cast-on: the needle size, e.g. "4.0 mm". */
    val needleSize: String? = null,
    /** Increases and decreases: number of them to make. */
    val shapingCount: Int? = null,
    /** Increases and decreases: where they go, e.g. "every 4th row". */
    val pattern: String? = null,
    /** Pattern: what kind of pattern the grid describes. */
    val patternType: PatternType? = null,
    /** Pattern: grid size. */
    val patternRows: Int? = null,
    val patternColumns: Int? = null,
    /** Pattern: how many repeats of the pattern need to be knitted. */
    val patternRepeats: Int? = null,
    /**
     * Pattern: one character per cell, row by row, holding the index of the painted color
     * (see PatternPalette; '0' is empty). Null until the user paints something.
     */
    val patternCells: String? = null,
    /** Plain rows: the stitch sequence worked. */
    val rowPattern: RowPattern? = null,
    /** Special: free-text description of what to do. */
    val description: String? = null,
    /** Plain rows and pattern: whether the section's length is tracked in centimeters instead of rows or repeats. */
    @ColumnInfo(defaultValue = "0")
    val trackInCm: Boolean = false,
    /** Plain rows and pattern: length in millimeters (so 2.5 cm is 25) this step should reach when [trackInCm] is set. */
    val targetMm: Int? = null,
    /**
     * Cast-on stitches, increases, decreases, plain rows or pattern repeats done so far, tracked while in
     * progress; millimeters when [trackInCm] is set.
     */
    val progress: Int = 0,
    /** Pattern: rows of the current repeat already knitted, tracked while in progress. */
    val patternRow: Int = 0,
)

const val MM_PER_CM = 10

/** What [Step.progress] counts up to, or null for step types that have no counter. */
fun Step.progressTarget(): Int? = when (type) {
    StepType.CastOn -> stitchCount
    StepType.Increases, StepType.Decreases -> shapingCount
    StepType.PlainRows -> if (trackInCm) targetMm else targetRows
    StepType.Pattern -> if (trackInCm) targetMm else patternRepeats
    // A special step is checked off: 0 = open, 1 = done.
    StepType.Special -> 1
}
