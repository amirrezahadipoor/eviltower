package ir.hadipoor.eviltower.game

import ir.hadipoor.eviltower.game.engine.Balance
import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceTest {
    @Test fun `boss replaces mini boss on tenth waves`() {
        val five = Balance.wavePlan(5)
        val ten = Balance.wavePlan(10)
        assertTrue(five.isMiniBoss && !five.isBoss)
        assertTrue(ten.isBoss && !ten.isMiniBoss)
        assertTrue(ten.units.any { it.type == EnemyType.BOSS })
        assertTrue(ten.units.none { it.type == EnemyType.MINI_BOSS })
    }

    @Test fun `endless phase has no cap`() {
        assertTrue(Balance.wavePlan(305).isMiniBoss)
        assertTrue(Balance.wavePlan(310).isBoss)
        assertTrue(Balance.regularHp(310) > Balance.regularHp(300))
        assertTrue(Balance.wavePlan(10000).units.isNotEmpty())
    }

    @Test fun `adjacent scaling is smooth and upgrade costs grow`() {
        val ratio = Balance.regularHp(101) / Balance.regularHp(100)
        assertTrue(ratio in 1.03f..1.06f)
        val t = Tower(1, TowerType.ARCHER, 0, 1)
        assertTrue(Balance.upgradeCost(t) > Balance.towerCost(t.type, 1))
        assertTrue(Balance.towerDamage(t.copy(level = 100)) > Balance.towerDamage(t))
    }
}
