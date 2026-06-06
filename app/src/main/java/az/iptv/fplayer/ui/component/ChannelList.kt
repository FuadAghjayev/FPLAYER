package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    currentFrameRate: Float = 0f,
    focusedIndex: Int,
    programLabel: String = "Program",
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(focusedIndex, channels.size) {
        if (focusedIndex in channels.indices) {
            listState.scrollToItem(maxOf(0, focusedIndex - 2))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        itemsIndexed(channels, key = { index, ch -> "${ch.stableKey}#$index" }) { index, channel ->
            val isPlaying = currentChannel?.stableKey == channel.stableKey
            ChannelItem(
                channel = channel,
                index = index + 1,
                isPlaying = isPlaying,
                isFocused = index == focusedIndex,
                programLabel = programLabel,
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
    programLabel: String = "Program",
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(channel.logoUrl, size = 48)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index",
                    color = primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = channel.name,
                    color = primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = programLabel,
                color = secondaryText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(2.dp)
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

        if (!isFocused && channel.isFavorite) {
            Text("Fav", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        if (isFocused) {
            Text("▶", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

