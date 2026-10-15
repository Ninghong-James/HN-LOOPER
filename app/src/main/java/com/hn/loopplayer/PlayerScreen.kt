package com.hn.loopplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import java.util.Locale

@Composable
fun PlayerScreen(
    library: Library,
    display: PlayerDisplay,
    controller: MediaController?,
    message: String?,
    onDismissMessage: () -> Unit,
    onPickFiles: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onSelectTrack: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
    ) {
        val wide = maxWidth >= 760.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Header() }
                    if (message != null) item { MessageCard(message, onDismissMessage) }
                    item {
                        LibraryHeader(library, onPickFiles, onCreatePlaylist, onRenamePlaylist,
                            onDeletePlaylist, onSelectPlaylist)
                    }
                    itemsIndexed(library.activePlaylist.tracks, key = { _, track -> track.uri }) { index, track ->
                        TrackRow(track, index, library.activePlaylist.tracks.size,
                            display.mediaId == track.uri, onSelectTrack, onRemoveTrack, onMoveTrack)
                    }
                    if (library.activePlaylist.tracks.isEmpty()) item { EmptyLibrary() }
                }
                LazyColumn(Modifier.weight(1f)) {
                    item {
                        PlayerPanel(display, controller, onPlayPause, onStop, onPrevious, onNext,
                            onSeek, onVolume, onSpeed, onRepeat)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Header() }
                if (message != null) item { MessageCard(message, onDismissMessage) }
                item {
                    PlayerPanel(display, controller, onPlayPause, onStop, onPrevious, onNext,
                        onSeek, onVolume, onSpeed, onRepeat)
                }
                item {
                    LibraryHeader(library, onPickFiles, onCreatePlaylist, onRenamePlaylist,
                        onDeletePlaylist, onSelectPlaylist)
                }
                itemsIndexed(library.activePlaylist.tracks, key = { _, track -> track.uri }) { index, track ->
                    TrackRow(track, index, library.activePlaylist.tracks.size,
                        display.mediaId == track.uri, onSelectTrack, onRemoveTrack, onMoveTrack)
                }
                if (library.activePlaylist.tracks.isEmpty()) item { EmptyLibrary() }
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text("HN LOOP PLAYER", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Your media, on repeat.", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Offline playback for your MP3 and MP4 files.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MessageCard(message: String, dismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            Text("Dismiss", modifier = Modifier.clickable(onClick = dismiss).padding(8.dp),
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun LibraryHeader(
    library: Library,
    onPickFiles: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
) {
    var playlistMenu by remember { mutableStateOf(false) }
    var actionsMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PLAYLIST", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(library.activePlaylist.name, style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${library.activePlaylist.tracks.size} files",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { playlistMenu = true },
                        enabled = library.playlists.size > 1) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Choose playlist")
                    }
                    DropdownMenu(expanded = playlistMenu, onDismissRequest = { playlistMenu = false }) {
                        library.playlists.forEach { playlist ->
                            DropdownMenuItem(text = { Text(playlist.name) }, onClick = {
                                playlistMenu = false
                                onSelectPlaylist(playlist.id)
                            })
                        }
                    }
                }
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { actionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Playlist options")
                    }
                    DropdownMenu(expanded = actionsMenu, onDismissRequest = { actionsMenu = false }) {
                        DropdownMenuItem(text = { Text("New playlist") }, onClick = {
                            actionsMenu = false; dialog = "new"
                        })
                        DropdownMenuItem(text = { Text("Rename playlist") }, onClick = {
                            actionsMenu = false; dialog = "rename"
                        })
                        DropdownMenuItem(text = { Text("Delete playlist") },
                            enabled = library.playlists.size > 1, onClick = {
                                actionsMenu = false; dialog = "delete"
                            })
                    }
                }
            }
            Button(onClick = onPickFiles, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add MP3 or MP4 files")
            }
        }
    }

    if (dialog == "new" || dialog == "rename") {
        val mode = dialog
        var name by remember(mode) {
            mutableStateOf(if (mode == "rename") library.activePlaylist.name else "")
        }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (mode == "new") "New playlist" else "Rename playlist") },
            text = {
                OutlinedTextField(name, { name = it }, singleLine = true,
                    label = { Text("Playlist name") })
            },
            confirmButton = {
                Button(onClick = {
                    if (mode == "new") onCreatePlaylist(name) else onRenamePlaylist(name)
                    dialog = null
                }, enabled = name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
    if (dialog == "delete") {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete playlist?") },
            text = { Text("This removes the playlist from the app. Your files stay on your device.") },
            confirmButton = { Button(onClick = { onDeletePlaylist(); dialog = null }) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TrackRow(
    track: Track,
    index: Int,
    count: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var actionsMenu by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected)
            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, contentDescription = null, Modifier.padding(8.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                Text(if (track.isVideo) "MP4 video" else "MP3 audio",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { actionsMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options for ${track.title}")
                }
                DropdownMenu(expanded = actionsMenu, onDismissRequest = { actionsMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                        enabled = index > 0,
                        onClick = { actionsMenu = false; onMove(index, -1) },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                        enabled = index < count - 1,
                        onClick = { actionsMenu = false; onMove(index, 1) },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { actionsMenu = false; onRemove(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("No files yet", style = MaterialTheme.typography.titleMedium)
            Text("Add files with Android's system picker to build this playlist.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
@OptIn(UnstableApi::class)
private fun PlayerPanel(
    display: PlayerDisplay,
    controller: MediaController?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
) {
    var speedMenu by remember { mutableStateOf(false) }
    var seekDrag by remember(display.mediaId) { mutableStateOf<Float?>(null) }
    val duration = display.durationMs.coerceAtLeast(0)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(display.title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)

            if (display.isVideo && controller != null) {
                val context = LocalContext.current
                val view = remember(context) {
                    PlayerView(context).apply {
                        useController = false
                    }
                }
                AndroidView(factory = { view }, update = {
                    it.player = controller
                    it.keepScreenOn = display.isPlaying
                },
                    modifier = Modifier.fillMaxWidth().height(220.dp))
                androidx.compose.runtime.DisposableEffect(view) {
                    onDispose { view.player = null }
                }
            } else {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.MusicNote, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Slider(
                value = (seekDrag ?: display.positionMs.toFloat()).coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { seekDrag = it },
                onValueChangeFinished = {
                    seekDrag?.let { onSeek(it.toLong()) }
                    seekDrag = null
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = controller != null && duration > 0,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(display.positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, enabled = controller != null) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                FilledIconButton(onClick = onPlayPause, enabled = controller != null,
                    modifier = Modifier.size(56.dp)) {
                    Icon(if (display.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (display.isPlaying) "Pause" else "Play")
                }
                IconButton(onClick = onNext, enabled = controller != null) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
                IconButton(onClick = onStop, enabled = controller != null) {
                    Icon(Icons.Default.Stop, "Stop")
                }
            }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRepeat, enabled = controller != null) {
                    Text(when (display.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    })
                }
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { speedMenu = true }, enabled = controller != null) {
                        Text("${display.speed}× speed")
                    }
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(text = { Text("${speed}×") }, onClick = {
                                onSpeed(speed); speedMenu = false
                            })
                        }
                    }
                }
            }
            Text("Volume", style = MaterialTheme.typography.labelMedium)
            Slider(value = display.volume.coerceIn(0f, 1f), onValueChange = onVolume,
                enabled = controller != null)
            if (controller == null) {
                Text("Connecting to playback…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1_000
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}
