package com.hn.loopplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns playback independently of the activity, so audio survives rotation and app backgrounding. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var store: PlaylistStore

    override fun onCreate() {
        super.onCreate()
        store = PlaylistStore(this)
        val settings = store.loadSettings()
        val library = store.loadLibrary()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = settings.repeatMode
                volume = settings.volume
                playbackParameters = PlaybackParameters(settings.speed)
                val tracks = library.activePlaylist.tracks
                if (tracks.isNotEmpty()) {
                    setMediaItems(tracks.map(Track::toMediaItem))
                    seekTo(library.lastIndex.coerceIn(tracks.indices), 0L)
                }
                addListener(object : Player.Listener {
                    override fun onRepeatModeChanged(repeatMode: Int) = persistSettings()
                    override fun onVolumeChanged(volume: Float) = persistSettings()
                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = persistSettings()

                    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                        if (mediaItem != null) {
                            val current = store.loadLibrary()
                            store.saveLibrary(current.copy(lastIndex = currentMediaItemIndex.coerceAtLeast(0)))
                        }
                    }
                })
            }
        session = MediaSession.Builder(this, player).build()
    }

    private fun persistSettings() {
        store.saveSettings(PlaybackSettings(player.repeatMode, player.volume, player.playbackParameters.speed))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }
}
