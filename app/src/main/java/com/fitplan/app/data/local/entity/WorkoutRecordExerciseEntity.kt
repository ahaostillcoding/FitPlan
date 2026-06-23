package com.fitplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_record_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordId"]),
        Index(value = ["recordId", "sortOrder"])
    ]
)
data class WorkoutRecordExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val originalExerciseId: Long?,
    val name: String,
    val bodyPart: String,
    val sets: Int,
    val reps: String,
    val weight: String,
    val restSeconds: Int,
    val plannedNotes: String,
    val isCompleted: Boolean,
    val actualNotes: String,
    val sortOrder: Int
)

