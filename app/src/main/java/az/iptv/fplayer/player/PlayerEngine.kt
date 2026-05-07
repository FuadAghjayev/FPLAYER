package az.iptv.fplayer.player

import android.view.SurfaceView
import kotlin.math.roundToInt

enum class PlayerType { EXOPLAYER, VLC }

enum class AudioDecoderMode { AUTO, HARDWARE, SOFTWARE }

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

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data object Playing : PlaybackState()
    data object Paused : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

interface PlayerEventListener {
    fun onStateChanged(state: PlaybackState)
    fun onVideoInfoChanged(info: VideoInfo)
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
}
