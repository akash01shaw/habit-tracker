package com.loopwise.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulletItemTest {

    @Test
    fun `encode then decode round-trips mixed bullet kinds and done state`() {
        val items = listOf(
            BulletItem(text = "Finish report", kind = BulletKind.TASK, done = true),
            BulletItem(text = "Dentist at 3pm", kind = BulletKind.EVENT),
            BulletItem(text = "Felt tired after lunch", kind = BulletKind.NOTE),
        )

        val decoded = decodeBulletItems(encodeBulletItems(items))

        assertEquals(items, decoded)
    }

    @Test
    fun `decode of null content returns empty list`() {
        assertTrue(decodeBulletItems(null).isEmpty())
    }

    @Test
    fun `decode of malformed json returns empty list instead of throwing`() {
        assertTrue(decodeBulletItems("not json").isEmpty())
    }

    @Test
    fun `each bullet kind has a distinct symbol`() {
        val symbols = BulletKind.entries.map { it.symbol }
        assertEquals(symbols.toSet().size, symbols.size)
    }
}
