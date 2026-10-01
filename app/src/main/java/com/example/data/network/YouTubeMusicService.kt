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

object YouTubeMusicService {
    private const val TAG = "YouTubeMusicService"
    private const val YTM_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val songs = mutableListOf<Song>()
        try {
            val url = URL(YTM_SEARCH_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val payload = JSONObject().apply {
                val contextObj = JSONObject().apply {
                    val clientObj = JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20231204.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    }
                    put("client", clientObj)
                }
                put("context", contextObj)
                put("query", query.trim())
                // Filter strictly for Songs (Audio Tracks), avoiding user videos and random playlists
                put("params", "EgWKAQIIAWoECAEQAQ%3D%3D")
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                parseMusicItems(rootJson, songs)
            } else {
                Log.w(TAG, "YouTube Music API returned HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying YouTube Music: ${e.message}")
        }
        val isUserSearchingMix = OriginalSongFilter.isUserSearchingForMix(query)
        return@withContext songs.filter { OriginalSongFilter.isOriginal(it, isUserSearchingMix) }
    }

    private fun parseMusicItems(root: Any, results: MutableList<Song>) {
        when (root) {
            is JSONObject -> {
                if (root.has("musicResponsiveListItemRenderer")) {
                    val item = root.optJSONObject("musicResponsiveListItemRenderer")
                    if (item != null) {
                        extractSong(item)?.let { results.add(it) }
                    }
                }
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    parseMusicItems(root.get(key), results)
                }
            }
            is JSONArray -> {
                for (i in 0 until root.length()) {
                    parseMusicItems(root.get(i), results)
                }
            }
        }
    }

    private fun extractSong(item: JSONObject): Song? {
        try {
            var videoId = ""
            val flexCols = item.optJSONArray("flexColumns") ?: return null

            // Find videoId from navigationEndpoint in flexColumns or item
            for (i in 0 until flexCols.length()) {
                val col = flexCols.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val textObj = col?.optJSONObject("text")
                val runs = textObj?.optJSONArray("runs")
                if (runs != null) {
                    for (j in 0 until runs.length()) {
                        val run = runs.optJSONObject(j)
                        val nav = run?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                        val vid = nav?.optString("videoId", "")
                        if (!vid.isNullOrBlank()) {
                            videoId = vid
                            break
                        }
                    }
                }
                if (videoId.isNotBlank()) break
            }

            if (videoId.isBlank()) {
                val nav = item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                videoId = nav?.optString("videoId", "") ?: ""
            }

            if (videoId.isBlank()) return null

            // Extract title from first flexColumn
            var title = ""
            if (flexCols.length() > 0) {
                val col0 = flexCols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val runs0 = col0?.optJSONObject("text")?.optJSONArray("runs")
                if (runs0 != null && runs0.length() > 0) {
                    title = runs0.optJSONObject(0)?.optString("text", "") ?: ""
                }
            }
            if (title.isBlank()) return null

            // Extract artist and duration from second flexColumn
            var artist = "YouTube Music"
            var album = "Single"
            var durationMs = 180000L

            if (flexCols.length() > 1) {
                val col1 = flexCols.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val runs1 = col1?.optJSONObject("text")?.optJSONArray("runs")
                if (runs1 != null) {
                    val textTokens = mutableListOf<String>()
                    for (k in 0 until runs1.length()) {
                        val t = runs1.optJSONObject(k)?.optString("text", "")?.trim() ?: ""
                        if (t.isNotBlank() && t != "•") {
                            textTokens.add(t)
                        }
                    }
                    if (textTokens.isNotEmpty()) {
                        if (textTokens[0].equals("Song", ignoreCase = true)) {
                            artist = if (textTokens.size > 1) textTokens[1] else "YouTube Music"
                            if (textTokens.size > 2 && !textTokens[2].contains(":")) {
                                album = textTokens[2]
                            }
                        } else {
                            artist = textTokens[0]
                            if (textTokens.size > 1 && !textTokens[1].contains(":")) {
                                album = textTokens[1]
                            }
                        }
                    }
                    for (token in textTokens) {
                        if (token.contains(":")) {
                            val parts = token.split(":")
                            if (parts.size == 2) {
                                val min = parts[0].toLongOrNull() ?: 3L
                                val sec = parts[1].toLongOrNull() ?: 0L
                                durationMs = (min * 60L + sec) * 1000L
                            } else if (parts.size == 3) {
                                val hr = parts[0].toLongOrNull() ?: 0L
                                val min = parts[1].toLongOrNull() ?: 0L
                                val sec = parts[2].toLongOrNull() ?: 0L
                                durationMs = (hr * 3600L + min * 60L + sec) * 1000L
                            }
                        }
                    }
                }
            }

            // Thumbnail
            var artworkUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val thumbObj = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
            val thumbs = thumbObj?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                val candidate = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                if (candidate.isNotBlank()) {
                    artworkUrl = if (candidate.startsWith("//")) "https:$candidate" else candidate
                }
            }

            val durationSec = durationMs / 1000L
            val fileSize = String.format(Locale.US, "%.1f", (durationSec * 256.0) / (8.0 * 1024.0)).toDoubleOrNull() ?: 6.2

            return Song(
                id = "ytm_$videoId",
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                streamUrl = "https://www.youtube.com/watch?v=$videoId",
                isCached = false,
                audioQuality = "YouTube Music 256k",
                artworkUrl = artworkUrl,
                genre = "Pop",
                source = "YouTube Music",
                fileSizeMb = fileSize
            )
        } catch (e: Exception) {
            return null
        }
    }
}
