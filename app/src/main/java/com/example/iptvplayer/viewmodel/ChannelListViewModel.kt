package com.example.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.local.AppDatabase
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.data.model.DisplayMode
import com.example.iptvplayer.repository.IptvRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelListViewModel(application: Application, private val playlistId: Int) : AndroidViewModel(application) {
    private val repository: IptvRepository
    val channels: StateFlow<List<Channel>>
    val epgMap = MutableStateFlow<Map<String, EpgProgram?>>(emptyMap())
    var epgUrl: String? = null
        private set

    // 新增状态用于长按显示EPG功能
    val selectedChannel = MutableStateFlow<Channel?>(null)
    val upcomingEpgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
    val isEpgBottomSheetVisible = MutableStateFlow(false)
    val isEpgLoading = MutableStateFlow(false)
    
    // 添加displayMode状态
    val displayMode = MutableStateFlow(DisplayMode.LIST)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = IptvRepository(db.playlistDao(), db.channelDao(), db.epgProgramDao())
        channels = repository.getChannels(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        viewModelScope.launch {
            android.util.Log.d("EPG_DEBUG", "ViewModel init - looking for playlistId: $playlistId")
            db.playlistDao().getAllPlaylists().collect { playlists ->
                val playlist = playlists.find { it.id == playlistId }
                epgUrl = playlist?.epgUrl
                android.util.Log.d("EPG_DEBUG", "Found playlist: ${playlist?.name}, epgUrl: $epgUrl")
                if (epgUrl != null) {
                    // Remove automatic EPG processing when entering playlist
                    // startBackgroundEpgProcessing()
                } else {
                    android.util.Log.d("EPG_DEBUG", "No EPG URL found for playlist")
                }
            }
        }
        
        // Load EPG data after channels are loaded
        viewModelScope.launch {
            channels.collect { channelList ->
                if (channelList.isNotEmpty() && epgUrl != null) {
                    loadEpgFromDatabase()
                }
            }
        }
    }

    fun loadEpgFromDatabase() {
        android.util.Log.d("EPG_DEBUG", "loadEpgFromDatabase called")
        viewModelScope.launch {
            val channelList = channels.value
            val map = mutableMapOf<String, EpgProgram?>()
            
            // Load current EPG programs for all channels from database
            // Match channel's tvgName with EPG's channelName
            for (channel in channelList) {
                try {
                    // Use channel's tvgName to find EPG data
                    if (!channel.tvgName.isNullOrBlank()) {
                        val epgProgram = repository.getCurrentEpgForChannelByTvgName(channel.tvgName!!)
                        map[channel.name] = epgProgram
                        if (epgProgram != null) {
                            android.util.Log.d("EPG_DEBUG", "Loaded EPG for ${channel.name} (tvgName: ${channel.tvgName}): ${epgProgram.title}")
                        }
                    } else {
                        // Fallback to channel name if tvgName is not available
                        val epgProgram = repository.getCurrentEpgForChannelFromDb(channel.name)
                        map[channel.name] = epgProgram
                        if (epgProgram != null) {
                            android.util.Log.d("EPG_DEBUG", "Loaded EPG for ${channel.name}: ${epgProgram.title}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EPG_DEBUG", "Error loading EPG for channel ${channel.name}: ${e.message}", e)
                }
            }
            
            epgMap.value = map.toMap()
            android.util.Log.d("EPG_DEBUG", "EPG map loaded from database with ${map.size} entries")
        }
    }

    fun startBackgroundEpgProcessing() {
        val url = epgUrl ?: return
        android.util.Log.d("EPG_DEBUG", "startBackgroundEpgProcessing called with url: $url")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("EPG_DEBUG", "Starting background EPG processing")
                // Pass the playlistId instead of channelList
                repository.parseEpgAndStore(url, playlistId)
                android.util.Log.d("EPG_DEBUG", "Background EPG processing completed")
                
                // Refresh the UI with updated EPG data
                loadEpgFromDatabase()
            } catch (e: Exception) {
                android.util.Log.e("EPG_DEBUG", "Error in background EPG processing: ${e.message}", e)
            }
        }
    }

    fun refreshEpg() {
        android.util.Log.d("EPG_DEBUG", "refreshEpg called - fetching fresh EPG data from URL")
        startBackgroundEpgProcessing()
    }

    // 新增方法：处理频道长按事件
    fun onChannelLongClick(channel: Channel) {
        viewModelScope.launch {
            selectedChannel.value = channel
            isEpgLoading.value = true
            
            try {
                // 获取频道的未来节目列表
                val programs = if (!channel.tvgName.isNullOrBlank()) {
                    repository.getUpcomingEpgForChannel(channel.tvgName!!)
                } else {
                    repository.getUpcomingEpgForChannel(channel.name)
                }
                
                upcomingEpgPrograms.value = programs
                isEpgLoading.value = false
                isEpgBottomSheetVisible.value = true
            } catch (e: Exception) {
                android.util.Log.e("EPG_DEBUG", "Error loading upcoming EPG for channel ${channel.name}: ${e.message}", e)
                isEpgLoading.value = false
            }
        }
    }

    // 新增方法：隐藏EPG BottomSheet
    fun hideEpgBottomSheet() {
        isEpgBottomSheetVisible.value = false
        selectedChannel.value = null
        upcomingEpgPrograms.value = emptyList()
    }
}