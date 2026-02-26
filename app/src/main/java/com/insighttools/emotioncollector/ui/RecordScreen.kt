package com.insighttools.emotioncollector.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.insighttools.emotioncollector.data.AssetPromptRepository
import com.insighttools.emotioncollector.data.RecordingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun RecordScreen(
    promptRepository: AssetPromptRepository,
    recordingStore: RecordingStore,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    var currentPrompt by remember { mutableStateOf(promptRepository.getRandomPrompt()) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var capturedVideoFile by remember { mutableStateOf<File?>(null) }
    var score by remember { mutableFloatStateOf(5f) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val captureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val captureFile = pendingCaptureFile
            if (result.resultCode == Activity.RESULT_OK && captureFile != null && captureFile.exists()) {
                capturedVideoFile = captureFile
                status = "录制完成，请预览并打分。"
            } else {
                captureFile?.delete()
                status = "录制已取消。"
            }
            pendingCaptureFile = null
        }

    fun launchRecorder() {
        val captureFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
        val captureUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            captureFile,
        )
        pendingCaptureFile = captureFile
        captureLauncher.launch(
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            },
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val allGranted = grants[Manifest.permission.CAMERA] == true &&
                grants[Manifest.permission.RECORD_AUDIO] == true
            if (allGranted) {
                launchRecorder()
            } else {
                status = "需要相机和录音权限才能录制。"
            }
        }

    fun ensurePermissionsAndLaunch() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            launchRecorder()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "随机素材",
            style = MaterialTheme.typography.titleLarge,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F7FF),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "类别：${currentPrompt.category}")
                Text(text = "表情文件夹：${currentPrompt.memeFolder}")

                if (currentPrompt.imageAssetPath != null) {
                    AsyncImage(
                        model = "file:///android_asset/${currentPrompt.imageAssetPath}",
                        imageLoader = imageLoader,
                        contentDescription = "Emotion",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFE3E3E3)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFE3E3E3)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("该素材目录下暂未找到 gif/png/jpg/webp")
                    }
                }

                if (currentPrompt.dialogue.isNullOrBlank()) {
                    Text("该表情无台词（可直接演绎情绪）")
                } else {
                    Text(
                        text = "台词：${currentPrompt.dialogue}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { ensurePermissionsAndLaunch() },
                enabled = !saving,
            ) {
                Text("打开前摄录制")
            }
            OutlinedButton(
                onClick = {
                    capturedVideoFile?.delete()
                    capturedVideoFile = null
                    score = 5f
                    currentPrompt = promptRepository.getRandomPrompt()
                    status = "已切换到下一条随机素材。"
                },
                enabled = !saving,
            ) {
                Text("换一个素材")
            }
        }

        capturedVideoFile?.let { video ->
            Text(
                text = "录制预览",
                style = MaterialTheme.typography.titleMedium,
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        setVideoURI(video.toUri())
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            start()
                        }
                    }
                },
                update = { view ->
                    view.setVideoURI(video.toUri())
                    view.start()
                },
            )

            Text("自评分数：${score.roundToInt()} / 10")
            Slider(
                value = score,
                onValueChange = { score = it },
                valueRange = 0f..10f,
                steps = 9,
                enabled = !saving,
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        saving = true
                        try {
                            withContext(Dispatchers.IO) {
                                recordingStore.saveRecording(
                                    sourceVideoFile = video,
                                    prompt = currentPrompt,
                                    score = score.roundToInt(),
                                )
                            }
                            capturedVideoFile = null
                            score = 5f
                            currentPrompt = promptRepository.getRandomPrompt()
                            status = "保存成功，已进入下一条拍摄。"
                        } catch (t: Throwable) {
                            status = "保存失败：${t.message}"
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
            ) {
                Text("确认保存并下一条")
            }
        }

        if (saving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        status?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun openFrontCameraIntent(context: Context, videoFile: File): Intent {
    val captureUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        videoFile,
    )
    return Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
        putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)
        putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        putExtra("android.intent.extras.CAMERA_FACING", 1)
        putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}
