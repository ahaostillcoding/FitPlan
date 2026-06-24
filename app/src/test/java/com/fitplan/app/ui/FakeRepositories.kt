package com.fitplan.app.ui

import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutPlanRepository(
    initialPlans: List<WorkoutPlan> = emptyList()
) : WorkoutPlanRepository {
    private val plans = MutableStateFlow(initialPlans)
    var savedPlan: WorkoutPlan? = null
    val currentPlans: List<WorkoutPlan> get() = plans.value

    override fun observePlans(): Flow<List<WorkoutPlan>> = plans

    override fun observeActivePlan(): Flow<WorkoutPlan?> {
        return plans.map { it.firstOrNull { plan -> plan.isActive } }
    }

    override fun observePlan(planId: Long): Flow<WorkoutPlan?> {
        return plans.map { it.firstOrNull { plan -> plan.id == planId } }
    }

    override suspend fun getPlan(planId: Long): Result<WorkoutPlan?> {
        return Result.success(plans.value.firstOrNull { it.id == planId })
    }

    override suspend fun savePlan(plan: WorkoutPlan): Result<Long> {
        savedPlan = plan
        val id = 100L
        plans.value = plans.value + plan.copy(id = id)
        return Result.success(id)
    }

    override suspend fun updatePlan(plan: WorkoutPlan): Result<Unit> {
        savedPlan = plan
        plans.value = plans.value.map { if (it.id == plan.id) plan else it }
        return Result.success(Unit)
    }

    override suspend fun deletePlan(planId: Long): Result<Unit> {
        plans.value = plans.value.filterNot { it.id == planId }
        return Result.success(Unit)
    }

    override suspend fun duplicatePlan(planId: Long): Result<Long> {
        val source = plans.value.first { it.id == planId }
        val copy = source.copy(id = 200L, name = "${source.name} Copy", isActive = false)
        plans.value = plans.value + copy
        return Result.success(copy.id)
    }

    override suspend fun setActivePlan(planId: Long): Result<Unit> {
        plans.value = plans.value.map { it.copy(isActive = it.id == planId) }
        return Result.success(Unit)
    }
}

class FakeWorkoutRecordRepository(
    initialRecords: List<WorkoutRecord> = emptyList()
) : WorkoutRecordRepository {
    private val records = MutableStateFlow(initialRecords)
    var savedRecord: WorkoutRecord? = null
    val currentRecords: List<WorkoutRecord> get() = records.value

    override fun observeRecords(): Flow<List<WorkoutRecord>> = records

    override fun observeRecord(recordId: Long): Flow<WorkoutRecord?> {
        return records.map { it.firstOrNull { record -> record.id == recordId } }
    }

    override fun observeRecordsBetween(startMillis: Long, endMillis: Long): Flow<List<WorkoutRecord>> {
        return records.map { list -> list.filter { it.date >= startMillis && it.date < endMillis } }
    }

    override fun observeRecordCountBetween(startMillis: Long, endMillis: Long): Flow<Int> {
        return observeRecordsBetween(startMillis, endMillis).map { it.size }
    }

    override fun observeLastRecord(): Flow<WorkoutRecord?> {
        return records.map { it.maxByOrNull { record -> record.date } }
    }

    override suspend fun saveRecord(record: WorkoutRecord): Result<Long> {
        savedRecord = record
        val id = 300L
        records.value = records.value + record.copy(id = id)
        return Result.success(id)
    }

    override suspend fun deleteRecord(recordId: Long): Result<Unit> {
        records.value = records.value.filterNot { it.id == recordId }
        return Result.success(Unit)
    }
}
