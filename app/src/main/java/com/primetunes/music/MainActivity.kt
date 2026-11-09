package com.primetunes.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primetunes.music.ui.AddToPlaylistDialog
import com.primetunes.music.ui.AppViewModel
import com.primetunes.music.ui.HomeScreen
import com.primetunes.music.ui.LibraryScreen
import com.primetunes.music.ui.MiniPlayer
import com.primetunes.music.ui.PlayerScreen
import com.primetunes.music.ui.PlaylistDetailScreen
import com.primetunes.music.ui.QueueScreen
import com.primetunes.music.ui.SearchScreen
import com.primetunes.music.ui.Tab
import com.primetunes.music.ui.theme.PrimeTunesTheme
import com.primetunes.music.ui.theme.SpotifyBlack
import com.primetunes.music.ui.theme.SpotifyDark
import com.primetunes.music.ui.theme.SpotifyGreen
import com.primetunes.music.ui.theme.SpotifyGreyText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrimeTunesTheme {
                PrimeTunesApp()
            }
        }
    }
}

@Composable
private fun PrimeTunesApp(viewModel: AppViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // "Add to playlist" dialog sits above every screen, player included.
    ui.addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = ui.playlists,
            onPick = viewModel::addToPlaylist,
            onCreateAndAdd = viewModel::createPlaylistAndAdd,
            onDismiss = viewModel::dismissAddToPlaylist,
        )
    }

    if (ui.queueOpen && playback.currentSong != null) {
        QueueScreen(
            playback = playback,
            onClose = viewModel::closeQueue,
            onJump = viewModel::jumpToQueueItem,
            onRemove = viewModel::removeFromQueue,
        )
        return
    }

    if (ui.playerOpen && playback.currentSong != null) {
        PlayerScreen(
            playback = playback,
            isFavorite = viewModel.isFavorite(playback.currentSong!!),
            lyricsText = ui.lyricsText,
            lyricsLoading = ui.lyricsLoading,
            sleepTimerLabel = ui.sleepTimerLabel,
            onClose = viewModel::closePlayer,
            onToggle = viewModel::togglePlayPause,
            onNext = viewModel::playNext,
            onPrevious = viewModel::playPrevious,
            onSeek = viewModel::seekTo,
            onToggleFavorite = viewModel::toggleFavorite,
            onDownload = viewModel::download,
            onOpenQueue = viewModel::openQueue,
            onAddToPlaylist = viewModel::requestAddToPlaylist,
            onSetSleepMinutes = viewModel::setSleepTimerMinutes,
            onSetSleepEndOfSong = viewModel::setSleepTimerEndOfSong,
            onCancelSleepTimer = viewModel::cancelSleepTimer,
        )
        return
    }

    Scaffold(
        containerColor = SpotifyBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                MiniPlayer(
                    playback = playback,
                    onClick = viewModel::openPlayer,
                    onToggle = viewModel::togglePlayPause,
                )
                NavigationBar(containerColor = SpotifyDark) {
                    val items = listOf(
                        Triple(Tab.HOME, Icons.Filled.Home, "Home"),
                        Triple(Tab.SEARCH, Icons.Filled.Search, "Search"),
                        Triple(Tab.LIBRARY, Icons.Filled.LibraryMusic, "Library"),
                    )
                    items.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = ui.tab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = SpotifyGreyText,
                                unselectedTextColor = SpotifyGreyText,
                                indicatorColor = Color.Transparent,
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
                .padding(padding)
        ) {
            val openPlaylist = ui.playlists.firstOrNull { it.id == ui.openPlaylistId }
            when {
                ui.tab == Tab.LIBRARY && openPlaylist != null -> PlaylistDetailScreen(
                    playlist = openPlaylist,
                    currentSong = playback.currentSong,
                    onBack = viewModel::closePlaylist,
                    onSongClick = { song, queue ->
                        viewModel.play(song, queue)
                        viewModel.openPlayer()
                    },
                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                    onAddToPlaylist = viewModel::requestAddToPlaylist,
                )

                ui.tab == Tab.HOME -> HomeScreen(
                    sections = ui.homeSections,
                    recentlyPlayed = ui.recentlyPlayed,
                    loading = ui.homeLoading,
                    error = ui.homeError,
                    currentSong = playback.currentSong,
                    onRetry = viewModel::loadHome,
                    onCategoryClick = viewModel::openCategory,
                    onSongClick = { song, queue ->
                        viewModel.play(song, queue)
                        viewModel.openPlayer()
                    },
                )

                ui.tab == Tab.SEARCH -> SearchScreen(
                    query = ui.searchQuery,
                    results = ui.searchResults,
                    loading = ui.searchLoading,
                    error = ui.searchError,
                    currentSong = playback.currentSong,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSubmit = viewModel::submitSearch,
                    onSongClick = { song, queue ->
                        viewModel.play(song, queue)
                        viewModel.openPlayer()
                    },
                    onAddToPlaylist = viewModel::requestAddToPlaylist,
                )

                ui.tab == Tab.LIBRARY -> LibraryScreen(
                    favorites = ui.favorites,
                    downloads = ui.downloads,
                    playlists = ui.playlists,
                    currentSong = playback.currentSong,
                    onSongClick = { song, queue ->
                        viewModel.play(song, queue)
                        viewModel.openPlayer()
                    },
                    onPlaylistClick = { viewModel.openPlaylist(it.id) },
                    onCreatePlaylist = viewModel::createPlaylist,
                    onAddToPlaylist = viewModel::requestAddToPlaylist,
                )
            }
        }
    }
}
