package com.akreutz.knitting.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun observeAll(): Flow<List<Project>>

    @Insert
    suspend fun insert(project: Project): Long

    @Delete
    suspend fun delete(project: Project)

    @Query("UPDATE projects SET name = :name, description = :description WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, description: String?)

    @Query(
        "UPDATE projects SET status = :status, startedAt = :startedAt, completedAt = :completedAt WHERE id = :id",
    )
    suspend fun updateStatus(id: Long, status: ProjectStatus, startedAt: Long?, completedAt: Long?)
}
