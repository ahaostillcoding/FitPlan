package com.fitplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fitplan.app.data.local.entity.WorkoutRecordEntity
import com.fitplan.app.data.local.entity.WorkoutRecordExerciseEntity
import com.fitplan.app.data.local.relation.WorkoutRecordWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {
    @Query("SELECT * FROM workout_records ORDER BY date DESC, id DESC")
    fun observeRecords(): Flow<List<WorkoutRecordEntity>>

    @Transaction
    @Query("SELECT * FROM workout_records WHERE id = :recordId LIMIT 1")
    fun observeRecordWithExercises(recordId: Long): Flow<WorkoutRecordWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_records WHERE id = :recordId LIMIT 1")
    suspend fun getRecordWithExercises(recordId: Long): WorkoutRecordWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_records ORDER BY date DESC, id DESC")
    suspend fun getAllRecordsWithExercises(): List<WorkoutRecordWithExercises>

    @Query("SELECT * FROM workout_records WHERE date >= :startMillis AND date < :endMillis ORDER BY date DESC, id DESC")
    fun observeRecordsBetween(startMillis: Long, endMillis: Long): Flow<List<WorkoutRecordEntity>>

    @Query("SELECT COUNT(*) FROM workout_records WHERE date >= :startMillis AND date < :endMillis")
    fun observeRecordCountBetween(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT * FROM workout_records ORDER BY date DESC, id DESC LIMIT 1")
    fun observeLastRecord(): Flow<WorkoutRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: WorkoutRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecordExercises(exercises: List<WorkoutRecordExerciseEntity>)

    @Query("DELETE FROM workout_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Long)
}
