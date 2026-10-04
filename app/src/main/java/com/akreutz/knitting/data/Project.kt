package com.akreutz.knitting.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val status: ProjectStatus = ProjectStatus.Created,
    /** Epoch millis of when the project was started; null while it is still Created. */
    val startedAt: Long? = null,
    /** Epoch millis of when the project was finished; null unless its status is Finished. */
    val completedAt: Long? = null,
)
