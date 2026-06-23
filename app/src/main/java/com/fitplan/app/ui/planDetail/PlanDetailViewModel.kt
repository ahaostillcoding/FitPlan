package com.fitplan.app.ui.planDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanDetailViewModel(
    private val planId: Long,
    private val repository: WorkoutPlanRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState<WorkoutPlan>> = combine(
        repository.observePlan(planId),
        errorMessage
    ) { plan, error ->
        when {
            error != null -> UiState.Error(error)
            plan == null -> UiState.Empty("未找到训练计划")
            else -> UiState.Content(plan)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    fun setActive() {
        viewModelScope.launch {
            repository.setActivePlan(planId)
                .onFailure { errorMessage.value = it.message ?: "设置当前计划失败" }
        }
    }

    fun duplicate(onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            repository.duplicatePlan(planId)
                .onSuccess(onDuplicated)
                .onFailure { errorMessage.value = it.message ?: "复制计划失败" }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlan(planId)
                .onSuccess { onDeleted() }
                .onFailure { errorMessage.value = it.message ?: "删除计划失败" }
        }
    }

    companion object {
        fun factory(
            planId: Long,
            repository: WorkoutPlanRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlanDetailViewModel(planId, repository) as T
            }
        }
    }
}

