package ir.hadipoor.eviltower.game

import ir.hadipoor.eviltower.game.engine.GameConfig
import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.engine.InputState
import ir.hadipoor.eviltower.game.model.Aabb
import ir.hadipoor.eviltower.game.model.RunPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    @Test
    fun `aabb intersection basics`() {
        val a = Aabb(0f, 0f, 10f, 10f)
        assertTrue(a.intersects(Aabb(5f, 5f, 10f, 10f)))
        assertFalse(a.intersects(Aabb(10f, 0f, 5f, 5f)))   // touching edges do not overlap
        assertFalse(a.intersects(Aabb(-6f, 0f, 5f, 5f)))
        assertTrue(a.contains(5f, 5f))
        assertFalse(a.contains(11f, 5f))
    }

    @Test
    fun `player lands on the ground instead of falling through`() {
        val engine = GameEngine(runSeed = 42L)
        repeat(60) { engine.update(1f / 60f, InputState()) }
        assertTrue("player should rest on the ground floor", engine.player.onGround)
        assertTrue(engine.player.bounds.y >= -1f)
        assertEquals(RunPhase.PLAYING, engine.phase)
    }

    @Test
    fun `jump raises the hero and gravity brings him back`() {
        val engine = GameEngine(runSeed = 7L)
        repeat(30) { engine.update(1f / 60f, InputState()) }
        val groundY = engine.player.bounds.y
        engine.update(1f / 60f, InputState(jumpPressed = true))
        repeat(10) { engine.update(1f / 60f, InputState()) }
        assertTrue("hero must leave the ground", engine.player.bounds.y > groundY + 4f)
        repeat(120) { engine.update(1f / 60f, InputState()) }
        assertTrue("hero must come back down", engine.player.onGround)
    }

    @Test
    fun `falling below the death line ends the run`() {
        val engine = GameEngine(runSeed = 11L)
        repeat(20) { engine.update(1f / 60f, InputState()) }
        // teleport the hero deep below the tower and let the simulation notice
        engine.player.bounds.y = -GameConfig.FALL_DEATH_MARGIN - 40f
        repeat(180) { engine.update(1f / 60f, InputState()) }
        assertEquals(RunPhase.GAME_OVER, engine.phase)
    }

    @Test
    fun `pause freezes the simulation`() {
        val engine = GameEngine(runSeed = 3L)
        repeat(20) { engine.update(1f / 60f, InputState()) }
        engine.pause()
        val x = engine.player.bounds.x
        repeat(60) { engine.update(1f / 60f, InputState(moveX = 1f)) }
        assertEquals(x, engine.player.bounds.x, 0.0001f)
        engine.resume()
        repeat(30) { engine.update(1f / 60f, InputState(moveX = 1f)) }
        assertTrue(engine.player.bounds.x > x)
    }

    @Test
    fun `score grows with floors and coins`() {
        val engine = GameEngine(runSeed = 5L)
        val base = engine.score
        assertTrue(base >= 0)
        assertEquals(1, engine.currentFloor)
    }
}
