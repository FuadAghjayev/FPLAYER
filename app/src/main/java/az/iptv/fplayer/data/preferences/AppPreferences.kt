package az.iptv.fplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fplayer_prefs")

enum class PlaylistType { M3U, XTREAM }

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_PLAYLIST_TYPE = stringPreferencesKey("playlist_type")
        private val KEY_M3U_URL = stringPreferencesKey("m3u_url")
        private val KEY_XTREAM_SERVER = stringPreferencesKey("xtream_server")
        private val KEY_XTREAM_USER = stringPreferencesKey("xtream_user")
        private val KEY_XTREAM_PASS = stringPreferencesKey("xtream_pass")
        private val KEY_PLAYER_TYPE = stringPreferencesKey("player_type")
        private val KEY_LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
        private val KEY_AUDIO_DECODER = stringPreferencesKey("audio_decoder")
    }

    val playlistType: Flow<String> = context.dataStore.data.map { it[KEY_PLAYLIST_TYPE] ?: "" }
    val m3uUrl: Flow<String> = context.dataStore.data.map { it[KEY_M3U_URL] ?: "" }
    val xtreamServer: Flow<String> = context.dataStore.data.map { it[KEY_XTREAM_SERVER] ?: "" }
    val xtreamUser: Flow<String> = context.dataStore.data.map { it[KEY_XTREAM_USER] ?: "" }
    val xtreamPass: Flow<String> = context.dataStore.data.map { it[KEY_XTREAM_PASS] ?: "" }
    val playerType: Flow<String> = context.dataStore.data.map { it[KEY_PLAYER_TYPE] ?: "EXOPLAYER" }
    val lastChannelId: Flow<String> = context.dataStore.data.map { it[KEY_LAST_CHANNEL_ID] ?: "" }
    val audioDecoderMode: Flow<String> = context.dataStore.data.map { it[KEY_AUDIO_DECODER] ?: "AUTO" }

    suspend fun saveM3u(url: String) = context.dataStore.edit {
        it[KEY_PLAYLIST_TYPE] = PlaylistType.M3U.name
        it[KEY_M3U_URL] = url
    }

    suspend fun saveXtream(server: String, user: String, pass: String) = context.dataStore.edit {
        it[KEY_PLAYLIST_TYPE] = PlaylistType.XTREAM.name
        it[KEY_XTREAM_SERVER] = server
        it[KEY_XTREAM_USER] = user
        it[KEY_XTREAM_PASS] = pass
    }

    suspend fun setPlayerType(type: String) = context.dataStore.edit { it[KEY_PLAYER_TYPE] = type }
    suspend fun setLastChannelId(id: String) = context.dataStore.edit { it[KEY_LAST_CHANNEL_ID] = id }
    suspend fun setAudioDecoderMode(mode: String) = context.dataStore.edit { it[KEY_AUDIO_DECODER] = mode }
}
