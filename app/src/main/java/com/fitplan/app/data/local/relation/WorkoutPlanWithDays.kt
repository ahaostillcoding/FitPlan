package com.fitplan.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fitplan.app.data.local.entity.WorkoutDayEntity
import com.fitplan.app.data.local.entity.WorkoutPlanEntity

data class WorkoutPlanWithDays(
    @Embedded
    val plan: WorkoutPlanEntity,
    @Relation(
        entity = WorkoutDayEntity::class,
        parentColumn = "id",
        entityColumn = "planId"
    )
    val days: List<WorkoutDayWithExercises>
)

