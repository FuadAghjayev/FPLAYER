package az.iptv.fplayer.data.repository

import az.iptv.fplayer.data.api.XtreamApi
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import az.iptv.fplayer.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun loadM3u(url: String): Result<List<ChannelGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            M3uParser.parse(resp.body?.string() ?: error("Boş cavab"))
        }
    }

    suspend fun loadXtream(config: XtreamConfig): Result<List<ChannelGroup>> =
        withContext(Dispatchers.IO) {
            runCatching { XtreamApi.loadChannels(config, client) }
        }
}
