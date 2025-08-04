package com.example.iptvplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.SettingsManager
import com.example.iptvplayer.data.model.ThemeMode
import com.example.iptvplayer.data.model.ChannelSwitchMode
import com.example.iptvplayer.data.model.FullscreenOrientationMode
import kotlinx.coroutines.launch

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

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ThemeSettingItem(
    currentTheme: ThemeMode?,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "主题模式",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ThemeOptionItem(
                    icon = Icons.Default.LightMode,
                    title = "浅色主题",
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                
                HorizontalDivider()
                
                ThemeOptionItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色主题",
                    isSelected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
                
                HorizontalDivider()
                
                ThemeOptionItem(
                    icon = Icons.Default.BrightnessAuto,
                    title = "跟随系统",
                    isSelected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
            }
        }
    }
}

@Composable
fun ChannelSwitchModeSettingItem(
    currentMode: ChannelSwitchMode?,
    onModeSelected: (ChannelSwitchMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "控制方式",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ChannelSwitchModeOptionItem(
                    icon = Icons.Default.TouchApp,
                    title = "双击屏幕",
                    description = "双击屏幕左侧或右侧切换频道",
                    isSelected = currentMode == ChannelSwitchMode.DOUBLE_TAP,
                    onClick = { onModeSelected(ChannelSwitchMode.DOUBLE_TAP) }
                )
                
                HorizontalDivider()
                
                ChannelSwitchModeOptionItem(
                    icon = Icons.Default.Swipe,
                    title = "滑动手势",
                    description = "水平滑动切换频道",
                    isSelected = currentMode == ChannelSwitchMode.SWIPE,
                    onClick = { onModeSelected(ChannelSwitchMode.SWIPE) }
                )
            }
        }
    }
}

@Composable
fun ThemeOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ChannelSwitchModeOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FullscreenOrientationModeSettingItem(
    currentMode: FullscreenOrientationMode?,
    onModeSelected: (FullscreenOrientationMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "屏幕方向",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                FullscreenOrientationModeOptionItem(
                    icon = Icons.Default.Landscape,
                    title = "强制横屏",
                    description = "全屏播放时始终以横屏显示",
                    isSelected = currentMode == FullscreenOrientationMode.LANDSCAPE,
                    onClick = { onModeSelected(FullscreenOrientationMode.LANDSCAPE) }
                )
                
                HorizontalDivider()
                
                FullscreenOrientationModeOptionItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "跟随系统",
                    description = "全屏播放时跟随系统方向设置",
                    isSelected = currentMode == FullscreenOrientationMode.FOLLOW_SYSTEM,
                    onClick = { onModeSelected(FullscreenOrientationMode.FOLLOW_SYSTEM) }
                )
            }
        }
    }
}

@Composable
fun FullscreenOrientationModeOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}