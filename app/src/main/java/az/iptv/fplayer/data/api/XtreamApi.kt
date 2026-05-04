package az.iptv.fplayer.data.api

import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object XtreamApi {

    suspend fun loadChannels(config: XtreamConfig, client: OkHttpClient): List<ChannelGroup> =
        withContext(Dispatchers.IO) {
            val catMap = fetchCategories(config, client)
            val channels = fetchStreams(config, catMap, client)
            channels
                .groupBy { it.group.ifEmpty { "-" } }
                .map { (group, list) -> ChannelGroup(name = group, channels = list) }
        }

    private fun fetchCategories(config: XtreamConfig, client: OkHttpClient): Map<String, String> {
        val body = get(client, "${config.apiUrl}&action=get_live_categories")
        val arr = JSONArray(body)
        return buildMap {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                put(obj.optString("category_id"), obj.optString("category_name"))
            }
        }
    }

    private fun fetchStreams(
        config: XtreamConfig,
        catMap: Map<String, String>,
        client: OkHttpClient
    ): List<Channel> {
        val body = get(client, "${config.apiUrl}&action=get_live_streams")
        val arr = JSONArray(body)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val streamId = obj.optInt("stream_id", -1)
                if (streamId < 0) continue
                val catId = obj.optString("category_id")
                add(
                    Channel(
                        id = streamId.toString(),
                        name = obj.optString("name"),
                        url = config.streamUrl(streamId),
                        logoUrl = obj.optString("stream_icon"),
                        group = catMap[catId] ?: catId,
                        epgId = obj.optString("epg_channel_id")
                    )
                )
            }
        }
    }

    private fun get(client: OkHttpClient, url: String): String {
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) error("HTTP ${resp.code}")
        return resp.body?.string() ?: error("Boş cavab")
    }
}
