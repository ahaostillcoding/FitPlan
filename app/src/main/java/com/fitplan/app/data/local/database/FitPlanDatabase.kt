package com.fitplan.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitplan.app.data.local.dao.WorkoutPlanDao
import com.fitplan.app.data.local.dao.WorkoutRecordDao
import com.fitplan.app.data.local.entity.ExerciseEntity
import com.fitplan.app.data.local.entity.WorkoutDayEntity
import com.fitplan.app.data.local.entity.WorkoutPlanEntity
import com.fitplan.app.data.local.entity.WorkoutRecordEntity
import com.fitplan.app.data.local.entity.WorkoutRecordExerciseEntity

@Database(
    entities = [
        WorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutRecordEntity::class,
        WorkoutRecordExerciseEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FitPlanDatabase : RoomDatabase() {
    abstract fun workoutPlanDao(): WorkoutPlanDao

    abstract fun workoutRecordDao(): WorkoutRecordDao

    companion object {
        private const val DATABASE_NAME = "fitplan.db"

        fun create(context: Context): FitPlanDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FitPlanDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
