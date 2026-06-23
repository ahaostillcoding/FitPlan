package com.fitplan.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.domain.model.WorkoutRecordExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class WorkoutExerciseProgress(
    val exerciseId: Long,
    val isCompleted: Boolean = false,
    val actualNotes: String = ""
)

data class WorkoutSessionUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val plan: WorkoutPlan? = null,
    val day: WorkoutDay? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val sessionNotes: String = "",
    val exerciseProgress: Map<Long, WorkoutExerciseProgress> = emptyMap(),
    val errorMessage: String? = null,
    val finishedRecordId: Long? = null
)

class WorkoutSessionViewModel(
    private val planId: Long,
    private val dayId: Long,
    private val planRepository: WorkoutPlanRepository,
    private val recordRepository: WorkoutRecordRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun updateCompleted(exerciseId: Long, completed: Boolean) {
        updateProgress(exerciseId) { copy(isCompleted = completed) }
    }

    fun updateExerciseNotes(exerciseId: Long, notes: String) {
        updateProgress(exerciseId) { copy(actualNotes = notes) }
    }

    fun updateSessionNotes(notes: String) {
        _uiState.update { it.copy(sessionNotes = notes) }
    }

    fun finishWorkout() {
        val state = uiState.value
        val plan = state.plan
        val day = state.day
        if (plan == null || day == null) {
            _uiState.update { it.copy(errorMessage = "训练内容未加载完成") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val now = System.currentTimeMillis()
            val duration = max(1, ((now - state.startedAt) / 60_000L).toInt())
            val record = WorkoutRecord(
                planId = plan.id,
                workoutDayId = day.id,
                planName = plan.name,
                workoutDayName = day.dayName,
                date = now,
                durationMinutes = duration,
                notes = state.sessionNotes,
                exercises = day.exercises.map { exercise ->
                    val progress = state.exerciseProgress[exercise.id] ?: WorkoutExerciseProgress(exercise.id)
                    WorkoutRecordExercise(
                        originalExerciseId = exercise.id,
                        name = exercise.name,
                        bodyPart = exercise.bodyPart,
                        sets = exercise.sets,
                        reps = exercise.reps,
                        weight = exercise.weight,
                        restSeconds = exercise.restSeconds,
                        plannedNotes = exercise.notes,
                        isCompleted = progress.isCompleted,
                        actualNotes = progress.actualNotes,
                        sortOrder = exercise.sortOrder
                    )
                }
            )
            recordRepository.saveRecord(record)
                .onSuccess { recordId -> _uiState.update { it.copy(isSaving = false, finishedRecordId = recordId) } }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "保存训练记录失败") } }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            planRepository.getPlan(planId)
                .onSuccess { plan ->
                    val selectedDay = plan?.days?.firstOrNull { it.id == dayId }
                        ?: plan?.days?.firstOrNull()
                    if (plan == null || selectedDay == null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "未找到训练内容") }
                    } else {
                        _uiState.value = WorkoutSessionUiState(
                            isLoading = false,
                            plan = plan,
                            day = selectedDay,
                            exerciseProgress = selectedDay.exercises.associate { exercise ->
                                exercise.id to WorkoutExerciseProgress(exercise.id)
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "加载训练失败") }
                }
        }
    }

    private fun updateProgress(
        exerciseId: Long,
        reducer: WorkoutExerciseProgress.() -> WorkoutExerciseProgress
    ) {
        _uiState.update { state ->
            val current = state.exerciseProgress[exerciseId] ?: WorkoutExerciseProgress(exerciseId)
            state.copy(exerciseProgress = state.exerciseProgress + (exerciseId to current.reducer()))
        }
    }

    companion object {
        fun factory(
            planId: Long,
            dayId: Long,
            planRepository: WorkoutPlanRepository,
            recordRepository: WorkoutRecordRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkoutSessionViewModel(planId, dayId, planRepository, recordRepository) as T
            }
        }
    }
}

