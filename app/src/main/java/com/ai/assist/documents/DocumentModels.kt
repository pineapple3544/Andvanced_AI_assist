package com.ai.assist.documents

import com.ai.assist.domain.AiMode

enum class DocumentFormat(val extension: String, val mimeType: String) {
    Pptx("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    Pdf("pdf", "application/pdf");

    companion object {
        fun from(value: String?, fallbackText: String = ""): DocumentFormat {
            val combined = listOfNotNull(value, fallbackText).joinToString(" ").lowercase()
            return when {
                combined.contains("pdf") -> Pdf
                combined.contains("ppt") || combined.contains("presentation") || combined.contains("slide") -> Pptx
                else -> Pptx
            }
        }
    }
}

data class DocumentGenerationRequest(
    val topic: String,
    val format: DocumentFormat,
    val style: String = "simple",
    val slideCount: Int = 6,
    val selectedMode: AiMode,
)

data class DocumentOutline(
    val title: String,
    val subtitle: String,
    val slides: List<SlideOutline>,
)

data class SlideOutline(
    val title: String,
    val bullets: List<String>,
    val speakerNote: String? = null,
)

data class DocumentGenerationResult(
    val filePath: String,
    val openPath: String,
    val displayLocation: String,
    val mimeType: String,
    val outline: DocumentOutline,
)

data class PendingDocumentRequest(
    val topic: String,
    val format: DocumentFormat,
    val style: String = "simple",
    val slideCount: Int = 6,
    val originalPrompt: String = "",
) {
    val summary: String
        get() = "${format.extension.uppercase()} template about \"$topic\" ($slideCount slides, $style)"
}
