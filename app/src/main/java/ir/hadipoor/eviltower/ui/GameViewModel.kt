package ir.hadipoor.eviltower.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hadipoor.eviltower.audio.GameAudio
import ir.hadipoor.eviltower.data.Achievements
import ir.hadipoor.eviltower.data.ControlScheme
import ir.hadipoor.eviltower.data.GameRepository
import ir.hadipoor.eviltower.data.GameSettings
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.data.RunResult
import ir.hadipoor.eviltower.data.ShopCatalog
import ir.hadipoor.eviltower.data.ShopEntry
import ir.hadipoor.eviltower.data.UpgradeEntry
import ir.hadipoor.eviltower.game.engine.GameConfig
import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.model.PowerUp
import ir.hadipoor.eviltower.monetization.AdManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVVM: the ViewModel owns persistence, settings, audio and the lifecycle of a run.
 * The [GameEngine] itself is a plain Kotlin object mutated by the render loop at 60fps —
 * keeping it out of Compose state avoids a recomposition storm.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)
    val audio = GameAudio(application)

    val profile: StateFlow<PlayerProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerProfile())

    val settings: StateFlow<GameSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    /** Current run (null when not playing). */
    var engine by mutableStateOf<GameEngine?>(null)
        private set

    /** Summary of the last finished run, shown by the game-over screen. */
    var lastResult by mutableStateOf<RunResult?>(null)
        private set

    var lastRunSavedCoins by mutableStateOf(0)
        private set

    /** Achievements unlocked by the last run, shown as a toast-like banner. */
    var freshAchievements by mutableStateOf<List<String>>(emptyList())
        private set

    var adRewardUsedThisRun by mutableStateOf(false)
        private set

    private var runSaved = false

    init {
        viewModelScope.launch {
            settings.collect { s ->
                audio.musicVolume = s.musicVolume
                audio.sfxVolume = s.sfxVolume
                audio.vibrationEnabled = s.vibration
            }
        }
    }

    // ------------------------------------------------------------------ run lifecycle

    fun startRun() {
        val p = profile.value
        val extraHearts = p.upgradeLevel("extra_heart")
        val startPowers = buildSet {
            if (p.upgradeLevel("start_shield") > 0) add(PowerUp.SHIELD)
            if (p.upgradeLevel("start_wings") > 0) add(PowerUp.WINGS)
        }
        runSaved = false
        adRewardUsedThisRun = false
        lastResult = null
        freshAchievements = emptyList()
        engine = GameEngine(
            runSeed = System.currentTimeMillis(),
            startHealth = GameConfig.START_HEALTH + extraHearts,
            startingPowerUps = startPowers,
            coinBonusMultiplier = ShopCatalog.coinMultiplier(p.upgradeLevel("coin_bonus")),
        )
        audio.startMusic()
    }

    fun quitRun() {
        engine = null
    }

    /** Called once by the game loop when the engine reports GAME_OVER / VICTORY. */
    fun finishRun(victory: Boolean) {
        if (runSaved) return
        val e = engine ?: return
        runSaved = true
        viewModelScope.launch {
            val result = repository.saveRun(
                floorsClimbed = e.highestFloor,
                coinsCollected = e.coins,
                gemsCollected = e.gems,
                score = e.score,
                enemiesDefeated = e.enemiesDefeated,
                victory = victory,
            )
            lastResult = result
            lastRunSavedCoins = result.coinsKept
            checkAchievements()
        }
    }

    private suspend fun checkAchievements() {
        val current = repository.profile.first()
        val unlocked = Achievements.newlyUnlocked(current)
        if (unlocked.isNotEmpty()) {
            repository.markAchievements(unlocked.map { it.id }, unlocked.sumOf { it.rewardGems })
            freshAchievements = unlocked.map { it.persianTitle }
        }
    }

    fun clearFreshAchievements() {
        freshAchievements = emptyList()
    }

    /** Rewarded ad: continue the current run from the floor where the hero fell. */
    fun continueAfterAd() {
        val e = engine ?: return
        adRewardUsedThisRun = true
        runSaved = false
        lastResult = null
        e.revive()
    }

    /** Rewarded ad: double the coins earned in the finished run. */
    fun doubleCoinsAfterAd() {
        val result = lastResult ?: return
        viewModelScope.launch {
            repository.grantBonusCoins(result.coinsKept)
            lastRunSavedCoins = result.coinsKept * 2
            lastResult = result.copy(coinsKept = result.coinsKept * 2)
        }
    }

    val adsAvailable: Boolean
        get() = AdManager.provider.isEnabled && !profile.value.adsRemoved

    // ------------------------------------------------------------------ shop

    fun purchase(entry: ShopEntry, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val level = if (entry is UpgradeEntry) profile.value.upgradeLevel(entry.id) else 0
            onResult(repository.purchase(entry, level))
        }
    }

    fun selectSkin(id: String) = viewModelScope.launch { repository.selectSkin(id) }
    fun selectTheme(id: String) = viewModelScope.launch { repository.selectTheme(id) }

    fun grantPurchasedGems(amount: Int) = viewModelScope.launch { repository.addGems(amount) }
    fun setAdsRemoved(removed: Boolean) = viewModelScope.launch { repository.setAdsRemoved(removed) }

    // ------------------------------------------------------------------ settings

    fun setMusicVolume(v: Float) = viewModelScope.launch { repository.setMusicVolume(v) }
    fun setSfxVolume(v: Float) = viewModelScope.launch { repository.setSfxVolume(v) }
    fun setControls(scheme: ControlScheme) = viewModelScope.launch { repository.setControlScheme(scheme) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { repository.setVibration(enabled) }
    fun setLanguage(language: String) = viewModelScope.launch { repository.setLanguage(language) }
    fun setScreenShake(enabled: Boolean) = viewModelScope.launch { repository.setScreenShake(enabled) }
    fun resetProgress() = viewModelScope.launch { repository.resetProgress() }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
