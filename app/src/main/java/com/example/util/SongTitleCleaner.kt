package com.example.util

import java.util.Locale

object SongTitleCleaner {
    private val NOISE_PATTERNS = listOf(
        Regex("(?i)\\b(official\\s+video|official\\s+music\\s+video|music\\s+video|official\\s+audio|audio\\s+track|lyric\\s+video|lyrics\\s+video|lyrical\\s+video|full\\s+song|full\\s+video\\s+song|video\\s+song|audio\\s+song|original\\s+soundtrack|ost|remastered|4k|hd|1080p|visualizer|audio)\\b"),
        Regex("(?i)\\[.*?\\]"),
        Regex("(?i)\\(.*?\\)"),
        Regex("(?i)\\bft\\.?\\s+.*"),
        Regex("(?i)\\bfeat\\.?\\s+.*")
    )

    private val GENERIC_ARTISTS = setOf(
        "youtube", "youtube music", "youtube audio", "various artists",
        "unknown", "unknown artist", "topic", "vevo", "records", "music"
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
        val dashParts = t.split(Regex("\\s+-\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (dashParts.size >= 2) {
            val partA = dashParts[0]
            val partB = dashParts[1]
            val normArtist = rawArtist.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            val normA = partA.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            val normB = partB.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

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
            val firstSegment = t.split(Regex("[|•/:]")).firstOrNull()?.trim() ?: t
            t = firstSegment
        }

        return t.replace(Regex("[^a-zA-Z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Generates a ranked list of queries to search against high-definition authentic music catalogs.
     */
    fun generateSearchQueries(rawTitle: String, rawArtist: String): List<String> {
        val primary = cleanPrimaryTitle(rawTitle, rawArtist)
        val queries = mutableListOf<String>()

        // Check if there is a secondary movie / album segment
        val segments = rawTitle.split(Regex("[|•]")).map { it.trim() }.filter { it.isNotBlank() }
        var secondarySegment: String? = null
        if (segments.size > 1) {
            var s = segments[1]
            for (pattern in NOISE_PATTERNS) {
                s = s.replace(pattern, " ")
            }
            s = s.replace(Regex("[^a-zA-Z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
            if (s.length > 2) {
                secondarySegment = s
            }
        }

        val artistLower = rawArtist.lowercase(Locale.ROOT).trim()
        val isGenericArtist = rawArtist.length <= 2 || GENERIC_ARTISTS.any { artistLower.contains(it) }
        val cleanArtist = if (!isGenericArtist) {
            rawArtist.replace(Regex("(?i)\\b(topic|vevo|records|music|official)\\b"), "")
                .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } else ""

        // Also check if rawTitle had a dash with artist
        var titleArtistPart = ""
        val dashParts = rawTitle.split(Regex("\\s+-\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (dashParts.size >= 2) {
            val p0 = dashParts[0].replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
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
        cleanedRaw = cleanedRaw.replace(Regex("[|•/:]"), " ").replace(Regex("[^a-zA-Z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        if (cleanedRaw.isNotBlank() && cleanedRaw != primary) {
            queries.add(cleanedRaw)
        }

        return queries.distinct()
    }

    /**
     * Checks if a candidate track matches the target query/primary title.
     */
    fun isFuzzyMatch(candidateTitle: String, primaryTitle: String): Boolean {
        val normCandidate = candidateTitle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val normPrimary = primaryTitle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        if (normPrimary.isBlank() || normCandidate.isBlank()) return false
        return normCandidate.contains(normPrimary) || normPrimary.contains(normCandidate)
    }
}
