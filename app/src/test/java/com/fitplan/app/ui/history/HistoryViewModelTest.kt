package com.fitplan.app.ui.history

import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.FakeWorkoutRecordRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun delete_removesRecordAndShowsMessage() = runTest {
        val repository = FakeWorkoutRecordRepository(
            initialRecords = listOf(
                WorkoutRecord(
                    id = 1,
                    planName = "四天增肌计划",
                    workoutDayName = "Day 1",
                    date = 1_000L,
                    durationMinutes = 60
                )
            )
        )
        val viewModel = HistoryViewModel(repository)

        viewModel.delete(1)
        advanceUntilIdle()

        assertEquals("训练记录已删除", viewModel.message.value)
        assertFalse(repository.currentRecords.any { it.id == 1L })
    }

    @Test
    fun selectFilter_thisWeekShowsOnlyCurrentWeekRecords() = runTest {
        val repository = FakeWorkoutRecordRepository(
            initialRecords = listOf(
                sampleRecord(id = 1, date = daysAgo(0)),
                sampleRecord(id = 2, date = daysAgo(40))
            )
        )
        val viewModel = HistoryViewModel(repository)
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectFilter(HistoryFilter.THIS_WEEK)
        advanceUntilIdle()

        assertEquals(HistoryFilter.THIS_WEEK, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(1L), viewModel.uiState.value.records.map { it.id })

        collectJob.cancel()
    }

    @Test
    fun selectFilter_thisMonthShowsOnlyCurrentMonthRecords() = runTest {
        val repository = FakeWorkoutRecordRepository(
            initialRecords = listOf(
                sampleRecord(id = 1, date = daysAgo(0)),
                sampleRecord(id = 2, date = daysAgo(40))
            )
        )
        val viewModel = HistoryViewModel(repository)
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectFilter(HistoryFilter.THIS_MONTH)
        advanceUntilIdle()

        assertEquals(HistoryFilter.THIS_MONTH, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(1L), viewModel.uiState.value.records.map { it.id })

        collectJob.cancel()
    }

    private fun sampleRecord(id: Long, date: Long): WorkoutRecord {
        return WorkoutRecord(
            id = id,
            planName = "四天增肌计划",
            workoutDayName = "Day 1",
            date = date,
            durationMinutes = 60
        )
    }

    private fun daysAgo(days: Long): Long {
        return LocalDate.now(ZoneId.systemDefault())
            .minusDays(days)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
