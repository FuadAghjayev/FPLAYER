package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.TextSecondary

@Composable
fun GroupTabs(
    groups: List<ChannelGroup>,
    selectedGroup: String?,
    onGroupSelect: (String?) -> Unit,
    focusedGroupIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val allGroups = listOf(null) + groups.map { it.name as String? }
    val selectedIndex = if (selectedGroup == null) 0 else groups.indexOfFirst { it.name == selectedGroup } + 1

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }

    LaunchedEffect(focusedGroupIndex) {
        if (focusedGroupIndex >= 0) listState.animateScrollToItem(focusedGroupIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A)),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(allGroups) { idx, groupName ->
            val isSelected = idx == selectedIndex
            val isFocused = idx == focusedGroupIndex

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isFocused && !isSelected -> Color(0x30FFFFFF)
                            else -> Color.Transparent
                        }
                    )
                    .then(
                        if (isFocused) Modifier.border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(6.dp))
                        else Modifier
                    )
                    .clickable { onGroupSelect(groupName) }
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = groupName ?: "Bütün kanallar",
                    color = when {
                        isSelected -> Color.White
                        isFocused -> Color(0xFFDDDDDD)
                        else -> TextSecondary
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .width(
                            when {
                                isSelected -> 24.dp
                                isFocused -> 16.dp
                                else -> 0.dp
                            }
                        )
                        .height(2.dp)
                        .background(if (isSelected) Accent else Color(0x88FFFFFF))
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
