package com.fitplan.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val GoalOptions = listOf("增肌", "减脂", "塑形", "力量提升", "新手入门")
private val LevelOptions = listOf("新手", "中级", "高级")

@Composable
fun AiPlanScreen(
    viewModel: AiPlanViewModel,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.savedPlanId) {
        state.savedPlanId?.let(onSaved)
    }
    AiPlanContent(
        state = state,
        actions = AiPlanActions(
            onGoalChange = viewModel::updateGoal,
            onDaysChange = viewModel::updateDaysPerWeek,
            onDurationChange = viewModel::updateDuration,
            onLevelChange = viewModel::updateExperienceLevel,
            onEquipmentChange = viewModel::updateEquipment,
            onLimitationsChange = viewModel::updateLimitations,
            onNotesChange = viewModel::updateNotes,
            onGenerate = viewModel::generatePlan,
            onToggleEdit = viewModel::togglePreviewEditing,
            onSave = viewModel::saveGeneratedPlan,
            onUpdatePreviewPlan = viewModel::updatePreviewPlan,
            onUpdatePreviewDay = viewModel::updatePreviewDay,
            onUpdatePreviewExercise = viewModel::updatePreviewExercise
        ),
        modifier = modifier
    )
}

@Composable
private fun AiPlanContent(
    state: AiPlanUiState,
    actions: AiPlanActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI 生成计划", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            AiInputCard(state = state, actions = actions)
        }
        if (state.errorMessage != null) {
            item {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (state.message != null) {
            item {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.preview?.let { preview ->
            item {
                PreviewHeader(
                    isEditing = state.isEditingPreview,
                    isSaving = state.isSaving,
                    onToggleEdit = actions.onToggleEdit,
                    onSave = actions.onSave
                )
            }
            item {
                PlanPreviewCard(
                    preview = preview,
                    isEditing = state.isEditingPreview,
                    actions = actions
                )
            }
        }
    }
}

@Composable
private fun AiInputCard(
    state: AiPlanUiState,
    actions: AiPlanActions
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("你的训练需求", style = MaterialTheme.typography.titleLarge)
            Text("健身目标", style = MaterialTheme.typography.titleSmall)
            ChipRow(options = GoalOptions, selected = state.goal, onSelect = actions.onGoalChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.daysPerWeek,
                    onValueChange = actions.onDaysChange,
                    label = { Text("每周天数") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.durationMinutes,
                    onValueChange = actions.onDurationChange,
                    label = { Text("每次分钟") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Text("经验水平", style = MaterialTheme.typography.titleSmall)
            ChipRow(options = LevelOptions, selected = state.experienceLevel, onSelect = actions.onLevelChange)
            OutlinedTextField(
                value = state.equipment,
                onValueChange = actions.onEquipmentChange,
                label = { Text("可用器械") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = state.limitations,
                onValueChange = actions.onLimitationsChange,
                label = { Text("需要避开的动作或受限部位") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = actions.onNotesChange,
                label = { Text("其他备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = actions.onGenerate,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("生成中...")
                } else {
                    Text(if (state.preview == null) "生成计划" else "重新生成计划")
                }
            }
            Text(
                "生成前请先在设置页保存 DeepSeek API Key。AI 计划只会进入预览，不会自动覆盖已有计划；手动计划和历史记录可离线使用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}

@Composable
private fun PreviewHeader(
    isEditing: Boolean,
    isSaving: Boolean,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("生成结果预览", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onToggleEdit) {
            Text(if (isEditing) "完成编辑" else "编辑")
        }
        Button(onClick = onSave, enabled = !isSaving) {
            Text(if (isSaving) "保存中..." else "保存计划")
        }
    }
}

@Composable
private fun PlanPreviewCard(
    preview: AiGeneratedPlanForm,
    isEditing: Boolean,
    actions: AiPlanActions
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEditing) {
                EditablePlanFields(preview = preview, actions = actions)
            } else {
                ReadOnlyPlanPreview(preview = preview)
            }
        }
    }
}

@Composable
private fun EditablePlanFields(
    preview: AiGeneratedPlanForm,
    actions: AiPlanActions
) {
    OutlinedTextField(
        value = preview.name,
        onValueChange = { value -> actions.onUpdatePreviewPlan { copy(name = value) } },
        label = { Text("计划名称") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = preview.goal,
        onValueChange = { value -> actions.onUpdatePreviewPlan { copy(goal = value) } },
        label = { Text("目标") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = preview.frequencyPerWeek,
            onValueChange = { value -> actions.onUpdatePreviewPlan { copy(frequencyPerWeek = value.filter { it.isDigit() }) } },
            label = { Text("每周次数") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        OutlinedTextField(
            value = preview.estimatedDurationMinutes,
            onValueChange = { value -> actions.onUpdatePreviewPlan { copy(estimatedDurationMinutes = value.filter { it.isDigit() }) } },
            label = { Text("每次分钟") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
    OutlinedTextField(
        value = preview.notes,
        onValueChange = { value -> actions.onUpdatePreviewPlan { copy(notes = value) } },
        label = { Text("备注") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    preview.days.forEachIndexed { dayIndex, day ->
        EditableDay(dayIndex = dayIndex, day = day, actions = actions)
    }
}

@Composable
private fun EditableDay(
    dayIndex: Int,
    day: AiWorkoutDayForm,
    actions: AiPlanActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("训练日 ${dayIndex + 1}", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = day.dayName,
            onValueChange = { actions.onUpdatePreviewDay(dayIndex, it) },
            label = { Text("训练日名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        day.exercises.forEachIndexed { exerciseIndex, exercise ->
            EditableExercise(
                dayIndex = dayIndex,
                exerciseIndex = exerciseIndex,
                exercise = exercise,
                actions = actions
            )
        }
    }
}

@Composable
private fun EditableExercise(
    dayIndex: Int,
    exerciseIndex: Int,
    exercise: AiExerciseForm,
    actions: AiPlanActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("动作 ${exerciseIndex + 1}", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = exercise.name,
            onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(name = value) } },
            label = { Text("动作名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = exercise.bodyPart,
            onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(bodyPart = value) } },
            label = { Text("训练部位") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercise.sets,
                onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(sets = value.filter { it.isDigit() }) } },
                label = { Text("组数") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = exercise.reps,
                onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(reps = value) } },
                label = { Text("次数") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercise.weight,
                onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(weight = value) } },
                label = { Text("重量") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = exercise.restSeconds,
                onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(restSeconds = value.filter { it.isDigit() }) } },
                label = { Text("休息秒") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = exercise.notes,
            onValueChange = { value -> actions.onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(notes = value) } },
            label = { Text("动作备注") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReadOnlyPlanPreview(preview: AiGeneratedPlanForm) {
    Text(preview.name, style = MaterialTheme.typography.titleLarge)
    Text(
        "${preview.goal} · 每周 ${preview.frequencyPerWeek} 次 · 每次 ${preview.estimatedDurationMinutes} 分钟",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (preview.notes.isNotBlank()) {
        Text(preview.notes, style = MaterialTheme.typography.bodyMedium)
    }
    preview.days.forEach { day ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(day.dayName, style = MaterialTheme.typography.titleMedium)
            day.exercises.forEach { exercise ->
                Text(
                    "${exercise.name} · ${exercise.bodyPart} · ${exercise.sets} 组 x ${exercise.reps} · 休息 ${exercise.restSeconds}s",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (exercise.notes.isNotBlank()) {
                    Text(
                        exercise.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class AiPlanActions(
    val onGoalChange: (String) -> Unit,
    val onDaysChange: (String) -> Unit,
    val onDurationChange: (String) -> Unit,
    val onLevelChange: (String) -> Unit,
    val onEquipmentChange: (String) -> Unit,
    val onLimitationsChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onGenerate: () -> Unit,
    val onToggleEdit: () -> Unit,
    val onSave: () -> Unit,
    val onUpdatePreviewPlan: (AiGeneratedPlanForm.() -> AiGeneratedPlanForm) -> Unit,
    val onUpdatePreviewDay: (Int, String) -> Unit,
    val onUpdatePreviewExercise: (Int, Int, AiExerciseForm.() -> AiExerciseForm) -> Unit
)

@Preview(showBackground = true)
@Composable
private fun AiPlanContentPreview() {
    AiPlanContent(
        state = AiPlanUiState(
            preview = AiGeneratedPlanForm(
                name = "三天新手训练计划",
                goal = "新手入门",
                frequencyPerWeek = "3",
                estimatedDurationMinutes = "45",
                notes = "以动作学习和稳定坚持为主",
                days = listOf(
                    AiWorkoutDayForm(
                        dayName = "Day 1 全身基础",
                        exercises = listOf(
                            AiExerciseForm("深蹲", "腿", "3", "10-12", "", "60", "保持核心稳定")
                        )
                    )
                )
            )
        ),
        actions = AiPlanActions(
            onGoalChange = {},
            onDaysChange = {},
            onDurationChange = {},
            onLevelChange = {},
            onEquipmentChange = {},
            onLimitationsChange = {},
            onNotesChange = {},
            onGenerate = {},
            onToggleEdit = {},
            onSave = {},
            onUpdatePreviewPlan = {},
            onUpdatePreviewDay = { _, _ -> },
            onUpdatePreviewExercise = { _, _, _ -> }
        )
    )
}
