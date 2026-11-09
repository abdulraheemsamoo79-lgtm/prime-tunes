package com.primetunes.music.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.primetunes.music.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: String? = null,
    /** Snapshot of the play queue + which item is current (Queue screen). */
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
)

/**
 * Thin wrapper around the shared Media3 ExoPlayer ([PlayerHolder]).
 *
 * The player itself lives in the holder so [PlaybackService] (background
 * playback + notification controls) drives the exact same instance as
 * the UI. The queue is manual: one MediaItem at a time, and when a
 * track ends [onTrackEnded] lets the ViewModel advance — that is also
 * the hook the sleep timer uses for its "end of song" mode.
 */
class PlayerController(context: Context) {

    private val appContext: Context = context.applicationContext

    val player: ExoPlayer = PlayerHolder.get(appContext)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val queue = mutableListOf<Song>()
    private var queueIndex = -1

    /** Set by the ViewModel: called when a track finishes, to advance the queue. */
    var onTrackEnded: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onTrackEnded?.invoke()
                publish()
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(
                    isBuffering = false,
                    error = "Could not play this song. Please try another one."
                )
            }
        })
    }

    fun setQueue(songs: List<Song>, startAt: Song) {
        queue.clear()
        queue.addAll(songs)
        queueIndex = queue.indexOfFirst { it.id == startAt.id }.coerceAtLeast(0)
        publish()
    }

    /** Caller resolves the stream URL first (repository), then calls this. */
    fun play(song: Song, streamUrl: String) {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .apply {
                if (!song.thumbnailUrl.isNullOrBlank()) {
                    setArtworkUri(Uri.parse(song.thumbnailUrl))
                }
            }
            .build()
        val item = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
        _state.value = _state.value.copy(
            currentSong = song,
            isBuffering = true,
            error = null,
        )
        player.setMediaItem(item)
        player.prepare()
        player.play()
        // Start the media-session service only AFTER the player actually
        // holds an item: a service started via startForegroundService()
        // must post its foreground notification within ~5 seconds, and
        // Media3's notification provider can only do that once there is
        // media to show. Starting it first raced that window.
        startPlaybackService()
        publish()
    }

    /** Bring up the media-session service so playback survives the UI. */
    private fun startPlaybackService() {
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, PlaybackService::class.java)
            )
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        publish()
    }

    fun pause() {
        player.pause()
        publish()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        publish()
    }

    fun nextSong(): Song? {
        if (queueIndex + 1 >= queue.size) return null
        queueIndex++
        return queue[queueIndex]
    }

    fun previousSong(): Song? {
        if (player.currentPosition > 3_000) {
            player.seekTo(0)
            return null
        }
        if (queueIndex - 1 < 0) return null
        queueIndex--
        return queue[queueIndex]
    }

    // --- Queue editing (Queue screen) -----------------------------------

    /**
     * Jump to the queue item at [index]. Returns the song the ViewModel
     * should resolve + play, or null when [index] is already current
     * (playback just restarts from 0 in that case).
     */
    fun jumpToQueueItem(index: Int): Song? {
        if (index !in queue.indices) return null
        if (index == queueIndex) {
            player.seekTo(0)
            return null
        }
        queueIndex = index
        publish()
        return queue[queueIndex]
    }

    /**
     * Remove the queue item at [index].
     *
     * Returns the replacement song the ViewModel must play when the
     * removed item was the one playing (the next song slides into its
     * place). Returns null in every other case — including when the
     * queue became empty, in which case playback is stopped and
     * [state]'s currentSong is cleared here.
     */
    fun removeFromQueue(index: Int): Song? {
        if (index !in queue.indices) return null
        queue.removeAt(index)
        when {
            queue.isEmpty() -> {
                queueIndex = -1
                player.stop()
                _state.value = PlaybackState()
                return null
            }
            index < queueIndex -> {
                queueIndex--
                publish()
                return null
            }
            index == queueIndex -> {
                queueIndex = minOf(index, queue.lastIndex)
                publish()
                return queue[queueIndex]
            }
            else -> {
                publish()
                return null
            }
        }
    }

    fun publish() {
        val s = _state.value
        _state.value = s.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            queue = queue.toList(),
            queueIndex = queueIndex,
        )
    }

    /**
     * No-op on purpose: the player is shared with [PlaybackService]
     * via [PlayerHolder] and must outlive the ViewModel, otherwise
     * background playback would die with the screen.
     */
    fun release() {
        // Intentionally empty — see PlayerHolder.
    }
}
