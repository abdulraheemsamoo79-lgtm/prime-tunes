package com.primetunes.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Preview A "Pure Spotify Green" — fixed palette (UI FINAL, user-confirmed 2026-11-09)
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyBlack = Color(0xFF000000)
val SpotifyDark = Color(0xFF121212)
val SpotifyCard = Color(0xFF282828)
val SpotifyGreyText = Color(0xFFB3B3B3)

private val PrimeTunesColors = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.Black,
    background = SpotifyBlack,
    onBackground = Color.White,
    surface = SpotifyDark,
    onSurface = Color.White,
    surfaceVariant = SpotifyCard,
    onSurfaceVariant = SpotifyGreyText,
    secondary = SpotifyGreen,
)

@Composable
fun PrimeTunesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrimeTunesColors,
        content = content
    )
}
