package ir.hadipoor.eviltower.game.render

import kotlin.math.sin

/** Small reusable motion layer shared by vector towers, enemies and upgrade bursts. */
enum class SpriteState { IDLE, MOVE, AIM, ATTACK, HIT, DEATH, UPGRADE }
data class SpriteMotion(val bob: Float, val tilt: Float, val scale: Float, val glow: Float)

object SpriteAnimation {
    fun sample(state: SpriteState, time: Float, id: Int, intensity: Float = 1f): SpriteMotion {
        val seed = id * .73f
        return when (state) {
            SpriteState.IDLE -> SpriteMotion(sin(time * 2f + seed) * 1.5f, sin(time + seed) * 1.5f, 1f, .3f)
            SpriteState.MOVE -> SpriteMotion(sin(time * 9f + seed) * 3f, sin(time * 7f + seed) * 2f, 1f + sin(time * 9f + seed) * .025f, .25f)
            SpriteState.AIM -> SpriteMotion(0f, sin(time * 5f + seed) * 4f, 1f, .35f)
            SpriteState.ATTACK -> SpriteMotion(0f, 0f, 1f + (sin(time * 22f + seed) * .05f).coerceAtLeast(0f), .65f)
            SpriteState.HIT -> SpriteMotion(0f, 0f, 1f + intensity * .08f, .9f)
            SpriteState.DEATH -> SpriteMotion(-time * 16f, time * 22f, 1f - time.coerceIn(0f, 1f) * .35f, 0f)
            SpriteState.UPGRADE -> SpriteMotion(0f, 0f, 1f + intensity * .12f, 1f)
        }
    }
}
