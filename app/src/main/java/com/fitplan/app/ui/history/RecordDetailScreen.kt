package com.fitplan.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.ErrorState
import com.fitplan.app.ui.common.LoadingState
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.common.UiState
import com.fitplan.app.ui.common.formatFullDate

@Composable
fun RecordDetailScreen(
    viewModel: RecordDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    when (val currentState = state) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Empty -> EmptyState(currentState.message, "返回", onBack, modifier)
        is UiState.Error -> ErrorState(currentState.message, modifier)
        is UiState.Content -> RecordDetailContent(currentState.data, onBack, modifier)
    }
}

@Composable
private fun RecordDetailContent(
    record: WorkoutRecord,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = record.workoutDayName,
                subtitle = "训练记录详情",
                action = {
                    OutlinedButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        }
        item {
            SectionCard {
                Text(record.planName, fontWeight = FontWeight.Bold)
                Text("${formatFullDate(record.date)} · ${record.durationMinutes} 分钟")
                if (record.notes.isNotBlank()) Text(record.notes)
            }
        }
        item {
            Text("动作快照", fontWeight = FontWeight.Bold)
        }
        items(record.exercises) { exercise ->
            SectionCard {
                AssistChip(
                    onClick = {},
                    label = { Text(if (exercise.isCompleted) "已完成" else "未完成") }
                )
                Text(exercise.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · ${exercise.weight.ifBlank { "自选重量" }} · 休息 ${exercise.restSeconds}s",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.plannedNotes.isNotBlank()) {
                    Text("计划备注：${exercise.plannedNotes}", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (exercise.actualNotes.isNotBlank()) {
                    Text("实际备注：${exercise.actualNotes}")
                }
            }
        }
    }
}
