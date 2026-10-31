package com.example.util

import java.util.Locale

object SongTitleCleaner {
    private val DASH_PARTS = Regex("\\s+-\\s+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]")
    private val NON_ALPHANUMERIC_WITH_SPACES = Regex("[^a-zA-Z0-9\\s]")
    private val WHITESPACE = Regex("\\s+")
    private val TITLE_SEGMENT_SEPARATOR = Regex("[|•/:]")
    private val QUERY_SEGMENT_SEPARATOR = Regex("[|•]")
    private val ARTIST_SEPARATOR = Regex("(?i)\\s*(?:,|&|/|\\bfeat\\.?\\b|\\bft\\.?\\b)\\s*")
    private val ARTIST_NOISE = Regex("(?i)\\b(topic|vevo|records|music|official)\\b")

    private val NOISE_PATTERNS = listOf(
        Regex("(?i)\\b(official\\s+video|official\\s+music\\s+video|music\\s+video|official\\s+audio|audio\\s+track|lyric\\s+video|lyrics\\s+video|lyrical\\s+video|full\\s+song|full\\s+video\\s+song|video\\s+song|audio\\s+song|original\\s+soundtrack|ost|remastered|4k|hd|1080p|visualizer|audio)\\b"),
        Regex("(?i)\\[.*?\\]"),
        Regex("(?i)\\(.*?\\)"),
        Regex("(?i)\\bft\\.?\\s+.*"),
        Regex("(?i)\\bfeat\\.?\\s+.*")
    )

    private val GENERIC_ARTISTS = setOf(
        "youtube", "youtube music", "youtube creator", "youtube audio",
        "various artists", "unknown", "unknown artist", "topic", "vevo",
        "records", "music"
    )

    /**
     * Cleans the raw title down to its primary song title.
     * Accurately handles "Artist - Song", "Song | Movie", "Song : Details", etc.
     */
    fun cleanPrimaryTitle(rawTitle: String, rawArtist: String = ""): String {
        var t = rawTitle
        for (pattern in NOISE_PATTERNS) {
            t = t.replace(pattern, " ")
        }

        // Check for "Artist - Title" or "Title - Artist" format
        val dashParts = t.split(DASH_PARTS).map { it.trim() }.filter { it.isNotBlank() }
        if (dashParts.size >= 2) {
            val partA = dashParts[0]
            val partB = dashParts[1]
            val normArtist = rawArtist.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")
            val normA = partA.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")
            val normB = partB.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")

            t = if (normArtist.isNotBlank() && (normArtist.contains(normA) || normA.contains(normArtist))) {
                // partA is the artist, so partB is the title
                partB
            } else if (normArtist.isNotBlank() && (normArtist.contains(normB) || normB.contains(normArtist))) {
                // partB is the artist, so partA is the title
                partA
            } else {
                // If unknown, partB is usually the track title
                partB.ifBlank { partA }
            }
        } else {
            // Split by pipe, bullet, colon, or slash
            val firstSegment = t.split(TITLE_SEGMENT_SEPARATOR).firstOrNull()?.trim() ?: t
            t = firstSegment
        }

        return t.replace(NON_ALPHANUMERIC_WITH_SPACES, " ").replace(WHITESPACE, " ").trim()
    }

    /**
     * Generates a ranked list of queries to search against high-definition authentic music catalogs.
     */
    fun generateSearchQueries(rawTitle: String, rawArtist: String): List<String> {
        val primary = cleanPrimaryTitle(rawTitle, rawArtist)
        val queries = mutableListOf<String>()

        // Check if there is a secondary movie / album segment
        val segments = rawTitle.split(QUERY_SEGMENT_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
        var secondarySegment: String? = null
        if (segments.size > 1) {
            var s = segments[1]
            for (pattern in NOISE_PATTERNS) {
                s = s.replace(pattern, " ")
            }
            s = s.replace(NON_ALPHANUMERIC_WITH_SPACES, " ").replace(WHITESPACE, " ").trim()
            if (s.length > 2) {
                secondarySegment = s
            }
        }

        val artistLower = rawArtist.lowercase(Locale.ROOT).trim()
        val isGenericArtist = rawArtist.length <= 2 || GENERIC_ARTISTS.any { artistLower.contains(it) }
        val cleanArtist = if (!isGenericArtist) {
            rawArtist.replace(ARTIST_NOISE, "")
                .replace(NON_ALPHANUMERIC_WITH_SPACES, " ")
                .replace(WHITESPACE, " ")
                .trim()
        } else ""

        // Also check if rawTitle had a dash with artist
        var titleArtistPart = ""
        val dashParts = rawTitle.split(DASH_PARTS).map { it.trim() }.filter { it.isNotBlank() }
        if (dashParts.size >= 2) {
            val p0 = dashParts[0].replace(NON_ALPHANUMERIC_WITH_SPACES, " ").trim()
            if (cleanArtist.isBlank() || p0.contains(cleanArtist, ignoreCase = true)) {
                titleArtistPart = p0
            }
        }

        val effectiveArtist = when {
            cleanArtist.isNotBlank() -> cleanArtist
            titleArtistPart.isNotBlank() -> titleArtistPart
            else -> ""
        }

        // 1. Primary + Artist (e.g. "Believer Imagine Dragons" or "Samajavaragamana Sid Sriram")
        if (primary.isNotBlank() && effectiveArtist.isNotBlank()) {
            queries.add("$primary $effectiveArtist".trim())
        }

        // 2. Primary + Movie/Album (e.g. "Samajavaragamana Ala Vaikunthapurramuloo")
        if (primary.isNotBlank() && !secondarySegment.isNullOrBlank()) {
            queries.add("$primary $secondarySegment".trim())
        }

        // 3. Primary title alone (e.g. "Believer" or "Samajavaragamana")
        if (primary.isNotBlank()) {
            queries.add(primary)
        }

        // 4. Raw cleaned title without noise tags
        var cleanedRaw = rawTitle
        for (pattern in NOISE_PATTERNS) {
            cleanedRaw = cleanedRaw.replace(pattern, " ")
        }
        cleanedRaw = cleanedRaw.replace(TITLE_SEGMENT_SEPARATOR, " ")
            .replace(NON_ALPHANUMERIC_WITH_SPACES, " ")
            .replace(WHITESPACE, " ")
            .trim()
        if (cleanedRaw.isNotBlank() && cleanedRaw != primary) {
            queries.add(cleanedRaw)
        }

        return queries.distinct()
    }

    /**
     * Checks if a candidate track matches the target query/primary title.
     */
    fun isFuzzyMatch(candidateTitle: String, primaryTitle: String): Boolean {
        val normCandidate = candidateTitle.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")
        val normPrimary = primaryTitle.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")
        if (normPrimary.isBlank() || normCandidate.isBlank()) return false
        return normCandidate.contains(normPrimary) || normPrimary.contains(normCandidate)
    }

    fun isStrongTrackMatch(
        candidateTitle: String,
        candidateArtist: String,
        expectedTitle: String,
        expectedArtist: String
    ): Boolean {
        val candidate = normalizeTrackTitle(cleanPrimaryTitle(candidateTitle, candidateArtist))
        val expected = normalizeTrackTitle(cleanPrimaryTitle(expectedTitle, expectedArtist))
        if (candidate.isBlank() || candidate != expected) return false

        if (isGenericArtist(expectedArtist)) return true
        if (isGenericArtist(candidateArtist)) return false

        val candidateArtists = splitArtists(candidateArtist)
        val expectedArtists = splitArtists(expectedArtist)
        return candidateArtists.any { it in expectedArtists }
    }

    private fun normalizeTrackTitle(title: String): String =
        title.lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")

    private fun isGenericArtist(artist: String): Boolean {
        val normalized = artist.lowercase(Locale.ROOT).trim()
        return normalized.isBlank() || GENERIC_ARTISTS.any { normalized == it }
    }

    private fun splitArtists(artist: String): Set<String> =
        artist.split(ARTIST_SEPARATOR)
            .map(::normalizeTrackTitle)
            .filter { it.isNotBlank() }
            .toSet()

}
