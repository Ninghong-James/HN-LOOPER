package com.hn.loopplayer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class Track(val uri: String, val title: String, val mimeType: String) {
    val isVideo: Boolean get() = mimeType == MimeTypes.VIDEO_MP4

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(uri)
        .setUri(Uri.parse(uri))
        .setMimeType(mimeType)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()
}

data class Playlist(val id: String, val name: String, val tracks: List<Track>)

data class Library(
    val playlists: List<Playlist>,
    val activePlaylistId: String,
    val lastIndex: Int,
) {
    val activePlaylist: Playlist
        get() = playlists.firstOrNull { it.id == activePlaylistId } ?: playlists.first()
}

data class PlaybackSettings(val repeatMode: Int, val volume: Float, val speed: Float)

class PlaylistStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("hn_loop_player", Context.MODE_PRIVATE)

    @Synchronized
    fun loadLibrary(): Library {
        val raw = prefs.getString("library", null) ?: return defaultLibrary()
        return try {
            val root = JSONObject(raw)
            val array = root.getJSONArray("playlists")
            val playlists = buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val tracksArray = item.getJSONArray("tracks")
                    val tracks = buildList {
                        for (j in 0 until tracksArray.length()) {
                            val track = tracksArray.getJSONObject(j)
                            val uri = track.getString("uri")
                            val mime = track.getString("mimeType")
                            if (Uri.parse(uri).scheme == "content" &&
                                (mime == MimeTypes.AUDIO_MPEG || mime == MimeTypes.VIDEO_MP4)
                            ) {
                                add(Track(uri, track.getString("title"), mime))
                            }
                        }
                    }
                    add(Playlist(item.getString("id"), item.getString("name"), tracks))
                }
            }
            if (playlists.isEmpty()) return defaultLibrary()
            val activeId = root.optString("activePlaylistId")
            Library(
                playlists = playlists,
                activePlaylistId = activeId.takeIf { id -> playlists.any { it.id == id } }
                    ?: playlists.first().id,
                lastIndex = root.optInt("lastIndex", 0).coerceAtLeast(0),
            )
        } catch (_: Exception) {
            defaultLibrary()
        }
    }

    @Synchronized
    fun saveLibrary(library: Library) {
        val root = JSONObject()
            .put("activePlaylistId", library.activePlaylistId)
            .put("lastIndex", library.lastIndex)
        val playlists = JSONArray()
        library.playlists.forEach { playlist ->
            val tracks = JSONArray()
            playlist.tracks.forEach { track ->
                tracks.put(JSONObject()
                    .put("uri", track.uri)
                    .put("title", track.title)
                    .put("mimeType", track.mimeType))
            }
            playlists.put(JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("tracks", tracks))
        }
        prefs.edit().putString("library", root.put("playlists", playlists).toString()).apply()
    }

    @Synchronized
    fun loadSettings(): PlaybackSettings = PlaybackSettings(
        repeatMode = prefs.getInt("repeatMode", Player.REPEAT_MODE_OFF)
            .takeIf { it in Player.REPEAT_MODE_OFF..Player.REPEAT_MODE_ALL }
            ?: Player.REPEAT_MODE_OFF,
        volume = prefs.getFloat("volume", 1f).coerceIn(0f, 1f),
        speed = prefs.getFloat("speed", 1f).coerceIn(0.5f, 2f),
    )

    @Synchronized
    fun saveSettings(settings: PlaybackSettings) {
        prefs.edit()
            .putInt("repeatMode", settings.repeatMode)
            .putFloat("volume", settings.volume.coerceIn(0f, 1f))
            .putFloat("speed", settings.speed.coerceIn(0.5f, 2f))
            .apply()
    }

    /** The system picker grants access only to selected documents; no storage permission is needed. */
    @Throws(IOException::class, SecurityException::class)
    fun importTrack(uri: Uri): Track {
        if (uri.scheme != "content") throw IOException("Only files selected through the system picker are supported.")

        val name = appContext.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "Untitled"

        val providerType = appContext.contentResolver.getType(uri)?.lowercase()
        val mimeType = when {
            name.endsWith(".mp3", ignoreCase = true) -> MimeTypes.AUDIO_MPEG
            name.endsWith(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
            providerType in setOf("audio/mpeg", "audio/mp3", "audio/x-mpeg") -> MimeTypes.AUDIO_MPEG
            providerType == "video/mp4" -> MimeTypes.VIDEO_MP4
            else -> throw IOException("$name is not an MP3 or MP4 file.")
        }

        appContext.contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        appContext.contentResolver.openFileDescriptor(uri, "r")?.use { }
            ?: throw IOException("$name could not be opened.")
        return Track(uri.toString(), name, mimeType)
    }

    fun canOpen(track: Track): Boolean = try {
        appContext.contentResolver.openFileDescriptor(Uri.parse(track.uri), "r")?.use { true } ?: false
    } catch (_: Exception) {
        false
    }

    companion object {
        fun defaultLibrary(): Library = Library(
            playlists = listOf(Playlist("default", "My playlist", emptyList())),
            activePlaylistId = "default",
            lastIndex = 0,
        )

        fun newPlaylist(name: String): Playlist = Playlist(UUID.randomUUID().toString(), name, emptyList())
    }
}
