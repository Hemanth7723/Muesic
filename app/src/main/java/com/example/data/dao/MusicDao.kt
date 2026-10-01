package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ListeningRecord
import com.example.data.entity.Playlist
import com.example.data.entity.PlaylistSongCrossRef
import com.example.data.entity.SearchHistory
import com.example.data.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<Song>

    @Query("SELECT * FROM songs WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 30")
    fun getLastPlayedSongs(): Flow<List<Song>>

    @Query("""
        SELECT DISTINCT songs.* FROM songs
        WHERE isFavorite = 1 OR isCached = 1 OR id IN (SELECT songId FROM playlist_song_cross_ref)
        ORDER BY title ASC
    """)
    fun getLibrarySavedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isCached = 1 ORDER BY title ASC")
    fun getCachedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY lastPlayedTimestamp DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY playCount DESC, lastPlayedTimestamp DESC")
    suspend fun getFavoriteSongsList(): List<Song>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedTimestamp DESC LIMIT :limit")
    suspend fun getTopPlayedSongs(limit: Int = 20): List<Song>

    @Query("SELECT * FROM songs WHERE genre = :genre AND id != :excludeId ORDER BY playCount DESC LIMIT :limit")
    suspend fun getSongsByGenre(genre: String, excludeId: String, limit: Int = 15): List<Song>

    @Query("SELECT * FROM songs WHERE (artist LIKE '%' || :artist || '%' OR :artist LIKE '%' || artist || '%') AND id != :excludeId ORDER BY playCount DESC LIMIT :limit")
    suspend fun getSongsByArtist(artist: String, excludeId: String, limit: Int = 15): List<Song>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): Song?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE songs SET isCached = :isCached, localFilePath = :localPath WHERE id = :id")
    suspend fun updateCacheStatus(id: String, isCached: Boolean, localPath: String?)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun recordPlay(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM songs WHERE streamUrl LIKE '%codeskulptor-demos%' OR streamUrl LIKE '%actions.google.com%' OR streamUrl LIKE '%archive.org%' OR streamUrl LIKE '%aac.saavn.cdn.jiosaavn.com%' OR id IN ('saavn_aRZbUYD7', 'saavn_ZAwUB9Jy', 'saavn_uiEWT3kP', 'ytm_d3YwD6e_r_k')")
    suspend fun deleteOldPlaceholderSongs()

    @Query("DELETE FROM songs WHERE streamUrl LIKE '%codeskulptor-demos%' OR streamUrl LIKE '%actions.google.com%' OR id IN ('saavn_aRZbUYD7', 'saavn_ZAwUB9Jy', 'saavn_uiEWT3kP', 'ytm_d3YwD6e_r_k')")
    suspend fun deleteLegacyBrokenSongs()

    @Query("SELECT * FROM songs WHERE localFilePath = :path LIMIT 1")
    suspend fun getSongByLocalPath(path: String): Song?

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<String>)

    @Query("DELETE FROM songs WHERE id IN ('saavn_pyJaNwrF', 'saavn_cLZ23OAQ', 'saavn_jTbaEykH') OR title LIKE '%(Remix)%' OR title LIKE '%(Instrumental%' OR title LIKE '%Karaoke%' OR artist LIKE '%Covers Culture%' OR artist LIKE '%Nxsser%' OR artist LIKE '%Veronica Bravo%' OR artist LIKE '%ZZang KARAOKE%' OR artist LIKE '%Workout Music%'")
    suspend fun deleteCoversAndRemixes()

    @Query("DELETE FROM songs WHERE source = 'Sample' OR title LIKE 'Sample%' OR title LIKE 'Test Track%'")
    suspend fun deleteSampleTracks(): Int
}

@Dao
interface PlaylistDao {
    @Query("DELETE FROM playlists WHERE title LIKE 'Sample%' OR title LIKE 'Test%'")
    suspend fun deleteSamplePlaylists(): Int

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId IN (SELECT id FROM playlists WHERE title LIKE 'Sample%' OR title LIKE 'Test%')")
    suspend fun deleteSamplePlaylistRefs(): Int

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsList(): List<Playlist>

    @Query("SELECT * FROM playlist_song_cross_ref")
    suspend fun getAllCrossRefsList(): List<PlaylistSongCrossRef>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.songId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
        ORDER BY playlist_song_cross_ref.addedAt ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: String): Int
}

@Dao
interface ListeningDao {
    @Insert
    suspend fun insertRecord(record: ListeningRecord)

    @Query("SELECT genre FROM listening_records GROUP BY genre ORDER BY COUNT(*) DESC LIMIT 5")
    suspend fun getTopGenres(): List<String>

    @Query("SELECT artist FROM listening_records GROUP BY artist ORDER BY COUNT(*) DESC LIMIT 5")
    suspend fun getTopArtists(): List<String>

    @Query("SELECT * FROM listening_records ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<ListeningRecord>>
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistory)

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getSearchHistory(): Flow<List<String>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllSearchHistory(): List<SearchHistory>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history WHERE query != :query AND :query LIKE query || '%'")
    suspend fun deletePrefixesOf(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
