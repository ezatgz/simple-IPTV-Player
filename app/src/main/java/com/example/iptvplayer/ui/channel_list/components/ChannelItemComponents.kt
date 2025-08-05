package com.example.iptvplayer.ui.channel_list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelItem(
    channel: Channel,
    epg: EpgProgram?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(channel.name) },
        supportingContent = {
            if (epg != null) {
                val now = System.currentTimeMillis()
                val progress = ((now - epg.start).toFloat() / (epg.end - epg.start).toFloat()).coerceIn(0f, 1f)
                Column {
                    Text("正在播放: ${epg.title} (${formatTime(epg.start)} - ${formatTime(epg.end)})", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                }
            } else {
                Text("暂无节目信息", style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        leadingContent = {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = "${channel.name} logo",
                modifier = Modifier.size(40.dp)
            )
        }
    )
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm")
    return sdf.format(Date(millis))
}