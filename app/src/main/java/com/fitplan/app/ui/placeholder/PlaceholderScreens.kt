package com.fitplan.app.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "AI 生成计划",
        body = "DeepSeek 计划生成将在阶段 3 接入。",
        modifier = modifier
    )
}

@Composable
fun SettingsPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "设置",
        body = "API Key 安全保存和设置项将在阶段 3 完成。",
        modifier = modifier
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = body,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

