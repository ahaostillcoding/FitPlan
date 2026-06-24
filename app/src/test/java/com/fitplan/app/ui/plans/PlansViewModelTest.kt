package com.fitplan.app.ui.plans

import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlansViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setActive_updatesPlanAndShowsMessage() = runTest {
        val repository = FakeWorkoutPlanRepository(
            initialPlans = listOf(
                WorkoutPlan(id = 1, name = "A", goal = "增肌", frequencyPerWeek = 3, estimatedDurationMinutes = 60),
                WorkoutPlan(id = 2, name = "B", goal = "减脂", frequencyPerWeek = 4, estimatedDurationMinutes = 45)
            )
        )
        val viewModel = PlansViewModel(repository)

        viewModel.setActive(2)
        advanceUntilIdle()

        assertEquals("已设为当前计划", viewModel.message.value)
        assertEquals(2L, repository.currentPlans.single { it.isActive }.id)
    }

    @Test
    fun duplicate_showsMessageAndAddsCopy() = runTest {
        val repository = FakeWorkoutPlanRepository(
            initialPlans = listOf(
                WorkoutPlan(id = 1, name = "A", goal = "增肌", frequencyPerWeek = 3, estimatedDurationMinutes = 60)
            )
        )
        val viewModel = PlansViewModel(repository)

        viewModel.duplicate(1)
        advanceUntilIdle()

        assertEquals("计划已复制", viewModel.message.value)
        assertEquals(2, repository.currentPlans.size)
    }

    @Test
    fun delete_removesPlanAndShowsMessage() = runTest {
        val repository = FakeWorkoutPlanRepository(
            initialPlans = listOf(
                WorkoutPlan(id = 1, name = "A", goal = "增肌", frequencyPerWeek = 3, estimatedDurationMinutes = 60)
            )
        )
        val viewModel = PlansViewModel(repository)

        viewModel.delete(1)
        advanceUntilIdle()

        assertEquals("计划已删除", viewModel.message.value)
        assertFalse(repository.currentPlans.any { it.id == 1L })
    }
}
