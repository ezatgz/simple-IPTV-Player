@file:OptIn(UnstableApi::class)

package com.example.iptvplayer.ui.player

import android.media.AudioManager
import android.view.View
import android.view.Window
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.data.model.ChannelSwitchMode
import com.example.iptvplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * 添加处理双击手势的修饰符
 */
fun Modifier.addDoubleTapGesture(
    channelSwitchMode: ChannelSwitchMode?,
    scope: CoroutineScope,
    viewModel: PlayerViewModel,
    onSwitchChannel: (Int) -> Unit,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    onToggleControlAndEpgVisibility: () -> Unit
): Modifier = this.pointerInput(Unit) {
    android.util.Log.d("GestureHandler", "Initializing gesture detector - isFullscreen: $isFullscreen, isLandscape: $isLandscape")
    
    detectTapGestures(
        onDoubleTap = { offset: Offset ->
            android.util.Log.d("GestureHandler", "Double tap detected at offset: $offset, channelSwitchMode: $channelSwitchMode")
            
            // 仅在启用双击模式时处理双击事件
            if (channelSwitchMode == ChannelSwitchMode.DOUBLE_TAP) {
                val containerWidth = size.width
                val x = offset.x
                
                android.util.Log.d("GestureHandler", "Processing double tap - containerWidth: $containerWidth, x: $x")

                // 左侧1/3区域切换到上一频道
                if (x < containerWidth / 3) {
                    android.util.Log.d("Gesture", "Double tap on left side - switching to previous channel")
                    scope.launch {
                        viewModel.switchToAdjacentChannel(true, onSwitchChannel)
                    }
                }
                // 右侧1/3区域切换到下一频道
                else if (x > containerWidth * 2 / 3) {
                    android.util.Log.d("Gesture", "Double tap on right side - switching to next channel")
                    scope.launch {
                        viewModel.switchToAdjacentChannel(false, onSwitchChannel)
                    }
                }
                else {
                    android.util.Log.d("GestureHandler", "Double tap in center area - no action")
                }
            } else {
                android.util.Log.d("GestureHandler", "Double tap ignored - channelSwitchMode is not DOUBLE_TAP")
            }
        },
        onLongPress = { offset: Offset ->
            android.util.Log.d("GestureHandler", "Long press detected at offset: $offset")
            
            val containerWidth = size.width
            val x = offset.x
            
            android.util.Log.d("GestureHandler", "Processing long press - containerWidth: $containerWidth, x: $x")

            // 检查是否在中间1/3区域
            if (x >= containerWidth / 3 && x <= containerWidth * 2 / 3) {
                // 检查屏幕方向是否为横向
                if (isLandscape) {
                    // 伪功能反馈检测到长按
                    android.util.Log.d("Gesture", "Long press detected in center third area during fullscreen landscape mode")
                    // 这里可以添加实际的功能实现
                } else {
                    android.util.Log.d("GestureHandler", "Long press in center area but not in landscape mode")
                }
            } else {
                android.util.Log.d("GestureHandler", "Long press not in center area")
            }
        },
        onTap = { offset: Offset ->
            android.util.Log.d("GestureHandler", "Single tap detected at offset: $offset, isFullscreen: $isFullscreen")
            
            if (isFullscreen) {
                // 点击屏幕切换控制栏和EPG信息的可见性
                android.util.Log.d("GestureHandler", "Toggling control and EPG visibility in fullscreen mode")
                onToggleControlAndEpgVisibility()
            } else {
                android.util.Log.d("GestureHandler", "Single tap in non-fullscreen mode - no action")
                // 非全屏模式下点击不隐藏控制栏
            }
        }
    )
}


/**
 * 添加处理水平滑动手势的修饰符（用于频道切换）
 */
fun Modifier.addHorizontalDragGesture(
    channelSwitchMode: ChannelSwitchMode?,
    scope: CoroutineScope,
    viewModel: PlayerViewModel,
    onSwitchChannel: (Int) -> Unit,
    isChannelSwitching: MutableState<Boolean>,
    onChannelSwitching: ((Boolean) -> Unit)? = null
): Modifier = this.pointerInput(Unit) {
    detectHorizontalDragGestures(
        onDragStart = { offset: Offset ->
            // 手势开始
            android.util.Log.d("Gesture", "Horizontal drag start")
        },
        onDragEnd = {
            // 手势结束，重置频道切换状态
            android.util.Log.d("Gesture", "Horizontal drag end")
            scope.launch {
                // 延迟一段时间再重置状态，确保切换操作已完成
                kotlinx.coroutines.delay(500)
                if (onChannelSwitching != null) {
                    onChannelSwitching(false)
                } else {
                    isChannelSwitching.value = false
                }
            }
        },
        onDragCancel = {
            // 手势取消，重置频道切换状态
            android.util.Log.d("Gesture", "Horizontal drag cancel")
            scope.launch {
                // 延迟一段时间再重置状态，确保切换操作已完成
                kotlinx.coroutines.delay(500)
                if (onChannelSwitching != null) {
                    onChannelSwitching(false)
                } else {
                    isChannelSwitching.value = false
                }
            }
        },
        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
            // 仅在启用滑动模式时处理滑动事件
            if (channelSwitchMode == ChannelSwitchMode.SWIPE) {
                // 处理水平拖动，dragAmount > 0 表示向右滑动，dragAmount < 0 表示向左滑动
                android.util.Log.d("Gesture", "Horizontal drag detected, dragAmount: $dragAmount")

                // 提高阈值以减少误触发，只有在未进行频道切换且滑动距离超过阈值时才执行频道切换
                val dragThreshold = 50f // 提高阈值到100像素

                // 添加防抖动处理，确保手势是主要的水平滑动
                val previousPosition = change.previousPosition
                val currentPosition = change.position
                val positionDeltaX = currentPosition.x - previousPosition.x
                val positionDeltaY = currentPosition.y - previousPosition.y
                val isHorizontalDrag = abs(positionDeltaX) > abs(positionDeltaY)

                val switchingState = if (onChannelSwitching != null) {
                    // 如果提供了回调函数，我们需要外部管理状态
                    isChannelSwitching.value
                } else {
                    // 否则直接使用状态值
                    isChannelSwitching.value
                }

                if (!switchingState && abs(dragAmount) > dragThreshold && isHorizontalDrag) {
                    android.util.Log.d("Gesture", "Horizontal drag threshold exceeded: $dragThreshold, isHorizontalDrag: $isHorizontalDrag")

                    // 设置频道切换状态，防止重复触发
                    if (onChannelSwitching != null) {
                        onChannelSwitching(true)
                    } else {
                        isChannelSwitching.value = true
                    }

                    // 执行频道切换
                    scope.launch {
                        // 添加延迟以确保手势完成
                        kotlinx.coroutines.delay(100)
                        viewModel.switchToAdjacentChannel(dragAmount > 0, onSwitchChannel)
                    }

                    // 消费所有事件，防止重复触发
                    change.consume()
                }
            }
        }
    )
}

/**
 * 添加处理垂直滑动手势的修饰符（用于音量和亮度调节）
 */
fun Modifier.addVerticalDragGesture(
    audioManager: AudioManager,
    view: View,
    window: Window,
    scope: CoroutineScope,
    onVolumePercentageChange: (Int?) -> Unit,
    onBrightnessPercentageChange: (Int?) -> Unit
): Modifier = this.pointerInput(audioManager, view, window) {
    detectVerticalDragGestures(
        onDragStart = { offset: Offset ->
            // 手势开始时不需要特殊处理
        },
        onDragEnd = {
            // 手势结束时，延迟一段时间后隐藏音量和亮度指示器
            scope.launch {
                kotlinx.coroutines.delay(1000) // 延迟1秒后隐藏
                onVolumePercentageChange(null)
                onBrightnessPercentageChange(null)
            }
        },
        onDragCancel = {
            // 手势取消时，同样延迟隐藏音量和亮度指示器
            scope.launch {
                kotlinx.coroutines.delay(1000) // 延迟1秒后隐藏
                onVolumePercentageChange(null)
                onBrightnessPercentageChange(null)
            }
        },
        onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
            android.util.Log.d("Gesture", "Vertical drag detected, dragAmount: $dragAmount")

            val containerWidth = size.width
            val x = change.position.x
            // 左侧1/3区域处理亮度调节
            if (x < containerWidth / 3) {
                android.util.Log.d("Gesture", "Adjusting brightness, dragAmount: $dragAmount")

                // 向上滑动增加亮度，向下滑动减少亮度
                val delta = -sign(dragAmount).toInt() * 0.01f // 每次调节1%亮度
                val layoutParams = window.attributes
                val currentBrightness = layoutParams.screenBrightness
                // 如果当前亮度是默认值(-1)，则获取系统亮度
                val actualBrightness = if (currentBrightness < 0) {
                    android.provider.Settings.System.getInt(
                        view.context.contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        127
                    ) / 255f
                } else {
                    currentBrightness
                }
                val newBrightness = (actualBrightness + delta).coerceIn(0f, 1f)
                layoutParams.screenBrightness = newBrightness
                window.attributes = layoutParams
                val percentage = (newBrightness * 100).toInt()
                onBrightnessPercentageChange(percentage)
                // 消费事件防止其他手势处理
                change.consume()
            }
            // 右侧1/3区域处理音量调节
            else if (x > containerWidth * 2 / 3) {
                android.util.Log.d("Gesture", "Adjusting volume, dragAmount: $dragAmount")

                // 向上滑动增加音量，向下滑动减少音量
                val delta = -sign(dragAmount).toInt() * 1 // 每次调节1个单位
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val newVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                val percentage = (newVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
                onVolumePercentageChange(percentage)
                // 消费事件防止其他手势处理
                change.consume()
            }
        }
    )
}
