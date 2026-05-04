package az.iptv.fplayer.player

import android.content.Context
import android.view.SurfaceView

// LibVLC stub — to enable real VLC support:
// 1. Download libvlc-all-3.6.0.aar from https://nightlies.videolan.org/vlc-android/
// 2. Place the .aar file in app/libs/
// 3. Uncomment the fileTree line in app/build.gradle.kts
// 4. Replace this file with VlcPlayerEngine_Full.kt (kept in docs/)
class VlcPlayerEngine(context: Context) : PlayerEngine {

    override val type = PlayerType.VLC
    private var listener: PlayerEventListener? = null

    override fun init(surfaceView: SurfaceView) = Unit

    override fun play(url: String) {
        listener?.onStateChanged(
            PlaybackState.Error("VLC kütüphanesi eklenmemiş. ExoPlayer'a geçin.")
        )
    }

    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = Unit
    override fun release() = Unit

    override fun setEventListener(listener: PlayerEventListener) {
        this.listener = listener
    }
}
