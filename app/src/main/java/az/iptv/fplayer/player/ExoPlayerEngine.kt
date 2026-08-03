package az.iptv.fplayer.player

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Surface
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
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.session.MediaSession
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    private val context: Context,
    private val settings: PlaybackSettings = PlaybackSettings()
) : PlayerEngine {

    private val audioMode: AudioDecoderMode get() = settings.audioMode

    override val type = PlayerType.EXOPLAYER

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var listener: PlayerEventListener? = null
    private var surface: SurfaceView? = null
    private var autoAudioSelectionAttempted = false
    private var audioDisabledRecoveryAttempted = false
    // init() bitməmiş gələn oxutma tələbi itməsin deyə saxlanılır
    private var pendingUrl: String? = null
    private val trackRefs = mutableMapOf<String, TrackRef>()
    private val defaultHeaders = mapOf(
        "User-Agent" to DEFAULT_USER_AGENT,
        "Accept" to "*/*"
    )

    override fun init(surfaceView: SurfaceView) {
        surface = surfaceView

        val rendererMode = when {
            // RAW səs çevirmə: passthrough əvəzinə proqram dekoderi ilə PCM-ə çevrilir
            settings.rawAudioConvert -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            audioMode == AudioDecoderMode.HARDWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
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

        val renderersFactory = buildRenderersFactory()
            .setExtensionRendererMode(rendererMode)
            .setEnableDecoderFallback(true)
        if (settings.fix1080i) {
            // Android 14-də 1080i axınlarında sinxron MediaCodec sırası kadr düşməsinə səbəb olur
            renderersFactory.forceEnableMediaCodecAsynchronousQueueing()
        }

        val trackSelector = DefaultTrackSelector(context)
        if (settings.tunneledPlayback) {
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setTunnelingEnabled(true)
                .build()
        }

        player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory())
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exo ->
                exo.setVideoSurfaceView(surfaceView)
                exo.addListener(exoListener)
                exo.volume = 1f
                exo.playWhenReady = true
                mediaSession = MediaSession.Builder(context, exo)
                    .setId(MEDIA_SESSION_ID)
                    .build()
            }

        // Səth hazır olmamışdan əvvəl gələn tələb indi icra olunur
        pendingUrl?.let { url ->
            pendingUrl = null
            play(url)
        }
    }

    private fun buildRenderersFactory(): DefaultRenderersFactory =
        if (settings.rawAudioConvert) {
            object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink = DefaultAudioSink.Builder(context)
                    .setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        } else {
            DefaultRenderersFactory(context)
        }

    private fun applyFrameRateMatching(frameRate: Float) {
        if (!settings.frameRateMatching || settings.tunneledPlayback) return
        if (frameRate <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val holderSurface = surface?.holder?.surface?.takeIf { it.isValid } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                holderSurface.setFrameRate(
                    frameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS
                )
            } else {
                holderSurface.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
            }
        }
    }

    override fun play(url: String) {
        val stream = StreamRequest.from(url)
        Log.d(
            LOG_TAG,
            "play request=${stream.debugLabel()} hls=${stream.isLikelyHls()} audioMode=$audioMode"
        )
        listener?.onStateChanged(PlaybackState.Buffering)
        listener?.onMediaTracksChanged(MediaTracks())
        trackRefs.clear()
        autoAudioSelectionAttempted = false
        audioDisabledRecoveryAttempted = false

        val exo = player
        if (exo == null) {
            // Mühərrik hələ qurulmayıb — tələb yaddaşda saxlanır və init()-də oxudulur
            Log.w(LOG_TAG, "play deferred: player not initialised yet")
            pendingUrl = url
            return
        }
        pendingUrl = null

        exo.stop()
        // Köhnə axının yükləyicisi/soketi tam bağlanmasa, bir bağlantı limitli
        // serverlər yeni tələbi rədd edir (kanala geri qayıdanda "açılmır" problemi)
        exo.clearMediaItems()
        exo.volume = 1f
        exo.playWhenReady = true
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
        exo.setMediaSource(stream.toMediaSource())
        exo.prepare()
    }

    override fun pause() { player?.pause() }
    override fun resume() { player?.play() }
    override fun stop() {
        pendingUrl = null
        player?.stop()
    }

    override fun release() {
        pendingUrl = null
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
                Player.STATE_ENDED -> PlaybackState.Ended
                else -> PlaybackState.Idle
            }
            Log.d(
                LOG_TAG,
                "state=${stateName(state)} mapped=$s playWhenReady=${player?.playWhenReady} " +
                    "bufferedMs=${player?.bufferedPosition} positionMs=${player?.currentPosition}"
            )
            listener?.onStateChanged(s)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                LOG_TAG,
                "playerError code=${error.errorCode} name=${error.errorCodeName} " +
                    "message=${error.message} cause=${error.cause?.javaClass?.simpleName}: ${error.cause?.message}",
                error
            )
            if (tryRecoverWithoutAudio(error)) return
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
        val frameRate = format?.frameRate?.takeIf { it > 0f } ?: 0f
        applyFrameRateMatching(frameRate)
        listener?.onVideoInfoChanged(
            VideoInfo(
                width = size.width,
                height = size.height,
                frameRate = frameRate,
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

        if (audio.isNotEmpty() && audio.none { it.selected } && !autoAudioSelectionAttempted) {
            autoAudioSelectionAttempted = true
            Log.d(LOG_TAG, "autoSelectAudio first=${audio.first().label} options=${audio.size}")
            selectAudioTrack(audio.first().id)
            return
        }

        listener?.onMediaTracksChanged(
            MediaTracks(
                audioTracks = audio,
                subtitleTracks = subtitles,
                subtitlesEnabled = selectedSubtitle && !exo.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            )
        )
    }

    private fun tryRecoverWithoutAudio(error: PlaybackException): Boolean {
        val exo = player ?: return false
        if (audioDisabledRecoveryAttempted || !isLikelyAudioDecoderError(error)) return false

        audioDisabledRecoveryAttempted = true
        Log.w(
            LOG_TAG,
            "recoverWithoutAudio code=${error.errorCode} name=${error.errorCodeName} message=${error.message}"
        )
        exo.trackSelectionParameters = exo.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        listener?.onStateChanged(PlaybackState.Buffering)
        listener?.onMediaTracksChanged(MediaTracks())
        exo.prepare()
        exo.play()
        return true
    }

    private fun isLikelyAudioDecoderError(error: PlaybackException): Boolean {
        val parts = mutableListOf<String>()
        var throwable: Throwable? = error
        var depth = 0
        while (throwable != null && depth < 6) {
            parts += throwable.javaClass.name
            throwable.message?.let(parts::add)
            throwable = throwable.cause
            depth += 1
        }
        val text = parts.joinToString(" ").lowercase()

        return "audio/mpeg-l2" in text ||
            "mpeg-l2" in text ||
            "mpeg audio layer 2" in text ||
            ("audio" in text && "decoder" in text)
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

    private fun dataSourceFactory(extraHeaders: Map<String, String> = emptyMap()): DefaultDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(HTTP_CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_READ_TIMEOUT_MS)
            .setUserAgent(DEFAULT_USER_AGENT)
            .setDefaultRequestProperties(defaultHeaders + extraHeaders)
        return DefaultDataSource.Factory(context, httpFactory)
    }

    private fun mediaSourceFactory(extraHeaders: Map<String, String> = emptyMap()): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(dataSourceFactory(extraHeaders))
    }

    private fun StreamRequest.toMediaSource(): MediaSource {
        val mediaItem = toMediaItem()
        if (!isLikelyHls()) {
            return mediaSourceFactory(headers).createMediaSource(mediaItem)
        }
        return HlsMediaSource.Factory(dataSourceFactory(headers))
            .setExtractorFactory(tolerantHlsExtractorFactory())
            .setAllowChunklessPreparation(false)
            .createMediaSource(mediaItem)
    }

    private fun tolerantHlsExtractorFactory(): DefaultHlsExtractorFactory =
        DefaultHlsExtractorFactory(
            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS,
            /* exposeCea608WhenMissingDeclarations= */ true
        )

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
                if (isLikelyHls()) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()
    }

    private fun StreamRequest.isLikelyHls(): Boolean {
        val urlWithoutQuery = url.substringBefore('?').substringBefore('#')
        return urlWithoutQuery.endsWith(".m3u8", ignoreCase = true) ||
            url.contains("m3u8", ignoreCase = true)
    }

    private fun StreamRequest.debugLabel(): String {
        val safeUrl = url
            .replace(Regex("(?i)(username|user|password|pass|token|auth|key)=([^&]+)")) {
                "${it.groupValues[1]}=***"
            }
            .take(260)
        return "url=$safeUrl headerNames=${headers.keys.sorted()}"
    }

    private fun stateName(state: Int): String =
        when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN_$state"
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
        private const val LOG_TAG = "FPLAYER_EXO"
        private const val DEFAULT_USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"
        private const val MEDIA_SESSION_ID = "FPLAYER_EXOPLAYER_SESSION"
        private const val LIVE_MIN_BUFFER_MS = 12_000
        private const val LIVE_MAX_BUFFER_MS = 45_000
        private const val PLAYBACK_START_BUFFER_MS = 1_500
        private const val REBUFFER_START_BUFFER_MS = 3_000
        private const val LIVE_TARGET_OFFSET_MS = 9_000L
        private const val LIVE_MIN_OFFSET_MS = 5_000L
        private const val LIVE_MAX_OFFSET_MS = 22_000L
        private const val LIVE_MIN_PLAYBACK_SPEED = 0.97f
        private const val LIVE_MAX_PLAYBACK_SPEED = 1.04f
        private const val HTTP_CONNECT_TIMEOUT_MS = 12_000
        private const val HTTP_READ_TIMEOUT_MS = 20_000
    }
}
