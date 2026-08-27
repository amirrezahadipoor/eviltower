package ir.hadipoor.eviltower.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import ir.hadipoor.eviltower.game.engine.GameConfig
import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnemyKind
import ir.hadipoor.eviltower.game.model.EnemyState
import ir.hadipoor.eviltower.game.model.FloorData
import ir.hadipoor.eviltower.game.model.Pickup
import ir.hadipoor.eviltower.game.model.PickupKind
import ir.hadipoor.eviltower.game.model.Platform
import ir.hadipoor.eviltower.game.model.PlatformKind
import ir.hadipoor.eviltower.game.model.Player
import ir.hadipoor.eviltower.game.model.PlayerState
import ir.hadipoor.eviltower.game.model.PowerUp
import ir.hadipoor.eviltower.game.model.Trap
import ir.hadipoor.eviltower.game.model.TrapKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the whole world with cached vector paths (see [SpriteCache]).
 * Everything is resolution independent: world units are mapped to pixels once per frame.
 */
class GameRenderer {

    private var scale = 1f
    private var camY = 0f
    private var viewH = 0f

    private fun sx(worldX: Float) = worldX * scale
    private fun sy(worldY: Float) = viewH / 2f - (worldY - camY) * scale
    private fun s(len: Float) = len * scale

    fun DrawScope.render(
        engine: GameEngine,
        time: Float,
        tower: TowerStyle,
        hero: HeroStyle,
        shakeEnabled: Boolean = true,
    ) {
        scale = size.width / GameConfig.WORLD_WIDTH
        camY = engine.cameraY
        viewH = size.height

        drawSky(tower)

        val shake = if (shakeEnabled) engine.screenShake else 0f
        val dx = if (shake > 0f) sin(time * 60f) * shake * 5f else 0f
        val dy = if (shake > 0f) cos(time * 71f) * shake * 5f else 0f

        translate(dx, dy) {
            drawWalls(tower, time)
            engine.visibleFloors().forEach { floor ->
                if (isFloorVisible(floor)) drawFloor(floor, tower, time)
            }
            drawHero(engine.player, hero, time)
            drawFog(tower, time)
            drawEmbers(tower, time)
        }
        drawDarkness(engine, tower)
    }

    private fun isFloorVisible(floor: FloorData): Boolean {
        val top = sy(floor.baseY + floor.height + 30f)
        val bottom = sy(floor.baseY - 30f)
        return bottom > 0f && top < viewH
    }

    // -------------------------------------------------------------------------------------
    // Background
    // -------------------------------------------------------------------------------------

    private fun DrawScope.drawSky(tower: TowerStyle) {
        drawRect(
            brush = Brush.verticalGradient(listOf(tower.skyTop, tower.sky)),
            size = size,
        )
    }

    private fun DrawScope.drawWalls(tower: TowerStyle, time: Float) {
        val wallW = s(GameConfig.WALL_THICKNESS)
        // background brick pattern with a light parallax (0.55 of camera speed)
        val brickH = s(11f)
        val brickW = s(15f)
        val parallax = (camY * 0.45f * scale) % (brickH * 2f)
        var y = -brickH * 2f + parallax
        var row = 0
        while (y < viewH + brickH) {
            var x = if (row % 2 == 0) 0f else -brickW / 2f
            while (x < size.width) {
                drawRect(
                    color = if ((row + (x / brickW).toInt()) % 3 == 0) tower.wallDark else tower.wall,
                    topLeft = Offset(x + 1f, y + 1f),
                    size = Size(brickW - 2f, brickH - 2f),
                    alpha = 0.55f,
                )
                x += brickW
            }
            y += brickH
            row++
        }
        // solid side walls
        drawRect(tower.wallDark, Offset(0f, 0f), Size(wallW, viewH))
        drawRect(tower.wallDark, Offset(size.width - wallW, 0f), Size(wallW, viewH))
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)),
            topLeft = Offset(0f, 0f),
            size = Size(wallW * 2.4f, viewH),
        )
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))),
            topLeft = Offset(size.width - wallW * 2.4f, 0f),
            size = Size(wallW * 2.4f, viewH),
        )

        // torches every half floor, flickering
        val first = ((camY - viewH / scale) / 40f).toInt() - 1
        val last = ((camY + viewH / scale) / 40f).toInt() + 1
        for (i in first..last) {
            val worldY = i * 40f
            val leftSide = i % 2 == 0
            val tx = if (leftSide) sx(GameConfig.WALL_THICKNESS + 1f) else sx(GameConfig.WORLD_WIDTH - GameConfig.WALL_THICKNESS - 8f)
            val ty = sy(worldY)
            if (ty < -60f || ty > viewH + 60f) continue
            val flick = 0.75f + 0.25f * sin(time * 9f + i * 1.7f) + 0.1f * sin(time * 23f + i)
            drawSvg(SvgPaths.TORCH_HOLDER, tx, ty, s(7f), s(9f), tower.wallDark.copy(alpha = 0.9f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tower.accent.copy(alpha = 0.35f * flick), Color.Transparent),
                    center = Offset(tx + s(3.5f), ty),
                    radius = s(26f),
                ),
                radius = s(26f),
                center = Offset(tx + s(3.5f), ty),
            )
            drawSvg(
                SvgPaths.TORCH_FLAME,
                tx + s(1.6f), ty - s(7f) * flick,
                s(4.4f), s(7.5f) * flick,
                tower.accent, alpha = 0.95f,
            )
            drawSvg(
                SvgPaths.TORCH_FLAME,
                tx + s(2.4f), ty - s(5f) * flick,
                s(2.8f), s(5f) * flick,
                TowerPalette_Torch, alpha = 0.9f,
            )
        }
    }

    private val TowerPalette_Torch = Color(0xFFFFD27D)

    private fun DrawScope.drawFog(tower: TowerStyle, time: Float) {
        for (i in 0 until 5) {
            val seed = i * 37.13f
            val fx = (pseudo(seed) * 1.2f - 0.1f) * size.width
            val speed = 6f + pseudo(seed + 3f) * 8f
            val worldY = ((camY * 0.25f + time * speed + i * 60f) % 220f) - 40f
            val y = viewH - ((worldY / 220f) * (viewH + 200f))
            val w = size.width * (0.5f + pseudo(seed + 9f) * 0.6f)
            drawSvg(
                SvgPaths.FOG_BLOB,
                fx - w / 2f, y, w, w * 0.35f,
                tower.fog, alpha = 0.5f,
            )
        }
    }

    private fun DrawScope.drawEmbers(tower: TowerStyle, time: Float) {
        for (i in 0 until 26) {
            val seed = i * 12.9898f
            val px = pseudo(seed) * size.width
            val speed = 14f + pseudo(seed + 1f) * 26f
            val cycle = (time * speed + pseudo(seed + 2f) * 400f) % 400f
            val py = viewH - (cycle / 400f) * (viewH + 80f)
            val wob = sin(time * 2.2f + i) * s(2.5f)
            val r = s(0.5f + pseudo(seed + 5f) * 0.9f)
            val alpha = (0.15f + 0.55f * pseudo(seed + 7f)) * (1f - cycle / 400f)
            drawCircle(tower.accent, r, Offset(px + wob, py), alpha = alpha)
        }
    }

    private fun DrawScope.drawDarkness(engine: GameEngine, tower: TowerStyle) {
        val floor = engine.visibleFloors().firstOrNull { it.number == engine.currentFloor }
        val darkness = floor?.darkness ?: 0.1f
        if (darkness <= 0.02f) return
        val p = engine.player.bounds
        val center = Offset(sx(p.centerX), sy(p.centerY))
        val radius = size.width * (0.95f - 0.45f * darkness)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = darkness.coerceAtMost(0.85f))),
                center = center,
                radius = radius,
            ),
            size = size,
        )
    }

    // -------------------------------------------------------------------------------------
    // Floor content
    // -------------------------------------------------------------------------------------

    private fun DrawScope.drawFloor(floor: FloorData, tower: TowerStyle, time: Float) {
        floor.platforms.forEach { drawPlatform(it, tower, time) }
        floor.traps.forEach { drawTrap(it, tower, time) }
        floor.pickups.forEach { if (!it.taken) drawPickup(it, time) }
        floor.enemies.forEach { drawEnemy(it, tower, time) }
        if (floor.gateLocked) drawGate(floor, tower, time)
        drawFloorSign(floor, tower)
    }

    private fun DrawScope.drawFloorSign(floor: FloorData, tower: TowerStyle) {
        val y = sy(floor.baseY)
        if (y < -20f || y > viewH + 20f) return
        drawLine(
            color = tower.platformEdge.copy(alpha = 0.25f),
            start = Offset(sx(GameConfig.WALL_THICKNESS), y),
            end = Offset(sx(GameConfig.WORLD_WIDTH - GameConfig.WALL_THICKNESS), y),
            strokeWidth = 1f,
        )
    }

    private fun DrawScope.drawPlatform(p: Platform, tower: TowerStyle, time: Float) {
        if (p.gone) {
            // ghost outline while the tile regrows
            val x = sx(p.bounds.x)
            val y = sy(p.bounds.top)
            drawRect(
                color = tower.platformEdge.copy(alpha = 0.15f),
                topLeft = Offset(x, y),
                size = Size(s(p.bounds.w), s(p.bounds.h)),
                style = Stroke(width = 1.5f),
            )
            return
        }
        val shakeX = if (p.kind == PlatformKind.CRUMBLING && p.crumbleTimer > 0f) {
            sin(p.shakeSeed) * s(0.8f)
        } else 0f
        val x = sx(p.bounds.x) + shakeX
        val y = sy(p.bounds.top)
        val w = s(p.bounds.w)
        val h = s(p.bounds.h)

        val body = when (p.kind) {
            PlatformKind.CRUMBLING -> Color(0xFF6B4A3A)
            PlatformKind.MOVING -> tower.platformEdge
            else -> tower.platform
        }
        drawRect(body, Offset(x, y), Size(w, h))
        drawRect(
            color = tower.platformEdge,
            topLeft = Offset(x, y),
            size = Size(w, h * 0.28f),
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(x, y + h - h * 0.2f),
            size = Size(w, h * 0.2f),
        )
        // brick seams
        var bx = x + s(4f)
        while (bx < x + w - s(2f)) {
            drawLine(
                tower.brickLine.copy(alpha = 0.6f),
                Offset(bx, y),
                Offset(bx, y + h),
                strokeWidth = 1f,
            )
            bx += s(7f)
        }
        if (p.kind == PlatformKind.CRUMBLING) {
            drawLine(
                Color(0xFF2A1A12),
                Offset(x + w * 0.35f, y),
                Offset(x + w * 0.45f, y + h),
                strokeWidth = 1.5f,
            )
            drawLine(
                Color(0xFF2A1A12),
                Offset(x + w * 0.7f, y),
                Offset(x + w * 0.62f, y + h),
                strokeWidth = 1.5f,
            )
        }
        if (p.kind == PlatformKind.MOVING) {
            drawCircle(tower.accent.copy(alpha = 0.8f), s(0.9f), Offset(x + s(2.5f), y + h / 2f))
            drawCircle(tower.accent.copy(alpha = 0.8f), s(0.9f), Offset(x + w - s(2.5f), y + h / 2f))
        }
    }

    private fun DrawScope.drawTrap(trap: Trap, tower: TowerStyle, time: Float) {
        val b = trap.bounds
        when (trap.kind) {
            TrapKind.SPINNING_BLADE -> {
                val travelX = if (trap.travel > 0f) sin(trap.timer * 1.2f) * trap.travel else 0f
                val x = sx(b.x + travelX)
                val y = sy(b.top)
                val w = s(b.w)
                val h = s(b.h)
                drawSvg(
                    SvgPaths.TRAP_BLADE, x, y, w, h,
                    Color(0xFFC9CEDD), rotation = trap.timer * 520f,
                )
                drawSvg(SvgPaths.TRAP_BLADE_CORE, x, y, w, h, Color(0xFF5A5F70))
                drawCircle(
                    tower.accent.copy(alpha = 0.18f),
                    w * 0.75f,
                    Offset(x + w / 2f, y + h / 2f),
                )
            }

            TrapKind.FIRE_JET -> {
                val hb = trap.hitBox()
                val x = sx(b.x)
                val baseY = sy(b.y)
                drawRect(
                    color = tower.wallDark,
                    topLeft = Offset(x, baseY - s(2f)),
                    size = Size(s(b.w), s(2.5f)),
                )
                if (hb.h > 0.5f) {
                    val flameH = s(hb.h)
                    val flicker = 0.85f + 0.15f * sin(time * 30f)
                    drawSvg(
                        SvgPaths.TRAP_FIRE,
                        x, baseY - flameH * flicker, s(b.w), flameH * flicker,
                        Color(0xFFFF6A1A), alpha = 0.95f,
                    )
                    drawSvg(
                        SvgPaths.TRAP_FIRE_CORE,
                        x + s(b.w) * 0.2f, baseY - flameH * 0.7f * flicker,
                        s(b.w) * 0.6f, flameH * 0.7f * flicker,
                        Color(0xFFFFD27D),
                    )
                } else {
                    // idle glow warns the player a second before the burst
                    drawCircle(
                        Color(0xFFFF6A1A).copy(alpha = 0.25f + 0.25f * sin(time * 8f)),
                        s(3f),
                        Offset(x + s(b.w / 2f), baseY - s(1f)),
                    )
                }
            }

            TrapKind.CRUSHER -> {
                val hb = trap.hitBox()
                val x = sx(hb.x)
                val top = sy(hb.top)
                drawSvg(SvgPaths.TRAP_CRUSHER, x, top, s(hb.w), s(hb.h), Color(0xFF4A4459))
                drawSvg(
                    SvgPaths.TRAP_SPIKE,
                    x, top + s(hb.h) - s(3.4f), s(hb.w), s(3.6f),
                    Color(0xFFB9BECD),
                )
                drawLine(
                    tower.wallDark,
                    Offset(x + s(hb.w) / 2f, sy(b.top + b.h)),
                    Offset(x + s(hb.w) / 2f, top),
                    strokeWidth = 2f,
                )
            }

            TrapKind.SLEEP_GAS -> {
                if (!trap.isActive) return
                val x = sx(b.x)
                val y = sy(b.top)
                val pulse = 0.6f + 0.4f * sin(time * 3f)
                drawSvg(
                    SvgPaths.TRAP_GAS, x, y, s(b.w), s(b.h),
                    Color(0xFF7BE38B), alpha = 0.35f * pulse,
                )
                drawSvg(
                    SvgPaths.TRAP_GAS,
                    x + s(2f), y + s(2f), s(b.w - 4f), s(b.h - 4f),
                    Color(0xFFB6FFC6), alpha = 0.25f * pulse,
                )
            }

            TrapKind.SPIKES -> {
                val x = sx(b.x)
                val y = sy(b.top)
                var sxp = x
                val step = s(9f)
                while (sxp < x + s(b.w)) {
                    drawSvg(SvgPaths.TRAP_SPIKE, sxp, y - s(3.5f), step, s(4f), Color(0xFF9AA0B2))
                    sxp += step
                }
            }
        }
    }

    private fun DrawScope.drawPickup(pickup: Pickup, time: Float) {
        val b = pickup.bounds
        val bob = sin(time * 3f + b.x) * s(1.2f)
        val x = sx(b.x)
        val y = sy(b.top) + bob
        val w = s(b.w)
        val h = s(b.h)
        when (pickup.kind) {
            PickupKind.COIN -> {
                // spin: horizontal squash simulates a rotating coin
                val spin = abs(cos(time * 3.4f + b.x * 0.2f))
                val cw = w * (0.25f + 0.75f * spin)
                drawSvg(SvgPaths.COIN_BODY, x + (w - cw) / 2f, y, cw, h, Color(0xFFFFC93C))
                drawSvg(
                    SvgPaths.COIN_INNER,
                    x + (w - cw) / 2f, y, cw, h,
                    Color(0xFFFFE9A3), alpha = 0.9f,
                )
                if (spin > 0.5f) {
                    drawSvg(
                        SvgPaths.COIN_MARK,
                        x + (w - cw * 0.5f) / 2f, y + h * 0.25f, cw * 0.5f, h * 0.5f,
                        Color(0xFFB07A12),
                    )
                }
            }

            PickupKind.GEM -> {
                val glow = 0.5f + 0.5f * sin(time * 4f)
                drawCircle(
                    Color(0xFF4FD6FF).copy(alpha = 0.25f * glow),
                    w * 1.1f,
                    Offset(x + w / 2f, y + h / 2f),
                )
                drawSvg(SvgPaths.GEM_BODY, x, y, w, h, Color(0xFF4FD6FF))
                drawSvg(SvgPaths.GEM_FACET, x, y, w, h, Color(0xFFBFF3FF), alpha = 0.8f)
            }

            PickupKind.HEART -> {
                val pulse = 1f + 0.12f * sin(time * 6f)
                drawSvg(
                    SvgPaths.HEART_BODY,
                    x - w * (pulse - 1f) / 2f, y - h * (pulse - 1f) / 2f,
                    w * pulse, h * pulse,
                    Color(0xFFC8203C),
                )
            }

            PickupKind.SHIELD -> {
                drawSvg(SvgPaths.SHIELD_BODY, x, y, w, h, Color(0xFF4F7FD6))
                drawSvg(SvgPaths.SHIELD_INNER, x, y, w, h, Color(0xFFBBD7FF), alpha = 0.85f)
            }

            PickupKind.WINGS -> drawSvg(SvgPaths.WINGS_BODY, x, y, w, h, Color(0xFFE8E2D0))
            PickupKind.SPEED -> drawSvg(SvgPaths.SPEED_BOLT, x, y, w, h, Color(0xFFFFE45C))
            PickupKind.MAGNET -> {
                drawSvg(SvgPaths.MAGNET_BODY, x, y, w, h, Color(0xFFD6444F))
                drawSvg(SvgPaths.MAGNET_TIP, x, y, w, h, Color(0xFFE8E2D0))
            }
        }
    }

    private fun DrawScope.drawGate(floor: FloorData, tower: TowerStyle, time: Float) {
        val w = s(30f)
        val h = s(24f)
        val x = sx(GameConfig.WORLD_WIDTH / 2f - 15f)
        val y = sy(floor.baseY + floor.height) - h * 0.2f
        drawSvg(SvgPaths.GATE_ARCH, x, y, w, h, tower.wallDark)
        drawSvg(
            SvgPaths.SKULL_MARK,
            x + w * 0.36f, y + h * 0.18f, w * 0.28f, h * 0.4f,
            tower.accent.copy(alpha = 0.55f + 0.35f * sin(time * 3f)),
        )
    }

    // -------------------------------------------------------------------------------------
    // Enemies
    // -------------------------------------------------------------------------------------

    private fun DrawScope.drawEnemy(enemy: Enemy, tower: TowerStyle, time: Float) {
        val dying = !enemy.alive
        val alpha = if (dying) (enemy.deathTimer / 0.7f).coerceIn(0f, 1f) else 1f
        if (alpha <= 0.01f) return
        val hurt = enemy.state == EnemyState.HIT && enemy.stateTime < 0.18f
        val b = enemy.bounds
        val x = sx(b.x)
        val y = sy(b.top)
        val w = s(b.w)
        val h = s(b.h)
        val flip = enemy.facing > 0
        val deathRot = if (dying) (1f - alpha) * 80f else 0f
        val tint: Color? = if (hurt) Color.White else null

        when (enemy.kind) {
            EnemyKind.STONE_SERPENT -> {
                val wiggle = sin(enemy.animTime * 6f) * h * 0.12f
                drawSvg(
                    SvgPaths.SERPENT_BODY, x, y + wiggle, w, h, tint ?: Color(0xFF7A8B6B),
                    alpha = alpha, rotation = deathRot, flipX = !flip,
                )
                drawSvg(
                    SvgPaths.SERPENT_HEAD, x, y + wiggle, w, h, tint ?: Color(0xFF93A683),
                    alpha = alpha, rotation = deathRot, flipX = !flip,
                )
                drawSvg(
                    SvgPaths.SERPENT_EYE, x, y + wiggle, w, h, Color(0xFFFF7A18),
                    alpha = alpha, rotation = deathRot, flipX = !flip,
                )
            }

            EnemyKind.EVIL_BAT -> {
                val flap = sin(enemy.animTime * 14f)
                val wingH = h * (0.5f + 0.35f * abs(flap))
                drawSvg(
                    SvgPaths.BAT_WING, x - w * 0.35f, y + h * 0.1f - flap * h * 0.15f,
                    w * 0.9f, wingH, tint ?: Color(0xFF43305C), alpha = alpha,
                )
                drawSvg(
                    SvgPaths.BAT_WING, x + w * 0.45f, y + h * 0.1f - flap * h * 0.15f,
                    w * 0.9f, wingH, tint ?: Color(0xFF43305C), alpha = alpha, flipX = true,
                )
                drawSvg(SvgPaths.BAT_EAR, x, y, w, h, tint ?: Color(0xFF2C1F40), alpha = alpha)
                drawSvg(SvgPaths.BAT_BODY, x, y, w, h, tint ?: Color(0xFF35254D), alpha = alpha)
                drawSvg(SvgPaths.BAT_EYE, x, y, w, h, Color(0xFFFF3B57), alpha = alpha)
                if (enemy.state == EnemyState.ATTACK) {
                    drawCircle(
                        Color(0xFFFF3B57).copy(alpha = 0.15f * alpha),
                        w * 0.9f, Offset(x + w / 2f, y + h / 2f),
                    )
                }
            }

            EnemyKind.SKELETON_WARRIOR -> {
                val walk = if (enemy.state == EnemyState.MOVE) sin(enemy.animTime * 8f) else 0f
                // legs
                drawLimb(x + w * 0.34f, y + h * 0.62f, w * 0.14f, h * 0.36f, walk * 22f, Color(0xFFE8E2D0), alpha)
                drawLimb(x + w * 0.54f, y + h * 0.62f, w * 0.14f, h * 0.36f, -walk * 22f, Color(0xFFCFC7B2), alpha)
                drawSvg(SvgPaths.SKELETON_RIBS, x, y, w, h, tint ?: Color(0xFFE8E2D0), alpha = alpha, flipX = !flip)
                drawSvg(SvgPaths.SKELETON_JAW, x, y, w, h, tint ?: Color(0xFFCFC7B2), alpha = alpha, flipX = !flip)
                drawSvg(
                    SvgPaths.SKELETON_SKULL, x, y - h * 0.02f, w, h * 0.55f,
                    tint ?: Color(0xFFF3EEDF), alpha = alpha, rotation = deathRot, flipX = !flip,
                )
                drawCircle(Color(0xFFFF7A18), s(0.9f), Offset(x + w * (if (flip) 0.6f else 0.4f), y + h * 0.14f), alpha = alpha)
                // axe swing
                val swing = when (enemy.state) {
                    EnemyState.ATTACK -> -70f + (enemy.stateTime / 0.35f).coerceAtMost(1f) * 130f
                    else -> -18f
                }
                drawSvg(
                    SvgPaths.SKELETON_AXE,
                    x + (if (flip) w * 0.55f else -w * 0.25f), y + h * 0.3f,
                    w * 0.7f, h * 0.35f,
                    tint ?: Color(0xFF9AA0B2), alpha = alpha,
                    rotation = if (flip) swing else -swing, pivotX = if (flip) 0f else 1f, pivotY = 0.5f,
                    flipX = !flip,
                )
            }

            EnemyKind.SHADOW_WOLF -> {
                val run = if (enemy.chaseTimer > 0f) sin(enemy.animTime * 16f) else sin(enemy.animTime * 3f) * 0.3f
                drawSvg(SvgPaths.WOLF_TAIL, x, y + run * h * 0.06f, w, h, tint ?: Color(0xFF241B33), alpha = alpha, flipX = !flip)
                drawLimb(x + w * 0.22f, y + h * 0.6f, w * 0.12f, h * 0.42f, run * 30f, Color(0xFF1C1428), alpha)
                drawLimb(x + w * 0.66f, y + h * 0.6f, w * 0.12f, h * 0.42f, -run * 30f, Color(0xFF1C1428), alpha)
                drawSvg(SvgPaths.WOLF_BODY, x, y, w, h, tint ?: Color(0xFF2E2242), alpha = alpha, rotation = deathRot, flipX = !flip)
                drawSvg(SvgPaths.WOLF_HEAD, x, y - run * h * 0.03f, w, h, tint ?: Color(0xFF3A2B52), alpha = alpha, flipX = !flip)
                drawSvg(SvgPaths.WOLF_EYE, x, y - run * h * 0.03f, w, h, Color(0xFFFF3B57), alpha = alpha, flipX = !flip)
                if (enemy.chaseTimer > 0f) {
                    drawCircle(Color(0xFFFF3B57).copy(alpha = 0.12f), w * 0.7f, Offset(x + w / 2f, y + h / 2f))
                }
            }

            EnemyKind.GATE_GUARDIAN -> {
                val breathe = 1f + 0.03f * sin(enemy.animTime * 3f)
                val slam = if (enemy.state == EnemyState.ATTACK) {
                    (enemy.stateTime / 0.5f).coerceIn(0f, 1f)
                } else 0f
                drawSvg(SvgPaths.GUARDIAN_HORN, x, y, w, h * breathe, tint ?: Color(0xFFE8E2D0), alpha = alpha)
                drawSvg(SvgPaths.GUARDIAN_BODY, x, y, w, h * breathe, tint ?: Color(0xFF6B2F3E), alpha = alpha, rotation = deathRot)
                drawSvg(SvgPaths.GUARDIAN_EYE, x, y, w, h * breathe, Color(0xFFFFD27D), alpha = alpha)
                drawSvg(SvgPaths.GUARDIAN_MOUTH, x, y, w, h * breathe, Color(0xFF1A0D12), alpha = alpha)
                // arms slam down when the attack lands
                drawLimb(x - w * 0.12f, y + h * 0.34f, w * 0.2f, h * 0.4f, -35f + slam * 70f, Color(0xFF7E3A48), alpha)
                drawLimb(x + w * 0.92f, y + h * 0.34f, w * 0.2f, h * 0.4f, 35f - slam * 70f, Color(0xFF7E3A48), alpha)
                if (enemy.telegraph > 0f) {
                    drawCircle(
                        Color(0xFFFF3B57).copy(alpha = 0.35f * enemy.telegraph / 0.45f),
                        w * 0.9f, Offset(x + w / 2f, y + h * 0.4f),
                    )
                }
                drawBossBar(enemy, x, y, w)
            }

            EnemyKind.TOWER_LORD -> {
                val float = sin(enemy.animTime * 2f) * h * 0.03f
                val aura = 0.4f + 0.3f * sin(enemy.animTime * 4f)
                drawCircle(
                    Color(0xFF9B5BFF).copy(alpha = 0.18f * aura),
                    w * 1.1f, Offset(x + w / 2f, y + h * 0.5f + float),
                )
                drawSvg(
                    SvgPaths.LORD_STAFF,
                    x + (if (flip) w * 0.86f else -w * 0.1f), y + h * 0.1f + float,
                    w * 0.12f, h * 0.95f, Color(0xFF4A3A2A), alpha = alpha,
                )
                drawSvg(
                    SvgPaths.LORD_ORB,
                    x + (if (flip) w * 0.78f else -w * 0.18f), y + float - h * 0.06f,
                    w * 0.28f, h * 0.2f,
                    Color(0xFF7BE38B).copy(alpha = 0.9f), alpha = alpha,
                )
                drawSvg(SvgPaths.LORD_ROBE, x, y + float, w, h, tint ?: Color(0xFF2E1B4C), alpha = alpha, rotation = deathRot)
                drawSvg(SvgPaths.LORD_HOOD, x, y + float, w, h * 0.55f, tint ?: Color(0xFF241239), alpha = alpha)
                drawSvg(SvgPaths.LORD_FACE, x, y + float, w, h * 0.55f, Color(0xFF0B0713), alpha = alpha)
                drawSvg(SvgPaths.LORD_EYE, x, y + float, w, h * 0.55f, Color(0xFFFF3B57), alpha = alpha)
                if (enemy.state == EnemyState.ATTACK) {
                    drawCircle(
                        Color(0xFFFF3B57).copy(alpha = 0.22f),
                        w * (0.8f + enemy.stateTime), Offset(x + w / 2f, y + h * 0.5f),
                    )
                }
                drawBossBar(enemy, x, y, w)
            }
        }
    }

    private fun DrawScope.drawBossBar(enemy: Enemy, x: Float, y: Float, w: Float) {
        if (!enemy.alive) return
        val barW = w * 1.2f
        val barX = x - (barW - w) / 2f
        val barY = y - s(7f)
        drawRect(Color(0x99000000), Offset(barX, barY), Size(barW, s(2.6f)))
        drawRect(
            Color(0xFFC8203C),
            Offset(barX, barY),
            Size(barW * (enemy.health.toFloat() / enemy.maxHealth.toFloat()).coerceIn(0f, 1f), s(2.6f)),
        )
    }

    /** A limb is a rounded vector bar rotated around its top end. */
    private fun DrawScope.drawLimb(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        angle: Float,
        color: Color,
        alpha: Float = 1f,
    ) {
        drawSvg(SvgPaths.HERO_LIMB, x, y, w, h, color, alpha = alpha, rotation = angle, pivotX = 0.5f, pivotY = 0f)
    }

    // -------------------------------------------------------------------------------------
    // Hero
    // -------------------------------------------------------------------------------------

    private fun DrawScope.drawHero(player: Player, style: HeroStyle, time: Float) {
        val b = player.bounds
        // squash & stretch feedback
        val squash = player.squash
        val stretch = when (player.state) {
            PlayerState.JUMP -> 1f + 0.12f * squash
            PlayerState.FALL -> 1.06f
            else -> 1f - 0.14f * squash
        }
        val w = s(b.w) * (2f - stretch)
        val h = s(b.h) * stretch
        val x = sx(b.centerX) - w / 2f
        val y = sy(b.top) + (s(b.h) - h)
        val flip = player.facing < 0
        val blinking = player.invulnerable > 0f && ((player.invulnerable * 14f).toInt() % 2 == 0)
        val alpha = if (blinking) 0.4f else 1f

        if (player.state == PlayerState.DEATH) {
            drawSvg(
                SvgPaths.HERO_TORSO, x, y, w, h, style.armor,
                alpha = alpha, rotation = time * 260f % 360f,
            )
            drawSvg(SvgPaths.HERO_HEAD, x, y, w, h * 0.55f, style.armorDark, alpha = alpha, rotation = time * 260f % 360f)
            return
        }

        // shield bubble
        if (player.hasPower(PowerUp.SHIELD)) {
            drawCircle(
                Color(0xFF4F7FD6).copy(alpha = 0.22f + 0.1f * sin(time * 5f)),
                w * 1.15f, Offset(x + w / 2f, y + h / 2f),
            )
            drawCircle(
                Color(0xFFBBD7FF).copy(alpha = 0.5f),
                w * 1.15f, Offset(x + w / 2f, y + h / 2f),
                style = Stroke(width = 1.5f),
            )
        }
        // wings power-up
        if (player.hasPower(PowerUp.WINGS)) {
            val flap = sin(time * 12f) * 12f
            drawSvg(
                SvgPaths.WINGS_BODY, x - w * 0.5f, y + h * 0.12f, w * 2f, h * 0.5f,
                Color(0xFFE8E2D0), alpha = 0.85f * alpha, rotation = flap,
            )
        }
        if (player.hasPower(PowerUp.SPEED)) {
            drawCircle(
                Color(0xFFFFE45C).copy(alpha = 0.12f),
                w * 0.9f, Offset(x + w / 2f - player.facing * w * 0.4f, y + h * 0.6f),
            )
        }

        val moving = player.state == PlayerState.RUN
        val cycle = sin(player.animTime * 13f)
        val legSwing = when (player.state) {
            PlayerState.RUN -> cycle * 34f
            PlayerState.JUMP -> -22f
            PlayerState.FALL -> 16f
            else -> sin(player.animTime * 2.2f) * 4f
        }
        val armSwing = when (player.state) {
            PlayerState.RUN -> -cycle * 28f
            PlayerState.JUMP -> -40f
            PlayerState.ATTACK -> -80f + (1f - player.attackTimer / GameConfig.ATTACK_TIME) * 150f
            else -> sin(player.animTime * 2.2f + 1f) * 5f
        }

        // cape flutters behind
        drawSvg(
            SvgPaths.HERO_CAPE,
            x - (if (flip) -w * 0.16f else w * 0.16f), y + h * 0.06f + sin(time * 6f) * h * 0.02f,
            w, h * 0.9f, style.cape, alpha = alpha, flipX = flip,
            rotation = if (moving) -player.facing * 6f else 0f,
        )
        // legs
        drawLimb(x + w * 0.3f, y + h * 0.6f, w * 0.18f, h * 0.4f, legSwing, style.armorDark, alpha)
        drawLimb(x + w * 0.52f, y + h * 0.6f, w * 0.18f, h * 0.4f, -legSwing, style.armorDark, alpha)
        // torso + belt
        drawSvg(SvgPaths.HERO_TORSO, x, y, w, h * 0.78f, style.armor, alpha = alpha, flipX = flip)
        drawSvg(SvgPaths.HERO_BELT, x, y, w, h * 0.78f, style.trim, alpha = alpha, flipX = flip)
        // head
        drawSvg(SvgPaths.HERO_HEAD, x + w * 0.06f, y - h * 0.04f, w * 0.88f, h * 0.42f, style.armor, alpha = alpha, flipX = flip)
        drawSvg(SvgPaths.HERO_HELMET_CREST, x + w * 0.06f, y - h * 0.06f, w * 0.88f, h * 0.42f, style.trim, alpha = alpha, flipX = flip)
        drawSvg(SvgPaths.HERO_VISOR, x + w * 0.06f, y - h * 0.04f, w * 0.88f, h * 0.42f, style.visor, alpha = alpha, flipX = flip)
        drawCircle(
            Color(0xFFFF7A18).copy(alpha = alpha * 0.9f),
            s(0.8f),
            Offset(x + w * (if (flip) 0.34f else 0.66f), y + h * 0.12f),
        )
        // sword arm
        val armX = x + (if (flip) w * 0.1f else w * 0.62f)
        drawLimb(armX, y + h * 0.28f, w * 0.18f, h * 0.34f, if (flip) -armSwing else armSwing, style.armor, alpha)
        // sword
        val swordAngle = (if (flip) -armSwing else armSwing) + (if (flip) -35f else 35f)
        drawSvg(
            SvgPaths.HERO_SWORD,
            armX - w * 0.05f, y + h * 0.22f, w * 0.28f, h * 0.62f,
            style.blade, alpha = alpha, rotation = swordAngle, pivotX = 0.5f, pivotY = 0f,
        )
        drawSvg(
            SvgPaths.HERO_SWORD_HILT,
            armX - w * 0.12f, y + h * 0.3f, w * 0.42f, h * 0.16f,
            style.trim, alpha = alpha, rotation = swordAngle, pivotX = 0.5f, pivotY = 0f,
        )
        // attack arc
        if (player.state == PlayerState.ATTACK) {
            val t = 1f - player.attackTimer / GameConfig.ATTACK_TIME
            drawArc(
                color = style.blade.copy(alpha = 0.35f * (1f - t)),
                startAngle = if (flip) 150f else -30f,
                sweepAngle = if (flip) -120f else 120f,
                useCenter = false,
                topLeft = Offset(x + w / 2f - s(GameConfig.ATTACK_RANGE), y - s(2f)),
                size = Size(s(GameConfig.ATTACK_RANGE) * 2f, s(GameConfig.ATTACK_RANGE) * 2f),
                style = Stroke(width = s(1.6f)),
            )
        }
    }
}

/** Cheap deterministic pseudo-random in 0..1 (same value every frame for a given seed). */
private fun pseudo(seed: Float): Float {
    val v = sin(seed * 91.7f) * 43758.547f
    return v - kotlin.math.floor(v)
}
