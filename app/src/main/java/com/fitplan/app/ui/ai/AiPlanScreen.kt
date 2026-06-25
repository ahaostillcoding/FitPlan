package com.fitplan.app.ui.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard

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
        onUpdatePreviewExercise = viewModel::updatePreviewExercise,
        modifier = modifier
    )
}

@Composable
private fun AiPlanContent(
    state: AiPlanUiState,
    onGoalChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onEquipmentChange: (String) -> Unit,
    onLimitationsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onUpdatePreviewPlan: (AiGeneratedPlanForm.() -> AiGeneratedPlanForm) -> Unit,
    onUpdatePreviewDay: (Int, String) -> Unit,
    onUpdatePreviewExercise: (Int, Int, AiExerciseForm.() -> AiExerciseForm) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(title = "AI 生成计划", subtitle = "生成结果先预览，不会覆盖已有计划") }
        item {
            SectionCard {
                Text("训练需求", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("健身目标", fontWeight = FontWeight.Bold)
                ChipRow(GoalOptions, state.goal, onGoalChange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.daysPerWeek,
                        onValueChange = onDaysChange,
                        label = { Text("每周天数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.durationMinutes,
                        onValueChange = onDurationChange,
                        label = { Text("每次分钟") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Text("经验水平", fontWeight = FontWeight.Bold)
                ChipRow(LevelOptions, state.experienceLevel, onLevelChange)
                OutlinedTextField(
                    value = state.equipment,
                    onValueChange = onEquipmentChange,
                    label = { Text("可用器械") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = state.limitations,
                    onValueChange = onLimitationsChange,
                    label = { Text("避开动作或受限部位") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    label = { Text("其他备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Button(onClick = onGenerate, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        Text("生成中...")
                    } else {
                        Text(if (state.preview == null) "生成并预览" else "重新生成")
                    }
                }
                Text(
                    "未配置 API Key 时会提示去设置；手动计划和历史记录可离线使用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        state.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        state.message?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        state.preview?.let { preview ->
            item {
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("生成结果预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("确认后才会保存为新计划", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = onToggleEdit) {
                            Text(if (state.isEditingPreview) "完成编辑" else "编辑")
                        }
                    }
                    if (state.isEditingPreview) {
                        EditablePreview(
                            preview = preview,
                            onUpdatePreviewPlan = onUpdatePreviewPlan,
                            onUpdatePreviewDay = onUpdatePreviewDay,
                            onUpdatePreviewExercise = onUpdatePreviewExercise
                        )
                    } else {
                        ReadOnlyPreview(preview)
                    }
                    Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isSaving) "保存中..." else "保存计划")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun ReadOnlyPreview(preview: AiGeneratedPlanForm) {
    Text(preview.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("${preview.goal} · 每周 ${preview.frequencyPerWeek} 次 · ${preview.estimatedDurationMinutes} 分钟")
    if (preview.notes.isNotBlank()) Text(preview.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
    preview.days.forEach { day ->
        SectionCard {
            Text(day.dayName, fontWeight = FontWeight.Bold)
            day.exercises.forEach { exercise ->
                Text("${exercise.name} · ${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次")
            }
        }
    }
}

@Composable
private fun EditablePreview(
    preview: AiGeneratedPlanForm,
    onUpdatePreviewPlan: (AiGeneratedPlanForm.() -> AiGeneratedPlanForm) -> Unit,
    onUpdatePreviewDay: (Int, String) -> Unit,
    onUpdatePreviewExercise: (Int, Int, AiExerciseForm.() -> AiExerciseForm) -> Unit
) {
    OutlinedTextField(
        value = preview.name,
        onValueChange = { value -> onUpdatePreviewPlan { copy(name = value) } },
        label = { Text("计划名称") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = preview.goal,
        onValueChange = { value -> onUpdatePreviewPlan { copy(goal = value) } },
        label = { Text("目标") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = preview.frequencyPerWeek,
            onValueChange = { value -> onUpdatePreviewPlan { copy(frequencyPerWeek = value.filter { it.isDigit() }) } },
            label = { Text("每周次数") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = preview.estimatedDurationMinutes,
            onValueChange = { value -> onUpdatePreviewPlan { copy(estimatedDurationMinutes = value.filter { it.isDigit() }) } },
            label = { Text("每次分钟") },
            modifier = Modifier.weight(1f)
        )
    }
    preview.days.forEachIndexed { dayIndex, day ->
        SectionCard {
            OutlinedTextField(
                value = day.dayName,
                onValueChange = { onUpdatePreviewDay(dayIndex, it) },
                label = { Text("训练日名称") },
                modifier = Modifier.fillMaxWidth()
            )
            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                OutlinedTextField(
                    value = exercise.name,
                    onValueChange = { value -> onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(name = value) } },
                    label = { Text("动作名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = exercise.bodyPart,
                        onValueChange = { value -> onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(bodyPart = value) } },
                        label = { Text("部位") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = exercise.sets,
                        onValueChange = { value -> onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(sets = value.filter { it.isDigit() }) } },
                        label = { Text("组数") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = exercise.reps,
                        onValueChange = { value -> onUpdatePreviewExercise(dayIndex, exerciseIndex) { copy(reps = value) } },
                        label = { Text("次数") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
