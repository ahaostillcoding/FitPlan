package com.fitplan.app.data.repository

import com.fitplan.app.data.remote.deepseek.DeepSeekApiService
import com.fitplan.app.data.remote.deepseek.DeepSeekChatRequest
import com.fitplan.app.data.remote.deepseek.DeepSeekMessage
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class AiPlanInput(
    val goal: String,
    val daysPerWeek: Int,
    val durationMinutes: Int,
    val experienceLevel: String,
    val equipment: String,
    val limitations: String,
    val notes: String
)

interface AiPlanRepository {
    suspend fun generatePlan(input: AiPlanInput): Result<WorkoutPlan>
    fun parseWorkoutPlanJson(json: String): Result<WorkoutPlan>
}

class DefaultAiPlanRepository(
    private val apiService: DeepSeekApiService,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AiPlanRepository {
    override suspend fun generatePlan(input: AiPlanInput): Result<WorkoutPlan> {
        return withContext(ioDispatcher) {
            runCatching {
                validateInput(input)
                val settings = settingsRepository.settings.first()
                val apiKey = settings.deepSeekApiKey.trim()
                if (apiKey.isBlank()) {
                    throw IllegalStateException("请先在设置页配置 DeepSeek API Key")
                }
                val response = apiService.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = DeepSeekChatRequest(
                        model = settings.deepSeekModel.ifBlank { DEFAULT_DEEPSEEK_MODEL },
                        messages = listOf(
                            DeepSeekMessage(
                                role = "system",
                                content = "你是一名专业健身教练，只返回合法 json，不要输出 Markdown。"
                            ),
                            DeepSeekMessage(
                                role = "user",
                                content = buildPrompt(input)
                            )
                        )
                    )
                )
                val content = response.choices.firstOrNull()?.message?.content
                    ?: throw IllegalStateException("DeepSeek 没有返回计划内容")
                parseWorkoutPlanJson(content).getOrThrow()
            }
        }
    }

    override fun parseWorkoutPlanJson(json: String): Result<WorkoutPlan> {
        return runCatching {
            val cleanedJson = extractJsonObject(json)
            val dto = try {
                gson.fromJson(cleanedJson, AiWorkoutPlanDto::class.java)
            } catch (error: JsonSyntaxException) {
                throw IllegalArgumentException("AI 返回内容不是可解析的 JSON", error)
            } ?: throw IllegalArgumentException("AI 返回内容为空")

            dto.toDomain().also { validateGeneratedPlan(it) }
        }
    }

    private fun buildPrompt(input: AiPlanInput): String {
        return """
            请根据以下用户信息生成健身训练计划，并只返回 json 对象。
            健身目标：${input.goal}
            每周训练天数：${input.daysPerWeek}
            每次训练时长：${input.durationMinutes} 分钟
            当前经验水平：${input.experienceLevel}
            可用器械：${input.equipment.ifBlank { "不限" }}
            需要避开的动作或受限部位：${input.limitations.ifBlank { "无" }}
            其他备注：${input.notes.ifBlank { "无" }}

            必须返回如下 JSON 结构，字段名保持一致：
            {
              "name": "四天增肌训练计划",
              "goal": "增肌",
              "frequencyPerWeek": 4,
              "estimatedDurationMinutes": 60,
              "notes": "适合有一定训练基础的用户",
              "days": [
                {
                  "dayName": "Day 1 胸肩三头",
                  "exercises": [
                    {
                      "name": "杠铃卧推",
                      "bodyPart": "胸",
                      "sets": 4,
                      "reps": "8-10",
                      "weight": "",
                      "restSeconds": 90,
                      "notes": "注意肩胛稳定"
                    }
                  ]
                }
              ]
            }
            要求：
            - days 数量尽量等于每周训练天数。
            - 每个训练日至少 3 个动作，动作按执行顺序排列。
            - sets 必须为正整数，restSeconds 必须为 0 或正整数。
            - reps 必须是字符串，例如 "8-12"、"12-15"、"30秒"。
            - 不要添加解释文字，不要使用代码块。
        """.trimIndent()
    }

    private fun validateInput(input: AiPlanInput) {
        require(input.goal.isNotBlank()) { "请选择健身目标" }
        require(input.daysPerWeek in 1..7) { "每周训练天数必须在 1 到 7 之间" }
        require(input.durationMinutes > 0) { "每次训练时长必须大于 0" }
        require(input.experienceLevel.isNotBlank()) { "请选择经验水平" }
    }

    private fun extractJsonObject(raw: String): String {
        val withoutFence = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = withoutFence.indexOf('{')
        val end = withoutFence.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw IllegalArgumentException("AI 返回内容中没有找到 JSON 对象")
        }
        return withoutFence.substring(start, end + 1)
    }

    private fun AiWorkoutPlanDto.toDomain(): WorkoutPlan {
        return WorkoutPlan(
            name = name.orEmpty().trim(),
            goal = goal.orEmpty().trim(),
            frequencyPerWeek = frequencyPerWeek ?: 0,
            estimatedDurationMinutes = estimatedDurationMinutes ?: 0,
            notes = notes.orEmpty().trim(),
            isActive = false,
            days = days.orEmpty().mapIndexed { dayIndex, day ->
                WorkoutDay(
                    dayName = day.dayName.orEmpty().trim(),
                    sortOrder = dayIndex,
                    exercises = day.exercises.orEmpty().mapIndexed { exerciseIndex, exercise ->
                        Exercise(
                            name = exercise.name.orEmpty().trim(),
                            bodyPart = exercise.bodyPart.orEmpty().trim(),
                            sets = exercise.sets ?: 0,
                            reps = exercise.reps.orEmpty().trim(),
                            weight = exercise.weight.orEmpty().trim(),
                            restSeconds = exercise.restSeconds ?: 0,
                            notes = exercise.notes.orEmpty().trim(),
                            sortOrder = exerciseIndex
                        )
                    }
                )
            }
        )
    }

    private fun validateGeneratedPlan(plan: WorkoutPlan) {
        require(plan.name.isNotBlank()) { "AI 返回的计划名称为空" }
        require(plan.goal.isNotBlank()) { "AI 返回的训练目标为空" }
        require(plan.frequencyPerWeek in 1..7) { "AI 返回的每周训练次数不合法" }
        require(plan.estimatedDurationMinutes > 0) { "AI 返回的训练时长不合法" }
        require(plan.days.isNotEmpty()) { "AI 没有返回训练日" }
        plan.days.forEachIndexed { dayIndex, day ->
            require(day.dayName.isNotBlank()) { "第 ${dayIndex + 1} 个训练日名称为空" }
            require(day.exercises.isNotEmpty()) { "${day.dayName} 没有动作" }
            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                val prefix = "${day.dayName} 第 ${exerciseIndex + 1} 个动作"
                require(exercise.name.isNotBlank()) { "$prefix 名称为空" }
                require(exercise.bodyPart.isNotBlank()) { "$prefix 训练部位为空" }
                require(exercise.sets > 0) { "$prefix 组数不合法" }
                require(exercise.reps.isNotBlank()) { "$prefix 次数为空" }
                require(exercise.restSeconds >= 0) { "$prefix 休息时间不合法" }
            }
        }
    }
}

private data class AiWorkoutPlanDto(
    val name: String?,
    val goal: String?,
    val frequencyPerWeek: Int?,
    val estimatedDurationMinutes: Int?,
    val notes: String?,
    val days: List<AiWorkoutDayDto>?
)

private data class AiWorkoutDayDto(
    val dayName: String?,
    val exercises: List<AiExerciseDto>?
)

private data class AiExerciseDto(
    val name: String?,
    val bodyPart: String?,
    val sets: Int?,
    val reps: String?,
    val weight: String?,
    val restSeconds: Int?,
    val notes: String?
)
