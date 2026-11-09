package com.primetunes.music.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetunes.music.data.Song
import com.primetunes.music.ui.theme.SpotifyCard
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

@Composable
fun SearchScreen(
    query: String,
    results: List<Song>,
    loading: Boolean,
    error: String?,
    currentSong: Song?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Search",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("What do you want to listen to?", color = SpotifyGreyText) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpotifyCard,
                unfocusedContainerColor = SpotifyCard,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = SpotifyCard,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        when {
            loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = SpotifyGreen) }

            error != null -> Text(
                error,
                color = SpotifyGreyText,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )

            results.isEmpty() -> Text(
                "Search any song, artist or album — results come from Saavn and YouTube together.",
                color = SpotifyGreyText,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSong?.id,
                        onClick = { onSongClick(song, results) },
                        onAddToPlaylist = onAddToPlaylist
                    )
                }
            }
        }
    }
}
