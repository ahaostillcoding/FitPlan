package com.fitplan.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.EmptyState
import com.fitplan.app.ui.common.formatShortDate
import com.fitplan.app.ui.theme.FitPlanTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: (Long, Long) -> Unit,
    onNewPlan: () -> Unit,
    onAiPlan: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    HomeContent(
        state = state,
        onStartWorkout = onStartWorkout,
        onSelectWorkoutDay = viewModel::selectWorkoutDay,
        onNewPlan = onNewPlan,
        onAiPlan = onAiPlan,
        onHistory = onHistory,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStartWorkout: (Long, Long) -> Unit,
    onSelectWorkoutDay: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onAiPlan: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plan = state.activePlan
    if (plan == null) {
        EmptyState(
            title = "今天还没有训练计划",
            actionText = "新建计划",
            onAction = onNewPlan,
            modifier = modifier
        )
        return
    }

    val todayDay = state.todayDay
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("今天练什么", style = MaterialTheme.typography.headlineMedium)
            Text(
                "本周已练 ${state.weeklyWorkoutCount} 次 · 最近一次 ${formatShortDate(state.lastWorkoutAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleLarge)
                    Text("目标：${plan.goal}")
                    Text("预计 ${plan.estimatedDurationMinutes} 分钟 · ${todayDay?.exercises?.size ?: 0} 个动作")
                    if (todayDay != null) {
                        Text(
                            "今日默认：${todayDay.dayName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (plan.days.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                plan.days.forEach { day ->
                                    FilterChip(
                                        selected = day.id == todayDay.id,
                                        onClick = { onSelectWorkoutDay(day.id) },
                                        label = { Text(day.dayName) }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "当前计划还没有训练日，请先编辑计划。",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = todayDay != null,
                            onClick = { if (todayDay != null) onStartWorkout(plan.id, todayDay.id) }
                        ) {
                            Text("开始训练")
                        }
                        OutlinedButton(onClick = onNewPlan) { Text("新建计划") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAiPlan) { Text("AI 生成计划") }
                        OutlinedButton(onClick = onHistory) { Text("查看历史记录") }
                    }
                }
            }
        }
        if (todayDay != null) {
            items(todayDay.exercises.take(5)) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · 休息 ${exercise.restSeconds}s",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    FitPlanTheme {
        HomeContent(
            state = HomeUiState(
                activePlan = WorkoutPlan(
                    id = 1,
                    name = "四天增肌计划",
                    goal = "增肌",
                    frequencyPerWeek = 4,
                    estimatedDurationMinutes = 60,
                    isActive = true,
                    days = listOf(
                        WorkoutDay(
                            id = 1,
                            dayName = "Day 1 胸肩三头",
                            sortOrder = 0,
                            exercises = listOf(
                                Exercise(
                                    name = "杠铃卧推",
                                    bodyPart = "胸",
                                    sets = 4,
                                    reps = "8-10",
                                    restSeconds = 90,
                                    sortOrder = 0
                                )
                            )
                        )
                    )
                ),
                weeklyWorkoutCount = 2
            ),
            onStartWorkout = { _, _ -> },
            onSelectWorkoutDay = {},
            onNewPlan = {},
            onAiPlan = {},
            onHistory = {}
        )
    }
}
