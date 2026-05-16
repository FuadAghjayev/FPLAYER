package az.iptv.fplayer.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.CardBg
import az.iptv.fplayer.ui.theme.FocusBorder
import az.iptv.fplayer.ui.theme.PanelBg
import az.iptv.fplayer.ui.theme.TextSecondary
import az.iptv.fplayer.ui.theme.WarmAccent
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.first

private enum class SourceTab { M3U, XTREAM }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    onPlaylistLoaded: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val loadState by vm.loadState.collectAsState()
    val playerType by vm.playerType.collectAsState()

    var selectedTab by remember { mutableStateOf(SourceTab.M3U) }
    var m3uUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    LaunchedEffect(loadState) {
        if (loadState is LoadState.Success) onPlaylistLoaded()
    }

    LaunchedEffect(Unit) {
        vm.resetForPlaylistEdit()   // Bug fix: geri qayıdanda avtomatik naviqasiyanın qarşısını al
        m3uUrl = vm.prefs.m3uUrl.first()
        xtreamServer = vm.prefs.xtreamServer.first()
        xtreamUser = vm.prefs.xtreamUser.first()
        xtreamPass = vm.prefs.xtreamPass.first()
        val savedType = vm.prefs.playlistType.first()
        if (savedType == "XTREAM" && xtreamServer.isNotEmpty()) selectedTab = SourceTab.XTREAM
        // vm.autoLoadSavedPlaylist()  <- Bunu sildik ki, loop yaratmasın
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
            // Left branding panel — sadece geniş ekranlarda (TV / tablet)
            if (isWide) {
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
                    Text(
                        text = "LIVE TV",
                        color = WarmAccent.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.width(42.dp).height(2.dp).background(Accent))
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "M3U pleylist və Xtream\nCodes dəstəyi",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0x22FFFFFF)))
            }

            // Right scrollable panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)  // scroll həm toxunuş, həm D-pad ilə
                    .padding(
                        horizontal = if (isWide) 56.dp else 24.dp,
                        vertical = if (isWide) 40.dp else 28.dp
                    ),
                verticalArrangement = Arrangement.Top
            ) {
                // Dar ekranlarda (telefon) yuxarıda kompakt branding
                if (!isWide) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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

                Text(
                    text = "Kanal mənbəyi",
                    color = Color.White,
                    fontSize = if (isWide) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Mənbə seçin, kanalları yükləyin və sonra istədiyiniz vaxt yeniləyin",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(24.dp))

                // Mənbə seçim kartları
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SourceCard(
                        icon = "M3U",
                        title = "M3U Pleylist",
                        subtitle = "URL linkdən yüklə",
                        selected = selectedTab == SourceTab.M3U,
                        onClick = { selectedTab = SourceTab.M3U },
                        modifier = Modifier.weight(1f)
                    )
                    SourceCard(
                        icon = "XC",
                        title = "Xtream Codes",
                        subtitle = "Server, istifadəçi, şifrə",
                        selected = selectedTab == SourceTab.XTREAM,
                        onClick = { selectedTab = SourceTab.XTREAM },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        SourceTab.M3U -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FormLabel("M3U URL")
                            FormField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                placeholder = "http://server.com/playlist.m3u",
                                keyboardType = KeyboardType.Uri
                            )
                        }

                        SourceTab.XTREAM -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FormLabel("Server URL")
                            FormField(
                                value = xtreamServer,
                                onValueChange = { xtreamServer = it },
                                placeholder = "http://server.com:8080",
                                keyboardType = KeyboardType.Uri
                            )
                            Spacer(Modifier.height(4.dp))
                            if (isWide) {
                                // TV / tablet: yan yana
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FormLabel("İstifadəçi adı")
                                        FormField(value = xtreamUser, onValueChange = { xtreamUser = it }, placeholder = "username")
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FormLabel("Şifrə")
                                        FormField(value = xtreamPass, onValueChange = { xtreamPass = it }, placeholder = "password")
                                    }
                                }
                            } else {
                                // Telefon: alt alta
                                FormLabel("İstifadəçi adı")
                                FormField(value = xtreamUser, onValueChange = { xtreamUser = it }, placeholder = "username")
                                Spacer(Modifier.height(4.dp))
                                FormLabel("Şifrə")
                                FormField(value = xtreamPass, onValueChange = { xtreamPass = it }, placeholder = "password")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                val isLoading = loadState is LoadState.Loading
                val canLoad = when (selectedTab) {
                    SourceTab.M3U -> m3uUrl.isNotBlank() && !isLoading
                    SourceTab.XTREAM -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank() && !isLoading
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LoadButton(
                        label = if (isLoading) "Yüklənir..." else "Yüklə / Yenilə",
                        enabled = canLoad,
                        onClick = {
                            when (selectedTab) {
                                SourceTab.M3U -> vm.loadM3u(m3uUrl.trim())
                                SourceTab.XTREAM -> vm.loadXtream(xtreamServer.trim(), xtreamUser.trim(), xtreamPass.trim())
                            }
                        }
                    )
                    when (val state = loadState) {
                        is LoadState.Loading -> StatusText("Kanallar yüklənir...", TextSecondary)
                        is LoadState.Success -> StatusText("✓ ${state.count} kanal tapıldı", Color(0xFF44FF88))
                        is LoadState.Error -> StatusText("✗ ${state.message}", Color(0xFFFF4444))
                        else -> {}
                    }
                }

                Spacer(Modifier.height(32.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "VİDEO OYNADICI",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerChip(
                        label = "ExoPlayer",
                        subtitle = "Tövsiyə edilir",
                        selected = playerType == PlayerType.EXOPLAYER,
                        onClick = { vm.setPlayerType(PlayerType.EXOPLAYER) }
                    )
                    PlayerChip(
                        label = "VLC",
                        subtitle = "Manual quraşdırma lazım",
                        selected = playerType == PlayerType.VLC,
                        onClick = { vm.setPlayerType(PlayerType.VLC) }
                    )
                }

                // Aşağı padding — telefonda naviqasiya çubuğunun üstündən keçməsin
                Spacer(Modifier.height(40.dp))
            }
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
    val borderColor = if (selected) FocusBorder else Color(0x2EFFFFFF)
    val bgColor = if (selected) Color(0xAA12383D) else CardBg

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
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
                    color = if (selected) Accent else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFFCCCCCC),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x6612252F))
            .border(1.dp, Color(0x2FFFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                androidx.compose.material3.Text(
                    text = placeholder,
                    color = Color(0x55FFFFFF),
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Accent else Accent.copy(alpha = 0.22f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 13.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 13.sp)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerChip(label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xAA12383D) else CardBg)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Accent.copy(alpha = 0.62f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (selected) Accent else Color.White,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
