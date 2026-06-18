package com.ai.assist.documents

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.assertTrue
import org.junit.Test

class PptxDocumentRendererTest {
    @Test
    fun writesMinimalPptxPackage() {
        val file = File.createTempFile("ai-assist-test", ".pptx")
        val outline = DocumentOutline(
            title = "Mobile AI",
            subtitle = "Test deck",
            slides = listOf(
                SlideOutline("Intro", listOf("Local", "API")),
                SlideOutline("Plan", listOf("MCP", "Tools")),
            ),
        )

        PptxDocumentRenderer().render(outline, file)

        ZipFile(file).use { zip ->
            assertTrue(zip.getEntry("[Content_Types].xml") != null)
            assertTrue(zip.getEntry("_rels/.rels") != null)
            assertTrue(zip.getEntry("ppt/presentation.xml") != null)
            assertTrue(zip.getEntry("ppt/_rels/presentation.xml.rels") != null)
            assertTrue(zip.getEntry("ppt/slides/slide1.xml") != null)
            assertTrue(zip.getEntry("ppt/slides/slide2.xml") != null)
        }
    }
}
