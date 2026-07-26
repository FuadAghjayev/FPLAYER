@file:OptIn(ExperimentalTvMaterial3Api::class)

package az.iptv.fplayer.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import az.iptv.fplayer.BuildConfig
import az.iptv.fplayer.data.preferences.AppLanguage
import az.iptv.fplayer.data.preferences.AppPreferences
import az.iptv.fplayer.data.preferences.AppThemeMode
import az.iptv.fplayer.data.preferences.PlaylistProfile
import az.iptv.fplayer.data.preferences.PlaylistType
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.PlaybackSettings
import az.iptv.fplayer.ui.text.AppTexts
import az.iptv.fplayer.ui.text.appTexts
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.AppBgEnd
import az.iptv.fplayer.ui.theme.AppBgMid
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel

private enum class SourceTab { M3U, XTREAM }

private enum class MenuSection { PLAYLISTS, SOURCE, PLAYBACK, APPEARANCE, ABOUT }

// ── Menyu dizayn tokenləri: qara panel + nazik cizgi + kəhrəba vurğu ────────────
private val MenuCardShape = RoundedCornerShape(10.dp)
private val MenuChipShape = RoundedCornerShape(9.dp)
private val MenuPanelShape = RoundedCornerShape(14.dp)
private val MenuRailBg = Color(0xF5080B10)
private val MenuSurface = Color(0xB3121820)
private val MenuSurfaceRaised = Color(0xE61A2331)
private val MenuHairline = Color(0x1AFFFFFF)
private val MenuHairlineStrong = Color(0x33FFFFFF)
private val MenuTextDim = Color(0xFF8C99A6)
private val MenuTextBody = Color(0xFFD3DBE3)
private val MenuOnFocus = Color(0xFF0A0E13)

private fun sectionLabel(section: MenuSection, t: AppTexts): String = when (section) {
    MenuSection.PLAYLISTS -> t.playlists
    MenuSection.SOURCE -> t.playlistSource
    MenuSection.PLAYBACK -> t.playbackSettings
    MenuSection.APPEARANCE -> t.appearance
    MenuSection.ABOUT -> t.about
}

private fun sectionHint(section: MenuSection, t: AppTexts): String = when (section) {
    MenuSection.PLAYLISTS -> t.playlistsHint
    MenuSection.SOURCE -> t.playlistSubtitle
    MenuSection.PLAYBACK -> t.playbackHint
    MenuSection.APPEARANCE -> t.appearanceHint
    MenuSection.ABOUT -> t.aboutHint
}

private fun sectionGlyph(section: MenuSection): String = when (section) {
    MenuSection.PLAYLISTS -> "≡"
    MenuSection.SOURCE -> "+"
    MenuSection.PLAYBACK -> "▶"
    MenuSection.APPEARANCE -> "◐"
    MenuSection.ABOUT -> "i"
}

@Composable
fun AddPlaylistScreen(
    onPlaylistLoaded: () -> Unit,
    onBackToPlayer: (() -> Unit)? = null,
    vm: PlayerViewModel = viewModel()
) {
    val loadState by vm.loadState.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val activePlaylist by vm.activePlaylist.collectAsState()
    val language by vm.appLanguage.collectAsState()
    val themeMode by vm.appThemeMode.collectAsState()
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()
    val playbackSettings by vm.playbackSettings.collectAsState()
    val showFps by vm.showFps.collectAsState()
    val t = appTexts(language)

    var section by remember { mutableStateOf(MenuSection.PLAYLISTS) }
    var selectedTab by remember { mutableStateOf(SourceTab.M3U) }
    var editingPlaylistId by remember { mutableStateOf<String?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var pendingPlayerReturn by remember { mutableStateOf(false) }

    fun clearForm() {
        editingPlaylistId = null
        playlistName = ""
        selectedTab = SourceTab.M3U
        m3uUrl = ""
        xtreamServer = ""
        xtreamUser = ""
        xtreamPass = ""
    }

    fun editProfile(profile: PlaylistProfile) {
        editingPlaylistId = profile.id
        playlistName = profile.name
        selectedTab = if (profile.type == PlaylistType.XTREAM) SourceTab.XTREAM else SourceTab.M3U
        m3uUrl = profile.m3uUrl
        xtreamServer = profile.xtreamServer
        xtreamUser = profile.xtreamUser
        xtreamPass = profile.xtreamPass
    }

    fun switchProfile(profile: PlaylistProfile) {
        editProfile(profile)
        pendingPlayerReturn = true
        vm.switchPlaylist(profile)
    }

    fun deleteProfile(profile: PlaylistProfile) {
        if (editingPlaylistId == profile.id) clearForm()
        pendingPlayerReturn = false
        vm.deletePlaylist(profile)
    }

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
        activePlaylist?.let(::editProfile)
    }

    val canReturnToPlayer = onBackToPlayer != null && playlists.isNotEmpty()
    val navFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { navFocus.requestFocus() } }

    val isLoading = loadState is LoadState.Loading
    val hasSlot = editingPlaylistId != null || playlists.size < AppPreferences.MAX_PLAYLISTS
    val detailsValid = when (selectedTab) {
        SourceTab.M3U -> m3uUrl.isNotBlank()
        SourceTab.XTREAM -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank()
    }

    fun saveAndLoad() {
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(AppBg, AppBgMid, AppBgEnd)))
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Back || event.key == Key.Escape) &&
                    canReturnToPlayer
                ) {
                    onBackToPlayer?.invoke()
                    true
                } else {
                    false
                }
            }
    ) {
        val isWide = maxWidth > 600.dp
        val contentScroll = remember(section) { ScrollState(0) }

        val navigation: @Composable () -> Unit = {
            if (isWide) {
                MenuRail(
                    texts = t,
                    selected = section,
                    canReturnToPlayer = canReturnToPlayer,
                    onSelect = { section = it },
                    onBackToPlayer = { onBackToPlayer?.invoke() },
                    focusRequester = navFocus,
                    modifier = Modifier
                        .width(232.dp)
                        .fillMaxHeight()
                )
            } else {
                MenuTopBar(
                    texts = t,
                    selected = section,
                    canReturnToPlayer = canReturnToPlayer,
                    onSelect = { section = it },
                    onBackToPlayer = { onBackToPlayer?.invoke() },
                    focusRequester = navFocus
                )
            }
        }

        val content: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScroll)
                    .padding(
                        horizontal = if (isWide) 38.dp else 18.dp,
                        vertical = if (isWide) 28.dp else 18.dp
                    )
            ) {
                SectionHeader(
                    title = sectionLabel(section, t),
                    hint = sectionHint(section, t)
                )
                Spacer(Modifier.height(20.dp))

                when (section) {
                    MenuSection.PLAYLISTS -> PlaylistsSection(
                        texts = t,
                        playlists = playlists,
                        activePlaylistId = activePlaylist?.id,
                        canAddPlaylist = playlists.size < AppPreferences.MAX_PLAYLISTS,
                        columns = if (isWide) 2 else 1,
                        onSwitch = ::switchProfile,
                        onEdit = { profile ->
                            editProfile(profile)
                            section = MenuSection.SOURCE
                        },
                        onDelete = ::deleteProfile,
                        onAdd = {
                            clearForm()
                            section = MenuSection.SOURCE
                        }
                    )

                    MenuSection.SOURCE -> SourceSection(
                        texts = t,
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it },
                        isEditing = editingPlaylistId != null,
                        editingName = playlists.firstOrNull { it.id == editingPlaylistId }?.name.orEmpty(),
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
                        isWide = isWide,
                        isLoading = isLoading,
                        canLoad = hasSlot && !isLoading && detailsValid,
                        loadState = loadState,
                        onLoad = ::saveAndLoad,
                        onReset = { clearForm() }
                    )

                    MenuSection.PLAYBACK -> PlaybackSection(
                        texts = t,
                        audioDecoderMode = audioDecoderMode,
                        settings = playbackSettings,
                        showFps = showFps,
                        onAudioMode = vm::setAudioDecoderMode,
                        onShowFps = vm::setShowFps,
                        onFrameRateMatching = vm::setFrameRateMatching,
                        onRawAudioConvert = vm::setRawAudioConvert,
                        onTunneledPlayback = vm::setTunneledPlayback,
                        onFix1080i = vm::setFix1080i
                    )

                    MenuSection.APPEARANCE -> AppearanceSection(
                        texts = t,
                        language = language,
                        themeMode = themeMode,
                        onLanguage = vm::setLanguage,
                        onTheme = vm::setThemeMode
                    )

                    MenuSection.ABOUT -> AboutSection(
                        texts = t,
                        playlistCount = playlists.size,
                        activeName = activePlaylist?.name.orEmpty()
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                navigation()
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MenuHairline)
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                navigation()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
            }
        }
    }
}

// ── Naviqasiya ────────────────────────────────────────────────────────────────

@Composable
private fun MenuRail(
    texts: AppTexts,
    selected: MenuSection,
    canReturnToPlayer: Boolean,
    onSelect: (MenuSection) -> Unit,
    onBackToPlayer: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val sections = remember { MenuSection.values().toList() }
    Column(
        modifier = modifier
            .background(MenuRailBg)
            .padding(horizontal = 16.dp, vertical = 22.dp)
    ) {
        BrandMark(logoSize = 42, compact = false)

        Spacer(Modifier.height(22.dp))
        BlockLabel(texts.menu)
        Spacer(Modifier.height(8.dp))

        sections.forEachIndexed { index, item ->
            MenuNavItem(
                glyph = sectionGlyph(item),
                label = sectionLabel(item, texts),
                selected = item == selected,
                onClick = { onSelect(item) },
                // İlk element açılışda fokusu qəbul edir ki, D-pad dərhal işləsin
                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier
            )
            Spacer(Modifier.height(5.dp))
        }

        Spacer(Modifier.weight(1f))

        if (canReturnToPlayer) {
            MenuNavItem(
                glyph = "←",
                label = texts.player,
                selected = false,
                onClick = onBackToPlayer
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = "${texts.version} ${BuildConfig.VERSION_NAME}",
            color = MenuTextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MenuTopBar(
    texts: AppTexts,
    selected: MenuSection,
    canReturnToPlayer: Boolean,
    onSelect: (MenuSection) -> Unit,
    onBackToPlayer: () -> Unit,
    focusRequester: FocusRequester
) {
    val sections = remember { MenuSection.values().toList() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuRailBg)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(logoSize = 34, compact = true)
            Spacer(Modifier.weight(1f))
            if (canReturnToPlayer) {
                MiniButton(label = texts.player, onClick = onBackToPlayer)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEachIndexed { index, item ->
                MenuTab(
                    label = sectionLabel(item, texts),
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier
                )
            }
        }
    }
}

@Composable
private fun MenuNavItem(
    glyph: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(MenuCardShape)
            .background(
                when {
                    focused -> accent
                    selected -> Color(0x1FFFFFFF)
                    else -> Color.Transparent
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        focused -> Color(0x1F000000)
                        selected -> accent.copy(alpha = 0.18f)
                        else -> Color(0x14FFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = when {
                    focused -> MenuOnFocus
                    selected -> accent
                    else -> MenuTextDim
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        Text(
            text = label,
            color = when {
                focused -> MenuOnFocus
                selected -> Color.White
                else -> MenuTextBody
            },
            fontSize = 13.sp,
            fontWeight = if (focused || selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MenuTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    Box(
        modifier = modifier
            .clip(MenuChipShape)
            .background(
                when {
                    focused -> accent
                    selected -> Color(0x1FFFFFFF)
                    else -> Color(0x0DFFFFFF)
                }
            )
            .border(
                1.dp,
                if (selected && !focused) accent.copy(alpha = 0.55f) else Color.Transparent,
                MenuChipShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = when {
                focused -> MenuOnFocus
                selected -> accent
                else -> MenuTextBody
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun BrandMark(logoSize: Int, compact: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FPlayerLogo(size = logoSize)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "FTV",
                color = Color.White,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                maxLines = 1
            )
            Text(
                text = "LIVE TV",
                color = Accent.copy(alpha = 0.85f),
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FPlayerLogo(size: Int, modifier: Modifier = Modifier) {
    val logoShape = RoundedCornerShape((size / 4).dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(logoShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A2230), Color(0xFF0A0E13))))
            .border(1.dp, MenuHairlineStrong, logoShape),
        contentAlignment = Alignment.Center
    ) {
        Text("F", color = Accent, fontSize = (size * 0.5f).sp, fontWeight = FontWeight.Black)
    }
}

// ── Ümumi hissələr ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, hint: String) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .width(44.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(Accent, Accent.copy(alpha = 0f))))
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = hint,
            color = MenuTextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun BlockLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MenuTextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.4.sp,
        maxLines = 1
    )
}

@Composable
private fun MenuPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(MenuPanelShape)
            .background(MenuSurface)
            .border(1.dp, MenuHairline, MenuPanelShape)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun MiniButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    val focusFill = if (danger) Color(0xFFE04141) else accent
    Box(
        modifier = modifier
            .clip(MenuChipShape)
            .background(
                when {
                    focused && enabled -> focusFill
                    enabled -> Color(0x14FFFFFF)
                    else -> Color(0x0AFFFFFF)
                }
            )
            .border(
                1.dp,
                when {
                    focused && enabled -> focusFill
                    enabled -> MenuHairlineStrong
                    else -> MenuHairline
                },
                MenuChipShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = when {
                focused && enabled -> if (danger) Color.White else MenuOnFocus
                enabled -> MenuTextBody
                else -> MenuTextDim
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    Box(
        modifier = Modifier
            .clip(MenuChipShape)
            .background(
                when {
                    focused && enabled -> Brush.horizontalGradient(
                        listOf(Color(0xFFFFE08A), accent)
                    )
                    enabled -> Brush.horizontalGradient(listOf(accent, Color(0xFFFFB020)))
                    else -> SolidColor(accent.copy(alpha = 0.2f))
                }
            )
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Color.White else Color.Transparent,
                MenuChipShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 11.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) MenuOnFocus else Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun OptionChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    Column(
        modifier = modifier
            .clip(MenuCardShape)
            .background(
                when {
                    focused -> accent
                    selected -> MenuSurfaceRaised
                    else -> MenuSurface
                }
            )
            .border(
                if (focused || selected) 1.5.dp else 1.dp,
                when {
                    focused -> accent
                    selected -> accent.copy(alpha = 0.7f)
                    else -> MenuHairline
                },
                MenuCardShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            focused -> MenuOnFocus
                            selected -> accent
                            else -> Color(0x33FFFFFF)
                        }
                    )
            )
            Text(
                text = label,
                color = if (focused) MenuOnFocus else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = subtitle,
            color = if (focused) MenuOnFocus.copy(alpha = 0.75f) else MenuTextDim,
            fontSize = 10.sp,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onLabel: String,
    offLabel: String,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MenuCardShape)
            .background(if (focused) accent else MenuSurface)
            .border(
                if (focused) 1.5.dp else 1.dp,
                if (focused) accent else MenuHairline,
                MenuCardShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = if (focused) MenuOnFocus else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hint,
                color = if (focused) MenuOnFocus.copy(alpha = 0.75f) else MenuTextDim,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
        // Klassik alıcı stilində ON / OFF açarı
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (focused) Color(0x24000000) else Color(0x14FFFFFF))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SwitchSide(
                text = offLabel,
                active = !checked,
                activeColor = if (focused) MenuOnFocus else Color(0x33FFFFFF),
                activeTextColor = if (focused) accent else Color.White,
                idleTextColor = if (focused) MenuOnFocus.copy(alpha = 0.6f) else MenuTextDim
            )
            SwitchSide(
                text = onLabel,
                active = checked,
                activeColor = if (focused) MenuOnFocus else accent,
                activeTextColor = if (focused) accent else MenuOnFocus,
                idleTextColor = if (focused) MenuOnFocus.copy(alpha = 0.6f) else MenuTextDim
            )
        }
    }
}

@Composable
private fun SwitchSide(
    text: String,
    active: Boolean,
    activeColor: Color,
    activeTextColor: Color,
    idleTextColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) activeColor else Color.Transparent)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (active) activeTextColor else idleTextColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

// ── Siyahılar ─────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsSection(
    texts: AppTexts,
    playlists: List<PlaylistProfile>,
    activePlaylistId: String?,
    canAddPlaylist: Boolean,
    columns: Int,
    onSwitch: (PlaylistProfile) -> Unit,
    onEdit: (PlaylistProfile) -> Unit,
    onDelete: (PlaylistProfile) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlockLabel("${texts.savedPlaylists} — ${playlists.size}/${AppPreferences.MAX_PLAYLISTS}")
            Spacer(Modifier.weight(1f))
            MiniButton(
                label = texts.addPlaylist,
                enabled = canAddPlaylist,
                onClick = onAdd
            )
        }

        if (playlists.isEmpty()) {
            MenuPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = texts.welcomeSetup,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = texts.playlistLimit,
                    color = MenuTextDim,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(label = texts.addPlaylist, enabled = true, onClick = onAdd)
            }
        } else {
            playlists.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { profile ->
                        PlaylistCard(
                            texts = texts,
                            profile = profile,
                            index = playlists.indexOf(profile) + 1,
                            active = profile.id == activePlaylistId,
                            onClick = { onSwitch(profile) },
                            onEdit = { onEdit(profile) },
                            onDelete = { onDelete(profile) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns.coerceAtLeast(1) - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    texts: AppTexts,
    profile: PlaylistProfile,
    index: Int,
    active: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = Accent
    val typeLabel = if (profile.type == PlaylistType.XTREAM) "XTREAM" else "M3U"
    val source = if (profile.type == PlaylistType.XTREAM) profile.xtreamServer else profile.m3uUrl

    Column(
        modifier = modifier
            .clip(MenuCardShape)
            .background(
                when {
                    focused -> Brush.verticalGradient(listOf(Color(0xFFFFE9A8), Color(0xFFFFD470)))
                    active -> Brush.verticalGradient(listOf(MenuSurfaceRaised, MenuSurface))
                    else -> Brush.verticalGradient(listOf(MenuSurface, MenuSurface))
                }
            )
            .border(
                if (focused || active) 1.5.dp else 1.dp,
                when {
                    focused -> accent
                    active -> accent.copy(alpha = 0.65f)
                    else -> MenuHairline
                },
                MenuCardShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (focused) Color(0x1F000000) else Color(0x14FFFFFF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "P$index",
                    color = if (focused) MenuOnFocus else MenuTextDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (focused) Color(0x1F000000) else accent.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = typeLabel,
                    color = if (focused) MenuOnFocus else accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            if (active) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (focused) MenuOnFocus else Color(0xFF4ADE80))
                    )
                    Text(
                        text = texts.active.uppercase(),
                        color = if (focused) MenuOnFocus else Color(0xFF8EF0B4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(9.dp))
        Text(
            text = profile.name,
            color = if (focused) MenuOnFocus else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = source.ifBlank { if (active) texts.active else texts.switchTo },
            color = if (focused) MenuOnFocus.copy(alpha = 0.7f) else MenuTextDim,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MiniButton(label = texts.edit, onClick = onEdit)
            MiniButton(label = texts.delete, danger = true, onClick = onDelete)
        }
    }
}

// ── Mənbə ─────────────────────────────────────────────────────────────────────

@Composable
private fun SourceSection(
    texts: AppTexts,
    selectedTab: SourceTab,
    onTabChange: (SourceTab) -> Unit,
    isEditing: Boolean,
    editingName: String,
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
    isWide: Boolean,
    isLoading: Boolean,
    canLoad: Boolean,
    loadState: LoadState,
    onLoad: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BlockLabel(texts.chooseSource)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OptionChip(
                label = texts.sourceM3u,
                subtitle = texts.m3uSubtitle,
                selected = selectedTab == SourceTab.M3U,
                onClick = { onTabChange(SourceTab.M3U) },
                modifier = Modifier.weight(1f)
            )
            OptionChip(
                label = texts.sourceXtream,
                subtitle = texts.xtreamSubtitle,
                selected = selectedTab == SourceTab.XTREAM,
                onClick = { onTabChange(SourceTab.XTREAM) },
                modifier = Modifier.weight(1f)
            )
        }

        BlockLabel(texts.connectionDetails)
        MenuPanel(modifier = Modifier.fillMaxWidth()) {
            if (isEditing && editingName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Accent.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = texts.edit.uppercase(),
                            color = Accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = editingName,
                        color = MenuTextBody,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    MiniButton(label = texts.addPlaylist, onClick = onReset)
                }
                Spacer(Modifier.height(14.dp))
            }

            FieldLabel(texts.playlistName)
            Spacer(Modifier.height(6.dp))
            MenuField(value = playlistName, onValueChange = onPlaylistNameChange, placeholder = "Home TV")

            when (selectedTab) {
                SourceTab.M3U -> {
                    Spacer(Modifier.height(12.dp))
                    FieldLabel("M3U URL")
                    Spacer(Modifier.height(6.dp))
                    MenuField(
                        value = m3uUrl,
                        onValueChange = onM3uUrlChange,
                        placeholder = "http://server.com/playlist.m3u",
                        keyboardType = KeyboardType.Uri
                    )
                }

                SourceTab.XTREAM -> {
                    Spacer(Modifier.height(12.dp))
                    FieldLabel(texts.serverUrl)
                    Spacer(Modifier.height(6.dp))
                    MenuField(
                        value = xtreamServer,
                        onValueChange = onXtreamServerChange,
                        placeholder = "http://server.com:8080",
                        keyboardType = KeyboardType.Uri
                    )
                    Spacer(Modifier.height(12.dp))
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel(texts.username)
                                Spacer(Modifier.height(6.dp))
                                MenuField(
                                    value = xtreamUser,
                                    onValueChange = onXtreamUserChange,
                                    placeholder = "username"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel(texts.password)
                                Spacer(Modifier.height(6.dp))
                                MenuField(
                                    value = xtreamPass,
                                    onValueChange = onXtreamPassChange,
                                    placeholder = "password"
                                )
                            }
                        }
                    } else {
                        FieldLabel(texts.username)
                        Spacer(Modifier.height(6.dp))
                        MenuField(
                            value = xtreamUser,
                            onValueChange = onXtreamUserChange,
                            placeholder = "username"
                        )
                        Spacer(Modifier.height(12.dp))
                        FieldLabel(texts.password)
                        Spacer(Modifier.height(6.dp))
                        MenuField(
                            value = xtreamPass,
                            onValueChange = onXtreamPassChange,
                            placeholder = "password"
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    label = if (isLoading) texts.loading else texts.loadRefresh,
                    enabled = canLoad,
                    onClick = onLoad
                )
                StatusLine(loadState = loadState, texts = texts)
            }
        }

        Text(
            text = texts.playlistLimit,
            color = MenuTextDim,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MenuTextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        maxLines = 1
    )
}

@Composable
private fun MenuField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    var keyboardVisible by remember { mutableStateOf(false) }
    val accent = Accent

    fun hideKeyboard() {
        keyboardController?.hide()
        keyboardVisible = false
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
        cursorBrush = SolidColor(accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            showKeyboardOnFocus = false
        ),
        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    if (keyboardVisible) {
                        hideKeyboard()
                    } else {
                        keyboardController?.show()
                        keyboardVisible = true
                    }
                    true
                } else {
                    false
                }
            }
            .clip(MenuChipShape)
            .background(if (focused) Color(0xFF1E2735) else Color(0x99101720))
            .border(
                if (focused) 1.5.dp else 1.dp,
                if (focused) accent else MenuHairline,
                MenuChipShape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) keyboardVisible = false
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = if (focused) Color(0x99FFFFFF) else Color(0x66FFFFFF),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            inner()
        }
    )
}

@Composable
private fun StatusLine(loadState: LoadState, texts: AppTexts) {
    val (text, color) = when (loadState) {
        is LoadState.Loading -> texts.loadingChannels to MenuTextBody
        is LoadState.Success -> texts.channelsFound(loadState.count) to Color(0xFF6BE39B)
        is LoadState.Error -> "× ${loadState.message}" to Color(0xFFFF7A7A)
        else -> "" to MenuTextDim
    }
    if (text.isBlank()) return
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Oynatma ───────────────────────────────────────────────────────────────────

@Composable
private fun PlaybackSection(
    texts: AppTexts,
    audioDecoderMode: AudioDecoderMode,
    settings: PlaybackSettings,
    showFps: Boolean,
    onAudioMode: (AudioDecoderMode) -> Unit,
    onShowFps: (Boolean) -> Unit,
    onFrameRateMatching: (Boolean) -> Unit,
    onRawAudioConvert: (Boolean) -> Unit,
    onTunneledPlayback: (Boolean) -> Unit,
    onFix1080i: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BlockLabel(texts.audioDecoder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptionChip(
                label = texts.auto,
                subtitle = "AUTO",
                selected = audioDecoderMode == AudioDecoderMode.AUTO,
                onClick = { onAudioMode(AudioDecoderMode.AUTO) },
                modifier = Modifier.weight(1f)
            )
            OptionChip(
                label = texts.hardware,
                subtitle = "HW",
                selected = audioDecoderMode == AudioDecoderMode.HARDWARE,
                onClick = { onAudioMode(AudioDecoderMode.HARDWARE) },
                modifier = Modifier.weight(1f)
            )
            OptionChip(
                label = texts.software,
                subtitle = "SW",
                selected = audioDecoderMode == AudioDecoderMode.SOFTWARE,
                onClick = { onAudioMode(AudioDecoderMode.SOFTWARE) },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = when (audioDecoderMode) {
                AudioDecoderMode.AUTO -> texts.autoHint
                AudioDecoderMode.HARDWARE -> texts.hwHint
                AudioDecoderMode.SOFTWARE -> texts.swHint
            },
            color = MenuTextDim,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Box(Modifier.fillMaxWidth().height(1.dp).background(MenuHairline))

        BlockLabel(texts.playbackSettings)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow(
                label = texts.showFps,
                hint = texts.showFpsHint,
                checked = showFps,
                onLabel = texts.on,
                offLabel = texts.off,
                onToggle = { onShowFps(!showFps) }
            )
            ToggleRow(
                label = texts.frameRateMatching,
                hint = texts.frameRateMatchingHint,
                checked = settings.frameRateMatching,
                onLabel = texts.on,
                offLabel = texts.off,
                onToggle = { onFrameRateMatching(!settings.frameRateMatching) }
            )
            ToggleRow(
                label = texts.rawAudioConvert,
                hint = texts.rawAudioConvertHint,
                checked = settings.rawAudioConvert,
                onLabel = texts.on,
                offLabel = texts.off,
                onToggle = { onRawAudioConvert(!settings.rawAudioConvert) }
            )
            ToggleRow(
                label = texts.tunneledPlayback,
                hint = texts.tunneledPlaybackHint,
                checked = settings.tunneledPlayback,
                onLabel = texts.on,
                offLabel = texts.off,
                onToggle = { onTunneledPlayback(!settings.tunneledPlayback) }
            )
            ToggleRow(
                label = texts.fix1080i,
                hint = texts.fix1080iHint,
                checked = settings.fix1080i,
                onLabel = texts.on,
                offLabel = texts.off,
                onToggle = { onFix1080i(!settings.fix1080i) }
            )
        }
    }
}

// ── Görünüş ───────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    texts: AppTexts,
    language: String,
    themeMode: String,
    onLanguage: (AppLanguage) -> Unit,
    onTheme: (AppThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BlockLabel(texts.language)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptionChip(
                label = texts.az,
                subtitle = "AZ",
                selected = language == AppLanguage.AZ.name,
                onClick = { onLanguage(AppLanguage.AZ) },
                modifier = Modifier.weight(1f)
            )
            OptionChip(
                label = texts.en,
                subtitle = "EN",
                selected = language == AppLanguage.EN.name,
                onClick = { onLanguage(AppLanguage.EN) },
                modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(MenuHairline))

        BlockLabel(texts.theme)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OptionChip(
                label = texts.themeClassic,
                subtitle = texts.themeClassicHint,
                selected = themeMode == AppThemeMode.CLASSIC.name,
                onClick = { onTheme(AppThemeMode.CLASSIC) },
                modifier = Modifier.weight(1f)
            )
            OptionChip(
                label = texts.themeDrmPlay,
                subtitle = texts.themeDrmPlayHint,
                selected = themeMode == AppThemeMode.DRM_PLAY.name,
                onClick = { onTheme(AppThemeMode.DRM_PLAY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Haqqında ──────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(
    texts: AppTexts,
    playlistCount: Int,
    activeName: String
) {
    Column(
        modifier = Modifier.widthIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MenuPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FPlayerLogo(size = 58)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "FTV LIVE TV",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${texts.version} ${BuildConfig.VERSION_NAME}",
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoTile(
                label = texts.savedPlaylists,
                value = "$playlistCount / ${AppPreferences.MAX_PLAYLISTS}",
                modifier = Modifier.weight(1f)
            )
            InfoTile(
                label = texts.active,
                value = activeName.ifBlank { texts.noInfo },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = texts.playlistLimit,
            color = MenuTextDim,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MenuCardShape)
            .background(MenuSurface)
            .border(1.dp, MenuHairline, MenuCardShape)
            .heightIn(min = 62.dp)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = MenuTextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
