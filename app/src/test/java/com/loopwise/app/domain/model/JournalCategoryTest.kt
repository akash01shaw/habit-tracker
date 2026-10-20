package com.loopwise.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class JournalCategoryTest {

    @Test
    fun `wireValue round-trips through fromWire for every category`() {
        JournalCategory.entries.forEach { category ->
            assertEquals(category, JournalCategory.fromWire(category.wireValue))
        }
    }

    @Test
    fun `fromWire falls back to GENERAL for an unknown or null value`() {
        assertEquals(JournalCategory.GENERAL, JournalCategory.fromWire("unknown"))
        assertEquals(JournalCategory.GENERAL, JournalCategory.fromWire(null))
    }
}
