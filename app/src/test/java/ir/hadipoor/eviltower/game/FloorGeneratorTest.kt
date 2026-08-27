package ir.hadipoor.eviltower.game

import ir.hadipoor.eviltower.game.engine.FloorGenerator
import ir.hadipoor.eviltower.game.engine.GameConfig
import ir.hadipoor.eviltower.game.model.PlatformKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Procedural generation must always produce a *finishable* floor. */
class FloorGeneratorTest {

    private val seed = 20260827L

    @Test
    fun `generation is deterministic for the same seed`() {
        repeat(30) { i ->
            val floor = i + 1
            val a = FloorGenerator.generate(floor, seed)
            val b = FloorGenerator.generate(floor, seed)
            assertEquals(a.templateName, b.templateName)
            assertEquals(a.platforms.size, b.platforms.size)
            a.platforms.indices.forEach { idx ->
                assertEquals(a.platforms[idx].bounds.x, b.platforms[idx].bounds.x, 0.0001f)
                assertEquals(a.platforms[idx].bounds.y, b.platforms[idx].bounds.y, 0.0001f)
            }
        }
    }

    @Test
    fun `every floor has ground and climbable steps`() {
        for (floor in 1..GameConfig.FINAL_FLOOR) {
            val data = FloorGenerator.generate(floor, seed)
            val ground = data.platforms.filter { it.bounds.y <= data.baseY }
            assertTrue("floor $floor has no ground", ground.isNotEmpty())
            val steps = data.platforms.filter { it.bounds.y > data.baseY }
            assertTrue("floor $floor has too few steps", steps.size >= 3)
        }
    }

    @Test
    fun `no vertical gap exceeds the maximum jump height`() {
        // v^2 / (2g) -> the highest the hero can ever reach from a standing jump
        val maxJump = (GameConfig.JUMP_VELOCITY * GameConfig.JUMP_VELOCITY) /
            (2f * -GameConfig.GRAVITY)
        assertTrue("jump height must clear the step gap", maxJump > FloorGenerator.STEP_GAP)

        for (floor in 1..GameConfig.FINAL_FLOOR) {
            val data = FloorGenerator.generate(floor, seed)
            val tops = data.platforms.map { it.bounds.top }.sorted()
            tops.forEach { top ->
                val below = tops.filter { it < top - 0.5f }
                if (below.isEmpty()) return@forEach
                val gap = top - below.max()
                assertTrue("floor $floor: vertical gap $gap is unreachable", gap <= maxJump)
            }
            // the hero must also be able to step onto the next floor's ground slab
            val gapToNextFloor = data.baseY + GameConfig.FLOOR_HEIGHT - tops.max()
            assertTrue(
                "floor $floor: cannot reach the next floor ($gapToNextFloor)",
                gapToNextFloor <= maxJump,
            )
        }
    }

    @Test
    fun `no horizontal gap exceeds the jump distance`() {
        // time to fall back to the height of one step, times the top running speed
        val v = GameConfig.JUMP_VELOCITY
        val g = -GameConfig.GRAVITY
        val flight = (v + kotlin.math.sqrt(v * v - 2 * g * FloorGenerator.STEP_GAP)) / g
        val reach = flight * GameConfig.PLAYER_MAX_SPEED
        assertTrue("MAX_EDGE_GAP must stay within the jump arc", FloorGenerator.MAX_EDGE_GAP < reach)

        for (floor in 1..GameConfig.FINAL_FLOOR) {
            val data = FloorGenerator.generate(floor, seed)
            data.platforms.forEach { platform ->
                val sources = data.platforms.filter {
                    it.bounds.top < platform.bounds.top - 0.5f &&
                        platform.bounds.top - it.bounds.top <= FloorGenerator.STEP_GAP + 6f
                }
                if (sources.isEmpty()) return@forEach
                val gap = sources.minOf { src ->
                    maxOf(
                        0f,
                        maxOf(
                            src.bounds.left - platform.bounds.right,
                            platform.bounds.left - src.bounds.right,
                        ),
                    )
                }
                assertTrue("floor $floor: horizontal gap $gap is unreachable", gap <= reach)
            }
        }
    }

    @Test
    fun `platforms always stay inside the tower walls`() {
        for (floor in 1..80) {
            val data = FloorGenerator.generate(floor, seed)
            data.platforms.forEach { p ->
                assertTrue(
                    "floor $floor platform out of bounds: ${p.bounds}",
                    p.bounds.left >= GameConfig.WALL_THICKNESS - 0.01f &&
                        p.bounds.right <= GameConfig.WORLD_WIDTH - GameConfig.WALL_THICKNESS + 0.01f,
                )
            }
        }
    }

    @Test
    fun `boss floors spawn the right boss and lock the gate`() {
        val mini = FloorGenerator.generate(10, seed)
        assertTrue(mini.isMiniBoss)
        assertTrue(mini.gateLocked)
        assertTrue(mini.enemies.any { it.kind.name == "GATE_GUARDIAN" })

        val boss = FloorGenerator.generate(25, seed)
        assertTrue(boss.isBoss)
        assertTrue(boss.gateLocked)
        assertTrue(boss.enemies.any { it.kind.name == "TOWER_LORD" })

        val normal = FloorGenerator.generate(7, seed)
        assertTrue(!normal.gateLocked)
    }

    @Test
    fun `difficulty increases with floor number`() {
        assertTrue(GameConfig.difficulty(1) < GameConfig.difficulty(50))
        assertTrue(GameConfig.difficulty(50) < GameConfig.difficulty(100))
        assertEquals(1f, GameConfig.difficulty(100), 0.0001f)
    }
}
