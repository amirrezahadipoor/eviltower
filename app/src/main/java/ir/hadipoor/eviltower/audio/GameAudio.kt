package ir.hadipoor.eviltower.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import ir.hadipoor.eviltower.R

/** Local audio: a looping tension bed plus 14 procedural SFX. */
class GameAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val hit = pool.load(context, R.raw.sfx_hit, 1)
    private val fire = pool.load(context, R.raw.sfx_fire, 1)
    private val boss = pool.load(context, R.raw.sfx_boss_roar, 1)
    private val upgrade = pool.load(context, R.raw.sfx_powerup, 1)
    private val death = pool.load(context, R.raw.sfx_enemy_death, 1)
    private val coin = pool.load(context, R.raw.sfx_coin, 1)
    private val music = MediaPlayer.create(context, R.raw.music_tower)?.apply { isLooping = true; setVolume(.22f, .22f) }
    fun startMusic() { runCatching { if (music?.isPlaying == false) music?.start() } }
    fun pauseMusic() { runCatching { if (music?.isPlaying == true) music?.pause() } }
    fun playHit() = pool.play(hit, 0.45f, 0.45f, 1, 0, 1f)
    fun playFire() = pool.play(fire, 0.55f, 0.55f, 1, 0, 1f)
    fun playBoss() = pool.play(boss, .8f, .8f, 1, 0, .8f)
    fun playUpgrade() = pool.play(upgrade, .6f, .6f, 1, 0, 1f)
    fun playDeath() = pool.play(death, .5f, .5f, 1, 0, 1f)
    fun playCoin() = pool.play(coin, .35f, .35f, 1, 0, 1f)
    fun release() { runCatching { music?.release() }; pool.release() }
}
