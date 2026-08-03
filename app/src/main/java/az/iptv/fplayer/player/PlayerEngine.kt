package az.iptv.fplayer.player

import android.view.SurfaceView
import kotlin.math.roundToInt

enum class PlayerType { EXOPLAYER, VLC }

enum class AudioDecoderMode { AUTO, HARDWARE, SOFTWARE }

data class PlaybackSettings(
    val audioMode: AudioDecoderMode = AudioDecoderMode.AUTO,
    val frameRateMatching: Boolean = false,
    val rawAudioConvert: Boolean = false,
    val tunneledPlayback: Boolean = false,
    val fix1080i: Boolean = false
)

data class VideoInfo(
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val codec: String = "",
) {
    val label: String get() = when {
        width >= 3840 -> "4K"
        width >= 1920 -> "FHD"
        width >= 1280 -> "HD"
        width > 0 -> "SD"
        else -> ""
    }
    val fpsLabel: String get() = if (frameRate > 0) "${frameRate.roundToInt()}fps" else ""
}

data class MediaTrackOption(
    val id: String,
    val label: String,
    val selected: Boolean = false
)

data class MediaTracks(
    val audioTracks: List<MediaTrackOption> = emptyList(),
    val subtitleTracks: List<MediaTrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = false
)

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data object Playing : PlaybackState()
    data object Paused : PlaybackState()
    data object Ended : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * Bəzi Xtream serverləri eyni kanalı yalnız bir formatda verir: birində `.ts`,
 * digərində `.m3u8` işləyir. İlk cəhd uğursuz olanda alternativ format sınanır.
 * Uyğun alternativ yoxdursa null qaytarır.
 */
fun alternateStreamUrl(rawUrl: String): String? {
    val headerSeparator = rawUrl.indexOf('|')
    val streamPart = if (headerSeparator > 0) rawUrl.substring(0, headerSeparator) else rawUrl
    val headerPart = if (headerSeparator > 0) rawUrl.substring(headerSeparator) else ""
    val query = streamPart.substringAfter('?', "")
    val path = streamPart.substringBefore('?')
    if (!path.contains("/live/", ignoreCase = true)) return null
    val swappedPath = when {
        path.endsWith(".m3u8", ignoreCase = true) -> path.dropLast(4) + "ts"
        path.endsWith(".ts", ignoreCase = true) -> path.dropLast(2) + "m3u8"
        else -> return null
    }
    return swappedPath + (if (query.isBlank()) "" else "?$query") + headerPart
}

interface PlayerEventListener {
    fun onStateChanged(state: PlaybackState)
    fun onVideoInfoChanged(info: VideoInfo)
    fun onMediaTracksChanged(tracks: MediaTracks) = Unit
}

interface PlayerEngine {
    val type: PlayerType
    fun init(surfaceView: SurfaceView)
    fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    fun release()
    fun setEventListener(listener: PlayerEventListener)
    fun selectAudioTrack(trackId: String) = Unit
    fun selectSubtitleTrack(trackId: String?) = Unit
}
