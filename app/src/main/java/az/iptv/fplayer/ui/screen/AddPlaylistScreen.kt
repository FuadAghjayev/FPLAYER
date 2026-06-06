package az.iptv.fplayer.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import az.iptv.fplayer.data.preferences.AppPreferences
import az.iptv.fplayer.data.preferences.AppLanguage
import az.iptv.fplayer.data.preferences.PlaylistProfile
import az.iptv.fplayer.data.preferences.PlaylistType
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.ui.text.AppTexts
import az.iptv.fplayer.ui.text.appTexts
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.FocusBorder
import az.iptv.fplayer.ui.theme.PanelBg
import az.iptv.fplayer.ui.theme.TextSecondary
import az.iptv.fplayer.ui.theme.WarmAccent
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel

private enum class SourceTab { M3U, XTREAM }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    onPlaylistLoaded: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val loadState by vm.loadState.collectAsState()
    val playerType by vm.playerType.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val activePlaylist by vm.activePlaylist.collectAsState()
    val language by vm.appLanguage.collectAsState()
    val t = appTexts(language)

    var selectedTab by remember { mutableStateOf(SourceTab.M3U) }
    var editingPlaylistId by remember { mutableStateOf<String?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var pendingPlayerReturn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.resetForPlaylistEdit()
    }

    LaunchedEffect(loadState, pendingPlayerReturn) {
        if (pendingPlayerReturn && loadState is LoadState.Success) {
            pendingPlayerReturn = false
            onPlaylistLoaded()
        }
    }

    LaunchedEffect(activePlaylist?.id) {
        activePlaylist?.let { profile ->
            editingPlaylistId = profile.id
            playlistName = profile.name
            selectedTab = if (profile.type == PlaylistType.XTREAM) SourceTab.XTREAM else SourceTab.M3U
            m3uUrl = profile.m3uUrl
            xtreamServer = profile.xtreamServer
            xtreamUser = profile.xtreamUser
            xtreamPass = profile.xtreamPass
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(AppBg, Color(0xFF0B1F27), Color(0xFF020609))
                )
            )
    ) {
        val isWide = maxWidth > 600.dp
        val scrollState = rememberScrollState()

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                BrandPanel(t)
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0x22FFFFFF)))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = if (isWide) 56.dp else 24.dp,
                        vertical = if (isWide) 40.dp else 28.dp
                    ),
                verticalArrangement = Arrangement.Top
            ) {
                if (!isWide) CompactBrand()

                Text(
                    text = t.playlistSource,
                    color = Color.White,
                    fontSize = if (isWide) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(text = t.playlistSubtitle, color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))

                if (playlists.isNotEmpty()) {
                    SectionTitle(t.savedPlaylists)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playlists, key = { it.id }) { profile ->
                            PlaylistChip(
                                profile = profile,
                                selected = profile.id == activePlaylist?.id,
                                texts = t,
                                deleteLabel = if (language == AppLanguage.EN.name) "Delete" else "Sil",
                                modifier = Modifier.width(156.dp),
                                onClick = {
                                    editingPlaylistId = profile.id
                                    pendingPlayerReturn = true
                                    vm.switchPlaylist(profile)
                                },
                                onDelete = {
                                    if (editingPlaylistId == profile.id) {
                                        editingPlaylistId = null
                                        playlistName = ""
                                        m3uUrl = ""
                                        xtreamServer = ""
                                        xtreamUser = ""
                                        xtreamPass = ""
                                    }
                                    pendingPlayerReturn = false
                                    vm.deletePlaylist(profile)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallActionButton(
                        label = t.addPlaylist,
                        enabled = playlists.size < AppPreferences.MAX_PLAYLISTS,
                        onClick = {
                            editingPlaylistId = null
                            playlistName = ""
                            selectedTab = SourceTab.M3U
                            m3uUrl = ""
                            xtreamServer = ""
                            xtreamUser = ""
                            xtreamPass = ""
                        }
                    )
                    Text(t.playlistLimit, color = TextSecondary, fontSize = 12.sp)
                }

                Spacer(Modifier.height(22.dp))
                SourceTabs(selectedTab = selectedTab, texts = t, onSelect = { selectedTab = it })
                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "playlist_form"
                ) { tab ->
                    PlaylistForm(
                        tab = tab,
                        texts = t,
                        playlistName = playlistName,
                        onPlaylistNameChange = { playlistName = it },
                        m3uUrl = m3uUrl,
                        onM3uUrlChange = { m3uUrl = it },
                        xtreamServer = xtreamServer,
                        onXtreamServerChange = { xtreamServer = it },
                        xtreamUser = xtreamUser,
                        onXtreamUserChange = { xtreamUser = it },
                        xtreamPass = xtreamPass,
                        onXtreamPassChange = { xtreamPass = it },
                        isWide = isWide
                    )
                }

                Spacer(Modifier.height(20.dp))

                val isLoading = loadState is LoadState.Loading
                val hasSlot = editingPlaylistId != null || playlists.size < AppPreferences.MAX_PLAYLISTS
                val canLoad = hasSlot && !isLoading && when (selectedTab) {
                    SourceTab.M3U -> m3uUrl.isNotBlank()
                    SourceTab.XTREAM -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank()
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LoadButton(
                        label = if (isLoading) t.loading else t.loadRefresh,
                        enabled = canLoad,
                        onClick = {
                            val type = if (selectedTab == SourceTab.XTREAM) PlaylistType.XTREAM else PlaylistType.M3U
                            val fallbackName = if (type == PlaylistType.XTREAM) t.sourceXtream else t.sourceM3u
                            pendingPlayerReturn = true
                            vm.savePlaylistAndLoad(
                                PlaylistProfile(
                                    id = editingPlaylistId ?: "playlist_${System.currentTimeMillis()}",
                                    name = playlistName.trim().ifBlank { fallbackName },
                                    type = type,
                                    m3uUrl = if (type == PlaylistType.M3U) m3uUrl.trim() else "",
                                    xtreamServer = if (type == PlaylistType.XTREAM) xtreamServer.trim() else "",
                                    xtreamUser = if (type == PlaylistType.XTREAM) xtreamUser.trim() else "",
                                    xtreamPass = if (type == PlaylistType.XTREAM) xtreamPass.trim() else ""
                                )
                            )
                        }
                    )
                    Status(loadState = loadState, texts = t)
                }

                Spacer(Modifier.height(32.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
                Spacer(Modifier.height(20.dp))

                SectionTitle(t.videoPlayer)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerChip(
                        label = "ExoPlayer",
                        subtitle = t.recommended,
                        selected = playerType == PlayerType.EXOPLAYER,
                        onClick = { vm.setPlayerType(PlayerType.EXOPLAYER) }
                    )
                    PlayerChip(
                        label = "VLC",
                        subtitle = t.manualVlc,
                        selected = playerType == PlayerType.VLC,
                        onClick = { vm.setPlayerType(PlayerType.VLC) }
                    )
                }

                Spacer(Modifier.height(22.dp))
                SectionTitle(t.language)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerChip(
                        label = "AZ",
                        subtitle = t.az,
                        selected = language == AppLanguage.AZ.name,
                        onClick = { vm.setLanguage(AppLanguage.AZ) }
                    )
                    PlayerChip(
                        label = "EN",
                        subtitle = t.en,
                        selected = language == AppLanguage.EN.name,
                        onClick = { vm.setLanguage(AppLanguage.EN) }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrandPanel(texts: AppTexts) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(PanelBg)
            .padding(40.dp),
        verticalArrangement = Arrangement.Center
    ) {
        FPlayerLogo(size = 92)
        Spacer(Modifier.height(22.dp))
        Text(
            text = "PLAYER",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "LIVE TV", color = WarmAccent.copy(alpha = 0.82f), fontSize = 13.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(32.dp))
        Box(Modifier.width(42.dp).height(2.dp).background(Accent))
        Spacer(Modifier.height(20.dp))
        Text(
            text = "${texts.sourceM3u} və\n${texts.sourceXtream}",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FPlayerLogo(size = 44)
        Column {
            Text(
                text = "PLAYER",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(text = "LIVE TV", color = TextSecondary, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SourceTabs(selectedTab: SourceTab, texts: AppTexts, onSelect: (SourceTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SourceCard(
            icon = "M3U",
            title = texts.sourceM3u,
            subtitle = texts.m3uSubtitle,
            selected = selectedTab == SourceTab.M3U,
            onClick = { onSelect(SourceTab.M3U) },
            modifier = Modifier.weight(1f)
        )
        SourceCard(
            icon = "XC",
            title = texts.sourceXtream,
            subtitle = texts.xtreamSubtitle,
            selected = selectedTab == SourceTab.XTREAM,
            onClick = { onSelect(SourceTab.XTREAM) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaylistForm(
    tab: SourceTab,
    texts: AppTexts,
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    m3uUrl: String,
    onM3uUrlChange: (String) -> Unit,
    xtreamServer: String,
    onXtreamServerChange: (String) -> Unit,
    xtreamUser: String,
    onXtreamUserChange: (String) -> Unit,
    xtreamPass: String,
    onXtreamPassChange: (String) -> Unit,
    isWide: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormLabel(texts.playlistName)
        FormField(value = playlistName, onValueChange = onPlaylistNameChange, placeholder = "Home TV")

        when (tab) {
            SourceTab.M3U -> {
                FormLabel("M3U URL")
                FormField(
                    value = m3uUrl,
                    onValueChange = onM3uUrlChange,
                    placeholder = "http://server.com/playlist.m3u",
                    keyboardType = KeyboardType.Uri
                )
            }
            SourceTab.XTREAM -> {
                FormLabel(texts.serverUrl)
                FormField(
                    value = xtreamServer,
                    onValueChange = onXtreamServerChange,
                    placeholder = "http://server.com:8080",
                    keyboardType = KeyboardType.Uri
                )
                if (isWide) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormLabel(texts.username)
                            FormField(value = xtreamUser, onValueChange = onXtreamUserChange, placeholder = "username")
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormLabel(texts.password)
                            FormField(value = xtreamPass, onValueChange = onXtreamPassChange, placeholder = "password")
                        }
                    }
                } else {
                    FormLabel(texts.username)
                    FormField(value = xtreamUser, onValueChange = onXtreamUserChange, placeholder = "username")
                    FormLabel(texts.password)
                    FormField(value = xtreamPass, onValueChange = onXtreamPassChange, placeholder = "password")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaylistChip(
    profile: PlaylistProfile,
    selected: Boolean,
    texts: AppTexts,
    deleteLabel: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color(0xFFEAF7FF)
        selected -> Color(0xDD14505A)
        else -> Color(0xD72B323A)
    }
    val border = when {
        focused -> Color(0xFFFFC247)
        selected -> Accent
        else -> Color(0x66FFFFFF)
    }
    val primaryText = if (focused) Color(0xFF071116) else Color.White
    val secondaryText = if (focused) Color(0xFF24333A) else if (selected) Accent else Color(0xFFDCE2E7)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(if (focused) 2.dp else 1.dp, border, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    color = primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (focused) Color(0xFFE43939) else Color(0xFF7E2525))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = deleteLabel,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = if (selected) texts.active else texts.switchTo,
                color = secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FPlayerLogo(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 5).dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B2A35), Color(0xFF061014))))
            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape((size / 5).dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("F", color = Accent, fontSize = (size * 0.48f).sp, fontWeight = FontWeight.Black)
            Text("▶", color = WarmAccent, fontSize = (size * 0.25f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceCard(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        focused -> Color(0xFFFFC247)
        selected -> FocusBorder
        else -> Color(0x66FFFFFF)
    }
    val bgColor = when {
        focused -> Color(0xFFEAF7FF)
        selected -> Color(0xDD14505A)
        else -> Color(0xD72B323A)
    }
    val titleColor = if (focused) Color(0xFF071116) else if (selected) Color.White else Color(0xFFF2F5F7)
    val subColor = if (focused) Color(0xFF2D3A42) else Color(0xFFD4DBE0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(width = if (focused) 2.dp else 1.5.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Accent.copy(alpha = 0.18f) else Color(0x18FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    icon,
                    color = if (focused) Color(0xFF0C5E65) else if (selected) Accent else Color(0xFFD4DBE0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = title,
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, color = subColor, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FormLabel(text: String) {
    Text(text = text, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = if (focused) Color.White else Color(0xFFF0F3F5), fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            showKeyboardOnFocus = false
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    keyboardController?.show()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0x99304A55) else Color(0x8012252F))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFFFFC247) else Color(0x66FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                androidx.compose.material3.Text(
                    text = placeholder,
                    color = if (focused) Color(0xBFFFFFFF) else Color(0x80FFFFFF),
                    fontSize = 14.sp
                )
            }
            inner()
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    focused && enabled -> Color(0xFFFFC247)
                    enabled -> Accent
                    else -> Accent.copy(alpha = 0.22f)
                }
            )
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Color.White else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 13.dp)
    ) {
        Text(
            text = label,
            color = if (focused && enabled) Color(0xFF081116) else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SmallActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    focused && enabled -> Color(0xFFFFC247)
                    enabled -> Color(0x6620D8C6)
                    else -> Color(0x18FFFFFF)
                }
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color.White else Color(0x44FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = if (focused && enabled) Color(0xFF081116) else if (enabled) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Status(loadState: LoadState, texts: AppTexts) {
    when (loadState) {
        is LoadState.Loading -> StatusText(texts.loadingChannels, TextSecondary)
        is LoadState.Success -> StatusText(texts.channelsFound(loadState.count), Color(0xFF44FF88))
        is LoadState.Error -> StatusText("× ${loadState.message}", Color(0xFFFF4444))
        else -> Unit
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 13.sp)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerChip(label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color(0xFFEAF7FF)
        selected -> Color(0xDD14505A)
        else -> Color(0xD72B323A)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color(0xFFFFC247) else if (selected) Accent.copy(alpha = 0.82f) else Color(0x44FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (focused) Color(0xFF071116) else if (selected) Accent else Color.White,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = if (focused) Color(0xFF2D3A42) else Color(0xFFD4DBE0),
                fontSize = 10.sp
            )
        }
    }
}
