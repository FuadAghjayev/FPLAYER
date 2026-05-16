package az.iptv.fplayer.data.parser

import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup

object M3uParser {

    fun parse(content: String): List<ChannelGroup> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val url = lines.getOrNull(i + 1)?.trim()
                if (!url.isNullOrEmpty() && !url.startsWith("#")) {
                    val channel = parseExtInf(line, url)
                    channels.add(channel)
                    i += 2
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
            frameRate = parseFrameRate(attrs)
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

    private fun parseFrameRate(attrs: Map<String, String>): Float {
        val keys = listOf("fps", "frame-rate", "frame_rate", "framerate", "tvg-fps")
        return keys.firstNotNullOfOrNull { key ->
            attrs[key]?.toPositiveFrameRate()
        } ?: 0f
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
