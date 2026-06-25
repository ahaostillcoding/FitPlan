package com.fitplan.app.ui.home

import com.fitplan.app.data.repository.AppSettings
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.FakeWorkoutRecordRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_summarizesRecentSevenDayRecords() = runTest {
        val viewModel = HomeViewModel(
            planRepository = FakeWorkoutPlanRepository(
                listOf(
                    WorkoutPlan(
                        id = 1,
                        name = "四天增肌计划",
                        goal = "增肌",
                        frequencyPerWeek = 4,
                        estimatedDurationMinutes = 60,
                        isActive = true
                    )
                )
            ),
            recordRepository = FakeWorkoutRecordRepository(
                listOf(
                    sampleRecord(id = 1, date = daysAgo(0), duration = 45),
                    sampleRecord(id = 2, date = daysAgo(3), duration = 60),
                    sampleRecord(id = 3, date = daysAgo(10), duration = 90)
                )
            ),
            settingsRepository = FakeSettingsRepository()
        )
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.recent7DayWorkoutCount)
        assertEquals(105, viewModel.uiState.value.recent7DayDurationMinutes)

        collectJob.cancel()
    }

    private fun sampleRecord(id: Long, date: Long, duration: Int): WorkoutRecord {
        return WorkoutRecord(
            id = id,
            planName = "四天增肌计划",
            workoutDayName = "Day 1",
            date = date,
            durationMinutes = duration
        )
    }

    private fun daysAgo(days: Long): Long {
        return LocalDate.now(ZoneId.systemDefault())
            .minusDays(days)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(
            AppSettings(deepSeekModel = DEFAULT_DEEPSEEK_MODEL)
        )

        override val settings: Flow<AppSettings> = state

        override suspend fun saveDeepSeekApiKey(apiKey: String) {
            state.value = state.value.copy(deepSeekApiKey = apiKey.trim())
        }

        override suspend fun saveDeepSeekModel(model: String) {
            state.value = state.value.copy(deepSeekModel = model)
        }

        override suspend fun clearDeepSeekApiKey() {
            state.value = state.value.copy(deepSeekApiKey = "")
        }

        override suspend fun saveSelectedWorkoutDayId(dayId: Long?) {
            state.value = state.value.copy(selectedWorkoutDayId = dayId)
        }

        override suspend fun saveWorkoutDraftJson(json: String) {
            state.value = state.value.copy(workoutDraftJson = json)
        }

        override suspend fun clearWorkoutDraft() {
            state.value = state.value.copy(workoutDraftJson = "")
        }
    }
}
