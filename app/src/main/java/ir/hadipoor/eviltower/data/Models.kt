package ir.hadipoor.eviltower.data

/** Which control scheme the player picked in تنظیمات. */
enum class ControlScheme { SWIPE, BUTTONS, TILT }

/** Everything that survives between runs and app restarts. */
data class PlayerProfile(
    val coins: Int = 0,
    val gems: Int = 0,
    val bestScore: Int = 0,
    val bestFloor: Int = 0,
    val totalCoins: Int = 0,
    val totalRuns: Int = 0,
    val totalEnemies: Int = 0,
    val unlockedSkins: Set<String> = setOf("knight"),
    val unlockedThemes: Set<String> = setOf("classic"),
    val selectedSkin: String = "knight",
    val selectedTheme: String = "classic",
    val upgrades: Map<String, Int> = emptyMap(),
    val achievements: Set<String> = emptySet(),
    val adsRemoved: Boolean = false,
    /** Last five runs, newest first: "score:floor:coins". */
    val recentRuns: List<RunRecord> = emptyList(),
) {
    fun upgradeLevel(id: String): Int = upgrades[id] ?: 0
}

data class RunRecord(val score: Int, val floor: Int, val coins: Int, val timestamp: Long) {
    fun serialize() = "$score:$floor:$coins:$timestamp"

    companion object {
        fun parse(raw: String): RunRecord? {
            val parts = raw.split(":")
            if (parts.size != 4) return null
            return runCatching {
                RunRecord(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), parts[3].toLong())
            }.getOrNull()
        }
    }
}

data class GameSettings(
    val musicVolume: Float = 0.6f,
    val sfxVolume: Float = 0.9f,
    val controlScheme: ControlScheme = ControlScheme.SWIPE,
    val vibration: Boolean = true,
    val language: String = "fa",
    val screenShake: Boolean = true,
)

/** Summary handed to the game-over screen. */
data class RunResult(
    val floorsClimbed: Int,
    val coinsCollected: Int,
    val coinsKept: Int,
    val gemsCollected: Int,
    val score: Int,
    val enemiesDefeated: Int,
    val isNewBestScore: Boolean,
    val isNewBestFloor: Boolean,
    val victory: Boolean,
)
