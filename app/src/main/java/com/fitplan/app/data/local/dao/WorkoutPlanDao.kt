package com.fitplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fitplan.app.data.local.entity.ExerciseEntity
import com.fitplan.app.data.local.entity.WorkoutDayEntity
import com.fitplan.app.data.local.entity.WorkoutPlanEntity
import com.fitplan.app.data.local.relation.WorkoutPlanWithDays
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY updatedAt DESC")
    abstract fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId LIMIT 1")
    abstract fun observePlanWithDays(planId: Long): Flow<WorkoutPlanWithDays?>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId LIMIT 1")
    abstract suspend fun getPlanWithDays(planId: Long): WorkoutPlanWithDays?

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE isActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    abstract fun observeActivePlanWithDays(): Flow<WorkoutPlanWithDays?>

    @Query("SELECT * FROM workout_plans WHERE id = :planId LIMIT 1")
    abstract suspend fun getPlan(planId: Long): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    abstract suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    abstract suspend fun deletePlanById(planId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertDay(day: WorkoutDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM workout_days WHERE planId = :planId")
    abstract suspend fun deleteDaysByPlanId(planId: Long)

    @Query("UPDATE workout_plans SET isActive = 0, updatedAt = :updatedAt")
    protected abstract suspend fun clearActivePlans(updatedAt: Long)

    @Query("UPDATE workout_plans SET isActive = 1, updatedAt = :updatedAt WHERE id = :planId")
    protected abstract suspend fun activatePlan(planId: Long, updatedAt: Long)

    @Transaction
    open suspend fun setActivePlan(planId: Long, updatedAt: Long) {
        clearActivePlans(updatedAt)
        activatePlan(planId, updatedAt)
    }
}

