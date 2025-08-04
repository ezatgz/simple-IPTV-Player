package com.example.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.local.AppDatabase
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.repository.IptvRepository  // 使用新的IptvRepository实现
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// 定义ImportState数据类
sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
}

// 定义EpgAssignState数据类
sealed class EpgAssignState {
    object Idle : EpgAssignState()
    object Loading : EpgAssignState()
    object Success : EpgAssignState()
    data class Error(val message: String) : EpgAssignState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: IptvRepository
    val playlists: StateFlow<List<Playlist>>

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    private val _epgAssignState = MutableStateFlow<EpgAssignState>(EpgAssignState.Idle)
    val epgAssignState: StateFlow<EpgAssignState> = _epgAssignState

    init {
        val db = AppDatabase.getDatabase(application)
        repository = IptvRepository(db.playlistDao(), db.channelDao(), db.epgProgramDao())
        playlists = repository.playlists.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun importPlaylist(url: String, name: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            val result = repository.importM3uFromUrl(url, name)
            _importState.value = if (result.isSuccess) {
                ImportState.Success
            } else {
                ImportState.Error(result.exceptionOrNull()?.message ?: "未知错误")
            }
        }
    }

    // 添加处理本地M3U文件导入的方法
    fun importPlaylistFromFile(content: String, name: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            val result = repository.importM3uFromFileContent(content, name)
            _importState.value = if (result.isSuccess) {
                ImportState.Success
            } else {
                ImportState.Error(result.exceptionOrNull()?.message ?: "未知错误")
            }
        }
    }

    private fun startBackgroundEpgProcessingForPlaylist(playlist: Playlist) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Check if playlist has EPG URL
                if (playlist.epgUrl.isNullOrBlank()) {
                    android.util.Log.d("EPG_DEBUG", "No EPG URL for playlist ${playlist.name}")
                    return@launch
                }
                
                // Process EPG for this playlist without channel comparison
                repository.parseEpgAndStore(playlist.epgUrl!!, playlist.id)
            } catch (e: Exception) {
                android.util.Log.e("EPG_DEBUG", "Error processing EPG for playlist ${playlist.name}: ${e.message}", e)
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun assignEpgToPlaylist(playlistId: Int, epgUrl: String?) {
        viewModelScope.launch {
            _epgAssignState.value = EpgAssignState.Loading
            try {
                // 如果epgUrl不为空，先验证URL是否有效
                if (!epgUrl.isNullOrBlank()) {
                    // 尝试解析URL以验证其有效性
                    val isValid = repository.validateEpgUrl(epgUrl)
                    if (!isValid) {
                        _epgAssignState.value = EpgAssignState.Error("无法访问或解析指定的EPG URL")
                        return@launch
                    }
                }
                
                repository.updatePlaylistEpgUrl(playlistId, epgUrl)
                _epgAssignState.value = EpgAssignState.Success
                
                // Start background EPG processing if EPG URL is not null
                if (!epgUrl.isNullOrBlank()) {
                    val playlist = playlists.value.find { it.id == playlistId }
                    if (playlist != null) {
                        // Create a new playlist object with the updated EPG URL
                        val updatedPlaylist = playlist.copy(epgUrl = epgUrl)
                        startBackgroundEpgProcessingForPlaylist(updatedPlaylist)
                    }
                }
            } catch (e: Exception) {
                _epgAssignState.value = EpgAssignState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun resetEpgAssignState() {
        _epgAssignState.value = EpgAssignState.Idle
    }

    fun updateEpgForPlaylist(playlistId: Int) {
        viewModelScope.launch {
            _epgAssignState.value = EpgAssignState.Loading
            try {
                val playlist = playlists.value.find { it.id == playlistId }
                if (playlist != null) {
                    if (playlist.epgUrl.isNullOrBlank()) {
                        _epgAssignState.value = EpgAssignState.Error("该播放列表未分配EPG")
                    } else {
                        startBackgroundEpgProcessingForPlaylist(playlist)
                        _epgAssignState.value = EpgAssignState.Success
                    }
                } else {
                    _epgAssignState.value = EpgAssignState.Error("未找到播放列表")
                }
            } catch (e: Exception) {
                _epgAssignState.value = EpgAssignState.Error(e.message ?: "未知错误")
            }
        }
    }


    private fun startBackgroundEpgProcessingForAllPlaylists() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                playlists.value.filter { !it.epgUrl.isNullOrBlank() }.forEach { playlist ->
                    try {
                        // Process EPG for this playlist without channel comparison
                        repository.parseEpgAndStore(playlist.epgUrl!!, playlist.id)
                    } catch (e: Exception) {
                        android.util.Log.e("EPG_DEBUG", "Error processing EPG for playlist ${playlist.name}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EPG_DEBUG", "Error in background EPG processing for all playlists: ${e.message}", e)
            }
        }
    }
}