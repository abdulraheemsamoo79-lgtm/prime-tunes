package com.primetunes.music.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.primetunes.music.data.HomeSection
import com.primetunes.music.data.LibraryStore
import com.primetunes.music.data.LyricsRepository
import com.primetunes.music.data.Playlist
import com.primetunes.music.data.SaavnRepository
import com.primetunes.music.data.Song
import com.primetunes.music.data.YouTubeRepository
import com.primetunes.music.player.PlaybackState
import com.primetunes.music.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Tab { HOME, SEARCH, LIBRARY }

/** Pure UI filter over the already-merged search results (no re-search). */
enum class SearchFilter { ALL, SAAVN, YOUTUBE }

data class AppUiState(
    val tab: Tab = Tab.HOME,
    val homeSections: List<HomeSection> = emptyList(),
    val homeLoading: Boolean = true,
    val homeError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    /** Live suggestions while typing (hidden again once a search is submitted). */
    val textSuggestions: List<String> = emptyList(),
    val songSuggestions: List<Song> = emptyList(),
    val suggestionsOpen: Boolean = false,
    /** All / Saavn / YouTube filter over [searchResults]. */
    val searchFilter: SearchFilter = SearchFilter.ALL,
    /** Last submitted queries (Search screen, empty-query state). */
    val recentSearches: List<String> = emptyList(),
    /** Current playback speed (player screen label). */
    val playbackSpeed: Float = 1.0f,
    val favorites: List<Song> = emptyList(),
    val downloads: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    /** Playlist whose detail page is open (Library), null = none. */
    val openPlaylistId: String? = null,
    val playerOpen: Boolean = false,
    val queueOpen: Boolean = false,
    /** Song waiting to be added to a playlist (dialog visible when set). */
    val addToPlaylistSong: Song? = null,
    /** Lyrics for the currently playing song (null = none found / not loaded). */
    val lyricsText: String? = null,
    val lyricsLoading: Boolean = false,
    /** "Sleep timer: 12 min left" / "Sleep timer: end of song"; null = off. */
    val sleepTimerLabel: String? = null,
    val resolvingSongId: String? = null,
    /** One-off message (e.g. a song that failed to play); UI shows and clears it. */
    val message: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val youtube = YouTubeRepository()
    private val saavn = SaavnRepository()
    private val lyricsRepository = LyricsRepository()
    private val library = LibraryStore(app)
    val player = PlayerController(app)
    val playback: StateFlow<PlaybackState> = player.state

    private val _ui = MutableStateFlow(
        AppUiState(
            favorites = library.favorites(),
            downloads = library.downloads(),
            recentlyPlayed = library.recentlyPlayed(),
            playlists = library.playlists(),
            recentSearches = library.recentSearches(),
        )
    )
    val ui: StateFlow<AppUiState> = _ui

    private var progressJob: Job? = null
    private var sleepJob: Job? = null
    private var suggestionJob: Job? = null
    private var sleepAtEndOfSong = false
    private var lyricsSongId: String? = null

    init {
        player.onTrackEnded = {
            if (sleepAtEndOfSong) {
                sleepAtEndOfSong = false
                _ui.value = _ui.value.copy(sleepTimerLabel = null)
                player.pause()
            } else {
                playNext()
            }
        }
        loadHome()
        progressJob = viewModelScope.launch {
            while (isActive) {
                player.publish()
                delay(500)
            }
        }
        // Lyrics follow the playing song.
        viewModelScope.launch {
            player.state.collect { state ->
                val song = state.currentSong
                if (song?.id != lyricsSongId) {
                    lyricsSongId = song?.id
                    if (song == null) {
                        _ui.value = _ui.value.copy(lyricsText = null, lyricsLoading = false)
                    } else {
                        loadLyrics(song)
                    }
                }
            }
        }
    }

    // --- Navigation ----------------------------------------------------

    fun selectTab(tab: Tab) {
        _ui.value = _ui.value.copy(tab = tab)
    }

    fun openPlayer() {
        _ui.value = _ui.value.copy(playerOpen = true)
    }

    fun closePlayer() {
        _ui.value = _ui.value.copy(playerOpen = false)
    }

    fun openQueue() {
        _ui.value = _ui.value.copy(queueOpen = true)
    }

    fun closeQueue() {
        _ui.value = _ui.value.copy(queueOpen = false)
    }

    /** Category cards on Home jump to Search with that query (Preview A behaviour). */
    fun openCategory(query: String) {
        _ui.value = _ui.value.copy(tab = Tab.SEARCH)
        onSearchQueryChange(query)
        submitSearch()
    }

    // --- Home (YouTube only, by design) ---------------------------------

    fun loadHome() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(homeLoading = true, homeError = null)
            runCatching { youtube.homeSections() }
                .onSuccess { sections ->
                    _ui.value = _ui.value.copy(
                        homeSections = sections,
                        homeLoading = false,
                        homeError = if (sections.isEmpty())
                            "Could not load songs. Check internet and retry." else null,
                    )
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        homeLoading = false,
                        homeError = "Could not load songs. Check internet and retry.",
                    )
                }
        }
    }

    // --- Search (YouTube + Saavn merged) ---------------------------------

    fun onSearchQueryChange(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        scheduleSuggestions(query)
    }

    /**
     * Live suggestions while typing: debounced ~300 ms, ignored under 2
     * chars, stale jobs cancelled, every failure degrades to an empty
     * list silently — suggestions must never disturb the screen, let
     * alone crash it. Results are only applied if the query they were
     * fetched for is still the current one.
     */
    private fun scheduleSuggestions(rawQuery: String) {
        suggestionJob?.cancel()
        val query = rawQuery.trim()
        if (query.length < 2) {
            _ui.value = _ui.value.copy(
                textSuggestions = emptyList(),
                songSuggestions = emptyList(),
                suggestionsOpen = false,
            )
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(300)
            val textsDeferred = async {
                runCatching { youtube.textSuggestions(query) }.getOrDefault(emptyList())
            }
            val songsDeferred = async {
                runCatching { saavn.searchSuggestions(query) }.getOrDefault(emptyList())
            }
            val texts = textsDeferred.await()
            val songs = songsDeferred.await()
            if (_ui.value.searchQuery.trim() == query) {
                _ui.value = _ui.value.copy(
                    textSuggestions = texts,
                    songSuggestions = songs,
                    suggestionsOpen = true,
                )
            }
        }
    }

    /** Hide the suggestion list and stop any in-flight suggestion fetch. */
    private fun closeSuggestions() {
        suggestionJob?.cancel()
        _ui.value = _ui.value.copy(
            textSuggestions = emptyList(),
            songSuggestions = emptyList(),
            suggestionsOpen = false,
        )
    }

    /** Tapping a text suggestion: fill the box with it and search at once. */
    fun onTextSuggestionClick(text: String) {
        _ui.value = _ui.value.copy(searchQuery = text)
        submitSearch()
    }

    fun setSearchFilter(filter: SearchFilter) {
        _ui.value = _ui.value.copy(searchFilter = filter)
    }

    fun clearRecentSearches() {
        runCatching { library.clearRecentSearches() }
        _ui.value = _ui.value.copy(recentSearches = emptyList())
    }

    /** Tapping a recent search runs it again. */
    fun onRecentSearchClick(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        submitSearch()
    }

    fun submitSearch() {
        val query = _ui.value.searchQuery.trim()
        closeSuggestions()
        if (query.isEmpty()) return
        runCatching { library.addRecentSearch(query) }
        _ui.value = _ui.value.copy(
            searchFilter = SearchFilter.ALL,
            recentSearches = runCatching { library.recentSearches() }
                .getOrDefault(_ui.value.recentSearches),
        )
        viewModelScope.launch {
            _ui.value = _ui.value.copy(searchLoading = true, searchError = null)
            val ytDeferred = async { runCatching { youtube.search(query) }.getOrDefault(emptyList()) }
            val saavnDeferred = async { runCatching { saavn.search(query) }.getOrDefault(emptyList()) }
            val merged = mergeResults(ytDeferred.await(), saavnDeferred.await())
            _ui.value = _ui.value.copy(
                searchResults = merged,
                searchLoading = false,
                searchError = if (merged.isEmpty())
                    "No songs found for \"$query\". Check internet and try again." else null,
            )
        }
    }

    /** Interleave both sources, dropping the same song appearing twice. */
    private fun mergeResults(youtubeSongs: List<Song>, saavnSongs: List<Song>): List<Song> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<Song>()
        val max = maxOf(youtubeSongs.size, saavnSongs.size)
        for (i in 0 until max) {
            if (i < saavnSongs.size) {
                val song = saavnSongs[i]
                if (seen.add(dedupeKey(song))) merged.add(song)
            }
            if (i < youtubeSongs.size) {
                val song = youtubeSongs[i]
                if (seen.add(dedupeKey(song))) merged.add(song)
            }
        }
        return merged.take(40)
    }

    private fun dedupeKey(song: Song): String {
        val title = song.title.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        val artist = song.artist.lowercase().split(",", "&").firstOrNull()?.trim().orEmpty()
        return "$title|$artist"
    }

    // --- Playback --------------------------------------------------------

    fun play(song: Song, queue: List<Song>) {
        runCatching { player.setQueue(queue.ifEmpty { listOf(song) }, song) }
        resolveAndPlay(song)
    }

    fun playNext() {
        val next = player.nextSong() ?: return
        resolveAndPlay(next)
    }

    fun playPrevious() {
        val prev = player.previousSong() ?: return
        resolveAndPlay(prev)
    }

    /** Stream URL for any source: Saavn songs already carry one. */
    private suspend fun resolveStream(song: Song): String {
        if (!song.directStreamUrl.isNullOrBlank()) return song.directStreamUrl
        return if (song.source == Song.SOURCE_SAAVN) saavn.streamUrl(song)
        else youtube.streamUrl(song)
    }

    private fun resolveAndPlay(song: Song) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(resolvingSongId = song.id)
            // Every step is guarded on its own: in v0.1 the side effects
            // inside onSuccess { } (here player.play) were NOT covered by
            // the runCatching around resolveStream, so a throw there
            // escaped the coroutine and crashed the app — the same
            // pattern that made Download crash. Now a failure at any
            // step lands in the clean message state instead.
            val url = runCatching { resolveStream(song) }.getOrNull()
            val played = url != null && runCatching { player.play(song, url) }.isSuccess
            if (played) {
                runCatching { library.addRecentlyPlayed(song) }
                _ui.value = _ui.value.copy(
                    resolvingSongId = null,
                    recentlyPlayed = runCatching { library.recentlyPlayed() }
                        .getOrDefault(_ui.value.recentlyPlayed),
                )
            } else {
                _ui.value = _ui.value.copy(
                    resolvingSongId = null,
                    message = "Could not play \"${song.title}\". Please try another song.",
                )
            }
        }
    }

    /** Player speed button: 1.0 → 1.25 → 1.5 → 2.0 → 0.75 → 1.0 … */
    fun cyclePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val currentIndex = speeds.indexOf(_ui.value.playbackSpeed)
        val next = speeds[(currentIndex + 1).mod(speeds.size)]
        runCatching { player.setPlaybackSpeed(next) }
            .onSuccess { _ui.value = _ui.value.copy(playbackSpeed = next) }
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun seekTo(fraction: Float) {
        val duration = player.state.value.durationMs
        if (duration > 0) player.seekTo((fraction * duration).toLong())
    }

    // --- Queue -----------------------------------------------------------

    fun jumpToQueueItem(index: Int) {
        val song = runCatching { player.jumpToQueueItem(index) }.getOrNull() ?: return
        resolveAndPlay(song)
    }

    fun removeFromQueue(index: Int) {
        val replacement = runCatching { player.removeFromQueue(index) }.getOrNull()
        if (replacement != null) {
            resolveAndPlay(replacement)
        } else if (player.state.value.currentSong == null) {
            _ui.value = _ui.value.copy(queueOpen = false, playerOpen = false)
        }
    }

    // --- Sleep timer ------------------------------------------------------

    fun setSleepTimerMinutes(minutes: Int) {
        sleepJob?.cancel()
        sleepAtEndOfSong = false
        _ui.value = _ui.value.copy(sleepTimerLabel = "Sleep timer: $minutes min left")
        sleepJob = viewModelScope.launch {
            var remainingSeconds = minutes * 60
            while (remainingSeconds > 0) {
                delay(30_000)
                remainingSeconds -= 30
                if (remainingSeconds > 0) {
                    val minsLeft = (remainingSeconds + 59) / 60
                    _ui.value = _ui.value.copy(sleepTimerLabel = "Sleep timer: $minsLeft min left")
                }
            }
            player.pause()
            _ui.value = _ui.value.copy(sleepTimerLabel = null)
        }
    }

    fun setSleepTimerEndOfSong() {
        sleepJob?.cancel()
        sleepJob = null
        sleepAtEndOfSong = true
        _ui.value = _ui.value.copy(sleepTimerLabel = "Sleep timer: end of song")
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepAtEndOfSong = false
        _ui.value = _ui.value.copy(sleepTimerLabel = null)
    }

    // --- Lyrics -----------------------------------------------------------

    private fun loadLyrics(song: Song) {
        _ui.value = _ui.value.copy(lyricsText = null, lyricsLoading = true)
        viewModelScope.launch {
            val text = runCatching { lyricsRepository.lyrics(song) }.getOrNull()
            // A different song may have started while we were fetching.
            if (lyricsSongId == song.id) {
                _ui.value = _ui.value.copy(lyricsText = text, lyricsLoading = false)
            }
        }
    }

    // --- Library: favourites + downloads -----------------------------------

    fun toggleFavorite(song: Song) {
        library.toggleFavorite(song)
        _ui.value = _ui.value.copy(favorites = library.favorites())
    }

    fun isFavorite(song: Song): Boolean = _ui.value.favorites.any { it.id == song.id }

    /**
     * Download via Android DownloadManager into Music/PRIME TUNES
     * (visible in Files app), with an app-folder fallback.
     *
     * v0.1 CRASH (user's phone report, verified in this code): the old
     * version wrapped ONLY resolveStream() in runCatching — the whole
     * DownloadManager block (service lookup, Request build,
     * setDestinationInExternalPublicDir, enqueue) sat inside
     * onSuccess { }, whose exceptions runCatching does NOT catch, so
     * the first DownloadManager exception escaped the coroutine and
     * killed the app on the Download tap. Now the ENTIRE operation
     * lives inside one runCatching; any failure at any step shows the
     * clean "Could not download" message instead.
     */
    fun download(song: Song) {
        viewModelScope.launch {
            val outcome = runCatching {
                val url = resolveStream(song)
                require(url.isNotBlank() &&
                    (url.startsWith("https://") || url.startsWith("http://"))) {
                    "Stream URL is not a valid http(s) URL"
                }
                val context = getApplication<Application>()
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    ?: error("Download service unavailable")
                val safeName = song.title.replace(Regex("[^A-Za-z0-9 _-]"), "").take(60).ifBlank { "song" }
                // Extension must match the real container: Saavn streams
                // are MP4 audio (verified audio/mp4 in the 2026-11-09 live
                // tests), YouTube's highest-bitrate audio stream from
                // NewPipe is normally Opus-in-WebM. Saving a YouTube file
                // as ".m4a" mislabels it and some players refuse it.
                val extension = if (song.source == Song.SOURCE_SAAVN) "m4a" else "webm"
                val fileName = "$safeName.$extension"

                fun newRequest(): DownloadManager.Request =
                    DownloadManager.Request(Uri.parse(url))
                        .setTitle(song.title)
                        .setDescription(song.artist)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Preferred: the public Music/PRIME TUNES folder. If
                // anything there throws (storage state, OEM quirks),
                // retry into the app's own external files dir — that
                // destination needs no permission and always exists.
                // Returns true when the fallback was used.
                try {
                    manager.enqueue(
                        newRequest().setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_MUSIC, "PRIME TUNES/$fileName"
                        )
                    )
                    false
                } catch (e: Exception) {
                    manager.enqueue(
                        newRequest().setDestinationInExternalFilesDir(
                            context, Environment.DIRECTORY_MUSIC, "PRIME TUNES/$fileName"
                        )
                    )
                    true
                }
            }
            outcome
                .onSuccess { usedAppFolder ->
                    // Only record the download once an enqueue actually
                    // succeeded — v0.1 recorded it unconditionally.
                    runCatching { library.addDownload(song) }
                    _ui.value = _ui.value.copy(
                        downloads = runCatching { library.downloads() }
                            .getOrDefault(_ui.value.downloads),
                        message = if (usedAppFolder)
                            "Downloading \"${song.title}\" — saved in the app folder"
                        else "Downloading \"${song.title}\"",
                    )
                }
                .onFailure {
                    _ui.value = _ui.value.copy(message = "Could not download \"${song.title}\".")
                }
        }
    }

    // --- Playlists ---------------------------------------------------------

    fun openPlaylist(playlistId: String) {
        _ui.value = _ui.value.copy(openPlaylistId = playlistId)
    }

    fun closePlaylist() {
        _ui.value = _ui.value.copy(openPlaylistId = null)
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        library.createPlaylist(name)
        _ui.value = _ui.value.copy(
            playlists = library.playlists(),
            message = "Playlist \"${name.trim()}\" created",
        )
    }

    fun deletePlaylist(playlistId: String) {
        library.deletePlaylist(playlistId)
        _ui.value = _ui.value.copy(
            playlists = library.playlists(),
            openPlaylistId = if (_ui.value.openPlaylistId == playlistId) null else _ui.value.openPlaylistId,
        )
    }

    /** Open the "add to playlist" dialog for [song] (player / song row menu). */
    fun requestAddToPlaylist(song: Song) {
        _ui.value = _ui.value.copy(addToPlaylistSong = song)
    }

    fun dismissAddToPlaylist() {
        _ui.value = _ui.value.copy(addToPlaylistSong = null)
    }

    fun addToPlaylist(playlistId: String) {
        val song = _ui.value.addToPlaylistSong ?: return
        library.addToPlaylist(playlistId, song)
        val playlistName = library.playlists().firstOrNull { it.id == playlistId }?.name
        _ui.value = _ui.value.copy(
            playlists = library.playlists(),
            addToPlaylistSong = null,
            message = if (playlistName != null) "Added to \"$playlistName\"" else "Added to playlist",
        )
    }

    /** Create a new playlist from the dialog and put the waiting song in it. */
    fun createPlaylistAndAdd(name: String) {
        val song = _ui.value.addToPlaylistSong ?: return
        if (name.isBlank()) return
        val playlist = library.createPlaylist(name)
        library.addToPlaylist(playlist.id, song)
        _ui.value = _ui.value.copy(
            playlists = library.playlists(),
            addToPlaylistSong = null,
            message = "Added to \"${playlist.name}\"",
        )
    }

    fun removeFromPlaylist(playlistId: String, song: Song) {
        library.removeFromPlaylist(playlistId, song.id)
        _ui.value = _ui.value.copy(playlists = library.playlists())
    }

    override fun onCleared() {
        progressJob?.cancel()
        sleepJob?.cancel()
        player.release()
        super.onCleared()
    }
}
