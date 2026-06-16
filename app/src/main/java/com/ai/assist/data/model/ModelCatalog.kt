package com.ai.assist.data.model

data class ModelCandidate(
    val name: String,
    val url: String,
    val sizeLabel: String,
    val description: String,
)

object ModelCatalog {
    val defaultModels = listOf(
        ModelCandidate(
            name = "Gemma4:E2B",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
            sizeLabel = "User supplied",
            description = "Gemma4 : E2B model is downloading.",
        ),
        ModelCandidate(
            name = "Local file placeholder",
            url = "",
            sizeLabel = "Manual",
            description = "Use the model path field after copying a .litertlm file into app storage.",
        ),
    )
}
