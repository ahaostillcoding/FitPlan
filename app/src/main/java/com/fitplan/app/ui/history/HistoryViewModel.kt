package com.fitplan.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: WorkoutRecordRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState<List<WorkoutRecord>>> = combine(
        repository.observeRecords(),
        errorMessage
    ) { records, error ->
        when {
            error != null -> UiState.Error(error)
            records.isEmpty() -> UiState.Empty("还没有训练记录")
            else -> UiState.Content(records)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    fun delete(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
                .onFailure { errorMessage.value = it.message ?: "删除训练记录失败" }
        }
    }

    companion object {
        fun factory(repository: WorkoutRecordRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
