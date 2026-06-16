package com.ai.assist.domain

enum class AiMode(val label: String) {
    Local("Local only"),
    Hybrid("Local + API"),
    ApiOnly("API only");

    val sliderPosition: Float
        get() = when (this) {
            Local -> 0f
            Hybrid -> 1f
            ApiOnly -> 2f
        }

    companion object {
        fun fromSliderPosition(value: Float): AiMode = when (value.toInt().coerceIn(0, 2)) {
            0 -> Local
            1 -> Hybrid
            else -> ApiOnly
        }
    }
}
