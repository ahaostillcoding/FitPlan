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

        viewModel.updateName("四天增肌训练计划")
        viewModel.updateGoal("增肌")
        viewModel.updateDayName(0, "Day 1 胸肩三头")
        viewModel.updateExercise(0, 0) { copy(name = "杠铃卧推", bodyPart = "胸") }
        viewModel.save()

        assertEquals("四天增肌训练计划", repository.savedPlan?.name)
        assertEquals(100L, viewModel.uiState.value.savedPlanId)
    }

    @Test
    fun moveDay_changesSavedDayOrder() = runTest {
        val repository = FakeWorkoutPlanRepository()
        val viewModel = EditPlanViewModel(null, repository)

        viewModel.updateName("排序计划")
        viewModel.updateDayName(0, "Day 1")
        viewModel.updateExercise(0, 0) { copy(name = "深蹲", bodyPart = "腿") }
        viewModel.addDay()
        viewModel.updateDayName(1, "Day 2")
        viewModel.updateExercise(1, 0) { copy(name = "卧推", bodyPart = "胸") }
        viewModel.moveDayUp(1)
        viewModel.save()

        assertEquals("Day 2", repository.savedPlan?.days?.first()?.dayName)
        assertEquals("Day 1", repository.savedPlan?.days?.last()?.dayName)
    }

    @Test
    fun moveExercise_changesSavedExerciseOrder() = runTest {
        val repository = FakeWorkoutPlanRepository()
        val viewModel = EditPlanViewModel(null, repository)

        viewModel.updateName("动作排序计划")
        viewModel.updateDayName(0, "Day 1")
        viewModel.updateExercise(0, 0) { copy(name = "深蹲", bodyPart = "腿") }
        viewModel.addExercise(0)
        viewModel.updateExercise(0, 1) { copy(name = "卧推", bodyPart = "胸") }
        viewModel.moveExerciseUp(0, 1)
        viewModel.save()

        assertEquals("卧推", repository.savedPlan?.days?.first()?.exercises?.first()?.name)
        assertEquals("深蹲", repository.savedPlan?.days?.first()?.exercises?.last()?.name)
    }
}
