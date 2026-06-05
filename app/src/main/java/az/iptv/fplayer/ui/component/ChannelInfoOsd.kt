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
import androidx.compose.foundation.layout.Spacer
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
        val qualityLabel = videoInfo.label.ifBlank {
            if (channel.name.contains("HD", ignoreCase = true)) "HD" else "SD"
        }
        val resolution = if (videoInfo.width > 0 && videoInfo.height > 0) {
            "${videoInfo.width}x${videoInfo.height}"
        } else {
            "--"
        }
        val codec = videoInfo.codec.ifBlank { "--" }.uppercase()
        val channelPosition = if (totalChannels > 0) "$channelIndex/$totalChannels" else channelIndex.toString()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xE2070A0E),
                        0.72f to Color(0xF1121821),
                        1f to Color(0xF607090C)
                    )
                )
                .border(1.dp, Color(0x991AADB1), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF49BFFF), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channelPosition,
                            color = Color(0xFF9DEB88),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.width(96.dp)
                        )
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(9.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.group.ifBlank { "All channels" },
                            color = Color(0xFF6DE873),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(190.dp)
                        )
                        Text(
                            text = "Program",
                            color = Color(0xFFDCE5F1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(82.dp)
                        )
                        Text(
                            text = "No information",
                            color = Color(0xFF94A6B7),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "00:00",
                            color = Color(0xFFB7C4CF),
                            fontSize = 12.sp,
                            modifier = Modifier.width(48.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF17222C))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(5.dp)
                                    .background(Color(0xFF58D568))
                            )
                        }
                        Text(
                            text = "24:00",
                            color = Color(0xFFB7C4CF),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .width(48.dp)
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TechBadge(qualityLabel)
                        TechBadge(codec)
                    }
                    Text(
                        text = resolution,
                        color = Color(0xFFEAF2FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SignalValue(label = "S", value = "--")
                        SignalValue(label = "Q", value = "--")
                    }
                }
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
private fun SignalValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = Color(0xFF78D7FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = value,
            color = Color(0xFFEAF2FA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp)
        )
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

