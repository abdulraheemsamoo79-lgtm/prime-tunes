package com.primetunes.music.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service (feature: play with the app closed).
 *
 * A [MediaSession] over the shared [PlayerHolder] player. Media3's
 * MediaSessionService posts the media notification itself using its
 * default notification provider: song title + artist + artwork (taken
 * from the MediaItem metadata [PlayerController] sets) with
 * play/pause, previous and next controls, and keeps this service in
 * the foreground while music plays — so a song keeps playing when the
 * app is minimized or swiped away.
 *
 * Declared in AndroidManifest with foregroundServiceType=mediaPlayback
 * and the MediaSessionService intent-filter.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, PlayerHolder.get(this)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Nothing playing (or nothing loaded): let the service die with
        // the task. Music playing: keep going in the background.
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        @Suppress("DEPRECATION")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Release only the session. The player itself belongs to
        // PlayerHolder and stays alive for the process, so reopening
        // the app never meets a released player.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
