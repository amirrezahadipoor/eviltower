package ir.hadipoor.eviltower.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import ir.hadipoor.eviltower.R

/** Local audio: a looping tension bed plus 14 procedural SFX. */
class GameAudio(context: Context) {
    private var soundVolume = .7f
    private var musicVolume = .22f
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val hit = pool.load(context, R.raw.sfx_hit, 1)
    private val fire = pool.load(context, R.raw.sfx_fire, 1)
    private val boss = pool.load(context, R.raw.sfx_boss_roar, 1)
    private val upgrade = pool.load(context, R.raw.sfx_powerup, 1)
    private val death = pool.load(context, R.raw.sfx_enemy_death, 1)
    private val coin = pool.load(context, R.raw.sfx_coin, 1)
    private val music = MediaPlayer.create(context, R.raw.music_tower)?.apply { isLooping = true; setVolume(musicVolume, musicVolume) }
    fun startMusic() { runCatching { if (music?.isPlaying == false) music?.start() } }
    fun pauseMusic() { runCatching { if (music?.isPlaying == true) music?.pause() } }
    fun setSoundVolume(value: Float) { soundVolume = value.coerceIn(0f, 1f) }
    fun setMusicVolume(value: Float) { musicVolume = value.coerceIn(0f, 1f); music?.setVolume(musicVolume, musicVolume) }
    private fun play(id: Int, amount: Float, rate: Float = 1f) = pool.play(id, amount * soundVolume, amount * soundVolume, 1, 0, rate)
    fun playHit() = play(hit, 0.45f)
    fun playFire() = play(fire, 0.55f)
    fun playBoss() = play(boss, .8f, .8f)
    fun playUpgrade() = play(upgrade, .6f)
    fun playDeath() = play(death, .5f)
    fun playCoin() = play(coin, .35f)
    fun release() { runCatching { music?.release() }; pool.release() }
}
