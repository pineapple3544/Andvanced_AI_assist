package com.ai.assist.ai

import com.ai.assist.domain.AiMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AiModeRoutingTest {
    @Test
    fun modesRemainStableForManualSelection() {
        assertEquals(listOf(AiMode.Local, AiMode.Hybrid, AiMode.ApiOnly), AiMode.entries)
        assertEquals("Local + API", AiMode.Hybrid.label)
    }

    @Test
    fun sliderPositionsMapToModes() {
        assertEquals(0f, AiMode.Local.sliderPosition)
        assertEquals(1f, AiMode.Hybrid.sliderPosition)
        assertEquals(2f, AiMode.ApiOnly.sliderPosition)
        assertEquals(AiMode.Local, AiMode.fromSliderPosition(0f))
        assertEquals(AiMode.Hybrid, AiMode.fromSliderPosition(1f))
        assertEquals(AiMode.ApiOnly, AiMode.fromSliderPosition(2f))
    }
}
