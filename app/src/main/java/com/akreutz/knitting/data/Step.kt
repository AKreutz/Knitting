package com.akreutz.knitting.data

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
    /**
     * Pattern: one character per cell, row by row, holding the index of the painted color
     * (see PatternPalette; '0' is empty). Null until the user paints something.
     */
    val patternCells: String? = null,
    /** Cast-on stitches, increases, decreases or stockinette rows done so far, tracked while in progress. */
    val progress: Int = 0,
)

/** What [Step.progress] counts up to, or null for step types that have no counter. */
fun Step.progressTarget(): Int? = when (type) {
    StepType.CastOn -> stitchCount
    StepType.Increases, StepType.Decreases -> shapingCount
    StepType.Stockinette -> targetRows
    else -> null
}
