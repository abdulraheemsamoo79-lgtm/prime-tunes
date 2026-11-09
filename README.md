# PRIME TUNES

Free music app — every song, Spotify-style UI (pure black + Spotify green).

- **Search & play** songs from YouTube (NewPipeExtractor, on-device, no API key) and JioSaavn (320kbps direct streams), merged in one Search with a Saavn/YouTube label on every song
- **Background playback** with notification controls (play/pause, next, previous)
- **Home** — Trending Pakistan, Pakistani Hits, Phonk, Indian Hits, English Hits
- **Library** — Liked songs, Downloads, your own playlists
- **Queue**, **lyrics** (LRCLIB), **sleep timer**, and **offline downloads** to `Music/PRIME TUNES/`

Package: `com.primetunes.music` · Native Android (Kotlin + Jetpack Compose + Media3)

APK builds automatically with GitHub Actions on every push to `main`; pushing a `v*` tag publishes a GitHub Release with `prime-tunes.apk` attached.
