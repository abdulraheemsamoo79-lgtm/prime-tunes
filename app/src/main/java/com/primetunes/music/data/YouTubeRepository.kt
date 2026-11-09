package com.primetunes.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Phase 1 source: YouTube, via NewPipeExtractor (on-device extraction,
 * no API key, no paid service).
 *
 * Risk (known, accepted for Phase 1): YouTube changes its player/search
 * internals from time to time; when that happens search/stream breaks
 * until NewPipeExtractor ships a fix and we bump the version in
 * app/build.gradle.kts. The [SongRepository] interface keeps that swap
 * contained to this file.
 *
 * NOTE: this code path could NOT be executed in the build sandbox
 * (no Android SDK/JVM there) — first real run is the on-device test.
 */
class YouTubeRepository : SongRepository {

    private fun ensureInit() {
        synchronized(NewPipeInitLock) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloaderImpl.getInstance())
                initialized = true
            }
        }
    }

    override suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        ensureInit()
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    /**
     * Text search suggestions for the Search screen, via
     * NewPipeExtractor's suggestion extractor. API verified against the
     * v0.27.0 javadoc: `StreamingService.getSuggestionExtractor()` takes
     * no arguments and returns a `SuggestionExtractor`, whose
     * `suggestionList(query)` returns the suggestion strings. Any
     * failure returns an empty list — suggestions are a nicety, never a
     * reason to disturb the screen.
     */
    suspend fun textSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInit()
            val extractor = ServiceList.YouTube.getSuggestionExtractor()
                ?: return@withContext emptyList()
            extractor.suggestionList(query)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(8)
        }.getOrDefault(emptyList())
    }

    override suspend fun homeSections(): List<HomeSection> = coroutineScope {
        // Real searches, no fake catalogue. Order mirrors the agreed Home layout.
        val defs = listOf(
            "Trending Pakistan" to "trending pakistan songs",
            "Pakistani Hits" to "pakistani hits songs",
            "Phonk" to "phonk music",
            "Indian Hits" to "bollywood hit songs",
            "English Hits" to "english top hits songs",
        )
        defs.map { (title, query) ->
            async {
                runCatching { HomeSection(title, query, search(query)) }.getOrNull()
            }
        }.awaitAll().filterNotNull().filter { it.songs.isNotEmpty() }
    }

    override suspend fun streamUrl(song: Song): String = withContext(Dispatchers.IO) {
        ensureInit()
        val extractor = ServiceList.YouTube.getStreamExtractor(song.pageUrl)
        extractor.fetchPage()
        val best = extractor.audioStreams.maxByOrNull { it.averageBitrate }
            ?: error("No audio stream found")
        best.content
    }

    private fun StreamInfoItem.toSong(): Song = Song(
        id = url,
        title = name,
        artist = uploaderName ?: "",
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url,
        durationSeconds = duration,
        pageUrl = url,
    )

    private companion object {
        val NewPipeInitLock = Any()
        @Volatile var initialized = false
    }
}
