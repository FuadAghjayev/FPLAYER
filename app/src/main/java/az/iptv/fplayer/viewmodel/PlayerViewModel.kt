package az.iptv.fplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import az.iptv.fplayer.data.preferences.AppPreferences
import az.iptv.fplayer.data.preferences.AppLanguage
import az.iptv.fplayer.data.preferences.PlaylistProfile
import az.iptv.fplayer.data.preferences.PlaylistType
import az.iptv.fplayer.data.repository.ChannelRepository
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.player.VideoInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class LoadState {
    data object Idle : LoadState()
    data object Loading : LoadState()
    data class Success(val count: Int, val fromCache: Boolean = false) : LoadState()
    data class Error(val message: String) : LoadState()
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChannelRepository(app)
    val prefs = AppPreferences(app)

    val playlists: StateFlow<List<PlaylistProfile>> = prefs.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activePlaylist: StateFlow<PlaylistProfile?> = prefs.activePlaylist
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val appLanguage: StateFlow<String> = prefs.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.AZ.name)

    private val _groups = MutableStateFlow<List<ChannelGroup>>(emptyList())
    val groups: StateFlow<List<ChannelGroup>> = _groups

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _videoInfo = MutableStateFlow(VideoInfo())
    val videoInfo: StateFlow<VideoInfo> = _videoInfo

    private val _sidebarVisible = MutableStateFlow(false)
    val sidebarVisible: StateFlow<Boolean> = _sidebarVisible

    private val _osdVisible = MutableStateFlow(false)
    val osdVisible: StateFlow<Boolean> = _osdVisible

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState

    private val _playerType = MutableStateFlow(PlayerType.EXOPLAYER)
    val playerType: StateFlow<PlayerType> = _playerType

    private val _audioDecoderMode = MutableStateFlow(AudioDecoderMode.AUTO)
    val audioDecoderMode: StateFlow<AudioDecoderMode> = _audioDecoderMode

    private var playlistLoadJob: Job? = null

    val visibleChannels: StateFlow<List<Channel>> = combine(_groups, _selectedGroup) { groups, group ->
        if (group == null) groups.flatMap { it.channels }
        else groups.find { it.name == group }?.channels ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            prefs.playerType.collect { saved ->
                _playerType.value = if (saved == "VLC") PlayerType.VLC else PlayerType.EXOPLAYER
            }
        }
        viewModelScope.launch {
            prefs.audioDecoderMode.collect { saved ->
                _audioDecoderMode.value = when (saved) {
                    "HARDWARE" -> AudioDecoderMode.HARDWARE
                    "SOFTWARE" -> AudioDecoderMode.SOFTWARE
                    else -> AudioDecoderMode.AUTO
                }
            }
        }
    }

    fun autoLoadSavedPlaylist() {
        if (_groups.value.isNotEmpty()) return
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            val profile = prefs.activePlaylist.first() ?: return@launch
            val lastChannelKey = prefs.lastChannelId.first()
            loadPlaylist(profile, preferredChannelKey = lastChannelKey)
        }
    }

    // Bug fix #3: AddPlaylistScreen-ə girəndə loadState sıfırla
    fun resetForPlaylistEdit() {
        _loadState.value = LoadState.Idle
    }

    fun loadM3u(url: String) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            prefs.saveM3u(url)
            prefs.activePlaylist.first()?.let { loadPlaylist(it, revealSidebar = true, preferNetwork = true) }
        }
    }

    fun loadXtream(server: String, username: String, password: String) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            prefs.saveXtream(server, username, password)
            prefs.activePlaylist.first()?.let { loadPlaylist(it, revealSidebar = true, preferNetwork = true) }
        }
    }

    fun savePlaylistAndLoad(profile: PlaylistProfile) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            prefs.savePlaylist(profile)
            loadPlaylist(profile, revealSidebar = true, preferNetwork = true)
        }
    }

    fun switchPlaylist(profile: PlaylistProfile) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            prefs.activatePlaylist(profile.id)
            _selectedGroup.value = null
            _currentChannel.value = null
            loadPlaylist(profile, revealSidebar = true)
        }
    }

    fun refreshPlaylist() {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            val currentKey = _currentChannel.value?.stableKey ?: prefs.lastChannelId.first()
            prefs.activePlaylist.first()?.let {
                loadPlaylist(
                    profile = it,
                    preferredChannelKey = currentKey,
                    revealSidebar = _sidebarVisible.value,
                    preferNetwork = true
                )
            }
        }
    }

    private suspend fun loadPlaylist(
        profile: PlaylistProfile,
        preferredChannelKey: String? = null,
        revealSidebar: Boolean = false,
        preferNetwork: Boolean = false
    ) {
        when (profile.type) {
            PlaylistType.M3U -> {
                if (profile.m3uUrl.isBlank()) return
                if (!preferNetwork) {
                    repo.loadCachedM3u(profile.m3uUrl)
                        .onSuccess {
                            onGroupsLoaded(it, fromCache = true, preferredChannelKey, revealSidebar)
                            return
                        }
                }
                loadM3uFromNetwork(profile.m3uUrl, preferredChannelKey, revealSidebar)
            }
            PlaylistType.XTREAM -> {
                if (profile.xtreamServer.isBlank() || profile.xtreamUser.isBlank()) return
                val config = xtreamConfig(profile.xtreamServer, profile.xtreamUser, profile.xtreamPass)
                if (!preferNetwork) {
                    repo.loadCachedXtream(config)
                        .onSuccess {
                            onGroupsLoaded(it, fromCache = true, preferredChannelKey, revealSidebar)
                            return
                        }
                }
                loadXtreamFromNetwork(config, preferredChannelKey, revealSidebar)
            }
        }
    }

    private suspend fun loadM3uFromNetwork(
        url: String,
        preferredChannelKey: String? = null,
        revealSidebar: Boolean = false
    ) {
        _loadState.value = LoadState.Loading
        repo.loadM3u(url)
            .onSuccess {
                onGroupsLoaded(
                    groups = it,
                    fromCache = false,
                    preferredChannelKey = preferredChannelKey,
                    revealSidebar = revealSidebar
                )
            }
            .onFailure { _loadState.value = LoadState.Error(it.message ?: "Xəta baş verdi") }
    }

    private suspend fun loadXtreamFromNetwork(
        config: XtreamConfig,
        preferredChannelKey: String? = null,
        revealSidebar: Boolean = false
    ) {
        _loadState.value = LoadState.Loading
        repo.loadXtream(config)
            .onSuccess {
                onGroupsLoaded(
                    groups = it,
                    fromCache = false,
                    preferredChannelKey = preferredChannelKey,
                    revealSidebar = revealSidebar
                )
            }
            .onFailure { _loadState.value = LoadState.Error(it.message ?: "Xəta baş verdi") }
    }

    private fun onGroupsLoaded(
        groups: List<ChannelGroup>,
        fromCache: Boolean,
        preferredChannelKey: String? = null,
        revealSidebar: Boolean = false
    ) {
        _groups.value = groups
        _loadState.value = LoadState.Success(groups.sumOf { it.channels.size }, fromCache)

        if (_selectedGroup.value != null && groups.none { it.name == _selectedGroup.value }) {
            _selectedGroup.value = null
        }

        val channels = groups.flatMap { it.channels }
        val currentKey = _currentChannel.value?.stableKey
        _currentChannel.value = channels.firstOrNull { it.stableKey == currentKey }
            ?: channels.firstOrNull { it.stableKey == preferredChannelKey }
            ?: channels.firstOrNull()
        _currentChannel.value?.group?.takeIf { it.isNotBlank() && revealSidebar }?.let { channelGroup ->
            if (groups.any { it.name == channelGroup }) _selectedGroup.value = channelGroup
        }
        _sidebarVisible.value = revealSidebar
    }

    private fun xtreamConfig(server: String, username: String, password: String): XtreamConfig {
        return XtreamConfig(
            serverUrl = if (server.startsWith("http")) server else "http://$server",
            username = username,
            password = password
        )
    }

    private var osdJob: Job? = null

    fun selectChannel(channel: Channel) {
        _currentChannel.value = channel
        _sidebarVisible.value = false
        _osdVisible.value = true
        viewModelScope.launch { prefs.setLastChannelId(channel.stableKey) }
        osdJob?.cancel()
        osdJob = viewModelScope.launch {
            delay(5000)
            _osdVisible.value = false
        }
    }

    fun selectGroup(group: String?) { _selectedGroup.value = group }
    fun toggleSidebar() { _sidebarVisible.value = !_sidebarVisible.value }
    fun showSidebar() { _sidebarVisible.value = true }
    fun hideSidebar() { _sidebarVisible.value = false }

    fun showOsd() {
        _osdVisible.value = true
        osdJob?.cancel()
        osdJob = viewModelScope.launch {
            delay(5000)
            _osdVisible.value = false
        }
    }

    fun hideOsd() {
        osdJob?.cancel()
        _osdVisible.value = false
    }

    fun onPlaybackStateChanged(state: PlaybackState) { _playbackState.value = state }
    fun onVideoInfoChanged(info: VideoInfo) { _videoInfo.value = info }

    fun setPlayerType(type: PlayerType) {
        _playerType.value = type
        viewModelScope.launch { prefs.setPlayerType(type.name) }
    }

    fun setAudioDecoderMode(mode: AudioDecoderMode) {
        _audioDecoderMode.value = mode
        viewModelScope.launch { prefs.setAudioDecoderMode(mode.name) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(language.name) }
    }

    fun nextChannel() {
        val channels = visibleChannels.value
        val currentKey = _currentChannel.value?.stableKey
        val idx = channels.indexOfFirst { it.stableKey == currentKey }
        (channels.getOrNull(idx + 1) ?: channels.firstOrNull())?.let { selectChannel(it) }
    }

    fun prevChannel() {
        val channels = visibleChannels.value
        val currentKey = _currentChannel.value?.stableKey
        val idx = channels.indexOfFirst { it.stableKey == currentKey }
        (channels.getOrNull(idx - 1) ?: channels.lastOrNull())?.let { selectChannel(it) }
    }
}
