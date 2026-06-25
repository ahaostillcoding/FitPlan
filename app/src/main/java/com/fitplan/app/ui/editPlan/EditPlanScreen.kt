package com.fitplan.app.ui.editPlan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
import kotlinx.coroutines.launch

private val GoalOptions = listOf("增肌", "减脂", "塑形", "力量", "耐力")

@Composable
fun EditPlanScreen(
    viewModel: EditPlanViewModel,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.savedPlanId) {
        state.savedPlanId?.let(onSaved)
    }
    EditPlanContent(
        state = state,
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
private fun EditPlanContent(
    state: EditPlanUiState,
    viewModel: EditPlanViewModel,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedDayIndex by remember(state.days.size) { mutableIntStateOf(0) }
    selectedDayIndex = selectedDayIndex.coerceIn(0, state.days.lastIndex.coerceAtLeast(0))
    val selectedDay = state.days.getOrNull(selectedDayIndex)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(
                title = if (state.planId == null) "新建训练计划" else "编辑训练计划",
                subtitle = "先搭好计划框架，再逐日补动作"
            )

            if (state.errorMessage != null) {
                ErrorState(state.errorMessage, Modifier.fillMaxWidth())
            }

            PlanBasicsCard(state = state, viewModel = viewModel)
            WorkoutDayTabs(
                days = state.days,
                selectedDayIndex = selectedDayIndex,
                onSelect = { selectedDayIndex = it },
                onAddDay = {
                    viewModel.addDay()
                    selectedDayIndex = state.days.size
                }
            )

            if (selectedDay != null) {
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("当前训练日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${selectedDay.exercises.size} 个动作 · 按执行顺序排列",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { viewModel.addExercise(selectedDayIndex) }) {
                            Text("+ 动作")
                        }
                    }
                    OutlinedTextField(
                        value = selectedDay.dayName,
                        onValueChange = { viewModel.updateDayName(selectedDayIndex, it) },
                        label = { Text("训练日名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.moveDayUp(selectedDayIndex) }, enabled = selectedDayIndex > 0) {
                            Text("上移")
                        }
                        OutlinedButton(onClick = { viewModel.moveDayDown(selectedDayIndex) }, enabled = selectedDayIndex < state.days.lastIndex) {
                            Text("下移")
                        }
                        OutlinedButton(onClick = { viewModel.removeDay(selectedDayIndex) }, enabled = state.days.size > 1) {
                            Text("删除训练日")
                        }
                    }
                    selectedDay.exercises.forEachIndexed { exerciseIndex, exercise ->
                        ExerciseEditorCard(
                            dayIndex = selectedDayIndex,
                            exerciseIndex = exerciseIndex,
                            exercise = exercise,
                            canDelete = selectedDay.exercises.size > 1,
                            viewModel = viewModel
                        )
                    }
                    OutlinedButton(onClick = { viewModel.addExercise(selectedDayIndex) }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ 添加动作")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val name = state.name.ifBlank { "未命名计划" }
                    val days = state.days.size
                    val exercises = state.days.sumOf { it.exercises.size }
                    scope.launch {
                        snackbarHostState.showSnackbar("$name：$days 个训练日，$exercises 个动作")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("预览计划")
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1.2f)
            ) {
                Text(if (state.isSaving) "保存中..." else "保存计划")
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PlanBasicsCard(
    state: EditPlanUiState,
    viewModel: EditPlanViewModel
) {
    SectionCard {
        Text("计划基础", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::updateName,
            label = { Text("计划名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text("训练目标", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            GoalOptions.forEach { goal ->
                FilterChip(
                    selected = state.goal == goal,
                    onClick = { viewModel.updateGoal(goal) },
                    label = { Text(goal) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatInputCard(
                label = "每周次数",
                value = state.frequencyPerWeek,
                onValueChange = viewModel::updateFrequency,
                modifier = Modifier.weight(1f)
            )
            StatInputCard(
                label = "每次分钟",
                value = state.estimatedDurationMinutes,
                onValueChange = viewModel::updateDuration,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::updateNotes,
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("设为当前计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("首页会优先展示这个计划", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.isActive, onCheckedChange = viewModel::updateActive)
        }
    }
}

@Composable
private fun StatInputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
private fun WorkoutDayTabs(
    days: List<WorkoutDayForm>,
    selectedDayIndex: Int,
    onSelect: (Int) -> Unit,
    onAddDay: () -> Unit
) {
    SectionCard {
        Text("训练日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            days.forEachIndexed { index, day ->
                FilterChip(
                    selected = index == selectedDayIndex,
                    onClick = { onSelect(index) },
                    label = {
                        Text(day.dayName.ifBlank { "Day ${index + 1}" })
                    }
                )
            }
            FilterChip(
                selected = false,
                onClick = onAddDay,
                label = { Text("+ 添加") }
            )
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    dayIndex: Int,
    exerciseIndex: Int,
    exercise: ExerciseForm,
    canDelete: Boolean,
    viewModel: EditPlanViewModel
) {
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("动作 ${exerciseIndex + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (exerciseIndex == 0) "主要动作" else "辅助动作", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = exercise.name,
            onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(name = value) } },
            label = { Text("动作名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = exercise.bodyPart,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(bodyPart = value) } },
                label = { Text("部位") },
                modifier = Modifier.weight(1.2f),
                singleLine = true
            )
            OutlinedTextField(
                value = exercise.sets,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(sets = value.filter { it.isDigit() }) } },
                label = { Text("组数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = exercise.reps,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(reps = value) } },
                label = { Text("次数") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = exercise.weight,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(weight = value) } },
                label = { Text("重量") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = exercise.restSeconds,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(restSeconds = value.filter { it.isDigit() }) } },
                label = { Text("休息秒") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = exercise.notes,
            onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(notes = value) } },
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { viewModel.moveExerciseUp(dayIndex, exerciseIndex) }, enabled = exerciseIndex > 0) {
                Text("上移")
            }
            TextButton(onClick = { viewModel.moveExerciseDown(dayIndex, exerciseIndex) }) {
                Text("下移")
            }
            TextButton(onClick = { viewModel.removeExercise(dayIndex, exerciseIndex) }, enabled = canDelete) {
                Text("删除")
            }
        }
    }
}
