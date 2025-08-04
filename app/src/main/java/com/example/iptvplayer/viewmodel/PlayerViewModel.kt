package com.example.iptvplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import com.example.iptvplayer.data.local.AppDatabase
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val currentEpg: EpgProgram? = null,
    val upcomingEpg: List<EpgProgram> = emptyList(),
    val videoSize: androidx.media3.common.VideoSize = androidx.media3.common.VideoSize.UNKNOWN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFullscreen: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var exoPlayer: ExoPlayer? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        val context = getApplication<Application>().applicationContext
        val renderersFactory = NextRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        exoPlayer = ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
        }
    }

    /**
     * 切换到指定ID的频道。
     * 此方法会更新播放器的媒体源和UI状态。
     */
    fun switchToChannel(channelId: Int) {
        // 保存当前的全屏状态
        val isFullscreen = _uiState.value.isFullscreen
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val db = AppDatabase.getDatabase(context)
                val channelDao = db.channelDao()
                val epgProgramDao = db.epgProgramDao()

                // 获取频道信息
                val channel = channelDao.getChannelById(channelId)
                if (channel != null) {
                    // 获取当前EPG信息
                    val channelName = channel.tvgName ?: channel.name
                    val now = System.currentTimeMillis()
                    val currentEpg = epgProgramDao.getCurrentProgramForChannel(channelName, now)
                    // 获取即将播放的节目列表
                    val upcomingEpgList = epgProgramDao.getProgramsForChannel(channelName)
                        .filter { it.start >= now || (it.start <= now && it.end > now) }
                        .sortedBy { it.start }

                    // 更新播放器媒体源
                    updatePlayerMedia(channel.url)

                    // 更新UI状态，保持全屏状态不变
                    _uiState.value = _uiState.value.copy(
                        currentChannel = channel,
                        currentEpg = currentEpg,
                        upcomingEpg = upcomingEpgList,
                        isLoading = false,
                        isFullscreen = isFullscreen // 保持全屏状态
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "频道未找到",
                        isFullscreen = isFullscreen // 保持全屏状态
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载频道失败: ${e.message}",
                    isFullscreen = isFullscreen // 保持全屏状态
                )
            }
        }
    }

    private fun updatePlayerMedia(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem, /* resetPosition= */ true) // 重置位置，从头开始播放
            player.prepare()
        }
    }

    /**
     * 切换到相邻频道。
     * @param moveToPrevious true为切换到上一个频道，false为下一个
     * @param onChannelSwitched 成功切换频道后调用的回调，用于通知外部导航器
     */
    fun switchToAdjacentChannel(moveToPrevious: Boolean, onChannelSwitched: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val currentChannel = _uiState.value.currentChannel
                if (currentChannel != null) {
                    android.util.Log.d("ChannelSwitch", "Current channel: ${currentChannel.name}, id: ${currentChannel.id}, sequence: ${currentChannel.sequence}")
                    
                    val context = getApplication<Application>().applicationContext
                    val db = AppDatabase.getDatabase(context)
                    val channelDao = db.channelDao()
                    
                    // 获取播放列表中的频道总数
                    val channelCount = channelDao.getChannelCount(currentChannel.playlistId)
                    android.util.Log.d("ChannelSwitch", "Total channels in playlist: $channelCount")
                    
                    // 计算目标sequence值（考虑循环播放）
                    val targetSequence = if (moveToPrevious) {
                        if (currentChannel.sequence == 0) channelCount - 1 else currentChannel.sequence - 1
                    } else {
                        if (currentChannel.sequence == channelCount - 1) 0 else currentChannel.sequence + 1
                    }
                    
                    android.util.Log.d("ChannelSwitch", "Current sequence: ${currentChannel.sequence}, Target sequence: $targetSequence")
                    
                    // 根据目标sequence获取频道
                    val newChannel = channelDao.getChannelBySequence(currentChannel.playlistId, targetSequence)
                    
                    if (newChannel != null) {
                        android.util.Log.d("ChannelSwitch", "Moving to ${if (moveToPrevious) "previous" else "next"} channel. Found channel: ${newChannel.name}, id: ${newChannel.id}, sequence: ${newChannel.sequence}")
                        
                        // 先在ViewModel内部切换频道
                        switchToChannel(newChannel.id)
                        // 然后通知外部导航器更新
                        onChannelSwitched(newChannel.id)
                    } else {
                        android.util.Log.d("ChannelSwitch", "No channel found with sequence: $targetSequence")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChannelSwitch", "Error switching channel", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "切换频道失败"
                )
            }
        }
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    // --- 新增: 切换全屏模式的方法 ---
    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }
}
