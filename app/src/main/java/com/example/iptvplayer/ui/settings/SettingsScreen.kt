package com.example.iptvplayer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.SettingsManager
import com.example.iptvplayer.data.model.ThemeMode
import com.example.iptvplayer.data.model.ChannelSwitchMode
import com.example.iptvplayer.data.model.FullscreenOrientationMode
import kotlinx.coroutines.launch
// 导入拆分后的组件
import com.example.iptvplayer.ui.settings.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var currentThemeMode by remember { mutableStateOf<ThemeMode?>(null) }
    var currentChannelSwitchMode by remember { mutableStateOf<ChannelSwitchMode?>(null) }
    var currentFullscreenOrientationMode by remember { mutableStateOf<FullscreenOrientationMode?>(null) }
    
    // 读取当前主题设置
    LaunchedEffect(Unit) {
        settingsManager.themeMode.collect { themeMode ->
            currentThemeMode = themeMode
        }
    }
    
    // 读取当前频道切换控制模式设置
    LaunchedEffect(Unit) {
        settingsManager.channelSwitchMode.collect { switchMode ->
            currentChannelSwitchMode = switchMode
        }
    }
    
    // 读取当前全屏方向模式设置
    LaunchedEffect(Unit) {
        settingsManager.fullscreenOrientationMode.collect { orientationMode ->
            currentFullscreenOrientationMode = orientationMode
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 主题设置部分
            SettingSectionTitle(title = "主题")
            ThemeSettingItem(
                currentTheme = currentThemeMode,
                onThemeSelected = { themeMode ->
                    coroutineScope.launch {
                        settingsManager.updateThemeMode(themeMode)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 频道切换控制模式设置部分
            SettingSectionTitle(title = "频道切换控制")
            ChannelSwitchModeSettingItem(
                currentMode = currentChannelSwitchMode,
                onModeSelected = { switchMode ->
                    coroutineScope.launch {
                        settingsManager.updateChannelSwitchMode(switchMode)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 全屏方向模式设置部分
            SettingSectionTitle(title = "全屏播放")
            FullscreenOrientationModeSettingItem(
                currentMode = currentFullscreenOrientationMode,
                onModeSelected = { orientationMode ->
                    coroutineScope.launch {
                        settingsManager.updateFullscreenOrientationMode(orientationMode)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}