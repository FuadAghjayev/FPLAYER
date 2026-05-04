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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.player.VideoInfo
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.BadgeBg
import az.iptv.fplayer.ui.theme.OsdBg
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
        
        // Receiver style Bottom Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6050505), Color(0xFF000000))
                    )
                )
                .padding(bottom = 24.dp, top = 40.dp, start = 32.dp, end = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Channel Logo
                ChannelLogo(logoUrl = channel.logoUrl, size = 64)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Channel Number and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = String.format("%03d", channelIndex),
                            color = Accent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Group and Stream Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = channel.group, color = TextSecondary, fontSize = 14.sp)
                        
                        // Technical Badges
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (videoInfo.label.isNotEmpty()) MiniBadge(videoInfo.label)
                            if (videoInfo.fpsLabel.isNotEmpty()) MiniBadge(videoInfo.fpsLabel)
                            if (videoInfo.codec.isNotEmpty()) MiniBadge(videoInfo.codec.uppercase())
                        }
                    }
                }

                // Clock or Date (Optional, can add later)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${videoInfo.width}×${videoInfo.height}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "FPLAYER",
                        color = Accent.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
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
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        } else {
            Text("TV", color = Accent, fontSize = (size / 3).sp, fontWeight = FontWeight.Bold)
        }
    }
}
