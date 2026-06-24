package com.fitplan.app.ui.plans

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlansViewModel(
    private val repository: WorkoutPlanRepository
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)
    private val userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState<List<WorkoutPlan>>> = combine(
        repository.observePlans(),
        errorMessage
    ) { plans, error ->
        when {
            error != null -> UiState.Error(error)
            plans.isEmpty() -> UiState.Empty("还没有训练计划")
            else -> UiState.Content(plans)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )

    val hasPlans: StateFlow<Boolean> = repository.observePlans()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val message: StateFlow<String?> = userMessage

    fun setActive(planId: Long) {
        viewModelScope.launch {
            repository.setActivePlan(planId)
                .onSuccess { userMessage.value = "已设为当前计划" }
                .onFailure { errorMessage.value = it.message ?: "设置当前计划失败" }
        }
    }

    fun duplicate(planId: Long) {
        viewModelScope.launch {
            repository.duplicatePlan(planId)
                .onSuccess { userMessage.value = "计划已复制" }
                .onFailure { errorMessage.value = it.message ?: "复制计划失败" }
        }
    }

    fun delete(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
                .onSuccess { userMessage.value = "计划已删除" }
                .onFailure { errorMessage.value = it.message ?: "删除计划失败" }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun clearMessage() {
        userMessage.value = null
    }

    companion object {
        fun factory(repository: WorkoutPlanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlansViewModel(repository) as T
                }
            }
    }
}
