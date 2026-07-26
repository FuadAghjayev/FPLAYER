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

private val OsdCardShape = RoundedCornerShape(12.dp)
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
    subtitlesLabel: String = "Subtitles",
    subtitlesOffLabel: String = "Off",
    showFps: Boolean = true,
    focusedTrackOption: Int = -1,
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
        val extraAudioCount = (mediaTracks.audioTracks.size - 1).coerceAtLeast(0)
        val hasSubtitles = mediaTracks.subtitleTracks.isNotEmpty()
        val selectedSubtitleLabel = when {
            !hasSubtitles -> null
            !mediaTracks.subtitlesEnabled -> subtitlesOffLabel
            else -> mediaTracks.subtitleTracks.firstOrNull { it.selected }?.label
                ?: mediaTracks.subtitleTracks.firstOrNull()?.label
                ?: subtitlesOffLabel
        }

        val programProgress = programInfo?.progress()
        val hasTrackPills = selectedAudioLabel != null || selectedSubtitleLabel != null

        // Klassik peyk qəbuledicisindəki infobar ölçüsü: ekranın altında,
        // eni ekranın yarısından bir az çox, hündürlüyü iki sətirlik kompakt lövhə
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val cardWidth = (maxWidth * 0.56f).coerceIn(300.dp, 560.dp)

            Column(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .width(cardWidth)
                    .clip(OsdCardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0C1016).copy(alpha = 0.86f),
                                Color(0xFF06080C).copy(alpha = 0.82f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x52FFFFFF), Color(0x14FFFFFF))
                        ),
                        OsdCardShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x00FFC247), Accent.copy(alpha = 0.8f), Color(0x00FFC247))
                            )
                        )
                )

                // Üst sətir: loqo, kanal nömrəsi, ad və texniki nişanlar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelLogo(
                        logoUrl = channel.logoUrl,
                        size = 34,
                        backgroundColor = Color(0x14FFFFFF),
                        borderColor = Color(0x24FFFFFF),
                        placeholderColor = Color(0xFFEAF0F5)
                    )

                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = channelIndex.coerceAtLeast(0).toString(),
                            color = Accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Text(
                            text = "/${totalChannels.coerceAtLeast(0)}",
                            color = OsdTextDim,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(30.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiveStatusBadge(isLive = isLive)
                            Text(
                                text = channel.group.ifBlank { allChannelsLabel },
                                color = OsdTextDim,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = clock,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            OsdInfoPill(qualityLabel, highlight = true)
                            if (fps.isNotBlank()) {
                                OsdInfoPill(fps)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RatingBadge(rating = channel.rating, compact = true)
                            OsdInfoPill(codec)
                            OsdInfoPill(resolution)
                        }
                    }
                }

                // Alt sətir: cari proqram, gedişat çubuğu və səs/subtitr seçimi
                if (programInfo != null || hasTrackPills) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (programInfo != null) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = programInfo.title.ifBlank { programLabel },
                                        color = Color(0xFFDDE4EA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (programInfo.timeRange.isNotBlank()) {
                                        Text(
                                            text = programInfo.timeRange,
                                            color = OsdTextDim,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (programProgress != null) {
                                    OsdProgressLine(
                                        progress = programProgress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        if (hasTrackPills) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedAudioLabel != null) {
                                    MediaTrackPill(
                                        label = audioLabel,
                                        value = selectedAudioLabel,
                                        extraCount = extraAudioCount,
                                        focused = focusedTrackOption == 0,
                                        icon = { SpeakerIcon(color = if (focusedTrackOption == 0) Color(0xFF14161A) else Accent) }
                                    )
                                }
                                if (selectedSubtitleLabel != null) {
                                    MediaTrackPill(
                                        label = subtitlesLabel,
                                        value = selectedSubtitleLabel,
                                        extraCount = 0,
                                        focused = focusedTrackOption == 1,
                                        icon = { SubtitleIcon(color = if (focusedTrackOption == 1) Color(0xFF14161A) else Accent) }
                                    )
                                }
                            }
                        }
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
                1.dp,
                if (isLive) Color(0xFFFF4D5E).copy(alpha = 0.4f) else Color(0xFFFFFF).copy(alpha = 0.2f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isLive) Color(0xFFFF4D5E) else Color(0xFF8A939C))
        )
        Text(
            text = if (isLive) "LIVE" else "OFF",
            color = if (isLive) Color(0xFFFFB8C2) else Color(0xFFC9D1D8),
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MediaTrackPill(
    label: String,
    value: String,
    extraCount: Int = 0,
    focused: Boolean = false,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) Color(0xFFE9EDF1)
                else Color(0xFFFFFF).copy(alpha = 0.08f)
            )
            .border(
                if (focused) 1.5.dp else 1.dp,
                if (focused) Accent else Color(0xFFFFFF).copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label.uppercase(),
                color = if (focused) Color(0x9914161A) else OsdTextDim.copy(alpha = 0.8f),
                fontSize = 6.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = value,
                color = if (focused) Color(0xFF14161A) else Color.White.copy(alpha = 0.95f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 84.dp)
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Accent.copy(alpha = 0.16f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "+$extraCount",
                    color = Accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SubtitleIcon(color: Color) {
    // Sadə "CC" altyazı nişanı
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CC",
            color = color,
            fontSize = 6.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun SpeakerIcon(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
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
                1.dp,
                if (highlight) Color(0xFFC247).copy(alpha = 0.35f) else Color(0xFFFFFF).copy(alpha = 0.18f),
                RoundedCornerShape(7.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) Color(0xFFFFE8B3) else Color(0xFFE6ECF1).copy(alpha = 0.9f),
            fontSize = 8.sp,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFFFFF).copy(alpha = 0.12f))
            .border(0.5.dp, Color(0xFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(3.dp))
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
