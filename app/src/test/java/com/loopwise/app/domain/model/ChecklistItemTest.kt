package com.loopwise.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistItemTest {

    @Test
    fun `encode then decode round-trips items and checked state`() {
        val items = listOf(
            ChecklistItem(text = "Pack passport", checked = true),
            ChecklistItem(text = "Charge camera", checked = false),
        )

        val decoded = decodeChecklistItems(encodeChecklistItems(items))

        assertEquals(items, decoded)
    }

    @Test
    fun `decode of null content returns empty list`() {
        assertTrue(decodeChecklistItems(null).isEmpty())
    }

    @Test
    fun `decode of malformed json returns empty list instead of throwing`() {
        assertTrue(decodeChecklistItems("not json").isEmpty())
    }

    @Test
    fun `decode of unrelated json (e g a plain text entry) returns empty list`() {
        assertTrue(decodeChecklistItems("just a free-form journal note").isEmpty())
    }
}
