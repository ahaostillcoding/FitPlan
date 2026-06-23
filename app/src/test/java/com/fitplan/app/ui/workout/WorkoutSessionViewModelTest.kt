package com.fitplan.app.ui.workout

import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.FakeWorkoutRecordRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun finishWorkoutSavesRecordWithExerciseProgress() = runTest {
        val plan = samplePlan()
        val planRepository = FakeWorkoutPlanRepository(listOf(plan))
        val recordRepository = FakeWorkoutRecordRepository()
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = planRepository,
            recordRepository = recordRepository
        )
        advanceUntilIdle()

        viewModel.updateCompleted(100, true)
        viewModel.updateExerciseNotes(100, "动作稳定")
        viewModel.updateSessionNotes("状态不错")
        viewModel.finishWorkout()
        advanceUntilIdle()

        val record = recordRepository.savedRecord
        assertEquals("四天增肌计划", record?.planName)
        assertEquals("状态不错", record?.notes)
        assertTrue(record?.exercises?.first()?.isCompleted == true)
        assertEquals("动作稳定", record?.exercises?.first()?.actualNotes)
        assertEquals(300L, viewModel.uiState.value.finishedRecordId)
    }

    private fun samplePlan(): WorkoutPlan {
        return WorkoutPlan(
            id = 1,
            name = "四天增肌计划",
            goal = "增肌",
            frequencyPerWeek = 4,
            estimatedDurationMinutes = 60,
            isActive = true,
            days = listOf(
                WorkoutDay(
                    id = 10,
                    planId = 1,
                    dayName = "Day 1 胸肩三头",
                    sortOrder = 0,
                    exercises = listOf(
                        Exercise(
                            id = 100,
                            workoutDayId = 10,
                            name = "杠铃卧推",
                            bodyPart = "胸",
                            sets = 4,
                            reps = "8-10",
                            restSeconds = 90,
                            sortOrder = 0
                        )
                    )
                )
            )
        )
    }
}

