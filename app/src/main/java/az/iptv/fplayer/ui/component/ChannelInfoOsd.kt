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

private val OsdCardShape = RoundedCornerShape(16.dp)
private val OsdTextDim = Color(0xFFAAB4BE)

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
            videoInfo.frameRate > 0f -> "${videoInfo.frameRate.toInt()} fps"
            channel.frameRate > 0f -> "${channel.frameRate.toInt()} fps"
            else -> "-- fps"
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
                        listOf(Color(0xF00C1016), Color(0xF6090C11))
                    )
                )
                .border(1.dp, Color(0x2EFFFFFF), OsdCardShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0x00FFC247), Accent, Color(0x00FFC247))
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
                        .width(1.dp)
                        .background(Color(0x1FFFFFFF))
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
                    modifier = Modifier.widthIn(min = 196.dp, max = 260.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OsdInfoPill(qualityLabel, highlight = true)
                        OsdInfoPill(fps)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            .clip(RoundedCornerShape(4.dp))
            .background(if (isLive) Color(0x2EFF4D5E) else Color(0x24FFFFFF))
            .border(
                1.dp,
                if (isLive) Color(0x66FF4D5E) else Color(0x33FFFFFF),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isLive) Color(0xFFFF4D5E) else Color(0xFF8A939C))
        )
        Text(
            text = if (isLive) "LIVE" else "OFF",
            color = if (isLive) Color(0xFFFFD7DB) else Color(0xFFC9D1D8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (progress != null) {
            OsdProgressLine(progress = progress, modifier = Modifier.width(150.dp))
        }
        Text(
            text = programText,
            color = Color(0xFFDDE4EA),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioTrackPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .widthIn(min = 190.dp, max = 260.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        SpeakerIcon(color = Accent)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label.uppercase(),
                color = OsdTextDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = ">",
            color = Accent,
            fontSize = 13.sp,
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
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlight) Color(0x2EFFC247) else Color(0x1AFFFFFF))
            .border(
                1.dp,
                if (highlight) Color(0x59FFC247) else Color(0x26FFFFFF),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (highlight) Color(0xFFFFDF9E) else Color(0xFFE6ECF1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x30FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
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
fun ChannelLogo(
    logoUrl: String,
    size: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x14FFFFFF),
    borderColor: Color = Color(0x24FFFFFF),
    placeholderColor: Color = Color(0xFFEAF0F5)
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 8).dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape((size / 8).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            )
        } else {
            Text(
                text = "TV",
                color = placeholderColor,
                fontSize = (size * 0.32f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
