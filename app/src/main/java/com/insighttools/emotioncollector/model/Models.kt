package com.insighttools.emotioncollector.model

import java.io.File

data class PromptSelection(
    val category: String,
    val memeFolder: String,
    val imageAssetPath: String?,
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
