package com.primetunes.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Second source: JioSaavn, via the keyless unofficial API at
 * saavn.sumit.co (same public endpoint family many open-source players
 * use). No API key, no account.
 *
 * VERIFIED from the build sandbox on 2026-11-09: GET
 * https://saavn.sumit.co/api/search/songs?query=pasoori returned
 * success=true with results carrying id / name / duration /
 * artists.primary[].name / image[] / downloadUrl[] (up to 320kbps).
 * The code below parses exactly that shape. It has NOT been executed
 * inside the Android app yet (no SDK in the sandbox) — first real run
 * is the on-device test. Any failure returns an empty list / throws a
 * plain error that the ViewModel turns into a clean retry state.
 *
 * Home stays YouTube-only by design, so [homeSections] is empty.
 */
class SaavnRepository : SongRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val json = getJson("$BASE_URL/api/search/songs?query=$encoded&limit=20")
            val results = json.getJSONObject("data").getJSONArray("results")
            List(results.length()) { i -> results.getJSONObject(i).toSong() }
                .filter { it.directStreamUrl != null }
        }.getOrDefault(emptyList())
    }

    override suspend fun homeSections(): List<HomeSection> = emptyList()

    override suspend fun streamUrl(song: Song): String = withContext(Dispatchers.IO) {
        song.directStreamUrl?.let { return@withContext it }
        // Fallback: the stored song lost its direct URL — fetch details by id.
        val rawId = song.id.removePrefix(ID_PREFIX)
        val json = getJson("$BASE_URL/api/songs/$rawId")
        val data = json.getJSONArray("data")
        if (data.length() == 0) error("Song not found on Saavn")
        data.getJSONObject(0).bestDownloadUrl() ?: error("No stream found on Saavn")
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PRIME TUNES (Android)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Saavn request failed (${response.code})")
            val body = response.body?.string().orEmpty()
            return JSONObject(body)
        }
    }

    private fun JSONObject.toSong(): Song {
        val artists = optJSONObject("artists")
            ?.optJSONArray("primary")
        val artistNames = if (artists == null) "" else List(artists.length()) { i ->
            artists.getJSONObject(i).optString("name")
        }.filter { it.isNotBlank() }.joinToString(", ")

        val images = optJSONArray("image")
        val thumbnail = if (images == null || images.length() == 0) null
        else images.getJSONObject(images.length() - 1).optString("url").ifBlank { null }

        return Song(
            id = ID_PREFIX + getString("id"),
            title = optString("name"),
            artist = artistNames,
            thumbnailUrl = thumbnail,
            durationSeconds = optLong("duration"),
            pageUrl = optString("url"),
            source = Song.SOURCE_SAAVN,
            directStreamUrl = bestDownloadUrl(),
        )
    }

    /** Highest-quality download URL (320kbps when offered). */
    private fun JSONObject.bestDownloadUrl(): String? {
        val urls = optJSONArray("downloadUrl") ?: return null
        if (urls.length() == 0) return null
        var best: String? = null
        for (i in 0 until urls.length()) {
            val entry = urls.getJSONObject(i)
            val url = entry.optString("url")
            if (url.isBlank()) continue
            if (entry.optString("quality") == "320kbps") return url
            best = url // entries arrive in ascending quality order
        }
        return best
    }

    private companion object {
        const val BASE_URL = "https://saavn.sumit.co"
        const val ID_PREFIX = "saavn:"
    }
}
