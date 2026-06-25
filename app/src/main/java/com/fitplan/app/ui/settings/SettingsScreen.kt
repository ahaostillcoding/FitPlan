package com.fitplan.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.common.ScreenHeader
import com.fitplan.app.ui.common.SectionCard

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    SettingsContent(
        state = state,
        onApiKeyChange = viewModel::updateApiKey,
        onModelChange = viewModel::updateModel,
        onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
        onSave = viewModel::save,
        onClearApiKey = viewModel::clearApiKey,
        onTestConnection = viewModel::testDeepSeekConnection,
        onExportBackup = viewModel::exportBackup,
        onImportJsonChange = viewModel::updateImportJson,
        onPreviewImport = viewModel::previewImport,
        onImportBackup = viewModel::importBackup,
        modifier = modifier
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSave: () -> Unit,
    onClearApiKey: () -> Unit,
    onTestConnection: () -> Unit,
    onExportBackup: () -> Unit,
    onImportJsonChange: (String) -> Unit,
    onPreviewImport: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "设置", subtitle = "本机配置")

        SectionCard {
            Text("DeepSeek API Key", style = MaterialTheme.typography.titleMedium)
            Text(state.apiKeyStatus, color = if (state.hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    OutlinedButton(onClick = onToggleApiKeyVisibility) {
                        Text(if (state.showApiKey) "隐藏" else "显示")
                    }
                }
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChange,
                label = { Text("模型") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "默认使用 deepseek-v4-flash。Key 仅保存在本机，卸载 App 后会丢失。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                    Text(if (state.isSaving) "保存中..." else "保存")
                }
                OutlinedButton(onClick = onClearApiKey, enabled = !state.isSaving && state.hasApiKey, modifier = Modifier.weight(1f)) {
                    Text("清空")
                }
            }
            OutlinedButton(
                onClick = onTestConnection,
                enabled = state.hasApiKey && !state.isTestingConnection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isTestingConnection) "测试中..." else "测试 DeepSeek 连接")
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        SectionCard {
            Text("本地备份", style = MaterialTheme.typography.titleMedium)
            Text(
                "导出 JSON 会包含计划、训练日、动作、历史记录和导出时间。导入时只新增，不覆盖已有数据。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onExportBackup, enabled = !state.isBackupBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isBackupBusy) "处理中..." else "生成备份 JSON")
            }
            state.exportSummary?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (state.exportJson.isNotBlank()) {
                OutlinedTextField(
                    value = state.exportJson,
                    onValueChange = {},
                    label = { Text("导出的备份 JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    readOnly = true
                )
            }
            OutlinedTextField(
                value = state.importJson,
                onValueChange = onImportJsonChange,
                label = { Text("粘贴备份 JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            state.importPreview?.let { preview ->
                Text("预览：${preview.planCount} 个计划，${preview.recordCount} 条记录", color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPreviewImport, enabled = !state.isBackupBusy, modifier = Modifier.weight(1f)) {
                    Text("预览导入")
                }
                Button(onClick = onImportBackup, enabled = !state.isBackupBusy && state.importPreview != null, modifier = Modifier.weight(1f)) {
                    Text("确认导入")
                }
            }
        }
    }
}
