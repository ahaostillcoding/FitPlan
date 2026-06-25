package com.fitplan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.AiPlanRepository
import com.fitplan.app.data.repository.BackupPreview
import com.fitplan.app.data.repository.BackupRepository
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val apiKey: String = "",
    val model: String = DEFAULT_DEEPSEEK_MODEL,
    val showApiKey: Boolean = false,
    val isSaving: Boolean = false,
    val isTestingConnection: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val exportJson: String = "",
    val exportSummary: String? = null,
    val importJson: String = "",
    val importPreview: BackupPreview? = null,
    val isBackupBusy: Boolean = false
) {
    val hasApiKey: Boolean = apiKey.isNotBlank()
    val apiKeyStatus: String = if (hasApiKey) {
        "已配置 API Key，可在 AI 页面生成计划。"
    } else {
        "未配置 API Key，AI 生成功能暂不可用；离线计划和记录不受影响。"
    }
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val aiPlanRepository: AiPlanRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        apiKey = settings.deepSeekApiKey,
                        model = settings.deepSeekModel.ifBlank { DEFAULT_DEEPSEEK_MODEL }
                    )
                }
            }
        }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value, message = null, errorMessage = null) }
    }

    fun updateModel(value: String) {
        _uiState.update { it.copy(model = value, message = null, errorMessage = null) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(showApiKey = !it.showApiKey) }
    }

    fun save() {
        val state = _uiState.value
        if (state.model.isBlank()) {
            _uiState.update { it.copy(errorMessage = "模型名不能为空") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, errorMessage = null) }
            runCatching {
                settingsRepository.saveDeepSeekApiKey(state.apiKey)
                settingsRepository.saveDeepSeekModel(state.model)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "设置已保存") }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "保存设置失败")
                }
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, errorMessage = null) }
            runCatching {
                settingsRepository.clearDeepSeekApiKey()
            }.onSuccess {
                _uiState.update { it.copy(apiKey = "", isSaving = false, message = "API Key 已清空") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message ?: "清空 API Key 失败") }
            }
        }
    }

    fun testDeepSeekConnection() {
        val repository = aiPlanRepository
        if (repository == null) {
            _uiState.update { it.copy(errorMessage = "当前环境未配置 AI 服务") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, message = null, errorMessage = null) }
            repository.testConnection()
                .onSuccess {
                    _uiState.update { it.copy(isTestingConnection = false, message = "DeepSeek 连接测试成功") }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            errorMessage = throwable.message ?: "DeepSeek 连接测试失败，请检查 Key、模型和网络"
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, message = null, errorMessage = null) }
            backupRepository.exportBackupJson()
                .onSuccess { json ->
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            exportJson = json,
                            exportSummary = buildExportSummary(json),
                            message = "备份 JSON 已生成"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isBackupBusy = false, errorMessage = throwable.message ?: "导出备份失败") }
                }
        }
    }

    fun updateImportJson(value: String) {
        _uiState.update {
            it.copy(importJson = value, importPreview = null, message = null, errorMessage = null)
        }
    }

    private fun buildExportSummary(json: String): String? {
        return runCatching {
            val obj = JsonParser.parseString(json).asJsonObject
            val version = obj.get("version")?.asInt ?: return null
            val exportedAt = obj.get("exportedAt")?.asLong ?: return null
            val formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(exportedAt))
            "备份版本 $version · 导出时间 $formattedTime"
        }.getOrNull()
    }

    fun previewImport() {
        backupRepository.previewImport(_uiState.value.importJson)
            .onSuccess { preview ->
                _uiState.update {
                    it.copy(
                        importPreview = preview,
                        message = "已读取备份：${preview.planCount} 个计划，${preview.recordCount} 条记录",
                        errorMessage = null
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update { it.copy(importPreview = null, errorMessage = throwable.message ?: "备份预览失败") }
            }
    }

    fun importBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, message = null, errorMessage = null) }
            backupRepository.importBackupJson(_uiState.value.importJson)
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            isBackupBusy = false,
                            importPreview = preview,
                            message = "导入完成：${preview.planCount} 个计划，${preview.recordCount} 条记录"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isBackupBusy = false, errorMessage = throwable.message ?: "导入备份失败") }
                }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            backupRepository: BackupRepository,
            aiPlanRepository: AiPlanRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository, backupRepository, aiPlanRepository) as T
                }
            }
    }
}
