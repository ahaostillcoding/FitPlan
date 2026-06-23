package com.fitplan.app.domain.mapper

import com.fitplan.app.data.local.entity.WorkoutPlanEntity
import com.fitplan.app.domain.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMappersTest {
    @Test
    fun workoutPlanEntityMapsToDomain() {
        val entity = WorkoutPlanEntity(
            id = 1,
            name = "Strength Base",
            goal = "Strength",
            frequencyPerWeek = 3,
            estimatedDurationMinutes = 60,
            notes = "Keep two reps in reserve.",
            isActive = true,
            createdAt = 100,
            updatedAt = 200
        )

        val model = entity.toDomain()

        assertEquals(1, model.id)
        assertEquals("Strength Base", model.name)
        assertEquals("Strength", model.goal)
        assertEquals(3, model.frequencyPerWeek)
        assertEquals(60, model.estimatedDurationMinutes)
        assertEquals(true, model.isActive)
    }

    @Test
    fun workoutPlanDomainMapsToTrimmedEntity() {
        val model = WorkoutPlan(
            id = 2,
            name = "  Hypertrophy  ",
            goal = "  Muscle Gain  ",
            frequencyPerWeek = 4,
            estimatedDurationMinutes = 75,
            notes = "  Moderate volume  ",
            isActive = false,
            createdAt = 300,
            updatedAt = 400
        )

        val entity = model.toEntity()

        assertEquals("Hypertrophy", entity.name)
        assertEquals("Muscle Gain", entity.goal)
        assertEquals("Moderate volume", entity.notes)
    }
}

