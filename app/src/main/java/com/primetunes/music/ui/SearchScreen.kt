package com.primetunes.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
    textSuggestions: List<String>,
    songSuggestions: List<Song>,
    suggestionsOpen: Boolean,
    searchFilter: SearchFilter,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onTextSuggestionClick: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
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

        val showSuggestions = suggestionsOpen && !loading &&
            (songSuggestions.isNotEmpty() || textSuggestions.isNotEmpty())

        when {
            loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = SpotifyGreen) }

            // Live suggestions while typing: rich song rows first
            // (tap = play, like a search result), then plain text
            // suggestions (tap = fill the box and run that search).
            showSuggestions -> LazyColumn(Modifier.fillMaxSize()) {
                items(songSuggestions) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSong?.id,
                        onClick = { onSongClick(song, songSuggestions) },
                        onAddToPlaylist = onAddToPlaylist,
                        onDownload = onDownload
                    )
                }
                items(textSuggestions) { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTextSuggestionClick(text) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = SpotifyGreyText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            error != null -> Text(
                error,
                color = SpotifyGreyText,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )

            results.isNotEmpty() -> {
                val filtered = when (searchFilter) {
                    SearchFilter.ALL -> results
                    SearchFilter.SAAVN -> results.filter { it.source == Song.SOURCE_SAAVN }
                    SearchFilter.YOUTUBE -> results.filter { it.source == Song.SOURCE_YOUTUBE }
                }
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterPill("All", searchFilter == SearchFilter.ALL) {
                            onFilterChange(SearchFilter.ALL)
                        }
                        Spacer(Modifier.width(8.dp))
                        FilterPill("Saavn", searchFilter == SearchFilter.SAAVN) {
                            onFilterChange(SearchFilter.SAAVN)
                        }
                        Spacer(Modifier.width(8.dp))
                        FilterPill("YouTube", searchFilter == SearchFilter.YOUTUBE) {
                            onFilterChange(SearchFilter.YOUTUBE)
                        }
                    }
                    if (filtered.isEmpty()) {
                        Text(
                            "No ${if (searchFilter == SearchFilter.SAAVN) "Saavn" else "YouTube"} songs in these results.",
                            color = SpotifyGreyText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered) { song ->
                                SongRow(
                                    song = song,
                                    isCurrent = song.id == currentSong?.id,
                                    onClick = { onSongClick(song, filtered) },
                                    onAddToPlaylist = onAddToPlaylist,
                                    onDownload = onDownload
                                )
                            }
                        }
                    }
                }
            }

            else -> Column(Modifier.fillMaxSize()) {
                if (query.isEmpty() && recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent searches",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearRecentSearches) {
                            Text("Clear", color = SpotifyGreyText, fontSize = 13.sp)
                        }
                    }
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(recentSearches) { recent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRecentSearchClick(recent) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = null,
                                    tint = SpotifyGreyText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    recent,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    "Search any song, artist or album — results come from Saavn and YouTube together.",
                    color = SpotifyGreyText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/** Spotify-style filter pill (green when selected). */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SpotifyGreen else SpotifyCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
