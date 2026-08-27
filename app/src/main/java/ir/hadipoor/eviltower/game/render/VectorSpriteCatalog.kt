package ir.hadipoor.eviltower.game.render

import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.TowerType

/** Shared naming and parametric rules for editable SVG bases in assets/svg. */
object VectorSpriteCatalog {
    fun towerAsset(type: TowerType, level: Int): String =
        "tower_${type.name.lowercase()}_tier_${((level - 1) / 10 + 1).coerceIn(1, 10).toString().padStart(2, '0')}.svg"
    fun enemyAsset(type: EnemyType, variant: Int = 0): String =
        "enemy_${type.name.lowercase()}_variant_${variant.coerceIn(0, 8).toString().padStart(2, '0')}.svg"
    fun levelScale(level: Int): Float = 1f + level.coerceIn(1, 100) * .0025f
    fun levelGlow(level: Int): Float = .18f + level.coerceIn(1, 100) * .006f
}
