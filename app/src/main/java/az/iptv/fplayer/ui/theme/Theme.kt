package az.iptv.fplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FPLAYERTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = AccentDim,
            background = SidebarBg,
            surface = CardBg,
            onPrimary = TextPrimary,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        ),
        typography = Typography,
        content = content
    )
}
