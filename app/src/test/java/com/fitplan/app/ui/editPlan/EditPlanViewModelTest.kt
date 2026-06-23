package com.fitplan.app.ui.editPlan

import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditPlanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveWithEmptyNameShowsValidationError() = runTest {
        val repository = FakeWorkoutPlanRepository()
        val viewModel = EditPlanViewModel(null, repository)

        viewModel.save()

        assertEquals("请输入计划名称", viewModel.uiState.value.errorMessage)
        assertNull(repository.savedPlan)
    }

    @Test
    fun saveWithValidFormCreatesPlan() = runTest {
        val repository = FakeWorkoutPlanRepository()
        val viewModel = EditPlanViewModel(null, repository)

        viewModel.updateName("四天增肌计划")
        viewModel.updateGoal("增肌")
        viewModel.updateDayName(0, "Day 1 胸肩三头")
        viewModel.updateExercise(0, 0) { copy(name = "杠铃卧推", bodyPart = "胸") }
        viewModel.save()

        assertEquals("四天增肌计划", repository.savedPlan?.name)
        assertEquals(100L, viewModel.uiState.value.savedPlanId)
    }
}

