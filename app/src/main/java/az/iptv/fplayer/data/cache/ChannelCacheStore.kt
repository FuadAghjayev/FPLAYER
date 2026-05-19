package az.iptv.fplayer.data.cache

import android.content.Context
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChannelCacheStore(context: Context) {

    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

    fun load(cacheKey: String): List<ChannelGroup>? = runCatching {
        if (!cacheFile.exists()) return null

        val root = JSONObject(cacheFile.readText())
        if (root.optString(KEY_CACHE_KEY) != cacheKey) return null

        root.getJSONArray(KEY_GROUPS).toChannelGroups()
    }.getOrNull()

    fun save(cacheKey: String, groups: List<ChannelGroup>) {
        val root = JSONObject()
            .put(KEY_CACHE_KEY, cacheKey)
            .put(KEY_CACHED_AT, System.currentTimeMillis())
            .put(KEY_GROUPS, groups.toGroupsJson())

        cacheFile.writeText(root.toString())
    }

    private fun List<ChannelGroup>.toGroupsJson(): JSONArray = JSONArray().also { arr ->
        forEach { group ->
            arr.put(
                JSONObject()
                    .put(KEY_NAME, group.name)
                    .put(KEY_CHANNELS, group.channels.toChannelsJson())
            )
        }
    }

    private fun List<Channel>.toChannelsJson(): JSONArray = JSONArray().also { arr ->
        forEach { channel ->
            arr.put(
                JSONObject()
                    .put(KEY_ID, channel.id)
                    .put(KEY_NAME, channel.name)
                    .put(KEY_URL, channel.url)
                    .put(KEY_LOGO_URL, channel.logoUrl)
                    .put(KEY_GROUP, channel.group)
                    .put(KEY_EPG_ID, channel.epgId)
                    .put(KEY_IS_FAVORITE, channel.isFavorite)
                    .put(KEY_FRAME_RATE, channel.frameRate.toDouble())
            )
        }
    }

    private fun JSONArray.toChannelGroups(): List<ChannelGroup> = buildList {
        for (i in 0 until length()) {
            val obj = getJSONObject(i)
            add(
                ChannelGroup(
                    name = obj.optString(KEY_NAME, "-"),
                    channels = obj.getJSONArray(KEY_CHANNELS).toChannels()
                )
            )
        }
    }

    private fun JSONArray.toChannels(): List<Channel> = buildList {
        for (i in 0 until length()) {
            val obj = getJSONObject(i)
            add(
                Channel(
                    id = obj.optString(KEY_ID),
                    name = obj.optString(KEY_NAME),
                    url = obj.optString(KEY_URL),
                    logoUrl = obj.optString(KEY_LOGO_URL),
                    group = obj.optString(KEY_GROUP),
                    epgId = obj.optString(KEY_EPG_ID),
                    isFavorite = obj.optBoolean(KEY_IS_FAVORITE, false),
                    frameRate = obj.optDouble(KEY_FRAME_RATE, 0.0).toFloat()
                )
            )
        }
    }

    companion object {
        private const val CACHE_FILE_NAME = "channel_cache.json"
        private const val KEY_CACHE_KEY = "cacheKey"
        private const val KEY_CACHED_AT = "cachedAt"
        private const val KEY_GROUPS = "groups"
        private const val KEY_CHANNELS = "channels"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_LOGO_URL = "logoUrl"
        private const val KEY_GROUP = "group"
        private const val KEY_EPG_ID = "epgId"
        private const val KEY_IS_FAVORITE = "isFavorite"
        private const val KEY_FRAME_RATE = "frameRate"
    }
}
