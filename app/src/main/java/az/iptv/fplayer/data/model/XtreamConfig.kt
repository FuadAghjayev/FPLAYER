package az.iptv.fplayer.data.model

data class XtreamConfig(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    private val base: String get() = serverUrl.trimEnd('/')
    val apiUrl: String get() = "$base/player_api.php?username=$username&password=$password"
    fun streamUrl(streamId: Int): String = "$base/$username/$password/$streamId"
}
