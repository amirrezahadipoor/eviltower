package ir.hadipoor.eviltower.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.R

/** Bundled Persian font (Vazirmatn, SIL OFL). No network fonts are used anywhere. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int = size + 10) = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val EvilTowerTypography = Typography(
    displayLarge = style(40, FontWeight.Bold, 56),
    displayMedium = style(32, FontWeight.Bold, 46),
    headlineLarge = style(26, FontWeight.Bold, 38),
    headlineMedium = style(22, FontWeight.Bold, 32),
    titleLarge = style(20, FontWeight.Medium, 30),
    titleMedium = style(17, FontWeight.Medium, 26),
    bodyLarge = style(16, FontWeight.Normal, 26),
    bodyMedium = style(14, FontWeight.Normal, 24),
    bodySmall = style(12, FontWeight.Normal, 20),
    labelLarge = style(15, FontWeight.Medium, 22),
    labelMedium = style(13, FontWeight.Medium, 20),
    labelSmall = style(11, FontWeight.Normal, 16),
)
