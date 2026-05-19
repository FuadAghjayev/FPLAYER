package az.iptv.fplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.BadgeBg
import coil.compose.AsyncImage

@Composable
fun ChannelInfoOsd(
    visible: Boolean,
    channel: Channel?,
    videoInfo: VideoInfo,
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
        @Suppress("UNUSED_VARIABLE")
        val ignoredTotalChannels = totalChannels

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.24f to Color(0x8A000000),
                        1f to Color(0xEA000000)
                    )
                )
                .padding(start = 68.dp, end = 68.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ChannelLogo(channel.logoUrl, size = 72)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$channelIndex  ${channel.name}",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Program", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("11:58 - 12:51 PM", color = Color(0xFFC7C9CD), fontSize = 16.sp)
                        ProgramProgress(width = 62.dp, progress = 0.35f)
                        Text("26 min", color = Color(0xFFC7C9CD), fontSize = 16.sp)
                        if (videoInfo.label.isNotEmpty()) TechBadge(videoInfo.label)
                        if (videoInfo.width > 0 && videoInfo.height > 0) {
                            TechBadge("${videoInfo.width}x${videoInfo.height}")
                        }
                        val fps = formatFrameRate(videoInfo.frameRate.takeIf { it > 0f } ?: channel.frameRate)
                        if (fps.isNotEmpty()) TechBadge(fps)
                        if (videoInfo.codec.isNotEmpty()) TechBadge(videoInfo.codec.uppercase())
                    }
                    Text(
                        text = "12:51 - 01:51 PM   Program",
                        color = Color(0xFFAEB0B5),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x66FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f)
                        .background(Accent)
                )
            }
        }
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

@Composable
private fun ProgramProgress(width: androidx.compose.ui.unit.Dp, progress: Float) {
    Box(
        modifier = Modifier
            .width(width)
            .height(5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x66FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color(0xFFDADDE1))
        )
    }
}

private fun formatFrameRate(frameRate: Float): String =
    if (frameRate > 0f) "${frameRate.toInt()} FPS" else ""
