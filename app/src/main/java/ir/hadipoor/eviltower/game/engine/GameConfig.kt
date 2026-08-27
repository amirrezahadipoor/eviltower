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

/** Single source of truth for the endless curve. Every number is covered by balance_simulation.py. */
object Balance {
    const val MAX_TOWER_LEVEL = 100
    const val MAX_CORE_HP = 20
    const val PREP_SECONDS = 3f
    const val HP_GROWTH = 1.03
    const val SPEED_GROWTH = 1.0018
    const val DAMAGE_GROWTH = 1.018
    const val TOWER_DAMAGE_GROWTH = 1.075
    const val TOWER_COST_GROWTH = 1.017

    fun regularHp(wave: Int): Float = 24f * HP_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()).toFloat()
    fun enemySpeed(type: EnemyType, wave: Int): Float = type.baseSpeed * SPEED_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()).toFloat()
    fun enemyDamage(wave: Int): Int = (1 + floor(DAMAGE_GROWTH.pow((wave - 1).coerceAtLeast(0).toDouble()) / 10.0)).toInt().coerceAtLeast(1)
    fun variantMultiplier(wave: Int): Float = 1f + .12f * floor((wave - 1) / 40f)

    fun enemyHp(type: EnemyType, wave: Int, elite: Boolean = false): Float {
        val base = when (type) {
            EnemyType.MINI_BOSS -> regularHp(wave) * (5.5f + wave / 110f)
            EnemyType.BOSS -> regularHp(wave) * (7f + wave / 90f)
            else -> regularHp(wave) * (type.baseHp / EnemyType.GRUNT.baseHp)
        }
        return base * variantMultiplier(wave) * if (elite) 1.18f else 1f
    }

    /** Reward grows sub-linearly compared with the 1.03 HP curve. */
    fun enemyReward(type: EnemyType, wave: Int): Int = ceil(type.reward * (1 + .028 * (wave - 1).coerceAtLeast(0).toDouble().pow(.78))).toInt()
    fun waveClearReward(wave: Int): Int = ceil(28 * wave.toDouble().pow(.58)).toInt()
    fun enemyCount(wave: Int): Int = 7 + floor(wave.toDouble().pow(.72)).toInt()

    fun towerCost(type: TowerType, level: Int): Int = ceil(type.baseCost * TOWER_COST_GROWTH.pow((level - 1).coerceAtLeast(0))).toInt()
    fun upgradeCost(tower: Tower): Int = towerCost(tower.type, tower.level + 1)
    fun towerDamage(tower: Tower): Float = typeDamage(tower.type, tower.level)
    fun typeDamage(type: TowerType, level: Int): Float = type.baseDamage * TOWER_DAMAGE_GROWTH.pow((level - 1).coerceIn(0, MAX_TOWER_LEVEL - 1)).toFloat()
    fun towerRange(tower: Tower): Float = tower.type.baseRange * (1f + .006f * (tower.level - 1))
    fun towerInterval(tower: Tower): Float = (tower.type.baseInterval * .992.pow((tower.level - 1).coerceAtLeast(0)).toFloat()).coerceAtLeast(.18f)
    fun slowStrength(level: Int): Float = (.18f + level * .004f).coerceAtMost(.68f)
    fun burnSeconds(level: Int): Float = 2.5f + level * .045f
    fun armorMultiplier(type: EnemyType, tower: TowerType, wave: Int, stealth: Boolean = false): Float = when {
        type == EnemyType.SKELETON && tower != TowerType.ARCANE -> .68f
        type == EnemyType.WRAITH && stealth && tower != TowerType.ARCANE -> .55f
        else -> 1f
    }

    fun wavePlan(wave: Int): WavePlan {
        val isBoss = wave % 10 == 0
        val isMini = wave % 5 == 0 && !isBoss
        val count = enemyCount(wave)
        val regularCount = (count * when { isBoss -> .62f; isMini -> .78f; else -> 1f }).toInt().coerceAtLeast(3)
        val units = buildList {
            repeat(regularCount) { index -> add(WaveUnit(typeFor(wave, index), elite = wave >= 40 && wave % 40 == 0)) }
            if (isMini) add(WaveUnit(EnemyType.MINI_BOSS, elite = true, bossName = MINI_BOSSES[(wave / 5 - 1) % MINI_BOSSES.size]))
            if (isBoss) add(WaveUnit(EnemyType.BOSS, elite = true, bossName = BOSS_NAMES[(wave / 10 - 1) % BOSS_NAMES.size]))
        }
        return WavePlan(wave, units, isBoss, isMini, units.lastOrNull { it.bossName != null }?.bossName)
    }

    private fun typeFor(wave: Int, index: Int): EnemyType {
        val pool = when {
            wave < 3 -> listOf(EnemyType.GRUNT, EnemyType.WOLF)
            wave < 8 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT)
            wave < 16 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER)
            wave < 28 -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER, EnemyType.IMP)
            else -> listOf(EnemyType.GRUNT, EnemyType.WOLF, EnemyType.BAT, EnemyType.SKELETON, EnemyType.SPIDER, EnemyType.OGRE, EnemyType.WRAITH, EnemyType.IMP)
        }
        return pool[(index + wave * 3) % pool.size]
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
