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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x55000000),
                        0.46f to Color.Transparent,
                        0.72f to Color(0xAA000000),
                        1f to Color(0xED000000)
                    )
                )
                .padding(horizontal = 32.dp, vertical = 26.dp)
        ) {
            Text(
                text = "Playlist 1  •  ${channel.group.ifBlank { "All channels" }}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = "Wed, Apr 17, 12:25 PM",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 92.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    ChannelLogo(channel.logoUrl, size = 116)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Program",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text("11:58 - 12:51 PM", color = Color(0xFFC7C9CD), fontSize = 23.sp)
                            ProgramProgress(width = 82.dp, progress = 0.35f)
                            Text("26 min", color = Color(0xFFC7C9CD), fontSize = 23.sp)
                            Text(
                                "$channelIndex  ${channel.name}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(260.dp)
                            )
                            if (videoInfo.label.isNotEmpty()) TechBadge(videoInfo.label)
                            val fps = videoInfo.fpsLabel.ifBlank {
                                channel.frameRate.takeIf { it > 0f }?.let { "${it.toInt()} FPS" } ?: ""
                            }
                            if (fps.isNotEmpty()) TechBadge(fps.uppercase())
                            if (videoInfo.codec.isNotEmpty()) TechBadge(videoInfo.codec.uppercase())
                        }
                        Text(
                            text = "12:51 - 01:51 PM   Program",
                            color = Color(0xFFAEB0B5),
                            fontSize = 23.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .fillMaxWidth()
                        .height(5.dp)
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

                Text(
                    text = "$channelIndex / $totalChannels",
                    color = Color(0xFF9B9DA2),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 48.dp, top = 12.dp)
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
        Text(text = text, color = Color(0xFF111317), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
