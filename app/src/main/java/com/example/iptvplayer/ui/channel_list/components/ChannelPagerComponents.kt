@file:OptIn(ExperimentalFoundationApi::class)

package com.example.iptvplayer.ui.channel_list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram

@Composable
fun ChannelPagerView(
    groupedChannels: Map<String, List<Channel>>,
    epgMap: Map<String, EpgProgram?>,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupList = groupedChannels.keys.toList()
    val pagerState = rememberPagerState(pageCount = { groupList.size })

    Column(modifier = modifier.fillMaxSize()) {
        // 显示当前分组的标题，样式与列表模式保持一致
        if (groupList.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = groupList[pagerState.currentPage],
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // 使用HorizontalPager显示每个分组的频道
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val channelsInGroup = groupedChannels[groupList[page]] ?: emptyList()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(channelsInGroup) { channel ->
                    val epg = epgMap[channel.name]
                    ChannelItem(
                        channel = channel,
                        epg = epg,
                        onClick = { onChannelClick(channel) },
                        onLongClick = { onChannelLongClick(channel) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}