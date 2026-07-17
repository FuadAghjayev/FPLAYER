package az.iptv.fplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ProgramInfo
import az.iptv.fplayer.player.MediaTracks
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.ui.theme.Accent
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OsdCardShape = RoundedCornerShape(16.dp)
private val OsdTextDim = Color(0xFFAAB4BE)

private fun currentClockText(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

@Composable
fun ChannelInfoOsd(
    visible: Boolean,
    channel: Channel?,
    videoInfo: VideoInfo,
    mediaTracks: MediaTracks = MediaTracks(),
    programInfo: ProgramInfo? = null,
    playbackState: PlaybackState,
    channelIndex: Int,
    totalChannels: Int,
    allChannelsLabel: String = "All channels",
    programLabel: String = "Program",
    audioLabel: String = "Audio",
    showFps: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && channel != null,
        enter = slideInVertically(
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 3 }
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            targetOffsetY = { it / 3 }
        ) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        channel ?: return@AnimatedVisibility
        val qualityLabel = videoInfo.label.ifBlank {
            if (channel.name.contains("HD", ignoreCase = true)) "HD" else "SD"
        }
        val resolution = if (videoInfo.width > 0 && videoInfo.height > 0) {
            "${videoInfo.width}x${videoInfo.height}"
        } else {
            "--"
        }
        val codec = videoInfo.codec.ifBlank { "--" }.uppercase()
        val fps = when {
            !showFps -> ""
            videoInfo.frameRate > 0f -> "${videoInfo.frameRate.toInt()} fps"
            channel.frameRate > 0f -> "${channel.frameRate.toInt()} fps"
            else -> ""
        }
        val clock by produceState(initialValue = currentClockText()) {
            while (true) {
                value = currentClockText()
                delay(20_000)
            }
        }
        val isLive = playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering
        val selectedAudioLabel = mediaTracks.audioTracks
            .firstOrNull { it.selected }
            ?.label
            ?: mediaTracks.audioTracks.firstOrNull()?.label

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 26.dp)
                .clip(OsdCardShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x1A0C1016).copy(alpha = 0.3f),
                            Color(0x1A090C11).copy(alpha = 0.25f)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0x4EFFFFFF), Color(0x15FFFFFF))
                    ),
                    OsdCardShape
                )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(
                    logoUrl = channel.logoUrl,
                    size = 64,
                    backgroundColor = Color(0x14FFFFFF),
                    borderColor = Color(0x24FFFFFF),
                    placeholderColor = Color(0xFFEAF0F5)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .width(78.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = channelIndex.coerceAtLeast(0).toString(),
                        color = Accent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "/ ${totalChannels.coerceAtLeast(0)}",
                        color = OsdTextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                        .background(Color(0xFFFFFF).copy(alpha = 0.15f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiveStatusBadge(isLive = isLive)
                        Text(
                            text = channel.group.ifBlank { allChannelsLabel },
                            color = OsdTextDim,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (programInfo != null) {
                        ProgramInfoLine(
                            program = programInfo,
                            programLabel = programLabel
                        )
                    }
                }

                Column(
                    modifier = Modifier.widthIn(min = 150.dp, max = 232.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clock,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(2.dp))
                        OsdInfoPill(qualityLabel, highlight = true)
                        if (fps.isNotBlank()) {
                            OsdInfoPill(fps)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RatingBadge(rating = channel.rating)
                        OsdInfoPill(codec)
                        OsdInfoPill(resolution)
                    }
                    if (selectedAudioLabel != null) {
                        AudioTrackPill(
                            label = audioLabel,
                            value = selectedAudioLabel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStatusBadge(isLive: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isLive -> Color(0xFF4D5E).copy(alpha = 0.15f)
                    else -> Color(0xFFFFFF).copy(alpha = 0.08f)
                }
            )
            .border(
                1.5.dp,
                if (isLive) Color(0xFFFF4D5E).copy(alpha = 0.4f) else Color(0xFFFFFF).copy(alpha = 0.2f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isLive) Color(0xFFFF4D5E) else Color(0xFF8A939C))
        )
        Text(
            text = if (isLive) "LIVE" else "OFF",
            color = if (isLive) Color(0xFFFFB8C2) else Color(0xFFC9D1D8),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgramInfoLine(program: ProgramInfo, programLabel: String) {
    val progress = program.progress()
    val programText = listOf(program.timeRange, program.title)
        .filter { it.isNotBlank() }
        .joinToString("   ")
        .ifBlank { programLabel }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (progress != null) {
            OsdProgressLine(progress = progress, modifier = Modifier.width(84.dp))
        }
        Text(
            text = programText,
            color = Color(0xFFDDE4EA),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AudioTrackPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFF).copy(alpha = 0.08f))
            .border(1.5.dp, Color(0xFFFFFF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpeakerIcon(color = Accent)
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label.uppercase(),
                color = OsdTextDim.copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        }
        Text(
            text = "›",
            color = Accent.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun SpeakerIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val body = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.40f)
            lineTo(size.width * 0.33f, size.height * 0.40f)
            lineTo(size.width * 0.58f, size.height * 0.22f)
            lineTo(size.width * 0.58f, size.height * 0.78f)
            lineTo(size.width * 0.33f, size.height * 0.60f)
            lineTo(size.width * 0.10f, size.height * 0.60f)
            close()
        }
        drawPath(body, color)
        drawArc(
            color = color,
            startAngle = -38f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(size.width * 0.54f, size.height * 0.28f),
            size = Size(size.width * 0.34f, size.height * 0.44f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun OsdInfoPill(text: String, highlight: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                when {
                    highlight -> Color(0xFFC247).copy(alpha = 0.12f)
                    else -> Color(0xFFFFFF).copy(alpha = 0.06f)
                }
            )
            .border(
                1.5.dp,
                if (highlight) Color(0xFFC247).copy(alpha = 0.35f) else Color(0xFFFFFF).copy(alpha = 0.18f),
                RoundedCornerShape(7.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) Color(0xFFFFE8B3) else Color(0xFFE6ECF1).copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFFF).copy(alpha = 0.12f))
            .border(0.5.dp, Color(0xFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFB020).copy(alpha = 0.9f), Accent.copy(alpha = 0.95f))
                    )
                )
        )
    }
}

@Composable
fun RatingBadge(
    rating: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDark: Boolean = true
) {
    if (rating <= 0f) return
    val label = "IMDb " + String.format(java.util.Locale.US, "%.1f", rating)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 5.dp else 6.dp))
            .background(if (onDark) Color(0xFFF5C518).copy(alpha = 0.85f) else Color(0xFF14161A).copy(alpha = 0.7f))
            .border(
                1.dp,
                if (onDark) Color(0xFFF5C518).copy(alpha = 0.3f) else Color(0xFFF5C518).copy(alpha = 0.4f),
                RoundedCornerShape(if (compact) 5.dp else 6.dp)
            )
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 2.dp else 3.dp)
    ) {
        Text(
            text = label,
            color = if (onDark) Color(0xFF14161A) else Color(0xFFF5C518),
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
fun ChannelPoster(
    posterUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFF).copy(alpha = 0.08f))
            .border(1.5.dp, Color(0xFFFFFF).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (posterUrl.isNotEmpty()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "?" },
                color = Color(0xFFEAF0F5).copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ChannelLogo(
    logoUrl: String,
    size: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFFFF).copy(alpha = 0.08f),
    borderColor: Color = Color(0xFFFFFF).copy(alpha = 0.2f),
    placeholderColor: Color = Color(0xFFEAF0F5)
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 7).dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape((size / 7).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Text(
                text = "TV",
                color = placeholderColor.copy(alpha = 0.9f),
                fontSize = (size * 0.32f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
