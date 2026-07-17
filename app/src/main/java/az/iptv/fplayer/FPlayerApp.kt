package az.iptv.fplayer

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * TV qutuları üçün yaddaş dostu şəkil yükləyicisi: RGB_565 loqolar üçün kifayətdir,
 * disk keşi təkrar açılışlarda şəbəkə sorğularını aradan qaldırır.
 */
class FPlayerApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(false)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("channel_images"))
                    .maxSizeBytes(96L * 1024 * 1024)
                    .build()
            }
            .build()
}
