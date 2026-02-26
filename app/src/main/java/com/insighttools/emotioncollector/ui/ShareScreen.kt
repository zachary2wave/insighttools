package com.insighttools.emotioncollector.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.insighttools.emotioncollector.data.RecordingStore
import com.insighttools.emotioncollector.data.SharePackager
import com.insighttools.emotioncollector.model.RecordingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareScreen(
    recordingStore: RecordingStore,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharePackager = remember(context) { SharePackager(context) }
    var records by remember { mutableStateOf(emptyList<RecordingItem>()) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var sharing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<RecordingItem?>(null) }

    fun refresh() {
        records = recordingStore.listRecordings()
        val shareableIds = records.filter { it.isShareable() }.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(shareableIds)
    }

    val shareableIds = remember(records) {
        records.filter { it.isShareable() }.map { it.id }.toSet()
    }
    val allSelected = shareableIds.isNotEmpty() && selectedIds.containsAll(shareableIds)

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "已保存样本：${records.size}（可分享 ${shareableIds.size}）",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "分享时会把所选记录对应的 json 和视频文件打包为 zip 后再发送。",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    selectedIds = if (allSelected) {
                        emptySet()
                    } else {
                        shareableIds
                    }
                },
                enabled = shareableIds.isNotEmpty() && !sharing,
            ) {
                Text(if (allSelected) "取消全选" else "全选")
            }

            OutlinedButton(onClick = { refresh() }, enabled = !sharing) {
                Text("刷新")
            }

            Button(
                onClick = {
                    val selectedRecords = records.filter { it.id in selectedIds }
                    if (selectedRecords.isEmpty()) {
                        status = "请先选择要分享的数据。"
                        return@Button
                    }

                    coroutineScope.launch {
                        sharing = true
                        status = "正在打包，请稍候..."
                        val packageResult = withContext(Dispatchers.IO) {
                            runCatching { sharePackager.packageAsZip(selectedRecords) }
                        }
                        sharing = false

                        packageResult.onSuccess { result ->
                            val shareResult = shareZipArchive(context, result.archiveFile)
                            status = if (shareResult.isSuccess) {
                                "已打开分享面板：打包 ${result.packedCount} 条，跳过 ${result.skippedCount} 条。"
                            } else {
                                "打包成功，但分享失败：${shareResult.exceptionOrNull()?.message}"
                            }
                        }.onFailure { error ->
                            status = "打包失败：${error.message}"
                        }
                    }
                },
                enabled = selectedIds.isNotEmpty() && !sharing,
            ) {
                Text("打包并分享")
            }
        }

        if (sharing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        status?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(records, key = { it.id }) { item ->
                val isShareable = item.isShareable()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.id in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) {
                                    selectedIds + item.id
                                } else {
                                    selectedIds - item.id
                                }
                            },
                            enabled = isShareable && !sharing,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("表情：${item.selectedEmo}")
                            if (!item.dialogue.isNullOrBlank()) {
                                Text("台词：${item.dialogue}")
                            } else {
                                Text("台词：无")
                            }
                            Text("分数：${item.score}/10")
                            Text("视频：${item.videoFile.name}")
                            Text(
                                text = if (isShareable) "文件状态：完整" else "文件状态：缺失（不可分享）",
                                color = if (isShareable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        OutlinedButton(
                            onClick = { previewItem = item },
                            enabled = item.videoFile.exists() && !sharing,
                        ) {
                            Text("预览")
                        }
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text("预览：${item.videoFile.name}") },
            text = {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { previewContext ->
                        VideoView(previewContext).apply {
                            setVideoURI(item.videoFile.toUri())
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = true
                                start()
                            }
                        }
                    },
                    update = { view ->
                        view.setVideoURI(item.videoFile.toUri())
                        view.start()
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) {
                    Text("关闭")
                }
            },
        )
    }
}

private fun RecordingItem.isShareable(): Boolean {
    return metadataFile.exists() && videoFile.exists()
}

private fun shareZipArchive(context: Context, zipFile: java.io.File): Result<Unit> {
    return runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "情绪数据采集打包文件")
            putExtra(Intent.EXTRA_TEXT, "这是情绪数据采集 APK 生成的打包文件。")
            clipData = ClipData.newUri(context.contentResolver, zipFile.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val resolvedActivities =
            context.packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolvedActivities.isEmpty()) {
            throw IllegalStateException("未找到可分享该文件的应用。")
        }
        resolvedActivities.forEach { resolveInfo ->
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val chooser = Intent.createChooser(shareIntent, "分享压缩包")
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
