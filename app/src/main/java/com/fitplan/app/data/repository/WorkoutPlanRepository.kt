package com.fitplan.app.data.repository

import androidx.room.withTransaction
import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.domain.mapper.toDomain
import com.fitplan.app.domain.mapper.toEntity
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface WorkoutPlanRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    fun observeActivePlan(): Flow<WorkoutPlan?>
    fun observePlan(planId: Long): Flow<WorkoutPlan?>
    suspend fun getPlan(planId: Long): Result<WorkoutPlan?>
    suspend fun savePlan(plan: WorkoutPlan): Result<Long>
    suspend fun updatePlan(plan: WorkoutPlan): Result<Unit>
    suspend fun deletePlan(planId: Long): Result<Unit>
    suspend fun duplicatePlan(planId: Long): Result<Long>
    suspend fun setActivePlan(planId: Long): Result<Unit>
}

class DefaultWorkoutPlanRepository(
    private val database: FitPlanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : WorkoutPlanRepository {
    private val planDao = database.workoutPlanDao()

    override fun observePlans(): Flow<List<WorkoutPlan>> {
        return planDao.observePlans().map { plans ->
            plans.map { it.toDomain() }
        }
    }

    override fun observeActivePlan(): Flow<WorkoutPlan?> {
        return planDao.observeActivePlanWithDays().map { it?.toDomain() }
    }

    override fun observePlan(planId: Long): Flow<WorkoutPlan?> {
        return planDao.observePlanWithDays(planId).map { it?.toDomain() }
    }

    override suspend fun getPlan(planId: Long): Result<WorkoutPlan?> {
        return withContext(ioDispatcher) {
            runCatching { planDao.getPlanWithDays(planId)?.toDomain() }
        }
    }

    override suspend fun savePlan(plan: WorkoutPlan): Result<Long> {
        return withContext(ioDispatcher) {
            runCatching {
                validatePlan(plan)
                val now = clock()
                database.withTransaction {
                    val planId = planDao.insertPlan(
                        plan.toEntity(
                            id = 0,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    insertPlanContent(planId, plan.days)
                    if (plan.isActive) {
                        planDao.setActivePlan(planId, now)
                    }
                    planId
                }
            }
        }
    }

    override suspend fun updatePlan(plan: WorkoutPlan): Result<Unit> {
        return withContext(ioDispatcher) {
            runCatching {
                require(plan.id > 0) { "Plan id is required for update." }
                validatePlan(plan)
                val now = clock()
                database.withTransaction {
                    val existing = planDao.getPlan(plan.id)
                        ?: error("Workout plan does not exist.")
                    planDao.updatePlan(
                        plan.toEntity(
                            id = existing.id,
                            createdAt = existing.createdAt,
                            updatedAt = now
                        )
                    )
                    planDao.deleteDaysByPlanId(existing.id)
                    insertPlanContent(existing.id, plan.days)
                    if (plan.isActive) {
                        planDao.setActivePlan(existing.id, now)
                    }
                }
            }
        }
    }

    override suspend fun deletePlan(planId: Long): Result<Unit> {
        return withContext(ioDispatcher) {
            runCatching {
                require(planId > 0) { "Plan id is required for delete." }
                planDao.deletePlanById(planId)
            }
        }
    }

    override suspend fun duplicatePlan(planId: Long): Result<Long> {
        return withContext(ioDispatcher) {
            runCatching {
                val source = planDao.getPlanWithDays(planId)?.toDomain()
                    ?: error("Workout plan does not exist.")
                val now = clock()
                val copy = source.copy(
                    id = 0,
                    name = "${source.name} Copy",
                    isActive = false,
                    createdAt = now,
                    updatedAt = now,
                    days = source.days.map { day ->
                        day.copy(
                            id = 0,
                            planId = 0,
                            exercises = day.exercises.map { exercise ->
                                exercise.copy(id = 0, workoutDayId = 0)
                            }
                        )
                    }
                )
                database.withTransaction {
                    val newPlanId = planDao.insertPlan(copy.toEntity(createdAt = now, updatedAt = now))
                    insertPlanContent(newPlanId, copy.days)
                    newPlanId
                }
            }
        }
    }

    override suspend fun setActivePlan(planId: Long): Result<Unit> {
        return withContext(ioDispatcher) {
            runCatching {
                require(planId > 0) { "Plan id is required." }
                val now = clock()
                database.withTransaction {
                    planDao.getPlan(planId) ?: error("Workout plan does not exist.")
                    planDao.setActivePlan(planId, now)
                }
            }
        }
    }

    private suspend fun insertPlanContent(planId: Long, days: List<WorkoutDay>) {
        days.sortedBy { it.sortOrder }.forEachIndexed { dayIndex, day ->
            val dayId = planDao.insertDay(
                day.copy(sortOrder = day.sortOrder.takeIf { it >= 0 } ?: dayIndex)
                    .toEntity(planId)
            )
            val exercises = day.exercises
                .sortedBy { it.sortOrder }
                .mapIndexed { exerciseIndex, exercise ->
                    exercise.copy(sortOrder = normalizedSortOrder(exercise, exerciseIndex))
                        .toEntity(dayId)
                }
            if (exercises.isNotEmpty()) {
                planDao.insertExercises(exercises)
            }
        }
    }

    private fun validatePlan(plan: WorkoutPlan) {
        require(plan.name.isNotBlank()) { "Plan name cannot be empty." }
        require(plan.goal.isNotBlank()) { "Workout goal cannot be empty." }
        require(plan.frequencyPerWeek in 1..7) { "Weekly frequency must be between 1 and 7." }
        require(plan.estimatedDurationMinutes > 0) { "Workout duration must be greater than 0." }
        plan.days.forEach { day ->
            require(day.dayName.isNotBlank()) { "Workout day name cannot be empty." }
            day.exercises.forEach { exercise ->
                validateExercise(exercise)
            }
        }
    }

    private fun validateExercise(exercise: Exercise) {
        require(exercise.name.isNotBlank()) { "Exercise name cannot be empty." }
        require(exercise.bodyPart.isNotBlank()) { "Exercise body part cannot be empty." }
        require(exercise.sets > 0) { "Exercise sets must be greater than 0." }
        require(exercise.reps.isNotBlank()) { "Exercise reps cannot be empty." }
        require(exercise.restSeconds >= 0) { "Rest seconds cannot be negative." }
    }

    private fun normalizedSortOrder(exercise: Exercise, fallback: Int): Int {
        return exercise.sortOrder.takeIf { it >= 0 } ?: fallback
    }
}

