package com.example.iptvplayer.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.ChannelSwitchMode

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