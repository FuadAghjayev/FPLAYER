package az.iptv.fplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fplayer_prefs")

enum class PlaylistType { M3U, XTREAM }
enum class AppLanguage { AZ, EN }

data class PlaylistProfile(
    val id: String,
    val name: String,
    val type: PlaylistType,
    val m3uUrl: String = "",
    val xtreamServer: String = "",
    val xtreamUser: String = "",
    val xtreamPass: String = ""
) {
    val sourceLabel: String
        get() = when (type) {
            PlaylistType.M3U -> m3uUrl
            PlaylistType.XTREAM -> xtreamServer
        }
}

class AppPreferences(private val context: Context) {

    companion object {
        const val MAX_PLAYLISTS = 10

        private val KEY_PLAYLIST_TYPE = stringPreferencesKey("playlist_type")
        private val KEY_M3U_URL = stringPreferencesKey("m3u_url")
        private val KEY_XTREAM_SERVER = stringPreferencesKey("xtream_server")
        private val KEY_XTREAM_USER = stringPreferencesKey("xtream_user")
        private val KEY_XTREAM_PASS = stringPreferencesKey("xtream_pass")
        private val KEY_PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
        private val KEY_ACTIVE_PLAYLIST_ID = stringPreferencesKey("active_playlist_id")
        private val KEY_PLAYER_TYPE = stringPreferencesKey("player_type")
        private val KEY_LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
        private val KEY_AUDIO_DECODER = stringPreferencesKey("audio_decoder")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_FAVORITE_CHANNELS = stringSetPreferencesKey("favorite_channels")
    }

    val playlists: Flow<List<PlaylistProfile>> = context.dataStore.data.map { readProfiles(it) }
    val activePlaylistId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PLAYLIST_ID].orEmpty()
    }
    val activePlaylist: Flow<PlaylistProfile?> = context.dataStore.data.map { prefs ->
        val profiles = readProfiles(prefs)
        val activeId = prefs[KEY_ACTIVE_PLAYLIST_ID]
        profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    }
    val playlistType: Flow<String> = activePlaylist.map { it?.type?.name.orEmpty() }
    val m3uUrl: Flow<String> = activePlaylist.map { it?.m3uUrl.orEmpty() }
    val xtreamServer: Flow<String> = activePlaylist.map { it?.xtreamServer.orEmpty() }
    val xtreamUser: Flow<String> = activePlaylist.map { it?.xtreamUser.orEmpty() }
    val xtreamPass: Flow<String> = activePlaylist.map { it?.xtreamPass.orEmpty() }
    val playerType: Flow<String> = context.dataStore.data.map { it[KEY_PLAYER_TYPE] ?: "EXOPLAYER" }
    val lastChannelId: Flow<String> = context.dataStore.data.map { it[KEY_LAST_CHANNEL_ID] ?: "" }
    val audioDecoderMode: Flow<String> = context.dataStore.data.map { it[KEY_AUDIO_DECODER] ?: "AUTO" }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: AppLanguage.AZ.name }
    val favoriteChannelKeys: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_FAVORITE_CHANNELS] ?: emptySet()
    }

    suspend fun saveM3u(url: String) = context.dataStore.edit {
        val profiles = readProfiles(it)
        val active = profiles.firstOrNull { profile -> profile.id == it[KEY_ACTIVE_PLAYLIST_ID] }
        val next = PlaylistProfile(
            id = active?.id ?: newProfileId(),
            name = active?.name?.takeIf { name -> name.isNotBlank() } ?: defaultPlaylistName(profiles.size, PlaylistType.M3U),
            type = PlaylistType.M3U,
            m3uUrl = url.trim()
        )
        writeProfiles(it, upsertProfile(profiles, next))
        it[KEY_ACTIVE_PLAYLIST_ID] = next.id
        it[KEY_PLAYLIST_TYPE] = PlaylistType.M3U.name
        it[KEY_M3U_URL] = url.trim()
    }

    suspend fun saveXtream(server: String, user: String, pass: String) = context.dataStore.edit {
        val profiles = readProfiles(it)
        val active = profiles.firstOrNull { profile -> profile.id == it[KEY_ACTIVE_PLAYLIST_ID] }
        val next = PlaylistProfile(
            id = active?.id ?: newProfileId(),
            name = active?.name?.takeIf { name -> name.isNotBlank() } ?: defaultPlaylistName(profiles.size, PlaylistType.XTREAM),
            type = PlaylistType.XTREAM,
            xtreamServer = server.trim(),
            xtreamUser = user.trim(),
            xtreamPass = pass.trim()
        )
        writeProfiles(it, upsertProfile(profiles, next))
        it[KEY_ACTIVE_PLAYLIST_ID] = next.id
        it[KEY_PLAYLIST_TYPE] = PlaylistType.XTREAM.name
        it[KEY_XTREAM_SERVER] = server.trim()
        it[KEY_XTREAM_USER] = user.trim()
        it[KEY_XTREAM_PASS] = pass.trim()
    }

    suspend fun savePlaylist(profile: PlaylistProfile) = context.dataStore.edit {
        val profiles = readProfiles(it)
        writeProfiles(it, upsertProfile(profiles, profile))
        it[KEY_ACTIVE_PLAYLIST_ID] = profile.id
        writeLegacyActive(it, profile)
    }

    suspend fun activatePlaylist(id: String) = context.dataStore.edit {
        val profiles = readProfiles(it)
        val profile = profiles.firstOrNull { profile -> profile.id == id } ?: return@edit
        it[KEY_ACTIVE_PLAYLIST_ID] = profile.id
        writeLegacyActive(it, profile)
    }

    suspend fun setPlayerType(type: String) = context.dataStore.edit { it[KEY_PLAYER_TYPE] = type }
    suspend fun setLastChannelId(id: String) = context.dataStore.edit { it[KEY_LAST_CHANNEL_ID] = id }
    suspend fun setAudioDecoderMode(mode: String) = context.dataStore.edit { it[KEY_AUDIO_DECODER] = mode }
    suspend fun setLanguage(language: String) = context.dataStore.edit { it[KEY_LANGUAGE] = language }
    suspend fun setFavoriteChannel(key: String, favorite: Boolean) = context.dataStore.edit {
        val current = it[KEY_FAVORITE_CHANNELS] ?: emptySet()
        it[KEY_FAVORITE_CHANNELS] = if (favorite) current + key else current - key
    }

    private fun readProfiles(prefs: Preferences): List<PlaylistProfile> {
        val saved = prefs[KEY_PLAYLISTS_JSON].orEmpty()
        val profiles = runCatching {
            val arr = JSONArray(saved)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val type = runCatching {
                        PlaylistType.valueOf(obj.optString("type"))
                    }.getOrDefault(PlaylistType.M3U)
                    add(
                        PlaylistProfile(
                            id = obj.optString("id").ifBlank { newProfileId() },
                            name = obj.optString("name").ifBlank { defaultPlaylistName(i, type) },
                            type = type,
                            m3uUrl = obj.optString("m3uUrl"),
                            xtreamServer = obj.optString("xtreamServer"),
                            xtreamUser = obj.optString("xtreamUser"),
                            xtreamPass = obj.optString("xtreamPass")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        return profiles.ifEmpty { legacyProfile(prefs)?.let(::listOf).orEmpty() }
    }

    private fun legacyProfile(prefs: Preferences): PlaylistProfile? {
        return when (prefs[KEY_PLAYLIST_TYPE]) {
            PlaylistType.M3U.name -> prefs[KEY_M3U_URL]
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    PlaylistProfile(
                        id = "legacy_m3u_${it.hashCode()}",
                        name = "M3U 1",
                        type = PlaylistType.M3U,
                        m3uUrl = it
                    )
                }
            PlaylistType.XTREAM.name -> prefs[KEY_XTREAM_SERVER]
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    PlaylistProfile(
                        id = "legacy_xtream_${it.hashCode()}",
                        name = "Xtream 1",
                        type = PlaylistType.XTREAM,
                        xtreamServer = it,
                        xtreamUser = prefs[KEY_XTREAM_USER].orEmpty(),
                        xtreamPass = prefs[KEY_XTREAM_PASS].orEmpty()
                    )
                }
            else -> null
        }
    }

    private fun writeProfiles(prefs: androidx.datastore.preferences.core.MutablePreferences, profiles: List<PlaylistProfile>) {
        prefs[KEY_PLAYLISTS_JSON] = JSONArray().also { arr ->
            profiles.take(MAX_PLAYLISTS).forEach { profile ->
                arr.put(
                    JSONObject()
                        .put("id", profile.id)
                        .put("name", profile.name)
                        .put("type", profile.type.name)
                        .put("m3uUrl", profile.m3uUrl)
                        .put("xtreamServer", profile.xtreamServer)
                        .put("xtreamUser", profile.xtreamUser)
                        .put("xtreamPass", profile.xtreamPass)
                )
            }
        }.toString()
    }

    private fun upsertProfile(profiles: List<PlaylistProfile>, profile: PlaylistProfile): List<PlaylistProfile> {
        val updated = if (profiles.any { it.id == profile.id }) {
            profiles.map { if (it.id == profile.id) profile else it }
        } else if (profiles.size >= MAX_PLAYLISTS) {
            profiles.drop(1) + profile
        } else {
            profiles + profile
        }
        return updated
    }

    private fun writeLegacyActive(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        profile: PlaylistProfile
    ) {
        prefs[KEY_PLAYLIST_TYPE] = profile.type.name
        when (profile.type) {
            PlaylistType.M3U -> prefs[KEY_M3U_URL] = profile.m3uUrl
            PlaylistType.XTREAM -> {
                prefs[KEY_XTREAM_SERVER] = profile.xtreamServer
                prefs[KEY_XTREAM_USER] = profile.xtreamUser
                prefs[KEY_XTREAM_PASS] = profile.xtreamPass
            }
        }
    }

    private fun newProfileId(): String = "playlist_${System.currentTimeMillis()}"

    private fun defaultPlaylistName(index: Int, type: PlaylistType): String {
        val prefix = if (type == PlaylistType.XTREAM) "Xtream" else "M3U"
        return "$prefix ${index + 1}"
    }
}
