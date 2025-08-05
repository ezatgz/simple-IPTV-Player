package com.example.iptvplayer.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.FullscreenOrientationMode

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