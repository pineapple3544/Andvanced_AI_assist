package com.ai.assist.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentOutlineParserTest {
    @Test
    fun parsesJsonOutline() {
        val outline = DocumentOutlineParser.parseOrFallback(
            """
                {"title":"Deck","subtitle":"Sub","slides":[{"title":"One","bullets":["A","B"],"speakerNote":"Note"}]}
            """.trimIndent(),
            topic = "Fallback",
            slideCount = 6,
        )

        assertEquals("Deck", outline.title)
        assertEquals("Sub", outline.subtitle)
        assertEquals(1, outline.slides.size)
        assertEquals("One", outline.slides.first().title)
        assertEquals(listOf("A", "B"), outline.slides.first().bullets)
    }

    @Test
    fun invalidJsonCreatesFallbackOutline() {
        val outline = DocumentOutlineParser.parseOrFallback("not json", topic = "Mobile AI", slideCount = 3)

        assertEquals("Mobile AI", outline.title)
        assertEquals(3, outline.slides.size)
        assertTrue(outline.slides.first().bullets.first().contains("Mobile AI"))
    }
}
