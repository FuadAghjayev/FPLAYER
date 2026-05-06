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
    data class Success(val count: Int) : LoadState()
    data class Error(val message: String) : LoadState()
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChannelRepository()
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

    private val _sidebarVisible = MutableStateFlow(true)
    val sidebarVisible: StateFlow<Boolean> = _sidebarVisible

    private val _osdVisible = MutableStateFlow(false)
    val osdVisible: StateFlow<Boolean> = _osdVisible

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState

    private val _playerType = MutableStateFlow(PlayerType.EXOPLAYER)
    val playerType: StateFlow<PlayerType> = _playerType

    private val _audioDecoderMode = MutableStateFlow(AudioDecoderMode.AUTO)
    val audioDecoderMode: StateFlow<AudioDecoderMode> = _audioDecoderMode

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

    // Bug fix #2: sadece qruplar boşdursa yüklə
    fun autoLoadSavedPlaylist() {
        if (_groups.value.isNotEmpty()) return
        viewModelScope.launch {
            val type = prefs.playlistType.first()
            when (type) {
                PlaylistType.M3U.name -> {
                    val url = prefs.m3uUrl.first()
                    if (url.isNotEmpty()) loadM3u(url)
                }
                PlaylistType.XTREAM.name -> {
                    val server = prefs.xtreamServer.first()
                    val user = prefs.xtreamUser.first()
                    val pass = prefs.xtreamPass.first()
                    if (server.isNotEmpty() && user.isNotEmpty()) loadXtream(server, user, pass)
                }
            }
        }
    }

    // Bug fix #3: AddPlaylistScreen-ə girəndə loadState sıfırla
    fun resetForPlaylistEdit() {
        _loadState.value = LoadState.Idle
    }

    fun loadM3u(url: String) {
        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            prefs.saveM3u(url)
            repo.loadM3u(url)
                .onSuccess { onGroupsLoaded(it) }
                .onFailure { _loadState.value = LoadState.Error(it.message ?: "Xəta baş verdi") }
        }
    }

    fun loadXtream(server: String, username: String, password: String) {
        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            prefs.saveXtream(server, username, password)
            val config = XtreamConfig(
                serverUrl = if (server.startsWith("http")) server else "http://$server",
                username = username,
                password = password
            )
            repo.loadXtream(config)
                .onSuccess { onGroupsLoaded(it) }
                .onFailure { _loadState.value = LoadState.Error(it.message ?: "Xəta baş verdi") }
        }
    }

    private fun onGroupsLoaded(groups: List<ChannelGroup>) {
        _groups.value = groups
        _loadState.value = LoadState.Success(groups.sumOf { it.channels.size })
        // Bug fix #1: ilk kanalı sidebar-ı gizlətmədən set et
        if (_currentChannel.value == null) {
            _currentChannel.value = groups.firstOrNull()?.channels?.firstOrNull()
        }
        _sidebarVisible.value = true
    }

    private var osdJob: Job? = null

    fun selectChannel(channel: Channel) {
        _currentChannel.value = channel
        _sidebarVisible.value = false
        _osdVisible.value = true
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
        val idx = channels.indexOf(_currentChannel.value)
        (channels.getOrNull(idx + 1) ?: channels.firstOrNull())?.let { selectChannel(it) }
    }

    fun prevChannel() {
        val channels = visibleChannels.value
        val idx = channels.indexOf(_currentChannel.value)
        (channels.getOrNull(idx - 1) ?: channels.lastOrNull())?.let { selectChannel(it) }
    }
}
