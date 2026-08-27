package ir.hadipoor.eviltower.game.engine

/**
 * Every tunable number of برج شیطانی lives here.
 * World units: the tower interior is [WORLD_WIDTH] wide, y grows upwards, one floor is
 * [FLOOR_HEIGHT] tall. The renderer scales world units to pixels so the game looks identical
 * on every screen size.
 */
object GameConfig {
    const val WORLD_WIDTH = 100f
    const val FLOOR_HEIGHT = 150f
    const val WALL_THICKNESS = 6f

    // Player -----------------------------------------------------------------
    const val PLAYER_W = 9f
    const val PLAYER_H = 14f
    const val PLAYER_ACCEL = 460f
    const val PLAYER_MAX_SPEED = 62f
    const val PLAYER_FRICTION = 420f
    const val JUMP_VELOCITY = 182f
    const val GRAVITY = -520f
    const val MAX_FALL_SPEED = -260f
    const val COYOTE_TIME = 0.10f
    const val JUMP_BUFFER = 0.12f
    const val INVULNERABLE_TIME = 1.15f
    const val ATTACK_TIME = 0.28f
    const val ATTACK_RANGE = 13f
    const val SPEED_BOOST_MULT = 1.45f

    // Traps / platforms ------------------------------------------------------
    const val CRUMBLE_DELAY = 0.45f
    const val CRUMBLE_RESPAWN = 3.5f
    const val SLEEP_GAS_DURATION = 3.0f

    // Power-ups --------------------------------------------------------------
    const val POWER_SHIELD_TIME = 999f
    const val POWER_WINGS_TIME = 9f
    const val POWER_SPEED_TIME = 8f
    const val POWER_MAGNET_TIME = 9f
    const val MAGNET_RADIUS = 46f

    // Progression ------------------------------------------------------------
    const val MINI_BOSS_EVERY = 10
    const val BOSS_EVERY = 25
    const val FINAL_FLOOR = 100
    const val START_HEALTH = 3

    // Camera -----------------------------------------------------------------
    const val CAMERA_SMOOTH = 9f
    const val VIEWPORT_HEIGHT = 178f

    /** Fall death threshold: this far below the floor the player last stood on. */
    const val FALL_DEATH_MARGIN = 1.35f * FLOOR_HEIGHT

    /** Difficulty ramps from 0f (floor 1) to 1f (floor [FINAL_FLOOR]). */
    fun difficulty(floor: Int): Float =
        ((floor - 1).toFloat() / (FINAL_FLOOR - 1).toFloat()).coerceIn(0f, 1f)

    fun scoreMultiplier(floor: Int): Int = 1 + floor / 10
}
