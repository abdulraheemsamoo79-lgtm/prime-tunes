package com.primetunes.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Lyrics from LRCLIB (lrclib.net) — free, keyless, community lyrics DB.
 *
 * VERIFIED from the build sandbox on 2026-11-09: GET
 * https://lrclib.net/api/get?artist_name=Ali%20Sethi&track_name=Pasoori
 * returned a JSON object with a non-empty "plainLyrics" field.
 *
 * Any failure (no match, no network, bad JSON) returns null and the
 * player shows a clean "Lyrics not available" note — never an error.
 */
class LyricsRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Plain lyrics for [song], or null when LRCLIB has none. */
    suspend fun lyrics(song: Song): String? = withContext(Dispatchers.IO) {
        runCatching { fetchByGet(song) }.getOrNull()
            ?: runCatching { fetchBySearch(song) }.getOrNull()
    }

    private fun fetchByGet(song: Song): String? {
        val title = URLEncoder.encode(song.title, "UTF-8")
        val artist = URLEncoder.encode(firstArtist(song.artist), "UTF-8")
        val durationPart = if (song.durationSeconds > 0) "&duration=${song.durationSeconds}" else ""
        val url = "https://lrclib.net/api/get?track_name=$title&artist_name=$artist$durationPart"
        val json = get(url) as? JSONObject ?: return null
        return extractLyrics(json)
    }

    private fun fetchBySearch(song: Song): String? {
        val title = URLEncoder.encode(song.title, "UTF-8")
        val artist = URLEncoder.encode(firstArtist(song.artist), "UTF-8")
        val url = "https://lrclib.net/api/search?track_name=$title&artist_name=$artist"
        val arr = get(url) as? JSONArray ?: return null
        for (i in 0 until arr.length()) {
            extractLyrics(arr.getJSONObject(i))?.let { return it }
        }
        return null
    }

    /** plainLyrics first; otherwise syncedLyrics with timestamps stripped. */
    private fun extractLyrics(json: JSONObject): String? {
        val plain = json.optString("plainLyrics").trim()
        if (plain.isNotEmpty()) return plain
        val synced = json.optString("syncedLyrics").trim()
        if (synced.isEmpty()) return null
        val stripped = synced.lines()
            .map { line -> line.replace(Regex("^\\[[0-9:.]+]\\s*"), "").trim() }
            .joinToString("\n")
            .trim()
        return stripped.ifEmpty { null }
    }

    private fun get(url: String): Any? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PRIME TUNES (Android music app)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isEmpty()) return null
            return runCatching {
                if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
            }.getOrNull()
        }
    }

    /** LRCLIB matches best on a single artist name. */
    private fun firstArtist(artist: String): String =
        artist.split(",", "&", " feat", " ft").firstOrNull()?.trim().orEmpty()
}
