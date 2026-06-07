package az.iptv.fplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import coil.compose.AsyncImage

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
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
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
            videoInfo.frameRate > 0f -> "${videoInfo.frameRate.toInt()}fps"
            channel.frameRate > 0f -> "${channel.frameRate.toInt()}fps"
            else -> "FPS --"
        }
        val isLive = playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering
        val selectedAudioLabel = mediaTracks.audioTracks
            .firstOrNull { it.selected }
            ?.label
            ?: mediaTracks.audioTracks.firstOrNull()?.label
        val hasMultipleAudioTracks = mediaTracks.audioTracks.size > 1
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, bottom = 22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xCC050505), Color(0xA60F1114), Color(0x4CFFFFFF))
                    )
                )
                .border(1.dp, Color(0x9AFFFFFF), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0x00FFFFFF), Color.White, Color(0x66FFFFFF), Color(0x00FFFFFF))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(
                    logoUrl = channel.logoUrl,
                    size = 58,
                    backgroundColor = Color(0x26000000),
                    borderColor = Color(0x44FFFFFF),
                    placeholderColor = Color.White
                )

                Column(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .width(84.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = channelIndex.toString().padStart(4, '0'),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "/ ${totalChannels.coerceAtLeast(0)}",
                        color = Color(0xFFD7D7D7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0x52FFFFFF))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 23.sp,
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
                            color = Color(0xFFE2E2E2),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
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
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OsdInfoPill(qualityLabel)
                        OsdInfoPill(fps)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OsdInfoPill(codec)
                        OsdInfoPill(resolution)
                    }
                    if (hasMultipleAudioTracks && selectedAudioLabel != null) {
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
    val border = if (isLive) Color.White else Color(0xB8FFFFFF)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (isLive) Brush.linearGradient(listOf(Color.White, Color(0xFFDDE3E8)))
                else Brush.linearGradient(listOf(Color(0xEE111111), Color(0xAA30343A)))
            )
            .border(1.dp, border, RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isLive) "LIVE" else "OFF",
            color = if (isLive) Color.Black else Color.White,
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
        .joinToString("  ")
        .ifBlank { programLabel }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (progress != null) {
            OsdProgressLine(progress = progress, modifier = Modifier.width(150.dp))
        }
        Text(
            text = programText,
            color = Color(0xFFE6E6E6),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioTrackPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.linearGradient(listOf(Color(0xF4FFFFFF), Color(0xD8E6EBF0))))
            .border(1.dp, Color.White, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SpeakerIcon(color = Color(0xFF080808))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label.uppercase(),
                color = Color(0xFF5B5B5B),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = value,
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = ">",
            color = Color.Black,
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
private fun OsdInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.linearGradient(listOf(Color(0xF2FFFFFF), Color(0xDDE7EBEF))))
            .border(1.dp, Color.White, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF050505),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color(0x42FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color.White)
        )
    }
}

@Composable
fun TechBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xBFD1D4D8))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = Color(0xFF111317), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniBadge(text: String) {
    TechBadge(text)
}

@Composable
fun ChannelLogo(
    logoUrl: String,
    size: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xE6F4F4F4),
    borderColor: Color = Color(0x66FFFFFF),
    placeholderColor: Color = Color(0xFF1C2025)
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 10).dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape((size / 10).dp)),
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
                color = placeholderColor,
                fontSize = (size * 0.36f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

