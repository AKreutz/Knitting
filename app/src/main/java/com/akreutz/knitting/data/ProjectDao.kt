package com.akreutz.knitting.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM steps ORDER BY id")
    fun observeAllSteps(): Flow<List<Step>>

    @Insert
    suspend fun insert(project: Project): Long

    @Insert
    suspend fun insertStep(step: Step): Long

    @Update
    suspend fun updateStep(step: Step)

    @Delete
    suspend fun deleteStep(step: Step)

    @Query("UPDATE steps SET patternCells = :patternCells WHERE id = :id")
    suspend fun updatePatternCells(id: Long, patternCells: String)

    @Query("UPDATE steps SET progress = :progress WHERE id = :id")
    suspend fun updateStepProgress(id: Long, progress: Int)

    @Query("UPDATE steps SET progress = :progress, patternRow = :patternRow WHERE id = :id")
    suspend fun updatePatternProgress(id: Long, progress: Int, patternRow: Int)

    @Query("UPDATE steps SET progress = 0, patternRow = 0 WHERE projectId = :projectId")
    suspend fun resetStepProgress(projectId: Long)

    @Delete
    suspend fun delete(project: Project)

    @Query("UPDATE projects SET name = :name, description = :description WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, description: String?)

    @Query(
        "UPDATE projects SET status = :status, startedAt = :startedAt, completedAt = :completedAt WHERE id = :id",
    )
    suspend fun updateStatus(id: Long, status: ProjectStatus, startedAt: Long?, completedAt: Long?)
}
