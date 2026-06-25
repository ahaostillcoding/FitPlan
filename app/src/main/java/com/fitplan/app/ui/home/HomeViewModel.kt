package com.fitplan.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val isEmpty: Boolean = true
) {
    val todayDay: WorkoutDay?
        get() = activePlan?.days
            ?.firstOrNull { it.id == selectedWorkoutDayId }
            ?: activePlan?.days?.firstOrNull()
}

class HomeViewModel(
    planRepository: WorkoutPlanRepository,
    recordRepository: WorkoutRecordRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val weekRange = currentWeekRange()
    private val recent7DayRange = recent7DayRange()

    val uiState: StateFlow<HomeUiState> = combine(
        planRepository.observeActivePlan(),
        recordRepository.observeRecordCountBetween(weekRange.first, weekRange.second),
        recordRepository.observeRecordsBetween(recent7DayRange.first, recent7DayRange.second),
        recordRepository.observeLastRecord(),
        settingsRepository.settings
    ) { activePlan, weeklyCount, recentRecords, lastRecord, settings ->
        val selectedDayId = settings.selectedWorkoutDayId
            ?.takeIf { dayId -> activePlan?.days?.any { it.id == dayId } == true }
        HomeUiState(
            activePlan = activePlan,
            selectedWorkoutDayId = selectedDayId,
            weeklyWorkoutCount = weeklyCount,
            recent7DayWorkoutCount = recentRecords.size,
            recent7DayDurationMinutes = recentRecords.sumOf { it.durationMinutes },
            lastWorkoutAt = lastRecord?.date,
            isEmpty = activePlan == null
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
