package com.insighttools.emotioncollector.data

import android.content.res.AssetManager
import com.insighttools.emotioncollector.model.PromptSelection
import kotlin.random.Random

class AssetPromptRepository(
    private val assetManager: AssetManager,
    private val rootPath: String = "emotions",
) {
    private val random = Random(System.currentTimeMillis())

    @Volatile
    private var cachedPrompts: List<PromptSelection>? = null

    fun getRandomPrompt(): PromptSelection {
        val prompts = cachedPrompts ?: loadAllPrompts().also { cachedPrompts = it }
        if (prompts.isEmpty()) {
            return PromptSelection(
                category = "placeholder",
                memeFolder = "placeholder",
                imageAssetPath = null,
                dialogue = "请先在 assets/emotions 下补充素材目录与台词 CSV。",
            )
        }
        return prompts[random.nextInt(prompts.size)].let { prompt ->
            val dialogueChoices = loadDialogues(prompt)
            if (dialogueChoices.isEmpty()) {
                prompt.copy(dialogue = null)
            } else {
                prompt.copy(dialogue = dialogueChoices[random.nextInt(dialogueChoices.size)])
            }
        }
    }

    private fun loadAllPrompts(): List<PromptSelection> {
        val results = mutableListOf<PromptSelection>()
        val categories = assetManager.list(rootPath).orEmpty().sorted()
        for (category in categories) {
            val categoryPath = "$rootPath/$category"
            val memeFolders = assetManager.list(categoryPath).orEmpty().sorted()
            for (meme in memeFolders) {
                val memePath = "$categoryPath/$meme"
                val children = assetManager.list(memePath).orEmpty()
                val image = children.firstOrNull { it.isImageFile() }?.let { "$memePath/$it" }
                results += PromptSelection(
                    category = category,
                    memeFolder = meme,
                    imageAssetPath = image,
                    dialogue = null,
                )
            }
        }
        return results
    }

    private fun loadDialogues(prompt: PromptSelection): List<String> {
        val csvPath = "$rootPath/${prompt.category}/${prompt.memeFolder}/dialogue.csv"
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

    private fun String.isImageFile(): Boolean {
        val lower = lowercase()
        return lower.endsWith(".gif") ||
            lower.endsWith(".png") ||
            lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".webp")
    }
}
