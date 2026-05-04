// Full VLC implementation — requires libvlc-all.aar in app/libs/
// Replace app/src/.../player/VlcPlayerEngine.kt with this file after adding the AAR.

package az.iptv.fplayer.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerEngine(private val context: Context) : PlayerEngine {

    override val type = PlayerType.VLC

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var listener: PlayerEventListener? = null

    override fun init(surfaceView: SurfaceView) {
        libVlc = LibVLC(context, arrayListOf(
            "--no-drop-late-frames",
            "--no-late-frames",
            "--aout=opensles",
            "--audio-time-stretch"
        ))
        mediaPlayer = MediaPlayer(libVlc).also { mp ->
            mp.vlcVout.apply {
                setVideoSurface(surfaceView.holder.surface, surfaceView.holder)
                attachViews()
            }
            mp.setEventListener(vlcEventListener)
        }
    }

    override fun play(url: String) {
        val mp = mediaPlayer ?: return
        val vlc = libVlc ?: return
        listener?.onStateChanged(PlaybackState.Buffering)
        val media = Media(vlc, Uri.parse(url)).apply {
            setHWDecoderEnabled(true, false)
        }
        mp.media = media
        media.release()
        mp.play()
    }

    override fun pause() { mediaPlayer?.pause() }
    override fun resume() { mediaPlayer?.play() }
    override fun stop() { mediaPlayer?.stop() }

    override fun release() {
        mediaPlayer?.setEventListener(null)
        mediaPlayer?.vlcVout?.detachViews()
        mediaPlayer?.release()
        libVlc?.release()
        mediaPlayer = null
        libVlc = null
    }

    override fun setEventListener(listener: PlayerEventListener) {
        this.listener = listener
    }

    private val vlcEventListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Buffering -> listener?.onStateChanged(PlaybackState.Buffering)
            MediaPlayer.Event.Playing -> {
                listener?.onStateChanged(PlaybackState.Playing)
                reportVideoInfo()
            }
            MediaPlayer.Event.Paused -> listener?.onStateChanged(PlaybackState.Paused)
            MediaPlayer.Event.Stopped -> listener?.onStateChanged(PlaybackState.Idle)
            MediaPlayer.Event.EncounteredError -> listener?.onStateChanged(PlaybackState.Error("VLC playback error"))
        }
    }

    private fun reportVideoInfo() {
        try {
            val mp = mediaPlayer ?: return
            val tracks = mp.media?.tracks ?: return
            val vt = tracks.firstOrNull { it.type == Media.Track.Type.Video } as? Media.VideoTrack ?: return
            listener?.onVideoInfoChanged(
                VideoInfo(
                    width = vt.width,
                    height = vt.height,
                    frameRate = if (vt.frameRateDen > 0) vt.frameRateNum.toFloat() / vt.frameRateDen else 0f
                )
            )
        } catch (_: Exception) {}
    }
}
