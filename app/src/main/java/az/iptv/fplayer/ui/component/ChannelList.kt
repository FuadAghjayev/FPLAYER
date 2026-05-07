package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.math.roundToInt

@Composable
fun ChannelList(
    channels: List<Channel>,
    currentChannel: Channel?,
    currentFrameRate: Float = 0f,
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
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        itemsIndexed(channels, key = { _, ch -> ch.stableKey }) { index, channel ->
            val isPlaying = currentChannel?.stableKey == channel.stableKey
            ChannelItem(
                channel = channel,
                index = index + 1,
                isPlaying = isPlaying,
                isFocused = index == focusedIndex,
                liveFrameRate = if (isPlaying) currentFrameRate else 0f,
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
    liveFrameRate: Float,
    onClick: () -> Unit
) {
    val bg = when {
        isPlaying && isFocused -> Color(0x99BF6B00)
        isPlaying -> Color(0x661A1200)
        isFocused -> Color(0x770D1B2A)
        index % 2 == 0 -> Color(0x330B1118)
        else -> Color(0x22060606)
    }
    val borderColor = when {
        isPlaying -> Accent.copy(alpha = 0.42f)
        isFocused -> Color(0xFF4A90D9).copy(alpha = 0.42f)
        else -> Color(0x16FFFFFF)
    }
    val fpsLabel = formatFrameRate(liveFrameRate.takeIf { it > 0f } ?: channel.frameRate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    when {
                        isPlaying -> Brush.verticalGradient(listOf(Accent, Accent.copy(0.4f)))
                        isFocused -> Brush.verticalGradient(listOf(Color(0xFF4A90D9), Color(0xFF1A4A80)))
                        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        )

        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(
                    when {
                        isPlaying -> Color(0x33FF8C00)
                        isFocused -> Color(0x18FFFFFF)
                        else -> Color(0x08FFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%d", index),
                color = when {
                    isPlaying -> Accent
                    isFocused -> Color(0xFF7EB8E8)
                    else -> Color(0xFF4A4A4A)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = channel.name,
            color = when {
                isPlaying -> Color.White
                isFocused -> Color(0xFFD0E8FF)
                else -> Color(0xFF888888)
            },
            fontSize = 13.sp,
            fontWeight = when {
                isPlaying -> FontWeight.SemiBold
                isFocused -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )

        if (fpsLabel.isNotEmpty()) {
            ReceiverBadge(
                text = fpsLabel,
                isActive = isPlaying || isFocused,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Accent.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ">",
                    color = Accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (isFocused) {
            Text(
                text = ">",
                color = Color(0xFF4A90D9),
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
}

@Composable
private fun ReceiverBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) Color(0x24FF8C00) else Color(0x14FFFFFF))
            .border(
                1.dp,
                if (isActive) Accent.copy(alpha = 0.32f) else Color(0x22FFFFFF),
                RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Accent else Color(0xFF777777),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun formatFrameRate(frameRate: Float): String =
    if (frameRate > 0f) "${frameRate.roundToInt()}fps" else ""
