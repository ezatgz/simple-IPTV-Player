package com.example.iptvplayer.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceTypeUtils {

    /** 判断是否是 Android TV（通过系统特征） */
    private fun isAndroidTV(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

    /** 通过 UI Mode 判断是否是电视 */
    private fun isTVByUiMode(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /** 判断是否是平板（根据最小宽度 dp） */
    private fun isTablet(context: Context): Boolean {
        val config = context.resources.configuration
        return config.smallestScreenWidthDp >= 600
    }

    /** 获取设备类型：TV / Tablet / Phone */
    fun getDeviceType(context: Context): String {
        return when {
            isAndroidTV(context) || isTVByUiMode(context) -> "TV"
            isTablet(context) -> "Tablet"
            else -> "Phone"
        }
    }
}
