package az.iptv.fplayer.ui.screen

import android.app.Activity
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelContentType
import az.iptv.fplayer.data.preferences.AdultAccessMode
import az.iptv.fplayer.data.preferences.PlaylistProfile
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.ExoPlayerEngine
import az.iptv.fplayer.player.MediaTrackOption
import az.iptv.fplayer.player.MediaTracks
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.PlayerEngine
import az.iptv.fplayer.player.PlayerEventListener
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.player.VlcPlayerEngine
import az.iptv.fplayer.ui.component.ChannelInfoOsd
import az.iptv.fplayer.ui.component.ChannelLogo
import az.iptv.fplayer.ui.component.TechBadge
import az.iptv.fplayer.ui.text.appTexts
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private enum class SelectorPane { CONTENT_TYPES, GROUPS, CHANNELS }
private enum class GuideCategoryType { PLAYLIST, ALL, GROUP }

private data class GuideCategoryItem(
    val type: GuideCategoryType,
    val label: String,
    val count: Int? = null,
    val groupName: String? = null,
    val playlist: PlaylistProfile? = null,
    val selected: Boolean = false,
    val active: Boolean = false
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateToPlaylist: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val groups by vm.groups.collectAsState()
    val currentChannel by vm.currentChannel.collectAsState()
    val playbackState by vm.playbackState.collectAsState()
    val videoInfo by vm.videoInfo.collectAsState()
    val currentProgram by vm.currentProgram.collectAsState()
    val selectorVisible by vm.sidebarVisible.collectAsState()
    val osdVisible by vm.osdVisible.collectAsState()
    val visibleChannels by vm.visibleChannels.collectAsState()
    val selectedGroup by vm.selectedGroup.collectAsState()
    val selectedContentType by vm.selectedContentType.collectAsState()
    val availableContentTypes by vm.availableContentTypes.collectAsState()
    val loadState by vm.loadState.collectAsState()
    val playerType by vm.playerType.collectAsState()
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val activePlaylist by vm.activePlaylist.collectAsState()
    val recentChannels by vm.recentChannels.collectAsState()
    val language by vm.appLanguage.collectAsState()
    val adultUnlocked by vm.adultUnlocked.collectAsState()
    val adultAccessMode by vm.adultAccessMode.collectAsState()
    val adultUnlockedChannelKeys by vm.adultUnlockedChannelKeys.collectAsState()
    val t = appTexts(language)
    fun isAdultLocked(channel: Channel): Boolean {
        if (!channel.isAdult) return false
        return when (adultAccessMode) {
            AdultAccessMode.SESSION.name -> !adultUnlocked
            AdultAccessMode.PER_CHANNEL.name -> channel.stableKey !in adultUnlockedChannelKeys
            else -> true
        }
    }
    val guideContentTypes = availableContentTypes.ifEmpty { listOf(selectedContentType) }
    val guideCategories = remember(playlists, activePlaylist?.id, groups, selectedGroup, t.allChannels) {
        buildGuideCategories(
            playlists = playlists,
            activePlaylistId = activePlaylist?.id,
            groups = groups,
            selectedGroup = selectedGroup,
            allChannelsLabel = t.allChannels
        )
    }
    var mediaTracks by remember { mutableStateOf(MediaTracks()) }
    var recoveryChannelKey by remember { mutableStateOf<String?>(null) }

    val engine: PlayerEngine = remember(playerType, audioDecoderMode, context) {
        when (playerType) {
            PlayerType.VLC -> VlcPlayerEngine(context)
            else -> ExoPlayerEngine(context, audioDecoderMode)
        }
    }

    DisposableEffect(engine) {
        engine.setEventListener(object : PlayerEventListener {
            override fun onStateChanged(state: PlaybackState) = vm.onPlaybackStateChanged(state)
            override fun onVideoInfoChanged(info: VideoInfo) = vm.onVideoInfoChanged(info)
            override fun onMediaTracksChanged(tracks: MediaTracks) {
                mediaTracks = tracks
            }
        })
        onDispose { engine.release() }
    }

    LaunchedEffect(Unit) {
        if (groups.isEmpty()) vm.autoLoadSavedPlaylist()
    }

    val playbackUrl = currentChannel?.url
    LaunchedEffect(engine, playbackUrl) {
        playbackUrl?.let {
            vm.onVideoInfoChanged(VideoInfo())
            engine.play(it)
        }
    }

    LaunchedEffect(playbackState, currentChannel?.stableKey) {
        val key = currentChannel?.stableKey ?: return@LaunchedEffect
        if (playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering) {
            recoveryChannelKey = null
            return@LaunchedEffect
        }
        if (playbackState is PlaybackState.Error || playbackState is PlaybackState.Idle) {
            if (recoveryChannelKey == key) return@LaunchedEffect
            delay(2500)
            recoveryChannelKey = key
            vm.refreshPlaylist()
        }
    }

    val currentChannelKey = currentChannel?.stableKey
    val currentChannelIndex = remember(visibleChannels, currentChannelKey) {
        visibleChannels.indexOfFirst { it.stableKey == currentChannelKey }
    }
    val channelIndex = currentChannelIndex + 1

    var focusedChannelIndex by remember { mutableIntStateOf(0) }
    var focusedGroupIndex by remember { mutableIntStateOf(0) }
    var focusedContentTypeIndex by remember { mutableIntStateOf(0) }
    var focusedMediaOption by remember { mutableIntStateOf(0) }
    var selectorPane by remember { mutableStateOf(SelectorPane.CHANNELS) }
    var mediaOptionsVisible by remember { mutableStateOf(false) }
    var audioTrackPanelVisible by remember { mutableStateOf(false) }
    var recentOverlayVisible by remember { mutableStateOf(false) }
    var channelOnlyGuideVisible by remember { mutableStateOf(false) }
    var focusedRecentIndex by remember { mutableIntStateOf(0) }
    var focusedAudioTrackIndex by remember { mutableIntStateOf(0) }
    var adultPinPromptVisible by remember { mutableStateOf(false) }
    var adultPinInput by remember { mutableStateOf("") }
    var adultPinError by remember { mutableStateOf(false) }
    var focusedPinDigit by remember { mutableIntStateOf(0) }
    var pendingAdultChannel by remember { mutableStateOf<Channel?>(null) }
    var exitPromptVisible by remember { mutableStateOf(false) }
    var exitHintVisible by remember { mutableStateOf(false) }
    var lastExitBackAt by remember { mutableStateOf(0L) }
    var leftPressCount by remember { mutableIntStateOf(0) }
    var lastLeftPressAt by remember { mutableStateOf(0L) }

    fun openPlaylistMenu() {
        leftPressCount = 0
        channelOnlyGuideVisible = false
        recentOverlayVisible = false
        mediaOptionsVisible = false
        vm.hideSidebar()
        onNavigateToPlaylist()
    }

    fun selectOrRequestAdultPin(channel: Channel) {
        if (isAdultLocked(channel)) {
            pendingAdultChannel = channel
            adultPinInput = ""
            adultPinError = false
            focusedPinDigit = 0
            adultPinPromptVisible = true
            recentOverlayVisible = false
            mediaOptionsVisible = false
            audioTrackPanelVisible = false
            vm.hideSidebar()
            vm.hideOsd()
            return
        }
        vm.selectChannel(channel)
    }

    fun submitAdultPin() {
        val channel = pendingAdultChannel
        if (channel != null && vm.unlockAdult(channel, adultPinInput)) {
            adultPinPromptVisible = false
            adultPinError = false
            vm.selectChannel(channel)
            pendingAdultChannel = null
        } else {
            adultPinError = true
            adultPinInput = ""
        }
    }

    fun appendAdultPinDigit() {
        if (adultPinInput.length >= 4) return
        val next = adultPinInput + focusedPinDigit.toString()
        adultPinInput = next
        adultPinError = false
        if (next.length == 4) {
            val channel = pendingAdultChannel
            if (channel != null && vm.unlockAdult(channel, next)) {
                adultPinPromptVisible = false
                vm.selectChannel(channel)
                pendingAdultChannel = null
            } else {
                adultPinError = true
                adultPinInput = ""
            }
        }
    }

    LaunchedEffect(selectorVisible) {
        if (selectorVisible) {
            focusedChannelIndex = currentChannelIndex.coerceAtLeast(0)
            focusedGroupIndex = selectedGuideCategoryIndex(guideCategories)
            focusedContentTypeIndex = selectedContentTypeIndex(guideContentTypes, selectedContentType)
        }
    }

    LaunchedEffect(visibleChannels, currentChannelKey) {
        focusedChannelIndex = currentChannelIndex.coerceAtLeast(0)
    }

    LaunchedEffect(guideContentTypes, selectedContentType) {
        focusedContentTypeIndex = focusedContentTypeIndex.coerceIn(
            0,
            guideContentTypes.size
        )
        if (focusedContentTypeIndex < guideContentTypes.size) {
            focusedContentTypeIndex = selectedContentTypeIndex(guideContentTypes, selectedContentType)
        }
    }

    LaunchedEffect(guideCategories.size) {
        focusedGroupIndex = focusedGroupIndex.coerceAtMost((guideCategories.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(recentOverlayVisible, recentChannels.size) {
        if (recentOverlayVisible) {
            focusedRecentIndex = focusedRecentIndex.coerceIn(
                0,
                (recentChannels.take(5).size - 1).coerceAtLeast(0)
            )
        }
    }

    LaunchedEffect(audioTrackPanelVisible, mediaTracks.audioTracks) {
        if (audioTrackPanelVisible) {
            focusedAudioTrackIndex = mediaTracks.audioTracks
                .indexOfFirst { it.selected }
                .takeIf { it >= 0 }
                ?: 0
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(exitHintVisible) {
        if (exitHintVisible) {
            delay(2200)
            exitHintVisible = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val isExitKey = event.key == Key.Back
                val isSelectKey = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.DirectionCenter
                if (adultPinPromptVisible) {
                    when {
                        isExitKey -> {
                            adultPinPromptVisible = false
                            pendingAdultChannel = null
                            adultPinInput = ""
                            adultPinError = false
                            true
                        }
                        isSelectKey -> {
                            appendAdultPinDigit()
                            true
                        }
                        event.key == Key.DirectionLeft -> {
                            focusedPinDigit = (focusedPinDigit + 9) % 10
                            adultPinError = false
                            true
                        }
                        event.key == Key.DirectionRight -> {
                            focusedPinDigit = (focusedPinDigit + 1) % 10
                            adultPinError = false
                            true
                        }
                        event.key == Key.DirectionUp -> {
                            focusedPinDigit = (focusedPinDigit + 9) % 10
                            adultPinError = false
                            true
                        }
                        event.key == Key.DirectionDown -> {
                            focusedPinDigit = (focusedPinDigit + 1) % 10
                            adultPinError = false
                            true
                        }
                        else -> true
                    }
                } else if (audioTrackPanelVisible) {
                    when (event.key) {
                        Key.Back -> {
                            audioTrackPanelVisible = false
                            true
                        }
                        Key.DirectionUp -> {
                            focusedAudioTrackIndex = (focusedAudioTrackIndex - 1).coerceAtLeast(0)
                            true
                        }
                        Key.DirectionDown -> {
                            focusedAudioTrackIndex = (focusedAudioTrackIndex + 1)
                                .coerceAtMost((mediaTracks.audioTracks.size - 1).coerceAtLeast(0))
                            true
                        }
                        Key.DirectionRight, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            mediaTracks.audioTracks.getOrNull(focusedAudioTrackIndex)?.let {
                                engine.selectAudioTrack(it.id)
                            }
                            audioTrackPanelVisible = false
                            vm.showOsd()
                            true
                        }
                        Key.DirectionLeft -> {
                            audioTrackPanelVisible = false
                            true
                        }
                        else -> true
                    }
                } else if (exitPromptVisible) {
                    when {
                        isSelectKey -> {
                            (context as? Activity)?.finish()
                            true
                        }
                        isExitKey -> {
                            exitPromptVisible = false
                            true
                        }
                        else -> true
                    }
                } else if (recentOverlayVisible) {
                    val recentList = recentChannels.take(5)
                    when (event.key) {
                        Key.Back -> {
                            recentOverlayVisible = false
                            true
                        }
                        Key.DirectionUp -> {
                            focusedRecentIndex = (focusedRecentIndex - 1).coerceAtLeast(0)
                            true
                        }
                        Key.DirectionDown -> {
                            focusedRecentIndex = (focusedRecentIndex + 1)
                                .coerceAtMost((recentList.size - 1).coerceAtLeast(0))
                            true
                        }
                        Key.DirectionLeft -> {
                            recentOverlayVisible = false
                            channelOnlyGuideVisible = false
                            focusedGroupIndex = selectedPlaylistCategoryIndex(guideCategories, activePlaylist?.id)
                            selectorPane = SelectorPane.GROUPS
                            vm.showSidebar()
                            true
                        }
                        Key.DirectionRight, Key.Info -> {
                            recentList.getOrNull(focusedRecentIndex)?.let(vm::toggleFavorite)
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            recentList.getOrNull(focusedRecentIndex)?.let {
                                recentOverlayVisible = false
                                selectOrRequestAdultPin(it)
                            }
                            true
                        }
                        else -> true
                    }
                } else if (mediaOptionsVisible) {
                    when (event.key) {
                        Key.Back, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            mediaOptionsVisible = false
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            if (mediaTracks.subtitleTracks.isNotEmpty() && mediaTracks.audioTracks.isNotEmpty()) {
                                focusedMediaOption = if (focusedMediaOption == 0) 1 else 0
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            changeSelectedMediaTrack(
                                mediaTracks = mediaTracks,
                                focusedMediaOption = focusedMediaOption,
                                delta = -1,
                                onAudioTrack = engine::selectAudioTrack,
                                onSubtitleTrack = engine::selectSubtitleTrack
                            )
                            true
                        }
                        Key.DirectionRight -> {
                            if (focusedMediaOption == 0 && mediaTracks.audioTracks.size > 2) {
                                focusedAudioTrackIndex = mediaTracks.audioTracks
                                    .indexOfFirst { it.selected }
                                    .takeIf { it >= 0 }
                                    ?: 0
                                mediaOptionsVisible = false
                                audioTrackPanelVisible = true
                            } else {
                                changeSelectedMediaTrack(
                                    mediaTracks = mediaTracks,
                                    focusedMediaOption = focusedMediaOption,
                                    delta = 1,
                                    onAudioTrack = engine::selectAudioTrack,
                                    onSubtitleTrack = engine::selectSubtitleTrack
                                )
                            }
                            true
                        }
                        else -> true
                    }
                } else if (isExitKey) {
                    when {
                        selectorVisible -> {
                            vm.hideSidebar()
                            true
                        }
                        osdVisible -> {
                            vm.hideOsd()
                            true
                        }
                        else -> {
                            val now = System.currentTimeMillis()
                            if (now - lastExitBackAt <= 2200L) {
                                exitHintVisible = false
                                exitPromptVisible = true
                            } else {
                                lastExitBackAt = now
                                exitHintVisible = true
                            }
                            true
                        }
                    }
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (selectorVisible) {
                                leftPressCount = 0
                                if (channelOnlyGuideVisible) {
                                    channelOnlyGuideVisible = false
                                    focusedGroupIndex = selectedGuideCategoryIndex(guideCategories)
                                    selectorPane = SelectorPane.GROUPS
                                } else {
                                    when (selectorPane) {
                                        SelectorPane.CHANNELS -> {
                                            focusedGroupIndex = selectedGuideCategoryIndex(guideCategories)
                                            selectorPane = SelectorPane.GROUPS
                                        }
                                        SelectorPane.GROUPS -> {
                                            val playlistIndex = selectedPlaylistCategoryIndex(
                                                guideCategories,
                                                activePlaylist?.id
                                            )
                                            if (
                                                guideCategories.any { it.type == GuideCategoryType.PLAYLIST } &&
                                                focusedGroupIndex != playlistIndex
                                            ) {
                                                focusedGroupIndex = playlistIndex
                                            } else {
                                                selectorPane = SelectorPane.CONTENT_TYPES
                                            }
                                        }
                                        SelectorPane.CONTENT_TYPES -> Unit
                                    }
                                }
                            } else {
                                val now = System.currentTimeMillis()
                                leftPressCount = if (now - lastLeftPressAt <= 1400L) leftPressCount + 1 else 1
                                lastLeftPressAt = now
                                if (leftPressCount >= 3) {
                                    openPlaylistMenu()
                                    return@onPreviewKeyEvent true
                                }
                                channelOnlyGuideVisible = false
                                focusedGroupIndex = selectedPlaylistCategoryIndex(guideCategories, activePlaylist?.id)
                                selectorPane = SelectorPane.GROUPS
                                vm.showSidebar()
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            if (selectorVisible) {
                                when (selectorPane) {
                                    SelectorPane.CONTENT_TYPES -> {
                                        if (focusedContentTypeIndex >= guideContentTypes.size) {
                                            openPlaylistMenu()
                                        } else {
                                            selectorPane = SelectorPane.GROUPS
                                        }
                                    }
                                    SelectorPane.GROUPS -> selectorPane = SelectorPane.CHANNELS
                                    SelectorPane.CHANNELS -> {
                                        if (hasSelectableMediaTracks(mediaTracks)) {
                                            focusedMediaOption = if (mediaTracks.audioTracks.isNotEmpty()) 0 else 1
                                            mediaOptionsVisible = true
                                            audioTrackPanelVisible = false
                                            vm.hideSidebar()
                                            vm.hideOsd()
                                        } else {
                                            visibleChannels.getOrNull(focusedChannelIndex)?.let(vm::toggleFavorite)
                                        }
                                    }
                                }
                            } else if (osdVisible && mediaTracks.audioTracks.size > 1) {
                                recentOverlayVisible = false
                                mediaOptionsVisible = false
                                focusedMediaOption = 0
                                if (mediaTracks.audioTracks.size > 2) {
                                    focusedAudioTrackIndex = mediaTracks.audioTracks
                                        .indexOfFirst { it.selected }
                                        .takeIf { it >= 0 }
                                        ?: 0
                                    audioTrackPanelVisible = true
                                } else {
                                    changeSelectedMediaTrack(
                                        mediaTracks = mediaTracks,
                                        focusedMediaOption = 0,
                                        delta = 1,
                                        onAudioTrack = engine::selectAudioTrack,
                                        onSubtitleTrack = engine::selectSubtitleTrack
                                    )
                                    vm.showOsd()
                                }
                            } else {
                                val recentList = recentChannels.take(5)
                                if (!osdVisible && recentList.isNotEmpty()) {
                                    focusedRecentIndex = 0
                                    recentOverlayVisible = true
                                    vm.hideSidebar()
                                } else if (!osdVisible) {
                                    vm.showOsd()
                                }
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            if (selectorVisible) {
                                when (selectorPane) {
                                    SelectorPane.CONTENT_TYPES -> {
                                        focusedContentTypeIndex = (focusedContentTypeIndex - 1).coerceAtLeast(0)
                                    }
                                    SelectorPane.GROUPS -> {
                                        focusedGroupIndex = (focusedGroupIndex - 1).coerceAtLeast(0)
                                    }
                                    SelectorPane.CHANNELS -> {
                                        focusedChannelIndex = (focusedChannelIndex - 1).coerceAtLeast(0)
                                    }
                                }
                                true
                            } else {
                                vm.nextChannel()
                                true
                            }
                        }

                        Key.DirectionDown -> {
                            if (selectorVisible) {
                                when (selectorPane) {
                                    SelectorPane.CONTENT_TYPES -> {
                                        focusedContentTypeIndex = (focusedContentTypeIndex + 1)
                                            .coerceAtMost(guideContentTypes.size)
                                    }
                                    SelectorPane.GROUPS -> {
                                        focusedGroupIndex = (focusedGroupIndex + 1)
                                            .coerceAtMost((guideCategories.size - 1).coerceAtLeast(0))
                                    }
                                    SelectorPane.CHANNELS -> {
                                        focusedChannelIndex = (focusedChannelIndex + 1)
                                            .coerceAtMost((visibleChannels.size - 1).coerceAtLeast(0))
                                    }
                                }
                                true
                            } else {
                                vm.prevChannel()
                                true
                            }
                        }

                        Key.ChannelUp, Key.PageUp -> {
                            if (!selectorVisible) {
                                vm.nextChannel()
                                true
                            } else false
                        }

                        Key.ChannelDown, Key.PageDown -> {
                            if (!selectorVisible) {
                                vm.prevChannel()
                                true
                            } else false
                        }

                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (selectorVisible) {
                                when (selectorPane) {
                                    SelectorPane.CONTENT_TYPES -> {
                                        if (focusedContentTypeIndex >= guideContentTypes.size) {
                                            openPlaylistMenu()
                                        } else {
                                            guideContentTypes.getOrNull(focusedContentTypeIndex)?.let {
                                                vm.selectContentType(it)
                                                focusedGroupIndex = 0
                                                focusedChannelIndex = 0
                                                selectorPane = SelectorPane.GROUPS
                                            }
                                        }
                                    }
                                    SelectorPane.GROUPS -> {
                                        guideCategories.getOrNull(focusedGroupIndex)?.let { item ->
                                            when (item.type) {
                                                GuideCategoryType.PLAYLIST -> item.playlist?.let(vm::switchPlaylist)
                                                GuideCategoryType.ALL -> vm.selectGroup(null)
                                                GuideCategoryType.GROUP -> vm.selectGroup(item.groupName)
                                            }
                                        }
                                        selectorPane = SelectorPane.CHANNELS
                                        focusedChannelIndex = 0
                                    }
                                    SelectorPane.CHANNELS -> {
                                        visibleChannels.getOrNull(focusedChannelIndex)?.let {
                                            mediaOptionsVisible = false
                                            audioTrackPanelVisible = false
                                            selectOrRequestAdultPin(it)
                                        }
                                    }
                                }
                                true
                            } else {
                                recentOverlayVisible = false
                                mediaOptionsVisible = false
                                channelOnlyGuideVisible = true
                                selectorPane = SelectorPane.CHANNELS
                                vm.showSidebar()
                                true
                            }
                        }

                        Key.Menu -> {
                            openPlaylistMenu()
                            true
                        }

                        Key.Info -> {
                            if (selectorVisible && selectorPane == SelectorPane.CHANNELS) {
                                visibleChannels.getOrNull(focusedChannelIndex)?.let(vm::toggleFavorite)
                            } else if (hasSelectableMediaTracks(mediaTracks)) {
                                focusedMediaOption = if (mediaTracks.audioTracks.isNotEmpty()) 0 else 1
                                mediaOptionsVisible = true
                                audioTrackPanelVisible = false
                                vm.hideOsd()
                            } else {
                                vm.showOsd()
                            }
                            true
                        }

                        else -> false
                    }
                }
            }
    ) {
        val isWide = maxWidth > 700.dp
        val guideWidth = if (isWide) (maxWidth * 0.92f).coerceIn(820.dp, 1240.dp) else maxWidth * 0.98f
        VideoSurface(engine = engine)

        AnimatedVisibility(
            visible = selectorVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            ReceiverGuideOverlay(
                contentTypes = guideContentTypes,
                selectedContentType = selectedContentType,
                focusedContentTypeIndex = focusedContentTypeIndex,
                categories = guideCategories,
                selectedGroup = selectedGroup,
                focusedGroupIndex = focusedGroupIndex,
                channels = visibleChannels,
                currentChannel = currentChannel,
                focusedChannelIndex = focusedChannelIndex,
                focusedPane = if (channelOnlyGuideVisible) SelectorPane.CHANNELS else selectorPane,
                guideWidth = guideWidth,
                channelsOnly = channelOnlyGuideVisible,
                isLoading = loadState is LoadState.Loading,
                categoriesLabel = t.categories,
                allChannelsLabel = t.allChannels,
                emptyLabel = t.noInfo,
                activeLabel = t.active,
                favoriteLabel = t.favorite,
                lockedLabel = t.locked,
                isAdultLocked = ::isAdultLocked,
                onContentTypeClick = {
                    vm.selectContentType(it)
                    focusedGroupIndex = 0
                    focusedChannelIndex = 0
                    selectorPane = SelectorPane.GROUPS
                },
                onMenuClick = { openPlaylistMenu() },
                onCategoryClick = { item ->
                    when (item.type) {
                        GuideCategoryType.PLAYLIST -> item.playlist?.let(vm::switchPlaylist)
                        GuideCategoryType.ALL -> vm.selectGroup(null)
                        GuideCategoryType.GROUP -> vm.selectGroup(item.groupName)
                    }
                    selectorPane = SelectorPane.CHANNELS
                    focusedChannelIndex = 0
                },
                onChannelClick = ::selectOrRequestAdultPin
            )
        }

        AnimatedVisibility(
            visible = playbackState is PlaybackState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .zIndex(4f)
        ) {
            val msg = (playbackState as? PlaybackState.Error)?.message ?: ""
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD1D1F23))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(msg, color = Color(0xFFFFD4D4), fontSize = 14.sp, maxLines = 2)
            }
        }

        ChannelInfoOsd(
            visible = osdVisible && !selectorVisible,
            channel = currentChannel,
            videoInfo = videoInfo,
            mediaTracks = mediaTracks,
            programInfo = currentProgram,
            playbackState = playbackState,
            channelIndex = channelIndex,
            totalChannels = visibleChannels.size,
            allChannelsLabel = t.allChannels,
            programLabel = t.program,
            audioLabel = t.audio,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(5f)
        )

        RecentChannelsOverlay(
            visible = recentOverlayVisible && !selectorVisible,
            channels = recentChannels.take(5),
            currentChannel = currentChannel,
            focusedIndex = focusedRecentIndex,
            title = t.recentChannels,
            emptyLabel = t.noInfo,
            favoriteLabel = t.favorite,
            lockedLabel = t.locked,
            isAdultLocked = ::isAdultLocked,
            onChannelClick = {
                recentOverlayVisible = false
                selectOrRequestAdultPin(it)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 34.dp)
                .zIndex(18f)
        )

        ExitHint(
            visible = exitHintVisible,
            text = t.exitHint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .zIndex(20f)
        )

        ExitConfirmDialog(
            visible = exitPromptVisible,
            title = t.exitTitle,
            subtitle = t.exitSubtitle,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(21f)
        )

        MediaOptionsOverlay(
            visible = mediaOptionsVisible,
            tracks = mediaTracks,
            focusedOption = focusedMediaOption,
            title = t.mediaOptions,
            audioLabel = t.audio,
            subtitlesLabel = t.subtitles,
            subtitlesOffLabel = t.off,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(22f)
        )

        AudioTrackPickerOverlay(
            visible = audioTrackPanelVisible,
            tracks = mediaTracks.audioTracks,
            focusedIndex = focusedAudioTrackIndex,
            title = t.audio,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 42.dp, bottom = 158.dp)
                .zIndex(23f)
        )

        AdultPinDialog(
            visible = adultPinPromptVisible,
            title = t.enterPin,
            error = if (adultPinError) t.wrongPin else "",
            pinLength = adultPinInput.length,
            focusedDigit = focusedPinDigit,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(24f)
        )

        if (!isWide && !selectorVisible) {
            MobileTapLayer(
                onTap = {
                    channelOnlyGuideVisible = true
                    vm.showSidebar()
                    selectorPane = SelectorPane.CHANNELS
                },
                onNext = vm::nextChannel,
                onPrevious = vm::prevChannel,
                modifier = Modifier.zIndex(6f)
            )
        }
    }
}

@Composable
private fun VideoSurface(engine: PlayerEngine) {
    AndroidView(
        factory = { ctx -> SurfaceView(ctx).also { engine.init(it) } },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExitHint(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xC81D2228))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExitConfirmDialog(visible: Boolean, title: String, subtitle: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE61D2228))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFFC8CCD1),
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackScrim(playlist: String, group: String, visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66000000),
                        0.16f to Color.Transparent,
                        0.74f to Color.Transparent,
                        1f to Color(0x88000000)
                    )
                )
        ) {
            Text(
                text = "$playlist  •  $group",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 32.dp, top = 24.dp)
            )
            Text(
                text = "Wed, Apr 17, 12:25 PM",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 32.dp, top = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RecentChannelsOverlay(
    visible: Boolean,
    channels: List<Channel>,
    currentChannel: Channel?,
    focusedIndex: Int,
    title: String,
    emptyLabel: String,
    favoriteLabel: String,
    lockedLabel: String,
    isAdultLocked: (Channel) -> Boolean,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 430.dp)
                .fillMaxHeight(0.58f)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xB0090B0F), Color(0x82161B20), Color(0x34FFFFFF))
                    )
                )
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = channels.size.toString(),
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(channels, key = { index, channel -> "${channel.stableKey}#$index" }) { index, channel ->
                    ReceiverChannelRow(
                        channel = channel,
                        index = index + 1,
                        isPlaying = currentChannel?.stableKey == channel.stableKey,
                        isFocused = index == focusedIndex,
                        groupFallbackLabel = emptyLabel,
                        favoriteLabel = favoriteLabel,
                        lockedLabel = lockedLabel,
                        isAdultLocked = isAdultLocked(channel),
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverGuideOverlay(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType,
    focusedContentTypeIndex: Int,
    categories: List<GuideCategoryItem>,
    selectedGroup: String?,
    focusedGroupIndex: Int,
    channels: List<Channel>,
    currentChannel: Channel?,
    focusedChannelIndex: Int,
    focusedPane: SelectorPane,
    guideWidth: androidx.compose.ui.unit.Dp,
    channelsOnly: Boolean = false,
    isLoading: Boolean,
    categoriesLabel: String,
    allChannelsLabel: String,
    emptyLabel: String,
    activeLabel: String,
    favoriteLabel: String,
    lockedLabel: String,
    isAdultLocked: (Channel) -> Boolean,
    onContentTypeClick: (ChannelContentType) -> Unit,
    onMenuClick: () -> Unit,
    onCategoryClick: (GuideCategoryItem) -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    val panelShape = RoundedCornerShape(10.dp)
    val panelWidth = if (channelsOnly) (guideWidth * 0.56f).coerceIn(500.dp, 700.dp) else guideWidth
    val railWidth = if (guideWidth >= 900.dp) 132.dp else 102.dp
    val groupWidth = if (guideWidth >= 1000.dp) 370.dp else 286.dp
    val footerChannel = channels.getOrNull(focusedChannelIndex) ?: currentChannel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x38000000)),
        contentAlignment = if (channelsOnly) Alignment.CenterStart else Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(start = if (channelsOnly) 38.dp else 0.dp)
                .width(panelWidth)
                .fillMaxHeight(0.82f)
                .clip(panelShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xB80B0D10),
                            Color(0x86161A20),
                            Color(0x4DFFFFFF)
                        )
                    )
                )
                .border(1.dp, Color(0x68FFFFFF), panelShape)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                if (!channelsOnly) {
                    ContentTypeColumn(
                        contentTypes = contentTypes,
                        selectedContentType = selectedContentType,
                        focusedIndex = focusedContentTypeIndex,
                        focused = focusedPane == SelectorPane.CONTENT_TYPES,
                        onContentTypeClick = onContentTypeClick,
                        menuLabel = "Menu",
                        onMenuClick = onMenuClick,
                        modifier = Modifier
                            .width(railWidth)
                            .fillMaxHeight()
                    )

                    ReceiverGuideDivider()

                    ReceiverCategoryColumn(
                        categories = categories,
                        selectedGroup = selectedGroup,
                        focusedIndex = focusedGroupIndex,
                        focused = focusedPane == SelectorPane.GROUPS,
                        categoriesLabel = categoriesLabel,
                        allChannelsLabel = allChannelsLabel,
                        activeLabel = activeLabel,
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier
                            .width(groupWidth)
                            .fillMaxHeight()
                    )

                    ReceiverGuideDivider()
                }

                ReceiverChannelColumn(
                    channels = channels,
                    currentChannel = currentChannel,
                    focusedIndex = focusedChannelIndex,
                    focused = focusedPane == SelectorPane.CHANNELS,
                    isLoading = isLoading,
                    emptyLabel = emptyLabel,
                    favoriteLabel = favoriteLabel,
                    lockedLabel = lockedLabel,
                    isAdultLocked = isAdultLocked,
                    onChannelClick = onChannelClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            ReceiverFooter(
                channel = footerChannel,
                selectedContentType = selectedContentType,
                selectedGroup = selectedGroup,
                allChannelsLabel = allChannelsLabel
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContentTypeColumn(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType,
    focusedIndex: Int,
    focused: Boolean,
    onContentTypeClick: (ChannelContentType) -> Unit,
    menuLabel: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        contentTypes.forEachIndexed { index, type ->
            val selected = type == selectedContentType
            val activeFocus = focused && index == focusedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (activeFocus) Color(0x22FFFFFF) else Color.Transparent)
                    .border(
                        width = if (activeFocus) 2.dp else 0.dp,
                        color = Color(0xFFFF744A),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clickable { onContentTypeClick(type) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = contentTypeLabel(type),
                    color = if (activeFocus || selected) Accent else Color(0xFFEAECEF),
                    fontSize = 18.sp,
                    fontWeight = if (activeFocus || selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val menuIndex = contentTypes.size
        val activeMenuFocus = focused && focusedIndex == menuIndex
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (activeMenuFocus) Color(0xEAF2F4F6) else Color(0x201AADB1))
                .border(
                    width = if (activeMenuFocus) 2.dp else 1.dp,
                    color = if (activeMenuFocus) Color(0xFFFF744A) else Accent.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onMenuClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = menuLabel,
                color = if (activeMenuFocus) Color(0xFF101317) else Accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReceiverGuideDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxHeight()
            .width(1.dp)
            .background(Color(0x45D7DCE0))
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverCategoryColumn(
    categories: List<GuideCategoryItem>,
    selectedGroup: String?,
    focusedIndex: Int,
    focused: Boolean,
    categoriesLabel: String,
    allChannelsLabel: String,
    activeLabel: String,
    onCategoryClick: (GuideCategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = categories.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex, categories.size) {
        if (focusedIndex in categories.indices) {
            listState.scrollToItem(maxOf(0, focusedIndex - 4))
        }
    }

    Column(modifier) {
        Text(
            text = categoriesLabel,
            color = Color(0xFFEAECEF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(categories) { index, item ->
                val activeFocus = focused && index == focusedIndex
                val selected = item.selected || index == selectedIndex
                val rowShape = RoundedCornerShape(6.dp)
                val rowBg = when {
                    activeFocus -> Color(0xEAF2F4F6)
                    selected -> Color(0x331AADB1)
                    else -> Color(0x08FFFFFF)
                }
                val textColor = when {
                    activeFocus -> Color(0xFF101317)
                    selected -> Accent
                    else -> Color.White
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .height(58.dp)
                        .clip(rowShape)
                        .background(rowBg)
                        .border(
                            width = if (activeFocus) 2.dp else 1.dp,
                            color = if (activeFocus) Color(0xFFFF744A) else Color(0x18FFFFFF),
                            shape = rowShape
                        )
                        .clickable { onCategoryClick(item) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prefix = when (item.type) {
                        GuideCategoryType.PLAYLIST -> "PL"
                        GuideCategoryType.ALL -> "ALL"
                        GuideCategoryType.GROUP -> index.toString().padStart(2, '0')
                    }
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeFocus) Color(0x1A101317) else Accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prefix,
                            color = if (activeFocus) Color(0xFF101317) else Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (activeFocus || selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val meta = if (item.active && item.type == GuideCategoryType.PLAYLIST) activeLabel
                    else item.count?.toString().orEmpty()
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (activeFocus) Color(0x1A101317) else Color(0x1FFFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = meta,
                                color = if (activeFocus) Color(0xFF2E3135) else Color(0xFFCED3D8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverChannelColumn(
    channels: List<Channel>,
    currentChannel: Channel?,
    focusedIndex: Int,
    focused: Boolean,
    isLoading: Boolean,
    emptyLabel: String,
    favoriteLabel: String,
    lockedLabel: String,
    isAdultLocked: (Channel) -> Boolean,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex, channels.size) {
        if (focusedIndex in channels.indices) {
            listState.scrollToItem(maxOf(0, focusedIndex - 5))
        }
    }

    Box(modifier) {
        when {
            isLoading && channels.isEmpty() -> {
                Text(
                    text = emptyLabel,
                    color = Color(0xFFCED3D8),
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            channels.isEmpty() -> {
                Text(
                    text = emptyLabel,
                    color = Color(0xFFCED3D8),
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 28.dp, bottom = 6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(channels, key = { index, channel -> "${channel.stableKey}#$index" }) { index, channel ->
                        ReceiverChannelRow(
                            channel = channel,
                            index = index + 1,
                            isPlaying = currentChannel?.stableKey == channel.stableKey,
                            isFocused = focused && index == focusedIndex,
                            groupFallbackLabel = emptyLabel,
                            favoriteLabel = favoriteLabel,
                            lockedLabel = lockedLabel,
                            isAdultLocked = isAdultLocked(channel),
                            onClick = { onChannelClick(channel) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverChannelRow(
    channel: Channel,
    index: Int,
    isPlaying: Boolean,
    isFocused: Boolean,
    groupFallbackLabel: String,
    favoriteLabel: String,
    lockedLabel: String,
    isAdultLocked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val rowBrush = when {
        isFocused -> Brush.linearGradient(listOf(Color(0xF2FFFFFF), Color(0xDCE7ECF2), Color(0xC7FFFFFF)))
        isPlaying -> Brush.linearGradient(listOf(Color(0x78343B43), Color(0x5212161B), Color(0x3AFFFFFF)))
        else -> Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x04000000)))
    }
    val rowBorder = when {
        isFocused -> Color.White
        isPlaying -> Color(0x78FFFFFF)
        else -> Color(0x18FFFFFF)
    }
    val textColor = if (isFocused) Color(0xFF0A0D10) else Color.White
    val numberColor = if (isFocused) Color.Black else Color(0xFFEFF5FA)
    val groupColor = if (isFocused) Color(0xFF283039) else Color(0xFFC7D2DA)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(rowBrush)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = rowBorder,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString().padStart(4, '0'),
                color = numberColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(74.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = if (isFocused || isPlaying) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.group.ifBlank { groupFallbackLabel },
                    color = groupColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (channel.isFavorite) {
                ReceiverHdBadge(
                    text = favoriteLabel,
                    active = isFocused || isPlaying
                )
                Spacer(Modifier.width(6.dp))
            }
            if (isAdultLocked) {
                ReceiverHdBadge(
                    text = lockedLabel,
                    active = isFocused || isPlaying
                )
                Spacer(Modifier.width(6.dp))
            }
            ReceiverHdBadge(
                text = if (channel.name.contains("HD", ignoreCase = true)) "HD" else "SD",
                active = isFocused || isPlaying
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isFocused) Color(0xFF111417) else Color(0x66FFFFFF))
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverHdBadge(text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, if (active) Color.White else Color(0x70FFFFFF), RoundedCornerShape(3.dp))
            .background(if (active) Color(0xE8FFFFFF) else Color(0x18FFFFFF))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = if (active) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverFooter(
    channel: Channel?,
    selectedContentType: ChannelContentType,
    selectedGroup: String?,
    allChannelsLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color(0x8F13171D))
            .border(1.dp, Accent.copy(alpha = 0.28f))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = contentTypeLabel(selectedContentType),
            color = Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = selectedGroup ?: channel?.group?.takeIf { it.isNotBlank() } ?: allChannelsLabel,
            color = Color(0xFFE3E6EA),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(220.dp)
        )
        Text(
            text = channel?.name.orEmpty(),
            color = Color(0xFFFFF4B7),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryOverlay(
    groups: List<Pair<String, Int>>,
    selectedGroup: String?,
    focusedIndex: Int,
    guideWidth: androidx.compose.ui.unit.Dp,
    availableContentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType,
    categoriesLabel: String,
    allChannelsLabel: String,
    onContentTypeClick: (ChannelContentType) -> Unit,
    onGroupClick: (String?) -> Unit
) {
    val panelShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x4D000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(guideWidth)
                .fillMaxHeight(0.82f)
                .clip(panelShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xD00B0D10), Color(0xA0161A20), Color(0x36FFFFFF))
                    )
                )
                .border(1.dp, Color(0x58FFFFFF), panelShape)
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoriesLabel,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${groups.size + 1}",
                    color = Color(0xFFB9C0C8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            ContentTypeTabs(
                contentTypes = availableContentTypes,
                selectedContentType = selectedContentType,
                onContentTypeClick = onContentTypeClick,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
            )

            CategoryList(
                groups = groups,
                selectedGroup = selectedGroup,
                focusedIndex = focusedIndex,
                allChannelsLabel = allChannelsLabel,
                onGroupClick = onGroupClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaOptionsOverlay(
    visible: Boolean,
    tracks: MediaTracks,
    focusedOption: Int,
    title: String,
    audioLabel: String,
    subtitlesLabel: String,
    subtitlesOffLabel: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 560.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE8171D24))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (tracks.audioTracks.isNotEmpty()) {
                MediaOptionRow(
                    label = audioLabel,
                    value = selectedTrackLabel(tracks.audioTracks),
                    focused = focusedOption == 0
                )
            }
            if (tracks.subtitleTracks.isNotEmpty()) {
                MediaOptionRow(
                    label = subtitlesLabel,
                    value = selectedSubtitleLabel(tracks, subtitlesOffLabel),
                    focused = focusedOption == 1 || tracks.audioTracks.isEmpty()
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AudioTrackPickerOverlay(
    visible: Boolean,
    tracks: List<MediaTrackOption>,
    focusedIndex: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(visible, focusedIndex, tracks.size) {
        if (visible && focusedIndex in tracks.indices) {
            listState.scrollToItem(maxOf(0, focusedIndex - 3))
        }
    }

    AnimatedVisibility(visible = visible && tracks.isNotEmpty(), enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(min = 270.dp, max = 360.dp)
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xEA10151B))
                .border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(7.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${tracks.size}",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    val focused = index == focusedIndex
                    val selected = track.selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    focused -> Color(0xFFE9EDF1)
                                    selected -> Color(0x331AADB1)
                                    else -> Color(0x16FFFFFF)
                                }
                            )
                            .border(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) Color(0xFFFFC247) else Color(0x18FFFFFF),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            color = if (focused) Color(0xFF101317) else Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = track.label,
                            color = if (focused) Color(0xFF101317) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Text(
                                text = "OK",
                                color = if (focused) Color(0xFF101317) else Accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AdultPinDialog(
    visible: Boolean,
    title: String,
    error: String,
    pinLength: Int,
    focusedDigit: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF0181D24))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 26.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (index < pinLength) Accent.copy(alpha = 0.75f) else Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (index < pinLength) "*" else "",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(10) { digit ->
                    val focused = digit == focusedDigit
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 30.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (focused) Color(0xFFFFC247) else Color(0x18FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            color = if (focused) Color(0xFF101317) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Text(
                text = error,
                color = Color(0xFFFF8A8A),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                minLines = 1
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaOptionRow(
    label: String,
    value: String,
    focused: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color(0xFFE6E7EA) else Color(0x1AFFFFFF))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (focused) Color(0xFF111417) else Color(0xFFC8D0D6),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "< $value >",
            color = if (focused) Color(0xFF111417) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContentTypeTabs(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType,
    onContentTypeClick: (ChannelContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (contentTypes.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        contentTypes.forEach { type ->
            val selected = type == selectedContentType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Accent else Color(0x241AADB1))
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = Color(0x26FFFFFF),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onContentTypeClick(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contentTypeLabel(type),
                    color = if (selected) Color.White else Color(0xFFC8D0D6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryList(
    groups: List<Pair<String, Int>>,
    selectedGroup: String?,
    focusedIndex: Int,
    allChannelsLabel: String,
    onGroupClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allGroups = listOf(null to groups.sumOf { it.second }) + groups
    val selectedIndex = if (selectedGroup == null) 0 else groups.indexOfFirst { it.first == selectedGroup } + 1
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in allGroups.indices) {
            listState.animateScrollToItem(maxOf(0, focusedIndex - 4))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(allGroups) { index, item ->
            val name = item.first
            val count = item.second
            val selected = index == selectedIndex
            val focused = index == focusedIndex
            val bg = when {
                focused -> Color(0xFFE6E7EA)
                selected -> Color(0x331F252B)
                else -> Color.Transparent
            }
            val textColor = if (focused) Color(0xFF101317) else Color.White

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(bg)
                    .clickable { onGroupClick(name) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name ?: allChannelsLabel,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$count",
                    color = if (focused) Color(0xFF33363A) else Color(0xFFB7BAC0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramTimeline(
    channel: Channel,
    channelIndex: Int,
    modifier: Modifier = Modifier
) {
    val times = listOf(
        "08:00 AM" to "No information",
        "09:00 AM" to "No information",
        "09:08 AM" to "Program",
        "10:05 AM" to "Program",
        "11:02 AM" to "Program",
        "11:58 AM" to "Program",
        "12:51 PM" to "Program",
        "01:51 PM" to "Program",
        "02:50 PM" to "Program",
        "03:57 PM" to "Program"
    )

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel.logoUrl, size = 72)
            Column(Modifier.padding(start = 18.dp)) {
                Text(
                    text = "$channelIndex  ${channel.name}",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.group.ifBlank { "All channels" },
                    color = Color(0xFFB9BBC0),
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(34.dp))

        times.forEach { (time, title) ->
            val active = time == "11:58 AM"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    time,
                    color = if (active) Accent else Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.width(150.dp)
                )
                Text(
                    title,
                    color = if (active) Accent else Color.White,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramInfoCard(
    channel: Channel,
    channelIndex: Int,
    videoInfo: VideoInfo,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xB821252C))
            .padding(horizontal = 32.dp, vertical = 26.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "$channelIndex  ${channel.name}",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Program", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text("11:58 - 12:51 PM", color = Color(0xFFC4C6CA), fontSize = 19.sp)
                ProgressLine(width = 74.dp, progress = 0.36f)
                Text("26 min", color = Color(0xFFC4C6CA), fontSize = 19.sp)
                if (videoInfo.label.isNotEmpty()) TechBadge(videoInfo.label)
            }
            Text("Program description", color = Color(0xFFB7B9BD), fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProgressLine(width: androidx.compose.ui.unit.Dp, progress: Float) {
    Box(
        modifier = Modifier
            .width(width)
            .height(5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x66FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color(0xFFDADDE1))
        )
    }
}

@Composable
private fun MobileTapLayer(
    onTap: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val swipePx = 60.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var currentX = startX
                    var moved = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        if ((change.position.x - startX).absoluteValue > 12f) moved = true
                        currentX = change.position.x
                    }

                    val delta = currentX - startX
                    when {
                        moved && delta < -swipePx -> onNext()
                        moved && delta > swipePx -> onPrevious()
                        !moved -> onTap()
                    }
                }
            }
    )
}

private fun buildGuideCategories(
    playlists: List<PlaylistProfile>,
    activePlaylistId: String?,
    groups: List<az.iptv.fplayer.data.model.ChannelGroup>,
    selectedGroup: String?,
    allChannelsLabel: String
): List<GuideCategoryItem> {
    val realGroups = groups.filterNot { it.name == PlayerViewModel.FAVORITE_GROUP_NAME }
    val playlistItems = if (playlists.size > 1) {
        playlists.map { profile ->
            GuideCategoryItem(
                type = GuideCategoryType.PLAYLIST,
                label = profile.name,
                playlist = profile,
                active = profile.id == activePlaylistId
            )
        }
    } else {
        emptyList()
    }
    val allItem = GuideCategoryItem(
        type = GuideCategoryType.ALL,
        label = allChannelsLabel,
        count = realGroups.sumOf { it.channels.size },
        selected = selectedGroup == null
    )
    val groupItems = groups.map { group ->
        GuideCategoryItem(
            type = GuideCategoryType.GROUP,
            label = group.name,
            count = group.channels.size,
            groupName = group.name,
            selected = selectedGroup == group.name
        )
    }
    return playlistItems + allItem + groupItems
}

private fun selectedGuideCategoryIndex(categories: List<GuideCategoryItem>): Int =
    categories.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0

private fun selectedPlaylistCategoryIndex(
    categories: List<GuideCategoryItem>,
    activePlaylistId: String?
): Int =
    categories.indexOfFirst {
        it.type == GuideCategoryType.PLAYLIST && it.playlist?.id == activePlaylistId
    }.takeIf { it >= 0 } ?: selectedGuideCategoryIndex(categories)

private fun selectedContentTypeIndex(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType
): Int = contentTypes.indexOf(selectedContentType).takeIf { it >= 0 } ?: 0

private fun hasSelectableMediaTracks(tracks: MediaTracks): Boolean =
    tracks.audioTracks.size > 1 || tracks.subtitleTracks.isNotEmpty()

private fun selectedTrackLabel(options: List<MediaTrackOption>): String =
    options.firstOrNull { it.selected }?.label ?: options.firstOrNull()?.label ?: "--"

private fun selectedSubtitleLabel(tracks: MediaTracks, offLabel: String): String =
    if (!tracks.subtitlesEnabled) offLabel
    else selectedTrackLabel(tracks.subtitleTracks)

private fun changeSelectedMediaTrack(
    mediaTracks: MediaTracks,
    focusedMediaOption: Int,
    delta: Int,
    onAudioTrack: (String) -> Unit,
    onSubtitleTrack: (String?) -> Unit
) {
    if (focusedMediaOption == 0 && mediaTracks.audioTracks.isNotEmpty()) {
        val next = nextTrack(mediaTracks.audioTracks, delta) ?: return
        onAudioTrack(next.id)
        return
    }

    if (mediaTracks.subtitleTracks.isEmpty()) return
    val subtitleOptions = listOf<MediaTrackOption?>(null) + mediaTracks.subtitleTracks
    val selectedIndex = if (!mediaTracks.subtitlesEnabled) {
        0
    } else {
        subtitleOptions.indexOfFirst { it?.selected == true }.takeIf { it >= 0 } ?: 0
    }
    val nextIndex = (selectedIndex + delta + subtitleOptions.size) % subtitleOptions.size
    onSubtitleTrack(subtitleOptions[nextIndex]?.id)
}

private fun nextTrack(options: List<MediaTrackOption>, delta: Int): MediaTrackOption? {
    if (options.isEmpty()) return null
    val selectedIndex = options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
    return options[(selectedIndex + delta + options.size) % options.size]
}

private fun contentTypeLabel(type: ChannelContentType): String =
    when (type) {
        ChannelContentType.TV -> "TV"
        ChannelContentType.MOVIE -> "Kino"
        ChannelContentType.SERIES -> "Series"
    }
