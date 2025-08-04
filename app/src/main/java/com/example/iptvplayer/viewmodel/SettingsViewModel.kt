package com.example.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.model.SettingsManager
import com.example.iptvplayer.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    
    val themeMode: StateFlow<ThemeMode> = settingsManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )
    
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.updateThemeMode(themeMode)
        }
    }
}