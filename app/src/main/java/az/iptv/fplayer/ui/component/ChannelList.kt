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
    focusedIndex: Int,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex) {
        if (focusedIndex < 0 || focusedIndex >= channels.size) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val viewport = layoutInfo.viewportEndOffset
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
        when {
            itemInfo == null ->
                listState.animateScrollToItem(maxOf(0, focusedIndex - 3))
            itemInfo.offset + itemInfo.size > viewport - 8 ->
                listState.animateScrollToItem(focusedIndex)
            itemInfo.offset < 8 ->
                listState.animateScrollToItem(maxOf(0, focusedIndex - 3))
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
                isFocused = index == focusedIndex,
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
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isFocused && isPlaying -> Color(0x55FF8C00)
        isPlaying              -> Color(0x35FF8C00)
        isFocused              -> Color(0x40FFFFFF)
        else                   -> Color.Transparent
    }
    val nameColor = when {
        isPlaying -> Color.White
        isFocused -> Color.White
        else      -> Color(0xFFAAAAAA)
    }
    val barColor = when {
        isPlaying -> Accent
        isFocused -> Color(0xCCFFFFFF)
        else      -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF1C1C1C)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                )
            } else {
                Text(
                    text = "TV",
                    color = if (isPlaying) Accent else Color(0xFF555555),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index",
                    color = if (isPlaying) Accent.copy(alpha = 0.7f) else TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.widthIn(min = 22.dp)
                )
                Text(
                    text = channel.name,
                    color = nameColor,
                    fontSize = 13.sp,
                    fontWeight = if (isPlaying || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (channel.group.isNotEmpty()) {
                Text(
                    text = channel.group,
                    color = if (isPlaying) Accent.copy(alpha = 0.8f) else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
