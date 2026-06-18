package com.ai.assist.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToolIntentPlannerDocumentTest {
    private val planner = ToolIntentPlanner()

    @Test
    fun pptTemplateRequestMapsToCreateDocument() {
        val call = planner.plan("make a ppt about mobile AI assistant")

        assertNotNull(call)
        assertEquals("createDocument", call?.name)
        assertEquals("pptx", call?.arguments?.get("format"))
        assertEquals("mobile AI assistant", call?.arguments?.get("topic"))
        assertEquals("6", call?.arguments?.get("slideCount"))
    }

    @Test
    fun koreanPdfTemplateRequestMapsToCreateDocument() {
        val call = planner.plan("Gemma와 MCP 구조를 설명하는 PDF 템플릿 만들어줘")

        assertNotNull(call)
        assertEquals("createDocument", call?.name)
        assertEquals("pdf", call?.arguments?.get("format"))
    }

    @Test
    fun slideCountIsParsed() {
        val call = planner.plan("create 4 slides ppt about Android MCP")

        assertEquals("createDocument", call?.name)
        assertEquals("4", call?.arguments?.get("slideCount"))
    }
}
