package com.example.iptvplayer.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.viewmodel.MainViewModel

@Composable
fun AssignEpgDialog(
    playlist: Playlist,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onShowResult: (Boolean, String) -> Unit
) {
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