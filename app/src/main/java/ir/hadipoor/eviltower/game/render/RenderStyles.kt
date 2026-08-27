package ir.hadipoor.eviltower.game.render

import androidx.compose.ui.graphics.Color
import ir.hadipoor.eviltower.ui.theme.TowerPalette

/** Visual identity of a tower theme (unlocked in the shop, فروشگاه → پوسته برج). */
data class TowerStyle(
    val id: String,
    val persianName: String,
    val wall: Color,
    val wallDark: Color,
    val brickLine: Color,
    val platform: Color,
    val platformEdge: Color,
    val accent: Color,
    val fog: Color,
    val sky: Color,
    val skyTop: Color,
)

/** Visual identity of a hero skin (unlocked in the shop, فروشگاه → پوسته قهرمان). */
data class HeroStyle(
    val id: String,
    val persianName: String,
    val armor: Color,
    val armorDark: Color,
    val cape: Color,
    val trim: Color,
    val visor: Color,
    val blade: Color,
)

object RenderStyles {

    val towerThemes: List<TowerStyle> = listOf(
        TowerStyle(
            id = "classic",
            persianName = "سنگ نفرین‌شده",
            wall = Color(0xFF2B2438),
            wallDark = Color(0xFF1A1526),
            brickLine = Color(0xFF120E1C),
            platform = Color(0xFF4A4159),
            platformEdge = Color(0xFF6E6288),
            accent = TowerPalette.Ember,
            fog = Color(0x22B39CFF),
            sky = Color(0xFF120C1E),
            skyTop = Color(0xFF261A3C),
        ),
        TowerStyle(
            id = "ember",
            persianName = "برج آتش",
            wall = Color(0xFF3A2018),
            wallDark = Color(0xFF23120C),
            brickLine = Color(0xFF160A06),
            platform = Color(0xFF67382A),
            platformEdge = Color(0xFF9C5233),
            accent = Color(0xFFFF9A2E),
            fog = Color(0x26FF8A3C),
            sky = Color(0xFF180A08),
            skyTop = Color(0xFF3B1710),
        ),
        TowerStyle(
            id = "frost",
            persianName = "برج یخ‌زده",
            wall = Color(0xFF1E2C3E),
            wallDark = Color(0xFF121C29),
            brickLine = Color(0xFF0B131C),
            platform = Color(0xFF35506B),
            platformEdge = Color(0xFF6FA0C4),
            accent = Color(0xFF7FE3FF),
            fog = Color(0x2A9FD8FF),
            sky = Color(0xFF0A1220),
            skyTop = Color(0xFF16304A),
        ),
        TowerStyle(
            id = "abyss",
            persianName = "پرتگاه سبز",
            wall = Color(0xFF16301F),
            wallDark = Color(0xFF0C1C13),
            brickLine = Color(0xFF07110B),
            platform = Color(0xFF2A5136),
            platformEdge = Color(0xFF4F9B63),
            accent = Color(0xFF7BE38B),
            fog = Color(0x2470FFA8),
            sky = Color(0xFF08130C),
            skyTop = Color(0xFF14301E),
        ),
    )

    val heroSkins: List<HeroStyle> = listOf(
        HeroStyle(
            id = "knight",
            persianName = "شوالیه گرفتار",
            armor = Color(0xFF8A93B5),
            armorDark = Color(0xFF565E7C),
            cape = Color(0xFFB13A4E),
            trim = Color(0xFFFFC93C),
            visor = Color(0xFF1A1626),
            blade = Color(0xFFE7ECFF),
        ),
        HeroStyle(
            id = "shadow",
            persianName = "راهب سایه",
            armor = Color(0xFF453A63),
            armorDark = Color(0xFF261F3B),
            cape = Color(0xFF6D3BC4),
            trim = Color(0xFFB98BFF),
            visor = Color(0xFF120E1C),
            blade = Color(0xFFC59BFF),
        ),
        HeroStyle(
            id = "ember",
            persianName = "جنگاور آتش",
            armor = Color(0xFF8C3A1E),
            armorDark = Color(0xFF54200F),
            cape = Color(0xFFFF7A18),
            trim = Color(0xFFFFD27D),
            visor = Color(0xFF2A0F06),
            blade = Color(0xFFFFB067),
        ),
        HeroStyle(
            id = "bone",
            persianName = "پهلوان استخوانی",
            armor = Color(0xFFE8E2D0),
            armorDark = Color(0xFFA79E86),
            cape = Color(0xFF3E4A5B),
            trim = Color(0xFF7BE38B),
            visor = Color(0xFF1B2029),
            blade = Color(0xFFDFF7E3),
        ),
    )

    fun tower(id: String): TowerStyle = towerThemes.firstOrNull { it.id == id } ?: towerThemes[0]
    fun hero(id: String): HeroStyle = heroSkins.firstOrNull { it.id == id } ?: heroSkins[0]
}
