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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.ui.common.ConfirmDialog
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard

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
        onConfirmIncompleteFinish = viewModel::confirmFinishWorkout,
        onCancelIncompleteFinish = viewModel::cancelIncompleteFinish,
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
    onConfirmIncompleteFinish: () -> Unit,
    onCancelIncompleteFinish: () -> Unit,
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
    val progress = if (state.totalExerciseCount == 0) {
        0f
    } else {
        state.completedExerciseCount.toFloat() / state.totalExerciseCount.toFloat()
    }
    if (state.showIncompleteFinishConfirm) {
        ConfirmDialog(
            title = "仍有动作未完成",
            message = "还有 ${state.incompleteExerciseCount} 个动作未勾选完成。你可以继续训练，也可以仍然保存本次记录。",
            confirmText = "仍然保存",
            dismissText = "继续训练",
            onConfirm = onConfirmIncompleteFinish,
            onDismiss = onCancelIncompleteFinish
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(title = day.dayName, subtitle = "当前计划 · ${plan.name}")
        }
        item {
            SectionCard {
                Text("训练进行中", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("完成动作会实时更新进度；结束后保存动作快照，不受后续计划编辑影响。")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.progressText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("预计 ${plan.estimatedDurationMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                if (state.restSecondsRemaining > 0) {
                    Text("休息倒计时：${state.restSecondsRemaining}s", color = MaterialTheme.colorScheme.primary)
                }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (state.restoredFromDraft) {
            item {
                SectionCard {
                    Text("发现上次未完成训练", fontWeight = FontWeight.Bold)
                    Text("可以继续上次进度，也可以放弃草稿重新开始。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onContinueDraft, modifier = Modifier.weight(1f)) {
                            Text("继续训练")
                        }
                        OutlinedButton(onClick = onDiscardDraft, modifier = Modifier.weight(1f)) {
                            Text("放弃草稿")
                        }
                    }
                }
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
            SectionCard {
                OutlinedTextField(
                    value = state.sessionNotes,
                    onValueChange = onSessionNotes,
                    label = { Text("训练备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text(if (state.isSaving) "保存中..." else "完成训练")
                }
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
    val completed = progress?.isCompleted == true
    val isResting = activeRestExerciseId == exercise.id && restSecondsRemaining > 0
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = completed,
                onCheckedChange = { onToggleCompleted(exercise.id, it) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("休息 ${exercise.restSeconds}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (exercise.notes.isNotBlank()) {
            Text(exercise.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = {
                if (isResting) onStopRest() else onStartRest(exercise.id, exercise.restSeconds)
            },
            enabled = exercise.restSeconds > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isResting) "停止休息 ${restSecondsRemaining}s" else "开始休息")
        }
        OutlinedTextField(
            value = progress?.actualNotes.orEmpty(),
            onValueChange = { onExerciseNotes(exercise.id, it) },
            label = { Text("实际备注") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
