package com.fitplan.app.ui.plans

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
import androidx.compose.material3.OutlinedTextField
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
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.ConfirmDialog
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
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
    var pendingDeletePlan by remember { mutableStateOf<WorkoutPlan?>(null) }

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
        onDeleteRequest = { pendingDeletePlan = it },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    pendingDeletePlan?.let { plan ->
        ConfirmDialog(
            title = "删除计划？",
            message = "将删除「${plan.name}」及其中的训练日和动作。历史训练记录会保留快照。",
            confirmText = "确认删除",
            onConfirm = {
                viewModel.delete(plan.id)
                pendingDeletePlan = null
            },
            onDismiss = { pendingDeletePlan = null }
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
        is UiState.Empty -> EmptyState(
            title = state.message,
            actionText = "新建计划",
            onAction = onNewPlan,
            modifier = modifier,
            description = "先创建一份计划，首页就能快速看到今天练什么。"
        )
        is UiState.Content -> {
            var query by remember { mutableStateOf("") }
            val filteredPlans = remember(query, state.data) {
                val keyword = query.trim()
                if (keyword.isBlank()) {
                    state.data
                } else {
                    state.data.filter { plan ->
                        plan.name.contains(keyword, ignoreCase = true) ||
                            plan.goal.contains(keyword, ignoreCase = true) ||
                            plan.days.any { it.dayName.contains(keyword, ignoreCase = true) }
                    }
                }
            }
            Box(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ScreenHeader(
                            title = "训练计划",
                            subtitle = "我的计划",
                            action = {
                                Button(onClick = onNewPlan) {
                                    Text("新建")
                                }
                            }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("搜索计划名称、训练日或目标") }
                        )
                    }
                    if (filteredPlans.isEmpty()) {
                        item {
                            EmptyState(
                                title = "没有找到相关计划",
                                description = "换个关键词，或直接新建一份训练计划。",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(filteredPlans) { plan ->
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
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plan.name, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${plan.goal} · 每周 ${plan.frequencyPerWeek} 次 · ${plan.estimatedDurationMinutes} 分钟",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (plan.isActive) {
                AssistChip(onClick = {}, label = { Text("当前") })
            }
        }
        Text(
            plan.days.take(4).joinToString(" · ") { it.dayName }.ifBlank { "还没有训练日" },
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onClick, modifier = Modifier.weight(1f)) {
                Text("查看详情")
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text("编辑")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSetActive, enabled = !plan.isActive, modifier = Modifier.weight(1f)) {
                Text("设为当前")
            }
            OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) {
                Text("复制")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text("删除")
            }
        }
    }
}
