package az.iptv.fplayer.data.model

import java.net.URLEncoder

data class XtreamConfig(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    private val base: String
        get() = serverUrl
            .trim()
            .substringBefore("?")
            .substringBefore("/player_api.php")
            .substringBefore("/get.php")
            .trimEnd('/')

    val apiUrl: String get() = "$base/player_api.php?username=${username.urlPart()}&password=${password.urlPart()}"

    fun streamUrl(streamId: Int, extension: String = "ts"): String =
        "$base/live/${username.urlPart()}/${password.urlPart()}/$streamId.${cleanExtension(extension, "ts")}"

    fun movieUrl(streamId: Int, extension: String): String =
        "$base/movie/${username.urlPart()}/${password.urlPart()}/$streamId.${cleanExtension(extension, "mp4")}"

    fun seriesUrl(episodeId: Int, extension: String): String =
        "$base/series/${username.urlPart()}/${password.urlPart()}/$episodeId.${cleanExtension(extension, "mp4")}"

    private fun String.urlPart(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun cleanExtension(extension: String, fallback: String): String {
        val cleaned = extension.trim().trimStart('.')
        return cleaned
            .takeIf { it.isNotBlank() && it.length <= 8 && it.all(Char::isLetterOrDigit) }
            ?: fallback
    }
}
