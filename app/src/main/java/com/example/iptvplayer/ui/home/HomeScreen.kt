package com.example.iptvplayer.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.viewmodel.ImportState
import com.example.iptvplayer.viewmodel.MainViewModel
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
// 导入新的组件
import com.example.iptvplayer.ui.home.components.PlaylistItem
import com.example.iptvplayer.ui.home.components.AddPlaylistDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Int, String) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val playlists by viewModel.playlists.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var epgDialogPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的播放列表") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加播放列表")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无播放列表，请点击右下角添加")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id, playlist.name) },
                        onDelete = { viewModel.deletePlaylist(playlist) },
                        onAssignEpg = { epgDialogPlaylist = playlist }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showDialog) {
            AddPlaylistDialog(
                viewModel = viewModel,
                onDismiss = { showDialog = false },
                onShowResult = { success, message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = if (success) SnackbarDuration.Short else SnackbarDuration.Long
                        )
                    }
                }
            )
        }
        if (epgDialogPlaylist != null) {
            AssignEpgDialog(
                playlist = epgDialogPlaylist!!,
                viewModel = viewModel,
                onDismiss = { epgDialogPlaylist = null },
                onShowResult = { success, message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = if (success) SnackbarDuration.Short else SnackbarDuration.Long
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun AssignEpgDialog(playlist: Playlist, viewModel: MainViewModel, onDismiss: () -> Unit, onShowResult: (Boolean, String) -> Unit) {
    var epgUrl by remember { mutableStateOf(playlist.epgUrl ?: "") }
    val context = LocalContext.current
    val epgAssignState by viewModel.epgAssignState.collectAsState()

    LaunchedEffect(epgAssignState) {
        when (epgAssignState) {
            is com.example.iptvplayer.viewmodel.EpgAssignState.Success -> {
                onShowResult(true, "EPG分配成功")
                viewModel.resetEpgAssignState()
                onDismiss()
            }
            is com.example.iptvplayer.viewmodel.EpgAssignState.Error -> {
                onShowResult(false, "EPG分配失败: ${(epgAssignState as com.example.iptvplayer.viewmodel.EpgAssignState.Error).message}")
                viewModel.resetEpgAssignState()
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("为 \"${playlist.name}\" 分配EPG") },
        text = {
            Column {
                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text("EPG URL (XMLTV格式)") },
                    singleLine = true
                )
                if (epgAssignState is com.example.iptvplayer.viewmodel.EpgAssignState.Loading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 添加URL有效性检查，包括检查URL是否能解析
                    if (epgUrl.isNotBlank()) {
                        if (!epgUrl.startsWith("http")) {
                            onShowResult(false, "请输入有效的EPG URL")
                            return@Button
                        }
                    }
                    viewModel.assignEpgToPlaylist(playlist.id, epgUrl.ifBlank { null })
                },
                enabled = epgAssignState !is com.example.iptvplayer.viewmodel.EpgAssignState.Loading
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (!playlist.epgUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.assignEpgToPlaylist(playlist.id, null)
                        },
                        enabled = epgAssignState !is com.example.iptvplayer.viewmodel.EpgAssignState.Loading
                    ) {
                        Text("移除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

// 结果对话框状态数据类
data class ResultDialogState(
    val success: Boolean,
    val message: String
)

// 结果对话框组件
@Composable
fun ResultDialog(
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(if (success) "成功" else "失败")
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}