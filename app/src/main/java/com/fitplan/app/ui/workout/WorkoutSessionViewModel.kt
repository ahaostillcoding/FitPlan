package com.fitplan.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.domain.model.WorkoutRecordExercise
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val restoredFromDraft: Boolean = false,
    val activeRestExerciseId: Long? = null,
    val restSecondsRemaining: Int = 0,
    val showIncompleteFinishConfirm: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val finishedRecordId: Long? = null
) {
    val totalExerciseCount: Int = day?.exercises?.size ?: 0
    val completedExerciseCount: Int = exerciseProgress.values.count { it.isCompleted }
    val incompleteExerciseCount: Int = (totalExerciseCount - completedExerciseCount).coerceAtLeast(0)
    val progressText: String = "$completedExerciseCount/$totalExerciseCount 已完成"
}

class WorkoutSessionViewModel(
    private val planId: Long,
    private val dayId: Long,
    private val planRepository: WorkoutPlanRepository,
    private val recordRepository: WorkoutRecordRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val gson = Gson()
    private var restTimerJob: Job? = null

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
        _uiState.update { it.copy(sessionNotes = notes, message = null, errorMessage = null) }
        persistDraft()
    }

    fun continueDraft() {
        _uiState.update { it.copy(restoredFromDraft = false, message = "继续上次未完成训练") }
    }

    fun discardDraft() {
        val state = _uiState.value
        val day = state.day ?: return
        restTimerJob?.cancel()
        viewModelScope.launch {
            settingsRepository.clearWorkoutDraft()
            _uiState.update {
                it.copy(
                    startedAt = System.currentTimeMillis(),
                    sessionNotes = "",
                    exerciseProgress = defaultProgress(day),
                    restoredFromDraft = false,
                    activeRestExerciseId = null,
                    restSecondsRemaining = 0,
                    message = "已放弃上次训练草稿",
                    errorMessage = null
                )
            }
        }
    }

    fun startRestTimer(exerciseId: Long, seconds: Int) {
        restTimerJob?.cancel()
        if (seconds <= 0) {
            stopRestTimer()
            return
        }
        _uiState.update {
            it.copy(
                activeRestExerciseId = exerciseId,
                restSecondsRemaining = seconds,
                message = null
            )
        }
        restTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.restSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { state ->
                    val next = (state.restSecondsRemaining - 1).coerceAtLeast(0)
                    state.copy(
                        restSecondsRemaining = next,
                        activeRestExerciseId = if (next == 0) null else state.activeRestExerciseId,
                        message = if (next == 0) "休息结束，可以开始下一组了" else state.message
                    )
                }
            }
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(
                activeRestExerciseId = null,
                restSecondsRemaining = 0,
                message = if (it.restSecondsRemaining > 0) "休息已停止" else it.message
            )
        }
    }

    fun finishWorkout() {
        val state = uiState.value
        if (state.incompleteExerciseCount > 0) {
            _uiState.update {
                it.copy(
                    showIncompleteFinishConfirm = true,
                    message = null,
                    errorMessage = null
                )
            }
            return
        }
        saveWorkout(state)
    }

    fun confirmFinishWorkout() {
        val state = uiState.value.copy(showIncompleteFinishConfirm = false)
        _uiState.update { it.copy(showIncompleteFinishConfirm = false) }
        saveWorkout(state)
    }

    fun cancelIncompleteFinish() {
        _uiState.update { it.copy(showIncompleteFinishConfirm = false) }
    }

    private fun saveWorkout(state: WorkoutSessionUiState) {
        val plan = state.plan
        val day = state.day
        if (plan == null || day == null) {
            _uiState.update { it.copy(showIncompleteFinishConfirm = false, errorMessage = "训练内容还没有加载完成") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, showIncompleteFinishConfirm = false, message = null, errorMessage = null) }
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
                .onSuccess { recordId ->
                    settingsRepository.clearWorkoutDraft()
                    stopRestTimer()
                    _uiState.update { it.copy(isSaving = false, finishedRecordId = recordId) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showIncompleteFinishConfirm = false,
                            errorMessage = error.message ?: "保存训练记录失败"
                        )
                    }
                }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            planRepository.getPlan(planId)
                .onSuccess { plan ->
                    val selectedDay = plan?.days?.firstOrNull { it.id == dayId }
                        ?: plan?.days?.firstOrNull()
                    if (plan == null || selectedDay == null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "没有找到可训练的内容") }
                    } else {
                        val draft = readMatchingDraft(selectedDay.id)
                        val isDraftExpired = draft?.isExpired() == true
                        if (isDraftExpired) {
                            settingsRepository.clearWorkoutDraft()
                        }
                        val restorableDraft = draft.takeUnless { isDraftExpired }
                        val defaultProgress = defaultProgress(selectedDay)
                        val restoredProgress = restorableDraft?.progress
                            ?.filter { progress -> selectedDay.exercises.any { it.id == progress.exerciseId } }
                            ?.associateBy { it.exerciseId }
                            .orEmpty()
                        _uiState.value = WorkoutSessionUiState(
                            isLoading = false,
                            plan = plan,
                            day = selectedDay,
                            startedAt = restorableDraft?.startedAt ?: System.currentTimeMillis(),
                            sessionNotes = restorableDraft?.sessionNotes.orEmpty(),
                            exerciseProgress = defaultProgress + restoredProgress,
                            restoredFromDraft = restorableDraft != null,
                            message = if (isDraftExpired) "发现超过 24 小时的训练草稿，已为你重新开始" else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "加载训练失败")
                    }
                }
        }
    }

    private suspend fun readMatchingDraft(selectedDayId: Long): WorkoutSessionDraft? {
        val json = settingsRepository.settings.first().workoutDraftJson
        if (json.isBlank()) return null
        return runCatching {
            gson.fromJson(json, WorkoutSessionDraft::class.java)
        }.getOrNull()
            ?.takeIf { draft -> draft.planId == planId && draft.dayId == selectedDayId }
    }

    private fun updateProgress(
        exerciseId: Long,
        reducer: WorkoutExerciseProgress.() -> WorkoutExerciseProgress
    ) {
        _uiState.update { state ->
            val current = state.exerciseProgress[exerciseId] ?: WorkoutExerciseProgress(exerciseId)
            state.copy(
                exerciseProgress = state.exerciseProgress + (exerciseId to current.reducer()),
                message = null,
                errorMessage = null
            )
        }
        persistDraft()
    }

    private fun defaultProgress(day: WorkoutDay): Map<Long, WorkoutExerciseProgress> {
        return day.exercises.associate { exercise ->
            exercise.id to WorkoutExerciseProgress(exercise.id)
        }
    }

    private fun persistDraft() {
        val state = _uiState.value
        val plan = state.plan ?: return
        val day = state.day ?: return
        val draft = WorkoutSessionDraft(
            planId = plan.id,
            dayId = day.id,
            startedAt = state.startedAt,
            sessionNotes = state.sessionNotes,
            progress = state.exerciseProgress.values.toList()
        )
        viewModelScope.launch {
            settingsRepository.saveWorkoutDraftJson(gson.toJson(draft))
        }
    }

    override fun onCleared() {
        restTimerJob?.cancel()
        super.onCleared()
    }

    private data class WorkoutSessionDraft(
        val planId: Long,
        val dayId: Long,
        val startedAt: Long,
        val sessionNotes: String,
        val progress: List<WorkoutExerciseProgress>
    ) {
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
            return startedAt <= 0 || now - startedAt > DRAFT_EXPIRY_MILLIS
        }
    }

    companion object {
        private const val DRAFT_EXPIRY_MILLIS = 24L * 60L * 60L * 1000L

        fun factory(
            planId: Long,
            dayId: Long,
            planRepository: WorkoutPlanRepository,
            recordRepository: WorkoutRecordRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkoutSessionViewModel(
                    planId,
                    dayId,
                    planRepository,
                    recordRepository,
                    settingsRepository
                ) as T
            }
        }
    }
}
