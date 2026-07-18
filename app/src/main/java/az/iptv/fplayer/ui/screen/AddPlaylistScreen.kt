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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.ui.text.AppTexts
import az.iptv.fplayer.ui.text.appTexts
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.AppBgEnd
import az.iptv.fplayer.ui.theme.AppBgMid
import az.iptv.fplayer.ui.theme.FocusBorder
import az.iptv.fplayer.ui.theme.PanelBg
import az.iptv.fplayer.ui.theme.TextSecondary
import az.iptv.fplayer.ui.theme.WarmAccent
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel

private enum class SourceTab { M3U, XTREAM }
private enum class WizardStep { SOURCE, DETAILS }

// Glassmorphism design tokens — matches ChannelInfoOsd premium style
private val GlassCardShape = RoundedCornerShape(14.dp)
private val GlassFieldShape = RoundedCornerShape(12.dp)
private val GlassCardBrush = Brush.verticalGradient(
    listOf(Color(0xFF141B25).copy(alpha = 0.72f), Color(0xFF0B0F16).copy(alpha = 0.62f))
)
private val GlassSelectedBrush = Brush.verticalGradient(
    listOf(Color(0xFF2E2517).copy(alpha = 0.88f), Color(0xFF1C1710).copy(alpha = 0.80f))
)
private val GlassBorderBrush = Brush.verticalGradient(
    listOf(Color(0x4DFFFFFF), Color(0x14FFFFFF))
)
private val GlassPanelBrush = Brush.verticalGradient(
    listOf(Color(0xFF121A23).copy(alpha = 0.94f), Color(0xFF0A0E14).copy(alpha = 0.90f))
)

@OptIn(ExperimentalTvMaterial3Api::class)
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
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()
    val playbackSettings by vm.playbackSettings.collectAsState()
    val showFps by vm.showFps.collectAsState()
    val t = appTexts(language)

    var selectedTab by remember { mutableStateOf(SourceTab.M3U) }
    var wizardStep by remember { mutableStateOf(WizardStep.SOURCE) }
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
        wizardStep = WizardStep.SOURCE
    }

    fun editProfile(profile: PlaylistProfile) {
        editingPlaylistId = profile.id
        playlistName = profile.name
        selectedTab = if (profile.type == PlaylistType.XTREAM) SourceTab.XTREAM else SourceTab.M3U
        m3uUrl = profile.m3uUrl
        xtreamServer = profile.xtreamServer
        xtreamUser = profile.xtreamUser
        xtreamPass = profile.xtreamPass
        wizardStep = WizardStep.DETAILS
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(AppBg, AppBgMid, AppBgEnd))
            )
    ) {
        val isWide = maxWidth > 600.dp
        val scrollState = rememberScrollState()

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                PlaylistLibraryPanel(
                    texts = t,
                    playlists = playlists,
                    activePlaylist = activePlaylist,
                    language = language,
                    canAddPlaylist = playlists.size < AppPreferences.MAX_PLAYLISTS,
                    canReturnToPlayer = onBackToPlayer != null && playlists.isNotEmpty(),
                    onBackToPlayer = { onBackToPlayer?.invoke() },
                    onAddPlaylist = { clearForm() },
                    onPlaylistClick = ::switchProfile,
                    onDeletePlaylist = ::deleteProfile
                )
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

                if (!isWide && playlists.isNotEmpty()) {
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
                                onClick = { switchProfile(profile) },
                                onDelete = { deleteProfile(profile) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                StepHeader(
                    step = wizardStep,
                    isEditing = editingPlaylistId != null,
                    texts = t
                )
                Spacer(Modifier.height(20.dp))

                val isLoading = loadState is LoadState.Loading
                val hasSlot = editingPlaylistId != null || playlists.size < AppPreferences.MAX_PLAYLISTS
                val detailsValid = when (selectedTab) {
                    SourceTab.M3U -> m3uUrl.isNotBlank()
                    SourceTab.XTREAM -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank()
                }
                val canLoad = hasSlot && !isLoading && detailsValid

                AnimatedContent(
                    targetState = wizardStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "wizard_step"
                ) { step ->
                    when (step) {
                        WizardStep.SOURCE -> Column {
                            SourceStepGrid(
                                selectedTab = selectedTab,
                                texts = t,
                                onSelect = { tab ->
                                    selectedTab = tab
                                    wizardStep = WizardStep.DETAILS
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                            if (!isWide) {
                                Text(t.playlistLimit, color = TextSecondary, fontSize = 12.sp)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        WizardStep.DETAILS -> Column {
                            SelectedSourceBar(
                                selectedTab = selectedTab,
                                texts = t,
                                onChangeSource = { wizardStep = WizardStep.SOURCE }
                            )
                            Spacer(Modifier.height(18.dp))
                            PlaylistForm(
                                tab = selectedTab,
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
                            Spacer(Modifier.height(22.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SmallActionButton(
                                    label = t.back,
                                    enabled = true,
                                    onClick = { wizardStep = WizardStep.SOURCE }
                                )
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
                            }
                            Spacer(Modifier.height(10.dp))
                            Status(loadState = loadState, texts = t)
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1CFFFFFF)))
                Spacer(Modifier.height(18.dp))

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

                Spacer(Modifier.height(26.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1CFFFFFF)))
                Spacer(Modifier.height(18.dp))

                SectionTitle(t.playbackSettings)
                Spacer(Modifier.height(10.dp))

                FormLabel(t.audioDecoder)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerChip(
                        label = t.auto,
                        subtitle = "AUTO",
                        selected = audioDecoderMode == AudioDecoderMode.AUTO,
                        onClick = { vm.setAudioDecoderMode(AudioDecoderMode.AUTO) }
                    )
                    PlayerChip(
                        label = t.hardware,
                        subtitle = "HW",
                        selected = audioDecoderMode == AudioDecoderMode.HARDWARE,
                        onClick = { vm.setAudioDecoderMode(AudioDecoderMode.HARDWARE) }
                    )
                    PlayerChip(
                        label = t.software,
                        subtitle = "SW",
                        selected = audioDecoderMode == AudioDecoderMode.SOFTWARE,
                        onClick = { vm.setAudioDecoderMode(AudioDecoderMode.SOFTWARE) }
                    )
                }
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        label = t.showFps,
                        hint = t.showFpsHint,
                        checked = showFps,
                        onLabel = t.on,
                        offLabel = t.off,
                        onToggle = { vm.setShowFps(!showFps) }
                    )
                    SettingToggleRow(
                        label = t.frameRateMatching,
                        hint = t.frameRateMatchingHint,
                        checked = playbackSettings.frameRateMatching,
                        onLabel = t.on,
                        offLabel = t.off,
                        onToggle = { vm.setFrameRateMatching(!playbackSettings.frameRateMatching) }
                    )
                    SettingToggleRow(
                        label = t.rawAudioConvert,
                        hint = t.rawAudioConvertHint,
                        checked = playbackSettings.rawAudioConvert,
                        onLabel = t.on,
                        offLabel = t.off,
                        onToggle = { vm.setRawAudioConvert(!playbackSettings.rawAudioConvert) }
                    )
                    SettingToggleRow(
                        label = t.tunneledPlayback,
                        hint = t.tunneledPlaybackHint,
                        checked = playbackSettings.tunneledPlayback,
                        onLabel = t.on,
                        offLabel = t.off,
                        onToggle = { vm.setTunneledPlayback(!playbackSettings.tunneledPlayback) }
                    )
                    SettingToggleRow(
                        label = t.fix1080i,
                        hint = t.fix1080iHint,
                        checked = playbackSettings.fix1080i,
                        onLabel = t.on,
                        offLabel = t.off,
                        onToggle = { vm.setFix1080i(!playbackSettings.fix1080i) }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StepHeader(step: WizardStep, isEditing: Boolean, texts: AppTexts) {
    val stepNumber = if (step == WizardStep.SOURCE) 1 else 2
    val title = when (step) {
        WizardStep.SOURCE -> texts.chooseSource
        WizardStep.DETAILS -> texts.connectionDetails
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepDot(active = true, done = stepNumber > 1)
            Box(
                Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(if (stepNumber > 1) Accent else Color(0x33FFFFFF))
            )
            StepDot(active = stepNumber >= 2, done = false)
            Spacer(Modifier.width(8.dp))
            Text(
                text = texts.stepLabel(stepNumber, 2),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.3.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(56.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Accent, Accent.copy(alpha = 0f))
                    )
                )
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (step == WizardStep.SOURCE) texts.welcomeSetup else texts.playlistSubtitle,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun StepDot(active: Boolean, done: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Accent else Color(0x33FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = if (done) "✓" else "",
            color = Color(0xFF081116),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SourceStepGrid(
    selectedTab: SourceTab,
    texts: AppTexts,
    onSelect: (SourceTab) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SourceCard(
            icon = "M3U",
            title = texts.sourceM3u,
            subtitle = texts.m3uSubtitle,
            selected = selectedTab == SourceTab.M3U,
            onClick = { onSelect(SourceTab.M3U) },
            modifier = Modifier.fillMaxWidth()
        )
        SourceCard(
            icon = "XC",
            title = texts.sourceXtream,
            subtitle = texts.xtreamSubtitle,
            selected = selectedTab == SourceTab.XTREAM,
            onClick = { onSelect(SourceTab.XTREAM) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SelectedSourceBar(
    selectedTab: SourceTab,
    texts: AppTexts,
    onChangeSource: () -> Unit
) {
    val title = if (selectedTab == SourceTab.XTREAM) texts.sourceXtream else texts.sourceM3u
    val icon = if (selectedTab == SourceTab.XTREAM) "XC" else "M3U"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassCardShape)
            .background(GlassSelectedBrush)
            .border(1.5.dp, Accent.copy(alpha = 0.55f), GlassCardShape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Accent.copy(alpha = 0.18f))
                .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(icon, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SmallActionButton(label = texts.edit, enabled = true, onClick = onChangeSource)
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
            text = "FTV",
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
private fun PlaylistLibraryPanel(
    texts: AppTexts,
    playlists: List<PlaylistProfile>,
    activePlaylist: PlaylistProfile?,
    language: String,
    canAddPlaylist: Boolean,
    canReturnToPlayer: Boolean,
    onBackToPlayer: () -> Unit,
    onAddPlaylist: () -> Unit,
    onPlaylistClick: (PlaylistProfile) -> Unit,
    onDeletePlaylist: (PlaylistProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .width(330.dp)
            .fillMaxHeight()
            .background(GlassPanelBrush)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FPlayerLogo(size = 58)
            Column {
                Text(
                    text = "FTV",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(text = "LIVE TV", color = WarmAccent.copy(alpha = 0.82f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallActionButton(label = texts.player, enabled = canReturnToPlayer, onClick = onBackToPlayer)
            SmallActionButton(label = texts.addPlaylist, enabled = canAddPlaylist, onClick = onAddPlaylist)
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle(texts.active)
        Spacer(Modifier.height(10.dp))
        ActivePlaylistCard(profile = activePlaylist, emptyLabel = texts.noInfo)

        Spacer(Modifier.height(24.dp))
        SectionTitle(texts.savedPlaylists)
        Spacer(Modifier.height(10.dp))
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(GlassCardShape)
                    .background(GlassCardBrush)
                    .border(1.dp, GlassBorderBrush, GlassCardShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = texts.noInfo, color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(playlists, key = { it.id }) { profile ->
                    PlaylistChip(
                        profile = profile,
                        selected = profile.id == activePlaylist?.id,
                        texts = texts,
                        deleteLabel = if (language == AppLanguage.EN.name) "Delete" else "Sil",
                        onClick = { onPlaylistClick(profile) },
                        onDelete = { onDeletePlaylist(profile) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActivePlaylistCard(profile: PlaylistProfile?, emptyLabel: String) {
    val typeLabel = when (profile?.type) {
        PlaylistType.XTREAM -> "Xtream"
        PlaylistType.M3U -> "M3U"
        null -> "--"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassCardShape)
            .background(GlassSelectedBrush)
            .border(1.5.dp, Accent.copy(alpha = 0.6f), GlassCardShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x00FFC247), Accent.copy(alpha = 0.8f), Color(0x00FFC247))
                    )
                )
        )
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = profile?.name ?: emptyLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = typeLabel,
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
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
        focused -> Brush.verticalGradient(listOf(Color(0xFFFFF6D8), Color(0xFFFFEDB8)))
        selected -> GlassSelectedBrush
        else -> GlassCardBrush
    }
    val border = when {
        focused -> SolidColor(Color(0xFFFFC247))
        selected -> SolidColor(Accent.copy(alpha = 0.75f))
        else -> GlassBorderBrush
    }
    val primaryText = if (focused) Color(0xFF071116) else Color.White
    val secondaryText = if (focused) Color(0xFF24333A) else if (selected) Accent else Color(0xFFDCE2E7)

    Box(
        modifier = modifier
            .clip(GlassCardShape)
            .background(bg)
            .border(if (focused) 2.dp else 1.dp, border, GlassCardShape)
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
    val logoShape = RoundedCornerShape((size / 4).dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(logoShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2230), Color(0xFF0B0F14))
                )
            )
            .border(1.dp, GlassBorderBrush, logoShape),
        contentAlignment = Alignment.Center
    ) {
        Text("F", color = Accent, fontSize = (size * 0.52f).sp, fontWeight = FontWeight.Black)
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
    val borderBrush = when {
        focused -> SolidColor(Color(0xFFFFC247))
        selected -> SolidColor(FocusBorder.copy(alpha = 0.8f))
        else -> GlassBorderBrush
    }
    val bgBrush = when {
        focused -> Brush.verticalGradient(listOf(Color(0xFFFFF6D8), Color(0xFFFFEDB8)))
        selected -> GlassSelectedBrush
        else -> GlassCardBrush
    }
    val titleColor = if (focused) Color(0xFF071116) else if (selected) Color.White else Color(0xFFF2F5F7)
    val subColor = if (focused) Color(0xFF2D3A42) else Color(0xFFD4DBE0)

    Box(
        modifier = modifier
            .clip(GlassCardShape)
            .background(bgBrush)
            .border(width = if (focused) 2.dp else 1.dp, brush = borderBrush, shape = GlassCardShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Accent.copy(alpha = 0.18f) else Color(0x18FFFFFF))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    icon,
                    color = if (focused) Color(0xFF5A3E00) else if (selected) Accent else Color(0xFFD4DBE0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
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
    var keyboardVisible by remember { mutableStateOf(false) }

    fun hideKeyboard() {
        keyboardController?.hide()
        keyboardVisible = false
    }

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
            .clip(GlassFieldShape)
            .background(
                if (focused) Brush.verticalGradient(
                    listOf(Color(0xFF2A2418).copy(alpha = 0.85f), Color(0xFF1B1710).copy(alpha = 0.75f))
                ) else GlassCardBrush
            )
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) SolidColor(Color(0xFFFFC247)) else GlassBorderBrush,
                GlassFieldShape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) keyboardVisible = false
            }
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
            .clip(GlassFieldShape)
            .background(
                when {
                    focused && enabled -> Brush.horizontalGradient(
                        listOf(Color(0xFFFFD166), Color(0xFFFFB020))
                    )
                    enabled -> Brush.horizontalGradient(
                        listOf(Accent, Color(0xFFFFB020))
                    )
                    else -> SolidColor(Accent.copy(alpha = 0.22f))
                }
            )
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Color.White else Color.Transparent,
                GlassFieldShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 13.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) Color(0xFF081116) else Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SmallActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(GlassFieldShape)
            .background(
                when {
                    focused && enabled -> Brush.horizontalGradient(
                        listOf(Color(0xFFFFD166), Color(0xFFFFB020))
                    )
                    enabled -> SolidColor(Color(0x33FFC247))
                    else -> SolidColor(Color(0x14FFFFFF))
                }
            )
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> SolidColor(Color.White)
                    enabled -> SolidColor(Accent.copy(alpha = 0.4f))
                    else -> GlassBorderBrush
                },
                GlassFieldShape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
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
private fun SettingToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onLabel: String,
    offLabel: String,
    onToggle: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = GlassCardShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    focused -> Brush.verticalGradient(listOf(Color(0xFFFFF6D8), Color(0xFFFFEDB8)))
                    checked -> GlassSelectedBrush
                    else -> GlassCardBrush
                }
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = when {
                    focused -> SolidColor(Color(0xFFFFC247))
                    checked -> SolidColor(Accent.copy(alpha = 0.7f))
                    else -> GlassBorderBrush
                },
                shape = shape
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
                color = if (focused) Color(0xFF071116) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hint,
                color = if (focused) Color(0xFF2D3A42) else Color(0xFFC3CBD3),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        checked -> Accent
                        focused -> Color(0x33071116)
                        else -> Color(0x22FFFFFF)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (checked) onLabel else offLabel,
                color = if (checked) Color(0xFF14161A) else if (focused) Color(0xFF071116) else Color(0xFFB9C2CB),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Brush.verticalGradient(listOf(Color(0xFFFFF6D8), Color(0xFFFFEDB8)))
        selected -> GlassSelectedBrush
        else -> GlassCardBrush
    }
    Box(
        modifier = Modifier
            .then(modifier)
            .clip(GlassFieldShape)
            .background(bg)
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = if (focused) SolidColor(Color(0xFFFFC247)) else if (selected) SolidColor(Accent.copy(alpha = 0.7f)) else GlassBorderBrush,
                shape = GlassFieldShape
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
