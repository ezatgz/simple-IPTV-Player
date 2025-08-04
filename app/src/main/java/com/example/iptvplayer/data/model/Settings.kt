package com.example.iptvplayer.data.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 应用主题模式枚举
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

// 频道切换控制模式枚举
enum class ChannelSwitchMode {
    DOUBLE_TAP,
    SWIPE
}

// 全屏方向模式枚举
enum class FullscreenOrientationMode {
    FOLLOW_SYSTEM,
    LANDSCAPE
}

// Context扩展来获取DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val CHANNEL_LIST_DISPLAY_MODE_KEY = stringPreferencesKey("channel_list_display_mode")
    private val CHANNEL_SWITCH_MODE_KEY = stringPreferencesKey("channel_switch_mode")
    private val FULLSCREEN_ORIENTATION_MODE_KEY = stringPreferencesKey("fullscreen_orientation_mode")

    // 获取主题模式的Flow
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[THEME_MODE_KEY]) {
                "dark" -> ThemeMode.DARK
                "light" -> ThemeMode.LIGHT
                else -> ThemeMode.SYSTEM // 默认使用系统设置
            }
        }

    // 获取频道列表显示模式的Flow
    val channelListDisplayMode: Flow<DisplayMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[CHANNEL_LIST_DISPLAY_MODE_KEY]) {
                "pager" -> DisplayMode.PAGER
                else -> DisplayMode.LIST // 默认使用列表模式
            }
        }

    // 获取频道切换控制模式的Flow
    val channelSwitchMode: Flow<ChannelSwitchMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[CHANNEL_SWITCH_MODE_KEY]) {
                "swipe" -> ChannelSwitchMode.SWIPE
                else -> ChannelSwitchMode.DOUBLE_TAP // 默认使用双击模式
            }
        }

    // 获取全屏方向模式的Flow
    val fullscreenOrientationMode: Flow<FullscreenOrientationMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[FULLSCREEN_ORIENTATION_MODE_KEY]) {
                "follow_system" -> FullscreenOrientationMode.FOLLOW_SYSTEM
                else -> FullscreenOrientationMode.LANDSCAPE // 默认使用强制横屏
            }
        }

    // 更新主题模式
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = when (themeMode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    // 更新频道列表显示模式
    suspend fun updateChannelListDisplayMode(displayMode: DisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[CHANNEL_LIST_DISPLAY_MODE_KEY] = when (displayMode) {
                DisplayMode.LIST -> "list"
                DisplayMode.PAGER -> "pager"
            }
        }
    }

    // 更新频道切换控制模式
    suspend fun updateChannelSwitchMode(mode: ChannelSwitchMode) {
        context.dataStore.edit { preferences ->
            preferences[CHANNEL_SWITCH_MODE_KEY] = when (mode) {
                ChannelSwitchMode.DOUBLE_TAP -> "double_tap"
                ChannelSwitchMode.SWIPE -> "swipe"
            }
        }
    }

    // 更新全屏方向模式
    suspend fun updateFullscreenOrientationMode(mode: FullscreenOrientationMode) {
        context.dataStore.edit { preferences ->
            preferences[FULLSCREEN_ORIENTATION_MODE_KEY] = when (mode) {
                FullscreenOrientationMode.FOLLOW_SYSTEM -> "follow_system"
                FullscreenOrientationMode.LANDSCAPE -> "landscape"
            }
        }
    }
}

// 频道列表显示模式枚举
enum class DisplayMode {
    LIST,
    PAGER
}