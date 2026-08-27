package ir.hadipoor.eviltower.game.engine

import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.Point
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import ir.hadipoor.eviltower.game.model.WavePlan
import ir.hadipoor.eviltower.game.model.WaveUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/** All tuning lives here so balance changes never leak into rendering. */
object Balance {
    const val MAX_TOWER_LEVEL = 100
    const val MAX_CORE_HP = 20
    const val PREP_SECONDS = 3f
    const val HP_GROWTH = 1.045
    const val SPEED_GROWTH = 1.004
    const val DAMAGE_GROWTH = 1.032

    fun regularHp(wave: Int): Float = 24f * HP_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()).toFloat()
    fun enemySpeed(type: EnemyType, wave: Int): Float =
        type.baseSpeed * SPEED_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()).toFloat()
    fun enemyDamage(wave: Int): Int = (1 + floor(DAMAGE_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()) / 8.0)).toInt().coerceAtLeast(1)
    fun variantMultiplier(wave: Int): Float = 1f + .18f * floor((wave - 1) / 35f)
    fun enemyHp(type: EnemyType, wave: Int, elite: Boolean = false): Float {
        val base = when (type) {
            EnemyType.MINI_BOSS -> regularHp(wave) * (7.5f + wave / 90f)
            EnemyType.BOSS -> regularHp(wave) * (13f + wave / 45f)
            else -> regularHp(wave) * (type.baseHp / EnemyType.GRUNT.baseHp)
        }
        val eliteBoost = if (elite) 1.28f else 1f
        return base * variantMultiplier(wave) * eliteBoost
    }
    fun enemyReward(type: EnemyType, wave: Int): Int =
        ceil(type.reward * (1 + 0.018 * (wave - 1).coerceAtLeast(0).toDouble().pow(.82))).toInt()
    fun enemyCount(wave: Int): Int = 7 + floor(wave.toDouble().pow(.72)).toInt()
    fun towerCost(type: TowerType, level: Int): Int = ceil(type.baseCost * 1.09.pow((level - 1).coerceAtLeast(0))).toInt()
    fun upgradeCost(tower: Tower): Int = towerCost(tower.type, tower.level + 1)
    fun towerDamage(tower: Tower): Float = tower.type.baseDamage * (1f + .105f * (tower.level - 1))
    fun towerRange(tower: Tower): Float = tower.type.baseRange * (1f + .008f * (tower.level - 1))
    fun towerInterval(tower: Tower): Float = (tower.type.baseInterval * .994.pow((tower.level - 1).coerceAtLeast(0)).toFloat()).coerceAtLeast(.16f)
    fun slowStrength(level: Int): Float = (.18f + level * .004f).coerceAtMost(.62f)
    fun burnSeconds(level: Int): Float = 2.5f + level * .045f

    fun wavePlan(wave: Int): WavePlan {
        val boss = wave % 10 == 0
        val mini = wave % 5 == 0 && !boss
        val count = enemyCount(wave)
        val units = buildList {
            val lightCount = if (boss) (count * .62f).toInt() else if (mini) (count * .78f).toInt() else count
            repeat(lightCount.coerceAtLeast(3)) { index -> add(WaveUnit(typeFor(wave, index), elite = wave >= 35 && wave % 35 == 0)) }
            if (mini) add(WaveUnit(EnemyType.MINI_BOSS, elite = true, bossName = MINI_BOSSES[(wave / 5 - 1) % MINI_BOSSES.size]))
            if (boss) add(WaveUnit(EnemyType.BOSS, elite = true, bossName = BOSS_NAMES[(wave / 10 - 1) % BOSS_NAMES.size]))
        }
        return WavePlan(wave, units, boss, mini, if (boss) units.last().bossName else if (mini) units.last().bossName else null)
    }

    private fun typeFor(wave: Int, index: Int): EnemyType {
        val unlocked = when {
            wave < 3 -> listOf(EnemyType.GRUNT, EnemyType.WOLF)
            wave < 8 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT)
            wave < 16 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER)
            wave < 28 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER, EnemyType.IMP)
            else -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER, EnemyType.OGRE, EnemyType.WRAITH, EnemyType.IMP)
        }
        return unlocked[(index + wave * 3) % unlocked.size]
    }

    val MINI_BOSSES = listOf("دیو دروازه‌بان", "شکارچی سایه", "جادوگر اهریمنی", "کاهن خون", "عنکبوت مادر", "شکننده‌ی سنگ")
    val BOSS_NAMES = listOf("ملکه‌ی خفاش‌ها", "فرمانروای استخوان", "تایتان خاکستر", "سایه‌ی نخستین", "ارباب برج", "ارباب برج: بیداری")

    val PATH = listOf(
        Point(.90f, .16f), Point(.70f, .16f), Point(.58f, .34f), Point(.38f, .27f),
        Point(.27f, .48f), Point(.46f, .60f), Point(.68f, .52f), Point(.78f, .76f), Point(.54f, .86f), Point(.13f, .78f)
    )
    val PLOTS = listOf(
        Point(.82f, .30f), Point(.67f, .45f), Point(.48f, .13f), Point(.35f, .40f),
        Point(.20f, .61f), Point(.40f, .76f), Point(.61f, .70f), Point(.84f, .62f),
        Point(.27f, .88f), Point(.14f, .40f), Point(.53f, .45f), Point(.72f, .87f)
    )
}
