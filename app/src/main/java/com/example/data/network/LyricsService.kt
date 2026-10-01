package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class SongLyrics(
    val songTitle: String,
    val artist: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val plainText: String = "",
    val isInstrumental: Boolean = false,
    val source: String = "LrcLib"
)

object LyricsService {
    private const val TAG = "LyricsService"
    private val memoryCache = mutableMapOf<String, SongLyrics?>()

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\(official\\s*(music)?\\s*video\\)"), "")
            .replace(Regex("(?i)\\[official\\s*(music)?\\s*video\\]"), "")
            .replace(Regex("(?i)\\(official\\s*audio\\)"), "")
            .replace(Regex("(?i)\\[official\\s*audio\\]"), "")
            .replace(Regex("(?i)\\(lyrics?\\s*video\\)"), "")
            .replace(Regex("(?i)\\[lyrics?\\s*video\\]"), "")
            .replace(Regex("(?i)\\(visualizer\\)"), "")
            .replace(Regex("(?i)\\[visualizer\\]"), "")
            .replace(Regex("(?i)\\(audio\\)"), "")
            .replace(Regex("(?i)\\[audio\\]"), "")
            .replace(Regex("(?i)\\(4k\\)"), "")
            .replace(Regex("(?i)\\[4k\\]"), "")
            .replace(Regex("(?i)\\(full\\s*song\\)"), "")
            .replace(Regex("(?i)\\[full\\s*song\\]"), "")
            .replace(Regex("(?i)\\(feat\\..*?\\)"), "")
            .replace(Regex("(?i)\\[feat\\..*?\\]"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .split(",", "&", "feat.", "ft.", "/")
            .firstOrNull()
            ?.trim() ?: artist.trim()
    }

    private fun isTitleMatch(candidate: String, expected: String): Boolean {
        val c = cleanTitle(candidate).lowercase().trim()
        val e = cleanTitle(expected).lowercase().trim()
        if (c == e) return true
        if (c.contains(e) || e.contains(c)) return true
        val cWords = c.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val eWords = e.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val intersection = cWords.intersect(eWords)
        return intersection.isNotEmpty() && intersection.size >= (eWords.size / 2).coerceAtLeast(1)
    }

    private fun isArtistMatch(candidate: String, expected: String): Boolean {
        if (expected.isBlank() || expected.equals("Unknown Artist", ignoreCase = true)) return true
        val c = cleanArtist(candidate).lowercase().trim()
        val e = cleanArtist(expected).lowercase().trim()
        if (c == e) return true
        if (c.contains(e) || e.contains(c)) return true
        val cWords = c.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val eWords = e.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        return cWords.intersect(eWords).isNotEmpty()
    }

    private fun isInstrumentalPiece(songTitle: String, artist: String): Boolean {
        val composers = listOf("pachelbel", "debussy", "beethoven", "vivaldi", "satie", "chopin", "bach", "mozart", "tchaikovsky", "brahms", "handel")
        val lowerArtist = artist.lowercase()
        val lowerTitle = songTitle.lowercase()
        if (composers.any { lowerArtist.contains(it) || lowerTitle.contains(it) }) return true
        if (lowerTitle.contains("instrumental") || lowerTitle.contains("karaoke") || lowerTitle.contains("sonata") || lowerTitle.contains("nocturne") || lowerTitle.contains("canon in d") || lowerTitle.contains("gymnopédie") || lowerTitle.contains("clair de lune") || lowerTitle.contains("für elise")) return true
        return false
    }

    suspend fun fetchLyrics(songTitle: String, artist: String, durationMs: Long = 0L): SongLyrics? = withContext(Dispatchers.IO) {
        val cacheKey = "${songTitle.lowercase().trim()}_${artist.lowercase().trim()}"
        if (memoryCache.containsKey(cacheKey)) {
            return@withContext memoryCache[cacheKey]
        }

        // Check if track is a classical or instrumental composition
        if (isInstrumentalPiece(songTitle, artist)) {
            val instrumentalLyrics = SongLyrics(
                songTitle = songTitle,
                artist = artist,
                isSynced = false,
                lines = emptyList(),
                plainText = "♪ Instrumental Composition ♪\n\nComposed by $artist\nEnjoy this timeless musical piece.",
                isInstrumental = true
            )
            memoryCache[cacheKey] = instrumentalLyrics
            return@withContext instrumentalLyrics
        }

        val cleanedTitle = cleanTitle(songTitle)
        val cleanedArtist = cleanArtist(artist)

        // Attempt 1: Direct exact match from lrclib.net
        var result = queryLrcLibExact(cleanedTitle, cleanedArtist, durationMs)

        // Attempt 2: Search endpoint on lrclib.net with title and artist
        if (result == null) {
            result = queryLrcLibSearch("$cleanedTitle $cleanedArtist", cleanedTitle, cleanedArtist)
        }

        // Attempt 3: Search with just title, but strictly verify title match
        if (result == null && cleanedTitle.isNotBlank()) {
            result = queryLrcLibSearch(cleanedTitle, cleanedTitle, cleanedArtist)
        }

        if (result != null) {
            memoryCache[cacheKey] = result
        }
        result
    }

    private fun queryLrcLibExact(trackName: String, artistName: String, durationMs: Long): SongLyrics? {
        try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            val durSec = if (durationMs > 0) "&duration=${durationMs / 1000}" else ""
            val urlString = "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist$durSec"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MuesicApp/1.0 (Android; Open Source Music Player)")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                return parseLrcLibJson(JSONObject(jsonStr), trackName, artistName, durationMs)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Exact lrclib fetch failed: ${e.message}")
        }
        return null
    }

    private fun queryLrcLibSearch(query: String, expectedTitle: String, expectedArtist: String): SongLyrics? {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://lrclib.net/api/search?q=$encodedQuery"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MuesicApp/1.0 (Android; Open Source Music Player)")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                if (array.length() > 0) {
                    var bestCandidate: JSONObject? = null

                    // Priority 1: Both title and artist match
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val track = item.optString("trackName", "")
                        val artist = item.optString("artistName", "")
                        val hasLyrics = item.optString("syncedLyrics", "").isNotBlank() ||
                                item.optString("plainLyrics", "").isNotBlank() ||
                                item.optBoolean("instrumental", false)
                        if (hasLyrics && isTitleMatch(track, expectedTitle) && isArtistMatch(artist, expectedArtist)) {
                            bestCandidate = item
                            break
                        }
                    }

                    // Priority 2: Title matches if artist could not be matched
                    if (bestCandidate == null) {
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val track = item.optString("trackName", "")
                            val hasLyrics = item.optString("syncedLyrics", "").isNotBlank() ||
                                    item.optString("plainLyrics", "").isNotBlank() ||
                                    item.optBoolean("instrumental", false)
                            if (hasLyrics && isTitleMatch(track, expectedTitle)) {
                                bestCandidate = item
                                break
                            }
                        }
                    }

                    if (bestCandidate != null) {
                        val track = bestCandidate.optString("trackName", expectedTitle)
                        val artist = bestCandidate.optString("artistName", expectedArtist)
                        val duration = (bestCandidate.optDouble("duration", 180.0) * 1000).toLong()
                        return parseLrcLibJson(bestCandidate, track, artist, duration)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Search lrclib fetch failed: ${e.message}")
        }
        return null
    }

    private fun parseLrcLibJson(obj: JSONObject, defaultTitle: String, defaultArtist: String, durationMs: Long): SongLyrics {
        val isInstrumental = obj.optBoolean("instrumental", false)
        val syncedText = obj.optString("syncedLyrics", "")
        val plainText = obj.optString("plainLyrics", "")

        val parsedLines = if (syncedText.isNotBlank()) {
            parseLrcString(syncedText)
        } else if (plainText.isNotBlank()) {
            distributePlainLyrics(plainText, durationMs)
        } else {
            emptyList()
        }

        return SongLyrics(
            songTitle = obj.optString("trackName", defaultTitle),
            artist = obj.optString("artistName", defaultArtist),
            isSynced = syncedText.isNotBlank(),
            lines = parsedLines,
            plainText = plainText.ifBlank { syncedText },
            isInstrumental = isInstrumental
        )
    }

    fun parseLrcString(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
        val result = mutableListOf<LyricLine>()

        lrc.lineSequence().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val (minStr, secStr, msStr, text) = match.destructured
                val min = minStr.toLongOrNull() ?: 0L
                val sec = secStr.toLongOrNull() ?: 0L
                val ms = when {
                    msStr.isEmpty() -> 0L
                    msStr.length == 1 -> msStr.toLong() * 100L
                    msStr.length == 2 -> msStr.toLong() * 10L
                    else -> msStr.take(3).toLong()
                }
                val timeMs = min * 60_000L + sec * 1_000L + ms
                val trimmedText = text.trim()
                if (trimmedText.isNotEmpty()) {
                    result.add(LyricLine(timestampMs = timeMs, text = trimmedText))
                }
            }
        }
        return result.sortedBy { it.timestampMs }
    }

    private fun distributePlainLyrics(plain: String, durationMs: Long): List<LyricLine> {
        val lines = plain.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val totalMs = if (durationMs > 10000L) durationMs - 5000L else 180000L
        val interval = totalMs / lines.size.coerceAtLeast(1)

        return lines.mapIndexed { index, text ->
            LyricLine(timestampMs = index * interval, text = text)
        }
    }
}
