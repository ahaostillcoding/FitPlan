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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
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
        onContinueDraft = viewModel::continueDraft,
        onDiscardDraft = viewModel::discardDraft,
        onStartRest = viewModel::startRestTimer,
        onStopRest = viewModel::stopRestTimer,
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
    onContinueDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onStartRest: (Long, Int) -> Unit,
    onStopRest: () -> Unit,
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
            Text("开始训练", style = MaterialTheme.typography.headlineMedium)
            Text(plan.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "今日训练日：${day.dayName} · 来自当前计划选择",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.progressText,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (state.restSecondsRemaining > 0) {
                Text(
                    "休息倒计时：${state.restSecondsRemaining}s",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (state.restoredFromDraft) {
                Card(modifier = Modifier.padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("发现上次未完成训练", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "可以继续上次进度，也可以放弃草稿重新开始。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onContinueDraft) {
                                Text("继续训练")
                            }
                            OutlinedButton(onClick = onDiscardDraft) {
                                Text("放弃草稿")
                            }
                        }
                    }
                }
            }
            if (state.message != null) {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
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
                activeRestExerciseId = state.activeRestExerciseId,
                restSecondsRemaining = state.restSecondsRemaining,
                onToggleCompleted = onToggleCompleted,
                onExerciseNotes = onExerciseNotes,
                onStartRest = onStartRest,
                onStopRest = onStopRest
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
    activeRestExerciseId: Long?,
    restSecondsRemaining: Int,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onExerciseNotes: (Long, String) -> Unit,
    onStartRest: (Long, Int) -> Unit,
    onStopRest: () -> Unit
) {
    val isResting = activeRestExerciseId == exercise.id && restSecondsRemaining > 0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = progress?.isCompleted == true,
                    onCheckedChange = { onToggleCompleted(exercise.id, it) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("休息 ${exercise.restSeconds}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (exercise.notes.isNotBlank()) Text(exercise.notes)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (isResting) onStopRest() else onStartRest(exercise.id, exercise.restSeconds)
                    },
                    enabled = exercise.restSeconds > 0
                ) {
                    Text(if (isResting) "停止休息 ${restSecondsRemaining}s" else "开始休息")
                }
            }
            OutlinedTextField(
                value = progress?.actualNotes.orEmpty(),
                onValueChange = { onExerciseNotes(exercise.id, it) },
                label = { Text("实际备注") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutSessionContentPreview() {
    WorkoutSessionContent(
        state = WorkoutSessionUiState(
            isLoading = false,
            plan = WorkoutPlan(
                id = 1,
                name = "四天增肌训练计划",
                goal = "增肌",
                frequencyPerWeek = 4,
                estimatedDurationMinutes = 60,
                isActive = true,
                days = emptyList()
            ),
            day = WorkoutDay(
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
                        weight = "20kg",
                        restSeconds = 90,
                        sortOrder = 0
                    )
                )
            ),
            exerciseProgress = mapOf(100L to WorkoutExerciseProgress(100L, true)),
            restoredFromDraft = true,
            activeRestExerciseId = 100L,
            restSecondsRemaining = 45
        ),
        onToggleCompleted = { _, _ -> },
        onExerciseNotes = { _, _ -> },
        onSessionNotes = {},
        onContinueDraft = {},
        onDiscardDraft = {},
        onStartRest = { _, _ -> },
        onStopRest = {},
        onFinish = {}
    )
}
