package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to dynamically fetch accurate, high-resolution official artwork and logos
 * for both songs and playlists from official music APIs (JioSaavn, Apple iTunes, Deezer).
 */
object ArtworkFetcherService {
    private const val TAG = "ArtworkFetcherService"
    private val artworkCache = ConcurrentHashMap<String, String>()

    /**
     * Fetches the correct high-res album/song artwork for the given title and artist.
     */
    suspend fun fetchSongArtwork(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val query = "$title $artist".trim()
        if (query.isBlank()) return@withContext null

        val cacheKey = "song_${title.lowercase()}_${artist.lowercase()}"
        artworkCache[cacheKey]?.let { return@withContext it }

        // 1. Try JioSaavn official search
        try {
            val jioArt = fetchFromJioSaavn(query)
            if (!jioArt.isNullOrBlank()) {
                artworkCache[cacheKey] = jioArt
                return@withContext jioArt
            }
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn artwork fetch failed for $query: ${e.message}")
        }

        // 2. Fallback to Apple iTunes Search API (High-res 600x600)
        try {
            val itunesArt = fetchFromItunes(query)
            if (!itunesArt.isNullOrBlank()) {
                artworkCache[cacheKey] = itunesArt
                return@withContext itunesArt
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes artwork fetch failed for $query: ${e.message}")
        }

        null
    }

    /**
     * Fetches high-resolution artist portrait photo.
     */
    suspend fun fetchArtistImage(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        val cacheKey = "artist_${artistName.lowercase()}"
        artworkCache[cacheKey]?.let { return@withContext it }

        // 1. Try Deezer Artist API (Official 500x500 high-res portrait)
        try {
            val encoded = URLEncoder.encode(artistName.trim(), "UTF-8")
            val url = URL("https://api.deezer.com/search/artist?q=$encoded&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val first = data.getJSONObject(0)
                    val pic = first.optString("picture_big", "").ifBlank {
                        first.optString("picture_medium", "")
                    }
                    if (pic.isNotBlank()) {
                        artworkCache[cacheKey] = pic
                        return@withContext pic
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer artist fetch failed: ${e.message}")
        }

        // 2. Fallback to iTunes
        try {
            val itunesArt = fetchFromItunes(artistName)
            if (!itunesArt.isNullOrBlank()) {
                artworkCache[cacheKey] = itunesArt
                return@withContext itunesArt
            }
        } catch (e: Exception) {
            // Ignore
        }

        null
    }

    /**
     * Fetches artwork for a playlist, album, or category topic.
     */
    suspend fun fetchPlaylistArtwork(playlistTitle: String, descriptionOrQuery: String? = null): String? = withContext(Dispatchers.IO) {
        val query = (descriptionOrQuery ?: playlistTitle).trim()
        if (query.isBlank()) return@withContext null

        val cacheKey = "playlist_${playlistTitle.lowercase()}"
        artworkCache[cacheKey]?.let { return@withContext it }

        // Try JioSaavn album or song search
        try {
            val jioArt = fetchFromJioSaavn(query)
            if (!jioArt.isNullOrBlank()) {
                artworkCache[cacheKey] = jioArt
                return@withContext jioArt
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Fallback to iTunes
        try {
            val itunesArt = fetchFromItunes(query)
            if (!itunesArt.isNullOrBlank()) {
                artworkCache[cacheKey] = itunesArt
                return@withContext itunesArt
            }
        } catch (e: Exception) {
            // Ignore
        }

        null
    }

    private fun fetchFromJioSaavn(query: String): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://www.jiosaavn.com/api.php?__call=search.getResults&_marker=0&q=$encoded&p=1&n=3&_format=json&ctx=android")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 4000
            readTimeout = 4000
        }
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val results = root.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val img = results.getJSONObject(0).optString("image", "")
                if (img.isNotBlank()) {
                    return img
                        .replace("150x150", "500x500")
                        .replace(".webp", ".jpg")
                        .replace("http://", "https://")
                }
            }
        }
        return null
    }

    private fun fetchFromItunes(query: String): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=1")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 4000
            readTimeout = 4000
        }
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val results = root.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val img = results.getJSONObject(0).optString("artworkUrl100", "")
                if (img.isNotBlank()) {
                    return img.replace("100x100bb", "600x600bb")
                }
            }
        }
        return null
    }
}
