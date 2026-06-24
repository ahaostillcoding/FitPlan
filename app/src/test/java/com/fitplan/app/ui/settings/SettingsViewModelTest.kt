package com.fitplan.app.ui.settings

import com.fitplan.app.data.repository.AppSettings
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun save_persistsApiKeyAndModel() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.updateApiKey(" sk-test ")
        viewModel.updateModel("deepseek-v4-flash")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("sk-test", repository.current.deepSeekApiKey)
        assertEquals("deepseek-v4-flash", repository.current.deepSeekModel)
        assertEquals("设置已保存", viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.hasApiKey)
    }

    @Test
    fun clearApiKey_removesSavedKey() = runTest {
        val repository = FakeSettingsRepository(apiKey = "sk-test")
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.clearApiKey()
        advanceUntilIdle()

        assertEquals("", repository.current.deepSeekApiKey)
        assertFalse(viewModel.uiState.value.hasApiKey)
        assertEquals("API Key 已清空", viewModel.uiState.value.message)
    }

    @Test
    fun emptyApiKey_reportsOfflineFeaturesStillAvailable() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        advanceUntilIdle()

        assertEquals(
            "未配置 API Key，AI 生成暂不可用；离线计划和记录不受影响。",
            viewModel.uiState.value.apiKeyStatus
        )
    }

    private class FakeSettingsRepository(
        apiKey: String = "",
        model: String = DEFAULT_DEEPSEEK_MODEL
    ) : SettingsRepository {
        private val state = MutableStateFlow(
            AppSettings(
                deepSeekApiKey = apiKey,
                deepSeekModel = model
            )
        )

        val current: AppSettings get() = state.value

        override val settings: Flow<AppSettings> = state

        override suspend fun saveDeepSeekApiKey(apiKey: String) {
            state.value = state.value.copy(deepSeekApiKey = apiKey.trim())
        }

        override suspend fun saveDeepSeekModel(model: String) {
            state.value = state.value.copy(deepSeekModel = model)
        }

        override suspend fun clearDeepSeekApiKey() {
            state.value = state.value.copy(deepSeekApiKey = "")
        }

        override suspend fun saveSelectedWorkoutDayId(dayId: Long?) {
            state.value = state.value.copy(selectedWorkoutDayId = dayId)
        }
    }
}
