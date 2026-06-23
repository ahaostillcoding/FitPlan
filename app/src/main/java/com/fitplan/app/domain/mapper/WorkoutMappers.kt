package com.fitplan.app.domain.mapper

import com.fitplan.app.data.local.entity.ExerciseEntity
import com.fitplan.app.data.local.entity.WorkoutDayEntity
import com.fitplan.app.data.local.entity.WorkoutPlanEntity
import com.fitplan.app.data.local.entity.WorkoutRecordEntity
import com.fitplan.app.data.local.entity.WorkoutRecordExerciseEntity
import com.fitplan.app.data.local.relation.WorkoutDayWithExercises
import com.fitplan.app.data.local.relation.WorkoutPlanWithDays
import com.fitplan.app.data.local.relation.WorkoutRecordWithExercises
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.domain.model.WorkoutRecordExercise

fun WorkoutPlanEntity.toDomain(days: List<WorkoutDay> = emptyList()): WorkoutPlan {
    return WorkoutPlan(
        id = id,
        name = name,
        goal = goal,
        frequencyPerWeek = frequencyPerWeek,
        estimatedDurationMinutes = estimatedDurationMinutes,
        notes = notes,
        isActive = isActive,
        days = days,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WorkoutPlanWithDays.toDomain(): WorkoutPlan {
    return plan.toDomain(
        days = days
            .map { it.toDomain() }
            .sortedBy { it.sortOrder }
    )
}

fun WorkoutDayWithExercises.toDomain(): WorkoutDay {
    return day.toDomain(
        exercises = exercises
            .map { it.toDomain() }
            .sortedBy { it.sortOrder }
    )
}

fun WorkoutDayEntity.toDomain(exercises: List<Exercise> = emptyList()): WorkoutDay {
    return WorkoutDay(
        id = id,
        planId = planId,
        dayName = dayName,
        sortOrder = sortOrder,
        exercises = exercises
    )
}

fun ExerciseEntity.toDomain(): Exercise {
    return Exercise(
        id = id,
        workoutDayId = workoutDayId,
        name = name,
        bodyPart = bodyPart,
        sets = sets,
        reps = reps,
        weight = weight,
        restSeconds = restSeconds,
        notes = notes,
        sortOrder = sortOrder
    )
}

fun WorkoutPlan.toEntity(
    id: Long = this.id,
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
    isActive: Boolean = this.isActive
): WorkoutPlanEntity {
    return WorkoutPlanEntity(
        id = id,
        name = name.trim(),
        goal = goal.trim(),
        frequencyPerWeek = frequencyPerWeek,
        estimatedDurationMinutes = estimatedDurationMinutes,
        notes = notes.trim(),
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WorkoutDay.toEntity(planId: Long): WorkoutDayEntity {
    return WorkoutDayEntity(
        id = 0,
        planId = planId,
        dayName = dayName.trim(),
        sortOrder = sortOrder
    )
}

fun Exercise.toEntity(workoutDayId: Long): ExerciseEntity {
    return ExerciseEntity(
        id = 0,
        workoutDayId = workoutDayId,
        name = name.trim(),
        bodyPart = bodyPart.trim(),
        sets = sets,
        reps = reps.trim(),
        weight = weight.trim(),
        restSeconds = restSeconds,
        notes = notes.trim(),
        sortOrder = sortOrder
    )
}

fun WorkoutRecordEntity.toDomain(
    exercises: List<WorkoutRecordExercise> = emptyList()
): WorkoutRecord {
    return WorkoutRecord(
        id = id,
        planId = planId,
        workoutDayId = workoutDayId,
        planName = planName,
        workoutDayName = workoutDayName,
        date = date,
        durationMinutes = durationMinutes,
        notes = notes,
        exercises = exercises,
        createdAt = createdAt
    )
}

fun WorkoutRecordWithExercises.toDomain(): WorkoutRecord {
    return record.toDomain(
        exercises = exercises
            .map { it.toDomain() }
            .sortedBy { it.sortOrder }
    )
}

fun WorkoutRecordExerciseEntity.toDomain(): WorkoutRecordExercise {
    return WorkoutRecordExercise(
        id = id,
        recordId = recordId,
        originalExerciseId = originalExerciseId,
        name = name,
        bodyPart = bodyPart,
        sets = sets,
        reps = reps,
        weight = weight,
        restSeconds = restSeconds,
        plannedNotes = plannedNotes,
        isCompleted = isCompleted,
        actualNotes = actualNotes,
        sortOrder = sortOrder
    )
}

fun WorkoutRecord.toEntity(
    id: Long = this.id,
    createdAt: Long = this.createdAt
): WorkoutRecordEntity {
    return WorkoutRecordEntity(
        id = id,
        planId = planId,
        workoutDayId = workoutDayId,
        planName = planName.trim(),
        workoutDayName = workoutDayName.trim(),
        date = date,
        durationMinutes = durationMinutes,
        notes = notes.trim(),
        createdAt = createdAt
    )
}

fun WorkoutRecordExercise.toEntity(recordId: Long): WorkoutRecordExerciseEntity {
    return WorkoutRecordExerciseEntity(
        id = 0,
        recordId = recordId,
        originalExerciseId = originalExerciseId,
        name = name.trim(),
        bodyPart = bodyPart.trim(),
        sets = sets,
        reps = reps.trim(),
        weight = weight.trim(),
        restSeconds = restSeconds,
        plannedNotes = plannedNotes.trim(),
        isCompleted = isCompleted,
        actualNotes = actualNotes.trim(),
        sortOrder = sortOrder
    )
}

