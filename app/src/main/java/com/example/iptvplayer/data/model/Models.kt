package com.example.iptvplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    val lastEpgUpdate: Long? = null
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val name: String,
    val url: String,
    val group: String,
    val logoUrl: String?,
    val tvgName: String? = null, // 新增字段
    val tvgId: String? = null, // 新增字段用于存储tvg-id
    val sequence: Int = 0 // 添加顺序字段，用于维护频道在M3U文件中的顺序
)

@Entity(tableName = "epg_programs")
data class EpgProgram(
    @PrimaryKey val id: String, // channelName + start timestamp as unique ID
    val channelName: String, // 频道名称或唯一标识
    val title: String, // 节目标题
    val start: Long, // 节目开始时间（时间戳，毫秒）
    val end: Long,   // 节目结束时间（时间戳，毫秒）
    val desc: String? = null // 节目描述
)