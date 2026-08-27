package ir.hadipoor.eviltower.game.engine

import androidx.compose.ui.graphics.Color
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.EnginePhase
import ir.hadipoor.eviltower.game.model.FloatingText
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.game.model.Particle
import ir.hadipoor.eviltower.game.model.Point
import ir.hadipoor.eviltower.game.model.Projectile
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import ir.hadipoor.eviltower.game.model.WavePlan
import ir.hadipoor.eviltower.game.model.WaveUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Deterministic, Android-free simulation. Mutations happen on one ViewModel coroutine and the UI
 * receives immutable snapshots. The engine intentionally owns all combat rules, not Composables.
 */
class GameEngine(private val random: Random = Random(77)) {
    private var phase = EnginePhase.PREP
    private var wave = 1
    private var bestWave = 0
    private var startingBest = 0
    private var gold = 520
    private var gems = 0
    private var coreHp = Balance.MAX_CORE_HP
    private var enemiesDefeated = 0
    private var bossesDefeated = 0
    private var goldEarned = 0
    private var elapsed = 0f
    private var prepRemaining = Balance.PREP_SECONDS
    private var spawnTimer = 0f
    private var spawned = 0
    private var combo = 0
    private var nextId = 1
    private var selectedPlot: Int? = null
    private var currentPlan: WavePlan? = null
    private var message: String? = null
    private var messageTimer = 0f
    private var abilityCooldown = 0f
    private var bossAttackCooldown = 7f
    private var bossTelegraph = 0f
    private var hitStop = 0f
    private var screenShake = 0f
    private var arcaneUnlocked = false
    private var lowGraphics = false
    private val towers = mutableListOf<Tower>()
    private val enemies = mutableListOf<Enemy>()
    private val projectiles = mutableListOf<Projectile>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val particles = mutableListOf<Particle>()
    private val particlePool = ObjectPool<Particle>(factory = { Particle(0, Point(0f, 0f), Color.White) })
    private val projectilePool = ObjectPool<Projectile>(factory = { Projectile(0, TowerType.ARCHER, Point(0f, 0f), Point(0f, 0f)) }, initialSize = 24)
    private val enemyPool = ObjectPool<Enemy>(factory = { Enemy(0, EnemyType.GRUNT, 0f, 0f, 1f, false) }, initialSize = 64)

    fun startRun(startingGold: Int = 520, personalBest: Int = 0, arcane: Boolean = false, lowGraphics: Boolean = false) {
        phase = EnginePhase.PREP; wave = 1; bestWave = personalBest; startingBest = personalBest
        gold = startingGold; gems = 0; coreHp = Balance.MAX_CORE_HP
        enemiesDefeated = 0; bossesDefeated = 0; goldEarned = 0; elapsed = 0f
        prepRemaining = Balance.PREP_SECONDS; spawnTimer = 0f; spawned = 0; combo = 0; nextId = 1
        selectedPlot = null; currentPlan = null; message = null; messageTimer = 0f
        abilityCooldown = 0f; bossAttackCooldown = 7f; bossTelegraph = 0f; hitStop = 0f; screenShake = 0f; arcaneUnlocked = arcane; this.lowGraphics = lowGraphics
        particles.forEach(particlePool::recycle)
        enemies.forEach(enemyPool::recycle)
        projectiles.forEach(projectilePool::recycle)
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
        towers[index] = tower.copy(level = tower.level + 1, upgradePulse = 1f, webbed = 0f)
        addBurst(Balance.PLOTS[plot], tower.type.color, 12)
        return true
    }

    fun sellSelected(): Boolean {
        val plot = selectedPlot ?: return false
        val index = towers.indexOfFirst { it.plot == plot }
        if (index < 0) return false
        val tower = towers.removeAt(index)
        gold += (Balance.towerCost(tower.type, tower.level) * .60f).toInt()
        selectedPlot = null
        return true
    }

    /** Map-targeted inferno: a fair cooldown, strong area damage and an obvious vector burst. */
    fun activateInferno(): Boolean {
        if (phase != EnginePhase.ACTIVE || abilityCooldown > 0f) return false
        abilityCooldown = 18f
        val center = selectedPlot?.let { Balance.PLOTS[it] } ?: Point(.50f, .50f)
        val victims = enemies.filter { distance(positionOf(it.progress), center) < .19f }.map { it.id }
        victims.forEach { id -> damageEnemy(id, 260f + wave * 1.2f, TowerType.FIRE, center, sourceLevel = 35, allowSplash = false) }
        addBurst(center, Color(0xFFFF5B4D), 22)
        screenShake = max(screenShake, .10f)
        return true
    }

    fun update(dtRaw: Float) {
        if (phase == EnginePhase.PAUSED || phase == EnginePhase.DEFEATED) return
        val dt = dtRaw.coerceIn(0f, .05f)
        elapsed += dt
        messageTimer = max(0f, messageTimer - dt)
        screenShake = max(0f, screenShake - dt * 1.7f)
        updateEffects(dt)
        if (hitStop > 0f) { hitStop = max(0f, hitStop - dt); return }
        abilityCooldown = max(0f, abilityCooldown - dt)
        when (phase) {
            EnginePhase.PREP -> { prepRemaining -= dt; if (prepRemaining <= 0f) beginWave() }
            EnginePhase.ACTIVE -> updateActive(dt)
            else -> Unit
        }
    }

    private fun beginWave() {
        bestWave = max(bestWave, wave)
        currentPlan = Balance.wavePlan(wave)
        spawned = 0; spawnTimer = 0f; prepRemaining = Balance.PREP_SECONDS; phase = EnginePhase.ACTIVE
        if (wave == 301) announce("شما وارد فاز بی‌پایان شده‌اید", 6f)
        if (currentPlan?.isBoss == true) {
            addBurst(Point(.90f, .16f), Color(0xFFFF4D6D), 26)
            announce("هشدار باس: ${currentPlan?.bossName}", 4f)
        } else if (currentPlan?.isMiniBoss == true) {
            announce("مینی‌باس نزدیک می‌شود: ${currentPlan?.bossName}", 3f)
        }
    }

    private fun announce(text: String, seconds: Float) {
        message = text; messageTimer = seconds
        floatingTexts += FloatingText(nextId++, text, Point(.50f, .10f), Color(0xFFFFD166))
    }

    private fun updateActive(dt: Float) {
        currentPlan?.let { plan ->
            if (spawned < plan.units.size) {
                spawnTimer -= dt
                if (spawnTimer <= 0f) {
                    spawn(plan.units[spawned]); spawned++
                    spawnTimer = if (plan.isBoss && spawned == plan.units.size) .9f else .52f
                }
            }
        }
        updateBossAttack(dt)
        updateEnemies(dt)
        updateTowers(dt)
        projectiles.replaceAll { it.copy(progress = it.progress + dt * 5.5f) }
        projectiles.filter { it.progress >= 1f }.forEach(projectilePool::recycle)
        projectiles.removeAll { it.progress >= 1f }
        if (phase == EnginePhase.ACTIVE && spawned >= (currentPlan?.units?.size ?: 0) && enemies.isEmpty() && projectiles.isEmpty()) clearWave()
    }

    private fun spawn(unit: WaveUnit) {
        val hp = Balance.enemyHp(unit.type, wave, unit.elite)
        enemies += enemyPool.obtain().copy(
            id = nextId++, type = unit.type, progress = 0f, hp = hp, maxHp = hp,
            flying = unit.type.flying, elite = unit.elite, bossName = unit.bossName,
            bossPhase = 1, stealth = unit.type == EnemyType.WRAITH,
        )
    }

    private fun updateBossAttack(dt: Float) {
        val boss = enemies.firstOrNull { it.type == EnemyType.BOSS } ?: run {
            bossAttackCooldown = 7f; bossTelegraph = 0f; return
        }
        if (bossTelegraph > 0f) {
            bossTelegraph -= dt
            if (bossTelegraph <= 0f && enemies.any { it.id == boss.id }) {
                coreHp -= if (boss.bossPhase >= 3) 2 else 1
                screenShake = max(screenShake, .16f)
                addBurst(Balance.PATH.last(), Color(0xFFFF476F), 16)
            }
        } else {
            bossAttackCooldown -= dt
            if (bossAttackCooldown <= 0f) {
                bossTelegraph = if (boss.bossPhase >= 3) 1.4f else 2.1f
                bossAttackCooldown = if (boss.bossPhase >= 3) 6.5f else 9f
                announce("ضربه‌ی سنگین باس آماده می‌شود", bossTelegraph + .2f)
                addBurst(Balance.PATH.last(), Color(0xFFFF476F), 10)
            }
        }
        if (coreHp <= 0) phase = EnginePhase.DEFEATED
    }

    private fun updateEnemies(dt: Float) {
        val iterator = enemies.listIterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            var updated = enemy
            if (enemy.burn > 0f) updated = updated.copy(burn = max(0f, enemy.burn - dt), hp = enemy.hp - enemy.burnDps * dt)
            val bossPhase = when {
                enemy.type != EnemyType.BOSS -> 1
                updated.hp > updated.maxHp * .66f -> 1
                updated.hp > updated.maxHp * .33f -> 2
                else -> 3
            }
            updated = updated.copy(
                bossPhase = bossPhase,
                stealth = updated.type == EnemyType.WRAITH && (elapsed.toInt() / 3) % 2 == 1,
                slow = max(0f, updated.slow - dt),
                hitFlash = max(0f, updated.hitFlash - dt),
                progress = updated.progress + Balance.enemySpeed(updated.type, wave) * (1f - updated.slow.coerceIn(0f, .75f)) * dt,
            )
            if (updated.type == EnemyType.SPIDER && random.nextFloat() < dt * .055f && towers.isNotEmpty()) {
                val target = random.nextInt(towers.size)
                towers[target] = towers[target].copy(webbed = max(towers[target].webbed, 2.5f))
                addBurst(Balance.PLOTS[towers[target].plot], Color(0xFFC65FA2), 4)
            }
            if (updated.hp <= 0f) {
                iterator.remove(); onKill(updated); enemyPool.recycle(updated)
            } else if (updated.progress >= 1f) {
                iterator.remove(); enemyPool.recycle(updated)
                val damage = if (updated.type == EnemyType.BOSS) 3 else if (updated.type == EnemyType.MINI_BOSS) 2 else Balance.enemyDamage(wave)
                coreHp -= damage; combo = 0; screenShake = max(screenShake, if (damage >= 2) .18f else .08f)
                addBurst(Balance.PATH.last(), Color(0xFFFF476F), 12)
                if (coreHp <= 0) phase = EnginePhase.DEFEATED
            } else iterator.set(updated)
        }
        towers.replaceAll { it.copy(webbed = max(0f, it.webbed - dt)) }
    }

    private fun updateTowers(dt: Float) {
        for (index in towers.indices) {
            val tower = towers[index]
            val cooldown = max(0f, tower.cooldown - dt)
            if (tower.webbed > 0f || cooldown > 0f) {
                towers[index] = tower.copy(cooldown = cooldown, upgradePulse = max(0f, tower.upgradePulse - dt * 2f)); continue
            }
            val origin = Balance.PLOTS[tower.plot]
            val target = enemies.filter { enemy ->
                (!enemy.flying || tower.type == TowerType.SKY_ARCHER) && distance(positionOf(enemy.progress), origin) <= Balance.towerRange(tower)
            }.maxByOrNull { it.progress }
            if (target == null) {
                towers[index] = tower.copy(cooldown = cooldown, upgradePulse = max(0f, tower.upgradePulse - dt * 2f)); continue
            }
            val targetPoint = positionOf(target.progress)
            projectiles += projectilePool.obtain().copy(id = nextId++, towerType = tower.type, from = origin, to = targetPoint, progress = 0f)
            damageEnemy(target.id, Balance.towerDamage(tower), tower.type, targetPoint, tower.level)
            if (tower.type == TowerType.LIGHTNING) {
                enemies.filter { it.id != target.id && distance(positionOf(it.progress), targetPoint) < .13f }.take(3)
                    .forEach { chain -> damageEnemy(chain.id, Balance.towerDamage(tower) * .42f, tower.type, targetPoint, tower.level, false) }
            }
            towers[index] = tower.copy(cooldown = Balance.towerInterval(tower), totalDamage = tower.totalDamage + Balance.towerDamage(tower).toLong(), upgradePulse = max(0f, tower.upgradePulse - dt * 2f))
        }
    }

    private fun damageEnemy(id: Int, rawDamage: Float, towerType: TowerType, at: Point, sourceLevel: Int = 1, allowSplash: Boolean = true) {
        val index = enemies.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = enemies[index]
        val multiplier = Balance.armorMultiplier(target.type, towerType, wave, target.stealth)
        val damage = rawDamage * multiplier
        val burn = if (towerType == TowerType.FIRE) Balance.burnSeconds(sourceLevel) else target.burn
        val burnDps = if (towerType == TowerType.FIRE) damage * .26f else target.burnDps
        val slow = if (towerType == TowerType.FROST) Balance.slowStrength(sourceLevel) else target.slow
        val nextPhase = when {
            target.type != EnemyType.BOSS -> target.bossPhase
            target.hp - damage > target.maxHp * .66f -> 1
            target.hp - damage > target.maxHp * .33f -> 2
            else -> 3
        }
        val updated = target.copy(
            hp = target.hp - damage, hitFlash = .12f, burn = max(target.burn, burn), burnDps = max(target.burnDps, burnDps),
            slow = max(target.slow, slow), bossPhase = nextPhase,
        )
        enemies[index] = updated
        addFloating("-${damage.toInt()}", at, towerType.color)
        addParticle(at, towerType.color, if (towerType == TowerType.CANNON) 1.8f else 1f)
        if (damage >= 90f || target.type == EnemyType.BOSS) hitStop = max(hitStop, .035f)
        if (target.type == EnemyType.BOSS && nextPhase != target.bossPhase) {
            announce("فاز ${nextPhase} باس آغاز شد", 2.2f); addBurst(at, Color(0xFFFF4D6D), 18); screenShake = max(screenShake, .12f)
        }
        val killed = updated.hp <= 0f
        if (killed) { enemies.removeAt(index); enemyPool.recycle(updated) }
        if (allowSplash && towerType == TowerType.CANNON) {
            enemies.filter { it.id != id && distance(positionOf(it.progress), at) < .115f }.map { it.id }.take(5)
                .forEach { splashId -> damageEnemy(splashId, rawDamage * .42f, towerType, at, sourceLevel, false) }
        }
        if (killed) { onKill(updated); return }
    }

    private fun onKill(enemy: Enemy) {
        val reward = Balance.enemyReward(enemy.type, wave)
        gold += reward; goldEarned += reward; enemiesDefeated++; combo++
        if (enemy.type == EnemyType.BOSS) bossesDefeated++
        addFloating("+$reward", positionOf(enemy.progress), Color(0xFFFFD166))
        addBurst(positionOf(enemy.progress), when (enemy.type) {
            EnemyType.BAT, EnemyType.WRAITH -> Color(0xFFC19BFF)
            EnemyType.IMP -> Color(0xFFFF5B4D)
            EnemyType.SKELETON -> Color(0xFFE8E0D0)
            EnemyType.SPIDER -> Color(0xFFC65FA2)
            else -> Color(0xFF7CE38B)
        }, if (enemy.type == EnemyType.BOSS) 30 else 8)
        if (enemy.type == EnemyType.IMP) {
            enemies.filter { distance(positionOf(it.progress), positionOf(enemy.progress)) < .12f }.map { it.id }.toList()
                .forEach { damageEnemy(it, enemy.maxHp * .12f, TowerType.FIRE, positionOf(enemy.progress), 20, false) }
        }
    }

    private fun clearWave() {
        val clearReward = Balance.waveClearReward(wave)
        gold += clearReward; goldEarned += clearReward; gems += if (wave % 10 == 0) 2 else 0
        addFloating("موج ${wave} پاک شد  +$clearReward", Point(.5f, .22f), Color(0xFFFFD166))
        wave++; bestWave = max(bestWave, wave - 1); currentPlan = null; prepRemaining = Balance.PREP_SECONDS; phase = EnginePhase.PREP; spawned = 0
    }

    private fun updateEffects(dt: Float) {
        floatingTexts.replaceAll { it.copy(age = it.age + dt) }
        particles.replaceAll { it.copy(age = it.age + dt) }
        floatingTexts.removeAll { it.age > 1.35f }
        val expired = particles.filter { it.age > .78f }
        expired.forEach(particlePool::recycle)
        particles.removeAll { it.age > .78f }
    }

    private fun addFloating(text: String, at: Point, color: Color) { floatingTexts += FloatingText(nextId++, text, at, color) }
    private fun addParticle(at: Point, color: Color, size: Float) { particles += particlePool.obtain().copy(id = nextId++, at = at, color = color, age = 0f, size = size) }
    private fun addBurst(at: Point, color: Color, count: Int = 8) {
        repeat(if (lowGraphics) max(1, count / 2) else count) { addParticle(at, color, .7f + random.nextFloat() * 1.2f) }
    }

    private fun positionOf(progress: Float): Point {
        val p = progress.coerceIn(0f, .9999f) * (Balance.PATH.size - 1)
        val index = p.toInt().coerceIn(0, Balance.PATH.size - 2); val t = p - index
        val a = Balance.PATH[index]; val b = Balance.PATH[index + 1]
        return Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
    private fun distance(a: Point, b: Point): Float = kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    fun snapshot(): GameSnapshot {
        val boss = enemies.firstOrNull { it.type == EnemyType.BOSS || it.type == EnemyType.MINI_BOSS }
        return GameSnapshot(
            phase = phase, wave = wave, bestWave = bestWave, gold = gold, gems = gems, coreHp = coreHp,
            enemiesDefeated = enemiesDefeated, bossesDefeated = bossesDefeated, goldEarned = goldEarned, runSeconds = elapsed.toInt(),
            prepRemaining = prepRemaining.coerceAtLeast(0f), worldTime = elapsed, screenShake = screenShake,
            spawned = spawned, totalToSpawn = currentPlan?.units?.size ?: 0, isEndless = wave >= 301,
            bossName = boss?.bossName, bossHp = boss?.hp ?: 0f, bossMaxHp = boss?.maxHp ?: 0f,
            bossPhase = boss?.bossPhase ?: 1, bossTelegraph = bossTelegraph.coerceAtLeast(0f), abilityRemaining = abilityCooldown, selectedPlot = selectedPlot,
            towers = towers.toList(), enemies = enemies.toList(), projectiles = projectiles.toList(),
            floatingTexts = floatingTexts.toList(), particles = particles.toList(), combo = combo,
            message = message.takeIf { messageTimer > 0f },
            newRecord = bestWave > startingBest,
        )
    }

    fun towerAt(plot: Int): Tower? = towers.firstOrNull { it.plot == plot }
    fun abilityRemaining(): Float = abilityCooldown
    fun hasArcane(): Boolean = arcaneUnlocked
}
