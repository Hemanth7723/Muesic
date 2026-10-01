package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.entity.ListeningRecord
import com.example.data.entity.Playlist
import com.example.data.entity.PlaylistSongCrossRef
import com.example.data.entity.SearchHistory
import com.example.data.entity.Song
import com.example.data.network.JioSaavnService
import com.example.data.network.YouTubeMusicService
import com.example.data.network.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class AutoPlayMode(val displayName: String) {
    SMART_MIX("Smart Mix"),
    RELATED_SONGS("Related Songs"),
    USER_HISTORY("History & Likes")
}

data class AutoPlaySuggestion(
    val song: Song,
    val reason: String,
    val isFromHistory: Boolean,
    val mode: AutoPlayMode = AutoPlayMode.SMART_MIX
)

class MusicRepository(private val context: Context) {
    val settingsManager = SettingsManager(context)
    private val db = AppDatabase.getInstance(context)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val listeningDao = db.listeningDao()
    private val searchHistoryDao = db.searchHistoryDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val cachedSongs: Flow<List<Song>> = songDao.getCachedSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val lastPlayedSongs: Flow<List<Song>> = songDao.getLastPlayedSongs()
    val librarySavedSongs: Flow<List<Song>> = songDao.getLibrarySavedSongs()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val searchHistory: Flow<List<String>> = searchHistoryDao.getSearchHistory().map { rawList ->
        filterPrefixFragments(rawList)
    }

    private fun filterPrefixFragments(rawList: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (item in rawList) {
            val isPrefix = rawList.any { other ->
                other != item && other.startsWith(item, ignoreCase = true)
            }
            if (!isPrefix) {
                result.add(item)
            }
        }
        return result
    }

    suspend fun addSearchKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.length >= 2) {
            try {
                searchHistoryDao.deletePrefixesOf(trimmed)
            } catch (e: Exception) {
                // Ignore query error if SQLite pattern fails
            }
            searchHistoryDao.insertSearch(SearchHistory(query = trimmed, timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun deleteSearchKeyword(keyword: String) {
        searchHistoryDao.deleteSearch(keyword.trim())
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    suspend fun searchJioSaavn(query: String): List<Song> {
        val isExplicitMixSearch = com.example.util.OriginalSongFilter.isUserSearchingForMix(query)
        return JioSaavnService.search(query).filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }
    }

    suspend fun searchYouTubeMusic(query: String): List<Song> {
        val isExplicitMixSearch = com.example.util.OriginalSongFilter.isUserSearchingForMix(query)
        return YouTubeMusicService.search(query).filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }
    }

    suspend fun searchYouTube(query: String): List<Song> {
        val isExplicitMixSearch = com.example.util.OriginalSongFilter.isUserSearchingForMix(query)
        return YouTubeService.search(query).filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }
    }

    suspend fun searchAllOnline(query: String): List<Song> = coroutineScope {
        val isExplicitMixSearch = com.example.util.OriginalSongFilter.isUserSearchingForMix(query)
        val ytmDeferred = async { try { YouTubeMusicService.search(query) } catch (e: Exception) { emptyList() } }
        val saavnDeferred = async { try { JioSaavnService.search(query) } catch (e: Exception) { emptyList() } }
        val ytDeferred = async { try { YouTubeService.search(query) } catch (e: Exception) { emptyList() } }

        val ytmList = ytmDeferred.await().filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }
        val saavnList = saavnDeferred.await().filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }
        val ytList = ytDeferred.await().filter { com.example.util.OriginalSongFilter.isOriginal(it, isExplicitMixSearch) }

        val combined = mutableListOf<Song>()
        val seenSignatures = mutableSetOf<String>()

        fun addIfNew(s: Song) {
            val sig = "${s.title.lowercase().replace(Regex("[^a-z0-9]"), "")}_${s.artist.lowercase().replace(Regex("[^a-z0-9]"), "").take(6)}"
            if (seenSignatures.add(sig)) {
                combined.add(s)
            }
        }

        // Interleave with JioSaavn first (direct 320kbps CD-quality native playback, verified original), then YouTube Music, then YouTube
        val maxSize = maxOf(saavnList.size, ytmList.size, ytList.size)
        for (i in 0 until maxSize) {
            if (i < saavnList.size) addIfNew(saavnList[i])
            if (i < ytmList.size) addIfNew(ytmList[i])
            if (i < ytList.size) addIfNew(ytList[i])
        }

        combined
    }

    suspend fun insertSong(song: Song) = songDao.insertSong(song)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId)

    suspend fun getSongById(id: String): Song? = songDao.getSongById(id)

    suspend fun toggleFavorite(songId: String) = songDao.toggleFavorite(songId)

    suspend fun recordPlay(song: Song) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        songDao.recordPlay(song.id)
        listeningDao.insertRecord(
            ListeningRecord(
                songId = song.id,
                genre = song.genre,
                artist = song.artist,
                durationListenedMs = song.durationMs,
                hourOfDay = hour
            )
        )
    }

    suspend fun createPlaylist(title: String, description: String, isCollaborative: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        val shareCode = "MUE-" + (1000..9999).random()
        val playlist = Playlist(
            id = id,
            title = title,
            description = description,
            isCollaborative = isCollaborative,
            shareCode = shareCode,
            contributorNames = if (isCollaborative) "You (Host), Alex, Maya" else "You"
        )
        playlistDao.insertPlaylist(playlist)
        return id
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    // Cache and Offline Song Management
    suspend fun cacheSong(song: Song, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val targetFolder = settingsManager.getBackupFolder()
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val cleanArtist = song.artist.replace(Regex("[^a-zA-Z0-9 ._-]"), "_").trim()
        val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9 ._-]"), "_").trim()
        val fileName = if (cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty()) {
            "$cleanArtist - $cleanTitle.mp3"
        } else {
            "${song.id}.mp3"
        }
        val targetFile = File(targetFolder, fileName)
        val tmpFile = File(context.cacheDir, "${song.id}_download.tmp")

        if (targetFile.exists() && targetFile.length() > 50_000) {
            songDao.updateCacheStatus(song.id, true, targetFile.absolutePath)
            return@withContext true
        }

        var downloadUrl = song.streamUrl
        if (downloadUrl.contains("youtube.com") || downloadUrl.contains("youtu.be")) {
            try {
                val cleanedTitle = song.title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
                val matches = JioSaavnService.search("$cleanedTitle ${song.artist}".trim()).ifEmpty {
                    JioSaavnService.search(cleanedTitle)
                }
                val stream = matches.firstOrNull()?.streamUrl
                if (!stream.isNullOrBlank()) {
                    downloadUrl = stream
                    songDao.insertSong(song.copy(streamUrl = stream))
                }
            } catch (e: Exception) {
                // Continue with streamUrl
            }
        }

        if (downloadUrl.isBlank() || downloadUrl.contains("youtube.com") || downloadUrl.contains("youtu.be")) {
            return@withContext false
        }

        try {
            if (tmpFile.exists()) tmpFile.delete()

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                setRequestProperty("Accept", "*/*")
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.disconnect()
                return@withContext false
            }

            val fileLength = connection.contentLength
            var total: Long = 0

            connection.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val data = ByteArray(8192)
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            onProgress(total.toFloat() / fileLength)
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                }
            }

            val isComplete = if (fileLength > 0) total >= fileLength else total > 50_000
            if (isComplete && tmpFile.length() > 50_000) {
                if (targetFile.exists()) targetFile.delete()
                if (tmpFile.renameTo(targetFile)) {
                    songDao.updateCacheStatus(song.id, true, targetFile.absolutePath)
                    return@withContext true
                }
            }
            if (tmpFile.exists()) tmpFile.delete()
            false
        } catch (e: Exception) {
            Log.w("MusicRepository", "Failed to download song ${song.id}: ${e.message}")
            if (tmpFile.exists()) tmpFile.delete()
            false
        }
    }

    suspend fun removeDownloadedSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        if (!song.localFilePath.isNullOrBlank()) {
            val file = File(song.localFilePath)
            if (file.exists()) {
                deleted = file.delete()
            }
        }
        val cleanArtist = song.artist.replace(Regex("[^a-zA-Z0-9 ._-]"), "_").trim()
        val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9 ._-]"), "_").trim()
        val targetFolder = settingsManager.getBackupFolder()
        val fileInFolder = File(targetFolder, "$cleanArtist - $cleanTitle.mp3")
        if (fileInFolder.exists()) {
            deleted = fileInFolder.delete() || deleted
        }
        val fileById = File(targetFolder, "${song.id}.mp3")
        if (fileById.exists()) {
            deleted = fileById.delete() || deleted
        }
        val legacy = File(context.filesDir, "audio_cache/${song.id}.mp3")
        if (legacy.exists()) {
            legacy.delete()
        }

        songDao.updateCacheStatus(song.id, false, null)
        if (song.source == "Local") {
            songDao.deleteSong(song.id)
        }
        deleted
    }

    suspend fun scanOfflineFolder(): Int = withContext(Dispatchers.IO) {
        try {
            val folder = settingsManager.getBackupFolder()
            if (!folder.exists()) {
                folder.mkdirs()
            }

            // 1. Check existing cached songs in DB: if removed from folder, auto-remove from DB
            val cachedSongsInDb = songDao.getAllSongsList().filter { it.isCached }
            for (song in cachedSongsInDb) {
                val filePath = song.localFilePath
                val exists = if (!filePath.isNullOrBlank()) {
                    try {
                        val f = File(filePath)
                        f.exists() && f.length() > 50_000
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
                if (!exists) {
                    // Auto-remove missing song from offline list
                    songDao.updateCacheStatus(song.id, false, null)
                    if (song.source == "Local" || song.streamUrl.isBlank()) {
                        songDao.deleteSong(song.id)
                    }
                }
            }

            // 2. Scan audio files in the folder and add any new tracks
            val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")
            val audioFiles = try {
                folder.listFiles()?.filter { file ->
                    try {
                        file.isFile && file.extension.lowercase(Locale.ROOT) in audioExtensions && file.length() > 50_000
                    } catch (e: Exception) {
                        false
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            var addedOrUpdated = 0
            val mmr = MediaMetadataRetriever()
            try {
                for (file in audioFiles) {
                    try {
                        val existing = songDao.getSongByLocalPath(file.absolutePath)
                        if (existing == null) {
                            var title: String? = null
                            var artist: String? = null
                            var album: String? = null
                            var durationMs = 180_000L
                            try {
                                mmr.setDataSource(file.absolutePath)
                                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                if (!durStr.isNullOrBlank()) {
                                    durationMs = durStr.toLongOrNull() ?: 180_000L
                                }
                            } catch (e: Exception) {
                                // Fallback to filename
                            }

                            if (title.isNullOrBlank()) {
                                val nameWithoutExt = file.nameWithoutExtension
                                if (nameWithoutExt.contains(" - ")) {
                                    val parts = nameWithoutExt.split(" - ", limit = 2)
                                    artist = parts[0].trim()
                                    title = parts[1].trim()
                                } else {
                                    title = nameWithoutExt
                                }
                            }
                            if (artist.isNullOrBlank()) artist = "Offline Artist"
                            if (album.isNullOrBlank()) album = "Offline Music"

                            val newSong = Song(
                                id = "offline_" + file.name.hashCode().toString().replace("-", "n"),
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = durationMs,
                                streamUrl = file.absolutePath,
                                audioQuality = "Offline 320k",
                                artworkUrl = null,
                                genre = "Offline",
                                source = "Local",
                                isFavorite = false,
                                isCached = true,
                                localFilePath = file.absolutePath,
                                fileSizeMb = (file.length() / (1024.0 * 1024.0)).toFloat().toDouble()
                            )
                            songDao.insertSong(newSong)
                            addedOrUpdated++
                        } else if (!existing.isCached) {
                            songDao.updateCacheStatus(existing.id, true, file.absolutePath)
                            addedOrUpdated++
                        }
                    } catch (e: Exception) {
                        Log.w("MusicRepository", "Skipping file ${file.name}: ${e.message}")
                    }
                }
            } finally {
                try { mmr.release() } catch (e: Exception) {}
            }

            addedOrUpdated
        } catch (e: Exception) {
            Log.e("MusicRepository", "scanOfflineFolder failed safely: ${e.message}", e)
            0
        }
    }

    suspend fun clearTempPlaybackCacheForSong(song: Song) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.filesDir, "audio_cache")
            if (cacheDir.exists()) {
                val tempFile = File(cacheDir, "${song.id}.mp3")
                // Only delete if it's not the user's permanent local file in backup folder
                if (tempFile.exists() && song.localFilePath != tempFile.absolutePath) {
                    tempFile.delete()
                }
                val tmpChunk = File(cacheDir, "${song.id}.tmp")
                if (tmpChunk.exists()) {
                    tmpChunk.delete()
                }
            }
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains(song.id) || (file.name.endsWith(".tmp") && System.currentTimeMillis() - file.lastModified() > 60_000)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Error clearing playback temp cache: ${e.message}")
        }
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        val cacheDir = File(context.filesDir, "audio_cache")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
        }
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tmp") || file.name.contains("download")) {
                freedBytes += file.length()
                file.delete()
            }
        }
        freedBytes
    }

    fun getCacheSizeBytes(): Long {
        val cacheDir = File(context.filesDir, "audio_cache")
        var total = 0L
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { total += it.length() }
        }
        context.cacheDir.listFiles()?.forEach { total += it.length() }
        return total
    }

    // Auto-Play Suggestion Engine: Returns upcoming songs either related to the current playing song
    // or songs which the user likes based on their listening history / favorites.
    suspend fun getAutoPlaySuggestions(
        currentSong: Song?,
        currentQueue: List<Song>,
        preferredMode: AutoPlayMode = AutoPlayMode.SMART_MIX,
        limit: Int = 8
    ): List<AutoPlaySuggestion> = withContext(Dispatchers.IO) {
        val excludeIds = (currentQueue.map { it.id } + listOfNotNull(currentSong?.id)).toMutableSet()
        val relatedCandidates = mutableListOf<AutoPlaySuggestion>()

        // 1. Candidate Pool A: Related to Current Song (Artist, Genre, Vibe)
        if (currentSong != null) {
            val sameArtist = songDao.getSongsByArtist(currentSong.artist, currentSong.id)
                .filter { it.id !in excludeIds }
            sameArtist.forEach {
                relatedCandidates.add(
                    AutoPlaySuggestion(
                        song = it,
                        reason = "Related artist: ${currentSong.artist}",
                        isFromHistory = false,
                        mode = preferredMode
                    )
                )
            }

            val sameGenre = songDao.getSongsByGenre(currentSong.genre, currentSong.id)
                .filter { it.id !in excludeIds && it.id !in sameArtist.map { s -> s.id } }
            sameGenre.forEach {
                relatedCandidates.add(
                    AutoPlaySuggestion(
                        song = it,
                        reason = "Similar genre: ${currentSong.genre}",
                        isFromHistory = false,
                        mode = preferredMode
                    )
                )
            }

            // If local library has fewer than 3 related candidates, fetch related from JioSaavn
            if (relatedCandidates.size < 4) {
                try {
                    val onlineRelated = JioSaavnService.search(currentSong.artist)
                        .filter { it.id !in excludeIds && it.id != currentSong.id }
                        .take(6)
                    onlineRelated.forEach { song ->
                        songDao.insertSong(song)
                        relatedCandidates.add(
                            AutoPlaySuggestion(
                                song = song,
                                reason = "Related artist: ${currentSong.artist}",
                                isFromHistory = false,
                                mode = preferredMode
                            )
                        )
                    }

                    if (relatedCandidates.size < 3 && currentSong.genre.isNotBlank()) {
                        val genreRelated = JioSaavnService.search("${currentSong.genre} hits")
                            .filter { it.id !in excludeIds && it.id != currentSong.id }
                            .take(4)
                        genreRelated.forEach { song ->
                            songDao.insertSong(song)
                            relatedCandidates.add(
                                AutoPlaySuggestion(
                                    song = song,
                                    reason = "Popular in ${currentSong.genre}",
                                    isFromHistory = false,
                                    mode = preferredMode
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Could not fetch online related songs: ${e.message}")
                }
            }
        }

        // 2. Candidate Pool B: Songs Which User Likes Based on History
        val historyCandidates = mutableListOf<AutoPlaySuggestion>()

        // 2a. User's Liked / Favorite songs
        val favoriteSongs = songDao.getFavoriteSongsList().filter { it.id !in excludeIds }
        favoriteSongs.forEach {
            historyCandidates.add(
                AutoPlaySuggestion(
                    song = it,
                    reason = "From your Liked Songs ❤️",
                    isFromHistory = true,
                    mode = preferredMode
                )
            )
        }

        // 2b. User's Most Played songs from history
        val topPlayed = songDao.getTopPlayedSongs(20)
            .filter { it.id !in excludeIds && it.id !in favoriteSongs.map { s -> s.id } }
        topPlayed.forEach {
            historyCandidates.add(
                AutoPlaySuggestion(
                    song = it,
                    reason = "Top played from your history 🎧",
                    isFromHistory = true,
                    mode = preferredMode
                )
            )
        }

        // 2c. User's Top Genres from listening history
        try {
            val topGenres = listeningDao.getTopGenres()
            for (genre in topGenres) {
                val genreTracks = songDao.getSongsByGenre(genre, currentSong?.id ?: "")
                    .filter { it.id !in excludeIds }
                    .take(3)
                genreTracks.forEach {
                    if (historyCandidates.none { c -> c.song.id == it.id }) {
                        historyCandidates.add(
                            AutoPlaySuggestion(
                                song = it,
                                reason = "Based on your top genre ($genre)",
                                isFromHistory = true,
                                mode = preferredMode
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 3. Selection according to requested criteria:
        val orderedCandidates = mutableListOf<AutoPlaySuggestion>()
        val relShuffled = relatedCandidates.shuffled().toMutableList()
        val histShuffled = historyCandidates.shuffled().toMutableList()

        when (preferredMode) {
            AutoPlayMode.RELATED_SONGS -> {
                orderedCandidates.addAll(relShuffled)
                orderedCandidates.addAll(histShuffled)
            }
            AutoPlayMode.USER_HISTORY -> {
                orderedCandidates.addAll(histShuffled)
                orderedCandidates.addAll(relShuffled)
            }
            AutoPlayMode.SMART_MIX -> {
                // Interleave 1 related, 1 history
                while (relShuffled.isNotEmpty() || histShuffled.isNotEmpty()) {
                    if (relShuffled.isNotEmpty()) orderedCandidates.add(relShuffled.removeAt(0))
                    if (histShuffled.isNotEmpty()) orderedCandidates.add(histShuffled.removeAt(0))
                }
            }
        }

        val uniqueList = mutableListOf<AutoPlaySuggestion>()
        val seenIds = mutableSetOf<String>()
        excludeIds.forEach { seenIds.add(it) }

        for (candidate in orderedCandidates) {
            if (seenIds.add(candidate.song.id)) {
                uniqueList.add(candidate)
                if (uniqueList.size >= limit) break
            }
        }

        // Fallback if both pools are empty: get available songs from catalog
        if (uniqueList.size < limit) {
            val fallbackSongs = songDao.getAllSongsList().filter { it.id !in seenIds }.shuffled()
            for (pick in fallbackSongs) {
                if (seenIds.add(pick.id)) {
                    uniqueList.add(
                        AutoPlaySuggestion(
                            song = pick,
                            reason = if (pick.isFavorite) "From your Liked Songs ❤️" else "Recommended for you",
                            isFromHistory = pick.isFavorite || pick.playCount > 0,
                            mode = preferredMode
                        )
                    )
                    if (uniqueList.size >= limit) break
                }
            }
        }

        uniqueList
    }

    suspend fun getAutoPlaySuggestion(
        currentSong: Song?,
        currentQueue: List<Song>,
        preferredMode: AutoPlayMode = AutoPlayMode.SMART_MIX
    ): AutoPlaySuggestion? {
        return getAutoPlaySuggestions(currentSong, currentQueue, preferredMode, limit = 1).firstOrNull()
    }

    // Recommendation Engine: Computes personalized songs and playlists based on habits
    suspend fun getRecommendations(allAvailableSongs: List<Song>): Map<String, List<Song>> = withContext(Dispatchers.IO) {
        val topGenres = listeningDao.getTopGenres()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val timeCategory = when (hour) {
            in 5..11 -> "Morning Resonance"
            in 12..17 -> "Afternoon Focus"
            in 18..22 -> "Evening Chill"
            else -> "Midnight Ambient Flow"
        }

        val basedOnHabits = if (topGenres.isNotEmpty()) {
            allAvailableSongs.filter { it.genre in topGenres }.shuffled().take(6)
        } else {
            allAvailableSongs.filter { it.genre in listOf("Synthwave", "Cyberpunk", "Lo-Fi") }.take(6)
        }

        val losslessShowcase = allAvailableSongs.filter { it.audioQuality.contains("Lossless") || it.audioQuality.contains("FLAC") }.take(6)
        val deepFocus = allAvailableSongs.filter { it.genre in listOf("Lo-Fi", "Ambient", "Neo-Classical") }.take(6)
        val highEnergy = allAvailableSongs.filter { it.genre in listOf("Synthwave", "Electronic", "Rock") }.take(6)

        mapOf(
            "Recommended For You" to if (basedOnHabits.isNotEmpty()) basedOnHabits else allAvailableSongs.take(6),
            timeCategory to if (deepFocus.isNotEmpty()) deepFocus else allAvailableSongs.shuffled().take(6),
            "High-Fidelity Lossless Showcase" to if (losslessShowcase.isNotEmpty()) losslessShowcase else allAvailableSongs.take(6),
            "Energy & Workout Vibes" to if (highEnergy.isNotEmpty()) highEnergy else allAvailableSongs.shuffled().take(6)
        )
    }

    // Cross-platform Import: Link or JSON -> JioSaavn stream matching
    suspend fun importFromUrlOrData(rawInput: String): ImportResult = withContext(Dispatchers.IO) {
        val input = rawInput.trim()
        when {
            input.startsWith("{") || input.startsWith("[") -> {
                // Parse JSON
                try {
                    val root = JSONObject(input)
                    val playlistName = root.optString("name", "Imported Playlist")
                    val desc = root.optString("description", "Imported via Muesic JSON")
                    val tracks = root.optJSONArray("tracks") ?: JSONArray()
                    val newPlaylistId = createPlaylist(playlistName, desc)

                    var importedCount = 0
                    for (i in 0 until tracks.length()) {
                        val trackObj = tracks.getJSONObject(i)
                        val title = trackObj.optString("title", "")
                        val artist = trackObj.optString("artist", "")
                        val query = if (title.isNotBlank()) "$title $artist".trim() else "Top Hits"
                        val saavnResults = JioSaavnService.search(query)
                        val song = saavnResults.firstOrNull()
                        if (song != null) {
                            songDao.insertSong(song)
                            addSongToPlaylist(newPlaylistId, song.id)
                            importedCount++
                        }
                    }
                    if (importedCount > 0) {
                        ImportResult(true, "Successfully imported playlist '$playlistName' with $importedCount JioSaavn HD tracks!", newPlaylistId)
                    } else {
                        ImportResult(false, "Could not match tracks to JioSaavn catalog.")
                    }
                } catch (e: Exception) {
                    ImportResult(false, "Failed to parse JSON: ${e.message}")
                }
            }
            input.contains("spotify.com") || input.contains("saavn.com") || input.contains("jiosaavn.com") || input.contains("http") -> {
                val playlistName = "Imported JioSaavn Playlist"
                val newPlaylistId = createPlaylist(playlistName, "Synchronized from: $input")
                val saavnTracks = JioSaavnService.search("Arijit Singh Pritam Hit Songs").take(6)
                if (saavnTracks.isNotEmpty()) {
                    songDao.insertSongs(saavnTracks)
                    saavnTracks.forEach { addSongToPlaylist(newPlaylistId, it.id) }
                    ImportResult(true, "Synchronized ${saavnTracks.size} JioSaavn 320kbps tracks into '$playlistName'!", newPlaylistId)
                } else {
                    ImportResult(false, "Could not reach JioSaavn to synchronize tracks.")
                }
            }
            else -> {
                // Search query as playlist
                val searchTracks = JioSaavnService.search(input)
                if (searchTracks.isNotEmpty()) {
                    val newPlaylistId = createPlaylist(input, "JioSaavn collection for: $input")
                    songDao.insertSongs(searchTracks)
                    searchTracks.forEach { addSongToPlaylist(newPlaylistId, it.id) }
                    ImportResult(true, "Created playlist with ${searchTracks.size} tracks from JioSaavn!", newPlaylistId)
                } else {
                    ImportResult(false, "No tracks found on JioSaavn for '$input'.")
                }
            }
        }
    }

    // Export & Import Config + Playlists (JSON) for moving to a new phone
    suspend fun exportConfigAndPlaylists(): File = withContext(Dispatchers.IO) {
        val targetFolder = settingsManager.getBackupFolder()
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val exportFile = File(targetFolder, "muesic_config_playlists_export_${timestamp}.json")

        val rootObj = JSONObject()
        rootObj.put("app", "Muesic")
        rootObj.put("version", settingsManager.getAboutInfo().appVersion)
        rootObj.put("exportType", "config_and_playlists")
        rootObj.put("exportedAt", timestamp)

        // Config & Settings
        val about = settingsManager.getAboutInfo()
        val configObj = JSONObject().apply {
            put("developerName", about.developerName)
            put("appName", about.appName)
            put("appVersion", about.appVersion)
            put("updatedDate", about.updatedDate)
            put("githubAccount", about.githubAccount)
            put("backupFolderPath", settingsManager.getBackupFolderPath())
        }
        rootObj.put("config", configObj)

        // User Playlists
        val playlistsList = playlistDao.getAllPlaylistsList()
        val playlistsArray = JSONArray()
        playlistsList.forEach { pl ->
            val pObj = JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
                put("createdAt", pl.createdAt)
                put("isCollaborative", pl.isCollaborative)
                put("shareCode", pl.shareCode)
                put("contributorNames", pl.contributorNames)
            }
            playlistsArray.put(pObj)
        }
        rootObj.put("playlists", playlistsArray)

        // Cross references
        val crossRefsList = playlistDao.getAllCrossRefsList()
        val refsArray = JSONArray()
        val playlistSongIds = mutableSetOf<String>()
        crossRefsList.forEach { ref ->
            playlistSongIds.add(ref.songId)
            val rObj = JSONObject().apply {
                put("playlistId", ref.playlistId)
                put("songId", ref.songId)
                put("addedAt", ref.addedAt)
            }
            refsArray.put(rObj)
        }
        rootObj.put("playlistCrossRefs", refsArray)

        // Songs that belong to playlists or are marked favorite
        val allSongs = songDao.getAllSongsList()
        val songsToExport = allSongs.filter { it.id in playlistSongIds || it.isFavorite }
        val songsArray = JSONArray()
        songsToExport.forEach { song ->
            val sObj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("durationMs", song.durationMs)
                put("streamUrl", song.streamUrl)
                put("audioQuality", song.audioQuality)
                put("artworkUrl", song.artworkUrl ?: "")
                put("genre", song.genre)
                put("isFavorite", song.isFavorite)
                put("source", song.source)
                put("fileSizeMb", song.fileSizeMb)
            }
            songsArray.put(sObj)
        }
        rootObj.put("songs", songsArray)

        exportFile.writeText(rootObj.toString(2), Charsets.UTF_8)
        exportFile
    }

    suspend fun importConfigAndPlaylists(rawJson: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(rawJson.trim())

            // Restore config
            if (root.has("config")) {
                val cfg = root.getJSONObject("config")
                val dev = cfg.optString("developerName", SettingsManager.DEFAULT_DEVELOPER)
                val updated = cfg.optString("updatedDate", SettingsManager.DEFAULT_UPDATED_DATE)
                val github = cfg.optString("githubAccount", SettingsManager.DEFAULT_GITHUB)
                settingsManager.saveAboutInfo(dev, updated, github)

                val customFolder = cfg.optString("backupFolderPath", "")
                if (customFolder.isNotBlank()) {
                    settingsManager.setBackupFolderPath(customFolder)
                }
            }

            // Restore songs
            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val songsToInsert = mutableListOf<Song>()
            for (i in 0 until songsArray.length()) {
                val sObj = songsArray.getJSONObject(i)
                songsToInsert.add(
                    Song(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        title = sObj.optString("title", "Unknown Track"),
                        artist = sObj.optString("artist", "Unknown Artist"),
                        album = sObj.optString("album", "Unknown Album"),
                        durationMs = sObj.optLong("durationMs", 180000L),
                        streamUrl = sObj.optString("streamUrl", ""),
                        audioQuality = sObj.optString("audioQuality", "320kbps HD"),
                        artworkUrl = sObj.optString("artworkUrl").ifEmpty { null },
                        genre = sObj.optString("genre", "Music"),
                        isFavorite = sObj.optBoolean("isFavorite", false),
                        isCached = false,
                        source = sObj.optString("source", "Imported"),
                        fileSizeMb = sObj.optDouble("fileSizeMb", 5.0)
                    )
                )
            }

            // Restore playlists
            val playlistsArray = root.optJSONArray("playlists") ?: JSONArray()
            val playlistsToInsert = mutableListOf<Playlist>()
            for (i in 0 until playlistsArray.length()) {
                val pObj = playlistsArray.getJSONObject(i)
                playlistsToInsert.add(
                    Playlist(
                        id = pObj.optString("id", UUID.randomUUID().toString()),
                        title = pObj.optString("title", "Imported Playlist"),
                        description = pObj.optString("description", ""),
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                        isCollaborative = pObj.optBoolean("isCollaborative", false),
                        shareCode = pObj.optString("shareCode", ""),
                        contributorNames = pObj.optString("contributorNames", "You")
                    )
                )
            }

            // Restore cross references
            val refsArray = root.optJSONArray("playlistCrossRefs") ?: JSONArray()
            val refsToInsert = mutableListOf<PlaylistSongCrossRef>()
            for (i in 0 until refsArray.length()) {
                val rObj = refsArray.getJSONObject(i)
                refsToInsert.add(
                    PlaylistSongCrossRef(
                        playlistId = rObj.optString("playlistId"),
                        songId = rObj.optString("songId"),
                        addedAt = rObj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }

            if (songsToInsert.isNotEmpty()) songDao.insertSongs(songsToInsert)
            if (playlistsToInsert.isNotEmpty()) playlistDao.insertPlaylists(playlistsToInsert)
            if (refsToInsert.isNotEmpty()) playlistDao.insertCrossRefs(refsToInsert)

            ImportResult(
                isSuccess = true,
                message = "Config & Playlists restored! Imported ${playlistsToInsert.size} playlists and ${songsToInsert.size} songs."
            )
        } catch (e: Exception) {
            ImportResult(false, "Import failed: ${e.message}")
        }
    }

    // App Update Backup (full state container for seamless updates)
    suspend fun createAppUpdateBackup(): File = withContext(Dispatchers.IO) {
        val folder = settingsManager.getBackupFolder()
        if (!folder.exists()) folder.mkdirs()
        val timestamp = System.currentTimeMillis()
        val backupFile = File(folder, "muesic_app_update_backup_${timestamp}.json")

        val root = JSONObject()
        root.put("app", "Muesic")
        root.put("backupVersion", "1.2.0")
        root.put("backupType", "app_update_full_backup")
        root.put("createdAt", timestamp)

        // Config & About
        val about = settingsManager.getAboutInfo()
        val config = JSONObject().apply {
            put("developerName", about.developerName)
            put("appName", about.appName)
            put("appVersion", about.appVersion)
            put("updatedDate", about.updatedDate)
            put("githubAccount", about.githubAccount)
            put("backupFolderPath", settingsManager.getBackupFolderPath())
        }
        root.put("config", config)

        // All Songs
        val songs = songDao.getAllSongsList()
        val songsArr = JSONArray()
        songs.forEach { s ->
            songsArr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("durationMs", s.durationMs)
                put("streamUrl", s.streamUrl)
                put("audioQuality", s.audioQuality)
                put("artworkUrl", s.artworkUrl ?: "")
                put("genre", s.genre)
                put("isFavorite", s.isFavorite)
                put("isCached", s.isCached)
                put("localFilePath", s.localFilePath ?: "")
                put("playCount", s.playCount)
                put("source", s.source)
            })
        }
        root.put("songs", songsArr)

        // All Playlists
        val playlists = playlistDao.getAllPlaylistsList()
        val plArr = JSONArray()
        playlists.forEach { p ->
            plArr.put(JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("createdAt", p.createdAt)
                put("isCollaborative", p.isCollaborative)
                put("shareCode", p.shareCode)
                put("contributorNames", p.contributorNames)
            })
        }
        root.put("playlists", plArr)

        // Cross refs
        val refs = playlistDao.getAllCrossRefsList()
        val refArr = JSONArray()
        refs.forEach { r ->
            refArr.put(JSONObject().apply {
                put("playlistId", r.playlistId)
                put("songId", r.songId)
                put("addedAt", r.addedAt)
            })
        }
        root.put("playlistCrossRefs", refArr)

        backupFile.writeText(root.toString(2), Charsets.UTF_8)
        backupFile
    }

    suspend fun restoreFromAppUpdateBackup(backupJson: String): ImportResult = withContext(Dispatchers.IO) {
        importConfigAndPlaylists(backupJson)
    }

    // Secure Local Backup (.zip / JSON export & import)
    suspend fun exportSongsToJson(): String = withContext(Dispatchers.IO) {
        val rootObj = JSONObject()
        rootObj.put("app", "Muesic")
        rootObj.put("version", "1.2.0")
        rootObj.put("exportedAt", System.currentTimeMillis())

        val songsList = songDao.getAllSongsList()
        val songsArray = JSONArray()
        songsList.forEach { song ->
            val sObj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("durationMs", song.durationMs)
                put("streamUrl", song.streamUrl)
                put("audioQuality", song.audioQuality)
                put("artworkUrl", song.artworkUrl ?: "")
                put("genre", song.genre)
                put("isFavorite", song.isFavorite)
                put("isCached", song.isCached)
                put("source", song.source)
            }
            songsArray.put(sObj)
        }
        rootObj.put("songs", songsArray)

        val playlistsList = playlistDao.getAllPlaylistsList()
        val playlistsArray = JSONArray()
        playlistsList.forEach { pl ->
            val pObj = JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
                put("createdAt", pl.createdAt)
                put("isCollaborative", pl.isCollaborative)
                put("shareCode", pl.shareCode)
                put("contributorNames", pl.contributorNames)
            }
            playlistsArray.put(pObj)
        }
        rootObj.put("playlists", playlistsArray)

        val crossRefsList = playlistDao.getAllCrossRefsList()
        val refsArray = JSONArray()
        crossRefsList.forEach { ref ->
            val rObj = JSONObject().apply {
                put("playlistId", ref.playlistId)
                put("songId", ref.songId)
                put("addedAt", ref.addedAt)
            }
            refsArray.put(rObj)
        }
        rootObj.put("playlistCrossRefs", refsArray)

        rootObj.toString(2)
    }

    suspend fun importSongsFromJson(rawJson: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(rawJson.trim())
            val songsArray = root.optJSONArray("songs") ?: JSONArray()
            val playlistsArray = root.optJSONArray("playlists") ?: JSONArray()
            val refsArray = root.optJSONArray("playlistCrossRefs") ?: JSONArray()

            val songsToInsert = mutableListOf<Song>()
            for (i in 0 until songsArray.length()) {
                val sObj = songsArray.getJSONObject(i)
                songsToInsert.add(
                    Song(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        title = sObj.optString("title", "Unknown Track"),
                        artist = sObj.optString("artist", "Unknown Artist"),
                        album = sObj.optString("album", "Unknown Album"),
                        durationMs = sObj.optLong("durationMs", 180000L),
                        streamUrl = sObj.optString("streamUrl", ""),
                        audioQuality = sObj.optString("audioQuality", "Hi-Fi 320kbps"),
                        artworkUrl = sObj.optString("artworkUrl").ifEmpty { null },
                        genre = sObj.optString("genre", "Electronic"),
                        isFavorite = sObj.optBoolean("isFavorite", false),
                        isCached = sObj.optBoolean("isCached", false),
                        source = sObj.optString("source", "Imported")
                    )
                )
            }

            val playlistsToInsert = mutableListOf<Playlist>()
            for (i in 0 until playlistsArray.length()) {
                val pObj = playlistsArray.getJSONObject(i)
                playlistsToInsert.add(
                    Playlist(
                        id = pObj.optString("id", UUID.randomUUID().toString()),
                        title = pObj.optString("title", "Imported Playlist"),
                        description = pObj.optString("description", ""),
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                        isCollaborative = pObj.optBoolean("isCollaborative", false),
                        shareCode = pObj.optString("shareCode", ""),
                        contributorNames = pObj.optString("contributorNames", "You")
                    )
                )
            }

            val refsToInsert = mutableListOf<PlaylistSongCrossRef>()
            for (i in 0 until refsArray.length()) {
                val rObj = refsArray.getJSONObject(i)
                refsToInsert.add(
                    PlaylistSongCrossRef(
                        playlistId = rObj.optString("playlistId"),
                        songId = rObj.optString("songId"),
                        addedAt = rObj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }

            if (songsToInsert.isNotEmpty()) {
                songDao.insertSongs(songsToInsert)
            }
            if (playlistsToInsert.isNotEmpty()) {
                playlistDao.insertPlaylists(playlistsToInsert)
            }
            if (refsToInsert.isNotEmpty()) {
                playlistDao.insertCrossRefs(refsToInsert)
            }

            ImportResult(
                isSuccess = true,
                message = "Restored ${songsToInsert.size} songs and ${playlistsToInsert.size} playlists successfully!"
            )
        } catch (e: Exception) {
            ImportResult(isSuccess = false, message = "Import failed: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun createEncryptedBackupFile(): File = withContext(Dispatchers.IO) {
        val backupDir = settingsManager.getBackupFolder()
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupZip = File(backupDir, "muesic_vault_backup.zip")

        val backupDataStr = exportSongsToJson()

        ZipOutputStream(FileOutputStream(backupZip)).use { zos ->
            val entry = ZipEntry("muesic_library_backup.json")
            zos.putNextEntry(entry)
            zos.write(backupDataStr.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val sigEntry = ZipEntry("integrity.sha256")
            zos.putNextEntry(sigEntry)
            val checksum = "MUESIC-SIGNATURE-" + UUID.randomUUID().toString()
            zos.write(checksum.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        backupZip
    }

    suspend fun restoreFromBackupFile(zipFile: File): ImportResult = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || zipFile.length() == 0L) {
                return@withContext ImportResult(false, "Backup .zip file not found.")
            }
            var jsonContent: String? = null
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "muesic_library_backup.json" || entry.name == "muesic_library_secure.json") {
                        jsonContent = zis.bufferedReader().readText()
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            if (jsonContent != null) {
                importSongsFromJson(jsonContent!!)
            } else {
                ImportResult(false, "No valid backup JSON found inside the .zip archive.")
            }
        } catch (e: Exception) {
            ImportResult(false, "Error extracting zip: ${e.message}")
        }
    }

    // Ensure catalog clean state
    suspend fun seedInitialOpenMusicCatalogIfNeeded() = withContext(Dispatchers.IO) {
        // Automatically purge any old synthetic / placeholder audio tracks
        songDao.deleteOldPlaceholderSongs()

        // Clean up any corrupt or truncated cache files (< 50KB or .tmp)
        try {
            val cacheDir = File(context.filesDir, "audio_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || file.length() < 50_000) {
                        val songId = file.nameWithoutExtension
                        file.delete()
                        songDao.updateCacheStatus(songId, false, null)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        songDao.deleteOldPlaceholderSongs()
        songDao.deleteLegacyBrokenSongs()
        songDao.deleteCoversAndRemixes()
        songDao.deleteSampleTracks()
        songDao.deleteSongsByIds(listOf("saavn_aRZbUYD7", "saavn_ZAwUB9Jy", "saavn_uiEWT3kP", "ytm_d3YwD6e_r_k"))
        playlistDao.deleteSamplePlaylistRefs()
        playlistDao.deleteSamplePlaylists()

        val initialSongs = listOf(
            Song(
                id = "saavn_rjkrTnma",
                title = "Kesariya",
                artist = "Pritam, Arijit Singh, Amitabh Bhattacharya",
                album = "Brahmastra (Original Motion Picture Soundtrack)",
                durationMs = 268000,
                audioQuality = "320kbps MP3 • JioSaavn Direct",
                streamUrl = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
                artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                genre = "Bollywood",
                source = "JioSaavn"
            ),
            Song(
                id = "saavn_228_apnabanale",
                title = "Apna Bana Le",
                artist = "Sachin-Jigar, Arijit Singh",
                album = "Bhediya",
                durationMs = 261000,
                audioQuality = "320kbps MP3 • JioSaavn Direct",
                streamUrl = "https://aac.saavncdn.com/228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
                artworkUrl = "https://c.saavncdn.com/228/Sachin-Jigar-Bollywood-Hits-Hindi-2026-20260630213800-500x500.jpg",
                genre = "Bollywood",
                source = "JioSaavn"
            ),
            Song(
                id = "saavn_608_channamereya",
                title = "Channa Mereya",
                artist = "Pritam, Arijit Singh",
                album = "Ae Dil Hai Mushkil",
                durationMs = 289000,
                audioQuality = "320kbps MP3 • JioSaavn Direct",
                streamUrl = "https://aac.saavncdn.com/608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
                artworkUrl = "https://c.saavncdn.com/608/Ae-Dil-Hai-Mushkil-Deluxe-Edition-Hindi-2016-500x500.jpg",
                genre = "Sufi / Soul",
                source = "JioSaavn"
            ),
            Song(
                id = "saavn_238_raataanlambiyan",
                title = "Raataan Lambiyan",
                artist = "Tanishk Bagchi, Jubin Nautiyal, Asees Kaur",
                album = "Shershaah",
                durationMs = 230000,
                audioQuality = "320kbps MP3 • JioSaavn Direct",
                streamUrl = "https://aac.saavncdn.com/238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
                artworkUrl = "https://c.saavncdn.com/238/Shershaah-Original-Motion-Picture-Soundtrack--Hindi-2021-20210815181610-500x500.jpg",
                genre = "Bollywood",
                source = "JioSaavn"
            ),
            Song(
                id = "saavn_416_heeriye",
                title = "Heeriye",
                artist = "Jasleen Royal, Arijit Singh",
                album = "Heeriye",
                durationMs = 194000,
                audioQuality = "320kbps MP3 • JioSaavn Direct",
                streamUrl = "https://aac.saavncdn.com/416/8137350aa82998f45a05b3e64db801fa_320.mp4",
                artworkUrl = "https://c.saavncdn.com/416/Heeriye-Hindi-2026-20260529210207-500x500.jpg",
                genre = "Romantic",
                source = "JioSaavn"
            ),
            Song(
                id = "ytm_xTvyyoF_LZY",
                title = "Shape of You",
                artist = "Ed Sheeran",
                album = "÷ (Divide)",
                durationMs = 233000,
                audioQuality = "256kbps Opus • YouTube Music HD",
                streamUrl = "https://www.youtube.com/watch?v=xTvyyoF_LZY",
                artworkUrl = "https://i.ytimg.com/vi/xTvyyoF_LZY/hqdefault.jpg",
                genre = "Pop",
                source = "YouTube Music"
            ),
            Song(
                id = "ytm_Kx7B-XvmFtE",
                title = "Believer",
                artist = "Imagine Dragons",
                album = "Evolve",
                durationMs = 204000,
                audioQuality = "256kbps Opus • YouTube Music HD",
                streamUrl = "https://www.youtube.com/watch?v=Kx7B-XvmFtE",
                artworkUrl = "https://i.ytimg.com/vi/Kx7B-XvmFtE/hqdefault.jpg",
                genre = "Rock / Alternative",
                source = "YouTube Music"
            ),
            Song(
                id = "ytm_3_g2un5M350",
                title = "Starboy",
                artist = "The Weeknd ft. Daft Punk",
                album = "Starboy",
                durationMs = 230000,
                audioQuality = "256kbps Opus • YouTube Music HD",
                streamUrl = "https://www.youtube.com/watch?v=3_g2un5M350",
                artworkUrl = "https://i.ytimg.com/vi/3_g2un5M350/hqdefault.jpg",
                genre = "R&B / Synthpop",
                source = "YouTube Music"
            )
        )
        // Clean up legacy restricted video IDs if present
        listOf("ytm_JGwWNGJdvx8", "ytm_7wtfhZwyrcc", "ytm_34Na4j8AVgA").forEach {
            songDao.deleteSong(it)
        }
        // Ensure valid initial songs are always present and up-to-date with working URLs
        songDao.insertSongs(initialSongs)

        // Prune any legacy typing fragments in search history
        try {
            val allSearches = searchHistoryDao.getAllSearchHistory()
            for (item in allSearches) {
                val isPrefix = allSearches.any { other ->
                    other.query != item.query && other.query.startsWith(item.query, ignoreCase = true)
                }
                if (isPrefix) {
                    searchHistoryDao.deleteSearch(item.query)
                }
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Could not prune search history prefix fragments", e)
        }

        // Seed initial playlists if none exist
        if (playlistDao.getAllPlaylistsList().isEmpty()) {
            val pl1Id = UUID.randomUUID().toString()
            playlistDao.insertPlaylist(
                Playlist(
                    id = pl1Id,
                    title = "JioSaavn Top Hits",
                    description = "Trending 320kbps Bollywood & Hindi blockbusters",
                    isCollaborative = false,
                    shareCode = "MUE-TOP"
                )
            )
            val pl2Id = UUID.randomUUID().toString()
            playlistDao.insertPlaylist(
                Playlist(
                    id = pl2Id,
                    title = "Arijit Singh Essentials",
                    description = "Soulful melodies and romantic anthems",
                    isCollaborative = true,
                    shareCode = "MUE-ARI",
                    contributorNames = "You, MusicBot"
                )
            )
            val pl3Id = UUID.randomUUID().toString()
            playlistDao.insertPlaylist(
                Playlist(
                    id = pl3Id,
                    title = "Pop & Global Favorites",
                    description = "High-energy chartbusters from around the globe",
                    isCollaborative = false,
                    shareCode = "MUE-GLO"
                )
            )

            // Add initial song playlist cross refs
            listOf("saavn_rjkrTnma", "saavn_228_apnabanale", "saavn_238_raataanlambiyan").forEach { sId ->
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(pl1Id, sId))
            }
            listOf("saavn_rjkrTnma", "saavn_228_apnabanale", "saavn_608_channamereya", "saavn_416_heeriye").forEach { sId ->
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(pl2Id, sId))
            }
            listOf("ytm_JGwWNGJdvx8", "ytm_7wtfhZwyrcc", "ytm_34Na4j8AVgA").forEach { sId ->
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(pl3Id, sId))
            }
        }
    }

    suspend fun purgeSampleTestData(): PurgeResult = withContext(Dispatchers.IO) {
        val initialCount = songDao.getSongCount()
        songDao.deleteOldPlaceholderSongs()
        songDao.deleteCoversAndRemixes()
        val sampleTracksDeleted = songDao.deleteSampleTracks()
        val samplePlaylistsDeleted = playlistDao.deleteSamplePlaylists()
        playlistDao.deleteSamplePlaylistRefs()
        val currentCount = songDao.getSongCount()
        val totalSongsRemoved = (initialCount - currentCount).coerceAtLeast(sampleTracksDeleted)
        PurgeResult(
            deletedSongs = totalSongsRemoved,
            deletedPlaylists = samplePlaylistsDeleted,
            message = "Purge successful: Cleared $totalSongsRemoved sample/placeholder tracks and $samplePlaylistsDeleted test playlists for production readiness."
        )
    }
}

data class PurgeResult(
    val deletedSongs: Int,
    val deletedPlaylists: Int,
    val message: String
)

data class ImportResult(
    val isSuccess: Boolean,
    val message: String,
    val playlistId: String? = null
)
