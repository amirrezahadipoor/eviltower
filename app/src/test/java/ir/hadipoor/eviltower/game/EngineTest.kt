package ir.hadipoor.eviltower.game

import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.model.EnginePhase
import ir.hadipoor.eviltower.game.model.TowerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    @Test fun `tower can be built upgraded and sold`() {
        val engine = GameEngine()
        engine.startRun(startingGold = 2000)
        engine.selectPlot(0)
        assertTrue(engine.buildTower(TowerType.ARCHER))
        assertEquals(1, engine.snapshot().towers.single().level)
        assertTrue(engine.upgradeSelected())
        assertEquals(2, engine.snapshot().towers.single().level)
        assertTrue(engine.sellSelected())
        assertTrue(engine.snapshot().towers.isEmpty())
    }

    @Test fun `simulation advances and never skips phase rules`() {
        val engine = GameEngine()
        engine.startRun(startingGold = 100000)
        engine.selectPlot(0); engine.buildTower(TowerType.ARCHER)
        repeat(9000) { engine.update(.08f) }
        val state = engine.snapshot()
        assertTrue(state.wave > 1 || state.phase == EnginePhase.DEFEATED)
        assertTrue(state.enemiesDefeated >= 0)
    }
}
