package az.iptv.fplayer.data.api

import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelContentType
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object XtreamApi {
    private const val MAX_SERIES_TO_EXPAND = 12
    private const val SERIES_INFO_TIMEOUT_SECONDS = 8L

    suspend fun loadChannels(config: XtreamConfig, client: OkHttpClient): List<ChannelGroup> =
        withContext(Dispatchers.IO) {
            val liveCategories = fetchCategories(config, client, "get_live_categories")
            val liveCatMap = liveCategories.associate { it.id to it.name }
            val liveChannels = fetchLiveStreams(config, liveCatMap, client)

            val movieCategories = fetchCategories(config, client, "get_vod_categories")
            val movieCatMap = movieCategories.associate { it.id to it.name }
            val movieChannels = fetchMovieStreams(config, movieCatMap, client)

            val seriesCategories = fetchCategories(config, client, "get_series_categories")
            val seriesCatMap = seriesCategories.associate { it.id to it.name }
            val seriesChannels = fetchSeriesEpisodes(config, seriesCatMap, client)

            groupChannels(liveCategories + movieCategories + seriesCategories, liveChannels + movieChannels + seriesChannels)
        }

    private fun fetchCategories(
        config: XtreamConfig,
        client: OkHttpClient,
        action: String
    ): List<XtreamCategory> = runCatching {
        val body = get(client, "${config.apiUrl}&action=$action")
        val arr = JSONArray(body)
        buildList {
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
    }.getOrDefault(emptyList())

    private fun fetchLiveStreams(
        config: XtreamConfig,
        catMap: Map<String, String>,
        client: OkHttpClient
    ): List<Channel> = runCatching {
        val body = get(client, "${config.apiUrl}&action=get_live_streams")
        val arr = JSONArray(body)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val streamId = obj.optInt("stream_id", -1)
                if (streamId < 0) continue
                val catId = obj.optString("category_id")
                add(
                    Channel(
                        id = streamId.toString(),
                        name = obj.optString("name"),
                        url = config.streamUrl(streamId, obj.optString("container_extension", "ts")),
                        logoUrl = obj.optString("stream_icon"),
                        group = catMap[catId] ?: catId,
                        epgId = obj.optString("epg_channel_id"),
                        frameRate = parseFrameRate(obj),
                        contentType = ChannelContentType.TV
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun fetchMovieStreams(
        config: XtreamConfig,
        catMap: Map<String, String>,
        client: OkHttpClient
    ): List<Channel> = runCatching {
        val body = get(client, "${config.apiUrl}&action=get_vod_streams")
        val arr = JSONArray(body)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val streamId = obj.optInt("stream_id", -1)
                if (streamId < 0) continue
                val catId = obj.optString("category_id")
                val extension = obj.optString("container_extension", "mp4")
                add(
                    Channel(
                        id = "movie_$streamId",
                        name = obj.optString("name"),
                        url = config.movieUrl(streamId, extension),
                        logoUrl = obj.optString("stream_icon"),
                        group = catMap[catId] ?: catId,
                        frameRate = parseFrameRate(obj),
                        contentType = ChannelContentType.MOVIE
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun fetchSeriesEpisodes(
        config: XtreamConfig,
        catMap: Map<String, String>,
        client: OkHttpClient
    ): List<Channel> = runCatching {
        val body = get(client, "${config.apiUrl}&action=get_series")
        val arr = JSONArray(body)
        buildList {
            for (i in 0 until minOf(arr.length(), MAX_SERIES_TO_EXPAND)) {
                val series = arr.getJSONObject(i)
                val seriesId = series.optInt("series_id", -1)
                if (seriesId < 0) continue
                val catId = series.optString("category_id")
                val group = catMap[catId] ?: catId
                val seriesName = series.optString("name")
                val cover = series.optString("cover")
                addAll(fetchEpisodesForSeries(config, client, seriesId, seriesName, group, cover))
            }
        }
    }.getOrDefault(emptyList())

    private fun fetchEpisodesForSeries(
        config: XtreamConfig,
        client: OkHttpClient,
        seriesId: Int,
        seriesName: String,
        group: String,
        cover: String
    ): List<Channel> = runCatching {
        val body = get(
            client = client,
            url = "${config.apiUrl}&action=get_series_info&series_id=$seriesId",
            timeoutSeconds = SERIES_INFO_TIMEOUT_SECONDS
        )
        val root = JSONObject(body)
        val episodes = root.opt("episodes")
        buildList {
            when (episodes) {
                is JSONObject -> {
                    episodes.keys().asSequence().forEach { season ->
                        val arr = episodes.optJSONArray(season) ?: return@forEach
                        addEpisodes(seriesId, seriesName, group, cover, season, arr, config)
                    }
                }
                is JSONArray -> addEpisodes(seriesId, seriesName, group, cover, "", episodes, config)
            }
        }
    }.getOrDefault(emptyList())

    private fun MutableList<Channel>.addEpisodes(
        seriesId: Int,
        seriesName: String,
        group: String,
        cover: String,
        season: String,
        episodes: JSONArray,
        config: XtreamConfig
    ) {
        for (i in 0 until episodes.length()) {
            val episode = episodes.optJSONObject(i) ?: continue
            val episodeId = episode.optInt("id", episode.optInt("episode_id", -1))
            if (episodeId < 0) continue
            val extension = episode.optString("container_extension", "mp4")
            val episodeNumber = episode.optInt("episode_num", i + 1)
            val title = episode.optString("title").ifBlank { episode.optString("name") }
            val prefix = episodePrefix(season, episodeNumber)
            add(
                Channel(
                    id = "series_${seriesId}_$episodeId",
                    name = listOf(seriesName, prefix, title).filter { it.isNotBlank() }.joinToString(" - "),
                    url = config.seriesUrl(episodeId, extension),
                    logoUrl = episode.optJSONObject("info")?.optString("movie_image")?.takeIf { it.isNotBlank() } ?: cover,
                    group = group,
                    contentType = ChannelContentType.SERIES
                )
            )
        }
    }

    private fun episodePrefix(season: String, episodeNumber: Int): String {
        val seasonNumber = season.filter { it.isDigit() }.toIntOrNull()
        return if (seasonNumber != null && episodeNumber > 0) {
            "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        } else if (episodeNumber > 0) {
            "E${episodeNumber.toString().padStart(2, '0')}"
        } else {
            ""
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

    private fun get(client: OkHttpClient, url: String, timeoutSeconds: Long? = null): String {
        val req = Request.Builder().url(url).build()
        val call = client.newCall(req)
        timeoutSeconds?.let { call.timeout().timeout(it, TimeUnit.SECONDS) }
        call.execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: error("Boş cavab")
        }
    }

    private data class XtreamCategory(
        val id: String,
        val name: String
    )
}
