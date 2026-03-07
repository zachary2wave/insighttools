package com.insighttools.emotioncollector.model

import java.io.File

enum class PromptMediaType {
    IMAGE,
    VIDEO,
    UNKNOWN,
}

data class PromptSelection(
    val category: String,
    val memeFolder: String,
    val mediaAssetPath: String?,
    val mediaType: PromptMediaType,
    val dialogue: String?,
)

data class RecordingItem(
    val id: String,
    val selectedEmo: String,
    val dialogue: String?,
    val score: Int,
    val videoFile: File,
    val metadataFile: File,
)
