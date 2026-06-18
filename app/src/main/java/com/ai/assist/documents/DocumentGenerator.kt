package com.ai.assist.documents

import android.content.Context
import com.ai.assist.ai.LocalLiteRtEngine
import com.ai.assist.ai.OpenAiCompatibleEngine
import com.ai.assist.domain.AiMode
import com.ai.assist.domain.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentGenerator(
    private val context: Context,
    private val localEngine: LocalLiteRtEngine,
    private val apiEngine: OpenAiCompatibleEngine,
    private val pdfRenderer: PdfDocumentRenderer = PdfDocumentRenderer(),
    private val pptxRenderer: PptxDocumentRenderer = PptxDocumentRenderer(),
    private val publicDocumentStore: PublicDocumentStore = PublicDocumentStore(context),
) {
    suspend fun generate(
        request: DocumentGenerationRequest,
        settings: AppSettings,
    ): DocumentGenerationResult = withContext(Dispatchers.IO) {
        val outline = createOutline(request, settings)
        val output = DocumentFileNamer.outputFile(context, request.topic, request.format)
        when (request.format) {
            DocumentFormat.Pdf -> pdfRenderer.render(outline, output)
            DocumentFormat.Pptx -> pptxRenderer.render(outline, output)
        }
        val published = publicDocumentStore.publish(output, request.format)
        DocumentGenerationResult(
            filePath = output.absolutePath,
            openPath = published.openPath,
            displayLocation = published.displayLocation,
            mimeType = request.format.mimeType,
            outline = outline,
        )
    }

    private suspend fun createOutline(
        request: DocumentGenerationRequest,
        settings: AppSettings,
    ): DocumentOutline {
        val prompt = outlinePrompt(request)
        val text = when (request.selectedMode) {
            AiMode.Local -> localEngine.generateText(prompt, settings).getOrElse { "" }
            AiMode.Hybrid -> {
                if (settings.api.apiKey.isBlank()) {
                    error("API key is empty. Local + API document generation needs an API key in Settings.")
                }
                val localSeed = localEngine.generateText(localSeedPrompt(request), settings).getOrElse { request.topic }
                apiEngine.completeText(settings, OUTLINE_SYSTEM_PROMPT, "$prompt\n\nLocal seed:\n$localSeed")
            }
            AiMode.ApiOnly -> apiEngine.completeText(settings, OUTLINE_SYSTEM_PROMPT, prompt)
        }
        return DocumentOutlineParser.parseOrFallback(text, request.topic, request.slideCount)
    }

    private fun outlinePrompt(request: DocumentGenerationRequest): String = """
        Create a lightweight presentation outline as strict JSON only.
        Topic: ${request.topic}
        Format: ${request.format.extension}
        Style: ${request.style}
        Slide count: ${request.slideCount}
        JSON shape:
        {
          "title": "string",
          "subtitle": "string",
          "slides": [
            {"title": "string", "bullets": ["string", "string", "string"], "speakerNote": "string"}
          ]
        }
        Keep bullets short. Use Korean if the topic is Korean.
    """.trimIndent()

    private fun localSeedPrompt(request: DocumentGenerationRequest): String = """
        Summarize the user's document request into a short outline seed.
        Topic: ${request.topic}
        Slides: ${request.slideCount}
        Style: ${request.style}
    """.trimIndent()

    private companion object {
        const val OUTLINE_SYSTEM_PROMPT =
            "You create concise slide deck outlines. Return strict JSON only. Do not include Markdown fences."
    }
}
