package az.iptv.fplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import az.iptv.fplayer.data.preferences.AppThemeMode

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FPLAYERTheme(
    themeMode: String = AppThemeMode.CLASSIC.name,
    content: @Composable () -> Unit
) {
    val colors = fPlayerColorsFor(themeMode)
    CompositionLocalProvider(LocalFPlayerColors provides colors) {
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
}
