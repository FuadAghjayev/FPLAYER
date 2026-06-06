package az.iptv.fplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.player.PlaybackState
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.ui.theme.BadgeBg
import coil.compose.AsyncImage

@Composable
fun ChannelInfoOsd(
    visible: Boolean,
    channel: Channel?,
    videoInfo: VideoInfo,
    playbackState: PlaybackState,
    channelIndex: Int,
    totalChannels: Int,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = 26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xB6080B10))
                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFFFF6C58))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(channel.logoUrl, size = 70)

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .width(92.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = channelIndex.toString().padStart(4, '0'),
                        color = Color(0xFFFF6C58),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "/ ${totalChannels.coerceAtLeast(0)}",
                        color = Color(0xFFC9CED4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0x26FFFFFF))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp, end = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 26.sp,
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
                            text = channel.group.ifBlank { "All channels" },
                            color = Color(0xFFD2D8DE),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OsdProgressLine(progress = if (isLive) 0.62f else 0f, modifier = Modifier.width(150.dp))
                        Text(
                            text = if (isLive) "Canli yayin" else "No signal",
                            color = Color(0xFFB9C1CA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OsdInfoPill(qualityLabel)
                        OsdInfoPill(fps)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OsdInfoPill(codec)
                        OsdInfoPill(resolution)
                    }
                    Text(
                        text = if (isLive) "LIVE" else "OFF",
                        color = if (isLive) Color(0xFF6DFF9D) else Color(0xFFFF8585),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatusBadge(isLive: Boolean) {
    val bg = if (isLive) Color(0xE018A957) else Color(0xE0C52F34)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isLive) "LIVE" else "OFF",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun OsdInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFEAF2FA),
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
            .background(Color(0x40FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color(0xFFFF6C58))
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
fun ChannelLogo(logoUrl: String, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 10).dp))
            .background(BadgeBg.copy(alpha = 0.72f)),
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
                color = Color(0xFF1C2025),
                fontSize = (size * 0.36f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

