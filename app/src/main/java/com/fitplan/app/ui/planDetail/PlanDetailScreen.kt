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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.ConfirmDialog
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.common.StatCard
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
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
        onDelete = { showDeleteConfirm = true },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除计划？",
            message = "删除后该计划无法恢复，历史训练记录会保留动作快照。",
            confirmText = "确认删除",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete(onBack)
            },
            onDismiss = { showDeleteConfirm = false }
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ScreenHeader(
                            title = plan.name,
                            subtitle = if (plan.isActive) "当前计划" else "计划详情",
                            action = {
                                OutlinedButton(onClick = onBack) {
                                    Text("返回")
                                }
                            }
                        )
                    }
                    item {
                        SectionCard {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = {}, label = { Text(plan.goal) })
                                if (plan.isActive) AssistChip(onClick = {}, label = { Text("当前") })
                            }
                            if (plan.notes.isNotBlank()) {
                                Text(plan.notes, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCard("频率", "${plan.frequencyPerWeek}", Modifier.weight(1f), "天/周")
                                StatCard("时长", "${plan.estimatedDurationMinutes}", Modifier.weight(1f), "分钟")
                                StatCard("训练日", "${plan.days.size}", Modifier.weight(1f), "个")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onEdit(plan.id) }, modifier = Modifier.weight(1f)) {
                                    Text("编辑")
                                }
                                OutlinedButton(onClick = onSetActive, enabled = !plan.isActive, modifier = Modifier.weight(1f)) {
                                    Text("设为当前")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) {
                                    Text("复制计划")
                                }
                                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                                    Text("删除")
                                }
                            }
                        }
                    }
                    if (plan.days.isEmpty()) {
                        item {
                            SectionCard {
                                Text("这个计划还没有训练日", fontWeight = FontWeight.Bold)
                                Text("请先编辑计划，补充训练日和动作后再开始训练。", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(plan.days) { day ->
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(day.dayName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${day.exercises.size} 个动作 · 按执行顺序排列",
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(onClick = { onStartWorkout(plan.id, day.id) }) {
                                    Text("开始")
                                }
                            }
                            day.exercises.forEach { ExerciseRow(it) }
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(exercise.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }} · 休息 ${exercise.restSeconds}s",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (exercise.notes.isNotBlank()) {
            Text(exercise.notes, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
