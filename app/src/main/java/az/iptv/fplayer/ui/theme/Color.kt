package az.iptv.fplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import az.iptv.fplayer.data.preferences.AppThemeMode

data class FPlayerColors(
    val accent: Color,
    val accentDim: Color,
    val warmAccent: Color,
    val appBg: Color,
    val appBgMid: Color,
    val appBgEnd: Color,
    val panelBg: Color,
    val panelBgSoft: Color,
    val sidebarBg: Color,
    val osdBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val focusBorder: Color,
    val badgeBg: Color,
    val dividerColor: Color,
    val selectionBg: Color,
    val selectionText: Color,
    val selectionSecondaryText: Color,
    val progressTrack: Color,
    val progressFill: Color,
    val selectionProgressFill: Color
)

private val ClassicColors = FPlayerColors(
    accent = Color(0xFF8CCBFF),
    accentDim = Color(0xCC8CCBFF),
    warmAccent = Color(0xFFD7D9DD),
    appBg = Color(0xFF0E1115),
    appBgMid = Color(0xFF0B1F27),
    appBgEnd = Color(0xFF020609),
    panelBg = Color(0xE91C2228),
    panelBgSoft = Color(0xB31B2026),
    sidebarBg = Color(0xD91C2228),
    osdBg = Color(0xD9000000),
    cardBg = Color(0xA821252C),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8BBC0),
    focusBorder = Color(0xFFE6E7EA),
    badgeBg = Color(0x99D8DADF),
    dividerColor = Color(0x33FFFFFF),
    selectionBg = Color(0xF2FFFFFF),
    selectionText = Color(0xFF111417),
    selectionSecondaryText = Color(0xFF2E3135),
    progressTrack = Color(0x52FFFFFF),
    progressFill = Color(0xF2FFFFFF),
    selectionProgressFill = Color(0xFF202226)
)

private val DrmPlayColors = FPlayerColors(
    accent = Color(0xFFFFD400),
    accentDim = Color(0xCCFFD400),
    warmAccent = Color(0xFFFFE15A),
    appBg = Color(0xFF000000),
    appBgMid = Color(0xFF0A0905),
    appBgEnd = Color(0xFF000000),
    panelBg = Color(0xE6000000),
    panelBgSoft = Color(0xB3000000),
    sidebarBg = Color(0xE6000000),
    osdBg = Color(0xD9000000),
    cardBg = Color(0xB80A0A0A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE3E3E3),
    focusBorder = Color(0xFF747397),
    badgeBg = Color(0xA86D6C91),
    dividerColor = Color(0xFFFFD400),
    selectionBg = Color(0xE56D6C91),
    selectionText = Color(0xFFFFFFFF),
    selectionSecondaryText = Color(0xFFEAEAF2),
    progressTrack = Color(0xFF4D4C71),
    progressFill = Color(0xFFFFD400),
    selectionProgressFill = Color(0xFFFFD400)
)

internal val LocalFPlayerColors = staticCompositionLocalOf { ClassicColors }

internal fun fPlayerColorsFor(themeMode: String): FPlayerColors =
    when (runCatching { AppThemeMode.valueOf(themeMode) }.getOrDefault(AppThemeMode.CLASSIC)) {
        AppThemeMode.CLASSIC -> ClassicColors
        AppThemeMode.DRM_PLAY -> DrmPlayColors
    }

val Accent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.accent

val AccentDim: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.accentDim

val WarmAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.warmAccent

val AppBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.appBg

val AppBgMid: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.appBgMid

val AppBgEnd: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.appBgEnd

val PanelBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.panelBg

val PanelBgSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.panelBgSoft

val SidebarBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.sidebarBg

val OsdBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.osdBg

val CardBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.cardBg

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.textSecondary

val FocusBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.focusBorder

val BadgeBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.badgeBg

val DividerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.dividerColor

val SelectionBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.selectionBg

val SelectionText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.selectionText

val SelectionSecondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.selectionSecondaryText

val ProgressTrack: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.progressTrack

val ProgressFill: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.progressFill

val SelectionProgressFill: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFPlayerColors.current.selectionProgressFill
