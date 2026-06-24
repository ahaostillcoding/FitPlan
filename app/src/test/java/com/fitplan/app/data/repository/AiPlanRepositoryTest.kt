package com.fitplan.app.data.repository

import com.fitplan.app.data.remote.deepseek.DeepSeekApiService
import com.fitplan.app.data.remote.deepseek.DeepSeekChatRequest
import com.fitplan.app.data.remote.deepseek.DeepSeekChatResponse
import com.fitplan.app.data.remote.deepseek.DeepSeekChoice
import com.fitplan.app.data.remote.deepseek.DeepSeekResponseMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiPlanRepositoryTest {
    private val input = AiPlanInput(
        goal = "增肌",
        daysPerWeek = 3,
        durationMinutes = 60,
        experienceLevel = "新手",
        equipment = "哑铃",
        limitations = "",
        notes = ""
    )

    @Test
    fun generatePlan_withoutApiKey_returnsFailure() = runTest {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> error("Should not call network") },
            settingsRepository = FakeSettingsRepository(apiKey = ""),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val result = repository.generatePlan(input)

        assertTrue(result.isFailure)
        assertEquals("请先在设置页配置 DeepSeek API Key", result.exceptionOrNull()?.message)
    }

    @Test
    fun generatePlan_networkFailure_returnsFailure() = runTest {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> throw IOException("offline") },
            settingsRepository = FakeSettingsRepository(apiKey = "sk-test"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val result = repository.generatePlan(input)

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun testConnection_withoutApiKey_returnsFailure() = runTest {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> error("Should not call network") },
            settingsRepository = FakeSettingsRepository(apiKey = ""),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val result = repository.testConnection()

        assertTrue(result.isFailure)
        assertEquals("请先保存 DeepSeek API Key", result.exceptionOrNull()?.message)
    }

    @Test
    fun testConnection_withContent_returnsSuccess() = runTest {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ ->
                DeepSeekChatResponse(
                    choices = listOf(DeepSeekChoice(DeepSeekResponseMessage("""{"ok":true}""")))
                )
            },
            settingsRepository = FakeSettingsRepository(apiKey = "sk-test"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val result = repository.testConnection()

        assertTrue(result.isSuccess)
    }

    @Test
    fun parseWorkoutPlanJson_validJson_returnsPlan() {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> error("Unused") },
            settingsRepository = FakeSettingsRepository(apiKey = "sk-test")
        )

        val result = repository.parseWorkoutPlanJson(validPlanJson)

        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertEquals("三天新手训练计划", plan.name)
        assertEquals(1, plan.days.size)
        assertEquals("深蹲", plan.days.first().exercises.first().name)
    }

    @Test
    fun parseWorkoutPlanJson_nonJson_returnsFailure() {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> error("Unused") },
            settingsRepository = FakeSettingsRepository(apiKey = "sk-test")
        )

        val result = repository.parseWorkoutPlanJson("not json")

        assertTrue(result.isFailure)
        assertEquals("AI 返回内容中没有找到 JSON 对象", result.exceptionOrNull()?.message)
    }

    @Test
    fun parseWorkoutPlanJson_missingRequiredField_returnsFailure() {
        val repository = DefaultAiPlanRepository(
            apiService = FakeDeepSeekApiService { _, _ -> error("Unused") },
            settingsRepository = FakeSettingsRepository(apiKey = "sk-test")
        )

        val result = repository.parseWorkoutPlanJson("""{"name":"","days":[]}""")

        assertTrue(result.isFailure)
        assertEquals("AI 返回的计划名称为空", result.exceptionOrNull()?.message)
    }

    private class FakeDeepSeekApiService(
        private val responder: suspend (String, DeepSeekChatRequest) -> DeepSeekChatResponse
    ) : DeepSeekApiService {
        override suspend fun createChatCompletion(
            authorization: String,
            request: DeepSeekChatRequest
        ): DeepSeekChatResponse = responder(authorization, request)
    }

    private class FakeSettingsRepository(
        apiKey: String
    ) : SettingsRepository {
        private val state = MutableStateFlow(
            AppSettings(
                deepSeekApiKey = apiKey,
                deepSeekModel = DEFAULT_DEEPSEEK_MODEL
            )
        )

        override val settings: Flow<AppSettings> = state

        override suspend fun saveDeepSeekApiKey(apiKey: String) {
            state.value = state.value.copy(deepSeekApiKey = apiKey)
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

    private companion object {
        val validPlanJson = """
            {
              "name": "三天新手训练计划",
              "goal": "新手入门",
              "frequencyPerWeek": 3,
              "estimatedDurationMinutes": 45,
              "notes": "循序渐进",
              "days": [
                {
                  "dayName": "Day 1 全身基础",
                  "exercises": [
                    {
                      "name": "深蹲",
                      "bodyPart": "腿",
                      "sets": 3,
                      "reps": "10-12",
                      "weight": "",
                      "restSeconds": 60,
                      "notes": "保持核心稳定"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
