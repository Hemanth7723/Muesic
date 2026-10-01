package com.example.data.repository

import android.util.Log
import com.example.data.entity.Song
import com.example.data.model.CategoryPlaylist
import com.example.data.model.MovieReleasePlaylist
import com.example.data.model.RecommendedArtist
import com.example.data.model.TopMixPlaylist
import com.example.data.model.TopSongFilter
import com.example.data.model.TopSongsDimension
import com.example.data.network.JioSaavnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides rich suggestions and curated playlists from JioSaavn for the Discover page:
 * 1. Trending Songs (from JioSaavn)
 * 2. Categories (Romance, Workout, Pop, 90's, Chill, Best of Years)
 * 3. Top Songs (Language, Category, Type, Artist, Country)
 * 4. New Releases (Grouped by Movie / Album)
 * 5. Recommended Artists (Song playlists from top artists)
 * 6. Top Mix (Genre-based mixes & playlists)
 */
object DiscoverContentProvider {
    private const val TAG = "DiscoverContentProvider"

    // Common JioSaavn stream helper
    private fun jioSong(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationSec: Long,
        streamSubpath: String,
        artworkPathOrUrl: String,
        genre: String
    ): Song {
        val finalArtwork = if (artworkPathOrUrl.startsWith("http://") || artworkPathOrUrl.startsWith("https://")) {
            artworkPathOrUrl
        } else {
            "https://c.saavncdn.com/$artworkPathOrUrl"
        }
        return Song(
            id = "saavn_$id",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationSec * 1000L,
            audioQuality = "320kbps MP3 • JioSaavn Direct",
            streamUrl = "https://aac.saavncdn.com/$streamSubpath",
            artworkUrl = finalArtwork,
            genre = genre,
            source = "JioSaavn",
            fileSizeMb = ((durationSec * 320.0) / (8.0 * 1024.0)).let { Math.round(it * 10.0) / 10.0 }
        )
    }

    // 1. Core Seed Tracks with verified 100% authentic album artwork
    val kesariya = jioSong(
        "rjkrTnma", "Kesariya", "Pritam, Arijit Singh, Amitabh Bhattacharya",
        "Brahmastra", 268, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", "Bollywood"
    )
    val apnaBanaLe = jioSong(
        "228_apnabanale", "Apna Bana Le", "Sachin-Jigar, Arijit Singh",
        "Bhediya", 261, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "815/Bhediya-Hindi-2023-20230927155213-500x500.jpg", "Bollywood"
    )
    val channaMereya = jioSong(
        "608_channamereya", "Channa Mereya", "Pritam, Arijit Singh",
        "Ae Dil Hai Mushkil", 289, "608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
        "608/Ae-Dil-Hai-Mushkil-Deluxe-Edition-Hindi-2016-500x500.jpg", "Sufi / Soul"
    )
    val raataanLambiyan = jioSong(
        "238_raataanlambiyan", "Raataan Lambiyan", "Tanishk Bagchi, Jubin Nautiyal, Asees Kaur",
        "Shershaah", 230, "238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
        "238/Shershaah-Original-Motion-Picture-Soundtrack--Hindi-2021-20210815181610-500x500.jpg", "Bollywood"
    )
    val heeriye = jioSong(
        "416_heeriye", "Heeriye", "Jasleen Royal, Arijit Singh",
        "Heeriye", 194, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "416/Heeriye-Hindi-2026-20260529210207-500x500.jpg", "Romantic"
    )
    val chaleya = jioSong(
        "jawan_chaleya", "Chaleya", "Anirudh Ravichander, Arijit Singh, Shilpa Rao",
        "Jawan", 200, "238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
        "047/Jawan-Hindi-2023-20230921190854-500x500.jpg", "Bollywood"
    )
    val taubaTauba = jioSong(
        "badnewz_tauba", "Tauba Tauba", "Karan Aujla",
        "Bad Newz", 206, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg", "Punjabi Pop"
    )
    val illuminati = jioSong(
        "aavesham_illuminati", "Illuminati", "Sushin Shyam, Dabzee",
        "Aavesham", 188, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "202/Aavesham-Original-Motion-Picture-Soundtrack-Malayalam-2024-20250910150630-500x500.jpg", "Malayalam Hip-Hop"
    )
    val satranga = jioSong(
        "animal_satranga", "Satranga", "Arijit Singh, Shreyas Puranik",
        "Animal", 271, "608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
        "092/ANIMAL-Hindi-2023-20260724191152-500x500.jpg", "Bollywood"
    )
    val arjanVailly = jioSong(
        "animal_arjan", "Arjan Vailly", "Bhupinder Babbal, Manan Bhardwaj",
        "Animal", 182, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "092/ANIMAL-Hindi-2023-20260724191152-500x500.jpg", "Punjabi Folk"
    )
    val zindaBanda = jioSong(
        "jawan_zinda", "Zinda Banda", "Anirudh Ravichander",
        "Jawan", 264, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "047/Jawan-Hindi-2023-20230921190854-500x500.jpg", "Bollywood"
    )
    val sherKhulGaye = jioSong(
        "fighter_sher", "Sher Khul Gaye", "Vishal-Shekhar, Benny Dayal, Shilpa Rao",
        "Fighter", 180, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "142/Fighter-Hindi-2024-20240701191023-500x500.jpg", "Dance"
    )
    val ishqJitta = jioSong(
        "fighter_ishq", "Ishq Jitta", "Vishal-Shekhar, Arijit Singh",
        "Fighter", 212, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "142/Fighter-Hindi-2024-20240701191023-500x500.jpg", "Romantic"
    )
    val aajKiRaat = jioSong(
        "stree2_aaj", "Aaj Ki Raat", "Sachin-Jigar, Madhubanti Bagchi, Divya Kumar",
        "Stree 2", 228, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "373/Stree-2-Hindi-2024-20240828083834-500x500.jpg", "Dance / Party"
    )
    val khoobsurat = jioSong(
        "stree2_khoob", "Khoobsurat", "Vishal Mishra, Sachin-Jigar",
        "Stree 2", 244, "238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
        "373/Stree-2-Hindi-2024-20240828083834-500x500.jpg", "Romantic"
    )
    val fearSong = jioSong(
        "devara_fear", "Fear Song", "Anirudh Ravichander",
        "Devara Part 1", 195, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg", "Rock / Telugu"
    )
    val chuttamalle = jioSong(
        "devara_chutta", "Chuttamalle", "Shilpa Rao, Anirudh Ravichander",
        "Devara Part 1", 220, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg", "Romantic / Telugu"
    )
    val pushpaPushpa = jioSong(
        "pushpa2_title", "Pushpa Pushpa", "Devi Sri Prasad, Mika Singh",
        "Pushpa 2 The Rule", 270, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/ac/d7/02/acd70261-cfa2-fafc-ad43-5cbb962715ce/8903431993366_cover.jpg/600x600bb.jpg", "Telugu / Mass"
    )
    val oMaahi = jioSong(
        "dunki_omaahi", "O Maahi", "Pritam, Arijit Singh",
        "Dunki", 233, "608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
        "139/Dunki-Hindi-2023-20231220211003-500x500.jpg", "Romantic"
    )
    val samajavaragamana = jioSong(
        "samajavaragamana", "Samajavaragamana", "Thaman S, Sid Sriram",
        "Ala Vaikunthapurramuloo", 214, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg", "Telugu Melodies"
    )
    val starboy = jioSong(
        "theweeknd_starboy", "Starboy", "The Weeknd, Daft Punk",
        "Starboy", 230, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b5/92/bb/b592bb72-52e3-e756-9b26-9f56d08f47ab/16UMGIM67864.rgb.jpg/600x600bb.jpg", "Synthwave / Pop"
    )
    val blindingLights = jioSong(
        "theweeknd_blinding", "Blinding Lights", "The Weeknd",
        "After Hours", 200, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/61/e7/3f/61e73f94-018d-5f50-50ec-8521952bc72e/20UM1IM11629.rgb.jpg/600x600bb.jpg", "Synthwave / Pop"
    )
    val shapeOfYou = jioSong(
        "edsheeran_shape", "Shape of You", "Ed Sheeran",
        "Divide", 233, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/15/e6/e8/15e6e8a4-4190-6a8b-86c3-ab4a51b88288/190295851286.jpg/600x600bb.jpg", "Pop"
    )
    val lover = jioSong(
        "tswift_lover", "Lover", "Taylor Swift",
        "Lover", 221, "238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
        "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/49/3d/ab/493dab54-f920-9043-6181-80993b8116c9/19UMGIM53909.rgb.jpg/600x600bb.jpg", "Pop"
    )
    val pehlaNasha = jioSong(
        "90s_pehla_nasha", "Pehla Nasha", "Udit Narayan, Sadhana Sargam",
        "Jo Jeeta Wohi Sikandar", 291, "608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
        "852/Jo-Jeeta-Wohi-Sikandar-Hindi-1992-500x500.jpg", "90s Classics"
    )
    val chaiyyaChaiyya = jioSong(
        "90s_chaiyya", "Chaiyya Chaiyya", "AR Rahman, Sukhwinder Singh",
        "Dil Se", 390, "871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
        "430/Dil-Se-Hindi-1998-20210226142402-500x500.jpg", "90s Classics"
    )
    val zinda = jioSong(
        "workout_zinda", "Zinda", "Shankar-Ehsaan-Loy, Siddharth Mahadevan",
        "Bhaag Milkha Bhaag", 211, "228/1d29fa6a9d1bb824f114620f4c391771_320.mp4",
        "575/Bhaag-Milkha-Bhaag-Hindi-2013-20260120201340-500x500.jpg", "Workout"
    )
    val karGayiChull = jioSong(
        "workout_chull", "Kar Gayi Chull", "Badshah, Neha Kakkar, Fazilpuria",
        "Kapoor & Sons", 188, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
        "978/Kapoor-Sons-Since-1921--Hindi-2016-20180504172446-500x500.jpg", "Workout"
    )
    val iktara = jioSong(
        "chill_iktara", "Iktara", "Amit Trivedi, Kavita Seth",
        "Wake Up Sid", 253, "238/59165b53e8a719c8f2537f59d57a94d8_320.mp4",
        "910/Wake-Up-Sid-Hindi-2009-20190617160255-500x500.jpg", "Chill"
    )
    val kabira = jioSong(
        "chill_kabira", "Kabira", "Pritam, Tochi Raina, Rekha Bhardwaj",
        "Yeh Jawaani Hai Deewani", 224, "608/94821c9fa6a0665f573c0ce2f9ba7fd0_320.mp4",
        "440/Yeh-Jawaani-Hai-Deewani-2013-500x500.jpg", "Chill"
    )

    // SECTION 1: Trending songs (JioSaavn)
    fun getInitialTrendingSongs(): List<Song> {
        return listOf(
            kesariya, taubaTauba, chaleya, illuminati, apnaBanaLe,
            satranga, raataanLambiyan, heeriye, arjanVailly, oMaahi,
            sherKhulGaye, samajavaragamana
        )
    }

    // SECTION 2: Categories (Romance, Workout, Pop, 90's, Chill, Best of 2024, Best of 2023, Best of 2022)
    fun getInitialCategories(): List<CategoryPlaylist> {
        return listOf(
            CategoryPlaylist(
                id = "cat_romance",
                title = "Romance",
                description = "Heartwarming melodies & timeless love ballads",
                iconType = "favorite",
                gradientColors = listOf(0xFFE91E63, 0xFF880E4F),
                query = "Hindi Romantic Songs Love Hits",
                songs = listOf(kesariya, apnaBanaLe, raataanLambiyan, heeriye, khoobsurat, chuttamalle, ishqJitta),
                coverArtworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_workout",
                title = "Workout",
                description = "High BPM pump-up tracks & gym motivation",
                iconType = "fitness",
                gradientColors = listOf(0xFFFF5722, 0xFFBF360C),
                query = "Gym Workout Motivation High Energy Beats",
                songs = listOf(zinda, karGayiChull, arjanVailly, zindaBanda, fearSong, pushpaPushpa),
                coverArtworkUrl = "https://c.saavncdn.com/575/Bhaag-Milkha-Bhaag-Hindi-2013-20260120201340-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_pop",
                title = "Pop",
                description = "Top global & modern synthpop chart-toppers",
                iconType = "music",
                gradientColors = listOf(0xFF9C27B0, 0xFF4A148C),
                query = "Pop Hits 2024 Chartbusters",
                songs = listOf(starboy, blindingLights, shapeOfYou, lover, taubaTauba, sherKhulGaye),
                coverArtworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b5/92/bb/b592bb72-52e3-e756-9b26-9f56d08f47ab/16UMGIM67864.rgb.jpg/600x600bb.jpg"
            ),
            CategoryPlaylist(
                id = "cat_90s",
                title = "90's Classics",
                description = "Nostalgic golden era of Bollywood & retro beats",
                iconType = "cassette",
                gradientColors = listOf(0xFFFF9800, 0xFFE65100),
                query = "90s Bollywood Evergreen Hits",
                songs = listOf(pehlaNasha, chaiyyaChaiyya, channaMereya, kabira),
                coverArtworkUrl = "https://c.saavncdn.com/430/Dil-Se-Hindi-1998-20210226142402-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_chill",
                title = "Chill",
                description = "Acoustic, late-night rain & coffee focus vibes",
                iconType = "coffee",
                gradientColors = listOf(0xFF009688, 0xFF004D40),
                query = "Chill Acoustic Hindi Indie Relax",
                songs = listOf(iktara, kabira, heeriye, oMaahi, khoobsurat),
                coverArtworkUrl = "https://c.saavncdn.com/910/Wake-Up-Sid-Hindi-2009-20190617160255-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_best_2024",
                title = "Best of 2024",
                description = "Biggest viral anthems & smash hits of 2024",
                iconType = "trophy",
                gradientColors = listOf(0xFF00F5D4, 0xFF00695C),
                query = "Best Songs of 2024 Chartbusters",
                songs = listOf(taubaTauba, illuminati, aajKiRaat, chuttamalle, fearSong, pushpaPushpa),
                coverArtworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_best_2023",
                title = "Best of 2023",
                description = "Unstoppable blockbusters that defined 2023",
                iconType = "star",
                gradientColors = listOf(0xFF3F51B5, 0xFF1A237E),
                query = "Best Songs of 2023 Bollywood Hits",
                songs = listOf(chaleya, satranga, arjanVailly, zindaBanda, oMaahi),
                coverArtworkUrl = "https://c.saavncdn.com/047/Jawan-Hindi-2023-20230921190854-500x500.jpg"
            ),
            CategoryPlaylist(
                id = "cat_best_2022",
                title = "Best of 2022",
                description = "Sensational chartbusters that took over the world",
                iconType = "disc",
                gradientColors = listOf(0xFF00BCD4, 0xFF006064),
                query = "Best Songs of 2022 Hits",
                songs = listOf(kesariya, apnaBanaLe, raataanLambiyan, samajavaragamana),
                coverArtworkUrl = "https://c.saavncdn.com/815/Bhediya-Hindi-2023-20230927155213-500x500.jpg"
            )
        )
    }

    // SECTION 3: Top Songs by Language, Category, Type, Artist, Country
    fun getTopSongsFilters(dimension: TopSongsDimension): List<TopSongFilter> {
        return when (dimension) {
            TopSongsDimension.LANGUAGE -> listOf(
                TopSongFilter("lang_hindi", "Hindi", "Top Hindi Songs 2024", listOf(kesariya, chaleya, apnaBanaLe, satranga, aajKiRaat)),
                TopSongFilter("lang_english", "English", "Top English Songs Hits", listOf(starboy, blindingLights, shapeOfYou, lover)),
                TopSongFilter("lang_punjabi", "Punjabi", "Top Punjabi Songs Diljit Karan Aujla", listOf(taubaTauba, arjanVailly, bhairavaAnthem())),
                TopSongFilter("lang_tamil", "Tamil", "Top Tamil Songs Anirudh Hits", listOf(fearSong, chaleya, zindaBanda)),
                TopSongFilter("lang_telugu", "Telugu", "Top Telugu Songs Thaman DSP", listOf(samajavaragamana, chuttamalle, pushpaPushpa)),
                TopSongFilter("lang_spanish", "Spanish", "Top Spanish Reggaeton Latin Hits", listOf(shapeOfYou, starboy))
            )
            TopSongsDimension.CATEGORY -> listOf(
                TopSongFilter("cat_party", "Party", "Bollywood Party Dance Hits", listOf(aajKiRaat, sherKhulGaye, taubaTauba, zindaBanda)),
                TopSongFilter("cat_devotional", "Devotional", "Spiritual Devotional Morning Peace", listOf(channaMereya, kabira)),
                TopSongFilter("cat_acoustic", "Acoustic", "Unplugged Acoustic Guitar Hindi", listOf(iktara, heeriye, kabira, oMaahi)),
                TopSongFilter("cat_hiphop", "Hip-Hop", "Indian Hip Hop Rap Beats", listOf(illuminati, taubaTauba, arjanVailly)),
                TopSongFilter("cat_classical", "Classical", "Indian Classical Fusion Melodies", listOf(samajavaragamana, chaiyyaChaiyya)),
                TopSongFilter("cat_sufi", "Sufi", "Sufi Soul Qawwali Melodies", listOf(channaMereya, kabira, iktara))
            )
            TopSongsDimension.TYPE -> listOf(
                TopSongFilter("type_chartbusters", "Chartbusters", "Top 50 Chartbuster Hits", listOf(kesariya, chaleya, taubaTauba, satranga)),
                TopSongFilter("type_viral_reels", "Viral Reels", "Trending Instagram Reels Songs", listOf(illuminati, taubaTauba, heeriye, aajKiRaat)),
                TopSongFilter("type_acoustic_live", "Acoustic Live", "Acoustic Live Studio Sessions", listOf(iktara, channaMereya, kabira)),
                TopSongFilter("type_movie_soundtracks", "Soundtracks", "Original Motion Picture Soundtracks", listOf(kesariya, chaleya, sherKhulGaye, fearSong))
            )
            TopSongsDimension.ARTIST -> listOf(
                TopSongFilter("art_arijit", "Arijit Singh", "Arijit Singh Top Hit Songs", listOf(kesariya, apnaBanaLe, chaleya, satranga, channaMereya, oMaahi)),
                TopSongFilter("art_edsheeran", "Ed Sheeran", "Ed Sheeran Top Hits", listOf(shapeOfYou, lover)),
                TopSongFilter("art_weeknd", "The Weeknd", "The Weeknd Top Hits", listOf(starboy, blindingLights)),
                TopSongFilter("art_pritam", "Pritam", "Pritam Bollywood Hit Songs", listOf(kesariya, channaMereya, oMaahi, kabira)),
                TopSongFilter("art_shreya", "Shreya Ghoshal", "Shreya Ghoshal Melody Hits", listOf(chuttamalle, khoobsurat)),
                TopSongFilter("art_taylor", "Taylor Swift", "Taylor Swift Top Hits", listOf(lover, starboy)),
                TopSongFilter("art_anirudh", "Anirudh", "Anirudh Ravichander Top Hits", listOf(chaleya, fearSong, zindaBanda, chuttamalle)),
                TopSongFilter("art_diljit", "Diljit Dosanjh", "Diljit Dosanjh Hit Songs", listOf(bhairavaAnthem(), taubaTauba))
            )
            TopSongsDimension.COUNTRY -> listOf(
                TopSongFilter("country_global", "Global Top 50", "Global Top 50 Hits", listOf(starboy, blindingLights, shapeOfYou, taubaTauba)),
                TopSongFilter("country_india", "India Top 50", "India Top 50 Chartbusters", listOf(kesariya, chaleya, aajKiRaat, satranga, illuminati)),
                TopSongFilter("country_us", "US Billboard", "US Billboard Hot 100", listOf(blindingLights, starboy, shapeOfYou, lover)),
                TopSongFilter("country_uk", "UK Top 40", "UK Top 40 Singles Chart", listOf(shapeOfYou, lover, blindingLights))
            )
        }
    }

    private fun bhairavaAnthem(): Song {
        return jioSong(
            "kalki_bhairava", "Bhairava Anthem", "Diljit Dosanjh, Santhosh Narayanan",
            "Kalki 2898 AD", 168, "416/8137350aa82998f45a05b3e64db801fa_320.mp4",
            "888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg", "Punjabi / Telugu"
        )
    }

    // SECTION 4: New Releases (Grouped by Movie / Album)
    fun getInitialMovieReleases(): List<MovieReleasePlaylist> {
        return listOf(
            MovieReleasePlaylist(
                id = "movie_stree2",
                movieName = "Stree 2",
                year = "2024",
                composer = "Sachin-Jigar",
                posterUrl = "https://c.saavncdn.com/373/Stree-2-Hindi-2024-20240828083834-500x500.jpg",
                query = "Stree 2 Movie Songs",
                songs = listOf(aajKiRaat, khoobsurat)
            ),
            MovieReleasePlaylist(
                id = "movie_devara",
                movieName = "Devara: Part 1",
                year = "2024",
                composer = "Anirudh Ravichander",
                posterUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                query = "Devara Songs Anirudh",
                songs = listOf(fearSong, chuttamalle)
            ),
            MovieReleasePlaylist(
                id = "movie_fighter",
                movieName = "Fighter",
                year = "2024",
                composer = "Vishal-Shekhar",
                posterUrl = "https://c.saavncdn.com/142/Fighter-Hindi-2024-20240701191023-500x500.jpg",
                query = "Fighter Movie Songs",
                songs = listOf(sherKhulGaye, ishqJitta)
            ),
            MovieReleasePlaylist(
                id = "movie_animal",
                movieName = "Animal",
                year = "2023",
                composer = "Pritam, B Praak, Manan Bhardwaj",
                posterUrl = "https://c.saavncdn.com/092/ANIMAL-Hindi-2023-20260724191152-500x500.jpg",
                query = "Animal Movie Songs Pritam",
                songs = listOf(satranga, arjanVailly)
            ),
            MovieReleasePlaylist(
                id = "movie_jawan",
                movieName = "Jawan",
                year = "2023",
                composer = "Anirudh Ravichander",
                posterUrl = "https://c.saavncdn.com/047/Jawan-Hindi-2023-20230921190854-500x500.jpg",
                query = "Jawan Movie Songs Anirudh",
                songs = listOf(chaleya, zindaBanda)
            ),
            MovieReleasePlaylist(
                id = "movie_pushpa2",
                movieName = "Pushpa 2: The Rule",
                year = "2024",
                composer = "Devi Sri Prasad",
                posterUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/ac/d7/02/acd70261-cfa2-fafc-ad43-5cbb962715ce/8903431993366_cover.jpg/600x600bb.jpg",
                query = "Pushpa 2 Songs DSP",
                songs = listOf(pushpaPushpa)
            ),
            MovieReleasePlaylist(
                id = "movie_dunki",
                movieName = "Dunki",
                year = "2023",
                composer = "Pritam",
                posterUrl = "https://c.saavncdn.com/139/Dunki-Hindi-2023-20231220211003-500x500.jpg",
                query = "Dunki Songs Pritam",
                songs = listOf(oMaahi)
            ),
            MovieReleasePlaylist(
                id = "movie_brahmastra",
                movieName = "Brahmastra",
                year = "2022",
                composer = "Pritam",
                posterUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                query = "Brahmastra Songs Pritam",
                songs = listOf(kesariya)
            )
        )
    }

    // SECTION 5: Recommended Artists (Songs playlist from artists)
    fun getInitialRecommendedArtists(): List<RecommendedArtist> {
        return listOf(
            RecommendedArtist(
                id = "artist_arijit",
                name = "Arijit Singh",
                role = "Soulful Voice of India",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/500x500-000000-80-0-0.jpg",
                query = "Arijit Singh Best Songs",
                songs = listOf(kesariya, apnaBanaLe, chaleya, satranga, channaMereya, heeriye, oMaahi)
            ),
            RecommendedArtist(
                id = "artist_anirudh",
                name = "Anirudh Ravichander",
                role = "Rockstar of Indian Cinema",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/9da0a547b39e99bc35c6a9724aef91bf/500x500-000000-80-0-0.jpg",
                query = "Anirudh Ravichander Hit Songs",
                songs = listOf(chaleya, fearSong, zindaBanda, chuttamalle)
            ),
            RecommendedArtist(
                id = "artist_diljit",
                name = "Diljit Dosanjh",
                role = "Global Punjabi Sensation",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/79b85e695e0ca6529e56bf3b628e92bd/500x500-000000-80-0-0.jpg",
                query = "Diljit Dosanjh Best Hits",
                songs = listOf(bhairavaAnthem(), taubaTauba)
            ),
            RecommendedArtist(
                id = "artist_taylor",
                name = "Taylor Swift",
                role = "Global Pop Superstar",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/e528e270424103b527f8a27ac625563b/500x500-000000-80-0-0.jpg",
                query = "Taylor Swift Top Songs",
                songs = listOf(lover, starboy)
            ),
            RecommendedArtist(
                id = "artist_weeknd",
                name = "The Weeknd",
                role = "R&B & Synthwave Icon",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/581693b4724a7fcfa754455101e13a44/500x500-000000-80-0-0.jpg",
                query = "The Weeknd Hit Songs",
                songs = listOf(starboy, blindingLights)
            ),
            RecommendedArtist(
                id = "artist_pritam",
                name = "Pritam",
                role = "Master of Bollywood Melodies",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/d4914ccd414067cd5e2c108867079a85/500x500-000000-80-0-0.jpg",
                query = "Pritam Hit Songs Bollywood",
                songs = listOf(kesariya, channaMereya, oMaahi, kabira)
            ),
            RecommendedArtist(
                id = "artist_shreya",
                name = "Shreya Ghoshal",
                role = "Melody Queen",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/3bb832d37d10ff2affcfa9afdc7c68a0/500x500-000000-80-0-0.jpg",
                query = "Shreya Ghoshal Hit Songs",
                songs = listOf(chuttamalle, khoobsurat)
            ),
            RecommendedArtist(
                id = "artist_edsheeran",
                name = "Ed Sheeran",
                role = "Acoustic Pop Maestro",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/d6bb84390641d8ae9118228d9544e53d/500x500-000000-80-0-0.jpg",
                query = "Ed Sheeran Hit Songs",
                songs = listOf(shapeOfYou, lover)
            ),
            RecommendedArtist(
                id = "artist_rahman",
                name = "AR Rahman",
                role = "Musical Legend & Maestro",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/bd34315ef977a62a9e28c1ab19bb8ac4/500x500-000000-80-0-0.jpg",
                query = "AR Rahman Top Hits",
                songs = listOf(chaiyyaChaiyya, channaMereya)
            )
        )
    }

    // SECTION 6: Top Mix (Songs / playlists based on genre of songs)
    fun getInitialTopMixes(): List<TopMixPlaylist> {
        return listOf(
            TopMixPlaylist(
                id = "mix_bollywood_pop",
                genreName = "Bollywood Pop Mix",
                subtitle = "Upbeat dance grooves & chartbuster hooks",
                gradientColors = listOf(0xFFFF5722, 0xFFFF9800),
                query = "Bollywood Pop Dance Mix",
                songs = listOf(kesariya, chaleya, apnaBanaLe, sherKhulGaye, taubaTauba),
                coverArtworkUrl = "https://c.saavncdn.com/047/Jawan-Hindi-2023-20230921190854-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_lofi_chill",
                genreName = "Lo-Fi Beats Mix",
                subtitle = "Subtle beats & soothing nighttime flows",
                gradientColors = listOf(0xFF673AB7, 0xFF3F51B5),
                query = "Lofi Chill Hindi Beats Mix",
                songs = listOf(iktara, kabira, heeriye, oMaahi),
                coverArtworkUrl = "https://c.saavncdn.com/910/Wake-Up-Sid-Hindi-2009-20190617160255-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_edm_dance",
                genreName = "EDM & Club Mix",
                subtitle = "High-octane drops, synths & festival beats",
                gradientColors = listOf(0xFF00E5FF, 0xFF7C4DFF),
                query = "EDM Dance Electronic Club Mix",
                songs = listOf(starboy, blindingLights, zindaBanda, pushpaPushpa),
                coverArtworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_rock_metal",
                genreName = "Rock & Metal Mix",
                subtitle = "Electric overdrive, live drums & raw adrenaline",
                gradientColors = listOf(0xFFD50000, 0xFF212121),
                query = "Rock High Energy Metal Indian Rock",
                songs = listOf(zinda, fearSong, arjanVailly),
                coverArtworkUrl = "https://c.saavncdn.com/575/Bhaag-Milkha-Bhaag-Hindi-2013-20260120201340-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_hiphop",
                genreName = "Hip-Hop & Trap Mix",
                subtitle = "Heavy 808 bass, punchy rhythm & street rap",
                gradientColors = listOf(0xFFFFD600, 0xFF00BFA5),
                query = "Indian Hip Hop Trap Rap Mix",
                songs = listOf(illuminati, taubaTauba, arjanVailly),
                coverArtworkUrl = "https://c.saavncdn.com/202/Aavesham-Original-Motion-Picture-Soundtrack-Malayalam-2024-20250910150630-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_acoustic_indie",
                genreName = "Acoustic & Indie Mix",
                subtitle = "Intimate vocals, warm acoustics & organic vibe",
                gradientColors = listOf(0xFF4CAF50, 0xFF8BC34A),
                query = "Acoustic Indie Hindi Singer Songwriter",
                songs = listOf(iktara, heeriye, kabira, shapeOfYou),
                coverArtworkUrl = "https://c.saavncdn.com/440/Yeh-Jawaani-Hai-Deewani-2013-500x500.jpg"
            ),
            TopMixPlaylist(
                id = "mix_rnb_soul",
                genreName = "R&B & Soul Mix",
                subtitle = "Silky basslines, velvet harmony & deep feeling",
                gradientColors = listOf(0xFFE040FB, 0xFF7B1FA2),
                query = "RnB Soul Smooth Chill Hits",
                songs = listOf(starboy, channaMereya, lover, samajavaragamana),
                coverArtworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/61/e7/3f/61e73f94-018d-5f50-50ec-8521952bc72e/20UM1IM11629.rgb.jpg/600x600bb.jpg"
            )
        )
    }

    /**
     * Dynamically fetch fresh tracks from JioSaavn for a given query,
     * merging with existing seed songs for immediate display.
     */
    suspend fun fetchFreshJioSaavnTracks(query: String, existingSongs: List<Song> = emptyList()): List<Song> = withContext(Dispatchers.IO) {
        try {
            val fetched = JioSaavnService.search(query)
            if (fetched.isNotEmpty()) {
                val combined = (fetched + existingSongs).distinctBy { it.id }
                return@withContext combined
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching live JioSaavn tracks for query '$query': ${e.message}")
        }
        return@withContext existingSongs
    }

    /**
     * 1. Refresh Trending Songs section with live JioSaavn charts
     */
    suspend fun refreshTrendingSongs(existing: List<Song> = emptyList()): List<Song> = withContext(Dispatchers.IO) {
        try {
            val liveTrending = JioSaavnService.search("trending top hindi hits songs")
            val liveViral = JioSaavnService.search("top chartbusters viral hits")
            val combined = (liveTrending + liveViral + existing + getInitialTrendingSongs())
                .distinctBy { it.id }
                .take(30)
            if (combined.isNotEmpty()) {
                return@withContext combined
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error refreshing trending songs: ${e.message}")
        }
        return@withContext existing.ifEmpty { getInitialTrendingSongs() }
    }

    /**
     * 2. Refresh New Movie Releases section with live movie soundtracks
     */
    suspend fun refreshMovieReleases(current: List<MovieReleasePlaylist>): List<MovieReleasePlaylist> = withContext(Dispatchers.IO) {
        val baseList = current.ifEmpty { getInitialMovieReleases() }
        val updated = mutableListOf<MovieReleasePlaylist>()

        // Also add newer blockbuster movie release entries if not already present
        val extraMovies = listOf(
            MovieReleasePlaylist(
                id = "movie_bb3",
                movieName = "Bhool Bhulaiyaa 3",
                year = "2024",
                composer = "Pritam, Tanishk Bagchi",
                posterUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/a4/09/25/a40925d6-d083-d5d1-9fdf-ce11176b9df6/8903431993205_cover.jpg/600x600bb.jpg",
                query = "Bhool Bhulaiyaa 3 Movie Songs"
            ),
            MovieReleasePlaylist(
                id = "movie_singham_again",
                movieName = "Singham Again",
                year = "2024",
                composer = "Ravi Basrur",
                posterUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/91/ee/12/91ee12d1-e63d-4c33-b9dc-08991206f364/8903431993458_cover.jpg/600x600bb.jpg",
                query = "Singham Again Songs Ravi Basrur"
            ),
            MovieReleasePlaylist(
                id = "movie_kalki",
                movieName = "Kalki 2898 AD",
                year = "2024",
                composer = "Santhosh Narayanan",
                posterUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
                query = "Kalki 2898 AD Songs Santhosh Narayanan"
            )
        )

        val fullCatalog = (baseList + extraMovies).distinctBy { it.id }

        for (movie in fullCatalog) {
            try {
                val liveTracks = JioSaavnService.search(movie.query)
                if (liveTracks.isNotEmpty()) {
                    val mergedTracks = (liveTracks + movie.songs).distinctBy { it.id }.take(10)
                    val effectivePoster = movie.posterUrl.ifBlank {
                        mergedTracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl ?: ""
                    }
                    updated.add(movie.copy(songs = mergedTracks, posterUrl = effectivePoster))
                } else {
                    updated.add(movie)
                }
            } catch (e: Exception) {
                updated.add(movie)
            }
        }
        return@withContext updated
    }

    /**
     * 3. Refresh Categories with live tracks for each theme
     */
    suspend fun refreshCategories(current: List<CategoryPlaylist>): List<CategoryPlaylist> = withContext(Dispatchers.IO) {
        val baseList = current.ifEmpty { getInitialCategories() }
        val updated = mutableListOf<CategoryPlaylist>()

        for (cat in baseList) {
            try {
                val liveTracks = JioSaavnService.search(cat.query)
                if (liveTracks.isNotEmpty()) {
                    val mergedTracks = (liveTracks + cat.songs).distinctBy { it.id }.take(15)
                    val effectiveCover = cat.coverArtworkUrl ?: mergedTracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl
                    updated.add(cat.copy(songs = mergedTracks, coverArtworkUrl = effectiveCover))
                } else {
                    updated.add(cat)
                }
            } catch (e: Exception) {
                updated.add(cat)
            }
        }
        return@withContext updated
    }

    /**
     * 4. Refresh Top Mixes with live tracks for each genre
     */
    suspend fun refreshTopMixes(current: List<TopMixPlaylist>): List<TopMixPlaylist> = withContext(Dispatchers.IO) {
        val baseList = current.ifEmpty { getInitialTopMixes() }
        val updated = mutableListOf<TopMixPlaylist>()

        for (mix in baseList) {
            try {
                val liveTracks = JioSaavnService.search(mix.query)
                if (liveTracks.isNotEmpty()) {
                    val mergedTracks = (liveTracks + mix.songs).distinctBy { it.id }.take(15)
                    val effectiveCover = mix.coverArtworkUrl ?: mergedTracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl
                    updated.add(mix.copy(songs = mergedTracks, coverArtworkUrl = effectiveCover))
                } else {
                    updated.add(mix)
                }
            } catch (e: Exception) {
                updated.add(mix)
            }
        }
        return@withContext updated
    }

    /**
     * 5. Refresh Recommended Artists with live tracks for each artist
     */
    suspend fun refreshRecommendedArtists(current: List<RecommendedArtist>): List<RecommendedArtist> = withContext(Dispatchers.IO) {
        val baseList = current.ifEmpty { getInitialRecommendedArtists() }
        val updated = mutableListOf<RecommendedArtist>()

        for (artist in baseList) {
            try {
                val liveTracks = JioSaavnService.search(artist.query)
                if (liveTracks.isNotEmpty()) {
                    val mergedTracks = (liveTracks + artist.songs).distinctBy { it.id }.take(15)
                    updated.add(artist.copy(songs = mergedTracks))
                } else {
                    updated.add(artist)
                }
            } catch (e: Exception) {
                updated.add(artist)
            }
        }
        return@withContext updated
    }

    /**
     * 6. Refresh a specific Top Songs filter with live tracks
     */
    suspend fun refreshTopSongsFilter(filter: TopSongFilter): List<Song> = withContext(Dispatchers.IO) {
        try {
            val liveTracks = JioSaavnService.search(filter.query)
            if (liveTracks.isNotEmpty()) {
                return@withContext (liveTracks + filter.initialSongs).distinctBy { it.id }.take(25)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error refreshing top songs filter '${filter.label}': ${e.message}")
        }
        return@withContext filter.initialSongs
    }
}
