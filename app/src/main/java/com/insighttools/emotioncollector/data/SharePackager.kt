package com.insighttools.emotioncollector.data

import android.content.Context
import com.insighttools.emotioncollector.model.RecordingItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ShareArchiveResult(
    val archiveFile: File,
    val packedCount: Int,
    val skippedCount: Int,
)

class SharePackager(context: Context) {
    private val exportDir = File(context.cacheDir, "share_exports").apply { mkdirs() }

    fun packageAsZip(records: List<RecordingItem>): ShareArchiveResult {
        require(records.isNotEmpty()) { "没有可打包的录制记录。" }

        val outputZip = File(exportDir, "emotion_records_${System.currentTimeMillis()}.zip")
        var packed = 0
        var skipped = 0

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zipOut ->
            records.forEach { item ->
                if (!item.metadataFile.exists() || !item.videoFile.exists()) {
                    skipped++
                    return@forEach
                }

                val itemDir = "record_${item.id}/"
                addDirectory(zipOut, itemDir)
                addFile(zipOut, item.metadataFile, itemDir + item.metadataFile.name)
                addFile(zipOut, item.videoFile, itemDir + item.videoFile.name)
                packed++
            }
        }

        if (packed == 0) {
            outputZip.delete()
            throw IllegalStateException("所选记录中没有可用的 json+视频 文件。")
        }

        return ShareArchiveResult(
            archiveFile = outputZip,
            packedCount = packed,
            skippedCount = skipped,
        )
    }

    private fun addDirectory(zipOut: ZipOutputStream, directoryName: String) {
        val entry = ZipEntry(directoryName).apply { time = System.currentTimeMillis() }
        zipOut.putNextEntry(entry)
        zipOut.closeEntry()
    }

    private fun addFile(
        zipOut: ZipOutputStream,
        sourceFile: File,
        entryName: String,
    ) {
        val entry = ZipEntry(entryName).apply { time = sourceFile.lastModified() }
        zipOut.putNextEntry(entry)
        BufferedInputStream(FileInputStream(sourceFile)).use { input ->
            input.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }
}
