package az.iptv.fplayer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import az.iptv.fplayer.player.AudioDecoderMode
import az.iptv.fplayer.player.PlayerType
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.CardBg
import az.iptv.fplayer.ui.theme.OsdBg
import az.iptv.fplayer.ui.theme.TextSecondary
import az.iptv.fplayer.viewmodel.LoadState
import az.iptv.fplayer.viewmodel.PlayerViewModel
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val playerType by vm.playerType.collectAsState()
    val audioDecoderMode by vm.audioDecoderMode.collectAsState()
    val loadState by vm.loadState.collectAsState()
    var m3uUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.prefs.m3uUrl.collect { m3uUrl = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OsdBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(560.dp)
                .align(Alignment.CenterStart)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "←",
                    color = Accent,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable(onClick = onBack)
                )
                Text(
                    text = "Настройки",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            SectionLabel("M3U Плейлист")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Accent),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (m3uUrl.isEmpty()) {
                            Text(
                                "http://example.com/playlist.m3u",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        inner()
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        label = "Загрузить",
                        enabled = m3uUrl.isNotBlank() && loadState !is LoadState.Loading,
                        onClick = { vm.loadM3u(m3uUrl) }
                    )
                    when (val state = loadState) {
                        is LoadState.Loading -> Text("Загрузка...", color = TextSecondary, fontSize = 12.sp)
                        is LoadState.Success -> Text("✓ ${state.count} каналов", color = Color(0xFF44FF88), fontSize = 12.sp)
                        is LoadState.Error -> Text("✗ ${state.message}", color = Color(0xFFFF4444), fontSize = 12.sp)
                        else -> {}
                    }
                }
            }

            SectionLabel("Видеоплеер")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerTypeChip(
                    label = "ExoPlayer",
                    subtitle = "Рекомендуется",
                    selected = playerType == PlayerType.EXOPLAYER,
                    onClick = { vm.setPlayerType(PlayerType.EXOPLAYER) }
                )
                PlayerTypeChip(
                    label = "VLC",
                    subtitle = "Больше форматов",
                    selected = playerType == PlayerType.VLC,
                    onClick = { vm.setPlayerType(PlayerType.VLC) }
                )
            }

            SectionLabel("Аудио декодер")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecoderChip(
                    label = "Авто",
                    subtitle = "Рекомендуется",
                    mode = AudioDecoderMode.AUTO,
                    current = audioDecoderMode,
                    onClick = { vm.setAudioDecoderMode(AudioDecoderMode.AUTO) }
                )
                DecoderChip(
                    label = "Аппаратный",
                    subtitle = "HW",
                    mode = AudioDecoderMode.HARDWARE,
                    current = audioDecoderMode,
                    onClick = { vm.setAudioDecoderMode(AudioDecoderMode.HARDWARE) }
                )
                DecoderChip(
                    label = "Программный",
                    subtitle = "SW",
                    mode = AudioDecoderMode.SOFTWARE,
                    current = audioDecoderMode,
                    onClick = { vm.setAudioDecoderMode(AudioDecoderMode.SOFTWARE) }
                )
            }
            Text(
                text = when (audioDecoderMode) {
                    AudioDecoderMode.AUTO     -> "Авто: аппаратный декодер, при ошибке — программный"
                    AudioDecoderMode.HARDWARE -> "HW: только аппаратный (MediaCodec). Если нет звука — попробуйте Авто/SW"
                    AudioDecoderMode.SOFTWARE -> "SW: предпочтительно программный декодер (помогает с RAW-каналами)"
                },
                color = TextSecondary,
                fontSize = 11.sp
            )

            SectionLabel("Версия")
            Text("FPLAYER v1.0  •  ExoPlayer + LibVLC", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) Accent else Color(0x55FF8C00))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DecoderChip(
    label: String,
    subtitle: String,
    mode: AudioDecoderMode,
    current: AudioDecoderMode,
    onClick: () -> Unit
) {
    val selected = mode == current
    val bg = if (selected) Color(0xFF0D2035) else CardBg
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (selected) Color(0xFF7EB8E8) else Color.White,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerTypeChip(label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Accent.copy(alpha = 0.2f) else CardBg
    val border = if (selected) Accent else Color(0x33FFFFFF)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (selected) Accent else Color.White,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
