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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
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
    val pendingDeleteRecord = remember { mutableStateOf<WorkoutRecord?>(null) }

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
        onDeleteRequest = { pendingDeleteRecord.value = it },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    pendingDeleteRecord.value?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord.value = null },
            title = { Text("删除训练记录？") },
            text = { Text("将删除「${record.planName} - ${record.workoutDayName}」这条记录。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(record.id)
                        pendingDeleteRecord.value = null
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteRecord.value = null }) {
                    Text("取消")
                }
            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("训练记录", style = MaterialTheme.typography.headlineMedium) }
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
                    Card(onClick = { onRecordClick(record.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(record.planName, style = MaterialTheme.typography.titleLarge)
                            Text("${record.workoutDayName} · ${record.durationMinutes} 分钟")
                            Text(formatFullDate(record.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (record.notes.isNotBlank()) Text(record.notes)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onDeleteRequest(record) }) { Text("删除") }
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
