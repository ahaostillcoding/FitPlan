package com.fitplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_records",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["workoutDayId"]),
        Index(value = ["date"])
    ]
)
data class WorkoutRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long?,
    val workoutDayId: Long?,
    val planName: String,
    val workoutDayName: String,
    val date: Long,
    val durationMinutes: Int,
    val notes: String,
    val createdAt: Long
)

