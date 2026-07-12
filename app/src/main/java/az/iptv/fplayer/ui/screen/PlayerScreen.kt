package az.iptv.fplayer.ui.screen

import android.app.Activity
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.aspectRatio
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
import az.iptv.fplayer.data.preferences.PlaylistProfile
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.ExoPlayerEngine
import az.iptv.fplayer.player.MediaTrackOption
import az.iptv.fplayer.player.MediaTracks
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.PlayerEngine
import az.iptv.fplayer.player.PlayerEventListener
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.ui.component.ChannelInfoOsd
import az.iptv.fplayer.ui.component.ChannelLogo
import az.iptv.fplayer.ui.component.ChannelPoster
import az.iptv.fplayer.ui.component.RatingBadge
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
    val selectedGroups by vm.selectedGroups.collectAsState()
    val selectedContentType by vm.selectedContentType.collectAsState()
    val availableContentTypes by vm.availableContentTypes.collectAsState()
    val loadState by vm.loadState.collectAsState()
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val activePlaylist by vm.activePlaylist.collectAsState()
    val recentChannels by vm.recentChannels.collectAsState()
    val language by vm.appLanguage.collectAsState()
    val t = appTexts(language)
    fun isAdultLocked(channel: Channel): Boolean = false
    val guideContentTypes = availableContentTypes.ifEmpty { listOf(selectedContentType) }
    val guideRailItemCount = guideContentTypes.size + playlists.size + 2
    val quickRefreshLabel = remember(t.loadRefresh) {
        t.loadRefresh.substringAfter("/", t.loadRefresh).trim().ifBlank { t.loadRefresh }
    }
    val guideCategories = remember(playlists, activePlaylist?.id, groups, selectedGroup, selectedGroups, t.allChannels) {
        buildGuideCategories(
            playlists = playlists,
            activePlaylistId = activePlaylist?.id,
            groups = groups,
            selectedGroup = selectedGroup,
            selectedGroups = selectedGroups,
            allChannelsLabel = t.allChannels
        )
    }
    var mediaTracks by remember { mutableStateOf(MediaTracks()) }
    var channelOffKey by remember { mutableStateOf<String?>(null) }

    val engine: PlayerEngine = remember(audioDecoderMode, context) {
        ExoPlayerEngine(context, audioDecoderMode)
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
    val playRequestNonce by vm.playRequestNonce.collectAsState()
    var recoveryAttemptKey by remember { mutableStateOf<String?>(null) }
    var recoveryAttemptCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(engine, playbackUrl, playRequestNonce) {
        playbackUrl?.let {
            channelOffKey = null
            recoveryAttemptKey = null
            recoveryAttemptCount = 0
            vm.onVideoInfoChanged(VideoInfo())
            engine.play(it)
        }
    }

    LaunchedEffect(playbackState, currentChannel?.stableKey, playbackUrl) {
        val channel = currentChannel ?: return@LaunchedEffect
        val key = channel.stableKey
        if (playbackState is PlaybackState.Playing) {
            recoveryAttemptKey = null
            recoveryAttemptCount = 0
            channelOffKey = null
            return@LaunchedEffect
        }
        // OFF elan olunmuş kanal üçün nə təkrar cəhd, nə də siyahı yeniləməsi olmur
        if (channelOffKey == key) return@LaunchedEffect
        val waitMs = when (playbackState) {
            is PlaybackState.Buffering -> 18_000L
            is PlaybackState.Error -> 1_400L
            is PlaybackState.Idle -> 2_200L
            else -> return@LaunchedEffect
        }
        if (recoveryAttemptKey != key) {
            recoveryAttemptKey = key
            recoveryAttemptCount = 0
        }
        delay(waitMs)
        if (recoveryAttemptCount < 2 && playbackUrl != null) {
            recoveryAttemptCount += 1
            vm.onVideoInfoChanged(VideoInfo())
            engine.play(playbackUrl)
        } else {
            channelOffKey = key
            engine.stop()
        }
    }

    val currentChannelKey = currentChannel?.stableKey
    val currentChannelGroupChannels = remember(groups, currentChannel?.group, currentChannelKey) {
        val groupName = currentChannel?.group?.takeIf { it.isNotBlank() }
        groups.find { it.name == groupName }?.channels.orEmpty()
    }
    val osdChannels = currentChannelGroupChannels
        .takeIf { channels -> currentChannelKey != null && channels.any { it.stableKey == currentChannelKey } }
        ?: visibleChannels
    val currentVisibleChannelIndex = remember(osdChannels, currentChannelKey) {
        osdChannels.indexOfFirst { it.stableKey == currentChannelKey }
    }
    val channelIndex = currentVisibleChannelIndex + 1
    val preferredGuideIndex = remember(guideCategories, selectedGroup, selectedGroups, currentChannel?.group) {
        selectedGuideCategoryIndex(
            categories = guideCategories,
            selectedGroup = selectedGroup,
            selectedGroups = selectedGroups,
            currentChannel = currentChannel
        )
    }

    var focusedChannelIndex by remember { mutableIntStateOf(0) }
    var focusedGroupIndex by remember { mutableIntStateOf(0) }
    var focusedContentTypeIndex by remember { mutableIntStateOf(0) }
    var focusedMediaOption by remember { mutableIntStateOf(0) }
    var selectorPane by remember { mutableStateOf(SelectorPane.CHANNELS) }
    var mediaOptionsVisible by remember { mutableStateOf(false) }
    var audioTrackPanelVisible by remember { mutableStateOf(false) }
    var recentOverlayVisible by remember { mutableStateOf(false) }
    var channelOnlyGuideVisible by remember { mutableStateOf(false) }
    val guideChannels = visibleChannels
    val isVodGuide = selectedContentType != ChannelContentType.TV
    val guideColumns = if (isVodGuide) 3 else 1
    val currentGuideChannelIndex = remember(guideChannels, currentChannelKey) {
        guideChannels.indexOfFirst { it.stableKey == currentChannelKey }
    }
    var focusedRecentIndex by remember { mutableIntStateOf(0) }
    var focusedAudioTrackIndex by remember { mutableIntStateOf(0) }
    var exitPromptVisible by remember { mutableStateOf(false) }
    var exitHintVisible by remember { mutableStateOf(false) }
    var pendingExitOnSelectRelease by remember { mutableStateOf(false) }
    var lastExitBackAt by remember { mutableStateOf(0L) }
    var leftPressCount by remember { mutableIntStateOf(0) }
    var lastLeftPressAt by remember { mutableStateOf(0L) }

    fun closeApp() {
        engine.stop()
        (context as? Activity)?.let { activity ->
            activity.finishAndRemoveTask()
            activity.finishAffinity()
        }
    }

    fun openPlaylistMenu() {
        leftPressCount = 0
        channelOnlyGuideVisible = false
        recentOverlayVisible = false
        mediaOptionsVisible = false
        vm.hideSidebar()
        onNavigateToPlaylist()
    }

    fun selectOrRequestAdultPin(channel: Channel) {
        vm.selectChannel(channel)
    }

    fun focusCurrentChannelCategory() {
        val groupName = currentChannel?.group?.takeIf { name ->
            name.isNotBlank() && groups.any { it.name == name }
        }
        vm.selectGroup(groupName)
        focusedGroupIndex = groupName
            ?.let { name -> guideCategories.indexOfFirst { it.groupName == name }.takeIf { it >= 0 } }
            ?: guideCategories.indexOfFirst { it.type == GuideCategoryType.ALL }.takeIf { it >= 0 }
            ?: 0
    }

    fun selectFocusedRailItem() {
        guideContentTypes.getOrNull(focusedContentTypeIndex)?.let {
            vm.selectContentType(it)
            focusedGroupIndex = preferredGuideIndex
            focusedChannelIndex = 0
            selectorPane = SelectorPane.GROUPS
            return
        }
        playlists.getOrNull(focusedContentTypeIndex - guideContentTypes.size)?.let {
            vm.switchPlaylist(it)
            focusedGroupIndex = 0
            focusedChannelIndex = 0
            selectorPane = SelectorPane.GROUPS
            return
        }
        val menuIndex = guideContentTypes.size + playlists.size
        if (focusedContentTypeIndex == menuIndex) {
            openPlaylistMenu()
            return
        }
        vm.refreshPlaylist(revealSidebar = true)
    }

    fun activateGuideCategory(item: GuideCategoryItem) {
        when (item.type) {
            GuideCategoryType.PLAYLIST -> item.playlist?.let(vm::switchPlaylist)
            GuideCategoryType.ALL -> vm.selectGroup(null)
            GuideCategoryType.GROUP -> item.groupName?.let(vm::selectGroup)
        }
        selectorPane = SelectorPane.CHANNELS
        focusedChannelIndex = 0
    }

    LaunchedEffect(selectorVisible) {
        if (selectorVisible) {
            focusedChannelIndex = currentGuideChannelIndex.coerceAtLeast(0)
            focusedGroupIndex = preferredGuideIndex
            focusedContentTypeIndex = selectedContentTypeIndex(guideContentTypes, selectedContentType)
        }
    }

    LaunchedEffect(guideChannels, currentChannelKey) {
        focusedChannelIndex = currentGuideChannelIndex.coerceAtLeast(0)
    }

    LaunchedEffect(guideContentTypes, selectedContentType, guideRailItemCount) {
        focusedContentTypeIndex = focusedContentTypeIndex.coerceIn(
            0,
            (guideRailItemCount - 1).coerceAtLeast(0)
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
                (recentChannels.take(10).size - 1).coerceAtLeast(0)
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
    LaunchedEffect(channelOnlyGuideVisible, selectorVisible, focusedChannelIndex, guideChannels.size) {
        if (channelOnlyGuideVisible && selectorVisible) {
            delay(10_000)
            channelOnlyGuideVisible = false
            vm.hideSidebar()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val isSelectKey = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.DirectionCenter
                if (exitPromptVisible && pendingExitOnSelectRelease && event.type == KeyEventType.KeyUp && isSelectKey) {
                    pendingExitOnSelectRelease = false
                    closeApp()
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val isExitKey = event.key == Key.Back
                if (audioTrackPanelVisible) {
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
                            pendingExitOnSelectRelease = true
                            true
                        }
                        isExitKey -> {
                            exitPromptVisible = false
                            pendingExitOnSelectRelease = false
                            true
                        }
                        else -> true
                    }
                } else if (recentOverlayVisible) {
                    val recentList = recentChannels.take(10)
                    when (event.key) {
                        Key.Back -> {
                            recentOverlayVisible = false
                            true
                        }
                        Key.DirectionUp -> {
                            focusedRecentIndex = wrapIndex(focusedRecentIndex, -1, recentList.size)
                            true
                        }
                        Key.DirectionDown -> {
                            focusedRecentIndex = wrapIndex(focusedRecentIndex, 1, recentList.size)
                            true
                        }
                        Key.DirectionLeft -> {
                            recentOverlayVisible = false
                            channelOnlyGuideVisible = false
                            focusedGroupIndex = preferredGuideIndex
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
                                    focusedGroupIndex = preferredGuideIndex
                                    selectorPane = SelectorPane.GROUPS
                                } else {
                                    when (selectorPane) {
                                        SelectorPane.CHANNELS -> {
                                            if (guideColumns > 1 && focusedChannelIndex % guideColumns != 0) {
                                                focusedChannelIndex -= 1
                                            } else {
                                                focusedGroupIndex = preferredGuideIndex
                                                selectorPane = SelectorPane.GROUPS
                                            }
                                        }
                                        SelectorPane.GROUPS -> {
                                            selectorPane = SelectorPane.CONTENT_TYPES
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
                                focusCurrentChannelCategory()
                                selectorPane = SelectorPane.GROUPS
                                vm.showSidebar()
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            if (selectorVisible) {
                                when (selectorPane) {
                                    SelectorPane.CONTENT_TYPES -> {
                                        if (focusedContentTypeIndex < guideContentTypes.size) {
                                            selectorPane = SelectorPane.GROUPS
                                        } else {
                                            selectFocusedRailItem()
                                        }
                                    }
                                    SelectorPane.GROUPS -> selectorPane = SelectorPane.CHANNELS
                                    SelectorPane.CHANNELS -> if (
                                        guideColumns > 1 &&
                                        focusedChannelIndex % guideColumns != guideColumns - 1 &&
                                        focusedChannelIndex + 1 < guideChannels.size
                                    ) {
                                        focusedChannelIndex += 1
                                    } else {
                                        if (hasSelectableMediaTracks(mediaTracks)) {
                                            focusedMediaOption = if (mediaTracks.audioTracks.isNotEmpty()) 0 else 1
                                            mediaOptionsVisible = true
                                            audioTrackPanelVisible = false
                                            vm.hideSidebar()
                                            vm.hideOsd()
                                        } else {
                                            guideChannels.getOrNull(focusedChannelIndex)?.let(vm::toggleFavorite)
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
                                val recentList = recentChannels.take(10)
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
                                        focusedContentTypeIndex = wrapIndex(focusedContentTypeIndex, -1, guideRailItemCount)
                                    }
                                    SelectorPane.GROUPS -> {
                                        focusedGroupIndex = wrapIndex(focusedGroupIndex, -1, guideCategories.size)
                                    }
                                    SelectorPane.CHANNELS -> {
                                        focusedChannelIndex = if (guideColumns > 1) {
                                            (focusedChannelIndex - guideColumns)
                                                .takeIf { it >= 0 } ?: focusedChannelIndex
                                        } else {
                                            wrapIndex(focusedChannelIndex, -1, guideChannels.size)
                                        }
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
                                        focusedContentTypeIndex = wrapIndex(focusedContentTypeIndex, 1, guideRailItemCount)
                                    }
                                    SelectorPane.GROUPS -> {
                                        focusedGroupIndex = wrapIndex(focusedGroupIndex, 1, guideCategories.size)
                                    }
                                    SelectorPane.CHANNELS -> {
                                        focusedChannelIndex = if (guideColumns > 1) {
                                            (focusedChannelIndex + guideColumns)
                                                .takeIf { it < guideChannels.size }
                                                ?: focusedChannelIndex
                                        } else {
                                            wrapIndex(focusedChannelIndex, 1, guideChannels.size)
                                        }
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
                                        selectFocusedRailItem()
                                    }
                                    SelectorPane.GROUPS -> {
                                        guideCategories.getOrNull(focusedGroupIndex)?.let(::activateGuideCategory)
                                    }
                                    SelectorPane.CHANNELS -> {
                                        guideChannels.getOrNull(focusedChannelIndex)?.let {
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
                                focusCurrentChannelCategory()
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
                                guideChannels.getOrNull(focusedChannelIndex)?.let(vm::toggleFavorite)
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
        val guideWidth = if (isWide) (maxWidth * 0.76f).coerceIn(700.dp, 1000.dp) else maxWidth * 0.98f
        val startupLoadingVisible = !selectorVisible &&
            currentChannel == null &&
            loadState !is LoadState.Error
        VideoSurface(engine = engine)

        StartupLoadingOverlay(
            visible = startupLoadingVisible || loadState is LoadState.Loading,
            title = t.loadingChannels,
            subtitle = activePlaylist?.name ?: t.playlistSource,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(3f)
        )

        AnimatedVisibility(
            visible = selectorVisible,
            enter = fadeIn(tween(220)) + slideInHorizontally(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                initialOffsetX = { -it / 12 }
            ),
            exit = fadeOut(tween(160)) + slideOutHorizontally(
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                targetOffsetX = { -it / 12 }
            ),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            ReceiverGuideOverlay(
                contentTypes = guideContentTypes,
                selectedContentType = selectedContentType,
                playlists = playlists,
                activePlaylistId = activePlaylist?.id,
                focusedContentTypeIndex = focusedContentTypeIndex,
                categories = guideCategories,
                selectedGroup = selectedGroup,
                selectedGroups = selectedGroups,
                focusedGroupIndex = focusedGroupIndex,
                channels = guideChannels,
                columns = guideColumns,
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
                refreshLabel = quickRefreshLabel,
                favoriteLabel = t.favorite,
                lockedLabel = t.locked,
                isAdultLocked = ::isAdultLocked,
                onContentTypeClick = {
                    vm.selectContentType(it)
                    focusedGroupIndex = preferredGuideIndex
                    focusedChannelIndex = 0
                    selectorPane = SelectorPane.GROUPS
                },
                onPlaylistClick = {
                    vm.switchPlaylist(it)
                    focusedGroupIndex = 0
                    focusedChannelIndex = 0
                    selectorPane = SelectorPane.GROUPS
                },
                onMenuClick = { openPlaylistMenu() },
                onRefreshClick = { vm.refreshPlaylist(revealSidebar = true) },
                onCategoryClick = ::activateGuideCategory,
                onChannelClick = ::selectOrRequestAdultPin
            )
        }

        ChannelOffOverlay(
            visible = channelOffKey != null && channelOffKey == currentChannelKey && !selectorVisible,
            channelName = currentChannel?.name.orEmpty(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(4f)
        )

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
            totalChannels = osdChannels.size,
            allChannelsLabel = t.allChannels,
            programLabel = t.program,
            audioLabel = t.audio,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(5f)
        )

        RecentChannelsOverlay(
            visible = recentOverlayVisible && !selectorVisible,
            channels = recentChannels.take(10),
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
private fun StartupLoadingOverlay(
    visible: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180)), modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF20A0E14), Color(0xF6090C11))
                    )
                )
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 38.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Accent)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = Color(0xFFD5E8EF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelOffOverlay(
    visible: Boolean,
    channelName: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(160)), modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xEB0D1116))
                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 46.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF5D5D))
            )
            Text(
                text = "CHANNEL OFF",
                color = Color(0xFFFF6B6B),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            if (channelName.isNotBlank()) {
                Text(
                    text = channelName,
                    color = Color(0xFFCBD4DB),
                    fontSize = 15.sp,
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
                .widthIn(min = 430.dp, max = 540.dp)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF20A0E14), Color(0xF6090C11))
                    )
                )
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
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
    playlists: List<PlaylistProfile>,
    activePlaylistId: String?,
    focusedContentTypeIndex: Int,
    categories: List<GuideCategoryItem>,
    selectedGroup: String?,
    selectedGroups: Set<String>,
    focusedGroupIndex: Int,
    channels: List<Channel>,
    columns: Int = 1,
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
    refreshLabel: String,
    favoriteLabel: String,
    lockedLabel: String,
    isAdultLocked: (Channel) -> Boolean,
    onContentTypeClick: (ChannelContentType) -> Unit,
    onPlaylistClick: (PlaylistProfile) -> Unit,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCategoryClick: (GuideCategoryItem) -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    val panelWidth = if (channelsOnly) (guideWidth * 0.55f).coerceIn(420.dp, 560.dp) else guideWidth
    val railWidth = if (guideWidth >= 900.dp) 118.dp else 104.dp
    val groupWidth = if (guideWidth >= 1000.dp) 290.dp else 244.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (channelsOnly) Color(0x33000000) else Color(0x73000000)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .background(Color(0xF510141B))
        ) {
            if (!channelsOnly) {
                ContentTypeColumn(
                    contentTypes = contentTypes,
                    selectedContentType = selectedContentType,
                    playlists = playlists,
                    activePlaylistId = activePlaylistId,
                    focusedIndex = focusedContentTypeIndex,
                    focused = focusedPane == SelectorPane.CONTENT_TYPES,
                    onContentTypeClick = onContentTypeClick,
                    onPlaylistClick = onPlaylistClick,
                    menuLabel = "Menu",
                    onMenuClick = onMenuClick,
                    refreshLabel = refreshLabel,
                    onRefreshClick = onRefreshClick,
                    modifier = Modifier
                        .width(railWidth)
                        .fillMaxHeight()
                        .background(Color(0xF50B0E14))
                        .padding(horizontal = 8.dp)
                )

                ReceiverCategoryColumn(
                    categories = categories,
                    selectedGroup = selectedGroup,
                    selectedGroups = selectedGroups,
                    focusedIndex = focusedGroupIndex,
                    focused = focusedPane == SelectorPane.GROUPS,
                    categoriesLabel = categoriesLabel,
                    allChannelsLabel = allChannelsLabel,
                    activeLabel = activeLabel,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier
                        .width(groupWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                )

                ReceiverGuideDivider()
            }

            ReceiverChannelColumn(
                channels = channels,
                columns = columns,
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
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContentTypeColumn(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType,
    playlists: List<PlaylistProfile>,
    activePlaylistId: String?,
    focusedIndex: Int,
    focused: Boolean,
    onContentTypeClick: (ChannelContentType) -> Unit,
    onPlaylistClick: (PlaylistProfile) -> Unit,
    menuLabel: String,
    onMenuClick: () -> Unit,
    refreshLabel: String,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val menuIndex = contentTypes.size + playlists.size
    val refreshIndex = menuIndex + 1

    LaunchedEffect(focusedIndex, contentTypes.size, playlists.size) {
        if (focusedIndex in 0..refreshIndex) {
            listState.scrollToItem(maxOf(0, focusedIndex - 4))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(top = 18.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(contentTypes, key = { _, type -> "type-${type.name}" }) { index, type ->
            RailItem(
                label = contentTypeShortLabel(type),
                selected = type == selectedContentType,
                activeFocus = focused && index == focusedIndex,
                onClick = { onContentTypeClick(type) }
            )
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlist_gap") { Spacer(Modifier.height(10.dp)) }
        }
        itemsIndexed(playlists, key = { index, profile -> "playlist-${profile.id}-$index" }) { index, profile ->
            RailItem(
                label = providerShortLabel(profile, index),
                selected = profile.id == activePlaylistId,
                activeFocus = focused && focusedIndex == contentTypes.size + index,
                small = true,
                onClick = { onPlaylistClick(profile) }
            )
        }
        item(key = "rail_gap") { Spacer(Modifier.height(10.dp)) }
        item(key = "menu") {
            RailItem(
                label = menuLabel,
                selected = false,
                activeFocus = focused && focusedIndex == menuIndex,
                small = true,
                onClick = onMenuClick
            )
        }
        item(key = "refresh") {
            RailItem(
                label = refreshLabel,
                selected = false,
                activeFocus = focused && focusedIndex == refreshIndex,
                small = true,
                onClick = onRefreshClick
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailItem(
    label: String,
    selected: Boolean,
    activeFocus: Boolean,
    small: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (small) 34.dp else 38.dp)
            .clip(shape)
            .background(
                when {
                    activeFocus -> Accent
                    selected -> Color(0x1FFFFFFF)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                activeFocus -> Color(0xFF14161A)
                selected -> Accent
                else -> Color(0xFFAFB8C2)
            },
            fontSize = if (small) 12.sp else 14.sp,
            fontWeight = if (activeFocus || selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReceiverGuideDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color(0x1AFFFFFF))
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverCategoryColumn(
    categories: List<GuideCategoryItem>,
    selectedGroup: String?,
    selectedGroups: Set<String>,
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
    val firstChannelCategoryIndex = categories.indexOfFirst { it.type != GuideCategoryType.PLAYLIST }
        .takeIf { it >= 0 }
        ?: categories.size

    LaunchedEffect(focusedIndex, categories.size) {
        if (focusedIndex in categories.indices) {
            val headerOffset = when {
                focusedIndex < firstChannelCategoryIndex -> 1
                firstChannelCategoryIndex > 0 -> 2
                else -> 1
            }
            listState.scrollToItem(maxOf(0, focusedIndex + headerOffset - 4))
        }
    }

    Column(modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (firstChannelCategoryIndex > 0) {
                item(key = "provider_header") {
                    ReceiverCategorySectionHeader("IPTV providers")
                }
            }
            itemsIndexed(categories) { index, item ->
                if (index == firstChannelCategoryIndex) {
                    ReceiverCategorySectionHeader(categoriesLabel)
                }
                val activeFocus = focused && index == focusedIndex
                val selected = item.selected ||
                    (item.groupName != null && item.groupName in selectedGroups) ||
                    index == selectedIndex
                val rowShape = RoundedCornerShape(6.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .height(42.dp)
                        .clip(rowShape)
                        .background(
                            when {
                                activeFocus -> Accent
                                selected -> Color(0x1FFFFFFF)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onCategoryClick(item) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        color = when {
                            activeFocus -> Color(0xFF14161A)
                            selected -> Accent
                            else -> Color(0xFFD7DEE5)
                        },
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (activeFocus || selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val meta = if (item.active && item.type == GuideCategoryType.PLAYLIST) activeLabel
                    else item.count?.toString().orEmpty()
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = meta,
                            color = if (activeFocus) Color(0xB314161A) else Color(0xFF7C8894),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverCategorySectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF6E7A87),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 6.dp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverChannelColumn(
    channels: List<Channel>,
    columns: Int = 1,
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
    val gridState = rememberLazyGridState()

    LaunchedEffect(focusedIndex, channels.size, columns) {
        if (focusedIndex in channels.indices) {
            if (columns > 1) {
                gridState.scrollToItem(maxOf(0, focusedIndex - columns * 2))
            } else {
                listState.scrollToItem(maxOf(0, focusedIndex - 5))
            }
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
            columns > 1 -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItemsIndexed(channels, key = { index, channel -> "${channel.stableKey}#$index" }) { index, channel ->
                        ReceiverChannelCard(
                            channel = channel,
                            isPlaying = currentChannel?.stableKey == channel.stableKey,
                            isFocused = focused && index == focusedIndex,
                            groupFallbackLabel = emptyLabel,
                            lockedLabel = lockedLabel,
                            isAdultLocked = isAdultLocked(channel),
                            onClick = { onChannelClick(channel) }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp, bottom = 4.dp),
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
    val shape = RoundedCornerShape(6.dp)
    val accent = Accent
    val isVod = channel.contentType != ChannelContentType.TV
    val nameColor = when {
        isFocused -> Color(0xFF14161A)
        isPlaying -> accent
        else -> Color.White
    }
    val numberColor = if (isFocused) Color(0xB314161A) else Color(0xFF7C8894)
    val groupColor = if (isFocused) Color(0x9914161A) else Color(0xFF7C8894)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .height(if (isVod) 68.dp else 48.dp)
            .clip(shape)
            .background(
                when {
                    isFocused -> accent
                    isPlaying -> Color(0x1FFFFFFF)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            color = numberColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(42.dp)
        )
        if (isVod) {
            ChannelPoster(
                posterUrl = channel.logoUrl,
                name = channel.name,
                modifier = Modifier
                    .width(42.dp)
                    .height(60.dp)
            )
        } else {
            ChannelLogo(channel.logoUrl, size = 32)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = nameColor,
                fontSize = 16.sp,
                fontWeight = if (isFocused || isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = if (isVod) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isVod && channel.rating > 0f) {
                    RatingBadge(rating = channel.rating, compact = true, onDark = !isFocused)
                }
                Text(
                    text = channel.group.ifBlank { groupFallbackLabel },
                    color = groupColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isAdultLocked) {
            Text(
                text = lockedLabel,
                color = if (isFocused) Color(0xB314161A) else Color(0xFF7C8894),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
        }
        if (channel.isFavorite) {
            Text(
                text = "★",
                color = if (isFocused) Color(0xFF14161A) else accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(6.dp))
        }
        if (isPlaying) {
            Text(
                text = "▶",
                color = if (isFocused) Color(0xFF14161A) else accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReceiverChannelCard(
    channel: Channel,
    isPlaying: Boolean,
    isFocused: Boolean,
    groupFallbackLabel: String,
    lockedLabel: String,
    isAdultLocked: Boolean,
    onClick: () -> Unit
) {
    val accent = Accent
    val cardShape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isFocused) Color(0x1FFFFFFF) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) accent else Color(0x14FFFFFF),
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            ChannelPoster(
                posterUrl = channel.logoUrl,
                name = channel.name,
                modifier = Modifier.fillMaxSize()
            )
            if (channel.rating > 0f) {
                RatingBadge(
                    rating = channel.rating,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color(0xFF14161A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            if (channel.isFavorite) {
                Text(
                    text = "★",
                    color = accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            if (isAdultLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lockedLabel,
                        color = Color(0xFFE6ECF1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = channel.name,
            color = if (isFocused) Color.White else Color(0xFFD7DEE5),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (isFocused || isPlaying) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = channel.group.ifBlank { groupFallbackLabel },
            color = if (isPlaying) accent else Color(0xFF7C8894),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                                    selected -> Color(0x33FFC247)
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
    selectedGroups: Set<String>,
    allChannelsLabel: String
): List<GuideCategoryItem> {
    val realGroups = groups.filterNot { it.name == PlayerViewModel.FAVORITE_GROUP_NAME }
    val allItem = GuideCategoryItem(
        type = GuideCategoryType.ALL,
        label = allChannelsLabel,
        count = realGroups.sumOf { it.channels.size },
        selected = selectedGroup == null && selectedGroups.isEmpty()
    )
    val groupItems = groups.map { group ->
        GuideCategoryItem(
            type = GuideCategoryType.GROUP,
            label = group.name,
            count = group.channels.size,
            groupName = group.name,
            selected = group.name in selectedGroups || selectedGroup == group.name
        )
    }
    return listOf(allItem) + groupItems
}

private fun selectedGuideCategoryIndex(
    categories: List<GuideCategoryItem>,
    selectedGroup: String?,
    selectedGroups: Set<String>,
    currentChannel: Channel?
): Int =
    selectedGroups.firstOrNull()?.let { group ->
            categories.indexOfFirst { it.groupName == group }.takeIf { it >= 0 }
        }
        ?: selectedGroup?.let { group ->
            categories.indexOfFirst { it.groupName == group }.takeIf { it >= 0 }
        }
        ?: currentChannel?.group?.takeIf { it.isNotBlank() }?.let { group ->
            categories.indexOfFirst { it.groupName == group }.takeIf { it >= 0 }
        }
        ?: categories.indexOfFirst { it.selected }.takeIf { it >= 0 }
        ?: categories.indexOfFirst { it.type == GuideCategoryType.ALL }.takeIf { it >= 0 }
        ?: 0

private fun selectedContentTypeIndex(
    contentTypes: List<ChannelContentType>,
    selectedContentType: ChannelContentType
): Int = contentTypes.indexOf(selectedContentType).takeIf { it >= 0 } ?: 0

private fun wrapIndex(current: Int, delta: Int, size: Int): Int {
    if (size <= 0) return 0
    return (current + delta + size) % size
}

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

private fun contentTypeShortLabel(type: ChannelContentType): String =
    when (type) {
        ChannelContentType.TV -> "TV"
        ChannelContentType.MOVIE -> "KINO"
        ChannelContentType.SERIES -> "SER"
    }

private fun providerShortLabel(profile: PlaylistProfile, index: Int): String {
    val cleaned = profile.name
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "IPTV ${index + 1}" }
    return cleaned.take(12).uppercase()
}
