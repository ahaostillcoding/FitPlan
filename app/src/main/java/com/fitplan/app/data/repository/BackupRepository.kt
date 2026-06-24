package com.fitplan.app.data.repository

import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.domain.mapper.toDomain
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FitPlanBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val plans: List<WorkoutPlan> = emptyList(),
    val records: List<WorkoutRecord> = emptyList()
)

data class BackupPreview(
    val planCount: Int,
    val recordCount: Int
)

interface BackupRepository {
    suspend fun exportBackupJson(): Result<String>
    fun previewImport(json: String): Result<BackupPreview>
    suspend fun importBackupJson(json: String): Result<BackupPreview>
}

class DefaultBackupRepository(
    private val database: FitPlanDatabase,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutRecordRepository: WorkoutRecordRepository,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupRepository {
    override suspend fun exportBackupJson(): Result<String> {
        return withContext(ioDispatcher) {
            runCatching {
                val plans = database.workoutPlanDao()
                    .getAllPlansWithDays()
                    .map { it.toDomain().forBackup() }
                val records = database.workoutRecordDao()
                    .getAllRecordsWithExercises()
                    .map { it.toDomain().forBackup() }
                gson.toJson(FitPlanBackup(plans = plans, records = records))
            }
        }
    }

    override fun previewImport(json: String): Result<BackupPreview> {
        return runCatching {
            val backup = parseBackup(json)
            BackupPreview(
                planCount = backup.plans.size,
                recordCount = backup.records.size
            )
        }
    }

    override suspend fun importBackupJson(json: String): Result<BackupPreview> {
        return withContext(ioDispatcher) {
            runCatching {
                val backup = parseBackup(json)
                backup.plans.forEach { plan ->
                    workoutPlanRepository.savePlan(plan.forBackup())
                        .getOrThrow()
                }
                backup.records.forEach { record ->
                    workoutRecordRepository.saveRecord(record.forBackup())
                        .getOrThrow()
                }
                BackupPreview(
                    planCount = backup.plans.size,
                    recordCount = backup.records.size
                )
            }
        }
    }

    private fun parseBackup(json: String): FitPlanBackup {
        require(json.isNotBlank()) { "请先粘贴备份 JSON" }
        val backup = runCatching {
            gson.fromJson(json, FitPlanBackup::class.java)
        }.getOrElse { error ->
            throw IllegalArgumentException("备份 JSON 解析失败", error)
        } ?: throw IllegalArgumentException("备份内容为空")
        require(backup.version == 1) { "不支持的备份版本：${backup.version}" }
        backup.plans.forEach { validatePlan(it) }
        backup.records.forEach { validateRecord(it) }
        return backup
    }

    private fun validatePlan(plan: WorkoutPlan) {
        require(plan.name.isNotBlank()) { "备份中存在空名称计划" }
        require(plan.goal.isNotBlank()) { "${plan.name} 缺少训练目标" }
        require(plan.frequencyPerWeek in 1..7) { "${plan.name} 每周次数不合法" }
        require(plan.estimatedDurationMinutes > 0) { "${plan.name} 训练时长不合法" }
    }

    private fun validateRecord(record: WorkoutRecord) {
        require(record.planName.isNotBlank()) { "备份中存在空计划名记录" }
        require(record.workoutDayName.isNotBlank()) { "${record.planName} 缺少训练日名称" }
        require(record.date > 0) { "${record.planName} 缺少训练日期" }
    }

    private fun WorkoutPlan.forBackup(): WorkoutPlan {
        return copy(
            id = 0,
            isActive = false,
            createdAt = 0,
            updatedAt = 0,
            days = days.mapIndexed { dayIndex, day ->
                day.copy(
                    id = 0,
                    planId = 0,
                    sortOrder = dayIndex,
                    exercises = day.exercises.mapIndexed { exerciseIndex, exercise ->
                        exercise.copy(
                            id = 0,
                            workoutDayId = 0,
                            sortOrder = exerciseIndex
                        )
                    }
                )
            }
        )
    }

    private fun WorkoutRecord.forBackup(): WorkoutRecord {
        return copy(
            id = 0,
            planId = null,
            workoutDayId = null,
            createdAt = 0,
            exercises = exercises.mapIndexed { index, exercise ->
                exercise.copy(
                    id = 0,
                    recordId = 0,
                    sortOrder = index
                )
            }
        )
    }
}
