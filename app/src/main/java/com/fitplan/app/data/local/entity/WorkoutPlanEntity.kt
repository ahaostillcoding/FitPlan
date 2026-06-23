package com.fitplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_plans",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["updatedAt"])
    ]
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val goal: String,
    val frequencyPerWeek: Int,
    val estimatedDurationMinutes: Int,
    val notes: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

