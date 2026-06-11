package az.iptv.fplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelContentType
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.data.model.ProgramInfo
import az.iptv.fplayer.data.model.XtreamConfig
import az.iptv.fplayer.data.preferences.AdultAccessMode
import az.iptv.fplayer.data.preferences.AppPreferences
import az.iptv.fplayer.data.preferences.AppLanguage
import az.iptv.fplayer.data.preferences.AppThemeMode
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
    val appThemeMode: StateFlow<String> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.CLASSIC.name)
    val adultPin: StateFlow<String> = prefs.adultPin
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_ADULT_PIN)
    val adultAccessMode: StateFlow<String> = prefs.adultAccessMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AdultAccessMode.PER_CHANNEL.name)
    val favoriteChannelKeys: StateFlow<Set<String>> = prefs.favoriteChannelKeys
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _adultUnlocked = MutableStateFlow(false)
    val adultUnlocked: StateFlow<Boolean> = _adultUnlocked
    private val _adultUnlockedChannelKeys = MutableStateFlow<Set<String>>(emptySet())
    val adultUnlockedChannelKeys: StateFlow<Set<String>> = _adultUnlockedChannelKeys

    private val _groups = MutableStateFlow<List<ChannelGroup>>(emptyList())
    private val _selectedContentType = MutableStateFlow(ChannelContentType.TV)
    val selectedContentType: StateFlow<ChannelContentType> = _selectedContentType

    val availableContentTypes: StateFlow<List<ChannelContentType>> = combine(
        _groups,
        adultAccessMode
    ) { groups, adultMode ->
        val present = groups
            .flatMap { group -> group.channels.filterVisibleForAdultMode(adultMode).map { it.contentType } }
            .toSet()
        contentTypeOrder.filter { it in present }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(ChannelContentType.TV))

    val groups: StateFlow<List<ChannelGroup>> = combine(
        _groups,
        _selectedContentType,
        favoriteChannelKeys,
        adultAccessMode
    ) { groups, contentType, favorites, adultMode ->
        val filteredGroups = groups.mapNotNull { group ->
            val channels = group.channels
                .filterVisibleForAdultMode(adultMode)
                .filter { it.contentType == contentType }
                .markFavorites(favorites)
            if (channels.isEmpty()) null else group.copy(channels = channels)
        }
        val favoriteChannels = filteredGroups
            .flatMap { it.channels }
            .distinctBy { it.stableKey }
            .filter { it.stableKey in favorites }
        if (favoriteChannels.isEmpty()) filteredGroups
        else listOf(ChannelGroup(FAVORITE_GROUP_NAME, favoriteChannels)) + filteredGroups
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel

    private val _recentChannels = MutableStateFlow<List<Channel>>(emptyList())
    val recentChannels: StateFlow<List<Channel>> = _recentChannels

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _videoInfo = MutableStateFlow(VideoInfo())
    val videoInfo: StateFlow<VideoInfo> = _videoInfo

    private val _currentProgram = MutableStateFlow<ProgramInfo?>(null)
    val currentProgram: StateFlow<ProgramInfo?> = _currentProgram

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
    private var epgJob: Job? = null
    private val maxRecentChannels = 10

    val visibleChannels: StateFlow<List<Channel>> = combine(groups, _selectedGroup) { groups, group ->
        if (group == null) groups.flatMap { it.channels }
        else groups.find { it.name == group }?.channels ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            prefs.playerType.collect { saved ->
                _playerType.value = PlayerType.EXOPLAYER
                if (saved != PlayerType.EXOPLAYER.name) {
                    prefs.setPlayerType(PlayerType.EXOPLAYER.name)
                }
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
        viewModelScope.launch {
            adultAccessMode.collect { mode ->
                _adultUnlocked.value = false
                _adultUnlockedChannelKeys.value = emptySet()
                _recentChannels.value = _recentChannels.value.filterVisibleForAdultMode(mode)
                if (mode == AdultAccessMode.HIDDEN.name && _currentChannel.value?.isAdult == true) {
                    val nextChannel = firstPlayableVisibleChannel()
                    if (nextChannel != null) {
                        _currentChannel.value = nextChannel
                        loadCurrentProgram(nextChannel)
                    } else {
                        _currentChannel.value = null
                        _currentProgram.value = null
                    }
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
            _currentProgram.value = null
            loadPlaylist(profile, revealSidebar = true)
        }
    }

    fun deletePlaylist(profile: PlaylistProfile) {
        playlistLoadJob?.cancel()
        playlistLoadJob = viewModelScope.launch {
            val wasActive = activePlaylist.value?.id == profile.id
            prefs.deletePlaylist(profile.id)
            if (!wasActive) return@launch

            _groups.value = emptyList()
            _selectedGroup.value = null
            _currentChannel.value = null
            _currentProgram.value = null
            _recentChannels.value = emptyList()
            _selectedContentType.value = ChannelContentType.TV
            val nextProfile = prefs.activePlaylist.first()
            if (nextProfile != null) {
                loadPlaylist(nextProfile, revealSidebar = true)
            } else {
                _loadState.value = LoadState.Idle
                _sidebarVisible.value = false
                _osdVisible.value = false
            }
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

        val channels = groups.flatMap { it.channels }.markFavorites(favoriteChannelKeys.value)
        val currentKey = _currentChannel.value?.stableKey
        _currentChannel.value = channels.firstOrNull { it.stableKey == currentKey && it.isPlayable() }
            ?: channels.firstOrNull { it.stableKey == preferredChannelKey && it.isPlayable() }
            ?: preferredContentTypeChannels(groups).markFavorites(favoriteChannelKeys.value).firstOrNull { it.isPlayable() }
            ?: channels.firstOrNull { it.isPlayable() }

        _currentChannel.value?.let { selected ->
            _selectedContentType.value = selected.contentType
            loadCurrentProgram(selected)
        }

        val filteredGroups = groupsForSelectedContent()
        if (_selectedGroup.value != null && filteredGroups.none { it.name == _selectedGroup.value }) {
            _selectedGroup.value = null
        }

        val channelsByKey = channels
            .filterVisibleForAdultMode(adultAccessMode.value)
            .associateBy { it.stableKey }
        _recentChannels.value = _recentChannels.value.mapNotNull { channelsByKey[it.stableKey] }
        _currentChannel.value?.let(::rememberRecentChannel)
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
        if (!channel.isPlayable()) return
        _selectedContentType.value = channel.contentType
        _currentChannel.value = channel
        loadCurrentProgram(channel)
        rememberRecentChannel(channel)
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
    fun selectContentType(contentType: ChannelContentType) {
        if (_selectedContentType.value == contentType) return
        _selectedContentType.value = contentType
        _selectedGroup.value = null
        val channels = groupsForSelectedContent(contentType).flatMap { it.channels }
        channels.firstOrNull { it.isPlayable() }?.let { channel ->
            _currentChannel.value = channel
            loadCurrentProgram(channel)
            rememberRecentChannel(channel)
            viewModelScope.launch { prefs.setLastChannelId(channel.stableKey) }
        }
    }
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

    private fun loadCurrentProgram(channel: Channel) {
        epgJob?.cancel()
        _currentProgram.value = null

        val profile = activePlaylist.value ?: return
        if (profile.type != PlaylistType.XTREAM || channel.contentType != ChannelContentType.TV) return
        val streamId = channel.id.toIntOrNull() ?: return
        val config = xtreamConfig(profile.xtreamServer, profile.xtreamUser, profile.xtreamPass)
        val channelKey = channel.stableKey

        epgJob = viewModelScope.launch {
            repo.loadXtreamProgram(config, streamId)
                .onSuccess { program ->
                    if (_currentChannel.value?.stableKey == channelKey) {
                        _currentProgram.value = program
                    }
                }
        }
    }

    private fun rememberRecentChannel(channel: Channel) {
        _recentChannels.value = (listOf(channel) + _recentChannels.value.filterNot {
            it.stableKey == channel.stableKey
        }).take(maxRecentChannels)
    }

    fun onPlaybackStateChanged(state: PlaybackState) { _playbackState.value = state }
    fun onVideoInfoChanged(info: VideoInfo) { _videoInfo.value = info }

    fun setPlayerType(type: PlayerType) {
        if (type != PlayerType.EXOPLAYER) return
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

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode.name) }
    }

    fun setAdultPin(pin: String) {
        _adultUnlocked.value = false
        _adultUnlockedChannelKeys.value = emptySet()
        viewModelScope.launch { prefs.setAdultPin(pin) }
    }

    fun setAdultAccessMode(mode: AdultAccessMode) {
        _adultUnlocked.value = false
        _adultUnlockedChannelKeys.value = emptySet()
        viewModelScope.launch { prefs.setAdultAccessMode(mode.name) }
    }

    fun unlockAdult(channel: Channel, pin: String): Boolean {
        val unlocked = pin == adultPin.value
        if (!unlocked) return false
        when (adultMode()) {
            AdultAccessMode.SESSION -> _adultUnlocked.value = true
            AdultAccessMode.PER_CHANNEL -> {
                _adultUnlockedChannelKeys.value = _adultUnlockedChannelKeys.value + channel.stableKey
            }
            AdultAccessMode.HIDDEN -> return false
        }
        return unlocked
    }

    fun lockAdult() {
        _adultUnlocked.value = false
        _adultUnlockedChannelKeys.value = emptySet()
    }

    fun toggleFavorite(channel: Channel) {
        val nextFavorite = !channel.isFavorite
        viewModelScope.launch { prefs.setFavoriteChannel(channel.stableKey, nextFavorite) }
        if (_currentChannel.value?.stableKey == channel.stableKey) {
            _currentChannel.value = channel.copy(isFavorite = nextFavorite)
        }
        _recentChannels.value = _recentChannels.value.map {
            if (it.stableKey == channel.stableKey) it.copy(isFavorite = nextFavorite) else it
        }
    }

    fun nextChannel() {
        val channels = visibleChannels.value.filter { isChannelPlayable(it) }
        val currentKey = _currentChannel.value?.stableKey
        val idx = channels.indexOfFirst { it.stableKey == currentKey }
        (channels.getOrNull(idx + 1) ?: channels.firstOrNull())?.let { selectChannel(it) }
    }

    fun prevChannel() {
        val channels = visibleChannels.value.filter { isChannelPlayable(it) }
        val currentKey = _currentChannel.value?.stableKey
        val idx = channels.indexOfFirst { it.stableKey == currentKey }
        (channels.getOrNull(idx - 1) ?: channels.lastOrNull())?.let { selectChannel(it) }
    }

    private fun preferredContentTypeChannels(groups: List<ChannelGroup>): List<Channel> =
        contentTypeOrder.firstNotNullOfOrNull { type ->
            groups.flatMap { group -> group.channels.filter { it.contentType == type } }
                .takeIf { it.isNotEmpty() }
        } ?: emptyList()

    private fun groupsForSelectedContent(
        contentType: ChannelContentType = _selectedContentType.value
    ): List<ChannelGroup> {
        val favorites = favoriteChannelKeys.value
        val adultMode = adultAccessMode.value
        val filteredGroups = _groups.value.mapNotNull { group ->
            val channels = group.channels
                .filterVisibleForAdultMode(adultMode)
                .filter { it.contentType == contentType }
                .markFavorites(favorites)
            if (channels.isEmpty()) null else group.copy(channels = channels)
        }
        val favoriteChannels = filteredGroups
            .flatMap { it.channels }
            .distinctBy { it.stableKey }
            .filter { it.stableKey in favorites }
        return if (favoriteChannels.isEmpty()) filteredGroups
        else listOf(ChannelGroup(FAVORITE_GROUP_NAME, favoriteChannels)) + filteredGroups
    }

    private fun List<Channel>.markFavorites(favorites: Set<String>): List<Channel> =
        map { channel -> channel.copy(isFavorite = channel.stableKey in favorites) }

    private fun List<Channel>.filterVisibleForAdultMode(mode: String): List<Channel> =
        if (mode == AdultAccessMode.HIDDEN.name) filterNot { it.isAdult } else this

    fun isAdultLocked(channel: Channel): Boolean =
        channel.isAdult && !isChannelPlayable(channel)

    private fun Channel.isPlayable(): Boolean = isChannelPlayable(this)

    private fun isChannelPlayable(channel: Channel): Boolean {
        if (!channel.isAdult) return true
        return when (adultMode()) {
            AdultAccessMode.SESSION -> _adultUnlocked.value
            AdultAccessMode.PER_CHANNEL -> channel.stableKey in _adultUnlockedChannelKeys.value
            AdultAccessMode.HIDDEN -> false
        }
    }

    private fun adultMode(): AdultAccessMode =
        runCatching { AdultAccessMode.valueOf(adultAccessMode.value) }
            .getOrDefault(AdultAccessMode.PER_CHANNEL)

    private fun firstPlayableVisibleChannel(): Channel? =
        groupsForSelectedContent().flatMap { it.channels }.firstOrNull { isChannelPlayable(it) }

    companion object {
        const val FAVORITE_GROUP_NAME = "Favoriler"

        private val contentTypeOrder = listOf(
            ChannelContentType.TV,
            ChannelContentType.MOVIE,
            ChannelContentType.SERIES
        )
    }
}
