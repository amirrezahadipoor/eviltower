package ir.hadipoor.eviltower.game.model

import androidx.compose.ui.graphics.Color

data class Point(val x: Float, val y: Float)

enum class TowerType(
    val title: String,
    val shortTitle: String,
    val baseCost: Int,
    val baseDamage: Float,
    val baseRange: Float,
    val baseInterval: Float,
    val color: Color,
    val flyingOnly: Boolean = false,
    val splash: Boolean = false,
    val unlockable: Boolean = false,
) {
    ARCHER("برج تیرانداز", "تیر", 90, 16f, .21f, .62f, Color(0xFF65D6A0)),
    CANNON("برج توپخانه", "توپ", 145, 45f, .24f, 1.65f, Color(0xFFFF9B54), splash = true),
    FROST("برج یخ", "یخ", 120, 9f, .23f, .72f, Color(0xFF70D6FF)),
    FIRE("برج آتش", "آتش", 135, 13f, .22f, .88f, Color(0xFFFF5B4D)),
    LIGHTNING("برج رعد", "رعد", 175, 29f, .25f, 1.20f, Color(0xFFFFE36E), splash = true),
    SKY_ARCHER("برج کماندار آسمان", "آسمان", 155, 22f, .28f, .78f, Color(0xFFC19BFF), flyingOnly = true),
    ARCANE("برج جادوی اهریمنی", "اهریمنی", 240, 58f, .26f, 1.12f, Color(0xFFE68CFF), unlockable = true),
}

enum class EnemyType(
    val title: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val reward: Int,
    val flying: Boolean = false,
) {
    GRUNT("پیاده اهریمنی", 24f, .030f, 7),
    WOLF("گرگ سایه", 16f, .052f, 8),
    BAT("خفاش شیطانی", 20f, .042f, 10, flying = true),
    SKELETON("اسکلت زره‌پوش", 62f, .022f, 12),
    SPIDER("عنکبوت اهریمنی", 48f, .025f, 13),
    OGRE("غول سنگی", 160f, .012f, 26),
    WRAITH("شبح سایه", 72f, .028f, 20),
    IMP("جن آتشین", 27f, .060f, 11),
    MINI_BOSS("مینی‌باس", 450f, .016f, 100),
    BOSS("باس", 1800f, .010f, 400),
}

data class Tower(
    val id: Int,
    val type: TowerType,
    val plot: Int,
    val level: Int = 1,
    val cooldown: Float = 0f,
    val totalDamage: Long = 0,
    val upgradePulse: Float = 0f,
)

data class Enemy(
    val id: Int,
    val type: EnemyType,
    val progress: Float,
    val hp: Float,
    val maxHp: Float,
    val flying: Boolean,
    val elite: Boolean = false,
    val bossName: String? = null,
    val slow: Float = 0f,
    val burn: Float = 0f,
    val burnDps: Float = 0f,
    val hitFlash: Float = 0f,
)

data class Projectile(
    val id: Int,
    val towerType: TowerType,
    val from: Point,
    val to: Point,
    val progress: Float = 0f,
)

data class FloatingText(val id: Int, val text: String, val at: Point, val color: Color, val age: Float = 0f)
data class Particle(val id: Int, val at: Point, val color: Color, val age: Float = 0f, val size: Float = 1f)

data class WaveUnit(val type: EnemyType, val elite: Boolean = false, val bossName: String? = null)
data class WavePlan(val wave: Int, val units: List<WaveUnit>, val isBoss: Boolean, val isMiniBoss: Boolean, val bossName: String? = null)

enum class EnginePhase { PREP, ACTIVE, PAUSED, DEFEATED }

data class GameSnapshot(
    val phase: EnginePhase = EnginePhase.PREP,
    val wave: Int = 1,
    val bestWave: Int = 0,
    val gold: Int = 520,
    val gems: Int = 0,
    val coreHp: Int = 20,
    val coreMaxHp: Int = 20,
    val enemiesDefeated: Int = 0,
    val goldEarned: Int = 0,
    val runSeconds: Int = 0,
    val prepRemaining: Float = 3f,
    val spawned: Int = 0,
    val totalToSpawn: Int = 0,
    val isEndless: Boolean = false,
    val bossName: String? = null,
    val bossHp: Float = 0f,
    val bossMaxHp: Float = 0f,
    val abilityRemaining: Float = 0f,
    val selectedPlot: Int? = null,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val combo: Int = 0,
    val message: String? = null,
)
