package com.fitplan.app.ui.planDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.UiState

@Composable
fun PlanDetailScreen(
    viewModel: PlanDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStartWorkout: (Long, Long) -> Unit,
    onDuplicated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    PlanDetailContent(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onStartWorkout = onStartWorkout,
        onSetActive = viewModel::setActive,
        onDuplicate = { viewModel.duplicate(onDuplicated) },
        onDelete = { showDeleteConfirm.value = true },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text("删除计划？") },
            text = { Text("删除后该计划无法恢复，历史训练记录会保留快照。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm.value = false
                        viewModel.delete(onBack)
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm.value = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun PlanDetailContent(
    state: UiState<WorkoutPlan>,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStartWorkout: (Long, Long) -> Unit,
    onSetActive: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Empty -> EmptyState(state.message, "返回", onBack, modifier)
        is UiState.Error -> ErrorState(state.message, modifier)
        is UiState.Content -> {
            val plan = state.data
            Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(plan.name, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${plan.goal} · 每周 ${plan.frequencyPerWeek} 次 · ${plan.estimatedDurationMinutes} 分钟",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (plan.isActive) {
                            Text("当前计划", color = MaterialTheme.colorScheme.primary)
                        }
                        if (plan.notes.isNotBlank()) {
                            Text(plan.notes, modifier = Modifier.padding(top = 8.dp))
                        }
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { onEdit(plan.id) }) { Text("编辑") }
                            OutlinedButton(onClick = onSetActive, enabled = !plan.isActive) { Text("设为当前") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDuplicate) { Text("复制") }
                            OutlinedButton(onClick = onDelete) { Text("删除") }
                        }
                    }
                    if (plan.days.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "这个计划还没有训练日，请编辑计划后再开始训练。",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(plan.days) { day ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(day.dayName, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            "按动作顺序执行",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(onClick = { onStartWorkout(plan.id, day.id) }) { Text("开始") }
                                }
                                day.exercises.forEach { ExerciseRow(it) }
                            }
                        }
                    }
                }
                if (snackbarHostState != null) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise) {
    Column {
        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }} · 休息 ${exercise.restSeconds}s",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (exercise.notes.isNotBlank()) {
            Text(exercise.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
