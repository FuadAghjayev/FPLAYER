package az.iptv.fplayer.data.cache

import android.content.Context
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelContentType
import az.iptv.fplayer.data.model.ChannelGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChannelCacheStore(context: Context) {

    private val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
    private val legacyCacheFile = File(context.filesDir, CACHE_FILE_NAME)

    fun load(cacheKey: String): List<ChannelGroup>? = runCatching {
        val entryFile = entryFile(cacheKey)
        if (entryFile.exists()) {
            val entry = JSONObject(entryFile.readText())
            return entry.getJSONArray(KEY_GROUPS).toChannelGroups()
        }
        loadLegacy(cacheKey)
    }.getOrNull()

    fun save(cacheKey: String, groups: List<ChannelGroup>) {
        runCatching {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val entry = JSONObject()
                .put(KEY_CACHE_KEY, cacheKey)
                .put(KEY_CACHED_AT, System.currentTimeMillis())
                .put(KEY_GROUPS, groups.toGroupsJson())
            val entryFile = entryFile(cacheKey)
            val tmpFile = File(entryFile.parentFile, "${entryFile.name}.tmp")
            tmpFile.writeText(entry.toString())
            if (!tmpFile.renameTo(entryFile)) {
                entryFile.writeText(entry.toString())
                tmpFile.delete()
            }
            migrateLegacyEntries()
        }
    }

    // Köhnə birləşik cache faylındakı digər playlistləri per-key fayllara köçürüb sil
    private fun migrateLegacyEntries() {
        if (!legacyCacheFile.exists()) return
        runCatching {
            val root = JSONObject(legacyCacheFile.readText())
            val entries = root.optJSONObject(KEY_ENTRIES) ?: JSONObject()
            entries.keys().forEach { key ->
                val target = entryFile(key)
                if (!target.exists()) {
                    entries.optJSONObject(key)?.let { entry ->
                        entry.put(KEY_CACHE_KEY, key)
                        target.writeText(entry.toString())
                    }
                }
            }
        }
        legacyCacheFile.delete()
    }

    private fun loadLegacy(cacheKey: String): List<ChannelGroup>? {
        if (!legacyCacheFile.exists()) return null
        val root = JSONObject(legacyCacheFile.readText())
        root.optJSONObject(KEY_ENTRIES)?.optJSONObject(cacheKey)?.let { entry ->
            return entry.getJSONArray(KEY_GROUPS).toChannelGroups()
        }
        if (root.optString(KEY_CACHE_KEY) != cacheKey) return null
        return root.getJSONArray(KEY_GROUPS).toChannelGroups()
    }

    private fun entryFile(cacheKey: String): File {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(cacheKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        return File(cacheDir, "$digest.json")
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
                    .put(KEY_IS_ADULT, channel.isAdult)
                    .put(KEY_FRAME_RATE, channel.frameRate.toDouble())
                    .put(KEY_RATING, channel.rating.toDouble())
                    .put(KEY_CONTENT_TYPE, channel.contentType.name)
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
                    isAdult = obj.optBoolean(KEY_IS_ADULT, false),
                    frameRate = obj.optDouble(KEY_FRAME_RATE, 0.0).toFloat(),
                    rating = obj.optDouble(KEY_RATING, 0.0).toFloat(),
                    contentType = obj.optString(KEY_CONTENT_TYPE, ChannelContentType.TV.name)
                        .toChannelContentType()
                )
            )
        }
    }

    private fun String.toChannelContentType(): ChannelContentType =
        runCatching { ChannelContentType.valueOf(this) }.getOrDefault(ChannelContentType.TV)

    companion object {
        private const val CACHE_DIR_NAME = "channel_cache"
        private const val CACHE_FILE_NAME = "channel_cache.json"
        private const val KEY_ENTRIES = "entries"
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
        private const val KEY_IS_ADULT = "isAdult"
        private const val KEY_FRAME_RATE = "frameRate"
        private const val KEY_RATING = "rating"
        private const val KEY_CONTENT_TYPE = "contentType"
    }
}
