package com.insighttools.emotioncollector.data

import android.content.res.AssetManager
import com.insighttools.emotioncollector.model.PromptMediaType
import com.insighttools.emotioncollector.model.PromptSelection
import kotlin.random.Random

class AssetPromptRepository(
    private val assetManager: AssetManager,
    private val rootPath: String = "emotions",
) {
    private val random = Random(System.currentTimeMillis())

    @Volatile
    private var cachedPromptSources: List<PromptSource>? = null

    fun getRandomPrompt(): PromptSelection {
        val sources = cachedPromptSources ?: loadPromptSources().also { cachedPromptSources = it }
        if (sources.isEmpty()) {
            return PromptSelection(
                category = "placeholder",
                memeFolder = "placeholder",
                mediaAssetPath = null,
                mediaType = PromptMediaType.UNKNOWN,
                dialogue = "请先在 assets/emotions 下补充素材目录与台词 CSV。",
            )
        }

        val source = sources[random.nextInt(sources.size)]
        val mediaCandidates = loadMediaCandidates(source)
        val media = if (mediaCandidates.isEmpty()) {
            null
        } else {
            mediaCandidates[random.nextInt(mediaCandidates.size)]
        }
        val dialogues = loadDialogues(source, media)
        val dialogue = if (dialogues.isEmpty()) null else dialogues[random.nextInt(dialogues.size)]

        return PromptSelection(
            category = source.category,
            memeFolder = source.memeFolder,
            mediaAssetPath = media?.assetPath,
            mediaType = media?.type ?: PromptMediaType.UNKNOWN,
            dialogue = dialogue,
        )
    }

    private fun loadPromptSources(): List<PromptSource> {
        val results = mutableListOf<PromptSource>()
        val categories = assetManager.list(rootPath).orEmpty().sorted()
        for (category in categories) {
            val categoryPath = "$rootPath/$category"
            val memeFolders = assetManager.list(categoryPath).orEmpty().sorted()
            for (meme in memeFolders) {
                results += PromptSource(category = category, memeFolder = meme)
            }
        }
        return results
    }

    private fun loadMediaCandidates(source: PromptSource): List<MediaCandidate> {
        val folderPath = "$rootPath/${source.category}/${source.memeFolder}"
        val children = assetManager.list(folderPath).orEmpty()
        return children.mapNotNull { fileName ->
            val type = fileName.toPromptMediaType() ?: return@mapNotNull null
            MediaCandidate(
                fileName = fileName,
                assetPath = "$folderPath/$fileName",
                type = type,
            )
        }
    }

    private fun loadDialogues(
        source: PromptSource,
        media: MediaCandidate?,
    ): List<String> {
        val folderPath = "$rootPath/${source.category}/${source.memeFolder}"
        val children = assetManager.list(folderPath).orEmpty().toSet()
        val candidateFileNames = buildDialogueCandidateFileNames(media)

        for (fileName in candidateFileNames) {
            if (fileName !in children) continue
            val lines = readDialogueLines("$folderPath/$fileName")
            if (lines.isNotEmpty()) return lines
        }

        return emptyList()
    }

    private fun buildDialogueCandidateFileNames(media: MediaCandidate?): List<String> {
        val candidates = mutableListOf<String>()
        if (media != null) {
            val mediaFileName = media.fileName
            val mediaBaseName = mediaFileName.substringBeforeLast('.')
            candidates += "$mediaFileName.dialog.csv"
            candidates += "$mediaFileName.dialogue.csv"
            candidates += "$mediaFileName.csv"
            candidates += "$mediaBaseName.dialog.csv"
            candidates += "$mediaBaseName.dialogue.csv"
            candidates += "$mediaBaseName.csv"
            candidates += "dialog_$mediaFileName.csv"
            candidates += "dialog_$mediaBaseName.csv"
            candidates += "${mediaBaseName}_dialog.csv"
            candidates += "${mediaBaseName}_dialogue.csv"
        }
        candidates += "dialogue.csv"
        candidates += "dialog.csv"
        return candidates.distinct()
    }

    private fun readDialogueLines(csvPath: String): List<String> {
        return try {
            assetManager.open(csvPath).bufferedReader().useLines { lines ->
                lines.map { parseDialogueCell(it) }
                    .filter { it.isNotBlank() && !it.equals("dialogue", ignoreCase = true) }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDialogueCell(line: String): String {
        val values = parseCsv(line)
        return values.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }

    private fun parseCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }

                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
            i++
        }
        result += current.toString()
        return result
    }

    private fun String.toPromptMediaType(): PromptMediaType? {
        val lower = lowercase()
        return when {
            lower.endsWith(".gif") ||
                lower.endsWith(".png") ||
                lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".webp") -> PromptMediaType.IMAGE

            lower.endsWith(".mp4") ||
                lower.endsWith(".m4v") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mov") -> PromptMediaType.VIDEO

            else -> null
        }
    }

    private data class PromptSource(
        val category: String,
        val memeFolder: String,
    )

    private data class MediaCandidate(
        val fileName: String,
        val assetPath: String,
        val type: PromptMediaType,
    )
}
