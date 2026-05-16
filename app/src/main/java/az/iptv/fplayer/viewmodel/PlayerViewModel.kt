package az.iptv.fplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.XtreamConfig
import az.iptv.fplayer.data.preferences.AppPreferences
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
            val type = prefs.playlistType.first()
            val lastChannelKey = prefs.lastChannelId.first()
            when (type) {
                PlaylistType.M3U.name -> {
                    val url = prefs.m3uUrl.first()
                    if (url.isNotEmpty()) {
                        repo.loadCachedM3u(url)
                            .onSuccess {
                                onGroupsLoaded(
                                    groups = it,
                                    fromCache = true,
                                    preferredChannelKey = lastChannelKey,
                                    revealSidebar = false
                                )
                                return@launch
                            }
                        loadM3uFromNetwork(url, preferredChannelKey = lastChannelKey)
                    }
                }
                PlaylistType.XTREAM.name -> {
                    val server = prefs.xtreamServer.first()
                    val user = prefs.xtreamUser.first()
                    val pass = prefs.xtreamPass.first()
                    if (server.isNotEmpty() && user.isNotEmpty()) {
                        val config = xtreamConfig(server, user, pass)
                        repo.loadCachedXtream(config)
                            .onSuccess {
                                onGroupsLoaded(
                                    groups = it,
                                    fromCache = true,
                                    preferredChannelKey = lastChannelKey,
                                    revealSidebar = false
                                )
                                return@launch
                            }
                        loadXtreamFromNetwork(config, preferredChannelKey = lastChannelKey)
                    }
                }
            }
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
            loadM3uFromNetwork(url, revealSidebar = true)
        }
    }

    fun loadXtream(server: String, username: String, password: String) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            prefs.saveXtream(server, username, password)
            loadXtreamFromNetwork(xtreamConfig(server, username, password), revealSidebar = true)
        }
    }

    fun refreshPlaylist() {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            val currentKey = _currentChannel.value?.stableKey ?: prefs.lastChannelId.first()
            when (prefs.playlistType.first()) {
                PlaylistType.M3U.name -> {
                    val url = prefs.m3uUrl.first()
                    if (url.isNotEmpty()) loadM3uFromNetwork(
                        url = url,
                        preferredChannelKey = currentKey,
                        revealSidebar = _sidebarVisible.value
                    )
                }
                PlaylistType.XTREAM.name -> {
                    val server = prefs.xtreamServer.first()
                    val user = prefs.xtreamUser.first()
                    val pass = prefs.xtreamPass.first()
                    if (server.isNotEmpty() && user.isNotEmpty()) loadXtreamFromNetwork(
                        config = xtreamConfig(server, user, pass),
                        preferredChannelKey = currentKey,
                        revealSidebar = _sidebarVisible.value
                    )
                }
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
