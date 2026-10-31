package com.example

import com.example.data.entity.SearchHistory
import com.example.data.entity.Song
import com.example.data.network.YtDlpService
import com.example.util.SongTitleCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MusicDataTest {

    @Test
    fun testSongEntityCreation() {
        val song = Song(
            id = "test-1",
            title = "Midnight Horizon",
            artist = "Neon Dreamer",
            album = "Cyber Odyssey",
            durationMs = 214000,
            audioQuality = "FLAC 24-bit / 96kHz Lossless",
            streamUrl = "https://actions.google.com/sounds/v1/science_fiction/alien_hum.ogg",
            genre = "Synthwave",
            isCached = true,
            isFavorite = true
        )

        assertEquals("test-1", song.id)
        assertEquals("Midnight Horizon", song.title)
        assertTrue(song.isCached)
        assertTrue(song.isFavorite)
    }

    @Test
    fun testJsonImportParsing() {
        val rawJson = """
            {
                "name": "Focus Zone",
                "description": "Deep flow audio",
                "tracks": [
                    {
                        "title": "Quantum Stream",
                        "artist": "Nova Pulse",
                        "durationMs": 195000,
                        "streamUrl": "https://example.com/audio.ogg",
                        "genre": "Ambient"
                    }
                ]
            }
        """.trimIndent()

        val json = JSONObject(rawJson)
        val name = json.getString("name")
        val tracksArray = json.getJSONArray("tracks")

        assertEquals("Focus Zone", name)
        assertEquals(1, tracksArray.length())

        val firstTrack = tracksArray.getJSONObject(0)
        assertEquals("Quantum Stream", firstTrack.getString("title"))
        assertEquals("Nova Pulse", firstTrack.getString("artist"))
    }

    @Test
    fun testSha256ChecksumGeneration() {
        val sampleData = "Muesic Encrypted Vault Data".toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(sampleData).joinToString("") { "%02x".format(it) }

        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun testSearchHistoryEntity() {
        val historyItem = SearchHistory(
            query = "Coldplay",
            timestamp = System.currentTimeMillis()
        )
        assertEquals("Coldplay", historyItem.query)
        assertTrue(historyItem.timestamp > 0)
    }

    @Test
    fun streamFallbackRequiresMatchingTitleAndArtist() {
        assertTrue(
            SongTitleCleaner.isStrongTrackMatch(
                candidateTitle = "Kesariya (Official Audio)",
                candidateArtist = "Arijit Singh, Pritam",
                expectedTitle = "Kesariya",
                expectedArtist = "Pritam, Arijit Singh, Amitabh Bhattacharya"
            )
        )
        assertTrue(
            SongTitleCleaner.isStrongTrackMatch(
                candidateTitle = "Kesariya",
                candidateArtist = "YouTube Creator",
                expectedTitle = "Kesariya",
                expectedArtist = "YouTube Creator"
            )
        )
        assertTrue(
            !SongTitleCleaner.isStrongTrackMatch(
                candidateTitle = "Kesari",
                candidateArtist = "Arijit Singh",
                expectedTitle = "Kesariya",
                expectedArtist = "Arijit Singh"
            )
        )
        assertTrue(
            !SongTitleCleaner.isStrongTrackMatch(
                candidateTitle = "Kesariya",
                candidateArtist = "Another Artist",
                expectedTitle = "Kesariya",
                expectedArtist = "Arijit Singh"
            )
        )
    }

    @Test
    fun ytDlpPageUrlMustUseHttpsAndCannotContainCredentials() {
        assertEquals(
            "https://example.com/audio?id=123",
            YtDlpService.validatePageUrl(" https://example.com/audio?id=123 ")
        )

        assertInvalidYtDlpUrl("http://example.com/audio")
        val credentialedUrl = "https://" + listOf("user", "secret").joinToString(":") +
            "@example.com/audio"
        assertInvalidYtDlpUrl(credentialedUrl)
        assertInvalidYtDlpUrl("not a URL")
    }

    private fun assertInvalidYtDlpUrl(url: String) {
        try {
            YtDlpService.validatePageUrl(url)
            throw AssertionError("Expected invalid yt-dlp URL: $url")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }

    @Test
    fun testDiscoverSectionsContent() {
        // 1. Trending Songs (from JioSaavn)
        val trending = com.example.data.repository.DiscoverContentProvider.getInitialTrendingSongs()
        assertTrue(trending.isNotEmpty())
        assertTrue(trending.all { it.source == "JioSaavn" })
        assertTrue(trending.all { it.streamUrl.startsWith("https://aac.saavncdn.com") })

        // 2. Categories (Romance, Workout, Pop, 90's, Chill, Best of Years)
        val categories = com.example.data.repository.DiscoverContentProvider.getInitialCategories()
        assertTrue(categories.size >= 8)
        val categoryTitles = categories.map { it.title }
        assertTrue(categoryTitles.contains("Romance"))
        assertTrue(categoryTitles.contains("Workout"))
        assertTrue(categoryTitles.contains("Pop"))
        assertTrue(categoryTitles.contains("90's Classics"))
        assertTrue(categoryTitles.contains("Chill"))
        assertTrue(categoryTitles.contains("Best of 2024"))
        assertTrue(categoryTitles.contains("Best of 2023"))
        assertTrue(categoryTitles.contains("Best of 2022"))
        assertTrue(categories.all { it.songs.isNotEmpty() })

        // 3. Top Songs (by Language, Category, Type, Artist, Country)
        for (dim in com.example.data.model.TopSongsDimension.values()) {
            val filters = com.example.data.repository.DiscoverContentProvider.getTopSongsFilters(dim)
            assertTrue(filters.isNotEmpty())
            assertTrue(filters.all { it.initialSongs.isNotEmpty() })
        }

        // 4. New Releases (Grouped by Movie)
        val movies = com.example.data.repository.DiscoverContentProvider.getInitialMovieReleases()
        assertTrue(movies.size >= 6)
        val movieNames = movies.map { it.movieName }
        assertTrue(movieNames.contains("Fighter"))
        assertTrue(movieNames.contains("Animal"))
        assertTrue(movieNames.contains("Jawan"))
        assertTrue(movieNames.contains("Stree 2"))
        assertTrue(movies.all { it.songs.isNotEmpty() })

        // 5. Recommended Artists (Songs playlist from artists)
        val artists = com.example.data.repository.DiscoverContentProvider.getInitialRecommendedArtists()
        assertTrue(artists.size >= 6)
        val artistNames = artists.map { it.name }
        assertTrue(artistNames.contains("Arijit Singh"))
        assertTrue(artistNames.contains("Anirudh Ravichander"))
        assertTrue(artistNames.contains("Taylor Swift"))
        assertTrue(artists.all { it.songs.isNotEmpty() })

        // 6. Top Mix (Songs/playlists based on genre of songs)
        val mixes = com.example.data.repository.DiscoverContentProvider.getInitialTopMixes()
        assertTrue(mixes.size >= 6)
        assertTrue(mixes.all { it.songs.isNotEmpty() })
    }

    @Test
    fun testLrcParser() {
        val lrc = """
            [00:04.20]First line of lyrics
            [00:10.50]Second line of lyrics
        """.trimIndent()
        val parsed = com.example.data.network.LyricsService.parseLrcString(lrc)
        assertEquals(2, parsed.size)
        assertEquals("First line of lyrics", parsed[0].text)
        assertEquals(4200L, parsed[0].timestampMs)
        assertEquals("Second line of lyrics", parsed[1].text)
        assertEquals(10500L, parsed[1].timestampMs)
    }

    @Test
    fun testJioSaavnSearchAndStreamUrl() {
        kotlinx.coroutines.runBlocking {
            val queries = listOf("Kesariya", "Pasoori", "Heeriye", "Maan Meri Jaan", "Pathaan", "Deva Deva", "Apna Bana Le", "Channa Mereya", "Raataan Lambiyan", "Tu Maan Meri Jaan")
            val working = mutableListOf<com.example.data.entity.Song>()
            for (q in queries) {
                val songs = com.example.data.network.JioSaavnService.search(q)
                for (s in songs.take(3)) {
                    if (s.streamUrl.isNotBlank() && s.artworkUrl != null) {
                        try {
                            val conn = java.net.URL(s.streamUrl).openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "HEAD"
                            conn.connectTimeout = 3000
                            conn.readTimeout = 3000
                            val code = conn.responseCode
                            if (code == 200) {
                                println("WORKING_STREAM: ${s.title} by ${s.artist} -> ${s.streamUrl}, art=${s.artworkUrl}")
                                working.add(s)
                                break
                            } else {
                                println("FAILED_STREAM ($code): ${s.title} -> ${s.streamUrl}")
                            }
                        } catch (e: Exception) {
                            println("ERR_STREAM: ${s.title} -> ${e.message}")
                        }
                    }
                }
            }
            println("TOTAL WORKING: ${working.size}")
        }
    }
}
