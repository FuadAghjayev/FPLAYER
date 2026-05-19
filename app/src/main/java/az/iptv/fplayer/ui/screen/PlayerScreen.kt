package az.iptv.fplayer.ui.screen

import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.key.onKeyEvent
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
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.ExoPlayerEngine
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.PlayerEngine
import az.iptv.fplayer.player.PlayerEventListener
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.player.VlcPlayerEngine
import az.iptv.fplayer.ui.component.ChannelInfoOsd
import az.iptv.fplayer.ui.component.ChannelList
import az.iptv.fplayer.ui.component.ChannelLogo
import az.iptv.fplayer.ui.component.TechBadge
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlin.math.absoluteValue

private enum class SelectorPane { CHANNELS, GROUPS }

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
    val selectorVisible by vm.sidebarVisible.collectAsState()
    val osdVisible by vm.osdVisible.collectAsState()
    val visibleChannels by vm.visibleChannels.collectAsState()
    val selectedGroup by vm.selectedGroup.collectAsState()
    val loadState by vm.loadState.collectAsState()
    val playerType by vm.playerType.collectAsState()
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()

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
        })
        onDispose { engine.release() }
    }

    LaunchedEffect(Unit) {
        if (groups.isEmpty()) vm.autoLoadSavedPlaylist()
    }

    LaunchedEffect(currentChannel) {
        currentChannel?.let {
            vm.onVideoInfoChanged(VideoInfo())
            engine.play(it.url)
        }
    }

    val currentChannelKey = currentChannel?.stableKey
    val currentChannelIndex = remember(visibleChannels, currentChannelKey) {
        visibleChannels.indexOfFirst { it.stableKey == currentChannelKey }
    }
    val channelIndex = currentChannelIndex + 1

    var focusedChannelIndex by remember { mutableIntStateOf(0) }
    var focusedGroupIndex by remember { mutableIntStateOf(0) }
    var selectorPane by remember { mutableStateOf(SelectorPane.CHANNELS) }

    LaunchedEffect(selectorVisible) {
        if (selectorVisible) {
            focusedChannelIndex = currentChannelIndex.coerceAtLeast(0)
            selectorPane = SelectorPane.CHANNELS
        }
    }

    LaunchedEffect(visibleChannels, currentChannelKey) {
        focusedChannelIndex = currentChannelIndex.coerceAtLeast(0)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (selectorVisible) {
                            selectorPane = SelectorPane.GROUPS
                            focusedGroupIndex = selectedGroupIndex(groups.map { it.name }, selectedGroup)
                        } else {
                            vm.showSidebar()
                            selectorPane = SelectorPane.CHANNELS
                        }
                        true
                    }

                    Key.DirectionRight -> {
                        if (selectorVisible && selectorPane == SelectorPane.GROUPS) {
                            selectorPane = SelectorPane.CHANNELS
                            true
                        } else false
                    }

                    Key.DirectionUp -> {
                        if (selectorVisible) {
                            if (selectorPane == SelectorPane.CHANNELS) {
                                focusedChannelIndex = (focusedChannelIndex - 1).coerceAtLeast(0)
                            } else {
                                focusedGroupIndex = (focusedGroupIndex - 1).coerceAtLeast(0)
                            }
                            true
                        } else {
                            vm.nextChannel()
                            true
                        }
                    }

                    Key.DirectionDown -> {
                        if (selectorVisible) {
                            if (selectorPane == SelectorPane.CHANNELS) {
                                focusedChannelIndex =
                                    (focusedChannelIndex + 1).coerceAtMost((visibleChannels.size - 1).coerceAtLeast(0))
                            } else {
                                focusedGroupIndex =
                                    (focusedGroupIndex + 1).coerceAtMost(groups.size)
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
                            if (selectorPane == SelectorPane.CHANNELS) {
                                visibleChannels.getOrNull(focusedChannelIndex)?.let(vm::selectChannel)
                            } else {
                                val groupName = if (focusedGroupIndex == 0) null
                                else groups.getOrNull(focusedGroupIndex - 1)?.name
                                vm.selectGroup(groupName)
                                selectorPane = SelectorPane.CHANNELS
                                focusedChannelIndex = 0
                            }
                            true
                        } else {
                            vm.showSidebar()
                            selectorPane = SelectorPane.CHANNELS
                            true
                        }
                    }

                    Key.Back -> {
                        when {
                            selectorVisible && selectorPane == SelectorPane.GROUPS -> {
                                selectorPane = SelectorPane.CHANNELS
                                true
                            }
                            selectorVisible -> {
                                vm.hideSidebar()
                                true
                            }
                            osdVisible -> {
                                vm.hideOsd()
                                true
                            }
                            else -> false
                        }
                    }

                    Key.Menu -> {
                        if (selectorVisible) vm.refreshPlaylist() else vm.showOsd()
                        true
                    }

                    Key.Info -> {
                        vm.showOsd()
                        true
                    }

                    else -> false
                }
            }
    ) {
        val isWide = maxWidth > 700.dp
        val guideWidth = if (isWide) (maxWidth * 0.31f).coerceIn(520.dp, 640.dp) else maxWidth

        VideoSurface(engine = engine)

        AnimatedVisibility(
            visible = selectorVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            if (selectorPane == SelectorPane.GROUPS) {
                CategoryOverlay(
                    groups = groups.map { it.name to it.channels.size },
                    selectedGroup = selectedGroup,
                    focusedIndex = focusedGroupIndex,
                    onGroupClick = {
                        vm.selectGroup(it)
                        selectorPane = SelectorPane.CHANNELS
                        focusedChannelIndex = 0
                    },
                    onPlaylistClick = onNavigateToPlaylist
                )
            } else {
                ChannelSelectorOverlay(
                    channels = visibleChannels,
                    currentChannel = currentChannel,
                    selectedGroup = selectedGroup,
                    focusedIndex = focusedChannelIndex,
                    channelIndex = channelIndex,
                    videoInfo = videoInfo,
                    guideWidth = guideWidth,
                    isLoading = loadState is LoadState.Loading,
                    onChannelClick = vm::selectChannel
                )
            }
        }

        AnimatedVisibility(
            visible = playbackState is PlaybackState.Buffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(3f)
        ) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(48.dp))
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
            channelIndex = channelIndex,
            totalChannels = visibleChannels.size,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(5f)
        )

        if (!isWide && !selectorVisible) {
            MobileTapLayer(
                onTap = {
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
private fun ChannelSelectorOverlay(
    channels: List<Channel>,
    currentChannel: Channel?,
    selectedGroup: String?,
    focusedIndex: Int,
    channelIndex: Int,
    videoInfo: VideoInfo,
    guideWidth: androidx.compose.ui.unit.Dp,
    isLoading: Boolean,
    onChannelClick: (Channel) -> Unit
) {
    val focusedChannel = channels.getOrNull(focusedIndex) ?: currentChannel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x5E000000))
    ) {
        Box(
            modifier = Modifier
                .width(guideWidth)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xD51D2228), Color(0xB20F1216), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .width(guideWidth)
                .fillMaxHeight()
                .background(Color(0x6C1A2026))
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp)
        ) {
            Text(
                text = selectedGroup ?: "All channels",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, bottom = 18.dp)
            )

            if (isLoading && channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                ChannelList(
                    channels = channels,
                    currentChannel = currentChannel,
                    currentFrameRate = videoInfo.frameRate,
                    focusedIndex = focusedIndex,
                    onChannelClick = onChannelClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (focusedChannel != null) {
            ProgramTimeline(
                channel = focusedChannel,
                channelIndex = (focusedIndex + 1).coerceAtLeast(1),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = guideWidth + 28.dp, top = 20.dp)
                    .widthIn(max = 470.dp)
            )

            ProgramInfoCard(
                channel = focusedChannel,
                channelIndex = (focusedIndex + 1).coerceAtLeast(1),
                videoInfo = videoInfo,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 48.dp)
                    .width(620.dp)
                    .heightIn(min = 138.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryOverlay(
    groups: List<Pair<String, Int>>,
    selectedGroup: String?,
    focusedIndex: Int,
    onGroupClick: (String?) -> Unit,
    onPlaylistClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC11151A))
            .padding(horizontal = 44.dp, vertical = 28.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Categories",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Select a category, then choose a channel",
                color = Color(0xFFB5B7BB),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            CategoryList(
                groups = groups,
                selectedGroup = selectedGroup,
                focusedIndex = focusedIndex,
                onGroupClick = onGroupClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Text(
            text = "Playlists",
            color = Color(0xFFD8DBDF),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0x26FFFFFF))
                .clickable(onClick = onPlaylistClick)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryList(
    groups: List<Pair<String, Int>>,
    selectedGroup: String?,
    focusedIndex: Int,
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
                    .height(76.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(bg)
                    .clickable { onGroupClick(name) }
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name ?: "All channels",
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$count",
                    color = if (focused) Color(0xFF33363A) else Color(0xFFB7BAC0),
                    fontSize = 18.sp,
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
                val fps = videoInfo.fpsLabel.ifBlank { channel.frameRate.takeIf { it > 0f }?.let { "${it.toInt()} FPS" } ?: "" }
                if (fps.isNotEmpty()) TechBadge(fps.uppercase())
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

private fun selectedGroupIndex(groupNames: List<String>, selectedGroup: String?): Int =
    if (selectedGroup == null) 0 else (groupNames.indexOf(selectedGroup) + 1).coerceAtLeast(0)
