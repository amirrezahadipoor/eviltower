package ir.hadipoor.eviltower.game.engine

import ir.hadipoor.eviltower.game.model.Aabb
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnemyKind
import ir.hadipoor.eviltower.game.model.EnemyState
import ir.hadipoor.eviltower.game.model.FloorData
import ir.hadipoor.eviltower.game.model.GameEvent
import ir.hadipoor.eviltower.game.model.Pickup
import ir.hadipoor.eviltower.game.model.PickupKind
import ir.hadipoor.eviltower.game.model.Platform
import ir.hadipoor.eviltower.game.model.PlatformKind
import ir.hadipoor.eviltower.game.model.Player
import ir.hadipoor.eviltower.game.model.PlayerState
import ir.hadipoor.eviltower.game.model.PowerUp
import ir.hadipoor.eviltower.game.model.RunPhase
import ir.hadipoor.eviltower.game.model.TrapKind
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** Per-frame player input, produced by swipe gestures, on-screen buttons or the accelerometer. */
data class InputState(
    /** -1 = left, 0 = still, +1 = right (analog values allowed for tilt control). */
    val moveX: Float = 0f,
    val jumpPressed: Boolean = false,
    val attackPressed: Boolean = false,
)

/**
 * The whole simulation of a single run: physics, collision, enemy AI, traps, pickups, camera
 * and progression. Pure Kotlin, zero Android dependencies, so it is unit-testable.
 */
class GameEngine(
    val runSeed: Long = Random.nextLong(),
    startHealth: Int = GameConfig.START_HEALTH,
    startingPowerUps: Set<PowerUp> = emptySet(),
    private val coinBonusMultiplier: Float = 1f,
    private val startFloor: Int = 1,
) {
    val player = Player(
        bounds = Aabb(
            x = GameConfig.WORLD_WIDTH / 2f - GameConfig.PLAYER_W / 2f,
            y = (startFloor - 1) * GameConfig.FLOOR_HEIGHT,
            w = GameConfig.PLAYER_W,
            h = GameConfig.PLAYER_H,
        ),
        health = startHealth,
        maxHealth = startHealth,
    )

    val floors = LinkedHashMap<Int, FloorData>()
    val events = ArrayList<GameEvent>(16)

    var phase: RunPhase = RunPhase.PLAYING
        private set
    var currentFloor: Int = startFloor
        private set
    var highestFloor: Int = startFloor
        private set
    var coins: Int = 0
        private set
    var gems: Int = 0
        private set
    var enemiesDefeated: Int = 0
        private set
    var runTime: Float = 0f
        private set
    var cameraY: Float = 0f
    var screenShake: Float = 0f
        private set
    var fallTimer: Float = 0f
        private set

    private val rng = Random(runSeed)
    private val spawnedBats = ArrayList<Enemy>()

    val score: Int get() = highestFloor * GameConfig.scoreMultiplier(highestFloor) * 10 + coins

    init {
        startingPowerUps.forEach { grantPower(it, silent = true) }
        ensureFloors()
        val ground = floors[startFloor]!!
        player.bounds.y = ground.baseY + 1f
        cameraY = player.bounds.centerY
    }

    // ---------------------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------------------

    fun pause() {
        if (phase == RunPhase.PLAYING) phase = RunPhase.PAUSED
    }

    fun resume() {
        if (phase == RunPhase.PAUSED) phase = RunPhase.PLAYING
    }

    /** Rewarded-ad continue: revive on the current floor with full health. */
    fun revive() {
        if (phase != RunPhase.GAME_OVER) return
        val floor = floors[currentFloor] ?: floors.values.first()
        player.health = player.maxHealth
        player.bounds.x = GameConfig.WORLD_WIDTH / 2f - player.bounds.w / 2f
        player.bounds.y = floor.baseY + 2f
        player.vx = 0f
        player.vy = 0f
        player.invulnerable = 2.5f
        player.state = PlayerState.IDLE
        fallTimer = 0f
        phase = RunPhase.PLAYING
    }

    fun visibleFloors(): List<FloorData> = floors.values.toList()

    fun consumeEvents(): List<GameEvent> {
        if (events.isEmpty()) return emptyList()
        val copy = events.toList()
        events.clear()
        return copy
    }

    /** Advances the simulation by [dtRaw] seconds (clamped so a hitch cannot tunnel collisions). */
    fun update(dtRaw: Float, input: InputState) {
        val dt = dtRaw.coerceIn(0f, 1f / 30f)
        if (phase == RunPhase.PAUSED || phase == RunPhase.GAME_OVER || phase == RunPhase.VICTORY) return

        runTime += dt
        screenShake = (screenShake - dt * 3.2f).coerceAtLeast(0f)

        if (phase == RunPhase.FALLING) {
            updateFalling(dt)
            return
        }

        ensureFloors()
        updateTimers(dt)
        updatePlatforms(dt)
        updateTraps(dt)
        updatePlayer(dt, input)
        updateEnemies(dt)
        updatePickups(dt)
        checkHazards()
        updateProgress()
        updateCamera(dt)
    }

    // ---------------------------------------------------------------------------------------
    // Floors
    // ---------------------------------------------------------------------------------------

    private fun ensureFloors() {
        for (f in (currentFloor - 1).coerceAtLeast(1)..(currentFloor + 2)) {
            if (f <= GameConfig.FINAL_FLOOR && !floors.containsKey(f)) {
                floors[f] = FloorGenerator.generate(f, runSeed)
            }
        }
        // drop floors far below the player to keep the simulation small
        val minKeep = (currentFloor - 2).coerceAtLeast(1)
        floors.keys.filter { it < minKeep }.forEach { floors.remove(it) }
    }

    private fun floorOf(y: Float): Int =
        ((y / GameConfig.FLOOR_HEIGHT).toInt() + 1).coerceIn(1, GameConfig.FINAL_FLOOR)

    // ---------------------------------------------------------------------------------------
    // Timers / power-ups
    // ---------------------------------------------------------------------------------------

    private fun updateTimers(dt: Float) {
        player.animTime += dt
        if (player.invulnerable > 0f) player.invulnerable -= dt
        if (player.attackTimer > 0f) player.attackTimer -= dt
        if (player.controlsReversed > 0f) player.controlsReversed -= dt
        if (player.squash > 0f) player.squash = (player.squash - dt * 4f).coerceAtLeast(0f)
        val iterator = player.powerUps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == PowerUp.SHIELD) continue // consumed on hit, not on time
            entry.setValue(entry.value - dt)
            if (entry.value <= 0f) iterator.remove()
        }
    }

    private fun grantPower(power: PowerUp, silent: Boolean = false) {
        val duration = when (power) {
            PowerUp.SHIELD -> GameConfig.POWER_SHIELD_TIME
            PowerUp.WINGS -> GameConfig.POWER_WINGS_TIME
            PowerUp.SPEED -> GameConfig.POWER_SPEED_TIME
            PowerUp.MAGNET -> GameConfig.POWER_MAGNET_TIME
        }
        player.powerUps[power] = duration
        if (!silent) events += GameEvent.PowerUpTaken
    }

    // ---------------------------------------------------------------------------------------
    // Platforms & traps
    // ---------------------------------------------------------------------------------------

    private fun updatePlatforms(dt: Float) {
        floors.values.forEach { floor ->
            floor.platforms.forEach { p ->
                when (p.kind) {
                    PlatformKind.MOVING -> {
                        p.phase += dt * p.moveSpeed / (p.moveTo - p.moveFrom).coerceAtLeast(1f)
                        val t = (sin(p.phase * Math.PI.toFloat() * 2f) + 1f) / 2f
                        p.bounds.x = p.moveFrom + (p.moveTo - p.moveFrom) * t
                    }

                    PlatformKind.CRUMBLING -> {
                        if (p.crumbleTimer > 0f) {
                            p.crumbleTimer -= dt
                            p.shakeSeed += dt * 40f
                            if (p.crumbleTimer <= 0f && !p.gone) {
                                p.gone = true
                                p.crumbleTimer = -GameConfig.CRUMBLE_RESPAWN
                                events += GameEvent.Crumble
                            }
                        } else if (p.crumbleTimer < -0.001f) {
                            p.crumbleTimer += dt
                            if (p.crumbleTimer >= -0.001f) {
                                p.crumbleTimer = -1f
                                p.gone = false
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun updateTraps(dt: Float) {
        floors.values.forEach { floor ->
            floor.traps.forEach { trap ->
                val before = trap.isActive
                trap.timer += dt
                if (!before && trap.isActive &&
                    (trap.kind == TrapKind.FIRE_JET || trap.kind == TrapKind.SLEEP_GAS)
                ) {
                    if (abs(trap.bounds.centerY - player.bounds.centerY) < GameConfig.VIEWPORT_HEIGHT / 2f) {
                        events += GameEvent.TrapTrigger
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Player
    // ---------------------------------------------------------------------------------------

    private fun updatePlayer(dt: Float, input: InputState) {
        val b = player.bounds
        val reversed = player.controlsReversed > 0f
        val moveX = (if (reversed) -input.moveX else input.moveX).coerceIn(-1f, 1f)

        val speedMult = if (player.hasPower(PowerUp.SPEED)) GameConfig.SPEED_BOOST_MULT else 1f
        val maxSpeed = GameConfig.PLAYER_MAX_SPEED * speedMult

        if (abs(moveX) > 0.05f) {
            player.vx += moveX * GameConfig.PLAYER_ACCEL * dt
            player.vx = player.vx.coerceIn(-maxSpeed, maxSpeed)
            player.facing = if (moveX > 0) 1 else -1
        } else {
            val drop = GameConfig.PLAYER_FRICTION * dt
            player.vx = if (abs(player.vx) <= drop) 0f else player.vx - drop * sign(player.vx)
        }

        // jump buffering + coyote time make the controls feel responsive at 60fps
        if (input.jumpPressed) player.jumpBuffer = GameConfig.JUMP_BUFFER
        if (player.jumpBuffer > 0f) player.jumpBuffer -= dt
        if (player.coyoteTime > 0f) player.coyoteTime -= dt

        val maxJumps = if (player.hasPower(PowerUp.WINGS)) 2 else 1
        if (player.jumpBuffer > 0f) {
            val canGroundJump = player.onGround || player.coyoteTime > 0f
            if (canGroundJump) {
                player.vy = GameConfig.JUMP_VELOCITY
                player.onGround = false
                player.coyoteTime = 0f
                player.jumpBuffer = 0f
                player.jumpsLeft = maxJumps - 1
                player.squash = 1f
                events += GameEvent.Jump
            } else if (player.jumpsLeft > 0) {
                player.vy = GameConfig.JUMP_VELOCITY * 0.92f
                player.jumpsLeft--
                player.jumpBuffer = 0f
                player.squash = 1f
                events += GameEvent.DoubleJump
            }
        }

        if (input.attackPressed && player.attackTimer <= 0f) {
            player.attackTimer = GameConfig.ATTACK_TIME
            events += GameEvent.Attack
            performAttack()
        }

        // gravity
        player.vy += GameConfig.GRAVITY * dt
        if (player.vy < GameConfig.MAX_FALL_SPEED) player.vy = GameConfig.MAX_FALL_SPEED

        // integrate X and clamp to the tower walls
        b.x += player.vx * dt
        val minX = GameConfig.WALL_THICKNESS
        val maxX = GameConfig.WORLD_WIDTH - GameConfig.WALL_THICKNESS - b.w
        if (b.x < minX) {
            b.x = minX; player.vx = 0f
        }
        if (b.x > maxX) {
            b.x = maxX; player.vx = 0f
        }

        // integrate Y with one-way platform resolution
        val prevBottom = b.y
        b.y += player.vy * dt
        val wasOnGround = player.onGround
        player.onGround = false

        if (player.vy <= 0f) {
            var landed: Platform? = null
            var landedTop = Float.NEGATIVE_INFINITY
            forEachSolidPlatform { p ->
                val top = p.bounds.top
                if (prevBottom >= top - 0.6f && b.y <= top &&
                    b.right > p.bounds.left + 0.5f && b.left < p.bounds.right - 0.5f
                ) {
                    if (top > landedTop) {
                        landedTop = top
                        landed = p
                    }
                }
            }
            val platform = landed
            if (platform != null) {
                b.y = landedTop
                player.vy = 0f
                player.onGround = true
                player.jumpsLeft = if (player.hasPower(PowerUp.WINGS)) 1 else 0
                player.coyoteTime = GameConfig.COYOTE_TIME
                if (!wasOnGround) {
                    player.squash = 1f
                    events += GameEvent.Land
                }
                if (platform.kind == PlatformKind.CRUMBLING && platform.crumbleTimer == -1f) {
                    platform.crumbleTimer = GameConfig.CRUMBLE_DELAY
                }
                if (platform.kind == PlatformKind.MOVING) {
                    val t = (sin(platform.phase * Math.PI.toFloat() * 2f) + 1f) / 2f
                    val next = platform.moveFrom + (platform.moveTo - platform.moveFrom) * t
                    b.x = (b.x + (next - platform.bounds.x)).coerceIn(minX, maxX)
                }
            }
        }

        if (player.onGround) player.coyoteTime = GameConfig.COYOTE_TIME

        // boss gate: cannot leave the arena until the guardian falls
        val floor = floors[floorOf(b.centerY)]
        if (floor != null && floor.gateLocked) {
            val ceiling = floor.baseY + floor.height - GameConfig.PLAYER_H - 2f
            if (b.y > ceiling) {
                b.y = ceiling
                if (player.vy > 0f) player.vy = 0f
            }
        }

        // animation state
        player.state = when {
            player.attackTimer > 0f -> PlayerState.ATTACK
            player.invulnerable > GameConfig.INVULNERABLE_TIME - 0.25f -> PlayerState.HIT
            !player.onGround && player.vy > 0f -> PlayerState.JUMP
            !player.onGround -> PlayerState.FALL
            abs(player.vx) > 4f -> PlayerState.RUN
            else -> PlayerState.IDLE
        }
    }

    private inline fun forEachSolidPlatform(action: (Platform) -> Unit) {
        floors.values.forEach { floor ->
            floor.platforms.forEach { p ->
                if (!p.gone) action(p)
            }
        }
    }

    private fun performAttack() {
        val b = player.bounds
        val range = GameConfig.ATTACK_RANGE
        val hit = if (player.facing > 0) {
            Aabb(b.right - 1f, b.y, range, b.h)
        } else {
            Aabb(b.left + 1f - range, b.y, range, b.h)
        }
        forEachEnemy { enemy ->
            if (enemy.alive && enemy.bounds.intersects(hit)) {
                damageEnemy(enemy, 1)
            }
        }
    }

    private fun damageEnemy(enemy: Enemy, amount: Int) {
        enemy.health -= amount
        enemy.state = EnemyState.HIT
        enemy.stateTime = 0f
        screenShake = maxOf(screenShake, 0.35f)
        if (enemy.health <= 0) {
            enemy.alive = false
            enemy.state = EnemyState.DEATH
            enemy.deathTimer = 0.7f
            enemiesDefeated++
            events += GameEvent.EnemyDeath
            if (enemy.kind == EnemyKind.GATE_GUARDIAN || enemy.kind == EnemyKind.TOWER_LORD) {
                onBossDefeated(enemy)
            }
        }
    }

    private fun onBossDefeated(enemy: Enemy) {
        val floor = floors[floorOf(enemy.bounds.centerY)] ?: return
        floor.gateLocked = false
        events += GameEvent.BossDefeated
        coins += (25 * coinBonusMultiplier).toInt()
        floor.pickups += Pickup(
            PickupKind.GEM,
            Aabb(enemy.bounds.centerX - 3.5f, enemy.bounds.y + 6f, 7f, 7f),
        )
        if (enemy.kind == EnemyKind.TOWER_LORD && floor.number >= GameConfig.FINAL_FLOOR) {
            phase = RunPhase.VICTORY
            events += GameEvent.Victory
        }
    }

    // ---------------------------------------------------------------------------------------
    // Enemies
    // ---------------------------------------------------------------------------------------

    private inline fun forEachEnemy(action: (Enemy) -> Unit) {
        floors.values.forEach { floor -> floor.enemies.forEach(action) }
    }

    private fun updateEnemies(dt: Float) {
        floors.values.forEach { floor ->
            floor.enemies.forEach { enemy -> updateEnemy(enemy, floor, dt) }
            floor.enemies.removeAll { !it.alive && it.deathTimer <= 0f }
        }
        if (spawnedBats.isNotEmpty()) {
            spawnedBats.forEach { bat ->
                floors[floorOf(bat.bounds.centerY)]?.enemies?.add(bat)
            }
            spawnedBats.clear()
        }
    }

    private fun updateEnemy(enemy: Enemy, floor: FloorData, dt: Float) {
        enemy.animTime += dt
        enemy.stateTime += dt
        if (!enemy.alive) {
            enemy.deathTimer -= dt
            return
        }
        if (enemy.attackCooldown > 0f) enemy.attackCooldown -= dt
        val distX = player.bounds.centerX - enemy.bounds.centerX
        val distY = player.bounds.centerY - enemy.bounds.centerY
        val visible = abs(distY) < GameConfig.VIEWPORT_HEIGHT

        when (enemy.kind) {
            EnemyKind.STONE_SERPENT -> {
                enemy.state = EnemyState.MOVE
                enemy.bounds.x += enemy.speed * enemy.facing * dt
                if (enemy.bounds.x < enemy.patrolFrom) {
                    enemy.bounds.x = enemy.patrolFrom; enemy.facing = 1
                }
                if (enemy.bounds.x > enemy.patrolTo) {
                    enemy.bounds.x = enemy.patrolTo; enemy.facing = -1
                }
            }

            EnemyKind.EVIL_BAT -> {
                val diving = enemy.state == EnemyState.ATTACK
                if (!diving) {
                    enemy.state = EnemyState.MOVE
                    enemy.bounds.x += enemy.speed * enemy.facing * dt
                    if (enemy.bounds.x < enemy.patrolFrom) {
                        enemy.bounds.x = enemy.patrolFrom; enemy.facing = 1
                    }
                    if (enemy.bounds.x > enemy.patrolTo) {
                        enemy.bounds.x = enemy.patrolTo; enemy.facing = -1
                    }
                    enemy.bounds.y = enemy.baseY + sin(enemy.animTime * 2.4f) * 7f
                    if (visible && abs(distX) < 26f && distY < -6f && enemy.attackCooldown <= 0f) {
                        enemy.state = EnemyState.ATTACK
                        enemy.stateTime = 0f
                        enemy.vy = -1f
                    }
                } else {
                    // dive-bomb, then climb back to the patrol height
                    if (enemy.vy < 0f) {
                        enemy.bounds.y -= (enemy.speed * 2.4f) * dt
                        enemy.bounds.x += distX.coerceIn(-1f, 1f) * enemy.speed * 0.5f * dt
                        if (enemy.bounds.y <= player.bounds.y - 2f || enemy.stateTime > 1.1f) enemy.vy = 1f
                    } else {
                        enemy.bounds.y += (enemy.speed * 1.6f) * dt
                        if (enemy.bounds.y >= enemy.baseY) {
                            enemy.bounds.y = enemy.baseY
                            enemy.state = EnemyState.MOVE
                            enemy.attackCooldown = 2.2f
                        }
                    }
                }
            }

            EnemyKind.SKELETON_WARRIOR -> {
                if (visible && abs(distX) < 15f && abs(distY) < 12f) {
                    if (enemy.attackCooldown <= 0f) {
                        enemy.state = EnemyState.ATTACK
                        enemy.stateTime = 0f
                        enemy.attackCooldown = 1.6f
                    } else if (enemy.state != EnemyState.ATTACK || enemy.stateTime > 0.5f) {
                        enemy.state = EnemyState.IDLE
                    }
                    enemy.facing = if (distX > 0) 1 else -1
                } else {
                    enemy.state = EnemyState.MOVE
                    enemy.bounds.x += enemy.speed * enemy.facing * dt
                    if (enemy.bounds.x < enemy.patrolFrom) {
                        enemy.bounds.x = enemy.patrolFrom; enemy.facing = 1
                    }
                    if (enemy.bounds.x > enemy.patrolTo) {
                        enemy.bounds.x = enemy.patrolTo; enemy.facing = -1
                    }
                }
            }

            EnemyKind.SHADOW_WOLF -> {
                if (enemy.chaseTimer > 0f) {
                    enemy.chaseTimer -= dt
                    enemy.state = EnemyState.MOVE
                    enemy.facing = if (distX > 0) 1 else -1
                    enemy.bounds.x += enemy.speed * enemy.facing * dt
                    enemy.bounds.x = enemy.bounds.x.coerceIn(enemy.patrolFrom, enemy.patrolTo)
                } else {
                    enemy.state = EnemyState.IDLE
                    if (visible && abs(distX) < 55f && abs(distY) < 22f && enemy.attackCooldown <= 0f) {
                        enemy.chaseTimer = 3f
                        enemy.attackCooldown = 5f
                    }
                }
            }

            EnemyKind.GATE_GUARDIAN -> {
                updateBoss(enemy, floor, dt, distX, phases = 1)
            }

            EnemyKind.TOWER_LORD -> {
                updateBoss(enemy, floor, dt, distX, phases = 3)
            }
        }

        // enemies always rest on the floor they belong to (except flyers)
        if (enemy.kind != EnemyKind.EVIL_BAT && enemy.baseY > 0f) {
            enemy.bounds.y = enemy.baseY
        }
    }

    private fun updateBoss(enemy: Enemy, floor: FloorData, dt: Float, distX: Float, phases: Int) {
        val hpFraction = enemy.health.toFloat() / enemy.maxHealth.toFloat()
        enemy.phaseIndex = when {
            phases == 1 -> 0
            hpFraction > 0.66f -> 0
            hpFraction > 0.33f -> 1
            else -> 2
        }
        val aggression = 1f + enemy.phaseIndex * 0.45f

        when (enemy.state) {
            EnemyState.ATTACK -> {
                if (enemy.stateTime > 1.1f / aggression) {
                    enemy.state = EnemyState.MOVE
                    enemy.stateTime = 0f
                    enemy.attackCooldown = (2.4f / aggression)
                }
            }

            EnemyState.HIT -> {
                if (enemy.stateTime > 0.25f) enemy.state = EnemyState.MOVE
            }

            else -> {
                enemy.state = EnemyState.MOVE
                enemy.facing = if (distX > 0) 1 else -1
                enemy.bounds.x += enemy.speed * aggression * enemy.facing * dt
                enemy.bounds.x = enemy.bounds.x.coerceIn(enemy.patrolFrom, enemy.patrolTo)
                if (enemy.attackCooldown <= 0f && abs(distX) < 40f) {
                    enemy.state = EnemyState.ATTACK
                    enemy.stateTime = 0f
                    enemy.telegraph = 0.45f
                    events += GameEvent.BossRoar
                    screenShake = maxOf(screenShake, 0.5f)
                    // phase 2+ of the Lord of the Tower summons bats
                    if (enemy.kind == EnemyKind.TOWER_LORD && enemy.phaseIndex >= 1) {
                        spawnBat(floor)
                    }
                }
            }
        }
        if (enemy.telegraph > 0f) enemy.telegraph -= dt
    }

    private fun spawnBat(floor: FloorData) {
        if (floor.enemies.count { it.kind == EnemyKind.EVIL_BAT && it.alive } >= 3) return
        val w = 11f
        val x = GameConfig.WALL_THICKNESS + rng.nextFloat() *
            (GameConfig.WORLD_WIDTH - 2 * GameConfig.WALL_THICKNESS - w)
        val y = floor.baseY + 70f + rng.nextFloat() * 30f
        spawnedBats += Enemy(
            kind = EnemyKind.EVIL_BAT,
            bounds = Aabb(x, y, w, 9f),
            health = 1,
            patrolFrom = GameConfig.WALL_THICKNESS + 2f,
            patrolTo = GameConfig.WORLD_WIDTH - GameConfig.WALL_THICKNESS - w - 2f,
            speed = 28f,
            baseY = y,
        )
    }

    // ---------------------------------------------------------------------------------------
    // Pickups
    // ---------------------------------------------------------------------------------------

    private fun updatePickups(dt: Float) {
        val magnet = player.hasPower(PowerUp.MAGNET)
        floors.values.forEach { floor ->
            floor.pickups.forEach { pickup ->
                if (pickup.taken) return@forEach
                pickup.animTime += dt
                if (magnet && (pickup.kind == PickupKind.COIN || pickup.kind == PickupKind.GEM)) {
                    val dx = player.bounds.centerX - pickup.bounds.centerX
                    val dy = player.bounds.centerY - pickup.bounds.centerY
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist < GameConfig.MAGNET_RADIUS && dist > 0.01f) {
                        val pull = 120f * dt
                        pickup.bounds.x += dx / dist * pull
                        pickup.bounds.y += dy / dist * pull
                    }
                }
                if (pickup.bounds.intersects(player.bounds)) collect(pickup)
            }
            floor.pickups.removeAll { it.taken && it.animTime > 0f }
        }
    }

    private fun collect(pickup: Pickup) {
        pickup.taken = true
        when (pickup.kind) {
            PickupKind.COIN -> {
                coins += (1 * coinBonusMultiplier).toInt().coerceAtLeast(1)
                events += GameEvent.Coin
            }

            PickupKind.GEM -> {
                gems += 1
                events += GameEvent.Gem
            }

            PickupKind.HEART -> {
                if (player.health < player.maxHealth) player.health++
                events += GameEvent.PowerUpTaken
            }

            PickupKind.SHIELD -> grantPower(PowerUp.SHIELD)
            PickupKind.WINGS -> grantPower(PowerUp.WINGS)
            PickupKind.SPEED -> grantPower(PowerUp.SPEED)
            PickupKind.MAGNET -> grantPower(PowerUp.MAGNET)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Hazards & damage
    // ---------------------------------------------------------------------------------------

    private fun checkHazards() {
        val b = player.bounds

        floors.values.forEach { floor ->
            floor.traps.forEach { trap ->
                if (!trap.isActive) return@forEach
                val box = trap.hitBox()
                if (box.intersects(b)) {
                    if (trap.kind == TrapKind.SLEEP_GAS) {
                        if (player.controlsReversed <= 0f) {
                            player.controlsReversed = GameConfig.SLEEP_GAS_DURATION
                            events += GameEvent.TrapTrigger
                        }
                    } else {
                        damagePlayer()
                    }
                }
            }
        }

        forEachEnemy { enemy ->
            if (!enemy.alive) return@forEachEnemy
            if (!enemy.bounds.intersects(b)) return@forEachEnemy
            val stompable = enemy.kind != EnemyKind.GATE_GUARDIAN && enemy.kind != EnemyKind.TOWER_LORD
            val stomping = player.vy < 0f && b.bottom > enemy.bounds.centerY
            if (stompable && stomping) {
                damageEnemy(enemy, 1)
                player.vy = GameConfig.JUMP_VELOCITY * 0.7f
                player.squash = 1f
            } else {
                damagePlayer()
            }
        }
    }

    private fun damagePlayer() {
        if (player.invulnerable > 0f) return
        if (player.hasPower(PowerUp.SHIELD)) {
            player.powerUps.remove(PowerUp.SHIELD)
            player.invulnerable = GameConfig.INVULNERABLE_TIME
            events += GameEvent.PlayerHit
            screenShake = maxOf(screenShake, 0.5f)
            return
        }
        player.health--
        player.invulnerable = GameConfig.INVULNERABLE_TIME
        player.vy = 70f
        player.vx = -player.facing * 40f
        screenShake = maxOf(screenShake, 0.8f)
        events += GameEvent.PlayerHit
        if (player.health <= 0) startFalling()
    }

    /**
     * The signature "Evil Tower" moment: the hero plunges all the way back to the ground floor.
     * The run is over — only the permanent progression (coins, best floor) survives.
     */
    private fun startFalling() {
        if (phase != RunPhase.PLAYING) return
        phase = RunPhase.FALLING
        fallTimer = 0f
        player.state = PlayerState.DEATH
        events += GameEvent.Fall
        screenShake = 1f
    }

    private fun updateFalling(dt: Float) {
        fallTimer += dt
        player.animTime += dt
        player.vy = (player.vy + GameConfig.GRAVITY * dt * 1.6f).coerceAtLeast(-460f)
        player.bounds.y += player.vy * dt
        cameraY += (player.bounds.centerY - cameraY) * (1f - kotlin.math.exp(-14f * dt))
        if (fallTimer > 1.6f || player.bounds.y <= 0f) {
            player.bounds.y = 0f
            phase = RunPhase.GAME_OVER
        }
    }

    // ---------------------------------------------------------------------------------------
    // Progress & camera
    // ---------------------------------------------------------------------------------------

    private fun updateProgress() {
        val floorNow = floorOf(player.bounds.y + 1f)
        if (floorNow != currentFloor) {
            if (floorNow > currentFloor) events += GameEvent.FloorCleared(floorNow)
            currentFloor = floorNow
            if (floorNow > highestFloor) highestFloor = floorNow
        }
        // fell too far below the highest point reached -> the tower claims another soul
        val deathLine = (highestFloor - 1) * GameConfig.FLOOR_HEIGHT - GameConfig.FALL_DEATH_MARGIN
        if (player.bounds.top < deathLine) startFalling()
    }

    private fun updateCamera(dt: Float) {
        val target = player.bounds.centerY + GameConfig.VIEWPORT_HEIGHT * 0.12f
        cameraY += (target - cameraY) * (1f - kotlin.math.exp(-GameConfig.CAMERA_SMOOTH * dt))
    }
}
