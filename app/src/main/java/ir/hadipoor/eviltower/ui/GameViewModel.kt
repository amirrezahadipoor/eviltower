package ir.hadipoor.eviltower.ui

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hadipoor.eviltower.audio.GameAudio
import ir.hadipoor.eviltower.data.GameRepository
import ir.hadipoor.eviltower.data.ProfileData
import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.model.EnginePhase
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.game.model.TowerType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen { SPLASH, MENU, GAME, SHOP, ACHIEVEMENTS, RECORDS, SETTINGS, RESULT }

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = GameRepository(app)
    val profile: StateFlow<ProfileData> = repository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileData())
    val screen: MutableState<AppScreen> = mutableStateOf(AppScreen.SPLASH)
    val snapshot: MutableState<GameSnapshot> = mutableStateOf(GameSnapshot())
    val notice: MutableState<String?> = mutableStateOf(null)
    val audio = GameAudio(app)
    private val engine = GameEngine()
    private var loop: Job? = null
    private var didSave = false

    init {
        audio.startMusic()
        viewModelScope.launch {
            profile.collect { data ->
                audio.setSoundVolume(if (data.soundOn) data.soundVolume else 0f)
                audio.setMusicVolume(data.musicVolume)
                if (data.musicOn) audio.startMusic() else audio.pauseMusic()
            }
        }
        viewModelScope.launch { delay(1_200); screen.value = AppScreen.MENU }
    }

    fun startRun() {
        val p = profile.value
        engine.startRun(startingGold = 520 + p.startingGoldBonus, personalBest = p.bestWave, arcane = p.arcaneUnlocked, lowGraphics = p.lowGraphics, skin = if (p.emberSkinUnlocked) 1 else 0)
        snapshot.value = engine.snapshot()
        didSave = false
        screen.value = AppScreen.GAME
        loop?.cancel()
        loop = viewModelScope.launch {
            var previousProjectiles = 0
            var previousKills = 0
            var previousCore = snapshot.value.coreHp
            var previousBoss: String? = null
            var previousWave = snapshot.value.wave
            var previousGems = snapshot.value.gems
            while (isActive && screen.value == AppScreen.GAME) {
                delay(16)
                engine.update(1f / 60f)
                snapshot.value = engine.snapshot()
                val current = snapshot.value
                if (current.wave != previousWave || current.bossName != previousBoss) audio.setIntensity(current.wave, current.bossName != null)
                if (current.projectiles.size > previousProjectiles) audio.playFire()
                if (current.enemiesDefeated > previousKills) { audio.playDeath(); audio.playCoin() }
                if (current.wave > previousWave) audio.playVictory()
                if (current.gems > previousGems) audio.playGem()
                if (current.coreHp < previousCore) { audio.playHit(); if (current.coreHp <= 5) audio.playCoreAlarm() }
                if (current.bossName != null && current.bossName != previousBoss) audio.playBoss()
                previousProjectiles = current.projectiles.size
                previousKills = current.enemiesDefeated
                previousCore = current.coreHp
                previousBoss = current.bossName
                previousWave = current.wave
                previousGems = current.gems
                if (current.phase == EnginePhase.DEFEATED) {
                    saveResult()
                    delay(420)
                    screen.value = AppScreen.RESULT
                    break
                }
            }
        }
    }

    fun restartRun() { startRun() }
    fun goMenu() { loop?.cancel(); screen.value = AppScreen.MENU }
    fun open(screenToOpen: AppScreen) { loop?.cancel(); screen.value = screenToOpen }
    fun togglePause() { engine.togglePause(); snapshot.value = engine.snapshot() }
    fun selectPlot(index: Int?) { engine.selectPlot(index); snapshot.value = engine.snapshot() }
    fun build(type: TowerType) { if (engine.buildTower(type)) audio.playUpgrade(); snapshot.value = engine.snapshot() }
    fun upgrade() { if (engine.upgradeSelected()) audio.playUpgrade(); snapshot.value = engine.snapshot() }
    fun sell() { engine.sellSelected(); snapshot.value = engine.snapshot() }
    fun inferno() { if (engine.activateInferno()) audio.playFire(); snapshot.value = engine.snapshot() }
    fun shield() { if (engine.activateShield()) audio.playUpgrade(); snapshot.value = engine.snapshot() }

    fun buyArcane() { viewModelScope.launch { repository.buyArcane(); notice.value = "برج جادوی اهریمنی باز شد" } }
    fun buyGoldBonus() { viewModelScope.launch { repository.buyStartingGold(); notice.value = "۸۰ سکه‌ی شروع اضافه شد" } }
    fun buyEmberSkin() { viewModelScope.launch { repository.buyEmberSkin(); notice.value = "پوسته‌ی شراره باز شد" } }
    fun setSound(value: Boolean) {
        audio.setSoundVolume(if (value) profile.value.soundVolume else 0f)
        viewModelScope.launch { repository.setSound(value) }
    }
    fun setSoundVolume(value: Float) { audio.setSoundVolume(value); viewModelScope.launch { repository.setSoundVolume(value) } }
    fun setMusicVolume(value: Float) { audio.setMusicVolume(value); viewModelScope.launch { repository.setMusicVolume(value) } }
    fun setMusic(value: Boolean) {
        if (value) audio.startMusic() else audio.pauseMusic()
        viewModelScope.launch { repository.setMusic(value) }
    }
    fun setVibration(value: Boolean) { viewModelScope.launch { repository.setVibration(value) } }
    fun setLowGraphics(value: Boolean) { viewModelScope.launch { repository.setLowGraphics(value) } }
    fun clearNotice() { notice.value = null }

    private fun saveResult() {
        if (didSave) return
        didSave = true
        viewModelScope.launch { repository.saveRun(snapshot.value) }
    }
    fun release() { loop?.cancel(); audio.release() }
    override fun onCleared() { release(); super.onCleared() }
}
