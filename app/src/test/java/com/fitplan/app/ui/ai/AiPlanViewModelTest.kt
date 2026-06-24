package com.fitplan.app.ui.ai

import com.fitplan.app.data.repository.AiPlanInput
import com.fitplan.app.data.repository.AiPlanRepository
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiPlanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun generatePlan_withInvalidDays_showsValidationError() {
        val viewModel = AiPlanViewModel(
            aiPlanRepository = FakeAiPlanRepository(Result.success(samplePlan)),
            workoutPlanRepository = FakeWorkoutPlanRepository()
        )

        viewModel.updateDaysPerWeek("0")
        viewModel.generatePlan()

        assertEquals("每周训练天数必须在 1 到 7 之间", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun generatePlan_success_showsPreview() = runTest {
        val viewModel = AiPlanViewModel(
            aiPlanRepository = FakeAiPlanRepository(Result.success(samplePlan)),
            workoutPlanRepository = FakeWorkoutPlanRepository()
        )

        viewModel.generatePlan()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.preview)
        assertEquals("三天新手训练计划", viewModel.uiState.value.preview?.name)
        assertEquals("AI 计划已生成，请预览并确认后保存", viewModel.uiState.value.message)
    }

    @Test
    fun saveGeneratedPlan_afterPreview_savesToRepository() = runTest {
        val workoutRepository = FakeWorkoutPlanRepository()
        val viewModel = AiPlanViewModel(
            aiPlanRepository = FakeAiPlanRepository(Result.success(samplePlan)),
            workoutPlanRepository = workoutRepository
        )

        viewModel.generatePlan()
        advanceUntilIdle()
        viewModel.saveGeneratedPlan()
        advanceUntilIdle()

        assertEquals("三天新手训练计划", workoutRepository.savedPlan?.name)
        assertEquals(100L, viewModel.uiState.value.savedPlanId)
    }

    @Test
    fun generatePlan_failure_showsError() = runTest {
        val viewModel = AiPlanViewModel(
            aiPlanRepository = FakeAiPlanRepository(Result.failure(IllegalStateException("请先配置 Key"))),
            workoutPlanRepository = FakeWorkoutPlanRepository()
        )

        viewModel.generatePlan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.preview == null)
        assertEquals("请先配置 Key", viewModel.uiState.value.errorMessage)
    }

    private class FakeAiPlanRepository(
        private val result: Result<WorkoutPlan>
    ) : AiPlanRepository {
        override suspend fun generatePlan(input: AiPlanInput): Result<WorkoutPlan> = result
        override fun parseWorkoutPlanJson(json: String): Result<WorkoutPlan> = result
    }

    private companion object {
        val samplePlan = WorkoutPlan(
            name = "三天新手训练计划",
            goal = "新手入门",
            frequencyPerWeek = 3,
            estimatedDurationMinutes = 45,
            notes = "循序渐进",
            days = listOf(
                WorkoutDay(
                    dayName = "Day 1 全身基础",
                    sortOrder = 0,
                    exercises = listOf(
                        Exercise(
                            name = "深蹲",
                            bodyPart = "腿",
                            sets = 3,
                            reps = "10-12",
                            restSeconds = 60,
                            sortOrder = 0
                        )
                    )
                )
            )
        )
    }
}
