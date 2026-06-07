package az.iptv.fplayer.player

import android.content.Context
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    private val context: Context,
    private val audioMode: AudioDecoderMode = AudioDecoderMode.AUTO
) : PlayerEngine {

    override val type = PlayerType.EXOPLAYER

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var listener: PlayerEventListener? = null
    private var surface: SurfaceView? = null
    private val trackRefs = mutableMapOf<String, TrackRef>()
    private val defaultHeaders = mapOf(
        "User-Agent" to DEFAULT_USER_AGENT,
        "Accept" to "*/*"
    )

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
                LIVE_MIN_BUFFER_MS,
                LIVE_MAX_BUFFER_MS,
                PLAYBACK_START_BUFFER_MS,
                REBUFFER_START_BUFFER_MS
            )
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context).setExtensionRendererMode(rendererMode)
        )
            .setMediaSourceFactory(mediaSourceFactory())
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exo ->
                exo.setVideoSurfaceView(surfaceView)
                exo.addListener(exoListener)
                exo.playWhenReady = true
                mediaSession = MediaSession.Builder(context, exo)
                    .setId(MEDIA_SESSION_ID)
                    .build()
            }
    }

    override fun play(url: String) {
        val exo = player ?: return
        val stream = StreamRequest.from(url)
        listener?.onStateChanged(PlaybackState.Buffering)
        listener?.onMediaTracksChanged(MediaTracks())
        trackRefs.clear()
        exo.stop()
        exo.setMediaSource(
            mediaSourceFactory(stream.headers)
                .createMediaSource(stream.toMediaItem())
        )
        exo.prepare()
    }

    override fun pause() { player?.pause() }
    override fun resume() { player?.play() }
    override fun stop() { player?.stop() }

    override fun release() {
        mediaSession?.release()
        mediaSession = null
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

    private fun mediaSourceFactory(extraHeaders: Map<String, String> = emptyMap()): DefaultMediaSourceFactory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(HTTP_CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_READ_TIMEOUT_MS)
            .setUserAgent(DEFAULT_USER_AGENT)
            .setDefaultRequestProperties(defaultHeaders + extraHeaders)
        return DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
    }

    private fun StreamRequest.toMediaItem(): MediaItem {
        val liveConfiguration = MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(LIVE_TARGET_OFFSET_MS)
            .setMinOffsetMs(LIVE_MIN_OFFSET_MS)
            .setMaxOffsetMs(LIVE_MAX_OFFSET_MS)
            .setMinPlaybackSpeed(LIVE_MIN_PLAYBACK_SPEED)
            .setMaxPlaybackSpeed(LIVE_MAX_PLAYBACK_SPEED)
            .build()

        return MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(liveConfiguration)
            .apply {
                if (url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()
    }

    private data class StreamRequest(
        val url: String,
        val headers: Map<String, String>
    ) {
        companion object {
            fun from(rawUrl: String): StreamRequest {
                val trimmed = rawUrl.trim()
                val separator = trimmed.indexOf('|')
                if (separator <= 0) return StreamRequest(trimmed, emptyMap())

                val cleanUrl = trimmed.substring(0, separator).trim()
                val headerText = trimmed.substring(separator + 1)
                val headers = headerText
                    .split('&', '|')
                    .mapNotNull { part ->
                        val key = part.substringBefore("=", missingDelimiterValue = "").trim()
                        if (key.isBlank()) return@mapNotNull null
                        val value = part.substringAfter("=", missingDelimiterValue = "").trim()
                        canonicalHeaderName(key)?.let { name -> name to decodeHeaderValue(value) }
                    }
                    .toMap()

                return StreamRequest(cleanUrl, headers)
            }

            private fun canonicalHeaderName(key: String): String? =
                when (key.lowercase()) {
                    "user-agent", "useragent", "ua", "http-user-agent" -> "User-Agent"
                    "referer", "referrer", "http-referrer", "http-referer" -> "Referer"
                    "origin", "http-origin" -> "Origin"
                    "cookie", "http-cookie" -> "Cookie"
                    "authorization" -> "Authorization"
                    else -> null
                }

            private fun decodeHeaderValue(value: String): String =
                runCatching {
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                }.getOrDefault(value)
        }
    }

    companion object {
        private const val DEFAULT_USER_AGENT = "FPLAYER/1.0 AndroidTV"
        private const val MEDIA_SESSION_ID = "FPLAYER_EXOPLAYER_SESSION"
        private const val LIVE_MIN_BUFFER_MS = 20_000
        private const val LIVE_MAX_BUFFER_MS = 70_000
        private const val PLAYBACK_START_BUFFER_MS = 2_500
        private const val REBUFFER_START_BUFFER_MS = 5_000
        private const val LIVE_TARGET_OFFSET_MS = 12_000L
        private const val LIVE_MIN_OFFSET_MS = 8_000L
        private const val LIVE_MAX_OFFSET_MS = 30_000L
        private const val LIVE_MIN_PLAYBACK_SPEED = 0.97f
        private const val LIVE_MAX_PLAYBACK_SPEED = 1.04f
        private const val HTTP_CONNECT_TIMEOUT_MS = 10_000
        private const val HTTP_READ_TIMEOUT_MS = 18_000
    }
}
