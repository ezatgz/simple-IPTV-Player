package com.example.iptvplayer.data.local

import androidx.room.*
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("UPDATE playlists SET epgUrl = :epgUrl WHERE id = :playlistId")
    suspend fun updateEpgUrl(playlistId: Int, epgUrl: String?)


    @Query("UPDATE playlists SET lastEpgUpdate = :timestamp WHERE id = :playlistId")
    suspend fun updateLastEpgUpdate(playlistId: Int, timestamp: Long?)
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY `group` ASC, sequence ASC")
    fun getChannelsForPlaylist(playlistId: Int): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE url = :url LIMIT 1")
    suspend fun getChannelByUrl(url: String): Channel?

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Int): Channel?

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND sequence = :sequence LIMIT 1")
    suspend fun getChannelBySequence(playlistId: Int, sequence: Int): Channel?

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelCount(playlistId: Int): Int

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: Int)
}

@Dao
interface EpgProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgram>)

    @Query("""SELECT * FROM epg_programs 
        WHERE channelName = :channelName 
        AND start <= :currentTime 
        AND end > :currentTime 
        AND date(start/1000, 'unixepoch', 'localtime') = date(:currentTime/1000, 'unixepoch', 'localtime')
        LIMIT 1""")
    suspend fun getCurrentProgramForChannel(channelName: String, currentTime: Long): EpgProgram?

    @Query("SELECT * FROM epg_programs WHERE channelName = :channelName")
    suspend fun getProgramsForChannel(channelName: String): List<EpgProgram>

    @Query("""SELECT * FROM epg_programs 
        WHERE channelName = :channelName 
        AND end > :currentTime 
        AND date(start/1000, 'unixepoch', 'localtime') = date(:currentTime/1000, 'unixepoch', 'localtime')
        ORDER BY start ASC LIMIT 10""")
    suspend fun getUpcomingProgramsForChannel(channelName: String, currentTime: Long): List<EpgProgram>

    @Query("DELETE FROM epg_programs WHERE channelName = :channelName")
    suspend fun deleteProgramsForChannel(channelName: String)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()
}