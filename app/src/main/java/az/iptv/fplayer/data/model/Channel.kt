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
    val isAdult: Boolean = false,
    val frameRate: Float = 0f,
    val contentType: ChannelContentType = ChannelContentType.TV
) {
    val stableKey: String get() = "$id|$url"
}

data class ChannelGroup(
    val name: String,
    val channels: List<Channel>
)

data class ProgramInfo(
    val title: String,
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val startEpochSeconds: Long = 0L,
    val endEpochSeconds: Long = 0L
) {
    val timeRange: String
        get() = listOf(startTime, endTime).filter { it.isNotBlank() }.joinToString(" - ")

    fun progress(nowSeconds: Long = System.currentTimeMillis() / 1000L): Float? {
        if (startEpochSeconds <= 0L || endEpochSeconds <= startEpochSeconds) return null
        return ((nowSeconds - startEpochSeconds).toFloat() / (endEpochSeconds - startEpochSeconds))
            .coerceIn(0f, 1f)
    }
}
