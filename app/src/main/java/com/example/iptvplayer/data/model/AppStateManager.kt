package com.example.iptvplayer.data.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppStateManager(private val settingsManager: SettingsManager) {
    var channelListDisplayMode: DisplayMode = DisplayMode.LIST
        private set

    init {
        // 初始化时从设置中读取显示模式
        runBlocking {
            channelListDisplayMode = settingsManager.channelListDisplayMode.first()
        }
    }

    fun setChannelListDisplayMode(mode: DisplayMode) {
        channelListDisplayMode = mode
        // 同步更新到设置中
        runBlocking {
            settingsManager.updateChannelListDisplayMode(mode)
        }
    }
}