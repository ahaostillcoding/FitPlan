package com.fitplan.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fitplan.app.data.local.entity.ExerciseEntity
import com.fitplan.app.data.local.entity.WorkoutDayEntity

data class WorkoutDayWithExercises(
    @Embedded
    val day: WorkoutDayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutDayId"
    )
    val exercises: List<ExerciseEntity>
)

