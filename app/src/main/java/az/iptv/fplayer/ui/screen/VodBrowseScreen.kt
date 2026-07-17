package az.iptv.fplayer.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.ui.component.ChannelPoster
import az.iptv.fplayer.ui.component.RatingBadge
import az.iptv.fplayer.ui.theme.Accent

/**
 * Netflix üslubunda kino/serial kataloqu. Açılanda heç bir yayın avtomatik başlamır —
 * istifadəçi kartı seçəndə oynatma başlayır. Naviqasiya PlayerScreen-dəki
 * onPreviewKeyEvent tərəfindən idarə olunur (focusedRow / focusedCol).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VodBrowseOverlay(
    title: String,
    rows: List<ChannelGroup>,
    currentChannel: Channel?,
    focusedRow: Int,
    focusedCol: Int,
    watchLabel: String,
    nowPlayingLabel: String,
    emptyLabel: String,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusedChannel = rows.getOrNull(focusedRow)?.channels?.getOrNull(focusedCol)
    val rowListState = rememberLazyListState()

    LaunchedEffect(focusedRow, rows.size) {
        if (focusedRow in rows.indices) {
            rowListState.animateScrollToItem(focusedRow)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0E13), Color(0xFF070A0E), Color(0xFF04060A))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VodHeroSection(
                sectionTitle = title,
                channel = focusedChannel,
                isPlaying = focusedChannel != null && focusedChannel.stableKey == currentChannel?.stableKey,
                watchLabel = watchLabel,
                nowPlayingLabel = nowPlayingLabel,
                emptyLabel = emptyLabel
            )

            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyLabel, color = Color(0xFF8A95A5), fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    state = rowListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 48.dp, end = 48.dp, top = 6.dp, bottom = 36.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    itemsIndexed(rows, key = { index, row -> "${row.name}#$index" }) { rowIndex, row ->
                        VodCategoryRow(
                            group = row,
                            rowFocused = rowIndex == focusedRow,
                            focusedCol = if (rowIndex == focusedRow) focusedCol else -1,
                            currentChannelKey = currentChannel?.stableKey,
                            onChannelClick = onChannelClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodHeroSection(
    sectionTitle: String,
    channel: Channel?,
    isPlaying: Boolean,
    watchLabel: String,
    nowPlayingLabel: String,
    emptyLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .padding(start = 48.dp, end = 48.dp, top = 26.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (channel != null) {
            ChannelPoster(
                posterUrl = channel.logoUrl,
                name = channel.name,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Accent)
                )
                Text(
                    text = sectionTitle.uppercase(),
                    color = Color(0xFF9AA6B4),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp
                )
            }
            Text(
                text = channel?.name ?: emptyLabel,
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (channel != null && channel.rating > 0f) {
                    RatingBadge(rating = channel.rating)
                }
                if (channel != null && channel.group.isNotBlank()) {
                    Text(
                        text = channel.group,
                        color = Color(0xFF9AA6B4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isPlaying) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Accent.copy(alpha = 0.16f))
                            .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = nowPlayingLabel,
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            if (channel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color(0xFF14161A), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "OK — $watchLabel",
                        color = Color(0xFFD5DEE7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodCategoryRow(
    group: ChannelGroup,
    rowFocused: Boolean,
    focusedCol: Int,
    currentChannelKey: String?,
    onChannelClick: (Channel) -> Unit
) {
    val colListState = rememberLazyListState()

    LaunchedEffect(focusedCol, group.channels.size) {
        if (focusedCol in group.channels.indices) {
            colListState.animateScrollToItem(maxOf(0, focusedCol - 1))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = group.name,
                color = if (rowFocused) Color.White else Color(0xFFB8C2CE),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.channels.size.toString(),
                color = if (rowFocused) Accent else Color(0xFF67727E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        LazyRow(
            state = colListState,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                group.channels,
                key = { index, channel -> "${channel.stableKey}#$index" }
            ) { colIndex, channel ->
                VodPosterCard(
                    channel = channel,
                    focused = rowFocused && colIndex == focusedCol,
                    isPlaying = channel.stableKey == currentChannelKey,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodPosterCard(
    channel: Channel,
    focused: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(160),
        label = "vod_card_scale"
    )
    val cardShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .width(118.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(168.dp)
                .clip(cardShape)
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = when {
                        focused -> Accent
                        isPlaying -> Color(0x66FFFFFF)
                        else -> Color(0x1AFFFFFF)
                    },
                    shape = cardShape
                )
                .clickable(onClick = onClick)
        ) {
            ChannelPoster(
                posterUrl = channel.logoUrl,
                name = channel.name,
                modifier = Modifier.fillMaxSize()
            )
            if (channel.rating > 0f) {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    RatingBadge(rating = channel.rating, compact = true)
                }
            }
            if (channel.isFavorite) {
                Text(
                    text = "★",
                    color = Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color(0xFF14161A), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Text(
            text = channel.name,
            color = if (focused) Color.White else Color(0xFFAEB9C5),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
