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
import com.example.iptvplayer.ui.home.components.AssignEpgDialog

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