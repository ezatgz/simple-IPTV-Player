@file:OptIn(UnstableApi::class)
package com.example.iptvplayer.ui.player

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.viewmodel.PlayerUiState
import com.example.iptvplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import android.media.AudioManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.sign
import com.example.iptvplayer.data.model.SettingsManager
import com.example.iptvplayer.data.model.ChannelSwitchMode
import com.example.iptvplayer.data.model.FullscreenOrientationMode
import com.example.iptvplayer.ui.player.ControlButtonBar
import com.example.iptvplayer.ui.player.VolumeIndicator
import com.example.iptvplayer.ui.player.BrightnessIndicator
import com.example.iptvplayer.ui.player.addDoubleTapGesture
import com.example.iptvplayer.ui.player.addHorizontalDragGesture
import com.example.iptvplayer.ui.player.addVerticalDragGesture
import com.example.iptvplayer.ui.player.rememberFullscreenManager
import kotlinx.coroutines.Job

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    channelId: Int,
    onBack: () -> Unit,
    onSwitchChannel: (Int) -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    val view = LocalView.current
    val window = (view.context as android.app.Activity).window
    
    // 获取设置管理��?
    val settingsManager = remember { SettingsManager(context) }
    var channelSwitchMode by remember { mutableStateOf<ChannelSwitchMode?>(null) }
    var fullscreenOrientationMode by remember { mutableStateOf<FullscreenOrientationMode?>(null) }
        
    // 读取当前频道切换控制模式设置
    LaunchedEffect(Unit) {
        settingsManager.channelSwitchMode.collect { switchMode ->
            channelSwitchMode = switchMode
        }
    }
    
    // 读取全屏方向模式设置
    LaunchedEffect(Unit) {
        settingsManager.fullscreenOrientationMode.collect { orientationMode ->
            fullscreenOrientationMode = orientationMode
        }
    }

    // 使用全屏管理器处理全屏相关逻辑
    val fullscreenManager = rememberFullscreenManager(uiState, fullscreenOrientationMode)

    // 添加保持屏幕常亮的副作用
    DisposableEffect(Unit) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 音量控制状��?
    var volumePercentage by remember { mutableStateOf<Int?>(null) }
    val volumeIndicatorAlpha = remember { Animatable(0f) }
    // 亮度控制状��?
    var brightnessPercentage by remember { mutableStateOf<Int?>(null) }
    val brightnessIndicatorAlpha = remember { Animatable(0f) }

    // --- 修改: 使用LaunchedEffect监听channelId变化 ---
    LaunchedEffect(channelId) {
        // 添加调试日志
        android.util.Log.d("PlayerScreen", "Loading channel with ID: $channelId")
        viewModel.switchToChannel(channelId)
        // 频道加载完成后，重置频道切换状��?
        // isChannelSwitching = false
    }

    // 音量指示器自动隐��?
    LaunchedEffect(volumePercentage) {
        if (volumePercentage != null) {
            volumeIndicatorAlpha.animateTo(1f, tween(100))
            volumeIndicatorAlpha.animateTo(0f, tween(1000))
        }
    }
    // 亮度指示器自动隐��?
    LaunchedEffect(brightnessPercentage) {
        if (brightnessPercentage != null) {
            brightnessIndicatorAlpha.animateTo(1f, tween(100))
            brightnessIndicatorAlpha.animateTo(0f, tween(1000))
        }
    }

    // 计算视频高度
    val videoHeight: Dp by derivedStateOf {
        val videoSize = uiState.videoSize
        if (videoSize != androidx.media3.common.VideoSize.UNKNOWN &&
            videoSize.width > 0 && videoSize.height > 0
        ) {
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val aspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
            val calculatedHeightPx = screenWidthPx / aspectRatio
            with(density) { calculatedHeightPx.toDp() }
        } else {
            // 使用16:9的默认宽高比，确保在没有视频尺寸信息时也能正确显��?
            (configuration.screenWidthDp * 9f / 16f).dp
        }
    }

    // 视频尺寸监听
    DisposableEffect(viewModel) {
        val player = viewModel.getExoPlayer()
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                // 在这里，您应该更��?ViewModel 中的 uiState.videoSize
                // 但当前的 PlayerViewModel 没有提供更新此状态的方法��?
                // 您可能需要在 ViewModel 中添加一个方法，例如 updateVideoSize(videoSize)
                // 为了演示，这里暂时注释掉��?
                // viewModel.updateVideoSize(videoSize)
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
        }
    }

    // 判断当前屏幕方向
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 用于取消自动隐藏任务的Job
    val autoHideJob = remember { mutableStateOf<Job?>(null) }

    // 根据屏幕方向和全屏状态选择布局
    if (isLandscape && !uiState.isFullscreen) {
        // 横屏，非全屏播放 - 分栏布局
        LandscapeNonFullscreenLayout(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBack,
            videoHeight = videoHeight,
            isControlBarVisibleInNormalMode = true, // 简化处��?
            scope = scope,
            configuration = configuration,
            audioManager = audioManager,
            window = window,
            view = view,
            context = context,
            channelSwitchMode = channelSwitchMode,
            isChannelSwitching = remember { mutableStateOf(false) },
            volumePercentage = volumePercentage,
            brightnessPercentage = brightnessPercentage,
            onSwitchChannel = onSwitchChannel,
            onVolumePercentageChange = { percentage ->
                volumePercentage = percentage
            },
            onBrightnessPercentageChange = { percentage ->
                brightnessPercentage = percentage
            }
        )
    } else {
        // 使用原有的布局代码
        if (uiState.isFullscreen) {
            android.util.Log.d("PlayerScreen", "Rendering fullscreen layout - uiState.isFullscreen: ${uiState.isFullscreen}")
            // 全屏模式 - 视频播放窗口占满整个屏幕
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .addDoubleTapGesture(
                        channelSwitchMode = channelSwitchMode,
                        scope = scope,
                        viewModel = viewModel,
                        onSwitchChannel = onSwitchChannel,
                        isFullscreen = uiState.isFullscreen,
                        isLandscape = isLandscape,
                        onToggleControlAndEpgVisibility = {
                            android.util.Log.d("PlayerScreen", "Toggling fullscreen control visibility")
                            // 只有当控制栏隐藏时才响应单击并显示控制栏
                            if (!fullscreenManager.isControlBarVisibleInFullscreen) {
                                // 取消之前的自动隐藏任务
                                autoHideJob.value?.cancel()
                                
                                // 显示控制栏和EPG信息
                                fullscreenManager.toggleControlAndEpgVisibility()
                                
                                // 启动新的自动隐藏任务
                                autoHideJob.value = scope.launch {
                                    kotlinx.coroutines.delay(5000) // 5秒后自动隐藏
                                    // 只有当控制栏仍然可见时才隐藏它
                                    if (fullscreenManager.isControlBarVisibleInFullscreen || fullscreenManager.isEpgInfoVisibleInFullscreen) {
                                        fullscreenManager.toggleControlAndEpgVisibility()
                                    }
                                }
                            }
                        }
                    )
                    .addHorizontalDragGesture(
                        channelSwitchMode = channelSwitchMode,
                        scope = scope,
                        viewModel = viewModel,
                        onSwitchChannel = onSwitchChannel,
                        isChannelSwitching = remember { mutableStateOf(false) }
                    )
                    .addVerticalDragGesture(
                        audioManager = audioManager,
                        view = view,
                        window = window,
                        scope = scope,
                        onVolumePercentageChange = { percentage ->
                            volumePercentage = percentage
                        },
                        onBrightnessPercentageChange = { percentage ->
                            brightnessPercentage = percentage
                        }
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
                
                // 进入全屏时启动自动隐藏任务
                LaunchedEffect(uiState.isFullscreen, fullscreenManager.isControlBarVisibleInFullscreen) {
                    if (uiState.isFullscreen && fullscreenManager.isControlBarVisibleInFullscreen) {
                        // 取消之前的自动隐藏任务
                        autoHideJob.value?.cancel()
                        
                        // 启动新的自动隐藏任务
                        autoHideJob.value = scope.launch {
                            kotlinx.coroutines.delay(5000) // 5秒后自动隐藏
                            // 只有当控制栏仍然可见时才隐藏它
                            if (fullscreenManager.isControlBarVisibleInFullscreen || fullscreenManager.isEpgInfoVisibleInFullscreen) {
                                fullscreenManager.toggleControlAndEpgVisibility()
                            }
                        }
                    }
                }
                
                // 底部控制区域（包含EPG信息和控制栏）
                android.util.Log.d("PlayerScreen", "Checking fullscreen visibility - isEpgInfoVisibleInFullscreen: ${fullscreenManager.isEpgInfoVisibleInFullscreen}, isControlBarVisibleInFullscreen: ${fullscreenManager.isControlBarVisibleInFullscreen}")
                if (fullscreenManager.isEpgInfoVisibleInFullscreen || fullscreenManager.isControlBarVisibleInFullscreen) {
                    android.util.Log.d("PlayerScreen", "Showing fullscreen controls")
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        // EPG信息和进度条
                        if (fullscreenManager.isEpgInfoVisibleInFullscreen) {
                            uiState.currentEpg?.let { epg ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        //.background(Color.Black.copy(alpha = 0.9f))
                                        //.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))


                                ) {
                                    EpgInfoCard(epg, isFullscreen = true)
                                    EpgProgressIndicator(epg, isFullscreen = uiState.isFullscreen)
                                }
                            }
                        }
                        
                        // 控制栏
                        if (fullscreenManager.isControlBarVisibleInFullscreen) {
                            android.util.Log.d("PlayerScreen", "Rendering control bar")
                            ControlButtonBar(
                                player = viewModel.getExoPlayer(),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                    //.background(
                                    //    color = Color.Black.copy(alpha = 0.3f)
                                    //),
                                onToggleFullscreen = {
                                    viewModel.toggleFullscreen()
                                },
                                isFullscreen = uiState.isFullscreen
                            )
                        }
                    }
                } else {
                    android.util.Log.d("PlayerScreen", "Not showing fullscreen controls")
                }
                
                // 音量指示��?
                volumePercentage?.let { percentage ->
                    VolumeIndicator(
                        percentage = percentage,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
                
                // 亮度指示��?
                brightnessPercentage?.let { percentage ->
                    BrightnessIndicator(
                        percentage = percentage,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
                
                // 加载指示��?
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
        } else {
            // 非全屏模式（包括横屏和竖屏）
            Scaffold(
                // --- 修改: 有条件地显示 TopAppBar ---
                topBar = {
                    if (!uiState.isFullscreen) { // 只有在非全屏模式下才显示顶部��?
                        TopAppBar(
                            title = { Text(uiState.currentChannel?.name ?: "正在加载...") },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                                }
                            }
                        )
                    }
                    // 如果是全屏，��?topBar 为空，不显示
                },
                // --- 修改: 动态调整内容的 padding ---
            ) { padding ->
                // 在全屏模式下，忽略系统提供的 padding，让内容铺满整个屏幕
                // 在非全屏模式下，应用系统 padding，避免内容被状态栏/导航栏遮��?
                val contentPadding = if (uiState.isFullscreen) {
                    PaddingValues(0.dp) // 全屏：无 padding
                } else {
                    padding // 非全屏：使用系统 padding
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding) // 应用动态的 padding
                ) {
                    // 视频播放窗口
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(videoHeight)
                            .background(MaterialTheme.colorScheme.background)
                            .addDoubleTapGesture(
                                channelSwitchMode = channelSwitchMode,
                                scope = scope,
                                viewModel = viewModel,
                                onSwitchChannel = onSwitchChannel,
                                isFullscreen = uiState.isFullscreen,
                                isLandscape = isLandscape,
                                onToggleControlAndEpgVisibility = {
                                    // 非全屏模式下点击不隐藏控制栏
                                    if (uiState.isFullscreen) {
                                        fullscreenManager.toggleControlAndEpgVisibility()
                                    }
                                }
                            )
                            .addHorizontalDragGesture(
                                channelSwitchMode = channelSwitchMode,
                                scope = scope,
                                viewModel = viewModel,
                                onSwitchChannel = onSwitchChannel,
                                isChannelSwitching = remember { mutableStateOf(false) }
                            )
                            .addVerticalDragGesture(
                                audioManager = audioManager,
                                view = view,
                                window = window,
                                scope = scope,
                                onVolumePercentageChange = { percentage ->
                                    volumePercentage = percentage
                                },
                                onBrightnessPercentageChange = { percentage ->
                                    brightnessPercentage = percentage
                                }
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
                        
                        // 音量指示��?
                        volumePercentage?.let { percentage ->
                            VolumeIndicator(
                                percentage = percentage,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        }
                        
                        // 亮度指示��?
                        brightnessPercentage?.let { percentage ->
                            BrightnessIndicator(
                                percentage = percentage,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        }
                        
                        // 加载指示��?
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
                        EpgProgressIndicator(epg, isFullscreen = uiState.isFullscreen)
                    }
                    
                    // 控制��?
                    ControlButtonBar(
                        player = viewModel.getExoPlayer(),
                        modifier = Modifier
                            .fillMaxWidth(),
                            //.background(
                            //    color = MaterialTheme.colorScheme.surfaceVariant
                            //),
                        onToggleFullscreen = {
                            viewModel.toggleFullscreen()
                        },
                        isFullscreen = uiState.isFullscreen
                    )
                    
                    // 即将播放的节目列表
                    if (uiState.upcomingEpg.isNotEmpty()) {
                        Text(
                            text = "节目列表",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                        UpcomingEpgList(uiState.upcomingEpg)
                    }
                }
            }
        }
    }
}