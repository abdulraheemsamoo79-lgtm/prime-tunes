package com.primetunes.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primetunes.music.data.HomeSection
import com.primetunes.music.data.Song
import com.primetunes.music.ui.theme.SpotifyCard
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

/**
 * Preview A Home: "PRIME TUNES" header, Good evening, 2x2 playlist cards,
 * then song rows per section (Recently played comes from the local store,
 * the rest from real YouTube searches).
 */
@Composable
fun HomeScreen(
    sections: List<HomeSection>,
    recentlyPlayed: List<Song>,
    loading: Boolean,
    error: String?,
    currentSong: Song?,
    onRetry: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                "PRIME TUNES",
                color = SpotifyGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )
            Text(
                greeting(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
            )
        }

        item { CategoryGrid(onCategoryClick) }

        if (loading) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = SpotifyGreen) }
            }
        }

        if (error != null) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(error, color = SpotifyGreyText, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black)
                    ) { Text("Retry") }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionRow(
                    title = "Recently played",
                    songs = recentlyPlayed,
                    currentSong = currentSong,
                    onSongClick = onSongClick
                )
            }
        }

        items(sections) { section ->
            SectionRow(
                title = section.title,
                songs = section.songs,
                currentSong = currentSong,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
private fun CategoryGrid(onCategoryClick: (String) -> Unit) {
    val categories = listOf(
        "Liked Songs" to "liked songs hits",
        "Trending Pakistan" to "trending pakistan songs",
        "Pakistani Hits" to "pakistani hits songs",
        "Phonk" to "phonk music",
        "Indian Hits" to "bollywood hit songs",
        "English Hits" to "english top hits songs",
    )
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, query) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SpotifyCard)
                            .clickable { onCategoryClick(query) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            label, color = Color.White, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionRow(
    title: String,
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song, List<Song>) -> Unit,
) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onSongClick(song, songs) }
                ) {
                    SongArtwork(song.thumbnailUrl, 140, corner = 4)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        song.title,
                        color = if (song.id == currentSong?.id) SpotifyGreen else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = SpotifyGreyText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
