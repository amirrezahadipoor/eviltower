package ir.hadipoor.eviltower.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.hadipoor.eviltower.game.model.GameSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.profileStore by preferencesDataStore("evil_tower_profile")

data class RunRecord(val wave: Int, val date: String)
data class ProfileData(
    val metaCoins: Int = 0,
    val gems: Int = 40,
    val bestWave: Int = 0,
    val totalEnemies: Int = 0,
    val totalBosses: Int = 0,
    val history: List<RunRecord> = emptyList(),
    val arcaneUnlocked: Boolean = false,
    val startingGoldBonus: Int = 0,
    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val vibrationOn: Boolean = true,
    val lowGraphics: Boolean = false,
)

class GameRepository(private val context: Context) {
    private object K {
        val coins = intPreferencesKey("meta_coins")
        val gems = intPreferencesKey("gems")
        val best = intPreferencesKey("best_wave")
        val enemies = intPreferencesKey("total_enemies")
        val bosses = intPreferencesKey("total_bosses")
        val history = stringPreferencesKey("history")
        val arcane = booleanPreferencesKey("arcane_unlocked")
        val goldBonus = intPreferencesKey("starting_gold_bonus")
        val sound = booleanPreferencesKey("sound_on")
        val music = booleanPreferencesKey("music_on")
        val vibration = booleanPreferencesKey("vibration_on")
        val lowGraphics = booleanPreferencesKey("low_graphics")
    }

    val profile: Flow<ProfileData> = context.profileStore.data.map { p ->
        ProfileData(
            metaCoins = p[K.coins] ?: 0,
            gems = p[K.gems] ?: 40,
            bestWave = p[K.best] ?: 0,
            totalEnemies = p[K.enemies] ?: 0,
            totalBosses = p[K.bosses] ?: 0,
            history = decode(p[K.history].orEmpty()),
            arcaneUnlocked = p[K.arcane] ?: false,
            startingGoldBonus = p[K.goldBonus] ?: 0,
            soundOn = p[K.sound] ?: true,
            musicOn = p[K.music] ?: true,
            vibrationOn = p[K.vibration] ?: true,
            lowGraphics = p[K.lowGraphics] ?: false,
        )
    }

    suspend fun saveRun(snapshot: GameSnapshot) {
        context.profileStore.edit { p ->
            val oldBest = p[K.best] ?: 0
            val newBest = maxOf(oldBest, snapshot.bestWave)
            p[K.best] = newBest
            p[K.enemies] = (p[K.enemies] ?: 0) + snapshot.enemiesDefeated
            p[K.bosses] = (p[K.bosses] ?: 0) + snapshot.wave / 10
            p[K.coins] = (p[K.coins] ?: 0) + (snapshot.goldEarned * .40f).toInt()
            p[K.gems] = (p[K.gems] ?: 40) + snapshot.gems
            val records = decode(p[K.history].orEmpty()).toMutableList()
            records.add(0, RunRecord(snapshot.bestWave, now()))
            p[K.history] = encode(records.take(10))
        }
    }

    suspend fun buyArcane() { context.profileStore.edit { p -> if ((p[K.coins] ?: 0) >= 850) { p[K.coins] = (p[K.coins] ?: 0) - 850; p[K.arcane] = true } } }
    suspend fun buyStartingGold() { context.profileStore.edit { p -> if ((p[K.coins] ?: 0) >= 500) { p[K.coins] = (p[K.coins] ?: 0) - 500; p[K.goldBonus] = (p[K.goldBonus] ?: 0) + 80 } } }
    suspend fun setSound(value: Boolean) { context.profileStore.edit { it[K.sound] = value } }
    suspend fun setMusic(value: Boolean) { context.profileStore.edit { it[K.music] = value } }
    suspend fun setVibration(value: Boolean) { context.profileStore.edit { it[K.vibration] = value } }
    suspend fun setLowGraphics(value: Boolean) { context.profileStore.edit { it[K.lowGraphics] = value } }

    private fun encode(records: List<RunRecord>) = records.joinToString(";") { "${it.wave},${it.date}" }
    private fun decode(value: String): List<RunRecord> = value.split(';').mapNotNull { token ->
        val parts = token.split(',', limit = 2)
        parts.getOrNull(0)?.toIntOrNull()?.let { RunRecord(it, parts.getOrElse(1) { "—" }) }
    }
    private fun now() = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
}
