package ir.hadipoor.eviltower.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import ir.hadipoor.eviltower.R

/** Lightweight local SFX layer; calls are safe even when a device has no audio output. */
class GameAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val hit = pool.load(context, R.raw.sfx_hit, 1)
    private val fire = pool.load(context, R.raw.sfx_fire, 1)
    private val boss = pool.load(context, R.raw.sfx_boss_roar, 1)
    private val upgrade = pool.load(context, R.raw.sfx_powerup, 1)
    fun playHit() = pool.play(hit, 0.45f, 0.45f, 1, 0, 1f)
    fun playFire() = pool.play(fire, 0.55f, 0.55f, 1, 0, 1f)
    fun playBoss() = pool.play(boss, .8f, .8f, 1, 0, .8f)
    fun playUpgrade() = pool.play(upgrade, .6f, .6f, 1, 0, 1f)
    fun release() = pool.release()
}
