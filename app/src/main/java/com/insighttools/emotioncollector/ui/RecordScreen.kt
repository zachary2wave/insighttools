package com.insighttools.emotioncollector.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }

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
    var permissionsGranted by remember { mutableStateOf(hasRequiredPermissions(context)) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var capturedVideoFile by remember { mutableStateOf<File?>(null) }
    var score by remember { mutableFloatStateOf(5f) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            permissionsGranted = grants[Manifest.permission.CAMERA] == true &&
                grants[Manifest.permission.RECORD_AUDIO] == true
            if (!permissionsGranted) {
                status = "需要相机和录音权限才能录制。"
            }
        }

    DisposableEffect(previewView, permissionsGranted, lifecycleOwner) {
        val currentPreviewView = previewView
        if (!permissionsGranted || currentPreviewView == null) {
            onDispose { }
        } else {
            var disposed = false
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = kotlin.runCatching { cameraProviderFuture.get() }.getOrNull()
                    if (cameraProvider == null) {
                        if (!disposed) {
                            status = "相机初始化失败。"
                        }
                        return@addListener
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = currentPreviewView.surfaceProvider
                    }
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.FHD,
                                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                            ),
                        )
                        .build()
                    val capture = VideoCapture.withOutput(recorder)

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            capture,
                        )
                        videoCapture = capture
                        if (!disposed && status == null) {
                            status = "前摄已就绪。"
                        }
                    } catch (_: Exception) {
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                            )
                            videoCapture = capture
                            if (!disposed) {
                                status = "前摄不可用，已切换后摄。"
                            }
                        } catch (fallbackError: Exception) {
                            videoCapture = null
                            if (!disposed) {
                                status = "相机绑定失败：${fallbackError.message}"
                            }
                        }
                    }
                },
                mainExecutor,
            )

            onDispose {
                disposed = true
                activeRecording?.stop()
                activeRecording = null
                isRecording = false
                videoCapture = null
                if (cameraProviderFuture.isDone) {
                    kotlin.runCatching {
                        cameraProviderFuture.get().unbindAll()
                    }
                }
            }
        }
    }

    fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }

    fun startRecordingWithCameraX() {
        permissionsGranted = hasRequiredPermissions(context)
        if (!permissionsGranted) {
            requestPermissions()
            return
        }

        val capture = videoCapture
        if (capture == null) {
            status = "相机还在初始化，请稍后重试。"
            return
        }

        capturedVideoFile?.delete()
        capturedVideoFile = null
        val captureFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(captureFile).build()
        var pendingRecording = capture.output.prepareRecording(context, outputOptions)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        isRecording = true
        status = "录制中，点击“停止录制”结束。"
        activeRecording = pendingRecording.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    status = "录制中，点击“停止录制”结束。"
                }

                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    isRecording = false
                    if (event.hasError()) {
                        captureFile.delete()
                        status = "录制失败：错误码 ${event.error}"
                    } else {
                        capturedVideoFile = captureFile
                        status = "录制完成，请预览并打分。"
                    }
                }
            }
        }
    }

    fun stopRecordingWithCameraX() {
        activeRecording?.stop()
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

        Text(
            text = "前摄实时预览",
            style = MaterialTheme.typography.titleMedium,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F7FF),
            ),
        ) {
            if (permissionsGranted) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = this
                        }
                    },
                    update = {
                        previewView = it
                    },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFFE3E3E3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("请先授权相机和麦克风。")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isRecording) {
                Button(
                    onClick = { stopRecordingWithCameraX() },
                    enabled = !saving,
                ) {
                    Text("停止录制")
                }
            } else {
                Button(
                    onClick = {
                        if (permissionsGranted) {
                            startRecordingWithCameraX()
                        } else {
                            requestPermissions()
                        }
                    },
                    enabled = !saving,
                ) {
                    Text("开始录制")
                }
            }
            OutlinedButton(
                onClick = {
                    capturedVideoFile?.delete()
                    capturedVideoFile = null
                    score = 5f
                    currentPrompt = promptRepository.getRandomPrompt()
                    status = "已切换到下一条随机素材。"
                },
                enabled = !saving && !isRecording,
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
                enabled = !saving && !isRecording,
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
                enabled = !saving && !isRecording,
            ) {
                Text("确认保存并下一条")
            }
        }

        if (saving || isRecording) {
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

private fun hasRequiredPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
}
