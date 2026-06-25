package com.fitplan.app.ui.workout

import com.fitplan.app.data.repository.AppSettings
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.FakeWorkoutPlanRepository
import com.fitplan.app.ui.FakeWorkoutRecordRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val settingsRepository = FakeSettingsRepository()
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = planRepository,
            recordRepository = recordRepository,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        viewModel.updateCompleted(100, true)
        viewModel.updateCompleted(101, true)
        viewModel.updateExerciseNotes(100, "动作稳定")
        viewModel.updateSessionNotes("状态不错")
        viewModel.finishWorkout()
        advanceUntilIdle()

        val record = recordRepository.savedRecord
        assertEquals("四天增肌训练计划", record?.planName)
        assertEquals("状态不错", record?.notes)
        assertTrue(record?.exercises?.first()?.isCompleted == true)
        assertEquals("动作稳定", record?.exercises?.first()?.actualNotes)
        assertEquals(300L, viewModel.uiState.value.finishedRecordId)
        assertEquals("", settingsRepository.current.workoutDraftJson)
    }

    @Test
    fun finishWorkoutWithIncompleteExercisesRequiresConfirmation() = runTest {
        val plan = samplePlan()
        val planRepository = FakeWorkoutPlanRepository(listOf(plan))
        val recordRepository = FakeWorkoutRecordRepository()
        val settingsRepository = FakeSettingsRepository()
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = planRepository,
            recordRepository = recordRepository,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        viewModel.updateCompleted(100, true)
        viewModel.finishWorkout()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showIncompleteFinishConfirm)
        assertEquals(1, viewModel.uiState.value.incompleteExerciseCount)
        assertNull(recordRepository.savedRecord)

        viewModel.confirmFinishWorkout()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showIncompleteFinishConfirm)
        assertEquals(300L, viewModel.uiState.value.finishedRecordId)
        assertTrue(recordRepository.savedRecord?.exercises?.first()?.isCompleted == true)
        assertFalse(recordRepository.savedRecord?.exercises?.last()?.isCompleted == true)
    }

    @Test
    fun progressChangesPersistWorkoutDraft() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        viewModel.updateCompleted(100, true)
        viewModel.updateSessionNotes("还差一组")
        advanceUntilIdle()

        assertTrue(settingsRepository.current.workoutDraftJson.contains("\"planId\":1"))
        assertTrue(settingsRepository.current.workoutDraftJson.contains("\"dayId\":10"))
        assertTrue(settingsRepository.current.workoutDraftJson.contains("还差一组"))
    }

    @Test
    fun matchingDraftRestoresSession() = runTest {
        val draftJson = """
            {
              "planId": 1,
              "dayId": 10,
              "startedAt": 123456,
              "sessionNotes": "恢复训练",
              "progress": [
                {
                  "exerciseId": 100,
                  "isCompleted": true,
                  "actualNotes": "上一轮完成"
                }
              ]
            }
        """.trimIndent()
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = FakeSettingsRepository(workoutDraftJson = draftJson)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.restoredFromDraft)
        assertEquals(123456L, state.startedAt)
        assertEquals("恢复训练", state.sessionNotes)
        assertTrue(state.exerciseProgress.getValue(100).isCompleted)
        assertEquals("上一轮完成", state.exerciseProgress.getValue(100).actualNotes)
    }

    @Test
    fun progressTextCountsCompletedExercises() = runTest {
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = FakeSettingsRepository()
        )
        advanceUntilIdle()

        assertEquals("0/2 已完成", viewModel.uiState.value.progressText)

        viewModel.updateCompleted(100, true)

        assertEquals("1/2 已完成", viewModel.uiState.value.progressText)
    }

    @Test
    fun discardDraftClearsSavedDraftAndResetsSession() = runTest {
        val draftJson = """
            {
              "planId": 1,
              "dayId": 10,
              "startedAt": 123456,
              "sessionNotes": "恢复训练",
              "progress": [
                {
                  "exerciseId": 100,
                  "isCompleted": true,
                  "actualNotes": "上一轮完成"
                }
              ]
            }
        """.trimIndent()
        val settingsRepository = FakeSettingsRepository(workoutDraftJson = draftJson)
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        viewModel.discardDraft()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.restoredFromDraft)
        assertEquals("", state.sessionNotes)
        assertEquals("0/2 已完成", state.progressText)
        assertEquals("", settingsRepository.current.workoutDraftJson)
        assertEquals("已放弃上次训练草稿", state.message)
    }

    @Test
    fun restTimerCountsDownAndStops() = runTest {
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = FakeSettingsRepository()
        )
        advanceUntilIdle()

        viewModel.startRestTimer(exerciseId = 100, seconds = 3)
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.restSecondsRemaining)
        assertEquals(100L, viewModel.uiState.value.activeRestExerciseId)

        advanceTimeBy(2_000)
        runCurrent()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.restSecondsRemaining)
        assertEquals(null, viewModel.uiState.value.activeRestExerciseId)
        assertEquals("休息结束，可以开始下一组了", viewModel.uiState.value.message)
    }

    @Test
    fun stopRestTimerClearsTimerState() = runTest {
        val viewModel = WorkoutSessionViewModel(
            planId = 1,
            dayId = 10,
            planRepository = FakeWorkoutPlanRepository(listOf(samplePlan())),
            recordRepository = FakeWorkoutRecordRepository(),
            settingsRepository = FakeSettingsRepository()
        )
        advanceUntilIdle()

        viewModel.startRestTimer(exerciseId = 100, seconds = 60)
        viewModel.stopRestTimer()

        assertEquals(0, viewModel.uiState.value.restSecondsRemaining)
        assertEquals(null, viewModel.uiState.value.activeRestExerciseId)
    }

    private class FakeSettingsRepository(
        apiKey: String = "",
        model: String = DEFAULT_DEEPSEEK_MODEL,
        workoutDraftJson: String = ""
    ) : SettingsRepository {
        private val state = MutableStateFlow(
            AppSettings(
                deepSeekApiKey = apiKey,
                deepSeekModel = model,
                workoutDraftJson = workoutDraftJson
            )
        )

        val current: AppSettings get() = state.value

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

    private fun samplePlan(): WorkoutPlan {
        return WorkoutPlan(
            id = 1,
            name = "四天增肌训练计划",
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
                        ),
                        Exercise(
                            id = 101,
                            workoutDayId = 10,
                            name = "坐姿推举",
                            bodyPart = "肩",
                            sets = 4,
                            reps = "8-10",
                            restSeconds = 90,
                            sortOrder = 1
                        )
                    )
                )
            )
        )
    }
}
