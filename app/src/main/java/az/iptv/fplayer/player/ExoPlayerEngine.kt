package az.iptv.fplayer.player

import android.content.Context
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    private val context: Context,
    private val audioMode: AudioDecoderMode = AudioDecoderMode.AUTO
) : PlayerEngine {

    override val type = PlayerType.EXOPLAYER

    private var player: ExoPlayer? = null
    private var listener: PlayerEventListener? = null
    private var surface: SurfaceView? = null
    private val trackRefs = mutableMapOf<String, TrackRef>()

    override fun init(surfaceView: SurfaceView) {
        surface = surfaceView

        val rendererMode = when (audioMode) {
            AudioDecoderMode.SOFTWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            AudioDecoderMode.HARDWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            AudioDecoderMode.AUTO     -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                50_000,
                1_500,
                3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context).setExtensionRendererMode(rendererMode)
        )
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exo ->
                exo.setVideoSurfaceView(surfaceView)
                exo.addListener(exoListener)
                exo.playWhenReady = true
            }
    }

    override fun play(url: String) {
        val exo = player ?: return
        listener?.onStateChanged(PlaybackState.Buffering)
        listener?.onMediaTracksChanged(MediaTracks())
        trackRefs.clear()
        exo.stop()
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
    }

    override fun pause() { player?.pause() }
    override fun resume() { player?.play() }
    override fun stop() { player?.stop() }

    override fun release() {
        player?.removeListener(exoListener)
        player?.release()
        player = null
    }

    override fun setEventListener(listener: PlayerEventListener) {
        this.listener = listener
    }

    override fun selectAudioTrack(trackId: String) {
        val exo = player ?: return
        val ref = trackRefs[trackId] ?: return
        if (ref.type != C.TRACK_TYPE_AUDIO) return
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(ref.group, ref.trackIndex))
            .build()
        emitMediaTracks()
    }

    override fun selectSubtitleTrack(trackId: String?) {
        val exo = player ?: return
        val builder = exo.trackSelectionParameters.buildUpon()
        if (trackId == null) {
            exo.trackSelectionParameters = builder
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            emitMediaTracks()
            return
        }

        val ref = trackRefs[trackId] ?: return
        if (ref.type != C.TRACK_TYPE_TEXT) return
        exo.trackSelectionParameters = builder
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(ref.group, ref.trackIndex))
            .build()
        emitMediaTracks()
    }

    private val exoListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val s = when (state) {
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> if (player?.playWhenReady == true) PlaybackState.Playing else PlaybackState.Paused
                Player.STATE_ENDED -> PlaybackState.Idle
                else -> PlaybackState.Idle
            }
            listener?.onStateChanged(s)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onStateChanged(PlaybackState.Error(error.message ?: "Unknown error"))
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            emitVideoInfo(videoSize)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.contains(Player.EVENT_TRACKS_CHANGED) ||
                events.contains(Player.EVENT_VIDEO_SIZE_CHANGED)
            ) {
                emitVideoInfo()
                emitMediaTracks()
            }
        }
    }

    private fun emitVideoInfo(videoSize: VideoSize? = null) {
        val exo = player ?: return
        val size = videoSize ?: exo.videoSize
        if (size.width == 0) return
        val format = exo.videoFormat
        listener?.onVideoInfoChanged(
            VideoInfo(
                width = size.width,
                height = size.height,
                frameRate = format?.frameRate?.takeIf { it > 0f } ?: 0f,
                codec = format?.sampleMimeType?.substringAfterLast("/") ?: ""
            )
        )
    }

    private fun emitMediaTracks() {
        val exo = player ?: return
        val tracks = exo.currentTracks
        trackRefs.clear()
        val audio = mutableListOf<MediaTrackOption>()
        val subtitles = mutableListOf<MediaTrackOption>()
        var selectedSubtitle = false

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                val id = "${group.type}:$groupIndex:$trackIndex"
                trackRefs[id] = TrackRef(group.mediaTrackGroup, trackIndex, group.type)
                val selected = group.isTrackSelected(trackIndex)
                val option = MediaTrackOption(
                    id = id,
                    label = formatTrackLabel(group, trackIndex),
                    selected = selected
                )
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    audio += option
                } else {
                    subtitles += option
                    selectedSubtitle = selectedSubtitle || selected
                }
            }
        }

        listener?.onMediaTracksChanged(
            MediaTracks(
                audioTracks = audio,
                subtitleTracks = subtitles,
                subtitlesEnabled = selectedSubtitle && !exo.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            )
        )
    }

    private fun formatTrackLabel(group: Tracks.Group, trackIndex: Int): String {
        val format = group.getTrackFormat(trackIndex)
        val language = format.language?.takeIf { it.isNotBlank() && it != "und" }?.uppercase()
        val label = format.label?.takeIf { it.isNotBlank() }
        val role = when (group.type) {
            C.TRACK_TYPE_AUDIO -> "Audio"
            C.TRACK_TYPE_TEXT -> "Sub"
            else -> "Track"
        }
        return listOfNotNull(label, language).firstOrNull()
            ?: "$role ${trackIndex + 1}"
    }

    private data class TrackRef(
        val group: TrackGroup,
        val trackIndex: Int,
        val type: Int
    )
}
