package com.primetunes.music.data

/**
 * Song source boundary. Two implementations:
 * [YouTubeRepository] (YouTube via NewPipeExtractor, no API key) and
 * [SaavnRepository] (JioSaavn via the keyless saavn.sumit.co API).
 * Search merges both; Home uses YouTube only. Nothing in the UI or
 * player depends on the concrete source — playback resolves through
 * whichever repository owns the song's `source`.
 */
interface SongRepository {
    /** Free-text search, songs only. */
    suspend fun search(query: String): List<Song>

    /** Home rows for Preview A (Recently played / Made for you are filled from real searches). */
    suspend fun homeSections(): List<HomeSection>

    /** Resolve a direct, playable audio stream URL for [song]. */
    suspend fun streamUrl(song: Song): String
}
