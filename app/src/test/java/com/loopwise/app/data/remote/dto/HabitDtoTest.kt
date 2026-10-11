package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitDtoTest {

    private fun habit(color: Long) = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Drink water",
        color = color,
        type = HabitType.COUNTER,
        targetValue = 8.0,
        unit = "cups",
    )

    @Test
    fun `toDto formats color as a 6-digit hex string dropping alpha`() {
        val dto = habit(color = 0xFF3B82F6).toDto()
        assertEquals("#3B82F6", dto.color)
    }

    @Test
    fun `toDomain parses a hex color string back to an opaque ARGB Long`() {
        val dto = HabitDto(
            id = "h1",
            ownerId = "u1",
            name = "Drink water",
            color = "#3B82F6",
            type = "counter",
        )
        assertEquals(0xFF3B82F6, dto.toDomain().color)
    }

    @Test
    fun `color survives a full domain-to-dto-to-domain round trip`() {
        val original = habit(color = 0xFF14B8A6)
        val roundTripped = original.toDto().toDomain()
        assertEquals(original.color, roundTripped.color)
    }

    @Test
    fun `habit type survives the round trip`() {
        val original = habit(color = 0xFF14B8A6)
        val roundTripped = original.toDto().toDomain()
        assertEquals(original.type, roundTripped.type)
    }
}
