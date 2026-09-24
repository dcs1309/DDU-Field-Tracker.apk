package com.example

import com.example.data.model.SakhyaScreeningEntity
import com.example.data.seed.SeedData
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DDU Field Intelligence & Sakhya Screening logic.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun sakhyaSeedData_hasValidScreening() {
        val screenings = SeedData.sakhyaScreenings
        assertFalse("Seed screenings should not be empty", screenings.isEmpty())

        val sunita = screenings.first()
        assertEquals("Sunita Devi", sunita.entrepreneurName)
        assertTrue("Sunita Devi should have high readiness score", sunita.readinessScore >= 75)
        assertEquals("Rakesh Sharma", sunita.fieldFellowName)
    }

    @Test
    fun sakhyaScreeningEntity_defaultInstantiation() {
        val entity = SakhyaScreeningEntity(
            screeningId = "SAKHYA-TEST-001",
            entrepreneurName = "Anita Sharma",
            productAndValueChain = "Handloom Cotton Gamchas",
            fieldFellowName = "Rahul Verma",
            screeningDate = "2026-09-23"
        )

        assertEquals("SAKHYA-TEST-001", entity.screeningId)
        assertEquals("Anita Sharma", entity.entrepreneurName)
        assertEquals("Partial", entity.recordFinancial)
        assertEquals("Practised", entity.recordCustomer)
    }
}
