@file:OptIn(UnstableApi::class)

package com.example.iptvplayer.ui.player

import android.media.AudioManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.data.model.ChannelSwitchMode
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.viewmodel.PlayerUiState
import com.example.iptvplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * 横屏非全屏布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeNonFullscreenLayout(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    videoHeight: Dp,
    isControlBarVisibleInNormalMode: Boolean,
    scope: CoroutineScope,
    configuration: android.content.res.Configuration,
    audioManager: AudioManager,
    window: Window,
    view: View,
    context: android.content.Context,
    channelSwitchMode: ChannelSwitchMode?,
    isChannelSwitching: MutableState<Boolean>,
    volumePercentage: Int?,
    brightnessPercentage: Int?,
    onSwitchChannel: (Int) -> Unit,
    onVolumePercentageChange: (Int?) -> Unit,
    onBrightnessPercentageChange: (Int?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentChannel?.name ?: "正在加载...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 左侧 - 视频播放区域
            Column(
                modifier = Modifier
                    .weight(1f) // 修改为1f，使左右区域各占一半宽度
            ) {
                // 视频播放窗口
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f) // 使用权重而不是固定高度，确保在不同屏幕尺寸上都能正确显示
                        .background(MaterialTheme.colorScheme.background)
                        .addDoubleTapGesture(
                            channelSwitchMode = channelSwitchMode,
                            scope = scope,
                            viewModel = viewModel,
                            onSwitchChannel = onSwitchChannel,
                            isFullscreen = false,
                            isLandscape = true,
                            onToggleControlAndEpgVisibility = {
                                // 在非全屏模式下，控制栏默认可见，不需要切换
                                // 但如果是全屏模式，则切换控制栏和EPG信息的可见性
                            }
                        )
                        .addHorizontalDragGesture(
                            channelSwitchMode = channelSwitchMode,
                            scope = scope,
                            viewModel = viewModel,
                            onSwitchChannel = onSwitchChannel,
                            isChannelSwitching = isChannelSwitching
                        )
                        .addVerticalDragGesture(
                            audioManager = audioManager,
                            view = view,
                            window = window,
                            scope = scope,
                            onVolumePercentageChange = onVolumePercentageChange,
                            onBrightnessPercentageChange = onBrightnessPercentageChange
                        )
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { playerView ->
                            playerView.player = viewModel.getExoPlayer()
                        }
                    )
                    
                    // 音量指示器
                    volumePercentage?.let { percentage ->
                        VolumeIndicator(
                            percentage = percentage,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                    
                    // 亮度指示器
                    brightnessPercentage?.let { percentage ->
                        BrightnessIndicator(
                            percentage = percentage,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                    
                    // 加载指示器
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    // 错误信息
                    uiState.error?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error,
                                color = Color.Red
                            )
                        }
                    }
                }
                
                // EPG信息显示
                uiState.currentEpg?.let { epg ->
                    EpgInfoCard(epg, isFullscreen = uiState.isFullscreen)
                    // 在卡片下方添加进度条
                    EpgProgressIndicator(epg, isFullscreen = uiState.isFullscreen)
                }
                
                // 控制栏
                if (isControlBarVisibleInNormalMode) {
                    ControlButtonBar(
                        player = viewModel.getExoPlayer(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        onToggleFullscreen = {
                            viewModel.toggleFullscreen()
                        },
                        isFullscreen = uiState.isFullscreen
                    )
                }
            }
            
            // 右侧 - 节目列表
            Column(
                modifier = Modifier
                    .weight(1f) // 修改为1f，使左右区域各占一半宽度
            ) {
                Text(
                    text = "节目列表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                // 使用UpcomingEpgList组件显示节目列表，保持与竖屏模式的一致性
                if (uiState.upcomingEpg.isNotEmpty()) {
                    UpcomingEpgList(
                        epgList = uiState.upcomingEpg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    Text(
                        text = "暂无节目信息",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}