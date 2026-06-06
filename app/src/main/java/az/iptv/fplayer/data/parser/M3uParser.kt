package az.iptv.fplayer.data.parser

import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelContentType
import az.iptv.fplayer.data.model.ChannelGroup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object M3uParser {

    fun parse(content: String): List<ChannelGroup> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                var j = i + 1
                val headers = linkedMapOf<String, String>()
                while (j < lines.size) {
                    val nextLine = lines[j].trim()
                    when {
                        nextLine.isBlank() -> j++
                        nextLine.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                            parseVlcOption(nextLine)?.let { (key, value) -> headers[key] = value }
                            j++
                        }
                        nextLine.startsWith("#KODIPROP:", ignoreCase = true) -> {
                            headers.putAll(parseKodiProperty(nextLine))
                            j++
                        }
                        nextLine.startsWith("#") -> j++
                        else -> break
                    }
                }
                val url = lines.getOrNull(j)?.trim()
                if (!url.isNullOrEmpty() && !url.startsWith("#")) {
                    val channel = parseExtInf(line, appendHeaders(url, headers))
                    channels.add(channel)
                    i = j + 1
                    continue
                }
            }
            i++
        }

        return groupChannels(channels)
    }

    private fun parseExtInf(extinf: String, url: String): Channel {
        val name = extinf.substringAfterLast(",").trim()
        val attrs = parseAttributes(extinf)
        val id = attrs["tvg-id"] ?: attrs["tvg-name"] ?: name
        return Channel(
            id = id,
            name = name,
            url = url,
            logoUrl = attrs["tvg-logo"] ?: "",
            group = attrs["group-title"] ?: "",
            epgId = attrs["tvg-id"] ?: "",
            frameRate = parseFrameRate(attrs),
            contentType = classifyContentType(name, url, attrs)
        )
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val attrSection = line.substringAfter("#EXTINF:").substringBefore(",")
        val regex = Regex("""(\w[\w-]*)="([^"]*?)"""")
        regex.findAll(attrSection).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }

    private fun parseVlcOption(line: String): Pair<String, String>? {
        val option = line.substringAfter("#EXTVLCOPT:", "").trim()
        val key = option.substringBefore("=", "").trim()
        val value = option.substringAfter("=", "").trim()
        if (key.isBlank() || value.isBlank()) return null
        val header = when (key.lowercase()) {
            "http-user-agent" -> "User-Agent"
            "http-referrer", "http-referer" -> "Referer"
            "http-origin" -> "Origin"
            "http-cookie" -> "Cookie"
            else -> return null
        }
        return header to value
    }

    private fun parseKodiProperty(line: String): Map<String, String> {
        val property = line.substringAfter("#KODIPROP:", "").trim()
        if (!property.startsWith("inputstream.adaptive.stream_headers=", ignoreCase = true)) return emptyMap()
        val headers = property.substringAfter("=", "").trim()
        return headers
            .split('&')
            .mapNotNull { part ->
                val key = part.substringBefore("=", "").trim()
                val value = part.substringAfter("=", "").trim()
                canonicalHeaderName(key)?.takeIf { value.isNotBlank() }?.let { it to value }
            }
            .toMap()
    }

    private fun appendHeaders(url: String, headers: Map<String, String>): String {
        if (headers.isEmpty()) return url
        val separator = if (url.contains('|')) "&" else "|"
        val suffix = headers.entries.joinToString("&") { (key, value) ->
            "$key=${encodeHeaderValue(value)}"
        }
        return "$url$separator$suffix"
    }

    private fun canonicalHeaderName(key: String): String? =
        when (key.lowercase()) {
            "user-agent", "useragent", "ua", "http-user-agent" -> "User-Agent"
            "referer", "referrer", "http-referrer", "http-referer" -> "Referer"
            "origin", "http-origin" -> "Origin"
            "cookie", "http-cookie" -> "Cookie"
            "authorization" -> "Authorization"
            else -> null
        }

    private fun encodeHeaderValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun parseFrameRate(attrs: Map<String, String>): Float {
        val keys = listOf("fps", "frame-rate", "frame_rate", "framerate", "tvg-fps")
        return keys.firstNotNullOfOrNull { key ->
            attrs[key]?.toPositiveFrameRate()
        } ?: 0f
    }

    private fun classifyContentType(
        name: String,
        url: String,
        attrs: Map<String, String>
    ): ChannelContentType {
        val haystack = buildString {
            append(name)
            append(' ')
            append(url)
            append(' ')
            append(attrs["group-title"] ?: "")
            append(' ')
            append(attrs["tvg-name"] ?: "")
        }.lowercase()

        val seriesWords = listOf(
            "series", "serial", "serials", "dizi", "diziler", "dizilər", "show", "shows",
            "season", "sezon", "saison", "s01", "s02", "episode", "epizod"
        )
        val movieWords = listOf(
            "movie", "movies", "film", "films", "kino", "sinema", "cinema", "vod",
            "video club", "videoclub", "pelicula", "peliculas"
        )

        return when {
            seriesWords.any { haystack.contains(it) } -> ChannelContentType.SERIES
            movieWords.any { haystack.contains(it) } -> ChannelContentType.MOVIE
            else -> ChannelContentType.TV
        }
    }

    private fun String.toPositiveFrameRate(): Float? {
        val value = Regex("""\d+(?:\.\d+)?""").find(this)?.value?.toFloatOrNull()
        return value?.takeIf { it > 0f }
    }

    private fun groupChannels(channels: List<Channel>): List<ChannelGroup> {
        val grouped = linkedMapOf<String, MutableList<Channel>>()
        channels.forEach { channel ->
            val groupName = channel.group.ifEmpty { "-" }
            grouped.getOrPut(groupName) { mutableListOf() }.add(channel)
        }
        return grouped.map { (group, list) ->
            ChannelGroup(name = group, channels = list)
        }
    }
}
