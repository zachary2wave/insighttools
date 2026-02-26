package com.insighttools.emotioncollector.data

import android.content.Context
import com.insighttools.emotioncollector.model.PromptSelection
import com.insighttools.emotioncollector.model.RecordingItem
import org.json.JSONObject
import java.io.File

class RecordingStore(context: Context) {
    private val baseDir = File(context.filesDir, "recordings").apply { mkdirs() }
    private val videoDir = File(baseDir, "videos").apply { mkdirs() }
    private val metadataDir = File(baseDir, "metadata").apply { mkdirs() }

    fun saveRecording(
        sourceVideoFile: File,
        prompt: PromptSelection,
        score: Int,
    ): RecordingItem {
        require(sourceVideoFile.exists()) { "录制视频不存在" }

        val id = System.currentTimeMillis().toString()
        val videoFile = File(videoDir, "recording_$id.mp4")
        sourceVideoFile.copyTo(videoFile, overwrite = true)
        sourceVideoFile.delete()

        val selectedEmo = prompt.imageAssetPath?.substringAfterLast('/') ?: prompt.memeFolder
        val metadataFile = File(metadataDir, "recording_$id.json")
        val json = JSONObject().apply {
            put("selectedemo", selectedEmo)
            put("Dialogue", prompt.dialogue ?: "")
            put("recordedvedio", videoFile.absolutePath)
            put("score", score)
            put("category", prompt.category)
            put("memeFolder", prompt.memeFolder)
            put("uploaded", false)
            put("createdAt", System.currentTimeMillis())
        }
        metadataFile.writeText(json.toString(2))

        return RecordingItem(
            id = id,
            selectedEmo = selectedEmo,
            dialogue = prompt.dialogue,
            score = score,
            videoFile = videoFile,
            metadataFile = metadataFile,
            uploaded = false,
        )
    }

    fun listRecordings(): List<RecordingItem> {
        val files = metadataDir
            .listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

        return files.mapNotNull { parseMetadata(it) }
    }

    fun markUploaded(item: RecordingItem) {
        val json = JSONObject(item.metadataFile.readText())
        json.put("uploaded", true)
        json.put("uploadedAt", System.currentTimeMillis())
        item.metadataFile.writeText(json.toString(2))
    }

    private fun parseMetadata(metadataFile: File): RecordingItem? {
        return try {
            val json = JSONObject(metadataFile.readText())
            val videoPath = json.optString("recordedvedio")
            val videoFile = File(videoPath)
            val id = metadataFile.nameWithoutExtension.removePrefix("recording_")
            RecordingItem(
                id = id,
                selectedEmo = json.optString("selectedemo"),
                dialogue = json.optString("Dialogue").ifBlank { null },
                score = json.optInt("score", 0),
                videoFile = videoFile,
                metadataFile = metadataFile,
                uploaded = json.optBoolean("uploaded", false),
            )
        } catch (_: Exception) {
            null
        }
    }
}
