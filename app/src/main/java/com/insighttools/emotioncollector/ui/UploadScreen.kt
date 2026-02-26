package com.insighttools.emotioncollector.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.insighttools.emotioncollector.data.RecordingStore
import com.insighttools.emotioncollector.data.UploadRepository
import com.insighttools.emotioncollector.model.RecordingItem
import kotlinx.coroutines.launch

@Composable
fun UploadScreen(
    recordingStore: RecordingStore,
    uploadRepository: UploadRepository,
    uploadUrl: String,
) {
    val coroutineScope = rememberCoroutineScope()
    var records by remember { mutableStateOf(emptyList<RecordingItem>()) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var uploading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<RecordingItem?>(null) }

    fun refresh() {
        records = recordingStore.listRecordings()
        selectedIds = selectedIds.intersect(records.map { it.id }.toSet())
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "上传地址：$uploadUrl")
        Text(
            text = "已保存样本：${records.size}",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    selectedIds = if (selectedIds.size == records.size) {
                        emptySet()
                    } else {
                        records.map { it.id }.toSet()
                    }
                },
                enabled = records.isNotEmpty() && !uploading,
            ) {
                Text(if (selectedIds.size == records.size) "取消全选" else "全选")
            }

            OutlinedButton(onClick = { refresh() }, enabled = !uploading) {
                Text("刷新")
            }

            Button(
                onClick = {
                    val selectedRecords = records.filter { it.id in selectedIds }
                    if (selectedRecords.isEmpty()) {
                        status = "请先选择要上传的数据。"
                        return@Button
                    }
                    coroutineScope.launch {
                        uploading = true
                        var success = 0
                        var failed = 0
                        for (item in selectedRecords) {
                            val result = uploadRepository.uploadRecord(uploadUrl, item)
                            if (result.isSuccess) {
                                success++
                                recordingStore.markUploaded(item)
                            } else {
                                failed++
                            }
                        }
                        refresh()
                        uploading = false
                        status = "上传完成：成功 $success 条，失败 $failed 条。"
                    }
                },
                enabled = selectedIds.isNotEmpty() && !uploading,
            ) {
                Text("上传选中项")
            }
        }

        if (uploading) {
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.uploaded) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
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
                            enabled = !uploading,
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
                            Text(if (item.uploaded) "状态：已上传" else "状态：未上传")
                        }
                        OutlinedButton(
                            onClick = { previewItem = item },
                            enabled = item.videoFile.exists() && !uploading,
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
                    factory = { context ->
                        VideoView(context).apply {
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
