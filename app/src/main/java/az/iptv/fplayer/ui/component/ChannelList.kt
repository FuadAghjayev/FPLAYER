package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.TextSecondary
import coil.compose.AsyncImage

@Composable
fun ChannelList(
    channels: List<Channel>,
    currentChannel: Channel?,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentIndex = channels.indexOf(currentChannel)

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -100
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
            ChannelItem(
                channel = channel,
                index = index + 1,
                isPlaying = channel == currentChannel,
                onClick = { onChannelClick(channel) }
            )
        }
    }
}

@Composable
fun ChannelItem(
    channel: Channel,
    index: Int,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isPlaying) Color(0x25FF8C00) else Color.Transparent
    val nameColor = if (isPlaying) Color.White else Color(0xFFCCCCCC)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel logo / placeholder
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF1C1C1C)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
            } else {
                Text(
                    text = "TV",
                    color = Color(0xFF555555),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Channel number + name + program
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.widthIn(min = 22.dp)
                )
                Text(
                    text = channel.name,
                    color = nameColor,
                    fontSize = 13.sp,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (channel.group.isNotEmpty()) {
                Text(
                    text = channel.group,
                    color = if (isPlaying) Accent.copy(alpha = 0.9f) else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Oynayan kanal göstəricisi
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Accent)
            )
        } else {
            Spacer(Modifier.width(3.dp))
        }
    }
}
