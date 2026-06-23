package com.fitplan.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.UiState
import com.fitplan.app.ui.common.formatFullDate

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    HistoryContent(
        state = state,
        onRecordClick = onRecordClick,
        onDelete = viewModel::delete,
        modifier = modifier
    )
}

@Composable
fun HistoryContent(
    state: UiState<List<WorkoutRecord>>,
    onRecordClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Empty -> EmptyState(state.message, modifier = modifier)
        is UiState.Error -> ErrorState(state.message, modifier)
        is UiState.Content -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("训练记录", style = MaterialTheme.typography.headlineMedium) }
            items(state.data) { record ->
                Card(onClick = { onRecordClick(record.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(record.planName, style = MaterialTheme.typography.titleLarge)
                        Text("${record.workoutDayName} · ${record.durationMinutes} 分钟")
                        Text(formatFullDate(record.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (record.notes.isNotBlank()) Text(record.notes)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onDelete(record.id) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

