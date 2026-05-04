package az.iptv.fplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val allGroups = listOf(null) + groups.map { it.name as String? }
    val selectedIndex = if (selectedGroup == null) 0 else groups.indexOfFirst { it.name == selectedGroup } + 1

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
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
            val count = if (groupName == null)
                groups.sumOf { it.channels.size }
            else
                groups.find { it.name == groupName }?.channels?.size ?: 0

            Column(
                modifier = Modifier
                    .clickable { onGroupSelect(groupName) }
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = groupName ?: "Bütün kanallar",
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    maxLines = 1
                )
                // Active underline
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 24.dp else 0.dp)
                        .height(2.dp)
                        .background(Accent)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
