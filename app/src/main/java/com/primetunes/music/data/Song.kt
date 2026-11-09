package com.primetunes.music.data

/**
 * One playable/searchable track.
 *
 * [source] is [SOURCE_YOUTUBE] or [SOURCE_SAAVN]. [pageUrl] is the source
 * page (YouTube watch URL / JioSaavn song page). Saavn results also carry a
 * [directStreamUrl] (320kbps preferred), so playback/download needs no
 * extra resolution step; YouTube songs resolve theirs via NewPipeExtractor.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val pageUrl: String,
    val source: String = SOURCE_YOUTUBE,
    val directStreamUrl: String? = null,
) {
    val durationLabel: String
        get() {
            if (durationSeconds <= 0) return ""
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return "%d:%02d".format(m, s)
        }

    /** Small label shown on song rows ("YouTube" / "Saavn"). */
    val sourceLabel: String
        get() = if (source == SOURCE_SAAVN) "Saavn" else "YouTube"

    companion object {
        const val SOURCE_YOUTUBE = "youtube"
        const val SOURCE_SAAVN = "saavn"
    }
}

data class HomeSection(
    val title: String,
    val query: String,
    val songs: List<Song>,
)

/** A user-created playlist, stored locally in [LibraryStore]. */
data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
)
