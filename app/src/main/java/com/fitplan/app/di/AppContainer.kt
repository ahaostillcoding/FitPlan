package com.fitplan.app.di

import android.content.Context
import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.data.repository.DataStoreSettingsRepository
import com.fitplan.app.data.repository.DefaultWorkoutPlanRepository
import com.fitplan.app.data.repository.DefaultWorkoutRecordRepository
import com.fitplan.app.data.repository.SettingsRepository
import com.fitplan.app.data.repository.WorkoutPlanRepository
import com.fitplan.app.data.repository.WorkoutRecordRepository

class AppContainer(
    appContext: Context
) {
    val database: FitPlanDatabase = FitPlanDatabase.create(appContext)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext)
    val workoutPlanRepository: WorkoutPlanRepository = DefaultWorkoutPlanRepository(database)
    val workoutRecordRepository: WorkoutRecordRepository = DefaultWorkoutRecordRepository(database)
}
