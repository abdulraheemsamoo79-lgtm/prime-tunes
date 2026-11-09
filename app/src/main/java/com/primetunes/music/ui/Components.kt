package com.primetunes.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.primetunes.music.data.Playlist
import com.primetunes.music.data.Song
import com.primetunes.music.player.PlaybackState
import com.primetunes.music.ui.theme.SpotifyCard
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

@Composable
fun SongArtwork(url: String?, size: Int, corner: Int = 4) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(SpotifyCard),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SpotifyGreyText)
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

/** Small pill showing where a song comes from ("YouTube" / "Saavn"). */
@Composable
fun SourceChip(song: Song) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SpotifyCard)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            song.sourceLabel,
            color = if (song.source == Song.SOURCE_SAAVN) SpotifyGreen else SpotifyGreyText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDownload: ((Song) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(song.thumbnailUrl, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) SpotifyGreen else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = SpotifyGreyText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SourceChip(song)
        if (song.durationLabel.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(song.durationLabel, color = SpotifyGreyText, fontSize = 13.sp)
        }
        if (onDownload != null) {
            IconButton(onClick = { onDownload(song) }) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = SpotifyGreyText
                )
            }
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = { onAddToPlaylist(song) }) {
                Icon(
                    Icons.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = SpotifyGreyText
                )
            }
        }
    }
}

/** Floating mini player above the bottom bar (Preview A). */
@Composable
fun MiniPlayer(
    playback: PlaybackState,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val song = playback.currentSong ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyCard)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongArtwork(song.thumbnailUrl, 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist, color = SpotifyGreyText, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playback.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        if (playback.durationMs > 0) {
            LinearProgressIndicator(
                progress = { (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = SpotifyGreen,
                trackColor = Color.Transparent,
            )
        }
    }
}

/** Round green play/pause button used by the full player. */
@Composable
fun BigPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(SpotifyGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * "Add to playlist" dialog: pick an existing playlist, or type a name
 * to create a new one with this song already inside.
 */
@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onPick: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCard,
        title = {
            Text("Add to playlist", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    song.title,
                    color = SpotifyGreyText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet. Create your first one below.",
                        color = SpotifyGreyText,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(Modifier.height(200.dp)) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(playlist.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        playlist.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${playlist.songs.size} songs",
                                        color = SpotifyGreyText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name", color = SpotifyGreyText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank()) onCreateAndAdd(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Create + Add", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotifyGreyText)
            }
        }
    )
}
