package com.example.iptvplayer.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.viewmodel.ImportState
import com.example.iptvplayer.viewmodel.MainViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onShowResult: (Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 for URL, 1 for Local file
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                try {
                    val contentResolver = context.contentResolver
                    contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                        fileContent = inputStream.bufferedReader().use { it.readText() }
                        // 获取文件名作为播放列表名称
                        val cursor = contentResolver.query(selectedUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (displayNameIndex != -1) {
                                    val displayName = it.getString(displayNameIndex)
                                    fileName = displayName ?: ""
                                    // 移除文件扩展名
                                    if (displayName.contains(".")) {
                                        name = displayName.substring(0, displayName.lastIndexOf("."))
                                    } else {
                                        name = displayName
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    onShowResult(false, "读取文件失败: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                onShowResult(true, "导入成功")
                viewModel.resetImportState()
                onDismiss()
            }
            is ImportState.Error -> {
                onShowResult(false, "导入失败: ${(importState as ImportState.Error).message}")
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加播放列表") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 选项卡
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        text = { Text("网络导入") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                        text = { Text("本地导入") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 播放列表名称输入框（通用）
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("列表名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 根据选项卡显示不同的内容
                when (selectedTab) {
                    0 -> {
                        // 网络导入
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("M3U URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        // 本地导入
                        Button(
                            onClick = {
                                // 启动文件选择器
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择M3U文件")
                        }

                        if (fileName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已选择文件: $fileName",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (importState is ImportState.Loading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            // 网络导入
                            if (name.isNotBlank() && url.isNotBlank()) {
                                viewModel.importPlaylist(url, name)
                            } else {
                                onShowResult(false, "名称和URL不能为空")
                            }
                        }
                        1 -> {
                            // 本地导入
                            if (name.isNotBlank() && fileContent.isNotBlank()) {
                                viewModel.importPlaylistFromFile(fileContent, name)
                            } else if (name.isBlank()) {
                                onShowResult(false, "名称不能为空")
                            } else if (fileContent.isBlank()) {
                                onShowResult(false, "请选择一个M3U文件")
                            }
                        }
                    }
                },
                enabled = importState !is ImportState.Loading
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}