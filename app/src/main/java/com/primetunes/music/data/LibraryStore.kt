package com.primetunes.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny local library: favourite songs, downloaded-song records,
 * recently-played and user playlists — all stored as JSON in
 * SharedPreferences (same keep-it-simple approach as PRIME VID VAULT's
 * history store). No database.
 *
 * Song JSON written before the Saavn update has no "source" /
 * "directStreamUrl" keys; reads default them (YouTube / null), so old
 * stored libraries keep working.
 */
class LibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    // --- Favourites / downloads / recent --------------------------------

    fun favorites(): List<Song> = readList(KEY_FAVORITES)

    fun isFavorite(song: Song): Boolean = favorites().any { it.id == song.id }

    fun toggleFavorite(song: Song): Boolean {
        val current = favorites().toMutableList()
        val exists = current.any { it.id == song.id }
        if (exists) current.removeAll { it.id == song.id } else current.add(0, song)
        writeList(KEY_FAVORITES, current)
        return !exists
    }

    fun downloads(): List<Song> = readList(KEY_DOWNLOADS)

    fun addDownload(song: Song) {
        val current = downloads().toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        writeList(KEY_DOWNLOADS, current)
    }

    fun recentlyPlayed(): List<Song> = readList(KEY_RECENT)

    fun addRecentlyPlayed(song: Song) {
        val current = recentlyPlayed().toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        writeList(KEY_RECENT, current.take(30))
    }

    // --- Playlists --------------------------------------------------------

    fun playlists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val songsArr = obj.optJSONArray("songs") ?: JSONArray()
                Playlist(
                    id = obj.getString("id"),
                    name = obj.optString("name"),
                    songs = List(songsArr.length()) { j -> songsArr.getJSONObject(j).toSong() },
                )
            }
        }.getOrDefault(emptyList())
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(
            id = "pl_" + System.currentTimeMillis().toString(),
            name = name.trim().ifBlank { "My Playlist" },
        )
        writePlaylists(playlists() + playlist)
        return playlist
    }

    fun deletePlaylist(playlistId: String) {
        writePlaylists(playlists().filterNot { it.id == playlistId })
    }

    fun addToPlaylist(playlistId: String, song: Song) {
        val updated = playlists().map { playlist ->
            if (playlist.id != playlistId) playlist
            else playlist.copy(songs = listOf(song) + playlist.songs.filterNot { it.id == song.id })
        }
        writePlaylists(updated)
    }

    fun removeFromPlaylist(playlistId: String, songId: String) {
        val updated = playlists().map { playlist ->
            if (playlist.id != playlistId) playlist
            else playlist.copy(songs = playlist.songs.filterNot { it.id == songId })
        }
        writePlaylists(updated)
    }

    private fun writePlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { playlist ->
            val songsArr = JSONArray()
            playlist.songs.forEach { songsArr.put(it.toJson()) }
            arr.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("songs", songsArr)
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    // --- JSON plumbing -------------------------------------------------

    private fun readList(key: String): List<Song> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.getJSONObject(i).toSong() }
        }.getOrDefault(emptyList())
    }

    private fun writeList(key: String, songs: List<Song>) {
        val arr = JSONArray()
        songs.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun Song.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("artist", artist)
        .put("thumbnailUrl", thumbnailUrl ?: JSONObject.NULL)
        .put("durationSeconds", durationSeconds)
        .put("pageUrl", pageUrl)
        .put("source", source)
        .put("directStreamUrl", directStreamUrl ?: JSONObject.NULL)

    private fun JSONObject.toSong(): Song = Song(
        id = getString("id"),
        title = getString("title"),
        artist = optString("artist"),
        thumbnailUrl = if (isNull("thumbnailUrl")) null else optString("thumbnailUrl"),
        durationSeconds = optLong("durationSeconds"),
        pageUrl = getString("pageUrl"),
        source = optString("source", Song.SOURCE_YOUTUBE),
        directStreamUrl = if (isNull("directStreamUrl")) null else optString("directStreamUrl"),
    )

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_DOWNLOADS = "downloads"
        const val KEY_RECENT = "recent"
        const val KEY_PLAYLISTS = "playlists"
    }
}
