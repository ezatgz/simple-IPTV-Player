@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.iptvplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.EpgProgram
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EpgInfoCard(epg: EpgProgram, isFullscreen: Boolean = false) {
    // 根据全屏模式设置背景颜色
    val backgroundColor = if (isFullscreen) {
        Color.Black.copy(alpha = 0.3f) // 全屏模式下使用半透明黑色背景
    } else {
        MaterialTheme.colorScheme.background
    }
    
    // 根据全屏模式设置文字颜色
    val textColor = if (isFullscreen) {
        Color.White.copy(alpha = 0.8f) // 全屏模式下使用白色文字
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = epg.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

@Composable
fun UpcomingEpgList(epgList: List<EpgProgram>) {
    val currentTime = System.currentTimeMillis()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
        //.padding(2.dp)
    ) {
        items(epgList) { epg ->
            val (textColor, backgroundColor) = when {
                // 正在播放的节目
                epg.start <= currentTime && epg.end > currentTime -> {
                    MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }
                // 已结束的节目
                epg.end <= currentTime -> {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to Color.Transparent
                }
                // 未播放的节目
                else -> {
                    MaterialTheme.colorScheme.onSurface to Color.Transparent
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(horizontal = 8.dp)
            ) {
                // 时间信息
                Column(
                    //modifier = Modifier.weight(1f)
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(
                        text = formatTime(epg.start),
                        style = MaterialTheme.typography.bodyMedium,
                        //color = MaterialTheme.colorScheme.onSurface
                        color = textColor
                    )
                    //Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatTime(epg.end),
                        //text = epg.title,
                        style = MaterialTheme.typography.bodySmall,
                        //color = MaterialTheme.colorScheme.onSurfaceVariant
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
                // 节目信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        //text = formatTime(epg.end),
                        text = epg.title,
                        style = MaterialTheme.typography.bodyMedium,
                        //color = MaterialTheme.colorScheme.onSurface
                        color = textColor
                    )
                    //Spacer(modifier = Modifier.height(2.dp))
                    // 计算并显示节目持续时间
                    val duration = (epg.end - epg.start) / (1000 * 60) // 转换为分钟
                    Text(
                        text = "${duration}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        //color = MaterialTheme.colorScheme.onSurfaceVariant
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EpgProgressIndicator(epg: EpgProgram) {
    val now = System.currentTimeMillis()
    val progress = if (epg.end > epg.start) {
        ((now - epg.start).toFloat() / (epg.end - epg.start).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 显示开始时间
        Text(
            //text = "${formatTime(epg.start)} - ${formatTime(epg.end)}",
            text = "${formatTime(epg.start)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 显示结束时间
        Text(
            text = "${formatTime(epg.end)}",
            //text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.wrapContentWidth()
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}