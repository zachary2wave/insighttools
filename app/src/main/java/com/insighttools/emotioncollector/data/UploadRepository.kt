package com.insighttools.emotioncollector.data

import com.insighttools.emotioncollector.model.RecordingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class UploadRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadRecord(
        uploadUrl: String,
        item: RecordingItem,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!item.videoFile.exists()) {
            return@withContext Result.failure(IOException("视频文件缺失: ${item.videoFile.absolutePath}"))
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "metadata",
                item.metadataFile.name,
                item.metadataFile.asRequestBody("application/json; charset=utf-8".toMediaType()),
            )
            .addFormDataPart(
                "video",
                item.videoFile.name,
                item.videoFile.asRequestBody("video/mp4".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(body)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("上传失败: HTTP ${response.code}"))
                }
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
