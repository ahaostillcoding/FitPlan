package com.fitplan.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    val activePlan: WorkoutPlan? = null,
    val weeklyWorkoutCount: Int = 0,
    val lastWorkoutAt: Long? = null,
    val isEmpty: Boolean = true
)

class HomeViewModel(
    planRepository: WorkoutPlanRepository,
    recordRepository: WorkoutRecordRepository
) : ViewModel() {
    private val weekRange = currentWeekRange()

    val uiState: StateFlow<HomeUiState> = combine(
        planRepository.observeActivePlan(),
        recordRepository.observeRecordCountBetween(weekRange.first, weekRange.second),
        recordRepository.observeLastRecord()
    ) { activePlan, weeklyCount, lastRecord ->
        HomeUiState(
            activePlan = activePlan,
            weeklyWorkoutCount = weeklyCount,
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

    companion object {
        fun factory(
            planRepository: WorkoutPlanRepository,
            recordRepository: WorkoutRecordRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(planRepository, recordRepository) as T
            }
        }
    }
}

