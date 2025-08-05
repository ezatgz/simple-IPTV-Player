package com.example.iptvplayer.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.ThemeMode

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