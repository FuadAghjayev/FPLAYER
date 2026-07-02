package az.iptv.fplayer.data.repository

import android.content.Context
import az.iptv.fplayer.data.api.XtreamApi
import az.iptv.fplayer.data.cache.ChannelCacheStore
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.ProgramInfo
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
        .readTimeout(75, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val cache = ChannelCacheStore(context.applicationContext)

    suspend fun loadM3u(url: String): Result<List<ChannelGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .header("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Bos cavab")
                val expectedLength = body.contentLength()
                val bytes = body.bytes()
                if (expectedLength > 0 && bytes.size < expectedLength) {
                    error("M3U cavabi yarim endi")
                }
                val content = String(bytes, Charsets.UTF_8)
                M3uParser.parse(content).also { groups ->
                    if (groups.sumOf { it.channels.size } == 0) error("M3U kanal tapilmadi")
                    cache.save(m3uCacheKey(url), groups)
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

    suspend fun loadXtreamProgram(config: XtreamConfig, streamId: Int): Result<ProgramInfo?> =
        withContext(Dispatchers.IO) {
            runCatching {
                XtreamApi.fetchCurrentProgram(config, streamId, client)
            }
        }

    private fun m3uCacheKey(url: String): String = "M3U_V3|${url.trim()}"

    private fun xtreamCacheKey(config: XtreamConfig): String =
        "XTREAM|${config.serverUrl.trim()}|${config.username.trim()}"
}
