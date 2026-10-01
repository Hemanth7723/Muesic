package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val localFilePath: String? = null,
    val isCached: Boolean = false,
    val audioQuality: String = "Hi-Fi 320kbps", // e.g. "FLAC 24-bit/96kHz Lossless"
    val artworkUrl: String? = null,
    val genre: String = "Electronic",
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val source: String = "OpenMusic", // "OpenMusic", "Imported", "Local"
    val fileSizeMb: Double = 8.4
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCollaborative: Boolean = false,
    val shareCode: String = "",
    val contributorNames: String = "You" // comma separated list
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_records")
data class ListeningRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val songId: String,
    val genre: String,
    val artist: String,
    val durationListenedMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val hourOfDay: Int = 12
)

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
