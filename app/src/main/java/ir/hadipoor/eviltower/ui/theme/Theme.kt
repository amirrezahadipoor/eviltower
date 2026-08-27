package ir.hadipoor.eviltower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0B0812)
val Panel = Color(0xFF1D162B)
val PanelLight = Color(0xFF2C203F)
val Ember = Color(0xFFFF754C)
val Gold = Color(0xFFFFD166)
val Frost = Color(0xFF70D6FF)
val Mist = Color(0xFFA98CC7)

private val scheme = darkColorScheme(
    primary = Ember, secondary = Gold, tertiary = Frost,
    background = Night, surface = Panel, surfaceVariant = PanelLight,
    onPrimary = Color(0xFF2A0B0A), onBackground = Color(0xFFF7F0FF), onSurface = Color(0xFFF7F0FF),
)

@Composable fun EvilTowerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
