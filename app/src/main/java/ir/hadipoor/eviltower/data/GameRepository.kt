package ir.hadipoor.eviltower.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eviltower_save")

/**
 * Offline-first persistence (DataStore Preferences). No login, no network, no Google Play
 * Services — a hard requirement for the Cafe Bazaar release.
 */
class GameRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val GEMS = intPreferencesKey("gems")
        val BEST_SCORE = intPreferencesKey("best_score")
        val BEST_FLOOR = intPreferencesKey("best_floor")
        val TOTAL_COINS = intPreferencesKey("total_coins")
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val TOTAL_ENEMIES = intPreferencesKey("total_enemies")
        val SKINS = stringSetPreferencesKey("unlocked_skins")
        val THEMES = stringSetPreferencesKey("unlocked_themes")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val UPGRADES = stringPreferencesKey("upgrades")
        val ACHIEVEMENTS = stringSetPreferencesKey("achievements")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val RECENT_RUNS = stringPreferencesKey("recent_runs")

        val MUSIC = floatPreferencesKey("music_volume")
        val SFX = floatPreferencesKey("sfx_volume")
        val CONTROLS = stringPreferencesKey("control_scheme")
        val VIBRATION = booleanPreferencesKey("vibration")
        val LANGUAGE = stringPreferencesKey("language")
        val SHAKE = booleanPreferencesKey("screen_shake")
    }

    private val safePrefs: Flow<Preferences> = context.dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val profile: Flow<PlayerProfile> = safePrefs.map { prefs ->
        PlayerProfile(
            coins = prefs[Keys.COINS] ?: 0,
            gems = prefs[Keys.GEMS] ?: 0,
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
            bestFloor = prefs[Keys.BEST_FLOOR] ?: 0,
            totalCoins = prefs[Keys.TOTAL_COINS] ?: 0,
            totalRuns = prefs[Keys.TOTAL_RUNS] ?: 0,
            totalEnemies = prefs[Keys.TOTAL_ENEMIES] ?: 0,
            unlockedSkins = prefs[Keys.SKINS] ?: setOf("knight"),
            unlockedThemes = prefs[Keys.THEMES] ?: setOf("classic"),
            selectedSkin = prefs[Keys.SELECTED_SKIN] ?: "knight",
            selectedTheme = prefs[Keys.SELECTED_THEME] ?: "classic",
            upgrades = decodeUpgrades(prefs[Keys.UPGRADES]),
            achievements = prefs[Keys.ACHIEVEMENTS] ?: emptySet(),
            adsRemoved = prefs[Keys.ADS_REMOVED] ?: false,
            recentRuns = (prefs[Keys.RECENT_RUNS] ?: "").split("|").mapNotNull(RunRecord::parse),
        )
    }

    val settings: Flow<GameSettings> = safePrefs.map { prefs ->
        GameSettings(
            musicVolume = prefs[Keys.MUSIC] ?: 0.6f,
            sfxVolume = prefs[Keys.SFX] ?: 0.9f,
            controlScheme = runCatching {
                ControlScheme.valueOf(prefs[Keys.CONTROLS] ?: ControlScheme.SWIPE.name)
            }.getOrDefault(ControlScheme.SWIPE),
            vibration = prefs[Keys.VIBRATION] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "fa",
            screenShake = prefs[Keys.SHAKE] ?: true,
        )
    }

    // ---------------------------------------------------------------- settings

    suspend fun setMusicVolume(value: Float) = edit { it[Keys.MUSIC] = value.coerceIn(0f, 1f) }
    suspend fun setSfxVolume(value: Float) = edit { it[Keys.SFX] = value.coerceIn(0f, 1f) }
    suspend fun setControlScheme(scheme: ControlScheme) = edit { it[Keys.CONTROLS] = scheme.name }
    suspend fun setVibration(enabled: Boolean) = edit { it[Keys.VIBRATION] = enabled }
    suspend fun setLanguage(language: String) = edit { it[Keys.LANGUAGE] = language }
    suspend fun setScreenShake(enabled: Boolean) = edit { it[Keys.SHAKE] = enabled }

    // ---------------------------------------------------------------- progression

    /** Stores the outcome of a finished run and returns the resulting summary. */
    suspend fun saveRun(
        floorsClimbed: Int,
        coinsCollected: Int,
        gemsCollected: Int,
        score: Int,
        enemiesDefeated: Int,
        victory: Boolean,
        keepAllCoins: Boolean = false,
    ): RunResult {
        var result: RunResult? = null
        edit { prefs ->
            val insurance = decodeUpgrades(prefs[Keys.UPGRADES])["coin_insurance"] ?: 0
            val keepFraction = if (keepAllCoins) 1f else ShopCatalog.coinKeepFraction(insurance)
            val kept = (coinsCollected * keepFraction).toInt()
            val bestScore = prefs[Keys.BEST_SCORE] ?: 0
            val bestFloor = prefs[Keys.BEST_FLOOR] ?: 0

            prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 0) + kept
            prefs[Keys.GEMS] = (prefs[Keys.GEMS] ?: 0) + gemsCollected
            prefs[Keys.TOTAL_COINS] = (prefs[Keys.TOTAL_COINS] ?: 0) + kept
            prefs[Keys.TOTAL_RUNS] = (prefs[Keys.TOTAL_RUNS] ?: 0) + 1
            prefs[Keys.TOTAL_ENEMIES] = (prefs[Keys.TOTAL_ENEMIES] ?: 0) + enemiesDefeated
            prefs[Keys.BEST_SCORE] = maxOf(bestScore, score)
            prefs[Keys.BEST_FLOOR] = maxOf(bestFloor, floorsClimbed)

            val runs = ((prefs[Keys.RECENT_RUNS] ?: "").split("|").mapNotNull(RunRecord::parse))
            val updated = (listOf(RunRecord(score, floorsClimbed, kept, System.currentTimeMillis())) + runs)
                .sortedByDescending { it.score }
                .take(10)
            prefs[Keys.RECENT_RUNS] = updated.joinToString("|") { it.serialize() }

            result = RunResult(
                floorsClimbed = floorsClimbed,
                coinsCollected = coinsCollected,
                coinsKept = kept,
                gemsCollected = gemsCollected,
                score = score,
                enemiesDefeated = enemiesDefeated,
                isNewBestScore = score > bestScore,
                isNewBestFloor = floorsClimbed > bestFloor,
                victory = victory,
            )
        }
        return result!!
    }

    /** Rewarded ad: double the coins of the last run. */
    suspend fun grantBonusCoins(amount: Int) = edit { prefs ->
        prefs[Keys.COINS] = (prefs[Keys.COINS] ?: 0) + amount
        prefs[Keys.TOTAL_COINS] = (prefs[Keys.TOTAL_COINS] ?: 0) + amount
    }

    suspend fun addGems(amount: Int) = edit { prefs ->
        prefs[Keys.GEMS] = ((prefs[Keys.GEMS] ?: 0) + amount).coerceAtLeast(0)
    }

    suspend fun setAdsRemoved(removed: Boolean) = edit { it[Keys.ADS_REMOVED] = removed }

    // ---------------------------------------------------------------- shop

    /** @return true when the purchase went through. */
    suspend fun purchase(entry: ShopEntry, level: Int = 0): Boolean {
        var success = false
        edit { prefs ->
            val price = when (entry) {
                is UpgradeEntry -> entry.priceFor(level)
                else -> entry.price
            }
            val coins = prefs[Keys.COINS] ?: 0
            val gems = prefs[Keys.GEMS] ?: 0
            val affordable = when (entry.currency) {
                Currency.COIN -> coins >= price
                Currency.GEM -> gems >= price
            }
            if (!affordable) return@edit

            when (entry.currency) {
                Currency.COIN -> prefs[Keys.COINS] = coins - price
                Currency.GEM -> prefs[Keys.GEMS] = gems - price
            }
            when (entry) {
                is SkinEntry -> {
                    prefs[Keys.SKINS] = (prefs[Keys.SKINS] ?: setOf("knight")) + entry.id
                    prefs[Keys.SELECTED_SKIN] = entry.id
                }

                is ThemeEntry -> {
                    prefs[Keys.THEMES] = (prefs[Keys.THEMES] ?: setOf("classic")) + entry.id
                    prefs[Keys.SELECTED_THEME] = entry.id
                }

                is UpgradeEntry -> {
                    val upgrades = decodeUpgrades(prefs[Keys.UPGRADES]).toMutableMap()
                    upgrades[entry.id] = (upgrades[entry.id] ?: 0) + 1
                    prefs[Keys.UPGRADES] = encodeUpgrades(upgrades)
                }
            }
            success = true
        }
        return success
    }

    suspend fun selectSkin(id: String) = edit { it[Keys.SELECTED_SKIN] = id }
    suspend fun selectTheme(id: String) = edit { it[Keys.SELECTED_THEME] = id }

    suspend fun markAchievements(ids: Collection<String>, rewardGems: Int) = edit { prefs ->
        prefs[Keys.ACHIEVEMENTS] = (prefs[Keys.ACHIEVEMENTS] ?: emptySet()) + ids
        if (rewardGems > 0) prefs[Keys.GEMS] = (prefs[Keys.GEMS] ?: 0) + rewardGems
    }

    suspend fun resetProgress() {
        context.dataStore.edit { it.clear() }
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun edit(
        block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        context.dataStore.edit(block)
    }

    private fun decodeUpgrades(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size != 2) return@mapNotNull null
            val level = parts[1].toIntOrNull() ?: return@mapNotNull null
            parts[0] to level
        }.toMap()
    }

    private fun encodeUpgrades(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}=${it.value}" }
}
