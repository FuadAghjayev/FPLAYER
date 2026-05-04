package az.iptv.fplayer.data.model

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String = "",
    val group: String = "",
    val epgId: String = "",
    val isFavorite: Boolean = false
)

data class ChannelGroup(
    val name: String,
    val channels: List<Channel>
)
