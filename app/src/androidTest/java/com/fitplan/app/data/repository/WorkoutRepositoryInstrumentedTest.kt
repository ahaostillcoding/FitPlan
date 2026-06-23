package com.fitplan.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitplan.app.data.local.database.FitPlanDatabase
import com.fitplan.app.domain.model.Exercise
import com.fitplan.app.domain.model.WorkoutDay
import com.fitplan.app.domain.model.WorkoutPlan
import com.fitplan.app.domain.model.WorkoutRecord
import com.fitplan.app.domain.model.WorkoutRecordExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryInstrumentedTest {
    private lateinit var database: FitPlanDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitPlanDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun planRepositorySavesReadsActivatesAndDeletesPlan() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = DefaultWorkoutPlanRepository(
            database = database,
            ioDispatcher = dispatcher,
            clock = { 1_000L }
        )

        val planId = repository.savePlan(samplePlan(isActive = true)).getOrThrow()
        val activePlan = repository.observeActivePlan().first()

        assertNotNull(activePlan)
        assertEquals(planId, activePlan?.id)
        assertEquals(1, activePlan?.days?.size)
        assertEquals(1, activePlan?.days?.first()?.exercises?.size)

        repository.deletePlan(planId).getOrThrow()

        assertEquals(emptyList<WorkoutPlan>(), repository.observePlans().first())
    }

    @Test
    fun recordRepositorySavesRecordsInDateOrderAndCountsRange() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = DefaultWorkoutRecordRepository(
            database = database,
            ioDispatcher = dispatcher,
            clock = { 5_000L }
        )

        repository.saveRecord(sampleRecord(date = 1_000L, duration = 45)).getOrThrow()
        repository.saveRecord(sampleRecord(date = 3_000L, duration = 50)).getOrThrow()

        val records = repository.observeRecords().first()
        val count = repository.observeRecordCountBetween(0L, 4_000L).first()
        val lastRecord = repository.observeLastRecord().first()

        assertEquals(2, records.size)
        assertEquals(3_000L, records.first().date)
        assertEquals(2, count)
        assertEquals(3_000L, lastRecord?.date)
    }

    private fun samplePlan(isActive: Boolean): WorkoutPlan {
        return WorkoutPlan(
            name = "Four Day Strength",
            goal = "Strength",
            frequencyPerWeek = 4,
            estimatedDurationMinutes = 60,
            notes = "Start light.",
            isActive = isActive,
            days = listOf(
                WorkoutDay(
                    dayName = "Day 1 Push",
                    sortOrder = 0,
                    exercises = listOf(
                        Exercise(
                            name = "Bench Press",
                            bodyPart = "Chest",
                            sets = 4,
                            reps = "6-8",
                            weight = "",
                            restSeconds = 120,
                            notes = "Control the eccentric.",
                            sortOrder = 0
                        )
                    )
                )
            )
        )
    }

    private fun sampleRecord(date: Long, duration: Int): WorkoutRecord {
        return WorkoutRecord(
            planId = null,
            workoutDayId = null,
            planName = "Four Day Strength",
            workoutDayName = "Day 1 Push",
            date = date,
            durationMinutes = duration,
            notes = "Solid session.",
            exercises = listOf(
                WorkoutRecordExercise(
                    originalExerciseId = null,
                    name = "Bench Press",
                    bodyPart = "Chest",
                    sets = 4,
                    reps = "6-8",
                    weight = "",
                    restSeconds = 120,
                    plannedNotes = "Control the eccentric.",
                    isCompleted = true,
                    actualNotes = "Felt good.",
                    sortOrder = 0
                )
            )
        )
    }
}

