package com.fitplan.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState

@Composable
fun WorkoutSessionScreen(
    viewModel: WorkoutSessionViewModel,
    onFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.finishedRecordId) {
        state.finishedRecordId?.let(onFinished)
    }
    WorkoutSessionContent(
        state = state,
        onToggleCompleted = viewModel::updateCompleted,
        onExerciseNotes = viewModel::updateExerciseNotes,
        onSessionNotes = viewModel::updateSessionNotes,
        onFinish = viewModel::finishWorkout,
        modifier = modifier
    )
}

@Composable
private fun WorkoutSessionContent(
    state: WorkoutSessionUiState,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onExerciseNotes: (Long, String) -> Unit,
    onSessionNotes: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    if (state.errorMessage != null && state.plan == null) {
        ErrorState(state.errorMessage, modifier)
        return
    }
    val plan = state.plan ?: return
    val day = state.day ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(plan.name, style = MaterialTheme.typography.headlineMedium)
            Text(day.dayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.errorMessage != null) {
                Text(
                    state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        items(day.exercises) { exercise ->
            WorkoutExerciseCard(
                exercise = exercise,
                progress = state.exerciseProgress[exercise.id],
                onToggleCompleted = onToggleCompleted,
                onExerciseNotes = onExerciseNotes
            )
        }
        item {
            OutlinedTextField(
                value = state.sessionNotes,
                onValueChange = onSessionNotes,
                label = { Text("本次训练备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "保存中..." else "完成训练")
            }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: Exercise,
    progress: WorkoutExerciseProgress?,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onExerciseNotes: (Long, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = progress?.isCompleted == true,
                    onCheckedChange = { onToggleCompleted(exercise.id, it) }
                )
                Column {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("休息 ${exercise.restSeconds}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (exercise.notes.isNotBlank()) Text(exercise.notes)
            OutlinedTextField(
                value = progress?.actualNotes.orEmpty(),
                onValueChange = { onExerciseNotes(exercise.id, it) },
                label = { Text("实际备注") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

