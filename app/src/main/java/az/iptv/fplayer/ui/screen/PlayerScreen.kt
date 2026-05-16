package az.iptv.fplayer.ui.screen

import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
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
import az.iptv.fplayer.ui.component.GroupTabs
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.PanelBg
import az.iptv.fplayer.ui.theme.PanelBgSoft
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlin.math.absoluteValue

private enum class SidebarPane { CHANNELS, GROUPS }

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
    val sidebarVisible by vm.sidebarVisible.collectAsState()
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

    var sidebarFocusedIndex by remember { mutableIntStateOf(0) }
    var sidebarPane by remember { mutableStateOf(SidebarPane.CHANNELS) }
    var groupFocusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(sidebarVisible) {
        if (sidebarVisible) {
            sidebarFocusedIndex = currentChannelIndex.coerceAtLeast(0)
            sidebarPane = SidebarPane.CHANNELS
        }
    }

    LaunchedEffect(visibleChannels, currentChannelKey) {
        sidebarFocusedIndex = currentChannelIndex.coerceAtLeast(0)
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
                        if (!sidebarVisible) {
                            vm.showSidebar()
                            sidebarPane = SidebarPane.CHANNELS
                        } else if (sidebarPane == SidebarPane.CHANNELS) {
                            sidebarPane = SidebarPane.GROUPS
                            val idx = if (selectedGroup == null) 0
                                else groups.indexOfFirst { it.name == selectedGroup } + 1
                            groupFocusedIndex = idx.coerceAtLeast(0)
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        if (sidebarVisible) {
                            if (sidebarPane == SidebarPane.GROUPS) {
                                // Qrupu seç və kanallar paneline keç
                                val groupName = if (groupFocusedIndex == 0) null
                                    else groups.getOrNull(groupFocusedIndex - 1)?.name
                                vm.selectGroup(groupName)
                                sidebarPane = SidebarPane.CHANNELS
                                sidebarFocusedIndex = 0
                            } else {
                                // Sağ düymə kanalı seçir və sidebarı bağlayır
                                visibleChannels.getOrNull(sidebarFocusedIndex)?.let { vm.selectChannel(it) }
                            }
                            true
                        } else false
                    }
                    Key.DirectionUp -> {
                        if (sidebarVisible) {
                            when (sidebarPane) {
                                SidebarPane.CHANNELS ->
                                    if (sidebarFocusedIndex > 0) sidebarFocusedIndex--
                                SidebarPane.GROUPS ->
                                    if (groupFocusedIndex > 0) groupFocusedIndex--
                            }
                            true
                        } else {
                            vm.nextChannel(); true
                        }
                    }
                    Key.DirectionDown -> {
                        if (sidebarVisible) {
                            when (sidebarPane) {
                                SidebarPane.CHANNELS ->
                                    if (sidebarFocusedIndex < visibleChannels.size - 1) sidebarFocusedIndex++
                                SidebarPane.GROUPS -> {
                                    val groupCount = groups.size + 1
                                    if (groupFocusedIndex < groupCount - 1) groupFocusedIndex++
                                }
                            }
                            true
                        } else {
                            vm.prevChannel(); true
                        }
                    }
                    Key.ChannelUp, Key.PageUp -> {
                        if (!sidebarVisible) { vm.nextChannel(); true } else false
                    }
                    Key.ChannelDown, Key.PageDown -> {
                        if (!sidebarVisible) { vm.prevChannel(); true } else false
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        if (sidebarVisible) {
                            when (sidebarPane) {
                                SidebarPane.CHANNELS ->
                                    visibleChannels.getOrNull(sidebarFocusedIndex)?.let { vm.selectChannel(it) }
                                SidebarPane.GROUPS -> {
                                    val groupName = if (groupFocusedIndex == 0) null
                                        else groups.getOrNull(groupFocusedIndex - 1)?.name
                                    vm.selectGroup(groupName)
                                    sidebarPane = SidebarPane.CHANNELS
                                    sidebarFocusedIndex = 0
                                }
                            }
                            true
                        } else {
                            vm.showSidebar()
                            sidebarPane = SidebarPane.CHANNELS
                            true
                        }
                    }
                    Key.Back -> {
                        when {
                            sidebarVisible && sidebarPane == SidebarPane.GROUPS -> {
                                sidebarPane = SidebarPane.CHANNELS; true
                            }
                            sidebarVisible -> { vm.hideSidebar(); true }
                            osdVisible -> { vm.hideOsd(); true }
                            else -> false
                        }
                    }
                    Key.Menu -> {
                        if (sidebarVisible) vm.refreshPlaylist() else vm.toggleSidebar()
                        true
                    }
                    Key.Info -> { vm.showOsd(); true }
                    else -> false
                }
            }
    ) {
        val isWide = maxWidth > 500.dp
        val panelWidth = if (isWide) (maxWidth * 0.32f).coerceIn(280.dp, 380.dp) else maxWidth

        // 1. Tam ekran video
        VideoSurface(engine = engine)

        // 2. Sidebar açıqkən örtük
        if (sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000))
                    .zIndex(1f)
            )
        }

        // 3. Sol gradient
        AnimatedVisibility(
            visible = sidebarVisible && isWide,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.zIndex(2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(panelWidth)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PanelBg, PanelBgSoft.copy(alpha = 0.46f), Color.Transparent)
                        )
                    )
            )
        }

        // 4. Mobil — sidebar açıq: kənar tap bağlayır
        if (!isWide && sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)
                    .clickable { vm.hideSidebar() }
            )
        }

        // 5. Sol kanal paneli
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight()
                    .background(PanelBg)
            ) {
                ChannelPanelHeader(
                    title = selectedGroup ?: "Bütün kanallar",
                    channelCount = visibleChannels.size,
                    currentChannelName = currentChannel?.name,
                    activePane = sidebarPane,
                    isRefreshing = loadState is LoadState.Loading,
                    fromCache = (loadState as? LoadState.Success)?.fromCache == true,
                    onRefreshClick = vm::refreshPlaylist,
                    onPlaylistClick = onNavigateToPlaylist,
                    onCloseClick = { vm.hideSidebar() }
                )
                GroupTabs(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onGroupSelect = {
                        vm.selectGroup(it)
                        sidebarPane = SidebarPane.CHANNELS
                        sidebarFocusedIndex = 0
                    },
                    focusedGroupIndex = if (sidebarPane == SidebarPane.GROUPS) groupFocusedIndex else -1,
                    modifier = if (sidebarPane == SidebarPane.GROUPS) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 178.dp)
                    }
                )
                if (sidebarPane == SidebarPane.CHANNELS) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x22FFFFFF))
                    )

                    if (visibleChannels.isEmpty() && loadState is LoadState.Loading) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = Accent, modifier = Modifier.size(34.dp))
                                Text("Kanallar hazırlanır", color = Color.White, fontSize = 14.sp)
                                Text(
                                    "İlk yükləmədən sonra cache-dən açılacaq",
                                    color = Color(0xFF7B919D),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (visibleChannels.isEmpty()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FPlayerMark(size = 46)
                                Text("Kanal tapılmadı", color = Color(0xFF8EA1AA), fontSize = 13.sp)
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Accent.copy(alpha = 0.2f))
                                        .clickable(onClick = onNavigateToPlaylist)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("Pleylist əlavə et", color = Accent, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        ChannelList(
                            channels = visibleChannels,
                            currentChannel = currentChannel,
                            currentFrameRate = videoInfo.frameRate,
                            focusedIndex = sidebarFocusedIndex,
                            onChannelClick = vm::selectChannel,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 6. Mobil — sidebar bağlı: swipe + tap
        if (!isWide && !sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
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
                                moved && delta < -swipePx -> vm.nextChannel()
                                moved && delta > swipePx  -> vm.prevChannel()
                                !moved -> vm.showSidebar()
                            }
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(6f)
                    .width(20.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0xAA000000))
                    .clickable { vm.showSidebar() },
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = Color(0xFFAAAAAA), fontSize = 18.sp)
            }
        }

        // 7. Yüklənir
        AnimatedVisibility(
            visible = playbackState is PlaybackState.Buffering,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(3f)
        ) {
            val offset = if (sidebarVisible && isWide) panelWidth / 2 else 0.dp
            CircularProgressIndicator(
                color = Accent,
                modifier = Modifier
                    .padding(start = offset)
                    .size(48.dp)
            )
        }

        // 8. Xəta bildirişi
        AnimatedVisibility(
            visible = playbackState is PlaybackState.Error,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (sidebarVisible && isWide) panelWidth else 0.dp,
                    bottom = 24.dp
                )
                .zIndex(3f)
        ) {
            val msg = (playbackState as? PlaybackState.Error)?.message ?: ""
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC1A0000))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("⚠ $msg", color = Color(0xFFFF6666), fontSize = 13.sp)
            }
        }

        // 9. OSD
        ChannelInfoOsd(
            visible = osdVisible,
            channel = currentChannel,
            videoInfo = videoInfo,
            channelIndex = channelIndex,
            totalChannels = visibleChannels.size,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(3f)
        )

        // 10. Swipe göstəricisi (sidebar bağlı, mobil)
        if (!isWide && !sidebarVisible && visibleChannels.size > 1) {
            SwipeHint(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .zIndex(6f)
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
private fun FPlayerMark(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 5).dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B2A35), Color(0xFF061014))
                )
            )
            .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape((size / 5).dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("F", color = Accent, fontSize = (size * 0.48f).sp, fontWeight = FontWeight.Black)
            Text("▶", color = Color(0xFFFFC857), fontSize = (size * 0.26f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SwipeHint(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        visible = false
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x88000000))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "← Sürüşdür →   Tap = Kanal siyahısı",
                color = Color(0xAAFFFFFF),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelPanelHeader(
    title: String,
    channelCount: Int,
    currentChannelName: String?,
    activePane: SidebarPane,
    isRefreshing: Boolean,
    fromCache: Boolean,
    onRefreshClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF091820))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FPlayerMark(size = 34)
                Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (activePane == SidebarPane.GROUPS) "KATEQORIYA" else "KANALLAR",
                        color = Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activePane == SidebarPane.GROUPS) "Kateqoriyalar" else title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (currentChannelName != null && activePane == SidebarPane.CHANNELS) {
                    Text(
                        text = currentChannelName,
                        color = Accent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activePane == SidebarPane.GROUPS) {
                    Text(
                        text = "← Kanal siyahısına qayıt",
                        color = Color(0xFF6F8792),
                        fontSize = 10.sp
                    )
                }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isRefreshing) "..." else "↻",
                    color = if (isRefreshing) Color(0xFF7B919D) else Accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable(enabled = !isRefreshing, onClick = onRefreshClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "☰",
                    color = Color(0xFF9FB1BA),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onPlaylistClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "✕",
                    color = Color(0xFFB8C6CC),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onCloseClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (channelCount > 0 && activePane == SidebarPane.CHANNELS) {
            Row(
                modifier = Modifier.padding(start = 44.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$channelCount kanal",
                    color = Color(0xFF6F8792),
                    fontSize = 10.sp
                )
                if (fromCache) {
                    Text(
                        text = "cache",
                        color = Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
