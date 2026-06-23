package com.fitplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutDayId"]),
        Index(value = ["workoutDayId", "sortOrder"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val name: String,
    val bodyPart: String,
    val sets: Int,
    val reps: String,
    val weight: String,
    val restSeconds: Int,
    val notes: String,
    val sortOrder: Int
)

