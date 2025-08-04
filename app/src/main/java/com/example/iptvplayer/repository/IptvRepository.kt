package com.example.iptvplayer.repository

import com.example.iptvplayer.data.local.ChannelDao
import com.example.iptvplayer.data.local.PlaylistDao
import com.example.iptvplayer.data.local.EpgProgramDao
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.Playlist
import com.example.iptvplayer.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import android.util.Log

class IptvRepository(private val playlistDao: PlaylistDao, private val channelDao: ChannelDao, private val epgProgramDao: EpgProgramDao) {

    val playlists = playlistDao.getAllPlaylists()

    fun getChannels(playlistId: Int) = channelDao.getChannelsForPlaylist(playlistId)

    suspend fun deletePlaylist(playlist: Playlist) {
        channelDao.deleteChannelsForPlaylist(playlist.id)
        playlistDao.delete(playlist)
    }

    suspend fun importM3uFromUrl(url: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = URL(url).readText()
            if (!content.startsWith("#EXTM3U")) {
                return@withContext Result.failure(IllegalArgumentException("无效的M3U文件"))
            }

            val playlistId = playlistDao.insert(Playlist(name = name, url = url)).toInt()
            val channels = parseM3uContent(content, playlistId)
            channelDao.insertAll(channels)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 添加处理本地M3U文件导入的方法
    suspend fun importM3uFromFileContent(content: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!content.startsWith("#EXTM3U")) {
                return@withContext Result.failure(IllegalArgumentException("无效的M3U文件"))
            }

            // 对于本地文件，我们使用"file://"作为URL占位符
            val playlistId = playlistDao.insert(Playlist(name = name, url = "file://$name.m3u")).toInt()
            val channels = parseM3uContent(content, playlistId)
            channelDao.insertAll(channels)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistEpgUrl(playlistId: Int, epgUrl: String?) {
        playlistDao.updateEpgUrl(playlistId, epgUrl)
    }

    suspend fun validateEpgUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val content = URL(url).readText()
            // 检查内容是否包含XMLTV相关标签
            content.contains("<tv", ignoreCase = true) && content.contains("</tv>", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun parseEpgAndStore(url: String, playlistId: Int) = withContext(Dispatchers.IO) {
        Log.d("EPG_DEBUG", "Starting EPG parsing for URL: $url")
        try {
            // Clear all existing EPG data
            epgProgramDao.deleteAllPrograms()

            val content = URL(url).readText()
            Log.d("EPG_DEBUG", "EPG response received, size: ${content.length}")

            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(content.reader())

            var eventType = parser.eventType
            val programmes = mutableListOf<EpgProgram>()
            val channelIdToDisplayName = mutableMapOf<String, String>()

            // First pass: Build channel ID to display name mapping
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "channel") {
                    val channelId = parser.getAttributeValue(null, "id")
                    // Look for display-name element
                    var displayName: String? = null
                    var inChannel = true
                    
                    while (inChannel && eventType != XmlPullParser.END_DOCUMENT) {
                        eventType = parser.next()
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "display-name") {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.TEXT) {
                                        displayName = parser.text
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "channel") {
                                    inChannel = false
                                }
                            }
                        }
                    }
                    
                    if (channelId != null && !displayName.isNullOrBlank()) {
                        channelIdToDisplayName[channelId] = displayName
                        Log.d("EPG_DEBUG", "Mapped channel ID '$channelId' to display name '$displayName'")
                    }
                } else {
                    eventType = parser.next()
                }
            }

            Log.d("EPG_DEBUG", "Channel ID to display name mapping completed, size: ${channelIdToDisplayName.size}")

            // Reset parser for second pass
            parser.setInput(content.reader())
            eventType = parser.eventType

            // Second pass: Parse programmes
            var currentChannelId: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentStart: String? = null
            var currentStop: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentChannelId = parser.getAttributeValue(null, "channel")
                                currentStart = parser.getAttributeValue(null, "start")
                                currentStop = parser.getAttributeValue(null, "stop")
                                Log.d("EPG_DEBUG", "Found programme element for channel: $currentChannelId")
                            }
                            "title" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentTitle = parser.text
                                }
                            }
                            "desc" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentDesc = parser.text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                Log.d("EPG_DEBUG", "End of programme element, channel: $currentChannelId, title: $currentTitle")
                                if (currentChannelId != null && currentStart != null && currentStop != null && currentTitle != null) {
                                    try {
                                        val startMillis = parseXmltvTime(currentStart)
                                        val endMillis = parseXmltvTime(currentStop)
                                        
                                        // Use display name as channelName if available, otherwise use channel ID
                                        val displayName = channelIdToDisplayName[currentChannelId] ?: currentChannelId
                                        val programId = "${currentChannelId}_$startMillis"
                                        val program = EpgProgram(
                                            id = programId,
                                            channelName = displayName, // Use display name from EPG
                                            title = currentTitle,
                                            start = startMillis,
                                            end = endMillis,
                                            desc = currentDesc
                                        )
                                        programmes.add(program)
                                        Log.d("EPG_DEBUG", "Added programme: ${program.title} for channel ${program.channelName} (ID: $currentChannelId)")
                                    } catch (e: Exception) {
                                        Log.e("EPG_DEBUG", "Error parsing programme: ${e.message}")
                                    }
                                }
                                // Reset variables
                                currentChannelId = null
                                currentTitle = null
                                currentDesc = null
                                currentStart = null
                                currentStop = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            Log.d("EPG_DEBUG", "Total programmes parsed: ${programmes.size}")

            // Store programmes in database in batches
            val batchSize = 100
            for (i in programmes.indices step batchSize) {
                val batch = programmes.subList(i, minOf(i + batchSize, programmes.size))
                epgProgramDao.insertAll(batch)
                Log.d("EPG_DEBUG", "Stored batch of ${batch.size} programmes")
            }

            // Update the last EPG update time for the playlist
            playlistDao.updateLastEpgUpdate(playlistId, System.currentTimeMillis())
            Log.d("EPG_DEBUG", "EPG parsing and storage completed, updated lastEpgUpdate for playlist $playlistId")
        } catch (e: Exception) {
            Log.e("EPG_DEBUG", "Error parsing EPG: ${e.message}", e)
        }
    }

    suspend fun getCurrentEpgForChannelFromDb(channelName: String): EpgProgram? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        return@withContext epgProgramDao.getCurrentProgramForChannel(channelName, now)
    }

    suspend fun getUpcomingEpgForChannel(channelName: String): List<EpgProgram> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        return@withContext epgProgramDao.getUpcomingProgramsForChannel(channelName, now)
    }

    suspend fun getCurrentEpgForChannelByTvgName(tvgName: String): EpgProgram? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        return@withContext epgProgramDao.getCurrentProgramForChannel(tvgName, now)
    }

    suspend fun getCurrentEpgForChannel(epgUrl: String, tvgId: String?, tvgName: String?): EpgProgram? = withContext(Dispatchers.IO) {
        try {
            // Validate and fix malformed URLs
            val fixedUrl = fixMalformedUrl(epgUrl)
            Log.d("EPG_DEBUG", "Original URL: $epgUrl, Fixed URL: $fixedUrl")
            val content = URL(fixedUrl).readText()
            val now = System.currentTimeMillis()
            Log.d("EPG_DEBUG", "Current time: $now (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(now))})")
            
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(content.reader())
            
            var eventType = parser.eventType
            val programmes = mutableMapOf<String, MutableList<EpgProgram>>()
            val channelIdToDisplayName = mutableMapOf<String, String>()

            // First pass: Build channel ID to display name mapping
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "channel") {
                    val channelId = parser.getAttributeValue(null, "id")
                    // Look for display-name element
                    var displayName: String? = null
                    var inChannel = true
                    
                    while (inChannel && eventType != XmlPullParser.END_DOCUMENT) {
                        eventType = parser.next()
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "display-name") {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.TEXT) {
                                        displayName = parser.text
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "channel") {
                                    inChannel = false
                                }
                            }
                        }
                    }
                    
                    if (channelId != null && !displayName.isNullOrBlank()) {
                        channelIdToDisplayName[channelId] = displayName
                        Log.d("EPG_DEBUG", "Mapped channel ID '$channelId' to display name '$displayName'")
                    }
                } else {
                    eventType = parser.next()
                }
            }

            Log.d("EPG_DEBUG", "Channel ID to display name mapping completed, size: ${channelIdToDisplayName.size}")

            // Reset parser for second pass
            parser.setInput(content.reader())
            eventType = parser.eventType
            
            // Second pass: Parse programmes
            var currentChannelId: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentStart: String? = null
            var currentStop: String? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                currentChannelId = parser.getAttributeValue(null, "channel")
                                currentStart = parser.getAttributeValue(null, "start")
                                currentStop = parser.getAttributeValue(null, "stop")
                            }
                            "title" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentTitle = parser.text
                                }
                            }
                            "desc" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentDesc = parser.text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                if (currentChannelId != null && currentStart != null && currentStop != null && currentTitle != null) {
                                    try {
                                        val startMillis = parseXmltvTime(currentStart)
                                        val endMillis = parseXmltvTime(currentStop)
                                        
                                        // Use display name as channelName if available, otherwise use channel ID
                                        val displayName = channelIdToDisplayName[currentChannelId] ?: currentChannelId
                                        val programId = "${currentChannelId}_$startMillis"
                                        val program = EpgProgram(
                                            id = programId,
                                            channelName = displayName, // Use display name from EPG
                                            title = currentTitle,
                                            start = startMillis,
                                            end = endMillis,
                                            desc = currentDesc
                                        )
                                        
                                        val list = programmes.getOrPut(displayName) { mutableListOf() }
                                        list.add(program)
                                    } catch (e: Exception) {
                                        Log.e("EPG_DEBUG", "Error parsing programme: ${e.message}")
                                    }
                                }
                                // Reset variables
                                currentChannelId = null
                                currentTitle = null
                                currentDesc = null
                                currentStart = null
                                currentStop = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            // Find current program for the specified tvgName
            if (!tvgName.isNullOrBlank()) {
                val progs = programmes[tvgName]
                Log.d("EPG_DEBUG", "Trying tvg-name match: $tvgName, found ${progs?.size ?: 0} programmes")
                progs?.firstOrNull { now in it.start..it.end }?.also {
                    Log.d("EPG_DEBUG", "Found current programme via tvg-name: ${it.title} ${it.start}-${it.end}")
                }?.let { return@withContext it }
            }
            
            Log.d("EPG_DEBUG", "No EPG match found for tvgName=$tvgName")
            return@withContext null
        } catch (e: Exception) {
            Log.e("EPG_DEBUG", "EPG解析异常: ${e.message}", e)
            return@withContext null
        }
    }


    private fun parseM3uContent(content: String, playlistId: Int): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                try {
                    val name = line.substringAfterLast(",").trim()
                    val group = extractValue(line, "group-title") ?: "未分组"
                    val logo = extractValue(line, "tvg-logo")
                    val tvgName = extractValue(line, "tvg-name")
                    val tvgId = extractValue(line, "tvg-id")

                    // The next line should be the stream URL
                    val streamUrl = lines.getOrNull(++i)?.trim()
                    if (streamUrl != null && (streamUrl.startsWith("http") || streamUrl.startsWith("rtsp"))) {
                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                url = streamUrl,
                                group = group,
                                logoUrl = logo,
                                tvgName = tvgName,
                                tvgId = tvgId
                                // 注意：这里不设置sequence，将在后面统一设置
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip malformed entry
                    e.printStackTrace()
                }
            }
            i++
        }
        
        // 按照与频道列表显示相同的顺序对频道进行排序并设置sequence
        return sortAndAssignSequence(channels)
    }
    
    private fun sortAndAssignSequence(channels: List<Channel>): List<Channel> {
        // 先按group分组
        val grouped = channels.groupBy { it.group }
        
        // 创建新的列表，按照频道列表显示的顺序排列
        val sortedChannels = mutableListOf<Channel>()
        
        // 按group名称排序，然后在每个group内保持原有顺序
        grouped.toSortedMap().forEach { (_, groupChannels) ->
            sortedChannels.addAll(groupChannels)
        }
        
        // 为每个频道分配sequence值
        return sortedChannels.mapIndexed { index, channel ->
            channel.copy(sequence = index)
        }
    }


    private fun parseXmltvTime(time: String?): Long {
        // 例：20240610190000 +0000
        return try {
            if (time == null) return 0L
            val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss Z", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(time)?.time ?: 0L
        } catch (e: Exception) { 
            Log.e("EPG_DEBUG", "Time parsing error for: $time", e)
            0L 
        }
    }

    private fun extractValue(line: String, key: String): String? {
        return Regex("""$key="([^"]*)"""").find(line)?.groupValues?.get(1)
    }

    private fun fixMalformedUrl(url: String): String {
        var fixedUrl = url.trim()
        // Fix common malformed URLs
        if (fixedUrl.startsWith("http://") || fixedUrl.startsWith("https://")) {
            return fixedUrl
        }
        // Fix missing colon in https://
        if (fixedUrl.startsWith("https//")) {
            fixedUrl = "https://" + fixedUrl.substring(6)
            Log.d("EPG_DEBUG", "Fixed missing colon in HTTPS URL")
        } else if (fixedUrl.startsWith("http//")) {
            fixedUrl = "http://" + fixedUrl.substring(5)
            Log.d("EPG_DEBUG", "Fixed missing colon in HTTP URL")
        }
        return fixedUrl
    }
}
