package az.iptv.fplayer.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import az.iptv.fplayer.ui.theme.TextSecondary
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
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        channel ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color(0xCC000000),
                        1f to Color(0xFF000000)
                    )
                )
                .padding(horizontal = 56.dp, vertical = 0.dp)
                .padding(top = 48.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sol accent çubuğu
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(listOf(Accent, Accent.copy(alpha = 0.3f)))
                        )
                )

                // Kanal loqosu
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    } else {
                        Text(
                            text = "TV",
                            color = Accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Kanal məlumatları
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Nömrə + ad
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = String.format("%03d", channelIndex),
                            color = Accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 26.sp
                        )
                    }

                    // Qrup + texniki məlumat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (channel.group.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x30FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = channel.group,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        if (videoInfo.label.isNotEmpty()) TechBadge(videoInfo.label)
                        if (videoInfo.fpsLabel.isNotEmpty()) TechBadge(videoInfo.fpsLabel)
                        if (videoInfo.codec.isNotEmpty()) TechBadge(videoInfo.codec.uppercase())
                    }
                }

                // Sağ: texniki info + sayğac
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (videoInfo.width > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x20FF8C00))
                                .border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${videoInfo.width}×${videoInfo.height}",
                                color = Accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "$channelIndex / $totalChannels",
                        color = Color(0xFF555555),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "FPLAYER",
                        color = Accent.copy(alpha = 0.35f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun TechBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color(0xFFCCCCCC), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MiniBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChannelLogo(logoUrl: String, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BadgeBg),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        } else {
            Text("TV", color = Accent, fontSize = (size / 3).sp, fontWeight = FontWeight.Bold)
        }
    }
}
