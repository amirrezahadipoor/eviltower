package ir.hadipoor.eviltower.game.engine

import androidx.compose.ui.graphics.Color
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnginePhase
import ir.hadipoor.eviltower.game.model.FloatingText
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.game.model.Particle
import ir.hadipoor.eviltower.game.model.Point
import ir.hadipoor.eviltower.game.model.Projectile
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.WavePlan
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Deterministic, Android-free wave-defense simulation. Compose only observes snapshots. */
class GameEngine(private val random: Random = Random(77)) {
    private var phase = EnginePhase.PREP
    private var wave = 1
    private var bestWave = 0
    private var gold = 520
    private var gems = 0
    private var coreHp = Balance.MAX_CORE_HP
    private var enemiesDefeated = 0
    private var goldEarned = 0
    private var elapsed = 0f
    private var prepRemaining = Balance.PREP_SECONDS
    private var spawnTimer = 0f
    private var spawned = 0
    private var combo = 0
    private var nextId = 1
    private var selectedPlot: Int? = null
    private var currentPlan: WavePlan? = null
    private var endlessMessageTimer = 0f
    private var abilityCooldown = 0f
    private var arcaneUnlocked = false
    private val towers = mutableListOf<Tower>()
    private val enemies = mutableListOf<Enemy>()
    private val projectiles = mutableListOf<Projectile>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val particles = mutableListOf<Particle>()
    private val particlePool = ObjectPool { Particle(0, Point(0f, 0f), Color.White) }

    fun startRun(startingGold: Int = 520, personalBest: Int = 0, arcane: Boolean = false) {
        phase = EnginePhase.PREP
        wave = 1
        bestWave = personalBest
        gold = startingGold
        gems = 0
        coreHp = Balance.MAX_CORE_HP
        enemiesDefeated = 0
        goldEarned = 0
        elapsed = 0f
        prepRemaining = Balance.PREP_SECONDS
        spawnTimer = 0f
        spawned = 0
        combo = 0
        nextId = 1
        selectedPlot = null
        currentPlan = null
        arcaneUnlocked = arcane
        abilityCooldown = 0f
        towers.clear(); enemies.clear(); projectiles.clear(); floatingTexts.clear(); particles.clear()
    }

    fun togglePause() {
        phase = when (phase) {
            EnginePhase.ACTIVE -> EnginePhase.PAUSED
            EnginePhase.PAUSED -> EnginePhase.ACTIVE
            else -> phase
        }
    }

    fun selectPlot(plot: Int?) { selectedPlot = plot?.takeIf { it in Balance.PLOTS.indices } }

    fun buildTower(type: TowerType): Boolean {
        val plot = selectedPlot ?: return false
        if (towers.any { it.plot == plot } || type.unlockable && !arcaneUnlocked) return false
        val cost = Balance.towerCost(type, 1)
        if (gold < cost) return false
        gold -= cost
        towers += Tower(nextId++, type, plot)
        addBurst(Balance.PLOTS[plot], type.color)
        return true
    }

    fun upgradeSelected(): Boolean {
        val plot = selectedPlot ?: return false
        val index = towers.indexOfFirst { it.plot == plot }
        if (index < 0) return false
        val tower = towers[index]
        if (tower.level >= Balance.MAX_TOWER_LEVEL) return false
        val cost = Balance.upgradeCost(tower)
        if (gold < cost) return false
        gold -= cost
        towers[index] = tower.copy(level = tower.level + 1, upgradePulse = 1f)
        addBurst(Balance.PLOTS[plot], tower.type.color)
        return true
    }

    fun sellSelected(): Boolean {
        val plot = selectedPlot ?: return false
        val index = towers.indexOfFirst { it.plot == plot }
        if (index < 0) return false
        val tower = towers.removeAt(index)
        gold += (Balance.towerCost(tower.type, tower.level) * .55f).toInt()
        selectedPlot = null
        return true
    }

    /** A map-targeted inferno ability: strong area damage, with a readable cooldown. */
    fun activateInferno(): Boolean {
        if (phase != EnginePhase.ACTIVE || abilityCooldown > 0f) return false
        abilityCooldown = 18f
        val center = selectedPlot?.let { Balance.PLOTS[it] } ?: Point(.50f, .50f)
        val victims = enemies.filter { distance(positionOf(it.progress), center) < .18f }.toList()
        victims.forEach { enemy -> damageEnemy(enemy.id, 180f, TowerType.FIRE, center) }
        addBurst(center, Color(0xFFFF5B4D), 16)
        return true
    }

    fun update(dtRaw: Float) {
        if (phase == EnginePhase.PAUSED || phase == EnginePhase.DEFEATED) return
        val dt = dtRaw.coerceIn(0f, .08f)
        elapsed += dt
        abilityCooldown = max(0f, abilityCooldown - dt)
        endlessMessageTimer = max(0f, endlessMessageTimer - dt)
        updateEffects(dt)
        when (phase) {
            EnginePhase.PREP -> {
                prepRemaining -= dt
                if (prepRemaining <= 0f) beginWave()
            }
            EnginePhase.ACTIVE -> updateActive(dt)
            else -> Unit
        }
    }

    private fun beginWave() {
        currentPlan = Balance.wavePlan(wave)
        spawned = 0
        spawnTimer = 0f
        prepRemaining = Balance.PREP_SECONDS
        phase = EnginePhase.ACTIVE
        if (wave == 301) endlessMessageTimer = 6f
        if (currentPlan?.isBoss == true) {
            addBurst(Point(.90f, .16f), Color(0xFFFF4D6D), 20)
            floatingTexts += FloatingText(nextId++, "هشدار باس: ${currentPlan?.bossName}", Point(.52f, .10f), Color(0xFFFFD166))
        }
    }

    private fun updateActive(dt: Float) {
        currentPlan?.let { plan ->
            if (spawned < plan.units.size) {
                spawnTimer -= dt
                if (spawnTimer <= 0f) {
                    spawn(plan.units[spawned])
                    spawned++
                    spawnTimer = if (plan.isBoss && spawned == plan.units.size) .9f else .52f
                }
            }
        }
        updateEnemies(dt)
        updateTowers(dt)
        projectiles.forEachIndexed { index, projectile -> projectiles[index] = projectile.copy(progress = projectile.progress + dt * 5.5f) }
        projectiles.removeAll { it.progress >= 1f }
        if (phase == EnginePhase.ACTIVE && spawned >= (currentPlan?.units?.size ?: 0) && enemies.isEmpty() && projectiles.isEmpty()) {
            clearWave()
        }
    }

    private fun spawn(unit: ir.hadipoor.eviltower.game.model.WaveUnit) {
        val hp = Balance.enemyHp(unit.type, wave, unit.elite)
        enemies += Enemy(
            id = nextId++, type = unit.type, progress = 0f, hp = hp, maxHp = hp,
            flying = unit.type.flying, elite = unit.elite, bossName = unit.bossName,
        )
    }

    private fun updateEnemies(dt: Float) {
        val iterator = enemies.listIterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            var updated = enemy
            if (enemy.burn > 0f) {
                updated = updated.copy(burn = max(0f, enemy.burn - dt), hp = enemy.hp - enemy.burnDps * dt)
            }
            val speedFactor = 1f - updated.slow.coerceIn(0f, .75f)
            updated = updated.copy(
                slow = max(0f, updated.slow - dt),
                hitFlash = max(0f, updated.hitFlash - dt),
                progress = updated.progress + Balance.enemySpeed(updated.type, wave) * speedFactor * dt,
            )
            if (updated.hp <= 0f) {
                iterator.remove()
                onKill(updated)
            } else if (updated.progress >= 1f) {
                iterator.remove()
                coreHp -= if (updated.type == EnemyType.BOSS) 3 else if (updated.type == EnemyType.MINI_BOSS) 2 else Balance.enemyDamage(wave)
                combo = 0
                addBurst(Balance.PATH.last(), Color(0xFFFF476F), 10)
                if (coreHp <= 0) phase = EnginePhase.DEFEATED
            } else {
                iterator.set(updated)
            }
        }
    }

    private fun updateTowers(dt: Float) {
        for (index in towers.indices) {
            var tower = towers[index]
            val cooldown = max(0f, tower.cooldown - dt)
            if (cooldown > 0f) {
                towers[index] = tower.copy(cooldown = cooldown, upgradePulse = max(0f, tower.upgradePulse - dt * 2f))
                continue
            }
            val origin = Balance.PLOTS[tower.plot]
            val target = enemies
                .filter { enemy -> (!enemy.flying || tower.type == TowerType.SKY_ARCHER) && distance(positionOf(enemy.progress), origin) <= Balance.towerRange(tower) }
                .maxByOrNull { it.progress }
            if (target != null) {
                val targetPoint = positionOf(target.progress)
                projectiles += Projectile(nextId++, tower.type, origin, targetPoint)
                damageEnemy(target.id, Balance.towerDamage(tower), tower.type, targetPoint)
                if (tower.type == TowerType.LIGHTNING) {
                    enemies.filter { it.id != target.id && distance(positionOf(it.progress), targetPoint) < .12f }.take(2)
                        .forEach { damageEnemy(it.id, Balance.towerDamage(tower) * .38f, tower.type, targetPoint) }
                }
                towers[index] = tower.copy(cooldown = Balance.towerInterval(tower), totalDamage = tower.totalDamage + Balance.towerDamage(tower).toLong())
            } else {
                towers[index] = tower.copy(cooldown = cooldown, upgradePulse = max(0f, tower.upgradePulse - dt * 2f))
            }
        }
    }

    private fun damageEnemy(id: Int, rawDamage: Float, towerType: TowerType, at: Point) {
        val index = enemies.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = enemies[index]
        val physicalMultiplier = if (target.type == EnemyType.SKELETON && towerType != TowerType.ARCANE) .68f else 1f
        val damage = rawDamage * physicalMultiplier
        val burn = if (towerType == TowerType.FIRE) Balance.burnSeconds(levelFor(towerType)) else target.burn
        val burnDps = if (towerType == TowerType.FIRE) damage * .22f else target.burnDps
        val slow = if (towerType == TowerType.FROST) Balance.slowStrength(levelFor(towerType)) else target.slow
        val updated = target.copy(hp = target.hp - damage, hitFlash = .10f, burn = max(target.burn, burn), burnDps = max(target.burnDps, burnDps), slow = max(target.slow, slow))
        enemies[index] = updated
        floatingTexts += FloatingText(nextId++, "-${damage.toInt()}", at, towerType.color)
        particles += Particle(nextId++, at, towerType.color, size = if (towerType == TowerType.CANNON) 1.8f else 1f)
        if (updated.hp <= 0f) {
            enemies.removeAt(index)
            onKill(updated)
        }
    }

    private fun levelFor(type: TowerType): Int = towers.filter { it.type == type }.maxOfOrNull { it.level } ?: 1

    private fun onKill(enemy: Enemy) {
        val reward = Balance.enemyReward(enemy.type, wave)
        gold += reward
        goldEarned += reward
        enemiesDefeated++
        combo++
        floatingTexts += FloatingText(nextId++, "+$reward", positionOf(enemy.progress), Color(0xFFFFD166))
        addBurst(positionOf(enemy.progress), when (enemy.type) {
            EnemyType.BAT, EnemyType.WRAITH -> Color(0xFFC19BFF)
            EnemyType.IMP -> Color(0xFFFF5B4D)
            EnemyType.SKELETON -> Color(0xFFE8E0D0)
            else -> Color(0xFF7CE38B)
        }, if (enemy.type == EnemyType.BOSS) 24 else 6)
    }

    private fun clearWave() {
        val clearReward = ceil(20 * wave.toDouble().pow(.55)).toInt()
        gold += clearReward
        goldEarned += clearReward
        gems += if (wave % 10 == 0) 2 else 0
        floatingTexts += FloatingText(nextId++, "موج ${wave} پاک شد  +$clearReward", Point(.5f, .22f), Color(0xFFFFD166))
        wave++
        bestWave = max(bestWave, wave - 1)
        currentPlan = null
        prepRemaining = Balance.PREP_SECONDS
        phase = EnginePhase.PREP
        spawned = 0
    }

    private fun updateEffects(dt: Float) {
        for (index in floatingTexts.indices) floatingTexts[index] = floatingTexts[index].copy(age = floatingTexts[index].age + dt)
        for (index in particles.indices) particles[index] = particles[index].copy(age = particles[index].age + dt)
        floatingTexts.removeAll { it.age > 1.2f }
        val expired = particles.filter { it.age > .75f }
        expired.forEach { particlePool.recycle(it) }
        particles.removeAll { it.age > .75f }
    }

    private fun addBurst(at: Point, color: Color, count: Int = 8) {
        repeat(count) {
            particles += particlePool.obtain().copy(id = nextId++, at = at, color = color, age = 0f, size = .7f + random.nextFloat() * 1.2f)
        }
    }

    private fun positionOf(progress: Float): Point {
        val p = progress.coerceIn(0f, .9999f) * (Balance.PATH.size - 1)
        val index = p.toInt().coerceIn(0, Balance.PATH.size - 2)
        val t = p - index
        val a = Balance.PATH[index]; val b = Balance.PATH[index + 1]
        return Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }

    private fun distance(a: Point, b: Point): Float = kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    fun snapshot(): GameSnapshot {
        val boss = enemies.firstOrNull { it.type == EnemyType.BOSS || it.type == EnemyType.MINI_BOSS }
        return GameSnapshot(
            phase = phase, wave = wave, bestWave = bestWave, gold = gold, gems = gems,
            coreHp = coreHp, enemiesDefeated = enemiesDefeated, goldEarned = goldEarned,
            runSeconds = elapsed.toInt(), prepRemaining = prepRemaining.coerceAtLeast(0f),
            spawned = spawned, totalToSpawn = currentPlan?.units?.size ?: 0, isEndless = wave >= 301,
            bossName = boss?.bossName, bossHp = boss?.hp ?: 0f, bossMaxHp = boss?.maxHp ?: 0f,
            abilityRemaining = abilityCooldown,
            selectedPlot = selectedPlot, towers = towers.toList(), enemies = enemies.toList(),
            projectiles = projectiles.toList(), floatingTexts = floatingTexts.toList(), particles = particles.toList(),
            combo = combo, message = if (endlessMessageTimer > 0f) "شما وارد فاز بی‌پایان شده‌اید" else null,
        )
    }

    fun towerAt(plot: Int): Tower? = towers.firstOrNull { it.plot == plot }
    fun abilityRemaining(): Float = abilityCooldown
    fun hasArcane(): Boolean = arcaneUnlocked
}
