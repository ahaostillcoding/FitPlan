package com.fitplan.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.data.repository.AppSettings
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    val activePlan: WorkoutPlan? = null,
    val selectedWorkoutDayId: Long? = null,
    val weeklyWorkoutCount: Int = 0,
    val recent7DayWorkoutCount: Int = 0,
    val recent7DayDurationMinutes: Int = 0,
    val lastWorkoutAt: Long? = null,
    val message: String? = null,
    val errorMessage: String? = null,
    val isCreatingSamplePlan: Boolean = false,
    val isEmpty: Boolean = true
) {
    val todayDay: WorkoutDay?
        get() = activePlan?.days
            ?.firstOrNull { it.id == selectedWorkoutDayId }
            ?: activePlan?.days?.firstOrNull()
}

class HomeViewModel(
    private val planRepository: WorkoutPlanRepository,
    recordRepository: WorkoutRecordRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val weekRange = currentWeekRange()
    private val recent7DayRange = recent7DayRange()
    private val samplePlanState = MutableStateFlow(SamplePlanState())

    private val baseState = combine(
        planRepository.observeActivePlan(),
        recordRepository.observeRecordCountBetween(weekRange.first, weekRange.second),
        recordRepository.observeRecordsBetween(recent7DayRange.first, recent7DayRange.second),
        recordRepository.observeLastRecord(),
        settingsRepository.settings
    ) { activePlan, weeklyCount, recentRecords, lastRecord, settings ->
        HomeBaseState(
            activePlan = activePlan,
            weeklyWorkoutCount = weeklyCount,
            recentRecords = recentRecords,
            lastRecord = lastRecord,
            settings = settings
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(baseState, samplePlanState) { base, sampleState ->
        val selectedDayId = base.settings.selectedWorkoutDayId
            ?.takeIf { dayId -> base.activePlan?.days?.any { it.id == dayId } == true }
        HomeUiState(
            activePlan = base.activePlan,
            selectedWorkoutDayId = selectedDayId,
            weeklyWorkoutCount = base.weeklyWorkoutCount,
            recent7DayWorkoutCount = base.recentRecords.size,
            recent7DayDurationMinutes = base.recentRecords.sumOf { it.durationMinutes },
            lastWorkoutAt = base.lastRecord?.date,
            message = sampleState.message,
            errorMessage = sampleState.errorMessage,
            isCreatingSamplePlan = sampleState.isCreatingSamplePlan,
            isEmpty = base.activePlan == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private fun currentWeekRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val end = LocalDate.now(zone)
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return start to end
    }

    private fun recent7DayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone)
            .minusDays(6)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val end = LocalDate.now(zone)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return start to end
    }

    fun selectWorkoutDay(dayId: Long) {
        viewModelScope.launch {
            settingsRepository.saveSelectedWorkoutDayId(dayId)
        }
    }

    fun createSamplePlan() {
        viewModelScope.launch {
            samplePlanState.update { it.copy(isCreatingSamplePlan = true, message = null, errorMessage = null) }
            planRepository.savePlan(sampleWorkoutPlan())
                .onSuccess {
                    samplePlanState.update {
                        it.copy(
                            isCreatingSamplePlan = false,
                            message = "示例计划已创建，并设为当前计划"
                        )
                    }
                }
                .onFailure { error ->
                    samplePlanState.update {
                        it.copy(
                            isCreatingSamplePlan = false,
                            errorMessage = error.message ?: "创建示例计划失败"
                        )
                    }
                }
        }
    }

    private fun sampleWorkoutPlan(): WorkoutPlan {
        return WorkoutPlan(
            name = "三天新手快速开始",
            goal = "新手入门",
            frequencyPerWeek = 3,
            estimatedDurationMinutes = 45,
            notes = "适合首次体验 FitPlan，可按需编辑动作和训练日。",
            isActive = true,
            days = listOf(
                WorkoutDay(
                    dayName = "Day 1 全身基础",
                    sortOrder = 0,
                    exercises = listOf(
                        Exercise(name = "徒手深蹲", bodyPart = "腿臀", sets = 3, reps = "12-15", restSeconds = 60, sortOrder = 0),
                        Exercise(name = "俯卧撑", bodyPart = "胸", sets = 3, reps = "8-12", restSeconds = 60, sortOrder = 1),
                        Exercise(name = "平板支撑", bodyPart = "核心", sets = 3, reps = "30-45 秒", restSeconds = 45, sortOrder = 2)
                    )
                ),
                WorkoutDay(
                    dayName = "Day 2 上肢激活",
                    sortOrder = 1,
                    exercises = listOf(
                        Exercise(name = "哑铃划船", bodyPart = "背", sets = 3, reps = "10-12", weight = "自选", restSeconds = 75, sortOrder = 0),
                        Exercise(name = "哑铃肩推", bodyPart = "肩", sets = 3, reps = "8-10", weight = "自选", restSeconds = 75, sortOrder = 1),
                        Exercise(name = "哑铃弯举", bodyPart = "手臂", sets = 2, reps = "12-15", weight = "自选", restSeconds = 60, sortOrder = 2)
                    )
                ),
                WorkoutDay(
                    dayName = "Day 3 低强度恢复",
                    sortOrder = 2,
                    exercises = listOf(
                        Exercise(name = "快走或跑步机", bodyPart = "心肺", sets = 1, reps = "20 分钟", restSeconds = 0, sortOrder = 0),
                        Exercise(name = "臀桥", bodyPart = "臀腿", sets = 3, reps = "12-15", restSeconds = 60, sortOrder = 1),
                        Exercise(name = "拉伸放松", bodyPart = "全身", sets = 1, reps = "8-10 分钟", restSeconds = 0, sortOrder = 2)
                    )
                )
            )
        )
    }

    private data class SamplePlanState(
        val isCreatingSamplePlan: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null
    )

    private data class HomeBaseState(
        val activePlan: WorkoutPlan?,
        val weeklyWorkoutCount: Int,
        val recentRecords: List<WorkoutRecord>,
        val lastRecord: WorkoutRecord?,
        val settings: AppSettings
    )

    companion object {
        fun factory(
            planRepository: WorkoutPlanRepository,
            recordRepository: WorkoutRecordRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(planRepository, recordRepository, settingsRepository) as T
            }
        }
    }
}
