package com.fitplan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val model: String = DEFAULT_DEEPSEEK_MODEL,
    val showApiKey: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
) {
    val hasApiKey: Boolean = apiKey.isNotBlank()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
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
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "保存设置失败"
                    )
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
                _uiState.update {
                    it.copy(
                        apiKey = "",
                        isSaving = false,
                        message = "API Key 已清空"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "清空 API Key 失败"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository) as T
                }
            }
    }
}
