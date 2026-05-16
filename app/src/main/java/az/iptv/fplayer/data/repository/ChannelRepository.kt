package az.iptv.fplayer.data.repository

import android.content.Context
import az.iptv.fplayer.data.api.XtreamApi
import az.iptv.fplayer.data.cache.ChannelCacheStore
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import az.iptv.fplayer.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val cache = ChannelCacheStore(context.applicationContext)

    suspend fun loadM3u(url: String): Result<List<ChannelGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                M3uParser.parse(resp.body?.string() ?: error("Boş cavab")).also {
                    cache.save(m3uCacheKey(url), it)
                }
            }
        }
    }

    suspend fun loadCachedM3u(url: String): Result<List<ChannelGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            cache.load(m3uCacheKey(url)) ?: error("Cache yoxdur")
        }
    }

    suspend fun loadXtream(config: XtreamConfig): Result<List<ChannelGroup>> =
        withContext(Dispatchers.IO) {
            runCatching {
                XtreamApi.loadChannels(config, client).also {
                    cache.save(xtreamCacheKey(config), it)
                }
            }
        }

    suspend fun loadCachedXtream(config: XtreamConfig): Result<List<ChannelGroup>> =
        withContext(Dispatchers.IO) {
            runCatching {
                cache.load(xtreamCacheKey(config)) ?: error("Cache yoxdur")
            }
        }

    private fun m3uCacheKey(url: String): String = "M3U|${url.trim()}"

    private fun xtreamCacheKey(config: XtreamConfig): String =
        "XTREAM|${config.serverUrl.trim()}|${config.username.trim()}"
}
