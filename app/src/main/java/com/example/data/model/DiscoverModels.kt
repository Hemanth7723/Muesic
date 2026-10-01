package com.example.data.model

import com.example.data.entity.Song

/**
 * Represents a curated category recommendation (e.g. Romance, Workout, 90's, Pop, Chill, Best of Years).
 */
data class CategoryPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val iconType: String,
    val gradientColors: List<Long>,
    val query: String,
    val songs: List<Song> = emptyList(),
    val coverArtworkUrl: String? = null
)

/**
 * Top Songs dimensions to filter by (Language, Category, Type, Artist, Country).
 */
enum class TopSongsDimension(val label: String) {
    LANGUAGE("Language"),
    CATEGORY("Category"),
    TYPE("Type"),
    ARTIST("Artist"),
    COUNTRY("Country")
}

/**
 * Filter option within a Top Songs dimension.
 */
data class TopSongFilter(
    val id: String,
    val label: String,
    val query: String,
    val initialSongs: List<Song> = emptyList()
)

/**
 * New release playlist grouped by movie/album.
 */
data class MovieReleasePlaylist(
    val id: String,
    val movieName: String,
    val year: String,
    val composer: String,
    val posterUrl: String,
    val query: String,
    val songs: List<Song> = emptyList()
)

/**
 * Recommended artist playlist.
 */
data class RecommendedArtist(
    val id: String,
    val name: String,
    val role: String,
    val avatarUrl: String,
    val query: String,
    val songs: List<Song> = emptyList()
)

/**
 * Top Mix playlist based on song genre.
 */
data class TopMixPlaylist(
    val id: String,
    val genreName: String,
    val subtitle: String,
    val gradientColors: List<Long>,
    val query: String,
    val songs: List<Song> = emptyList(),
    val coverArtworkUrl: String? = null
)

/**
 * Generic playlist detail representation for the Discover modal sheet.
 */
data class DiscoverPlaylistDetail(
    val title: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val gradientColors: List<Long> = listOf(0xFF00F5D4, 0xFF00796B),
    val songs: List<Song> = emptyList(),
    val sourceBadge: String = "JioSaavn 320kbps HD"
)
