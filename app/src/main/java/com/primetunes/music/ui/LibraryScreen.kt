package com.primetunes.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetunes.music.data.Playlist
import com.primetunes.music.data.Song
import com.primetunes.music.ui.theme.SpotifyCard
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

@Composable
fun LibraryScreen(
    favorites: List<Song>,
    downloads: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song?,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Your Library",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlists (${playlists.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create playlist", tint = SpotifyGreen)
                }
            }
        }
        if (playlists.isEmpty()) {
            item { EmptyNote("Create your own playlists — tap + above, or add a song from the player.") }
        } else {
            items(playlists) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onPlay = { if (playlist.songs.isNotEmpty()) onSongClick(playlist.songs.first(), playlist.songs) }
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            LibraryHeader("Liked Songs", favorites.size)
        }
        if (favorites.isEmpty()) {
            item { EmptyNote("Songs you like from the player will appear here.") }
        } else {
            items(favorites) { song ->
                SongRow(song, song.id == currentSong?.id,
                    onClick = { onSongClick(song, favorites) },
                    onAddToPlaylist = onAddToPlaylist)
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            LibraryHeader("Downloads", downloads.size)
        }
        if (downloads.isEmpty()) {
            item { EmptyNote("Songs you download will appear here. Files are saved in Music/PRIME TUNES on your phone.") }
        } else {
            items(downloads) { song ->
                SongRow(song, song.id == currentSong?.id,
                    onClick = { onSongClick(song, downloads) },
                    onAddToPlaylist = onAddToPlaylist)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(playlist.songs.firstOrNull()?.thumbnailUrl, 52)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.songs.size} songs",
                color = SpotifyGreyText,
                fontSize = 13.sp
            )
        }
        if (playlist.songs.isNotEmpty()) {
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play playlist", tint = SpotifyGreen, modifier = Modifier.size(30.dp))
            }
        }
    }
}

/** Detail page for one playlist: play it, tap songs, remove songs, delete playlist. */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    currentSong: Song?,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SpotifyCard,
            title = { Text("Delete playlist?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${playlist.name}\" will be removed. The songs themselves stay in the app.",
                    color = SpotifyGreyText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeletePlaylist(playlist)
                }) {
                    Text("Delete", color = Color(0xFFE22134), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = SpotifyGreyText)
                }
            }
        )
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${playlist.songs.size} songs", color = SpotifyGreyText, fontSize = 13.sp)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", tint = SpotifyGreyText)
                }
            }
        }

        if (playlist.songs.isEmpty()) {
            item { EmptyNote("No songs yet. Open any song and tap Playlist to add it here.") }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BigPlayButton(isPlaying = false, onClick = {
                        onSongClick(playlist.songs.first(), playlist.songs)
                    })
                    Spacer(Modifier.size(12.dp))
                    Text("Play all", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            items(playlist.songs) { song ->
                SongRow(song, song.id == currentSong?.id,
                    onClick = { onSongClick(song, playlist.songs) },
                    onAddToPlaylist = onAddToPlaylist)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotifyCard,
        title = { Text("Create playlist", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = SpotifyGreyText) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotifyGreyText)
            }
        }
    )
}

@Composable
private fun LibraryHeader(title: String, count: Int) {
    Text(
        "$title ($count)",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        color = SpotifyGreyText,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
