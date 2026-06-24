package com.fitplan.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.AiPlanInput
import com.fitplan.app.data.repository.AiPlanRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiPlanUiState(
    val goal: String = "增肌",
    val daysPerWeek: String = "3",
    val durationMinutes: String = "60",
    val experienceLevel: String = "新手",
    val equipment: String = "徒手、哑铃",
    val limitations: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val preview: AiGeneratedPlanForm? = null,
    val isEditingPreview: Boolean = false,
    val savedPlanId: Long? = null
)

data class AiGeneratedPlanForm(
    val name: String,
    val goal: String,
    val frequencyPerWeek: String,
    val estimatedDurationMinutes: String,
    val notes: String,
    val days: List<AiWorkoutDayForm>
)

data class AiWorkoutDayForm(
    val dayName: String,
    val exercises: List<AiExerciseForm>
)

data class AiExerciseForm(
    val name: String,
    val bodyPart: String,
    val sets: String,
    val reps: String,
    val weight: String,
    val restSeconds: String,
    val notes: String
)

class AiPlanViewModel(
    private val aiPlanRepository: AiPlanRepository,
    private val workoutPlanRepository: WorkoutPlanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiPlanUiState())
    val uiState: StateFlow<AiPlanUiState> = _uiState.asStateFlow()

    fun updateGoal(value: String) = updateState { copy(goal = value, errorMessage = null, message = null) }
    fun updateDaysPerWeek(value: String) = updateState { copy(daysPerWeek = value.filter { it.isDigit() }, errorMessage = null, message = null) }
    fun updateDuration(value: String) = updateState { copy(durationMinutes = value.filter { it.isDigit() }, errorMessage = null, message = null) }
    fun updateExperienceLevel(value: String) = updateState { copy(experienceLevel = value, errorMessage = null, message = null) }
    fun updateEquipment(value: String) = updateState { copy(equipment = value, errorMessage = null, message = null) }
    fun updateLimitations(value: String) = updateState { copy(limitations = value, errorMessage = null, message = null) }
    fun updateNotes(value: String) = updateState { copy(notes = value, errorMessage = null, message = null) }

    fun generatePlan() {
        val state = uiState.value
        val input = state.toInputOrError()
        if (input.isFailure) {
            updateState { copy(errorMessage = input.exceptionOrNull()?.message ?: "请输入有效信息") }
            return
        }

        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    errorMessage = null,
                    message = null,
                    preview = null,
                    savedPlanId = null
                )
            }
            aiPlanRepository.generatePlan(input.getOrThrow())
                .onSuccess { plan ->
                    updateState {
                        copy(
                            isLoading = false,
                            preview = plan.toForm(),
                            isEditingPreview = false,
                            message = "AI 计划已生成，请预览并确认后保存"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "AI 生成失败，请检查网络后重试"
                        )
                    }
                }
        }
    }

    fun togglePreviewEditing() {
        updateState { copy(isEditingPreview = !isEditingPreview, errorMessage = null, message = null) }
    }

    fun updatePreviewPlan(updater: AiGeneratedPlanForm.() -> AiGeneratedPlanForm) {
        updateState { copy(preview = preview?.updater(), errorMessage = null, message = null) }
    }

    fun updatePreviewDay(dayIndex: Int, dayName: String) {
        updatePreviewPlan {
            copy(days = days.mapIndexed { index, day ->
                if (index == dayIndex) day.copy(dayName = dayName) else day
            })
        }
    }

    fun updatePreviewExercise(
        dayIndex: Int,
        exerciseIndex: Int,
        updater: AiExerciseForm.() -> AiExerciseForm
    ) {
        updatePreviewPlan {
            copy(days = days.mapIndexed { currentDayIndex, day ->
                if (currentDayIndex != dayIndex) {
                    day
                } else {
                    day.copy(
                        exercises = day.exercises.mapIndexed { currentExerciseIndex, exercise ->
                            if (currentExerciseIndex == exerciseIndex) exercise.updater() else exercise
                        }
                    )
                }
            })
        }
    }

    fun saveGeneratedPlan() {
        val preview = uiState.value.preview
        if (preview == null) {
            updateState { copy(errorMessage = "请先生成计划") }
            return
        }
        val plan = preview.toWorkoutPlanOrError()
        if (plan.isFailure) {
            updateState { copy(errorMessage = plan.exceptionOrNull()?.message ?: "计划内容不完整，请编辑后再保存") }
            return
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessage = null, message = null) }
            workoutPlanRepository.savePlan(plan.getOrThrow())
                .onSuccess { planId ->
                    updateState {
                        copy(
                            isSaving = false,
                            savedPlanId = planId,
                            message = "AI 计划已保存"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "保存计划失败"
                        )
                    }
                }
        }
    }

    private fun AiPlanUiState.toInputOrError(): Result<AiPlanInput> {
        return runCatching {
            val days = daysPerWeek.toIntOrNull()
                ?: throw IllegalArgumentException("请输入每周训练天数")
            val duration = durationMinutes.toIntOrNull()
                ?: throw IllegalArgumentException("请输入每次训练时长")
            require(goal.isNotBlank()) { "请选择健身目标" }
            require(days in 1..7) { "每周训练天数必须在 1 到 7 之间" }
            require(duration > 0) { "每次训练时长必须大于 0" }
            require(experienceLevel.isNotBlank()) { "请选择经验水平" }
            AiPlanInput(
                goal = goal,
                daysPerWeek = days,
                durationMinutes = duration,
                experienceLevel = experienceLevel,
                equipment = equipment,
                limitations = limitations,
                notes = notes
            )
        }
    }

    private fun AiGeneratedPlanForm.toWorkoutPlanOrError(): Result<WorkoutPlan> {
        return runCatching {
            val frequency = frequencyPerWeek.toIntOrNull()
                ?: throw IllegalArgumentException("请输入每周训练次数")
            val duration = estimatedDurationMinutes.toIntOrNull()
                ?: throw IllegalArgumentException("请输入每次训练时长")
            require(name.isNotBlank()) { "计划名称不能为空" }
            require(goal.isNotBlank()) { "训练目标不能为空" }
            require(frequency in 1..7) { "每周训练次数必须在 1 到 7 之间" }
            require(duration > 0) { "训练时长必须大于 0" }
            require(days.isNotEmpty()) { "至少需要一个训练日" }

            WorkoutPlan(
                name = name.trim(),
                goal = goal.trim(),
                frequencyPerWeek = frequency,
                estimatedDurationMinutes = duration,
                notes = notes.trim(),
                isActive = false,
                days = days.mapIndexed { dayIndex, day ->
                    require(day.dayName.isNotBlank()) { "第 ${dayIndex + 1} 个训练日名称不能为空" }
                    require(day.exercises.isNotEmpty()) { "${day.dayName} 至少需要一个动作" }
                    WorkoutDay(
                        dayName = day.dayName.trim(),
                        sortOrder = dayIndex,
                        exercises = day.exercises.mapIndexed { exerciseIndex, exercise ->
                            val label = "${day.dayName} 第 ${exerciseIndex + 1} 个动作"
                            val sets = exercise.sets.toIntOrNull()
                                ?: throw IllegalArgumentException("$label 组数必须是数字")
                            val restSeconds = exercise.restSeconds.toIntOrNull()
                                ?: throw IllegalArgumentException("$label 休息时间必须是数字")
                            require(exercise.name.isNotBlank()) { "$label 名称不能为空" }
                            require(exercise.bodyPart.isNotBlank()) { "$label 部位不能为空" }
                            require(sets > 0) { "$label 组数必须大于 0" }
                            require(exercise.reps.isNotBlank()) { "$label 次数不能为空" }
                            require(restSeconds >= 0) { "$label 休息时间不能小于 0" }
                            Exercise(
                                name = exercise.name.trim(),
                                bodyPart = exercise.bodyPart.trim(),
                                sets = sets,
                                reps = exercise.reps.trim(),
                                weight = exercise.weight.trim(),
                                restSeconds = restSeconds,
                                notes = exercise.notes.trim(),
                                sortOrder = exerciseIndex
                            )
                        }
                    )
                }
            )
        }
    }

    private fun WorkoutPlan.toForm(): AiGeneratedPlanForm {
        return AiGeneratedPlanForm(
            name = name,
            goal = goal,
            frequencyPerWeek = frequencyPerWeek.toString(),
            estimatedDurationMinutes = estimatedDurationMinutes.toString(),
            notes = notes,
            days = days.map { day ->
                AiWorkoutDayForm(
                    dayName = day.dayName,
                    exercises = day.exercises.map { exercise ->
                        AiExerciseForm(
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
            }
        )
    }

    private fun updateState(reducer: AiPlanUiState.() -> AiPlanUiState) {
        _uiState.update { it.reducer() }
    }

    companion object {
        fun factory(
            aiPlanRepository: AiPlanRepository,
            workoutPlanRepository: WorkoutPlanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiPlanViewModel(aiPlanRepository, workoutPlanRepository) as T
            }
        }
    }
}
