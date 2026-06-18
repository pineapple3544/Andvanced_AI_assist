package com.ai.assist.documents

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFileNamerTest {
    @Test
    fun safeTopicRemovesUnsafeCharacters() {
        assertEquals("mobile-ai-assistant-v1", DocumentFileNamer.safeTopic("Mobile AI Assistant v1!!"))
    }

    @Test
    fun emptySafeTopicFallsBackToDocument() {
        assertEquals("document", DocumentFileNamer.safeTopic("!!!"))
    }
}
