package com.fitplan.app.ui.settings

import com.fitplan.app.data.repository.AiPlanInput
import com.fitplan.app.data.repository.AiPlanRepository
import com.fitplan.app.data.repository.AppSettings
import com.fitplan.app.data.repository.BackupPreview
import com.fitplan.app.data.repository.BackupRepository
import com.fitplan.app.data.repository.DEFAULT_DEEPSEEK_MODEL
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.domain.model.WorkoutPlan
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
        val viewModel = SettingsViewModel(repository, FakeBackupRepository())

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
        val viewModel = SettingsViewModel(repository, FakeBackupRepository())
        advanceUntilIdle()

        viewModel.clearApiKey()
        advanceUntilIdle()

        assertEquals("", repository.current.deepSeekApiKey)
        assertFalse(viewModel.uiState.value.hasApiKey)
        assertEquals("API Key 已清空", viewModel.uiState.value.message)
    }

    @Test
    fun emptyApiKey_reportsOfflineFeaturesStillAvailable() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), FakeBackupRepository())
        advanceUntilIdle()

        assertEquals(
            "未配置 API Key，AI 生成功能暂不可用；离线计划和记录不受影响。",
            viewModel.uiState.value.apiKeyStatus
        )
    }

    @Test
    fun testDeepSeekConnection_success_setsMessage() = runTest {
        val viewModel = SettingsViewModel(
            FakeSettingsRepository(apiKey = "sk-test"),
            FakeBackupRepository(),
            FakeAiPlanRepository(Result.success(Unit))
        )

        viewModel.testDeepSeekConnection()
        advanceUntilIdle()

        assertEquals("DeepSeek 连接测试成功", viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isTestingConnection)
    }

    @Test
    fun testDeepSeekConnection_failure_setsError() = runTest {
        val viewModel = SettingsViewModel(
            FakeSettingsRepository(apiKey = "sk-test"),
            FakeBackupRepository(),
            FakeAiPlanRepository(Result.failure(IllegalStateException("无效 Key")))
        )

        viewModel.testDeepSeekConnection()
        advanceUntilIdle()

        assertEquals("无效 Key", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isTestingConnection)
    }

    @Test
    fun exportBackup_setsJsonAndMessage() = runTest {
        val viewModel = SettingsViewModel(
            FakeSettingsRepository(),
            FakeBackupRepository(exportJson = """{"version":1,"exportedAt":1760000000000}""")
        )

        viewModel.exportBackup()
        advanceUntilIdle()

        assertEquals("""{"version":1,"exportedAt":1760000000000}""", viewModel.uiState.value.exportJson)
        assertTrue(viewModel.uiState.value.exportSummary?.startsWith("备份版本 1 · 导出时间") == true)
        assertEquals("备份 JSON 已生成", viewModel.uiState.value.message)
    }

    @Test
    fun previewImport_setsPreview() = runTest {
        val viewModel = SettingsViewModel(
            FakeSettingsRepository(),
            FakeBackupRepository(preview = BackupPreview(planCount = 2, recordCount = 3))
        )

        viewModel.updateImportJson("""{"version":1}""")
        viewModel.previewImport()

        assertEquals(2, viewModel.uiState.value.importPreview?.planCount)
        assertEquals(3, viewModel.uiState.value.importPreview?.recordCount)
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

        override suspend fun saveWorkoutDraftJson(json: String) {
            state.value = state.value.copy(workoutDraftJson = json)
        }

        override suspend fun clearWorkoutDraft() {
            state.value = state.value.copy(workoutDraftJson = "")
        }
    }

    private class FakeAiPlanRepository(
        private val connectionResult: Result<Unit>
    ) : AiPlanRepository {
        override suspend fun generatePlan(input: AiPlanInput): Result<WorkoutPlan> {
            return Result.failure(UnsupportedOperationException("Unused"))
        }

        override suspend fun testConnection(): Result<Unit> = connectionResult

        override fun parseWorkoutPlanJson(json: String): Result<WorkoutPlan> {
            return Result.failure(UnsupportedOperationException("Unused"))
        }
    }

    private class FakeBackupRepository(
        private val exportJson: String = "{}",
        private val preview: BackupPreview = BackupPreview(0, 0)
    ) : BackupRepository {
        override suspend fun exportBackupJson(): Result<String> = Result.success(exportJson)
        override fun previewImport(json: String): Result<BackupPreview> = Result.success(preview)
        override suspend fun importBackupJson(json: String): Result<BackupPreview> = Result.success(preview)
    }
}
