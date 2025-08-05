@file:OptIn(ExperimentalFoundationApi::class)
package com.example.iptvplayer.ui.channel_list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.viewmodel.ChannelListViewModel
import com.example.iptvplayer.data.model.DisplayMode
import com.example.iptvplayer.data.model.AppStateManager
import kotlinx.coroutines.launch
// 导入拆分后的组件
import com.example.iptvplayer.ui.channel_list.components.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelListScreen(
    playlistName: String,
    viewModel: ChannelListViewModel,
    onChannelClick: (Channel) -> Unit,
    onBack: () -> Unit,
    appStateManager: AppStateManager
) {
    val channels by viewModel.channels.collectAsState()
    val epgMap by viewModel.epgMap.collectAsState()
    val groupedChannels = channels.groupBy { it.group }
    var isRefreshing by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(appStateManager.channelListDisplayMode) } // 从AppState获取显示模式状态
    
    // 新增状态用于EPG BottomSheet
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val upcomingEpgPrograms by viewModel.upcomingEpgPrograms.collectAsState()
    val isEpgBottomSheetVisible by viewModel.isEpgBottomSheetVisible.collectAsState()
    val isEpgLoading by viewModel.isEpgLoading.collectAsState()

    // Remove the LaunchedEffect that automatically refreshes EPG when entering the playlist

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 添加切换显示模式的按钮
                    IconButton(onClick = {
                        // 切换显示模式并更新AppState中的状态
                        val newMode = if (displayMode == DisplayMode.LIST) DisplayMode.PAGER else DisplayMode.LIST
                        appStateManager.setChannelListDisplayMode(newMode)
                        displayMode = newMode
                    }) {
                        Icon(
                            if (displayMode == DisplayMode.LIST) Icons.Default.ViewStream else Icons.Default.ViewList,
                            contentDescription = if (displayMode == DisplayMode.LIST) "切换到分页视图" else "切换到列表视图"
                        )
                    }
                    IconButton(onClick = {
                        isRefreshing = true
                        viewModel.refreshEpg()
                        // Use a coroutine scope to handle the delay
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(2000) // Adjust delay as needed
                            isRefreshing = false
                        }
                    }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新EPG")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (displayMode) {
                DisplayMode.LIST -> {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        groupedChannels.forEach { (group, channelList) ->
                            stickyHeader {
                                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(group, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                }
                            }
                            items(channelList) { channel ->
                                val epg = epgMap[channel.name]
                                ChannelItem(
                                    channel = channel, 
                                    epg = epg, 
                                    onClick = { onChannelClick(channel) },
                                    onLongClick = { viewModel.onChannelLongClick(channel) } // 添加长按处理
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                DisplayMode.PAGER -> {
                    ChannelPagerView(
                        groupedChannels = groupedChannels,
                        epgMap = epgMap,
                        onChannelClick = onChannelClick,
                        onChannelLongClick = { viewModel.onChannelLongClick(it) },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
        
        // 添加EPG信息的BottomSheet
        if (isEpgBottomSheetVisible) {
            EpgBottomSheet(
                channel = selectedChannel,
                epgPrograms = upcomingEpgPrograms,
                isLoading = isEpgLoading,
                onDismiss = { viewModel.hideEpgBottomSheet() }
            )
        }
    }
}


