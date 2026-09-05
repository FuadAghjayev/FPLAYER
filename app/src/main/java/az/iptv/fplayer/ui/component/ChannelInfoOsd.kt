package az.iptv.fplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import kotlin.math.roundToInt

// Starsat/klassik peyk qəbuledicisi infobar-ı: ekranın altında enli, iki mərtəbəli
// panel — üstdə kanal kimliyi, ortada cari proqram və gedişat, altda texniki nişanlar.
private val OsdBarShape = RoundedCornerShape(16.dp)
private val OsdChipShape = RoundedCornerShape(7.dp)
private val OsdTextDim = Color(0xFF9AA6B2)
private val OsdTextSoft = Color(0xFFD7DEE6)
private val OsdSurfaceTop = Color(0xF2131A24)
private val OsdSurfaceBottom = Color(0xF6070A10)
private val OsdInnerFill = Color(0x0FFFFFFF)
private val OsdHairline = Color(0x1AFFFFFF)
private val LiveRed = Color(0xFFFF4D5E)

private fun currentClockText(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun currentDateText(): String =
    SimpleDateFormat("dd MMM · EEE", Locale.getDefault()).format(Date())

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
            initialOffsetY = { it }
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            targetOffsetY = { it }
        ) + fadeOut(tween(170)),
        modifier = modifier
    ) {
        channel ?: return@AnimatedVisibility

        // Saat hər 20 saniyədən bir yenilənir; eyni zamanda proqram gedişatını da
        // təzələyən yeganə tetikleyicidir, ona görə ilk oxunan dəyər budur.
        val clock by produceState(initialValue = currentClockText()) {
            while (true) {
                value = currentClockText()
                delay(20_000)
            }
        }
        val today = currentDateText()

        val qualityLabel = videoInfo.label.ifBlank {
            if (channel.name.contains("HD", ignoreCase = true)) "HD" else "SD"
        }
        val resolution = if (videoInfo.width > 0 && videoInfo.height > 0) {
            "${videoInfo.width}×${videoInfo.height}"
        } else {
            "--"
        }
        val codec = videoInfo.codec.ifBlank { "--" }.uppercase()
        val fps = when {
            !showFps -> ""
            videoInfo.frameRate > 0f -> "${videoInfo.frameRate.roundToInt()} FPS"
            channel.frameRate > 0f -> "${channel.frameRate.roundToInt()} FPS"
            else -> ""
        }
        val isLive = playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering
        val isBuffering = playbackState is PlaybackState.Buffering

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
        val hasTrackPills = selectedAudioLabel != null || selectedSubtitleLabel != null
        val programProgress = programInfo?.progress()

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val barWidth = (maxWidth * 0.86f)
                .coerceIn(520.dp, 1120.dp)
                .coerceAtMost(maxWidth - 24.dp)

            Column(
                modifier = Modifier
                    .padding(bottom = 26.dp)
                    .width(barWidth)
                    .clip(OsdBarShape)
                    .background(
                        Brush.verticalGradient(listOf(OsdSurfaceTop, OsdSurfaceBottom))
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                        ),
                        OsdBarShape
                    )
            ) {
                // Qəbuledici infobar-ının tanınmış qızılı üst kənarı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Accent.copy(alpha = 0f),
                                    Accent,
                                    Accent.copy(alpha = 0.35f),
                                    Accent.copy(alpha = 0f)
                                )
                            )
                        )
                )

                // ── 1-ci mərtəbə: loqo · kanal nömrəsi · ad · saat/tarix ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 74.dp)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChannelLogo(
                        logoUrl = channel.logoUrl,
                        size = 52,
                        backgroundColor = OsdInnerFill,
                        borderColor = Color.White.copy(alpha = 0.16f),
                        placeholderColor = OsdTextSoft
                    )

                    ChannelNumberPlate(index = channelIndex, total = totalChannels)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            LiveStatusBadge(isLive = isLive, isBuffering = isBuffering)
                            Text(
                                text = channel.group.ifBlank { allChannelsLabel }.uppercase(),
                                color = OsdTextDim,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (channel.isFavorite) FavoriteStar()
                            RatingBadge(rating = channel.rating, compact = true)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = clock,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Text(
                            text = today.uppercase(),
                            color = OsdTextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                // ── 2-ci mərtəbə: cari proqram və gedişat çubuğu ──
                if (programInfo != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OsdInnerFill)
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            SectionTag(text = programLabel)
                            Text(
                                text = programInfo.title.ifBlank { "--" },
                                color = Color(0xFFF0F4F8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (programInfo.timeRange.isNotBlank()) {
                                Text(
                                    text = programInfo.timeRange,
                                    color = Accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                        if (programProgress != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                OsdProgressLine(
                                    progress = programProgress,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(programProgress * 100).roundToInt()}%",
                                    color = OsdTextSoft,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OsdHairline)
                )

                // ── 3-cü mərtəbə: texniki nişanlar və səs/subtitr seçimi ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OsdInfoPill(qualityLabel, highlight = true)
                    OsdInfoPill(codec)
                    OsdInfoPill(resolution)
                    if (fps.isNotBlank()) OsdInfoPill(fps)

                    Spacer(Modifier.weight(1f))

                    if (hasTrackPills) {
                        if (selectedAudioLabel != null) {
                            MediaTrackPill(
                                label = audioLabel,
                                value = selectedAudioLabel,
                                extraCount = extraAudioCount,
                                focused = focusedTrackOption == 0,
                                icon = {
                                    SpeakerIcon(
                                        color = if (focusedTrackOption == 0) Color(0xFF14161A) else Accent
                                    )
                                }
                            )
                        }
                        if (selectedSubtitleLabel != null) {
                            MediaTrackPill(
                                label = subtitlesLabel,
                                value = selectedSubtitleLabel,
                                extraCount = 0,
                                focused = focusedTrackOption == 1,
                                icon = {
                                    SubtitleIcon(
                                        color = if (focusedTrackOption == 1) Color(0xFF14161A) else Accent
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Qəbuledicidəki kimi çərçivəyə alınmış kanal nömrəsi. */
@Composable
private fun ChannelNumberPlate(index: Int, total: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Accent.copy(alpha = 0.20f), Accent.copy(alpha = 0.06f))
                )
            )
            .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
            .widthIn(min = 54.dp)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = index.coerceAtLeast(0).toString().padStart(3, '0'),
            color = Accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = "/ ${total.coerceAtLeast(0)}",
            color = OsdTextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Accent)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFF14161A),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun FavoriteStar() {
    Canvas(modifier = Modifier.size(12.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.44f
        val star = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.toRadians((i * 36.0) - 90.0)
            val x = cx + radius * kotlin.math.cos(angle).toFloat()
            val y = cy + radius * kotlin.math.sin(angle).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, Color(0xFFFFC247))
    }
}

@Composable
private fun LiveStatusBadge(isLive: Boolean, isBuffering: Boolean = false) {
    val label = when {
        isBuffering -> "SYNC"
        isLive -> "LIVE"
        else -> "OFF"
    }
    val tone = when {
        isBuffering -> Accent
        isLive -> LiveRed
        else -> Color(0xFF8A939C)
    }
    // Buferləmə zamanı nöqtə yanıb-sönür ki, donmuş kadr ilə fərqi görünsün
    val dotAlpha by animateFloatAsState(
        targetValue = if (isBuffering) 0.45f else 1f,
        animationSpec = tween(600),
        label = "osdLiveDot"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tone.copy(alpha = 0.16f))
            .border(1.dp, tone.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = dotAlpha))
        )
        Text(
            text = label,
            color = tone,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
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
            .clip(OsdChipShape)
            .background(if (focused) Color(0xFFE9EDF1) else OsdInnerFill)
            .border(
                if (focused) 1.5.dp else 1.dp,
                if (focused) Accent else Color.White.copy(alpha = 0.18f),
                OsdChipShape
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label.uppercase(),
                color = if (focused) Color(0x9914161A) else OsdTextDim,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = value,
                color = if (focused) Color(0xFF14161A) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Accent.copy(alpha = 0.18f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "+$extraCount",
                    color = Accent,
                    fontSize = 9.sp,
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
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CC",
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun SpeakerIcon(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
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
            .clip(OsdChipShape)
            .background(if (highlight) Accent.copy(alpha = 0.16f) else OsdInnerFill)
            .border(
                1.dp,
                if (highlight) Accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.16f),
                OsdChipShape
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) Accent else OsdTextSoft,
            fontSize = 10.sp,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdProgressLine(progress: Float, modifier: Modifier = Modifier) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "osdProgress"
    )
    Box(
        modifier = modifier
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFB020), Accent)
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
    val label = "IMDb " + String.format(Locale.US, "%.1f", rating)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 5.dp else 6.dp))
            .background(if (onDark) Color(0xFFF5C518).copy(alpha = 0.85f) else Color(0xFF14161A).copy(alpha = 0.7f))
            .border(
                1.dp,
                Color(0xFFF5C518).copy(alpha = if (onDark) 0.3f else 0.4f),
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
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
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
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
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
