package com.fitplan.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
fun RecordDetailScreen(
    viewModel: RecordDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val currentState = state
    when (currentState) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Empty -> EmptyState(currentState.message, "返回", onBack, modifier)
        is UiState.Error -> ErrorState(currentState.message, modifier)
        is UiState.Content -> RecordDetailContent(currentState.data, modifier)
    }
}

@Composable
private fun RecordDetailContent(
    record: WorkoutRecord,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(record.planName, style = MaterialTheme.typography.headlineMedium)
            Text(record.workoutDayName)
            Text("${formatFullDate(record.date)} · ${record.durationMinutes} 分钟")
            if (record.notes.isNotBlank()) Text(record.notes, modifier = Modifier.padding(top = 8.dp))
        }
        items(record.exercises) { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (exercise.isCompleted) "已完成 · ${exercise.name}" else "未完成 · ${exercise.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (exercise.actualNotes.isNotBlank()) Text("备注：${exercise.actualNotes}")
                }
            }
        }
    }
}
