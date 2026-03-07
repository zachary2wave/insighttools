package com.insighttools.emotioncollector

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.insighttools.emotioncollector.data.AssetPromptRepository
import com.insighttools.emotioncollector.data.RecordingStore
import com.insighttools.emotioncollector.ui.RecordScreen
import com.insighttools.emotioncollector.ui.ShareScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionCollectorApp() {
    val context = LocalContext.current
    val promptRepository = remember { AssetPromptRepository(context.assets) }
    val recordingStore = remember { RecordingStore(context) }
    var tabIndex by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("情绪数据采集") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFECEBFF),
                    ),
                )
            },
        ) { innerPadding ->
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("录制模式") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("分享模式") },
                    )
                }

                when (tabIndex) {
                    0 -> RecordScreen(
                        promptRepository = promptRepository,
                        recordingStore = recordingStore,
                    )

                    else -> ShareScreen(
                        recordingStore = recordingStore,
                    )
                }
            }
        }
    }
}
