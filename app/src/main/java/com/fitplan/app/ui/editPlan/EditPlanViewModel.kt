package com.fitplan.app.ui.editPlan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseForm(
    val name: String = "",
    val bodyPart: String = "",
    val sets: String = "3",
    val reps: String = "8-12",
    val weight: String = "",
    val restSeconds: String = "60",
    val notes: String = ""
)

data class WorkoutDayForm(
    val dayName: String = "",
    val exercises: List<ExerciseForm> = listOf(ExerciseForm())
)

data class EditPlanUiState(
    val planId: Long? = null,
    val name: String = "",
    val goal: String = "增肌",
    val frequencyPerWeek: String = "3",
    val estimatedDurationMinutes: String = "60",
    val notes: String = "",
    val isActive: Boolean = false,
    val days: List<WorkoutDayForm> = listOf(WorkoutDayForm(dayName = "Day 1")),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedPlanId: Long? = null
)

class EditPlanViewModel(
    private val planId: Long?,
    private val repository: WorkoutPlanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditPlanUiState(planId = planId))
    val uiState: StateFlow<EditPlanUiState> = _uiState.asStateFlow()

    init {
        if (planId != null && planId > 0) {
            loadPlan(planId)
        }
    }

    fun updateName(value: String) = updateState { copy(name = value, errorMessage = null) }
    fun updateGoal(value: String) = updateState { copy(goal = value, errorMessage = null) }
    fun updateFrequency(value: String) = updateState { copy(frequencyPerWeek = value.filter { it.isDigit() }, errorMessage = null) }
    fun updateDuration(value: String) = updateState { copy(estimatedDurationMinutes = value.filter { it.isDigit() }, errorMessage = null) }
    fun updateNotes(value: String) = updateState { copy(notes = value) }
    fun updateActive(value: Boolean) = updateState { copy(isActive = value) }

    fun addDay() {
        updateState {
            copy(days = days + WorkoutDayForm(dayName = "Day ${days.size + 1}"), errorMessage = null)
        }
    }

    fun removeDay(index: Int) {
        updateState {
            if (days.size <= 1) {
                copy(errorMessage = "至少保留一个训练日")
            } else {
                copy(days = days.filterIndexed { i, _ -> i != index }, errorMessage = null)
            }
        }
    }

    fun updateDayName(index: Int, value: String) {
        updateState {
            copy(days = days.mapIndexed { i, day -> if (i == index) day.copy(dayName = value) else day }, errorMessage = null)
        }
    }

    fun addExercise(dayIndex: Int) {
        updateState {
            copy(days = days.mapIndexed { i, day ->
                if (i == dayIndex) day.copy(exercises = day.exercises + ExerciseForm()) else day
            }, errorMessage = null)
        }
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        updateState {
            copy(days = days.mapIndexed { i, day ->
                if (i != dayIndex) {
                    day
                } else if (day.exercises.size <= 1) {
                    day
                } else {
                    day.copy(exercises = day.exercises.filterIndexed { j, _ -> j != exerciseIndex })
                }
            }, errorMessage = null)
        }
    }

    fun updateExercise(dayIndex: Int, exerciseIndex: Int, updater: ExerciseForm.() -> ExerciseForm) {
        updateState {
            copy(days = days.mapIndexed { i, day ->
                if (i != dayIndex) {
                    day
                } else {
                    day.copy(
                        exercises = day.exercises.mapIndexed { j, exercise ->
                            if (j == exerciseIndex) exercise.updater() else exercise
                        }
                    )
                }
            }, errorMessage = null)
        }
    }

    fun save() {
        val state = uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            updateState { copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessage = null) }
            val plan = state.toWorkoutPlan()
            val result = if (state.planId == null) {
                repository.savePlan(plan)
            } else {
                repository.updatePlan(plan).map { state.planId }
            }
            result
                .onSuccess { savedId -> updateState { copy(isSaving = false, savedPlanId = savedId) } }
                .onFailure { updateState { copy(isSaving = false, errorMessage = it.message ?: "保存计划失败") } }
        }
    }

    private fun loadPlan(planId: Long) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            repository.getPlan(planId)
                .onSuccess { plan ->
                    if (plan == null) {
                        updateState { copy(isLoading = false, errorMessage = "未找到训练计划") }
                    } else {
                        _uiState.value = plan.toEditState()
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, errorMessage = error.message ?: "加载计划失败") }
                }
        }
    }

    private fun validate(state: EditPlanUiState): String? {
        if (state.name.isBlank()) return "请输入计划名称"
        if (state.goal.isBlank()) return "请输入训练目标"
        val frequency = state.frequencyPerWeek.toIntOrNull()
        if (frequency == null || frequency !in 1..7) return "每周训练次数需为 1-7"
        val duration = state.estimatedDurationMinutes.toIntOrNull()
        if (duration == null || duration <= 0) return "请输入有效训练时长"
        state.days.forEachIndexed { dayIndex, day ->
            if (day.dayName.isBlank()) return "请输入第 ${dayIndex + 1} 个训练日名称"
            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                val label = "第 ${dayIndex + 1} 天第 ${exerciseIndex + 1} 个动作"
                if (exercise.name.isBlank()) return "$label 需要动作名称"
                if (exercise.bodyPart.isBlank()) return "$label 需要训练部位"
                if ((exercise.sets.toIntOrNull() ?: 0) <= 0) return "$label 组数需大于 0"
                if (exercise.reps.isBlank()) return "$label 需要次数"
                if ((exercise.restSeconds.toIntOrNull() ?: -1) < 0) return "$label 休息时间不能小于 0"
            }
        }
        return null
    }

    private fun EditPlanUiState.toWorkoutPlan(): WorkoutPlan {
        return WorkoutPlan(
            id = planId ?: 0,
            name = name,
            goal = goal,
            frequencyPerWeek = frequencyPerWeek.toInt(),
            estimatedDurationMinutes = estimatedDurationMinutes.toInt(),
            notes = notes,
            isActive = isActive,
            days = days.mapIndexed { dayIndex, day ->
                WorkoutDay(
                    dayName = day.dayName,
                    sortOrder = dayIndex,
                    exercises = day.exercises.mapIndexed { exerciseIndex, exercise ->
                        Exercise(
                            name = exercise.name,
                            bodyPart = exercise.bodyPart,
                            sets = exercise.sets.toInt(),
                            reps = exercise.reps,
                            weight = exercise.weight,
                            restSeconds = exercise.restSeconds.toInt(),
                            notes = exercise.notes,
                            sortOrder = exerciseIndex
                        )
                    }
                )
            }
        )
    }

    private fun WorkoutPlan.toEditState(): EditPlanUiState {
        return EditPlanUiState(
            planId = id,
            name = name,
            goal = goal,
            frequencyPerWeek = frequencyPerWeek.toString(),
            estimatedDurationMinutes = estimatedDurationMinutes.toString(),
            notes = notes,
            isActive = isActive,
            days = days.ifEmpty { listOf(WorkoutDay(dayName = "Day 1", sortOrder = 0)) }
                .map { day ->
                    WorkoutDayForm(
                        dayName = day.dayName,
                        exercises = day.exercises.ifEmpty {
                            listOf(
                                Exercise(
                                    name = "",
                                    bodyPart = "",
                                    sets = 3,
                                    reps = "8-12",
                                    restSeconds = 60,
                                    sortOrder = 0
                                )
                            )
                        }.map { exercise ->
                            ExerciseForm(
                                name = exercise.name,
                                bodyPart = exercise.bodyPart,
                                sets = exercise.sets.toString(),
                                reps = exercise.reps,
                                weight = exercise.weight,
                                restSeconds = exercise.restSeconds.toString(),
                                notes = exercise.notes
                            )
                        }
                    )
                },
            isLoading = false
        )
    }

    private fun updateState(reducer: EditPlanUiState.() -> EditPlanUiState) {
        _uiState.update { current -> current.reducer() }
    }

    companion object {
        fun factory(
            planId: Long?,
            repository: WorkoutPlanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditPlanViewModel(planId, repository) as T
            }
        }
    }
}
