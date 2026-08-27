package ir.hadipoor.eviltower.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager as SystemAudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ir.hadipoor.eviltower.R
import ir.hadipoor.eviltower.game.model.GameEvent

/**
 * Sound of برج شیطانی.
 *
 * * SFX: [SoundPool] with the 14 procedurally generated WAVs in `res/raw`
 *   (see `tools/generate_audio.py`).
 * * Music: [MediaPlayer] looping the seamless `music_tower` bed.
 * * Haptics: short vibration pulses on damage and boss hits (togglable in the settings).
 *
 * Deliberately free of ExoPlayer/Play-Services so the app runs on every Cafe Bazaar device.
 */
class GameAudio(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sfx, Int>()
    private var musicPlayer: MediaPlayer? = null

    var sfxVolume: Float = 0.9f
    var musicVolume: Float = 0.6f
        set(value) {
            field = value
            musicPlayer?.setVolume(value, value)
        }
    var vibrationEnabled: Boolean = true

    enum class Sfx(val res: Int) {
        JUMP(R.raw.sfx_jump),
        DOUBLE_JUMP(R.raw.sfx_double_jump),
        LAND(R.raw.sfx_land),
        COIN(R.raw.sfx_coin),
        GEM(R.raw.sfx_gem),
        POWER_UP(R.raw.sfx_powerup),
        HIT(R.raw.sfx_hit),
        ENEMY_DEATH(R.raw.sfx_enemy_death),
        ATTACK(R.raw.sfx_attack),
        CRUMBLE(R.raw.sfx_crumble),
        FIRE(R.raw.sfx_fire),
        BOSS_ROAR(R.raw.sfx_boss_roar),
        VICTORY(R.raw.sfx_victory),
        FALL(R.raw.sfx_fall),
    }

    init {
        Sfx.entries.forEach { sfx ->
            ids[sfx] = soundPool.load(context, sfx.res, 1)
        }
    }

    fun play(sfx: Sfx, volumeScale: Float = 1f, rate: Float = 1f) {
        val id = ids[sfx] ?: return
        val v = (sfxVolume * volumeScale).coerceIn(0f, 1f)
        if (v <= 0.01f) return
        soundPool.play(id, v, v, 1, 0, rate)
    }

    /** Maps one frame of engine events onto sound + haptics. */
    fun handle(event: GameEvent) {
        when (event) {
            GameEvent.Jump -> play(Sfx.JUMP)
            GameEvent.DoubleJump -> play(Sfx.DOUBLE_JUMP)
            GameEvent.Land -> play(Sfx.LAND, 0.55f)
            GameEvent.Coin -> play(Sfx.COIN, 0.8f, 0.95f + Math.random().toFloat() * 0.1f)
            GameEvent.Gem -> play(Sfx.GEM)
            GameEvent.PowerUpTaken -> play(Sfx.POWER_UP)
            GameEvent.Attack -> play(Sfx.ATTACK, 0.7f)
            GameEvent.EnemyDeath -> play(Sfx.ENEMY_DEATH)
            GameEvent.PlayerHit -> {
                play(Sfx.HIT)
                vibrate(45)
            }

            GameEvent.TrapTrigger -> play(Sfx.FIRE, 0.7f)
            GameEvent.Crumble -> play(Sfx.CRUMBLE, 0.7f)
            GameEvent.BossRoar -> {
                play(Sfx.BOSS_ROAR)
                vibrate(70)
            }

            GameEvent.BossDefeated -> {
                play(Sfx.VICTORY, 0.8f)
                vibrate(90)
            }

            GameEvent.Fall -> {
                play(Sfx.FALL)
                vibrate(160)
            }

            GameEvent.Victory -> play(Sfx.VICTORY)
            is GameEvent.FloorCleared -> Unit
        }
    }

    fun startMusic() {
        if (musicPlayer != null) return
        runCatching {
            musicPlayer = MediaPlayer.create(context, R.raw.music_tower)?.apply {
                isLooping = true
                setVolume(musicVolume, musicVolume)
                setAudioStreamType(SystemAudioManager.STREAM_MUSIC)
                start()
            }
        }
    }

    fun pauseMusic() {
        runCatching { musicPlayer?.takeIf { it.isPlaying }?.pause() }
    }

    fun resumeMusic() {
        runCatching { musicPlayer?.takeIf { !it.isPlaying }?.start() }
    }

    fun stopMusic() {
        runCatching {
            musicPlayer?.stop()
            musicPlayer?.release()
        }
        musicPlayer = null
    }

    fun vibrate(millis: Long) {
        if (!vibrationEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        }
    }

    fun release() {
        stopMusic()
        soundPool.release()
    }
}
