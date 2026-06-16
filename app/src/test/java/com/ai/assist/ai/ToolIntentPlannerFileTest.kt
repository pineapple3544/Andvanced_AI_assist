package com.ai.assist.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToolIntentPlannerFileTest {
    @Test
    fun fileRequestMapsToOpenFile() {
        val call = ToolIntentPlanner().plan("open file report.pdf")

        assertNotNull(call)
        assertEquals("openFile", call?.name)
        assertEquals("report.pdf", call?.arguments?.get("pathOrUri"))
    }
}
