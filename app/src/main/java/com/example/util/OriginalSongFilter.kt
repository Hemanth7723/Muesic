package com.example.util

import com.example.data.entity.Song

object OriginalSongFilter {
    private val FORBIDDEN_PATTERN = Regex(
        "(?i)\\b(remix|remixes|mashup|mashups|cover|covers|slowed|reverb|nightcore|karaoke|instrumental|bass boosted|sped up|speed up|parody|reaction|10 hour|workout|tribute|orchestral|relaxing piano|mellow piano|acoustic cover|lofi|lo-fi|8d audio|8d|synthwave cover|ringtone|backing track|piano version|piano cover|clean edit|dj edit|originally performed by|melody karaoke|tribute to|in the style of)\\b"
    )
    private val MIX_QUERY_PATTERN =
        Regex("(?i)\\b(remix|cover|mashup|slowed|reverb|nightcore|karaoke|acoustic|instrumental)\\b")

    private val FORBIDDEN_ARTISTS = listOf(
        "zzang karaoke",
        "workout music",
        "covers culture",
        "nxsser",
        "deep mage",
        "veronica bravo",
        "lexdez",
        "guitar cafetéria",
        "guitar cafeteria",
        "ben plum",
        "audino",
        "boostereo",
        "nyae",
        "kate chruscicka",
        "sing2piano",
        "karaoke all stars",
        "the piano guys",
        "hits acoustic",
        "instrumental pop songs",
        "mega hits karaoke",
        "piano tribute players"
    )

    /**
     * Returns true if the song is an authentic original track (not a remix, cover, karaoke, etc.)
     */
    fun isOriginal(song: Song, allowNonOriginal: Boolean = false): Boolean {
        if (allowNonOriginal) return true
        val combined = "${song.title} ${song.artist} ${song.album}"
        if (combined.contains(FORBIDDEN_PATTERN)) {
            return false
        }
        if (FORBIDDEN_ARTISTS.any { song.artist.contains(it, ignoreCase = true) }) {
            return false
        }
        return true
    }

    /**
     * Checks if user explicitly typed words like 'remix' or 'cover' in their search query.
     */
    fun isUserSearchingForMix(query: String): Boolean {
        return query.contains(MIX_QUERY_PATTERN)
    }
}
