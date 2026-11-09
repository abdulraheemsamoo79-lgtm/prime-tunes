package com.primetunes.music.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetunes.music.data.Song
import com.primetunes.music.player.PlaybackState
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

/**
 * Full player (Preview A): big artwork, title/artist, green progress,
 * big green play — plus Like / Download / Queue / Add to Playlist /
 * Sleep timer actions and a Lyrics section (LRCLIB) underneath.
 */
@Composable
fun PlayerScreen(
    playback: PlaybackState,
    isFavorite: Boolean,
    lyricsText: String?,
    lyricsLoading: Boolean,
    sleepTimerLabel: String?,
    playbackSpeed: Float,
    onCycleSpeed: () -> Unit,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onSetSleepMinutes: (Int) -> Unit,
    onSetSleepEndOfSong: () -> Unit,
    onCancelSleepTimer: () -> Unit,
) {
    // No song loaded: close via an effect (never call callbacks during
    // composition) and draw nothing this frame.
    val song = playback.currentSong
    if (song == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onClose() }
        return
    }
    var showSleepDialog by remember { mutableStateOf(false) }

    if (showSleepDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onPickMinutes = { minutes ->
                onSetSleepMinutes(minutes)
                showSleepDialog = false
            },
            onPickEndOfSong = {
                onSetSleepEndOfSong()
                showSleepDialog = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player", tint = Color.White)
            }
            Text(
                "NOW PLAYING",
                color = SpotifyGreyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SongArtwork(song.thumbnailUrl, 300, corner = 8)
        }
        Spacer(Modifier.height(24.dp))

        Text(
            song.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            color = SpotifyGreyText,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))
        val fraction = if (playback.durationMs > 0)
            (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f
        Slider(
            value = fraction,
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = SpotifyGreen,
                inactiveTrackColor = SpotifyGreyText.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(playback.positionMs), color = SpotifyGreyText, fontSize = 12.sp)
            Text(formatMs(playback.durationMs), color = SpotifyGreyText, fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            BigPlayButton(isPlaying = playback.isPlaying, onClick = onToggle)
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerAction(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = "Like",
                tint = if (isFavorite) SpotifyGreen else Color.White,
                onClick = { onToggleFavorite(song) }
            )
            PlayerAction(
                icon = Icons.Filled.Download,
                label = "Download",
                tint = Color.White,
                onClick = { onDownload(song) }
            )
            PlayerAction(
                icon = Icons.Filled.QueueMusic,
                label = "Queue",
                tint = Color.White,
                onClick = onOpenQueue
            )
            PlayerAction(
                icon = Icons.Filled.PlaylistAdd,
                label = "Playlist",
                tint = Color.White,
                onClick = { onAddToPlaylist(song) }
            )
            PlayerAction(
                icon = Icons.Filled.Bedtime,
                label = "Sleep",
                tint = if (sleepTimerLabel != null) SpotifyGreen else Color.White,
                onClick = { showSleepDialog = true }
            )
        }

        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerAction(
                icon = Icons.Filled.Share,
                label = "Share",
                tint = Color.White,
                onClick = { shareSong(context, song) }
            )
            PlayerAction(
                icon = Icons.Filled.Speed,
                label = formatSpeed(playbackSpeed),
                tint = if (playbackSpeed != 1.0f) SpotifyGreen else Color.White,
                onClick = onCycleSpeed
            )
        }

        if (sleepTimerLabel != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    sleepTimerLabel,
                    color = SpotifyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancelSleepTimer) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel sleep timer", tint = SpotifyGreyText, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (playback.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(playback.error, color = SpotifyGreyText, fontSize = 13.sp)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Lyrics",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        when {
            lyricsLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
                Text("Loading lyrics…", color = SpotifyGreyText, fontSize = 14.sp)
            }
            !lyricsText.isNullOrBlank() -> Text(
                lyricsText,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
            else -> Text(
                "Lyrics not available for this song.",
                color = SpotifyGreyText,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PlayerAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(label, color = SpotifyGreyText, fontSize = 11.sp)
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onPickMinutes: (Int) -> Unit,
    onPickEndOfSong: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.primetunes.music.ui.theme.SpotifyCard,
        title = {
            Text("Sleep timer", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Playback will pause when the timer ends.",
                    color = SpotifyGreyText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                listOf(10, 20, 30, 60).forEach { minutes ->
                    TextButton(onClick = { onPickMinutes(minutes) }) {
                        Text("$minutes minutes", color = Color.White, fontSize = 15.sp)
                    }
                }
                TextButton(onClick = onPickEndOfSong) {
                    Text("End of this song", color = Color.White, fontSize = 15.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotifyGreyText)
            }
        }
    )
}

/**
 * Share "<title> — <artist>" via the system chooser. Fully guarded:
 * a missing share target or any intent failure simply does nothing —
 * sharing must never crash the player.
 */
private fun shareSong(context: Context, song: Song) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}\nvia PRIME TUNES")
        }
        val chooser = Intent.createChooser(send, "Share song").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

/** 1.0f → "1x", 1.25f → "1.25x" (the speed button's label). */
private fun formatSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) "${speed.toLong()}x" else "${speed}x"

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
