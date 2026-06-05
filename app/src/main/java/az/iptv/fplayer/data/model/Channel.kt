package az.iptv.fplayer.data.model

enum class ChannelContentType {
    TV,
    MOVIE,
    SERIES
}

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String = "",
    val group: String = "",
    val epgId: String = "",
    val isFavorite: Boolean = false,
    val frameRate: Float = 0f,
    val contentType: ChannelContentType = ChannelContentType.TV
) {
    val stableKey: String get() = "$id|$url"
}

data class ChannelGroup(
    val name: String,
    val channels: List<Channel>
)
