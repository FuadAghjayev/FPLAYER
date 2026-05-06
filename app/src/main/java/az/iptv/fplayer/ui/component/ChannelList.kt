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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.ui.theme.Accent

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
                listState.scrollToItem(maxOf(0, focusedIndex - 3))
            itemInfo.offset + itemInfo.size > viewport - 8 ->
                listState.scrollToItem(focusedIndex)
            itemInfo.offset < 8 ->
                listState.scrollToItem(maxOf(0, focusedIndex - 3))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 2.dp)
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
        isPlaying && isFocused -> Color(0xFFBF6B00)
        isPlaying              -> Color(0xFF1A1200)
        isFocused              -> Color(0xFF0D1B2A)
        index % 2 == 0         -> Color(0xFF0A0A0A)
        else                   -> Color(0xFF060606)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(bg)
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol rəng çubuğu
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    when {
                        isPlaying -> Brush.verticalGradient(listOf(Accent, Accent.copy(0.4f)))
                        isFocused -> Brush.verticalGradient(listOf(Color(0xFF4A90D9), Color(0xFF1A4A80)))
                        else      -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        )

        // Nömrə bloku
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .background(
                    when {
                        isPlaying -> Color(0x40FF8C00)
                        isFocused -> Color(0x25FFFFFF)
                        else      -> Color(0x0AFFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%d", index),
                color = when {
                    isPlaying -> Accent
                    isFocused -> Color(0xFF7EB8E8)
                    else      -> Color(0xFF4A4A4A)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Kanal adı
        Text(
            text = channel.name,
            color = when {
                isPlaying -> Color.White
                isFocused -> Color(0xFFD0E8FF)
                else      -> Color(0xFF888888)
            },
            fontSize = 13.sp,
            fontWeight = when {
                isPlaying -> FontWeight.SemiBold
                isFocused -> FontWeight.Medium
                else      -> FontWeight.Normal
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )

        // Oynatılır işarəsi
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Accent.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "▶",
                    color = Accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (isFocused) {
            Text(
                text = "›",
                color = Color(0xFF4A90D9),
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
}
