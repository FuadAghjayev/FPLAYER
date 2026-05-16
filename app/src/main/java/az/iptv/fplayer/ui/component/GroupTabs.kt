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
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.FocusBorder
import az.iptv.fplayer.ui.theme.PanelBgSoft

@Composable
fun GroupTabs(
    groups: List<ChannelGroup>,
    selectedGroup: String?,
    onGroupSelect: (String?) -> Unit,
    focusedGroupIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val allGroups = listOf(null as String?) + groups.map { it.name as String? }
    val selectedIndex = if (selectedGroup == null) 0 else groups.indexOfFirst { it.name == selectedGroup } + 1

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    LaunchedEffect(focusedGroupIndex) {
        if (focusedGroupIndex >= 0) listState.scrollToItem(focusedGroupIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(PanelBgSoft),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(allGroups) { idx, groupName ->
            val isSelected = idx == selectedIndex
            val isFocused = idx == focusedGroupIndex

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                        .background(
                            when {
                            isSelected && isFocused -> Color(0xCC12383D)
                            isSelected -> Color(0xAA102A31)
                            isFocused -> Color(0x9918323C)
                            else -> Color.Transparent
                        }
                    )
                    .focusProperties { canFocus = false }
                    .clickable { onGroupSelect(groupName) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol rəng çubuğu
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            when {
                                isSelected -> Brush.verticalGradient(listOf(Accent, Accent.copy(0.3f)))
                                isFocused -> Brush.verticalGradient(listOf(FocusBorder, FocusBorder.copy(alpha = 0.25f)))
                                else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                )

                // İkon
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (idx == 0) "☰" else "▸",
                        color = when {
                            isSelected -> Accent
                            isFocused -> FocusBorder
                            else -> Color(0xFF5C7079)
                        },
                        fontSize = if (idx == 0) 13.sp else 10.sp
                    )
                }

                // Qrup adı
                Text(
                    text = groupName ?: "Bütün kanallar",
                    color = when {
                        isSelected -> Color.White
                        isFocused -> Color(0xFFEAF8FF)
                        else -> Color(0xFF8EA1AA)
                    },
                    fontSize = 12.sp,
                    fontWeight = when {
                        isSelected -> FontWeight.SemiBold
                        isFocused -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                // Kanal sayı
                if (idx > 0) {
                    val count = groups.getOrNull(idx - 1)?.channels?.size ?: 0
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) Accent.copy(alpha = 0.25f) else Color(0x12FFFFFF)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$count",
                                color = if (isSelected) Accent else Color(0xFF788D96),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
