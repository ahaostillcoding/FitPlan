package com.fitplan.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutRecordRepository
import com.fitplan.app.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class HistoryFilter(val label: String) {
    ALL("全部"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月")
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val records: List<WorkoutRecord> = emptyList(),
    val emptyMessage: String = "还没有训练记录",
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val repository: WorkoutRecordRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val userMessage = MutableStateFlow<String?>(null)
    private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeRecords(),
        selectedFilter,
        errorMessage
    ) { records, filter, error ->
        if (error != null) {
            HistoryUiState(isLoading = false, selectedFilter = filter, errorMessage = error)
        } else {
            val filteredRecords = filterRecords(records, filter)
            HistoryUiState(
                isLoading = false,
                selectedFilter = filter,
                records = filteredRecords,
                emptyMessage = if (records.isEmpty()) {
                    "还没有训练记录"
                } else {
                    "当前筛选没有训练记录"
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    val message: StateFlow<String?> = userMessage

    fun selectFilter(filter: HistoryFilter) {
        selectedFilter.value = filter
    }

    fun delete(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
                .onSuccess { userMessage.value = "训练记录已删除" }
                .onFailure { errorMessage.value = it.message ?: "删除训练记录失败" }
        }
    }

    fun clearMessage() {
        userMessage.value = null
    }

    private fun filterRecords(records: List<WorkoutRecord>, filter: HistoryFilter): List<WorkoutRecord> {
        if (filter == HistoryFilter.ALL) return records
        val now = LocalDate.now(ZoneId.systemDefault())
        val range = when (filter) {
            HistoryFilter.ALL -> return records
            HistoryFilter.THIS_WEEK -> {
                val start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusWeeks(1)
            }
            HistoryFilter.THIS_MONTH -> {
                val start = now.withDayOfMonth(1)
                start to start.plusMonths(1)
            }
        }
        val zone = ZoneId.systemDefault()
        val startMillis = range.first.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = range.second.atStartOfDay(zone).toInstant().toEpochMilli()
        return records.filter { it.date >= startMillis && it.date < endMillis }
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
