package com.fitplan.app.di

import android.content.Context
import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.data.remote.deepseek.DeepSeekApiService
import com.fitplan.app.data.repository.AiPlanRepository
import com.fitplan.app.data.repository.BackupRepository
import com.fitplan.app.data.repository.DataStoreSettingsRepository
import com.fitplan.app.data.repository.DefaultAiPlanRepository
import com.fitplan.app.data.repository.DefaultBackupRepository
import com.fitplan.app.data.repository.DefaultWorkoutPlanRepository
import com.fitplan.app.data.repository.DefaultWorkoutRecordRepository
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(
    appContext: Context
) {
    val database: FitPlanDatabase = FitPlanDatabase.create(appContext)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext)
    val workoutPlanRepository: WorkoutPlanRepository = DefaultWorkoutPlanRepository(database)
    val workoutRecordRepository: WorkoutRecordRepository = DefaultWorkoutRecordRepository(database)
    val backupRepository: BackupRepository = DefaultBackupRepository(
        database = database,
        workoutPlanRepository = workoutPlanRepository,
        workoutRecordRepository = workoutRecordRepository
    )
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val deepSeekApiService: DeepSeekApiService =
        retrofit.create(DeepSeekApiService::class.java)

    val aiPlanRepository: AiPlanRepository = DefaultAiPlanRepository(
        apiService = deepSeekApiService,
        settingsRepository = settingsRepository
    )
}
