package com.fitplan.app.ui.editPlan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (state.planId == null) "新建训练计划" else "编辑训练计划",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        if (state.errorMessage != null) {
            item { ErrorState(state.errorMessage, Modifier.fillMaxWidth()) }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("计划名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.goal,
                        onValueChange = viewModel::updateGoal,
                        label = { Text("训练目标") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.frequencyPerWeek,
                            onValueChange = viewModel::updateFrequency,
                            label = { Text("每周次数") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.estimatedDurationMinutes,
                            onValueChange = viewModel::updateDuration,
                            label = { Text("每次分钟") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.isActive, onCheckedChange = viewModel::updateActive)
                        Text("设为当前计划")
                    }
                }
            }
        }
        itemsIndexed(state.days) { dayIndex, day ->
            WorkoutDayFormCard(
                dayIndex = dayIndex,
                day = day,
                viewModel = viewModel
            )
        }
        item {
            OutlinedButton(onClick = viewModel::addDay, modifier = Modifier.fillMaxWidth()) {
                Text("添加训练日")
            }
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "保存中..." else "保存计划")
            }
        }
    }
}

@Composable
private fun WorkoutDayFormCard(
    dayIndex: Int,
    day: WorkoutDayForm,
    viewModel: EditPlanViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("训练日 ${dayIndex + 1}", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.moveDayUp(dayIndex) }) { Text("上移") }
                    OutlinedButton(onClick = { viewModel.moveDayDown(dayIndex) }) { Text("下移") }
                    OutlinedButton(onClick = { viewModel.removeDay(dayIndex) }) { Text("删除") }
                }
            }
            OutlinedTextField(
                value = day.dayName,
                onValueChange = { viewModel.updateDayName(dayIndex, it) },
                label = { Text("训练日名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                ExerciseFormFields(
                    dayIndex = dayIndex,
                    exerciseIndex = exerciseIndex,
                    exercise = exercise,
                    viewModel = viewModel
                )
            }
            OutlinedButton(onClick = { viewModel.addExercise(dayIndex) }) {
                Text("添加动作")
            }
        }
    }
}

@Composable
private fun ExerciseFormFields(
    dayIndex: Int,
    exerciseIndex: Int,
    exercise: ExerciseForm,
    viewModel: EditPlanViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("动作 ${exerciseIndex + 1}", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.moveExerciseUp(dayIndex, exerciseIndex) }) { Text("上移") }
                OutlinedButton(onClick = { viewModel.moveExerciseDown(dayIndex, exerciseIndex) }) { Text("下移") }
                OutlinedButton(onClick = { viewModel.removeExercise(dayIndex, exerciseIndex) }) { Text("删除") }
            }
        }
        OutlinedTextField(
            value = exercise.name,
            onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(name = value) } },
            label = { Text("动作名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = exercise.bodyPart,
            onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(bodyPart = value) } },
            label = { Text("训练部位") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercise.sets,
                onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(sets = value.filter { it.isDigit() }) } },
                label = { Text("组数") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = exercise.notes,
            onValueChange = { value -> viewModel.updateExercise(dayIndex, exerciseIndex) { copy(notes = value) } },
            label = { Text("动作备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1
        )
    }
}
