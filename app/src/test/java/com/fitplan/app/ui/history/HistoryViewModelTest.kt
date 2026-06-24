package com.fitplan.app.ui.history

import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.FakeWorkoutRecordRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

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
}
