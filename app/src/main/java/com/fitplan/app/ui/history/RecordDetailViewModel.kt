package com.fitplan.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.ui.common.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecordDetailViewModel(
    recordId: Long,
    repository: WorkoutRecordRepository
) : ViewModel() {
    val uiState: StateFlow<UiState<WorkoutRecord>> = repository.observeRecord(recordId)
        .map { record ->
            if (record == null) UiState.Empty("未找到训练记录") else UiState.Content(record)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    companion object {
        fun factory(
            recordId: Long,
            repository: WorkoutRecordRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordDetailViewModel(recordId, repository) as T
            }
        }
    }
}
