package com.example.data.network

import android.util.Log
import com.example.data.entity.Song
import com.example.util.OriginalSongFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object YouTubeService {
    private const val TAG = "YouTubeService"
    private const val YT_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search"

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val songs = mutableListOf<Song>()
        try {
            val url = URL(YT_SEARCH_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", "https://www.youtube.com")
                setRequestProperty("Referer", "https://www.youtube.com/")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val isUserSearchingMix = OriginalSongFilter.isUserSearchingForMix(query)
            val effectiveQuery = if (!isUserSearchingMix && !query.contains(Regex("(?i)\\b(audio|song|official)\\b"))) {
                "${query.trim()} official audio"
            } else {
                query.trim()
            }

            val payload = JSONObject().apply {
                val contextObj = JSONObject().apply {
                    val clientObj = JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20231201.00.00")
                        put("hl", "en")
                        put("gl", "US")
                    }
                    put("client", clientObj)
                }
                put("context", contextObj)
                put("query", effectiveQuery)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                parseVideoItems(rootJson, songs)
            } else {
                Log.w(TAG, "YouTube API returned HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying YouTube: ${e.message}")
        }

        val isUserSearchingMix = OriginalSongFilter.isUserSearchingForMix(query)
        songs.filter { OriginalSongFilter.isOriginal(it, isUserSearchingMix) }
    }

    private fun parseVideoItems(root: Any, results: MutableList<Song>) {
        when (root) {
            is JSONObject -> {
                if (root.has("videoRenderer")) {
                    val item = root.optJSONObject("videoRenderer")
                    if (item != null) {
                        extractSong(item)?.let { results.add(it) }
                    }
                }
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    parseVideoItems(root.get(key), results)
                }
            }
            is JSONArray -> {
                for (i in 0 until root.length()) {
                    parseVideoItems(root.get(i), results)
                }
            }
        }
    }

    private fun extractSong(item: JSONObject): Song? {
        try {
            val videoId = item.optString("videoId", "")
            if (videoId.isBlank()) return null

            // Title
            val titleRuns = item.optJSONObject("title")?.optJSONArray("runs")
            val titleBuilder = StringBuilder()
            if (titleRuns != null) {
                for (i in 0 until titleRuns.length()) {
                    titleBuilder.append(titleRuns.optJSONObject(i)?.optString("text", ""))
                }
            }
            val title = titleBuilder.toString().trim()
            if (title.isBlank()) return null

            // Owner / Artist
            val ownerRuns = item.optJSONObject("ownerText")?.optJSONArray("runs")
            val ownerBuilder = StringBuilder()
            if (ownerRuns != null) {
                for (i in 0 until ownerRuns.length()) {
                    ownerBuilder.append(ownerRuns.optJSONObject(i)?.optString("text", ""))
                }
            }
            val artist = ownerBuilder.toString().trim().ifBlank { "YouTube Creator" }

            // Duration
            val lengthText = item.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
            var durationMs = 210000L
            if (lengthText.isNotBlank() && lengthText.contains(":")) {
                val parts = lengthText.split(":")
                if (parts.size == 2) {
                    val m = parts[0].toLongOrNull() ?: 3L
                    val s = parts[1].toLongOrNull() ?: 30L
                    durationMs = (m * 60L + s) * 1000L
                } else if (parts.size == 3) {
                    val h = parts[0].toLongOrNull() ?: 0L
                    val m = parts[1].toLongOrNull() ?: 0L
                    val s = parts[2].toLongOrNull() ?: 0L
                    durationMs = (h * 3600L + m * 60L + s) * 1000L
                }
            }

            // Thumbnail
            val thumbs = item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            var artworkUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            if (thumbs != null && thumbs.length() > 0) {
                artworkUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", artworkUrl) ?: artworkUrl
            }

            val durationSec = durationMs / 1000L
            val fileSize = String.format(Locale.US, "%.1f", (durationSec * 256.0) / (8.0 * 1024.0)).toDoubleOrNull() ?: 7.0

            return Song(
                id = "yt_$videoId",
                title = title,
                artist = artist,
                album = "YouTube Audio",
                durationMs = durationMs,
                streamUrl = "https://www.youtube.com/watch?v=$videoId",
                isCached = false,
                audioQuality = "YouTube Audio",
                artworkUrl = artworkUrl,
                genre = "Audio Stream",
                source = "YouTube",
                fileSizeMb = fileSize
            )
        } catch (e: Exception) {
            return null
        }
    }
}
