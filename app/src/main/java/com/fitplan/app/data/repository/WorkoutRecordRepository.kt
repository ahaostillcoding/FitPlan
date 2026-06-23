package com.fitplan.app.data.repository

import androidx.room.withTransaction
import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.domain.mapper.toDomain
import com.fitplan.app.domain.mapper.toEntity
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.domain.model.WorkoutRecordExercise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface WorkoutRecordRepository {
    fun observeRecords(): Flow<List<WorkoutRecord>>
    fun observeRecord(recordId: Long): Flow<WorkoutRecord?>
    fun observeRecordsBetween(startMillis: Long, endMillis: Long): Flow<List<WorkoutRecord>>
    fun observeRecordCountBetween(startMillis: Long, endMillis: Long): Flow<Int>
    fun observeLastRecord(): Flow<WorkoutRecord?>
    suspend fun saveRecord(record: WorkoutRecord): Result<Long>
    suspend fun deleteRecord(recordId: Long): Result<Unit>
}

class DefaultWorkoutRecordRepository(
    private val database: FitPlanDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : WorkoutRecordRepository {
    private val recordDao = database.workoutRecordDao()

    override fun observeRecords(): Flow<List<WorkoutRecord>> {
        return recordDao.observeRecords().map { records ->
            records.map { it.toDomain() }
        }
    }

    override fun observeRecord(recordId: Long): Flow<WorkoutRecord?> {
        return recordDao.observeRecordWithExercises(recordId).map { it?.toDomain() }
    }

    override fun observeRecordsBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<WorkoutRecord>> {
        return recordDao.observeRecordsBetween(startMillis, endMillis).map { records ->
            records.map { it.toDomain() }
        }
    }

    override fun observeRecordCountBetween(
        startMillis: Long,
        endMillis: Long
    ): Flow<Int> {
        return recordDao.observeRecordCountBetween(startMillis, endMillis)
    }

    override fun observeLastRecord(): Flow<WorkoutRecord?> {
        return recordDao.observeLastRecord().map { it?.toDomain() }
    }

    override suspend fun saveRecord(record: WorkoutRecord): Result<Long> {
        return withContext(ioDispatcher) {
            runCatching {
                validateRecord(record)
                val now = clock()
                database.withTransaction {
                    val recordId = recordDao.insertRecord(
                        record.toEntity(
                            id = 0,
                            createdAt = now
                        )
                    )
                    val exerciseEntities = record.exercises
                        .sortedBy { it.sortOrder }
                        .mapIndexed { index, exercise ->
                            exercise.copy(sortOrder = normalizedSortOrder(exercise, index))
                                .toEntity(recordId)
                        }
                    if (exerciseEntities.isNotEmpty()) {
                        recordDao.insertRecordExercises(exerciseEntities)
                    }
                    recordId
                }
            }
        }
    }

    override suspend fun deleteRecord(recordId: Long): Result<Unit> {
        return withContext(ioDispatcher) {
            runCatching {
                require(recordId > 0) { "Record id is required for delete." }
                recordDao.deleteRecordById(recordId)
            }
        }
    }

    private fun validateRecord(record: WorkoutRecord) {
        require(record.planName.isNotBlank()) { "Plan name cannot be empty." }
        require(record.workoutDayName.isNotBlank()) { "Workout day name cannot be empty." }
        require(record.date > 0) { "Workout date is required." }
        require(record.durationMinutes >= 0) { "Workout duration cannot be negative." }
        record.exercises.forEach { validateRecordExercise(it) }
    }

    private fun validateRecordExercise(exercise: WorkoutRecordExercise) {
        require(exercise.name.isNotBlank()) { "Exercise name cannot be empty." }
        require(exercise.bodyPart.isNotBlank()) { "Exercise body part cannot be empty." }
        require(exercise.sets > 0) { "Exercise sets must be greater than 0." }
        require(exercise.reps.isNotBlank()) { "Exercise reps cannot be empty." }
        require(exercise.restSeconds >= 0) { "Rest seconds cannot be negative." }
    }

    private fun normalizedSortOrder(exercise: WorkoutRecordExercise, fallback: Int): Int {
        return exercise.sortOrder.takeIf { it >= 0 } ?: fallback
    }
}

