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
import androidx.media3.common.VideoSize
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

        player = ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context).setExtensionRendererMode(rendererMode)
        )
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
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
            if (videoSize.width == 0) return
            val format = player?.videoFormat
            listener?.onVideoInfoChanged(
                VideoInfo(
                    width = videoSize.width,
                    height = videoSize.height,
                    frameRate = format?.frameRate?.takeIf { it > 0f } ?: 0f,
                    codec = format?.sampleMimeType?.substringAfterLast("/") ?: ""
                )
            )
        }
    }
}
