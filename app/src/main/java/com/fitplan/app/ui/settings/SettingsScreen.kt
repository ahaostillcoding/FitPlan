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
import androidx.compose.material3.Card
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("DeepSeek", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = state.apiKeyStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.hasApiKey) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("DeepSeek API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (state.showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
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
                    "默认使用 deepseek-v4-flash。Key 仅保存在本机 DataStore，后续可升级到加密存储。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.message != null) {
                    Text(state.message, color = MaterialTheme.colorScheme.primary)
                }
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isSaving) "保存中..." else "保存设置")
                    }
                    OutlinedButton(
                        onClick = onClearApiKey,
                        enabled = !state.isSaving && state.hasApiKey,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清空 Key")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    SettingsContent(
        state = SettingsUiState(
            apiKey = "sk-xxxxxxxx",
            message = "设置已保存"
        ),
        onApiKeyChange = {},
        onModelChange = {},
        onToggleApiKeyVisibility = {},
        onSave = {},
        onClearApiKey = {}
    )
}
