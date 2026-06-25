package com.fitplan.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.common.StatCard
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
        onCreateSamplePlan = viewModel::createSamplePlan,
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
    onCreateSamplePlan: () -> Unit,
    onNewPlan: () -> Unit,
    onAiPlan: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plan = state.activePlan
    if (plan == null) {
        EmptyHomeContent(
            state = state,
            onCreateSamplePlan = onCreateSamplePlan,
            onNewPlan = onNewPlan,
            onAiPlan = onAiPlan,
            modifier = modifier
        )
        return
    }

    val todayDay = state.todayDay
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = todayDay?.dayName ?: "还没有训练日",
                subtitle = "今天练什么",
                action = {
                    OutlinedButton(onClick = onNewPlan) {
                        Text("新建")
                    }
                }
            )
        }
        state.message?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        state.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        item {
            SectionCard {
                AssistChip(onClick = {}, label = { Text("当前计划") })
                Text(plan.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "来源：当前计划的第一个训练日。也可以在下方切换训练日。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("目标", plan.goal, Modifier.weight(1f))
                    StatCard("时长", "${plan.estimatedDurationMinutes}", Modifier.weight(1f), "分钟")
                    StatCard("动作", "${todayDay?.exercises?.size ?: 0}", Modifier.weight(1f), "个")
                }
                if (todayDay == null) {
                    Text("当前计划还没有训练日，请先编辑计划后再开始训练。", color = MaterialTheme.colorScheme.error)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onStartWorkout(plan.id, todayDay.id) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "开始今天的训练" }
                        ) {
                            Text("开始训练")
                        }
                        OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) {
                            Text("查看记录")
                        }
                    }
                }
            }
        }
        if (plan.days.size > 1 && todayDay != null) {
            item {
                SectionCard {
                    Text("切换训练日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        plan.days.forEach { day ->
                            FilterChip(
                                selected = day.id == todayDay.id,
                                onClick = { onSelectWorkoutDay(day.id) },
                                label = { Text(day.dayName) },
                                modifier = Modifier.semantics { contentDescription = "选择训练日 ${day.dayName}" }
                            )
                        }
                    }
                    Text(
                        "切换后会作为首页默认训练日保存在本机。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("本周训练", "${state.weeklyWorkoutCount}", Modifier.weight(1f), "次")
                StatCard("近 7 天", "${state.recent7DayWorkoutCount}", Modifier.weight(1f), "${state.recent7DayDurationMinutes} 分钟")
            }
        }
        item {
            SectionCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("最近一次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onHistory) {
                        Text("历史")
                    }
                }
                Text(
                    text = state.lastWorkoutAt?.let { "最近训练：${formatShortDate(it)}" } ?: "还没有完成过训练，今天可以开个好头。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (todayDay != null) {
            item {
                Text("动作预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(todayDay.exercises.take(5)) { exercise ->
                SectionCard {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${exercise.bodyPart} · ${exercise.sets} 组 · ${exercise.reps} 次 · 休息 ${exercise.restSeconds}s",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onNewPlan, modifier = Modifier.weight(1f)) {
                    Text("新建计划")
                }
                OutlinedButton(onClick = onAiPlan, modifier = Modifier.weight(1f)) {
                    Text("AI 生成")
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun EmptyHomeContent(
    state: HomeUiState,
    onCreateSamplePlan: () -> Unit,
    onNewPlan: () -> Unit,
    onAiPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(title = "欢迎使用 FitPlan", subtitle = "先创建一个计划，首页就能显示今天练什么")
        }
        state.message?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        state.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        item {
            SectionCard {
                Text("快速开始", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "创建一份本地示例计划，马上体验从计划到训练记录的完整流程。示例计划可以随时编辑或删除。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onCreateSamplePlan,
                    enabled = !state.isCreatingSamplePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "创建本地示例训练计划" }
                ) {
                    Text(if (state.isCreatingSamplePlan) "创建中..." else "创建示例计划")
                }
            }
        }
        item {
            SectionCard {
                Text("也可以从零开始", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onNewPlan, modifier = Modifier.weight(1f)) {
                        Text("手动创建")
                    }
                    OutlinedButton(onClick = onAiPlan, modifier = Modifier.weight(1f)) {
                        Text("AI 生成")
                    }
                }
                Text(
                    "AI 生成需要先在设置页配置 API Key；手动计划和历史记录可离线使用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                    name = "四天增肌训练计划",
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
                                    id = 10,
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
                weeklyWorkoutCount = 3,
                recent7DayWorkoutCount = 3,
                recent7DayDurationMinutes = 180
            ),
            onStartWorkout = { _, _ -> },
            onSelectWorkoutDay = {},
            onCreateSamplePlan = {},
            onNewPlan = {},
            onAiPlan = {},
            onHistory = {}
        )
    }
}
