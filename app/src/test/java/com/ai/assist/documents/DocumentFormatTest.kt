package com.ai.assist.documents

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFormatTest {
    @Test
    fun exactPdfFormatCreatesPdf() {
        assertEquals(DocumentFormat.Pdf, DocumentFormat.from("pdf"))
    }

    @Test
    fun fallbackTextCanSelectPdfWhenFormatIsMissing() {
        assertEquals(DocumentFormat.Pdf, DocumentFormat.from(null, "Gemma MCP PDF template"))
    }

    @Test
    fun originalPromptPdfOverridesWrongPptxToolArgument() {
        assertEquals(DocumentFormat.Pdf, DocumentFormat.from("pptx", "Gemma MCP PDF template"))
    }

    @Test
    fun pptIsDefaultWhenNoPdfHintExists() {
        assertEquals(DocumentFormat.Pptx, DocumentFormat.from(null, "mobile AI assistant"))
    }
}
