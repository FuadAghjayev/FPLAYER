package az.iptv.fplayer.data.api

import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object XtreamApi {

    suspend fun loadChannels(config: XtreamConfig, client: OkHttpClient): List<ChannelGroup> =
        withContext(Dispatchers.IO) {
            val categories = fetchCategories(config, client)
            val catMap = categories.associate { it.id to it.name }
            val channels = fetchStreams(config, catMap, client)
            groupChannels(categories, channels)
        }

    private fun fetchCategories(config: XtreamConfig, client: OkHttpClient): List<XtreamCategory> {
        val body = get(client, "${config.apiUrl}&action=get_live_categories")
        val arr = JSONArray(body)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    XtreamCategory(
                        id = obj.optString("category_id"),
                        name = obj.optString("category_name").ifEmpty { "-" }
                    )
                )
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
                        epgId = obj.optString("epg_channel_id"),
                        frameRate = parseFrameRate(obj)
                    )
                )
            }
        }
    }

    private fun groupChannels(
        categories: List<XtreamCategory>,
        channels: List<Channel>
    ): List<ChannelGroup> {
        val grouped = linkedMapOf<String, MutableList<Channel>>()
        channels.forEach { channel ->
            grouped.getOrPut(channel.group.ifEmpty { "-" }) { mutableListOf() }.add(channel)
        }

        val ordered = mutableListOf<ChannelGroup>()
        categories.forEach { category ->
            grouped.remove(category.name)?.let { ordered.add(ChannelGroup(category.name, it)) }
        }
        grouped.forEach { (group, list) -> ordered.add(ChannelGroup(group, list)) }
        return ordered
    }

    private fun parseFrameRate(obj: JSONObject): Float {
        val keys = listOf("fps", "frame_rate", "framerate", "stream_fps")
        return keys.firstNotNullOfOrNull { key ->
            obj.optString(key, "").toPositiveFrameRate()
        } ?: 0f
    }

    private fun String.toPositiveFrameRate(): Float? {
        val value = Regex("""\d+(?:\.\d+)?""").find(this)?.value?.toFloatOrNull()
        return value?.takeIf { it > 0f }
    }

    private fun get(client: OkHttpClient, url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: error("Boş cavab")
        }
    }

    private data class XtreamCategory(
        val id: String,
        val name: String
    )
}
