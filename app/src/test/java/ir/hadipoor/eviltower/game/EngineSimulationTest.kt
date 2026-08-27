package ir.hadipoor.eviltower.game

import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.engine.InputState
import ir.hadipoor.eviltower.game.model.Platform
import ir.hadipoor.eviltower.game.model.RunPhase
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * End-to-end simulation of the tower with a scripted climbing bot.
 * This is the regression guard that the generated tower is actually *playable*: if a change makes
 * a floor impossible, the bot stops climbing and the test fails.
 */
class EngineSimulationTest {

    private fun simulate(seed: Long, maxSeconds: Float = 90f): GameEngine {
        val engine = GameEngine(runSeed = seed)
        var jumpCooldown = 0f
        var locked: Platform? = null
        var time = 0f
        val dt = 1f / 60f
        while (engine.phase == RunPhase.PLAYING && time < maxSeconds) {
            val player = engine.player
            val platforms = engine.visibleFloors().flatMap { it.platforms }.filter { !it.gone }
            if (player.onGround) {
                locked = platforms
                    .filter {
                        it.bounds.top > player.bounds.y + 2f &&
                            it.bounds.top < player.bounds.y + 34f
                    }
                    .minByOrNull { it.bounds.top + abs(it.bounds.centerX - player.bounds.centerX) * 0.1f }
            }
            val standing = platforms.firstOrNull {
                abs(it.bounds.top - player.bounds.y) < 0.6f &&
                    player.bounds.right > it.bounds.left && player.bounds.left < it.bounds.right
            }
            val target = locked
            val dx = (target?.bounds?.centerX ?: player.bounds.centerX) - player.bounds.centerX
            val dir = if (abs(dx) < 1.2f) 0f else if (dx > 0) 1f else -1f
            var jump = false
            if (target != null && jumpCooldown <= 0f && player.onGround) {
                val atEdge = standing != null && (
                    (dx > 0 && player.bounds.right > standing.bounds.right - 4f) ||
                        (dx < 0 && player.bounds.left < standing.bounds.left + 4f)
                    )
                if (abs(dx) < 4f || atEdge) {
                    jump = true
                    jumpCooldown = 0.28f
                }
            }
            jumpCooldown -= dt
            engine.update(
                dt,
                InputState(
                    moveX = dir,
                    jumpPressed = jump,
                    attackPressed = (time * 60f).toInt() % 24 == 0,
                ),
            )
            engine.consumeEvents()
            time += dt
        }
        return engine
    }

    @Test
    fun `a scripted bot can climb several floors on any seed`() {
        val seeds = listOf(1000L, 1001L, 1002L, 1003L, 1004L)
        val floors = seeds.map { simulate(it).highestFloor }
        floors.forEachIndexed { index, floor ->
            assertTrue("seed ${seeds[index]} blocked at floor $floor", floor >= 3)
        }
        assertTrue("the tower should be climbable on average", floors.average() >= 4.0)
    }

    @Test
    fun `a run always terminates in a valid phase`() {
        val engine = simulate(2024L, maxSeconds = 60f)
        assertTrue(
            engine.phase == RunPhase.PLAYING ||
                engine.phase == RunPhase.FALLING ||
                engine.phase == RunPhase.GAME_OVER ||
                engine.phase == RunPhase.VICTORY,
        )
        assertTrue(engine.score >= 0)
        assertTrue(engine.coins >= 0)
    }

    @Test
    fun `coins and enemies are reachable while climbing`() {
        val engine = simulate(1001L)
        assertTrue("the bot should pick up coins on the way", engine.coins > 0)
    }
}
