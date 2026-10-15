package com.hn.loopplayer

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class PlayerDisplay(
    val mediaId: String? = null,
    val title: String = "Nothing playing",
    val isVideo: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volume: Float = 1f,
    val speed: Float = 1f,
)

class MainActivity : ComponentActivity() {
    private val store by lazy { PlaylistStore(this) }
    private var library by mutableStateOf(PlaylistStore.defaultLibrary())
    private var controller by mutableStateOf<MediaController?>(null)
    private var display by mutableStateOf(PlayerDisplay())
    private var message by mutableStateOf<String?>(null)
    private var connection: ListenableFuture<MediaController>? = null
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshDisplay()

        override fun onPlayerError(error: PlaybackException) {
            message = "Could not play this file. It may be missing or unsupported. Remove it and pick it again."
            refreshDisplay()
        }
    }

    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            val imported = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    try { Result.success(store.importTrack(uri)) }
                    catch (e: Exception) { Result.failure<Track>(e) }
                }
            }
            val tracks = imported.mapNotNull { it.getOrNull() }
            val failures = imported.size - tracks.size
            if (tracks.isNotEmpty()) {
                val active = library.activePlaylist
                val existing = active.tracks.mapTo(mutableSetOf()) { it.uri }
                val additions = tracks.filter { existing.add(it.uri) }
                val updated = library.copy(playlists = library.playlists.map {
                    if (it.id == active.id) it.copy(tracks = it.tracks + additions) else it
                })
                updateLibrary(updated, preservePlayback = true)
                message = when {
                    additions.isEmpty() -> "These files are already in the playlist."
                    failures > 0 -> "Added ${additions.size} file(s); $failures could not be opened."
                    else -> "Added ${additions.size} file(s)."
                }
            } else {
                message = "No MP3 or MP4 files could be opened."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        library = store.loadLibrary()
        setContent {
            LoopPlayerTheme {
                PlayerScreen(
                    library = library,
                    display = display,
                    controller = controller,
                    message = message,
                    onDismissMessage = { message = null },
                    onPickFiles = { pickFiles.launch(arrayOf("*/*")) },
                    onCreatePlaylist = ::createPlaylist,
                    onRenamePlaylist = ::renamePlaylist,
                    onDeletePlaylist = ::deletePlaylist,
                    onSelectPlaylist = ::selectPlaylist,
                    onRemoveTrack = ::removeTrack,
                    onMoveTrack = ::moveTrack,
                    onSelectTrack = ::selectTrack,
                    onPlayPause = ::playPause,
                    onStop = {
                        controller?.pause()
                        controller?.seekTo(0L)
                        controller?.stop()
                    },
                    onPrevious = { controller?.seekToPreviousMediaItem() },
                    onNext = { controller?.seekToNextMediaItem() },
                    onSeek = { controller?.seekTo(it) },
                    onVolume = { controller?.volume = it },
                    onSpeed = { controller?.setPlaybackSpeed(it) },
                    onRepeat = ::cycleRepeat,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val future = MediaController.Builder(
            this, SessionToken(this, ComponentName(this, PlaybackService::class.java)),
        ).buildAsync()
        connection = future
        future.addListener({
            try {
                val connected = future.get()
                if (connection !== future) {
                    MediaController.releaseFuture(future)
                    return@addListener
                }
                controller = connected
                connected.addListener(listener)
                // A service may have continued to another track while the activity was closed.
                library = store.loadLibrary()
                if (!queueMatches(connected, library.activePlaylist.tracks)) {
                    syncQueue(connected, library, preservePlayback = true)
                }
                refreshDisplay()
                ticker = lifecycleScope.launch {
                    while (isActive) {
                        refreshDisplay()
                        delay(500)
                    }
                }
            } catch (_: Exception) {
                message = "Playback service could not start. Reopen the app and try again."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        ticker?.cancel()
        ticker = null
        controller?.removeListener(listener)
        controller = null
        connection?.let { MediaController.releaseFuture(it) }
        connection = null
        super.onStop()
    }

    private fun refreshDisplay() {
        val player = controller ?: return
        val item = player.currentMediaItem
        val id = item?.mediaId
        val track = library.playlists.asSequence().flatMap { it.tracks.asSequence() }
            .firstOrNull { it.uri == id }
        display = PlayerDisplay(
            mediaId = id,
            title = track?.title ?: item?.mediaMetadata?.title?.toString() ?: "Nothing playing",
            isVideo = track?.isVideo == true,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            repeatMode = player.repeatMode,
            volume = player.volume,
            speed = player.playbackParameters.speed,
        )
    }

    private fun updateLibrary(next: Library, preservePlayback: Boolean) {
        store.saveLibrary(next)
        library = next
        controller?.let { player ->
            if (!queueMatches(player, next.activePlaylist.tracks) || !preservePlayback) {
                syncQueue(player, next, preservePlayback)
            }
        }
        refreshDisplay()
    }

    private fun queueMatches(player: MediaController, tracks: List<Track>): Boolean =
        player.mediaItemCount == tracks.size &&
            tracks.indices.all { player.getMediaItemAt(it).mediaId == tracks[it].uri }

    private fun syncQueue(player: MediaController, next: Library, preservePlayback: Boolean) {
        val tracks = next.activePlaylist.tracks
        val oldId = player.currentMediaItem?.mediaId
        val oldIndex = if (preservePlayback) tracks.indexOfFirst { it.uri == oldId } else -1
        val wasPlaying = preservePlayback && oldIndex >= 0 && player.isPlaying
        val position = if (oldIndex >= 0) player.currentPosition.coerceAtLeast(0) else 0L
        player.pause()
        if (tracks.isEmpty()) {
            player.clearMediaItems()
        } else {
            player.setMediaItems(tracks.map(Track::toMediaItem), oldIndex.coerceAtLeast(0), position)
            if (wasPlaying) {
                player.prepare()
                player.play()
            }
        }
    }

    private fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val playlist = PlaylistStore.newPlaylist(clean)
        updateLibrary(
            library.copy(playlists = library.playlists + playlist, activePlaylistId = playlist.id, lastIndex = 0),
            preservePlayback = false,
        )
    }

    private fun renamePlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val id = library.activePlaylistId
        updateLibrary(library.copy(playlists = library.playlists.map {
            if (it.id == id) it.copy(name = clean) else it
        }), preservePlayback = true)
    }

    private fun deletePlaylist() {
        if (library.playlists.size == 1) return
        val remaining = library.playlists.filterNot { it.id == library.activePlaylistId }
        updateLibrary(Library(remaining, remaining.first().id, 0), preservePlayback = false)
    }

    private fun selectPlaylist(id: String) {
        if (id == library.activePlaylistId || library.playlists.none { it.id == id }) return
        updateLibrary(library.copy(activePlaylistId = id, lastIndex = 0), preservePlayback = false)
    }

    private fun removeTrack(index: Int) {
        val active = library.activePlaylist
        if (index !in active.tracks.indices) return
        val nextTracks = active.tracks.toMutableList().also { it.removeAt(index) }
        updateLibrary(library.copy(playlists = library.playlists.map {
            if (it.id == active.id) it.copy(tracks = nextTracks) else it
        }, lastIndex = library.lastIndex.coerceAtMost((nextTracks.size - 1).coerceAtLeast(0))), true)
    }

    private fun moveTrack(index: Int, offset: Int) {
        val active = library.activePlaylist
        val target = index + offset
        if (index !in active.tracks.indices || target !in active.tracks.indices) return
        val tracks = active.tracks.toMutableList()
        java.util.Collections.swap(tracks, index, target)
        updateLibrary(library.copy(playlists = library.playlists.map {
            if (it.id == active.id) it.copy(tracks = tracks) else it
        }), true)
    }

    private fun selectTrack(index: Int) {
        val selectedPlaylistId = library.activePlaylistId
        val track = library.activePlaylist.tracks.getOrNull(index) ?: return
        lifecycleScope.launch {
            if (!withContext(Dispatchers.IO) { store.canOpen(track) }) {
                message = "${track.title} is missing or access was lost. Remove it and pick it again."
                return@launch
            }
            val player = controller ?: return@launch
            if (library.activePlaylistId != selectedPlaylistId ||
                library.activePlaylist.tracks.getOrNull(index)?.uri != track.uri
            ) return@launch
            val tracks = library.activePlaylist.tracks
            if (!queueMatches(player, tracks)) {
                player.setMediaItems(tracks.map(Track::toMediaItem))
            }
            player.seekTo(index, 0L)
            player.prepare()
            player.play()
            store.saveLibrary(library.copy(lastIndex = index))
            library = store.loadLibrary()
        }
    }

    private fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
            return
        }
        if (player.mediaItemCount == 0) {
            if (library.activePlaylist.tracks.isEmpty()) {
                message = "Add an MP3 or MP4 file first."
                return
            }
            selectTrack(0)
            return
        }
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val track = library.activePlaylist.tracks.getOrNull(index)
        if (track == null) {
            message = "Choose a file from the playlist."
            return
        }
        lifecycleScope.launch {
            if (!withContext(Dispatchers.IO) { store.canOpen(track) }) {
                message = "${track.title} is missing or access was lost. Remove it and pick it again."
                return@launch
            }
            if (controller !== player) return@launch
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
        }
    }

    private fun cycleRepeat() {
        controller?.let { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}
