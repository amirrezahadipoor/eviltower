package ir.hadipoor.eviltower.game.engine

import ir.hadipoor.eviltower.game.model.Aabb
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnemyKind
import ir.hadipoor.eviltower.game.model.FloorData
import ir.hadipoor.eviltower.game.model.Pickup
import ir.hadipoor.eviltower.game.model.PickupKind
import ir.hadipoor.eviltower.game.model.Platform
import ir.hadipoor.eviltower.game.model.PlatformKind
import ir.hadipoor.eviltower.game.model.Trap
import ir.hadipoor.eviltower.game.model.TrapKind
import kotlin.math.max
import kotlin.random.Random

/**
 * Procedural floor generation.
 *
 * A floor is a small vertical platforming section: a ground slab at `baseY`, [STEPS] staggered
 * climbing platforms 24 world-units apart (the player's max jump height is ~31 units, so every
 * step is always reachable), and the ground slab of the next floor on top.
 *
 * Difficulty (see [GameConfig.difficulty]) widens gaps, narrows platforms, speeds traps up,
 * adds enemies and darkens the lighting as the floor number grows.
 *
 * ### Adding a new floor template
 * 1. add an entry to [FloorTemplate];
 * 2. give it a Persian [FloorTemplate.persianName];
 * 3. implement its decoration inside [decorate];
 * 4. add it to [pickTemplate]'s weighted table.
 * Everything else (reachability, coins, camera, collision) keeps working automatically.
 */
object FloorGenerator {

    const val STEPS = 5
    const val STEP_GAP = 24f

    enum class FloorTemplate(val persianName: String) {
        STAIRWAY("پلکان سنگی"),
        BROKEN_BRIDGE("پل شکسته"),
        CRUMBLING_PATH("گذرگاه شکننده"),
        BLADE_CORRIDOR("راهروی تیغه‌ها"),
        FIRE_HALL("تالار آتش"),
        CRUSHER_GATE("دروازه له‌کننده"),
        MOVING_LEDGES("سکوهای لرزان"),
        SERPENT_NEST("لانه مار سنگی"),
        BAT_CAVE("غار خفاش‌ها"),
        WOLF_DEN("کنام گرگ سایه"),
        SLEEP_HALL("تالار خواب‌آور"),
        GUARDIAN_ARENA("میدان دروازه‌بان"),
        LORD_ARENA("تالار ارباب برج"),
    }

    fun isMiniBossFloor(floor: Int) =
        floor % GameConfig.MINI_BOSS_EVERY == 0 && floor % GameConfig.BOSS_EVERY != 0

    fun isBossFloor(floor: Int) = floor % GameConfig.BOSS_EVERY == 0

    fun pickTemplate(floor: Int, rng: Random): FloorTemplate {
        if (isBossFloor(floor)) return FloorTemplate.LORD_ARENA
        if (isMiniBossFloor(floor)) return FloorTemplate.GUARDIAN_ARENA
        if (floor <= 2) return FloorTemplate.STAIRWAY

        val diff = GameConfig.difficulty(floor)
        // weight = how often the template shows up; harder templates ramp up with difficulty.
        val weighted = listOf(
            FloorTemplate.STAIRWAY to 14f - 8f * diff,
            FloorTemplate.BROKEN_BRIDGE to 10f + 2f * diff,
            FloorTemplate.CRUMBLING_PATH to 6f + 8f * diff,
            FloorTemplate.BLADE_CORRIDOR to 5f + 9f * diff,
            FloorTemplate.FIRE_HALL to 4f + 9f * diff,
            FloorTemplate.CRUSHER_GATE to 2f + 9f * diff,
            FloorTemplate.MOVING_LEDGES to 5f + 6f * diff,
            FloorTemplate.SERPENT_NEST to 7f + 4f * diff,
            FloorTemplate.BAT_CAVE to 5f + 7f * diff,
            FloorTemplate.WOLF_DEN to 3f + 8f * diff,
            FloorTemplate.SLEEP_HALL to 2f + 7f * diff,
        )
        val total = weighted.sumOf { it.second.toDouble() }.toFloat()
        var roll = rng.nextFloat() * total
        for ((template, w) in weighted) {
            roll -= w
            if (roll <= 0f) return template
        }
        return FloorTemplate.STAIRWAY
    }

    /** Deterministic: the same [runSeed] + [floor] always yields the same layout. */
    fun generate(floor: Int, runSeed: Long): FloorData {
        val rng = Random(runSeed * 7919L + floor * 104729L)
        val template = pickTemplate(floor, rng)
        val diff = GameConfig.difficulty(floor)
        val baseY = (floor - 1) * GameConfig.FLOOR_HEIGHT
        val darkness = when (template) {
            FloorTemplate.BAT_CAVE -> (0.35f + 0.4f * diff).coerceAtMost(0.8f)
            FloorTemplate.LORD_ARENA -> 0.35f
            else -> (0.06f + 0.42f * diff).coerceAtMost(0.55f)
        }

        val data = FloorData(
            number = floor,
            templateName = template.persianName,
            baseY = baseY,
            height = GameConfig.FLOOR_HEIGHT,
            darkness = darkness,
            isMiniBoss = isMiniBossFloor(floor),
            isBoss = isBossFloor(floor),
            gateLocked = isMiniBossFloor(floor) || isBossFloor(floor),
        )

        buildGround(data, template, floor, diff, rng)
        buildSteps(data, template, floor, diff, rng)
        decorate(data, template, floor, diff, rng)
        return data
    }

    // ---------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------

    private fun buildGround(
        data: FloorData,
        template: FloorTemplate,
        floor: Int,
        diff: Float,
        rng: Random,
    ) {
        val y = data.baseY
        val inner = GameConfig.WALL_THICKNESS
        val innerWidth = GameConfig.WORLD_WIDTH - 2 * inner
        val pitAllowed = floor > 2 && !data.isBoss && !data.isMiniBoss
        val pitWidth = when {
            !pitAllowed -> 0f
            template == FloorTemplate.BROKEN_BRIDGE -> 18f + 12f * diff
            rng.nextFloat() < 0.35f -> 12f + 10f * diff
            else -> 0f
        }
        if (pitWidth <= 0f) {
            data.platforms += Platform(Aabb(inner, y - 6f, innerWidth, 6f), PlatformKind.STONE)
        } else {
            val pitX = inner + 14f + rng.nextFloat() * (innerWidth - pitWidth - 28f).coerceAtLeast(1f)
            data.platforms += Platform(Aabb(inner, y - 6f, pitX - inner, 6f), PlatformKind.STONE)
            data.platforms += Platform(
                Aabb(pitX + pitWidth, y - 6f, GameConfig.WORLD_WIDTH - inner - (pitX + pitWidth), 6f),
                PlatformKind.STONE,
            )
        }
    }

    private fun buildSteps(
        data: FloorData,
        template: FloorTemplate,
        floor: Int,
        diff: Float,
        rng: Random,
    ) {
        if (template == FloorTemplate.GUARDIAN_ARENA || template == FloorTemplate.LORD_ARENA) {
            buildArenaSteps(data, diff, rng)
            return
        }
        val inner = GameConfig.WALL_THICKNESS
        val usable = GameConfig.WORLD_WIDTH - 2 * inner
        var side = if (rng.nextBoolean()) 0 else 1
        for (i in 1..STEPS) {
            val y = data.baseY + i * STEP_GAP
            val width = (34f - 13f * diff - rng.nextFloat() * 5f).coerceIn(15f, 34f)
            // alternate sides so the climb zig-zags; jitter keeps it from feeling robotic
            val leftBias = if (side == 0) 0f else usable - width
            val jitter = (rng.nextFloat() - 0.5f) * (usable - width) * 0.35f
            val x = (inner + leftBias + jitter).coerceIn(inner, GameConfig.WORLD_WIDTH - inner - width)
            val kind = when {
                template == FloorTemplate.CRUMBLING_PATH && i % 2 == 1 -> PlatformKind.CRUMBLING
                template == FloorTemplate.CRUMBLING_PATH && rng.nextFloat() < 0.3f -> PlatformKind.CRUMBLING
                template == FloorTemplate.MOVING_LEDGES && i % 2 == 0 -> PlatformKind.MOVING
                floor > 6 && rng.nextFloat() < 0.10f + 0.12f * diff -> PlatformKind.CRUMBLING
                else -> PlatformKind.STONE
            }
            val platform = if (kind == PlatformKind.MOVING) {
                val from = inner
                val to = GameConfig.WORLD_WIDTH - inner - width
                Platform(
                    bounds = Aabb(x, y, width, 4f),
                    kind = kind,
                    moveFrom = from,
                    moveTo = to,
                    moveSpeed = 16f + 18f * diff,
                    phase = rng.nextFloat(),
                )
            } else {
                Platform(Aabb(x, y, width, 4f), kind)
            }
            data.platforms += platform
            side = 1 - side

            // coins float above most steps
            if (rng.nextFloat() < 0.55f) {
                data.pickups += Pickup(
                    PickupKind.COIN,
                    Aabb(x + width / 2f - 3f, y + 9f, 6f, 6f),
                )
            }
        }
        // one guaranteed safe landing pad right under the next floor's entrance
        val topY = data.baseY + (STEPS + 1) * STEP_GAP - 6f
        if (topY < data.baseY + data.height) {
            val w = 26f
            val x = (GameConfig.WORLD_WIDTH - w) / 2f + (rng.nextFloat() - 0.5f) * 20f
            data.platforms += Platform(
                Aabb(x.coerceIn(inner, GameConfig.WORLD_WIDTH - inner - w), topY, w, 4f),
                PlatformKind.STONE,
            )
        }
    }

    private fun buildArenaSteps(data: FloorData, diff: Float, rng: Random) {
        val inner = GameConfig.WALL_THICKNESS
        val usable = GameConfig.WORLD_WIDTH - 2 * inner
        // Boss arenas are open rooms with two side ledges and a top exit ledge.
        data.platforms += Platform(Aabb(inner, data.baseY + 34f, usable * 0.28f, 4f))
        data.platforms += Platform(
            Aabb(GameConfig.WORLD_WIDTH - inner - usable * 0.28f, data.baseY + 34f, usable * 0.28f, 4f)
        )
        data.platforms += Platform(Aabb(inner + usable * 0.34f, data.baseY + 64f, usable * 0.32f, 4f))
        data.platforms += Platform(Aabb(inner, data.baseY + 94f, usable * 0.26f, 4f))
        data.platforms += Platform(
            Aabb(GameConfig.WORLD_WIDTH - inner - usable * 0.26f, data.baseY + 94f, usable * 0.26f, 4f)
        )
        val w = 30f
        data.platforms += Platform(
            Aabb((GameConfig.WORLD_WIDTH - w) / 2f, data.baseY + 124f, w, 4f),
            PlatformKind.STONE,
        )
        if (rng.nextFloat() < 0.6f + diff * 0.2f) {
            data.pickups += Pickup(PickupKind.HEART, Aabb(inner + 4f, data.baseY + 44f, 7f, 7f))
        }
    }

    // ---------------------------------------------------------------------------------------
    // Template decoration: traps, enemies, treasure
    // ---------------------------------------------------------------------------------------

    private fun decorate(
        data: FloorData,
        template: FloorTemplate,
        floor: Int,
        diff: Float,
        rng: Random,
    ) {
        val inner = GameConfig.WALL_THICKNESS
        val stepPlatforms = data.platforms.filter { it.bounds.y > data.baseY }

        when (template) {
            FloorTemplate.STAIRWAY -> {
                if (floor > 3) spawnEnemy(data, EnemyKind.STONE_SERPENT, data.baseY, rng, diff)
            }

            FloorTemplate.BROKEN_BRIDGE -> {
                data.traps += Trap(
                    kind = TrapKind.SPIKES,
                    bounds = Aabb(inner, data.baseY - 6f, GameConfig.WORLD_WIDTH - 2 * inner, 3f),
                )
                if (floor > 5) spawnEnemy(data, EnemyKind.EVIL_BAT, data.baseY + 60f, rng, diff)
            }

            FloorTemplate.CRUMBLING_PATH -> {
                if (rng.nextFloat() < 0.5f) {
                    spawnEnemy(data, EnemyKind.EVIL_BAT, data.baseY + 70f, rng, diff)
                }
            }

            FloorTemplate.BLADE_CORRIDOR -> {
                val count = 1 + (diff * 3).toInt()
                repeat(count) { i ->
                    val y = data.baseY + 18f + i * 32f + rng.nextFloat() * 8f
                    val size = 13f
                    val x = inner + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - size)
                    data.traps += Trap(
                        kind = TrapKind.SPINNING_BLADE,
                        bounds = Aabb(x, y, size, size),
                        period = (1.6f - 0.9f * diff).coerceAtLeast(0.45f),
                        travel = 18f + 20f * diff,
                    )
                }
            }

            FloorTemplate.FIRE_HALL -> {
                val jets = 2 + (diff * 3).toInt()
                repeat(jets) { i ->
                    val x = inner + 8f + i * ((GameConfig.WORLD_WIDTH - 2 * inner - 16f) / jets)
                    data.traps += Trap(
                        kind = TrapKind.FIRE_JET,
                        bounds = Aabb(x, data.baseY, 8f, 26f + 12f * diff),
                        period = (2.4f - 1.1f * diff).coerceAtLeast(0.9f),
                        activeFraction = 0.35f,
                        timer = rng.nextFloat() * 2f,
                    )
                }
                if (floor > 8) spawnEnemy(data, EnemyKind.SKELETON_WARRIOR, data.baseY, rng, diff)
            }

            FloorTemplate.CRUSHER_GATE -> {
                val n = 1 + (diff * 2).toInt()
                repeat(n) { i ->
                    val x = inner + 12f + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 34f)
                    val y = data.baseY + 26f + i * 44f
                    data.traps += Trap(
                        kind = TrapKind.CRUSHER,
                        bounds = Aabb(x, y, 22f, 22f + 10f * diff),
                        period = (2.6f - 1.2f * diff).coerceAtLeast(1.0f),
                        timer = rng.nextFloat() * 2f,
                    )
                }
            }

            FloorTemplate.MOVING_LEDGES -> {
                if (floor > 6) spawnEnemy(data, EnemyKind.EVIL_BAT, data.baseY + 80f, rng, diff)
            }

            FloorTemplate.SERPENT_NEST -> {
                spawnEnemy(data, EnemyKind.STONE_SERPENT, data.baseY, rng, diff)
                val target = stepPlatforms.randomOrNull(rng)
                if (target != null) {
                    data.enemies += serpentOn(target.bounds, diff)
                }
                if (floor > 12) spawnEnemy(data, EnemyKind.SKELETON_WARRIOR, data.baseY, rng, diff)
            }

            FloorTemplate.BAT_CAVE -> {
                val count = 2 + (diff * 3).toInt()
                repeat(count) { i ->
                    spawnEnemy(data, EnemyKind.EVIL_BAT, data.baseY + 30f + i * 26f, rng, diff)
                }
            }

            FloorTemplate.WOLF_DEN -> {
                spawnEnemy(data, EnemyKind.SHADOW_WOLF, data.baseY, rng, diff)
                if (diff > 0.4f) spawnEnemy(data, EnemyKind.SHADOW_WOLF, data.baseY + 72f, rng, diff)
            }

            FloorTemplate.SLEEP_HALL -> {
                repeat(1 + (diff * 2).toInt()) {
                    val x = inner + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 24f)
                    data.traps += Trap(
                        kind = TrapKind.SLEEP_GAS,
                        bounds = Aabb(x, data.baseY + 8f + rng.nextFloat() * 90f, 24f, 18f),
                        period = 3.4f,
                        activeFraction = 0.5f,
                        timer = rng.nextFloat() * 3f,
                    )
                }
                if (floor > 10) spawnEnemy(data, EnemyKind.SKELETON_WARRIOR, data.baseY, rng, diff)
            }

            FloorTemplate.GUARDIAN_ARENA -> {
                val w = 20f
                val h = 24f
                data.enemies += Enemy(
                    kind = EnemyKind.GATE_GUARDIAN,
                    bounds = Aabb((GameConfig.WORLD_WIDTH - w) / 2f, data.baseY, w, h),
                    health = 4 + floor / 10,
                    patrolFrom = inner + 4f,
                    patrolTo = GameConfig.WORLD_WIDTH - inner - w - 4f,
                    speed = 20f + 12f * diff,
                    baseY = data.baseY,
                ).also { it.maxHealth = it.health }
                data.pickups += Pickup(
                    PickupKind.GEM,
                    Aabb(GameConfig.WORLD_WIDTH / 2f - 3.5f, data.baseY + 104f, 7f, 7f),
                )
            }

            FloorTemplate.LORD_ARENA -> {
                val w = 26f
                val h = 34f
                data.enemies += Enemy(
                    kind = EnemyKind.TOWER_LORD,
                    bounds = Aabb((GameConfig.WORLD_WIDTH - w) / 2f, data.baseY, w, h),
                    health = 10 + floor / 5,
                    patrolFrom = inner + 4f,
                    patrolTo = GameConfig.WORLD_WIDTH - inner - w - 4f,
                    speed = 24f + 16f * diff,
                    baseY = data.baseY,
                ).also { it.maxHealth = it.health }
                data.pickups += Pickup(
                    PickupKind.GEM,
                    Aabb(GameConfig.WORLD_WIDTH / 2f - 3.5f, data.baseY + 108f, 7f, 7f),
                )
            }
        }

        // Treasure & power-ups sprinkled through the tower.
        if (!data.isBoss && !data.isMiniBoss) {
            if (rng.nextFloat() < 0.05f + 0.03f * diff) {
                data.pickups += Pickup(
                    PickupKind.GEM,
                    Aabb(
                        inner + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 7f),
                        data.baseY + 20f + rng.nextFloat() * 100f,
                        7f, 7f,
                    ),
                )
            }
            if (rng.nextFloat() < 0.16f) {
                val kind = listOf(
                    PickupKind.SHIELD, PickupKind.WINGS, PickupKind.SPEED, PickupKind.MAGNET,
                ).random(rng)
                data.pickups += Pickup(
                    kind,
                    Aabb(
                        inner + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 8f),
                        data.baseY + 24f + rng.nextFloat() * 96f,
                        8f, 8f,
                    ),
                )
            }
            if (floor > 4 && rng.nextFloat() < 0.07f) {
                data.pickups += Pickup(
                    PickupKind.HEART,
                    Aabb(
                        inner + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 7f),
                        data.baseY + 30f + rng.nextFloat() * 80f,
                        7f, 7f,
                    ),
                )
            }
            // a few loose coins in the air
            repeat(rng.nextInt(1, 4)) {
                data.pickups += Pickup(
                    PickupKind.COIN,
                    Aabb(
                        inner + 4f + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 2 * inner - 14f),
                        data.baseY + 14f + rng.nextFloat() * 118f,
                        6f, 6f,
                    ),
                )
            }
        }
    }

    private fun serpentOn(platform: Aabb, diff: Float): Enemy {
        val w = 13f
        val h = 6f
        return Enemy(
            kind = EnemyKind.STONE_SERPENT,
            bounds = Aabb(platform.x + 1f, platform.top, w, h),
            health = 1,
            patrolFrom = platform.left,
            patrolTo = max(platform.left, platform.right - w),
            speed = 14f + 14f * diff,
            baseY = platform.top,
        )
    }

    private fun spawnEnemy(
        data: FloorData,
        kind: EnemyKind,
        y: Float,
        rng: Random,
        diff: Float,
    ) {
        val inner = GameConfig.WALL_THICKNESS
        val (w, h) = when (kind) {
            EnemyKind.STONE_SERPENT -> 13f to 6f
            EnemyKind.EVIL_BAT -> 11f to 9f
            EnemyKind.SKELETON_WARRIOR -> 10f to 14f
            EnemyKind.SHADOW_WOLF -> 15f to 9f
            EnemyKind.GATE_GUARDIAN -> 20f to 24f
            EnemyKind.TOWER_LORD -> 26f to 34f
        }
        val from = inner + 2f
        val to = GameConfig.WORLD_WIDTH - inner - w - 2f
        val x = from + rng.nextFloat() * (to - from).coerceAtLeast(1f)
        val speed = when (kind) {
            EnemyKind.STONE_SERPENT -> 13f + 12f * diff
            EnemyKind.EVIL_BAT -> 20f + 20f * diff
            EnemyKind.SKELETON_WARRIOR -> 11f + 10f * diff
            EnemyKind.SHADOW_WOLF -> 30f + 26f * diff
            else -> 20f
        }
        val hp = when (kind) {
            EnemyKind.SKELETON_WARRIOR -> 2
            EnemyKind.SHADOW_WOLF -> 2
            else -> 1
        }
        data.enemies += Enemy(
            kind = kind,
            bounds = Aabb(x, y, w, h),
            health = hp,
            patrolFrom = from,
            patrolTo = to,
            speed = speed,
            baseY = y,
        ).also { it.maxHealth = it.health }
    }
}
