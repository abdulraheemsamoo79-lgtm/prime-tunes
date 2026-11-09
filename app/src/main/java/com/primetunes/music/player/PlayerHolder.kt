package com.primetunes.music.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * The ONE ExoPlayer instance for the whole app.
 *
 * Both the UI ([PlayerController]) and the background [PlaybackService]
 * must drive the SAME player, otherwise the notification controls and
 * the on-screen player would fight over two different players. The
 * player is created once, on first use, and kept for the app process
 * lifetime (the service only releases its MediaSession, never this).
 */
object PlayerHolder {

    @Volatile
    private var instance: ExoPlayer? = null

    fun get(context: Context): ExoPlayer =
        instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

    private fun build(context: Context): ExoPlayer =
        ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
}
