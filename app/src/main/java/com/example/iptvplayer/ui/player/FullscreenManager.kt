@file:OptIn(UnstableApi::class)

package com.example.iptvplayer.ui.player

import android.util.Log
import android.view.View
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.data.model.FullscreenOrientationMode
import com.example.iptvplayer.viewmodel.PlayerUiState
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 全屏管理器，处理全屏状态切换和屏幕方向控制
 */
@Composable
fun rememberFullscreenManager(
    uiState: PlayerUiState,
    fullscreenOrientationMode: FullscreenOrientationMode?
): FullscreenManager {
    val context = LocalContext.current
    val view = LocalView.current
    val window = remember { (view.context as android.app.Activity).window }
    
    // 保存进入全屏前的方向设置
    var originalOrientation by remember { mutableStateOf<Int?>(null) }
    
    // 系统UI可见性状态
    var isSystemUiVisible by remember { mutableStateOf(true) }
    
    // 控制栏和EPG信息可见性状态
    var isControlBarVisibleInFullscreen by remember { mutableStateOf(true) }
    var isEpgInfoVisibleInFullscreen by remember { mutableStateOf(true) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 使用 LifecycleEventObserver 提前处理返回手势导致的屏幕方向恢复
    DisposableEffect(lifecycleOwner, originalOrientation) {
        val observer = LifecycleEventObserver { _, event ->
            // 当界面暂停或停止时（通常发生在返回操作），并且我们有保存的原始方向时
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && originalOrientation != null) {
                val activity = context as? android.app.Activity
                activity?.let {
                    it.requestedOrientation = originalOrientation!!
                    Log.d("PlayerScreen", "Restoring orientation via Lifecycle event: $originalOrientation")
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 处理系统UI的显示和隐藏以及屏幕方向
    DisposableEffect(uiState.isFullscreen) {
        val activity = context as? android.app.Activity

        if (uiState.isFullscreen) {
            // --- 核心修复点: 开始 ---
            // 只有当 originalOrientation 为 null 时，才说明我们是"首次"进入全屏模式。
            // 这样可以防止屏幕旋转重组后，错误地覆盖保存的状态。
            if (originalOrientation == null) {
                originalOrientation = activity?.requestedOrientation
                Log.d("PlayerScreen", "Entering fullscreen: SAVED original orientation as $originalOrientation")
            }
            // --- 核心修复点: 结束 ---

            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY).toInt()
            isSystemUiVisible = false
            // 修改：进入全屏模式时显示控制栏和EPG信息
            isControlBarVisibleInFullscreen = true
            isEpgInfoVisibleInFullscreen = true

            val targetOrientation = when (fullscreenOrientationMode) {
                FullscreenOrientationMode.FOLLOW_SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                FullscreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity?.requestedOrientation = targetOrientation
            Log.d("PlayerScreen", "Set orientation to landscape mode: $targetOrientation")

            // 启动自动隐藏任务
            // 注意：这个作用域在Composable外部，无法直接访问scope和autoHideJob
            // 需要在PlayerScreen中处理

        } else {
            // 正在退出全屏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE.toInt()
            isSystemUiVisible = true

            // 只有当我们确实保存过原始方向时，才进行恢复
            originalOrientation?.let { savedOrientation ->
                activity?.requestedOrientation = savedOrientation
                Log.d("PlayerScreen", "Exiting fullscreen: RESTORED orientation to $savedOrientation")

                // 恢复后，重置状态，为下一次进入全屏做准备
                originalOrientation = null
            }
        }

        onDispose {
            // 这个 onDispose 块主要用于一个特殊情况：
            // 如果用户在全屏模式下直接退出了这个播放界面（而不是先退出全屏再退出界面），
            // 我们也应该尝试恢复屏幕方向。
            originalOrientation?.let { savedOrientation ->
                activity?.requestedOrientation = savedOrientation
                Log.d("PlayerScreen", "onDispose: Restoring orientation as a safeguard to $savedOrientation")
            }
        }
    }
    
    // 切换控制栏和EPG信息可见性的函数
    fun toggleControlAndEpgVisibility() {
        android.util.Log.d("FullscreenManager", "Before toggle - isControlBarVisibleInFullscreen: $isControlBarVisibleInFullscreen, isEpgInfoVisibleInFullscreen: $isEpgInfoVisibleInFullscreen")
        isControlBarVisibleInFullscreen = !isControlBarVisibleInFullscreen
        isEpgInfoVisibleInFullscreen = !isEpgInfoVisibleInFullscreen
        android.util.Log.d("FullscreenManager", "After toggle - isControlBarVisibleInFullscreen: $isControlBarVisibleInFullscreen, isEpgInfoVisibleInFullscreen: $isEpgInfoVisibleInFullscreen")
    }
    
    return remember(isControlBarVisibleInFullscreen, isEpgInfoVisibleInFullscreen) {
        FullscreenManager(
            isSystemUiVisible = isSystemUiVisible,
            isControlBarVisibleInFullscreen = isControlBarVisibleInFullscreen,
            isEpgInfoVisibleInFullscreen = isEpgInfoVisibleInFullscreen,
            toggleControlAndEpgVisibility = { toggleControlAndEpgVisibility() }
        )
    }
}

/**
 * 全屏管理器数据类
 */
data class FullscreenManager(
    val isSystemUiVisible: Boolean,
    val isControlBarVisibleInFullscreen: Boolean,
    val isEpgInfoVisibleInFullscreen: Boolean,
    val toggleControlAndEpgVisibility: () -> Unit
)