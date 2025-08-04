@file:OptIn(ExperimentalFoundationApi::class)
package com.example.iptvplayer.ui.channel_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    playlistName: String,
    viewModel: ChannelListViewModel,
    onChannelClick: (Channel) -> Unit,  // 修改为只传递Channel对象
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
                                    onClick = { onChannelClick(channel) },  // 修改为只传递channel对象
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

// 定义显示模式枚举
enum class DisplayMode {
    LIST, PAGER
}

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

@Composable
fun ChannelItem(
    channel: Channel, 
    epg: EpgProgram?, 
    onClick: () -> Unit,
    onLongClick: () -> Unit // 添加长按回调
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
            onLongClick = onLongClick // 添加长按处理
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpgBottomSheet(
    channel: Channel?,
    epgPrograms: List<EpgProgram>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (channel != null) {
                    Row {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = "${channel.name} logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!channel.tvgName.isNullOrBlank()) {
                                Text(
                                    text = "ID: ${channel.tvgName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内容区域
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (epgPrograms.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无节目信息", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn {
                        items(epgPrograms) { program ->
                            EpgProgramItem(program = program, isCurrent = isCurrentProgram(program))
                            HorizontalDivider()
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EpgProgramItem(program: EpgProgram, isCurrent: Boolean) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(program.start) + " - " + formatTime(program.end),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Text(
                        text = "正在播放",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = program.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            program.desc?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun isCurrentProgram(program: EpgProgram): Boolean {
    val now = System.currentTimeMillis()
    return now in program.start..program.end
}

fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm")
    return sdf.format(java.util.Date(millis))
}