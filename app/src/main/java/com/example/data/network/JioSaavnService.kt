package com.example.data.network

import android.text.Html
import android.util.Base64
import android.util.Log
import com.example.data.entity.Song
import com.example.util.OriginalSongFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JioSaavnService {
    private const val TAG = "JioSaavnService"
    private const val DES_KEY = "38346591"

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val songs = mutableListOf<Song>()
        val isUserSearchingMix = OriginalSongFilter.isUserSearchingForMix(query)
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://www.jiosaavn.com/api.php?__call=search.getResults&_marker=0&q=$encodedQuery&p=1&n=25&_format=json&ctx=android"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                val resultsArray = rootJson.optJSONArray("results")
                if (resultsArray != null) {
                    for (i in 0 until resultsArray.length()) {
                        val songObj = resultsArray.getJSONObject(i)
                        val id = songObj.optString("id", "")
                        val rawTitle = songObj.optString("song", "")
                        val rawArtist = songObj.optString("primary_artists", "").ifBlank {
                            songObj.optString("singers", "")
                        }
                        val rawAlbum = songObj.optString("album", "")
                        val durationSec = songObj.optString("duration", "180").toLongOrNull() ?: 180L
                        val durationMs = durationSec * 1000L
                        val rawImage = songObj.optString("image", "")

                        // Upgrade thumbnail to high-res 500x500
                        val highResArtwork = rawImage
                            .replace("150x150", "500x500")
                            .replace(".webp", ".jpg")
                            .replace("http://", "https://")

                        val encryptedMediaUrl = songObj.optString("encrypted_media_url", "")
                        val streamUrl = decryptMediaUrl(encryptedMediaUrl)

                        if (id.isNotBlank() && !streamUrl.isNullOrBlank()) {
                            val cleanTitle = cleanHtml(rawTitle)
                            val cleanArtist = cleanHtml(rawArtist).ifBlank { "Unknown Artist" }
                            val cleanAlbum = cleanHtml(rawAlbum).ifBlank { "Single" }
                            val genre = songObj.optString("language", "Pop").replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            }

                            val song = Song(
                                id = "saavn_$id",
                                title = cleanTitle,
                                artist = cleanArtist,
                                album = cleanAlbum,
                                durationMs = durationMs,
                                streamUrl = streamUrl,
                                isCached = false,
                                audioQuality = "JioSaavn 320kbps HD",
                                artworkUrl = highResArtwork.ifBlank { null },
                                genre = genre,
                                source = "JioSaavn",
                                fileSizeMb = String.format(
                                    Locale.US,
                                    "%.1f",
                                    (durationSec * 320.0) / (8.0 * 1024.0)
                                ).toDoubleOrNull() ?: 7.5
                            )
                            if (OriginalSongFilter.isOriginal(song, isUserSearchingMix)) {
                                songs.add(song)
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "JioSaavn API returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from JioSaavn: ${e.message}")
        }
        return@withContext songs
    }

    private fun decryptMediaUrl(encryptedUrl: String): String? {
        if (encryptedUrl.isBlank()) return null
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val rawDecryptedUrl = String(decryptedBytes, Charsets.UTF_8).trim()
            rawDecryptedUrl
                .replace("http://aac.saavn.cdn.jiosaavn.com", "https://aac.saavncdn.com")
                .replace("https://aac.saavn.cdn.jiosaavn.com", "https://aac.saavncdn.com")
                .replace("http://", "https://")
                .replace("_96.mp4", "_320.mp4")
                .replace("_96.m4a", "_320.mp4")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt media url", e)
            null
        }
    }

    private fun cleanHtml(text: String): String {
        return try {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            text.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()
        }
    }
}
