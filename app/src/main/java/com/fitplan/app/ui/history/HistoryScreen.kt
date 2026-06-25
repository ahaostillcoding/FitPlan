package com.fitplan.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.ConfirmDialog
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.common.formatFullDate

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteRecord by remember { mutableStateOf<WorkoutRecord?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    HistoryContent(
        state = state,
        onRecordClick = onRecordClick,
        onFilterSelected = viewModel::selectFilter,
        onDeleteRequest = { pendingDeleteRecord = it },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    pendingDeleteRecord?.let { record ->
        ConfirmDialog(
            title = "删除训练记录？",
            message = "删除后无法恢复，但不会影响训练计划。",
            confirmText = "确认删除",
            onConfirm = {
                viewModel.delete(record.id)
                pendingDeleteRecord = null
            },
            onDismiss = { pendingDeleteRecord = null }
        )
    }
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onRecordClick: (Long) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onDeleteRequest: (WorkoutRecord) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingState(modifier)
        state.errorMessage != null -> ErrorState(state.errorMessage, modifier)
        else -> Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { ScreenHeader(title = "历史记录", subtitle = "训练回顾") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { onFilterSelected(filter) },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }
                if (state.records.isEmpty()) {
                    item {
                        EmptyState(state.emptyMessage, modifier = Modifier.fillMaxWidth())
                    }
                }
                items(state.records) { record ->
                    SectionCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.workoutDayName, fontWeight = FontWeight.Bold)
                                Text(record.planName, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${record.durationMinutes} 分钟", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        }
                        Text(formatFullDate(record.date), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        if (record.notes.isNotBlank()) Text(record.notes)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { onRecordClick(record.id) }, modifier = Modifier.weight(1f)) {
                                Text("详情")
                            }
                            OutlinedButton(onClick = { onDeleteRequest(record) }, modifier = Modifier.weight(1f)) {
                                Text("删除")
                            }
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
