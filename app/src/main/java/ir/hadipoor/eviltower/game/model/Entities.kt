package ir.hadipoor.eviltower.game.model

/** Simple 2D vector in world units. */
data class Vec2(var x: Float = 0f, var y: Float = 0f)

/**
 * Axis-aligned bounding box used for every collision in the game.
 * [x],[y] is the *bottom-left* corner; y grows upwards (world space).
 */
data class Aabb(
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
) {
    val left get() = x
    val right get() = x + w
    val bottom get() = y
    val top get() = y + h
    val centerX get() = x + w / 2f
    val centerY get() = y + h / 2f

    fun intersects(other: Aabb): Boolean =
        left < other.right && right > other.left && bottom < other.top && top > other.bottom

    fun contains(px: Float, py: Float): Boolean =
        px in left..right && py in bottom..top

    fun copyMoved(dx: Float, dy: Float) = Aabb(x + dx, y + dy, w, h)
}

/** Kind of platform tile. */
enum class PlatformKind {
    /** Ordinary stone slab. */
    STONE,

    /** تخته شکننده — collapses shortly after being stepped on. */
    CRUMBLING,

    /** سکوی متحرک — moves horizontally between two bounds. */
    MOVING,

    /** Solid side wall of the tower. */
    WALL,
}

class Platform(
    val bounds: Aabb,
    val kind: PlatformKind = PlatformKind.STONE,
    /** Movement range for [PlatformKind.MOVING]. */
    val moveFrom: Float = 0f,
    val moveTo: Float = 0f,
    val moveSpeed: Float = 0f,
    var phase: Float = 0f,
) {
    /** Crumbling state: -1 = untouched, otherwise seconds left until it disappears. */
    var crumbleTimer: Float = -1f
    var gone: Boolean = false
    var shakeSeed: Float = 0f
}

enum class TrapKind {
    /** تیغ چرخان */
    SPINNING_BLADE,

    /** تله آتش */
    FIRE_JET,

    /** دیوار متحرک */
    CRUSHER,

    /** تله خواب‌آور */
    SLEEP_GAS,

    /** خارهای زمینی */
    SPIKES,
}

class Trap(
    val kind: TrapKind,
    val bounds: Aabb,
    /** Seconds for one full cycle (blade rotation, fire burst, crusher travel). */
    val period: Float = 2f,
    /** Fraction of the cycle the trap is lethal (fire jets / crushers). */
    val activeFraction: Float = 0.4f,
    val travel: Float = 0f,
    var timer: Float = 0f,
) {
    /** Current animation phase in 0..1. */
    val phase: Float get() = if (period <= 0f) 0f else (timer % period) / period

    val isActive: Boolean
        get() = when (kind) {
            TrapKind.SPINNING_BLADE, TrapKind.SPIKES -> true
            TrapKind.FIRE_JET -> phase < activeFraction
            TrapKind.CRUSHER -> true
            TrapKind.SLEEP_GAS -> phase < activeFraction
        }

    /** Live hit box (crushers and fire jets change shape over their cycle). */
    fun hitBox(): Aabb = when (kind) {
        TrapKind.CRUSHER -> {
            val t = kotlin.math.abs(kotlin.math.sin(phase * Math.PI.toFloat() * 2f))
            Aabb(bounds.x, bounds.y, bounds.w, bounds.h * (0.25f + 0.75f * t))
        }

        TrapKind.FIRE_JET -> {
            val grow = (phase / activeFraction).coerceIn(0f, 1f)
            Aabb(bounds.x, bounds.y, bounds.w, bounds.h * grow)
        }

        else -> bounds
    }
}

enum class EnemyKind(val persianName: String) {
    STONE_SERPENT("مار سنگی"),
    EVIL_BAT("خفاش شیطانی"),
    SKELETON_WARRIOR("اسکلت جنگجو"),
    SHADOW_WOLF("گرگ سایه"),
    GATE_GUARDIAN("دیو دروازه‌بان"),
    TOWER_LORD("ارباب برج"),
}

enum class EnemyState { IDLE, MOVE, ATTACK, HIT, DEATH }

class Enemy(
    val kind: EnemyKind,
    val bounds: Aabb,
    var health: Int = 1,
    val patrolFrom: Float = 0f,
    val patrolTo: Float = 0f,
    var speed: Float = 20f,
    val baseY: Float = 0f,
) {
    var state: EnemyState = EnemyState.IDLE
    var stateTime: Float = 0f
    var facing: Int = -1
    var animTime: Float = 0f
    var alive: Boolean = true
    var chaseTimer: Float = 0f
    var attackCooldown: Float = 0f
    var telegraph: Float = 0f
    var phaseIndex: Int = 0
    var maxHealth: Int = health
    var deathTimer: Float = 0f
    var vy: Float = 0f
}

enum class PickupKind { COIN, GEM, SHIELD, WINGS, SPEED, MAGNET, HEART }

class Pickup(
    val kind: PickupKind,
    val bounds: Aabb,
    var taken: Boolean = false,
    var animTime: Float = 0f,
) {
    var vx: Float = 0f
    var vy: Float = 0f
}

enum class PlayerState { IDLE, RUN, JUMP, FALL, ATTACK, HIT, DEATH }

/** Temporary, in-run power-ups. */
enum class PowerUp(val persianName: String) {
    SHIELD("سپر محافظ"),
    WINGS("بال پرواز"),
    SPEED("سرعت"),
    MAGNET("مغناطیس سکه"),
}

class Player(
    val bounds: Aabb,
    var health: Int = 3,
    var maxHealth: Int = 3,
) {
    var vx: Float = 0f
    var vy: Float = 0f
    var onGround: Boolean = false
    var state: PlayerState = PlayerState.IDLE
    var facing: Int = 1
    var animTime: Float = 0f
    var invulnerable: Float = 0f
    var attackTimer: Float = 0f
    var jumpsLeft: Int = 1
    var coyoteTime: Float = 0f
    var jumpBuffer: Float = 0f
    var squash: Float = 0f
    var controlsReversed: Float = 0f
    val powerUps = mutableMapOf<PowerUp, Float>()

    fun hasPower(p: PowerUp) = (powerUps[p] ?: 0f) > 0f
}

/** One generated floor of the tower. */
class FloorData(
    val number: Int,
    val templateName: String,
    val baseY: Float,
    val height: Float,
    val platforms: MutableList<Platform> = mutableListOf(),
    val traps: MutableList<Trap> = mutableListOf(),
    val enemies: MutableList<Enemy> = mutableListOf(),
    val pickups: MutableList<Pickup> = mutableListOf(),
    /** 0f = torch-lit, 1f = pitch black. */
    val darkness: Float = 0f,
    val isMiniBoss: Boolean = false,
    val isBoss: Boolean = false,
    /** Exit gate opens once the boss is defeated. */
    var gateLocked: Boolean = false,
)

/** High-level run status. */
enum class RunPhase { READY, PLAYING, PAUSED, FALLING, GAME_OVER, VICTORY }

/** Events emitted by the engine each frame, consumed by audio/haptics/UI. */
sealed interface GameEvent {
    data object Jump : GameEvent
    data object DoubleJump : GameEvent
    data object Land : GameEvent
    data object Coin : GameEvent
    data object Gem : GameEvent
    data object PowerUpTaken : GameEvent
    data object PlayerHit : GameEvent
    data object EnemyDeath : GameEvent
    data object Attack : GameEvent
    data object TrapTrigger : GameEvent
    data object Crumble : GameEvent
    data class FloorCleared(val floor: Int) : GameEvent
    data object BossRoar : GameEvent
    data object BossDefeated : GameEvent
    data object Fall : GameEvent
    data object Victory : GameEvent
}
