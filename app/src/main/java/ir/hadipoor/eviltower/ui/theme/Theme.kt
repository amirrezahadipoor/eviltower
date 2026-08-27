package ir.hadipoor.eviltower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val EvilTowerColors = darkColorScheme(
    primary = TowerPalette.Ember,
    onPrimary = TowerPalette.Shadow,
    secondary = TowerPalette.PurpleLight,
    onSecondary = TowerPalette.TextPrimary,
    tertiary = TowerPalette.Gem,
    background = TowerPalette.Night,
    onBackground = TowerPalette.TextPrimary,
    surface = TowerPalette.DeepPurple,
    onSurface = TowerPalette.TextPrimary,
    surfaceVariant = TowerPalette.Purple,
    onSurfaceVariant = TowerPalette.TextMuted,
    error = TowerPalette.Blood,
)

/**
 * App theme. The whole UI is forced to RTL by default because the game ships in Persian;
 * switching the language to English in the settings flips [layoutDirection] back to LTR.
 */
@Composable
fun EvilTowerTheme(
    rtl: Boolean = true,
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = EvilTowerColors,
            typography = EvilTowerTypography,
            content = content,
        )
    }
}
