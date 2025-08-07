@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.iptvplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun ControlButtonBar(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    onToggleFullscreen: () -> Unit = {},
    isFullscreen: Boolean = false,
    onPreviousChannel: () -> Unit = {},
    onNextChannel: () -> Unit = {}
) {
    var isPlaying by remember {
        mutableStateOf(player?.playWhenReady ?: true)
    }

    // 监听播放器状态变化
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
        }
    }

    // 根据全屏模式设置背景颜色
    val backgroundColor = if (isFullscreen) {
        //Color.Black.copy(alpha = 0.3f) // 全屏模式下使用半透明黑色背景
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // 全屏模式下使用半透明背景

    } else {
        MaterialTheme.colorScheme.background // 非全屏模式下使用主题默认的背景颜色
    }

    // 根据全屏模式设置图标颜色
    val iconColor = if (isFullscreen) {
        Color.White.copy(alpha = 0.8f) // 全屏模式下使用白色图标
    } else {
        MaterialTheme.colorScheme.onSurface // 非全屏模式下使用主题默认的图标颜色
    }

    Row(
        modifier = modifier
            .background(color = backgroundColor)
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 播放/暂停按钮
        IconButton(
            onClick = {
                player?.let { exoPlayer ->
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = iconColor
            )
        }

        // 信息按钮
        IconButton(
            onClick = {
                // 功能暂时留空
            }
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "信息",
                tint = iconColor
            )
        }

        // 收藏按钮
        IconButton(
            onClick = {
                // 功能暂时留空
            }
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "收藏",
                tint = iconColor
            )
        }

        // 全屏按钮
        IconButton(
            onClick = onToggleFullscreen
        ) {
            Icon(
                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                tint = iconColor
            )
        }
    }
}

@Composable
fun VolumeIndicator(percentage: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = Modifier.width(48.dp)
            )
        }
    }
}

@Composable
fun BrightnessIndicator(percentage: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.BrightnessHigh,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = Modifier.width(48.dp)
            )
        }
    }
}