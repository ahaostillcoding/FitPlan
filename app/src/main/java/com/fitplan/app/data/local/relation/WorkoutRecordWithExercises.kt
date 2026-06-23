package com.fitplan.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fitplan.app.data.local.entity.WorkoutRecordEntity
import com.fitplan.app.data.local.entity.WorkoutRecordExerciseEntity

data class WorkoutRecordWithExercises(
    @Embedded
    val record: WorkoutRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recordId"
    )
    val exercises: List<WorkoutRecordExerciseEntity>
)

