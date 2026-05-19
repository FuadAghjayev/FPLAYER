package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    LaunchedEffect(focusedIndex, channels.size) {
        if (focusedIndex in channels.indices) {
            listState.animateScrollToItem(maxOf(0, focusedIndex - 2))
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
    val background = when {
        isFocused -> Color(0xFFE6E7EA)
        isPlaying -> Color(0x441F252B)
        else -> Color.Transparent
    }
    val primaryText = if (isFocused) Color(0xFF111417) else Color.White
    val secondaryText = if (isFocused) Color(0xFF2E3135) else Accent
    val progressColor = if (isFocused) Color(0xFF202226) else Color(0xFFE4E5E8)
    val fpsLabel = formatFrameRate(liveFrameRate.takeIf { it > 0f } ?: channel.frameRate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(channel.logoUrl, size = 72)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index",
                    color = primaryText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    text = channel.name,
                    color = primaryText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fpsLabel.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    ReceiverBadge(text = fpsLabel.uppercase(), active = isFocused || isPlaying)
                }
            }
            Text(
                text = "Program",
                color = secondaryText,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x66D9DCE0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f)
                        .background(progressColor)
                )
            }
        }

        if (isFocused) {
            Text("▶", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiverBadge(text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Color(0x33111111) else Color(0x24FFFFFF))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (active) Color(0xFF111417) else Color(0xFFCED3D8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun formatFrameRate(frameRate: Float): String =
    if (frameRate > 0f) "${frameRate.roundToInt()} FPS" else ""
