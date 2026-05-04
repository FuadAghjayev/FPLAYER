package az.iptv.fplayer.ui.screen

import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import az.iptv.fplayer.player.*
import az.iptv.fplayer.ui.component.ChannelInfoOsd
import az.iptv.fplayer.ui.component.ChannelList
import az.iptv.fplayer.ui.component.GroupTabs
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlin.math.absoluteValue

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

    val engine: PlayerEngine = remember(playerType, context) {
        when (playerType) {
            PlayerType.VLC -> VlcPlayerEngine(context)
            else -> ExoPlayerEngine(context)
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
        currentChannel?.let { engine.play(it.url) }
    }

    val channelIndex = visibleChannels.indexOf(currentChannel) + 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // TV remote / D-pad idarəsi
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { vm.showSidebar(); true }
                    Key.DirectionRight -> if (sidebarVisible) { vm.hideSidebar(); true } else false
                    Key.DirectionUp -> if (!sidebarVisible) { vm.prevChannel(); true } else false
                    Key.DirectionDown -> if (!sidebarVisible) { vm.nextChannel(); true } else false
                    Key.Enter, Key.NumPadEnter -> {
                        if (sidebarVisible) { vm.playCurrentChannel(); true } else false
                    }
                    Key.Back -> { vm.toggleSidebar(); true }
                    Key.Menu -> { vm.toggleSidebar(); true }
                    Key.Info -> { vm.showOsd(); true }
                    else -> false
                }
            }
    ) {
        val isWide = maxWidth > 500.dp
        val panelWidth = if (isWide) (maxWidth * 0.32f).coerceIn(280.dp, 380.dp) else maxWidth

        // ── 1. Tam ekran video (həmişə arxada) ──────────────────────────
        VideoSurface(engine = engine)

        // ── 2. Sol panel arxasında gradient ──────────────────────────────
        AnimatedVisibility(
            visible = sidebarVisible && isWide,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(panelWidth)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xF2050505), Color(0x660A0A0A))
                        )
                    )
            )
        }

        // ── 3. Mobil — sidebar AÇIQDIR ───────────────────────────────────
        // Panel xaricindəki sahəyə toxunmaq sidebar-ı bağlayır
        if (!isWide && sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)         // sidebar-dan (10f) aşağı
                    .clickable { vm.hideSidebar() }
            )
        }

        // ── 4. Sol kanal paneli ──────────────────────────────────────────
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxHeight().zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight()
                    .background(Color(0xF2050505))
            ) {
                ChannelPanelHeader(
                    title = selectedGroup ?: "Bütün kanallar",
                    channelCount = visibleChannels.size,
                    currentChannelName = currentChannel?.name,
                    onPlaylistClick = onNavigateToPlaylist,
                    onCloseClick = { vm.hideSidebar() }
                )
                GroupTabs(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onGroupSelect = vm::selectGroup
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))

                if (visibleChannels.isEmpty() && loadState !is LoadState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📺", fontSize = 32.sp)
                            Text("Kanal tapılmadı", color = Color(0xFF888888), fontSize = 13.sp)
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
                        onChannelClick = vm::selectChannel,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }

        // ── 5. Mobil — sidebar BAĞLIDIR ──────────────────────────────────
        // • Tap      → sidebar aç
        // • Sola sürüşdür → növbəti kanal
        // • Sağa sürüşdür → əvvəlki kanal
        if (!isWide && !sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .pointerInput(Unit) {
                        val swipePx = 60.dp.toPx()   // swipe üçün minimum məsafə
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
                                moved && delta < -swipePx -> vm.nextChannel()   // ← sola swipe
                                moved && delta > swipePx  -> vm.prevChannel()   // → sağa swipe
                                !moved -> vm.showSidebar()                      // tap → aç
                            }
                        }
                    }
            )

            // Sol kənarda görünən "aç" düyməsi
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(6f)
                    .padding(start = 0.dp)
                    .width(20.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0x99000000))
                    .clickable { vm.showSidebar() },
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = Color(0xFFAAAAAA), fontSize = 18.sp)
            }
        }

        // ── 6. Yüklənir ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = playbackState is PlaybackState.Buffering,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val offset = if (sidebarVisible && isWide) panelWidth / 2 else 0.dp
            CircularProgressIndicator(
                color = Accent,
                modifier = Modifier.padding(start = offset).size(48.dp)
            )
        }

        // ── 7. Xəta bildirişi ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = playbackState is PlaybackState.Error,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (sidebarVisible && isWide) panelWidth else 0.dp,
                    bottom = 24.dp
                )
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

        // ── 8. OSD kanalı məlumatları ─────────────────────────────────────
        ChannelInfoOsd(
            visible = osdVisible,
            channel = currentChannel,
            videoInfo = videoInfo,
            channelIndex = channelIndex,
            totalChannels = visibleChannels.size,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        )

        // ── 9. Swipe göstəricisi (sidebar bağlı, mobil) ──────────────────
        if (!isWide && !sidebarVisible && visibleChannels.size > 1) {
            SwipeHint(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
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
    onPlaylistClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentChannelName != null) {
                    Text(
                        text = currentChannelName,
                        color = Accent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "≡",
                    color = Color(0xFF888888),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable(onClick = onPlaylistClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "✕",
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable(onClick = onCloseClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (channelCount > 0) {
            Text(
                text = "$channelCount kanal",
                color = Color(0xFF555555),
                fontSize = 10.sp
            )
        }
    }
}
