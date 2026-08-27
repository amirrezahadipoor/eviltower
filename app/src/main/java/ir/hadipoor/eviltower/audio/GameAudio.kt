package ir.hadipoor.eviltower.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import ir.hadipoor.eviltower.R

/** Three loopable stems (base, tension, boss) plus local SFX, mixed without network dependencies. */
class GameAudio(context: Context) {
    private var soundVolume = .7f
    private var musicVolume = .22f
    private var lastWave = 1
    private var lastBossFight = false
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val hit = pool.load(context, R.raw.sfx_hit, 1)
    private val fire = pool.load(context, R.raw.sfx_fire, 1)
    private val boss = pool.load(context, R.raw.sfx_boss_roar, 1)
    private val upgrade = pool.load(context, R.raw.sfx_powerup, 1)
    private val death = pool.load(context, R.raw.sfx_enemy_death, 1)
    private val coin = pool.load(context, R.raw.sfx_coin, 1)
    private val gem = pool.load(context, R.raw.sfx_gem, 1)
    private val victory = pool.load(context, R.raw.sfx_victory, 1)
    private val attack = pool.load(context, R.raw.sfx_attack, 1)
    private val crumble = pool.load(context, R.raw.sfx_crumble, 1)
    private val jump = pool.load(context, R.raw.sfx_jump, 1)
    private val land = pool.load(context, R.raw.sfx_land, 1)
    private val fall = pool.load(context, R.raw.sfx_fall, 1)
    private val doubleJump = pool.load(context, R.raw.sfx_double_jump, 1)
    private val baseMusic = stem(context, R.raw.music_base)
    private val tensionMusic = stem(context, R.raw.music_tension)
    private val bossMusic = stem(context, R.raw.music_boss)

    private fun stem(context: Context, resource: Int): MediaPlayer? = MediaPlayer.create(context, resource)?.apply { isLooping = true }
    private fun each(action: (MediaPlayer) -> Unit) { listOfNotNull(baseMusic, tensionMusic, bossMusic).forEach(action) }
    fun startMusic() { runCatching { each { if (!it.isPlaying) it.start() } } }
    fun pauseMusic() { runCatching { each { if (it.isPlaying) it.pause() } } }
    fun setSoundVolume(value: Float) { soundVolume = value.coerceIn(0f, 1f) }
    fun setMusicVolume(value: Float) { musicVolume = value.coerceIn(0f, 1f); setIntensity(lastWave, lastBossFight) }
    fun setIntensity(wave: Int, bossFight: Boolean) {
        lastWave = wave; lastBossFight = bossFight
        val progress = (wave.coerceAtMost(300) / 300f)
        val base = (musicVolume * (1f - progress * .18f)).coerceAtLeast(0f)
        val tension = musicVolume * (.06f + progress * .42f)
        val bossGain = if (bossFight) musicVolume * .72f else 0f
        baseMusic?.setVolume(base, base)
        tensionMusic?.setVolume(tension, tension)
        bossMusic?.setVolume(bossGain, bossGain)
        runCatching { bossMusic?.let { it.setPlaybackParams(it.playbackParams.setSpeed(if (bossFight) 1.04f else 1f)) } }
    }
    private fun play(id: Int, amount: Float, rate: Float = 1f) = pool.play(id, amount * soundVolume, amount * soundVolume, 1, 0, rate)
    fun playHit() = play(hit, 0.45f)
    fun playFire() = play(fire, 0.55f)
    fun playBoss() = play(boss, .8f, .8f)
    fun playUpgrade() = play(upgrade, .6f)
    fun playDeath() = play(death, .5f)
    fun playCoin() = play(coin, .35f)
    fun playGem() = play(gem, .55f, 1.08f)
    fun playVictory() = play(victory, .6f, 1.0f)
    fun playAttack() = play(attack, .35f, 1.2f)
    fun playCoreAlarm() = play(crumble, .6f, .82f)
    fun playJump() = play(jump, .3f, 1.0f)
    fun playLand() = play(land, .25f, .9f)
    fun playFall() = play(fall, .5f, .8f)
    fun playDoubleJump() = play(doubleJump, .35f, 1.1f)
    fun release() { runCatching { each { it.release() } }; pool.release() }
}
