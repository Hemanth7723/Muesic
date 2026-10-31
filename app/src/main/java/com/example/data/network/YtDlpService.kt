package com.example.data.network

import android.content.Context
import android.util.Log
import com.example.data.entity.Song
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.security.MessageDigest

object YtDlpService {
    private const val TAG = "YtDlpService"
    private val updateMutex = Mutex()
    @Volatile
    private var stableVersionChecked = false

    suspend fun getSong(context: Context, pageUrl: String): Song =
        withContext(Dispatchers.IO) {
            val validatedUrl = validatePageUrl(pageUrl)
            initialize(context.applicationContext)
            val info = YoutubeDL.getInfo(validatedUrl)
            val title = info.title?.takeIf(String::isNotBlank)
                ?: info.fulltitle?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("yt-dlp did not return a title for this page.")
            val durationMs = info.duration.coerceAtLeast(0).toLong() * 1_000L
            val sizeBytes = info.fileSize.takeIf { it > 0 }
                ?: info.fileSizeApproximate.takeIf { it > 0 }
            val fileSizeMb = sizeBytes?.div(1024.0 * 1024.0)
                ?: (durationMs / 1_000L * 256.0 / (8.0 * 1024.0))

            Song(
                id = "ytdlp_${stableId(info.extractorKey.orEmpty(), info.id ?: validatedUrl)}",
                title = title,
                artist = info.uploader?.takeIf(String::isNotBlank)
                    ?: info.extractor?.takeIf(String::isNotBlank)
                    ?: "Unknown creator",
                album = info.extractor?.takeIf(String::isNotBlank) ?: "yt-dlp audio",
                durationMs = durationMs,
                streamUrl = validatedUrl,
                audioQuality = "yt-dlp audio",
                artworkUrl = info.thumbnail,
                genre = info.categories?.firstOrNull() ?: "Audio",
                source = "yt-dlp",
                fileSizeMb = fileSizeMb
            )
        }

    suspend fun downloadAudio(context: Context, pageUrl: String, outputFile: File) =
        withContext(Dispatchers.IO) {
            val validatedUrl = validatePageUrl(pageUrl)
            val appContext = context.applicationContext
            initialize(appContext)
            FFmpeg.getInstance().init(appContext)
            outputFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IllegalStateException("Could not create the temporary download directory.")
                }
            }
            outputFile.delete()

            val request = YoutubeDLRequest(validatedUrl)
                .addOption("--no-playlist")
                .addOption("--no-part")
                .addOption("--force-overwrites")
                .addOption("--extract-audio")
                .addOption("--audio-format", "mp3")
                .addOption("--audio-quality", "0")
                .addOption("--output", outputFile.absolutePath)
            YoutubeDL.execute(request)

            if (!outputFile.isFile || outputFile.length() == 0L) {
                throw IllegalStateException("yt-dlp completed without producing an audio file.")
            }
        }

    private suspend fun initialize(context: Context) {
        YoutubeDL.init(context)
        updateMutex.withLock {
            if (stableVersionChecked) return

            try {
                YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                stableVersionChecked = true
            } catch (e: Exception) {
                Log.w(TAG, "Could not update yt-dlp; continuing with the bundled version.", e)
            }
        }
    }

    fun validatePageUrl(value: String): String {
        val trimmed = value.trim()
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Enter a valid HTTPS media-page URL.", e)
        }

        require(
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null
        ) {
            "Enter an HTTPS media-page URL without embedded credentials."
        }
        return trimmed
    }

    private fun stableId(extractor: String, id: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$extractor:$id".toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { byte -> "%02x".format(byte) }
    }
}
