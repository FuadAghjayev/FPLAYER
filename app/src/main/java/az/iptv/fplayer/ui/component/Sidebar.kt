package az.iptv.fplayer.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import az.iptv.fplayer.data.model.Channel
import az.iptv.fplayer.data.model.ChannelGroup
import az.iptv.fplayer.ui.theme.Accent
import az.iptv.fplayer.ui.theme.DividerColor
import az.iptv.fplayer.ui.theme.SidebarBg
import az.iptv.fplayer.ui.theme.TextSecondary

@Composable
fun Sidebar(
    visible: Boolean,
    groups: List<ChannelGroup>,
    channels: List<Channel>,
    currentChannel: Channel?,
    selectedGroup: String?,
    onGroupSelect: (String?) -> Unit,
    onChannelSelect: (Channel) -> Unit,
    onPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            GroupPanel(
                groups = groups,
                selectedGroup = selectedGroup,
                onGroupSelect = onGroupSelect,
                onPlaylistClick = onPlaylistClick
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))
            ChannelPanel(
                channels = channels,
                currentChannel = currentChannel,
                onChannelSelect = onChannelSelect
            )
        }
    }
}

@Composable
private fun GroupPanel(
    groups: List<ChannelGroup>,
    selectedGroup: String?,
    onGroupSelect: (String?) -> Unit,
    onPlaylistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .padding(vertical = 8.dp)
    ) {
        var searchQuery by remember { mutableStateOf("") }

        SearchBox(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))
        MenuRow(label = "Pleylist əlavə et", icon = "📋", onClick = onPlaylistClick)
        MenuRow(label = "Axtar", icon = "🔍", onClick = {})
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
        Spacer(Modifier.height(8.dp))

        Text(
            text = "QRUPLAR",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                GroupRow(
                    name = "Bütün kanallar",
                    count = groups.sumOf { it.channels.size },
                    isSelected = selectedGroup == null,
                    onClick = { onGroupSelect(null) }
                )
            }
            items(groups) { group ->
                GroupRow(
                    name = group.name,
                    count = group.channels.size,
                    isSelected = selectedGroup == group.name,
                    onClick = { onGroupSelect(group.name) }
                )
            }
        }
    }
}

@Composable
private fun ChannelPanel(
    channels: List<Channel>,
    currentChannel: Channel?,
    onChannelSelect: (Channel) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(SidebarBg.copy(alpha = 0.85f))
    ) {
        ChannelList(
            channels = channels,
            currentChannel = currentChannel,
            onChannelClick = onChannelSelect,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { inner ->
            if (query.isEmpty()) Text("Axtar...", color = TextSecondary, fontSize = 14.sp)
            inner()
        }
    )
}

@Composable
private fun MenuRow(label: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun GroupRow(name: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) Accent.copy(alpha = 0.2f) else Color.Transparent
    val color = if (isSelected) Accent else Color(0xFFDDDDDD)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isSelected) Text("»", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(name, color = color, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        }
        Text("($count)", color = TextSecondary, fontSize = 11.sp)
    }
}
