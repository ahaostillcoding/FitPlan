package com.fitplan.app.ui.plans

import androidx.compose.foundation.layout.Arrangement
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
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.UiState

@Composable
fun PlansScreen(
    viewModel: PlansViewModel,
    onNewPlan: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onEditPlan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDeletePlan = remember { mutableStateOf<WorkoutPlan?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    PlansContent(
        state = state,
        onNewPlan = onNewPlan,
        onPlanClick = onPlanClick,
        onEditPlan = onEditPlan,
        onSetActive = viewModel::setActive,
        onDuplicate = viewModel::duplicate,
        onDeleteRequest = { pendingDeletePlan.value = it },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    pendingDeletePlan.value?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlan.value = null },
            title = { Text("删除计划？") },
            text = { Text("将删除「${plan.name}」及其中的训练日和动作。此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(plan.id)
                        pendingDeletePlan.value = null
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeletePlan.value = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun PlansContent(
    state: UiState<List<WorkoutPlan>>,
    onNewPlan: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onEditPlan: (Long) -> Unit,
    onSetActive: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDeleteRequest: (WorkoutPlan) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Error -> ErrorState(state.message, modifier)
        is UiState.Empty -> EmptyState(state.message, "新建计划", onNewPlan, modifier)
        is UiState.Content -> {
            androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("训练计划", style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = onNewPlan) { Text("新建") }
                        }
                    }
                    items(state.data) { plan ->
                        PlanCard(
                            plan = plan,
                            onClick = { onPlanClick(plan.id) },
                            onEdit = { onEditPlan(plan.id) },
                            onSetActive = { onSetActive(plan.id) },
                            onDuplicate = { onDuplicate(plan.id) },
                            onDelete = { onDeleteRequest(plan) }
                        )
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
private fun PlanCard(
    plan: WorkoutPlan,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${plan.goal} · 每周 ${plan.frequencyPerWeek} 次 · ${plan.estimatedDurationMinutes} 分钟",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(if (plan.isActive) "当前计划" else "未设为当前计划")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("编辑") }
                OutlinedButton(onClick = onSetActive, enabled = !plan.isActive) { Text("设为当前") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDuplicate) { Text("复制") }
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}
