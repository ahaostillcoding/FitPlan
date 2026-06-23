package com.fitplan.app.domain.model

data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    val goal: String,
    val frequencyPerWeek: Int,
    val estimatedDurationMinutes: Int,
    val notes: String = "",
    val isActive: Boolean = false,
    val days: List<WorkoutDay> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class WorkoutDay(
    val id: Long = 0,
    val planId: Long = 0,
    val dayName: String,
    val sortOrder: Int,
    val exercises: List<Exercise> = emptyList()
)

data class Exercise(
    val id: Long = 0,
    val workoutDayId: Long = 0,
    val name: String,
    val bodyPart: String,
    val sets: Int,
    val reps: String,
    val weight: String = "",
    val restSeconds: Int,
    val notes: String = "",
    val sortOrder: Int
)

data class WorkoutRecord(
    val id: Long = 0,
    val planId: Long? = null,
    val workoutDayId: Long? = null,
    val planName: String,
    val workoutDayName: String,
    val date: Long,
    val durationMinutes: Int,
    val notes: String = "",
    val exercises: List<WorkoutRecordExercise> = emptyList(),
    val createdAt: Long = 0
)

data class WorkoutRecordExercise(
    val id: Long = 0,
    val recordId: Long = 0,
    val originalExerciseId: Long? = null,
    val name: String,
    val bodyPart: String,
    val sets: Int,
    val reps: String,
    val weight: String = "",
    val restSeconds: Int,
    val plannedNotes: String = "",
    val isCompleted: Boolean,
    val actualNotes: String = "",
    val sortOrder: Int
)

