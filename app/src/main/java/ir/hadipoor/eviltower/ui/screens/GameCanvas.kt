package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import ir.hadipoor.eviltower.game.engine.Balance
import ir.hadipoor.eviltower.game.model.Enemy
import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.game.model.Point
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import ir.hadipoor.eviltower.game.render.SpriteAnimation
import ir.hadipoor.eviltower.game.render.SpriteState
import ir.hadipoor.eviltower.ui.theme.Acid
import ir.hadipoor.eviltower.ui.theme.Danger
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Stone
import ir.hadipoor.eviltower.ui.theme.StoneDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(snapshot: GameSnapshot, onPlotTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.fillMaxSize().pointerInput(snapshot.towers, snapshot.selectedPlot) {
            detectTapGestures { tap ->
                val plot = Balance.PLOTS.indices.minByOrNull { index ->
                    val p = Balance.PLOTS[index]
                    val dx = tap.x / size.width - p.x
                    val dy = tap.y / size.height - p.y
                    dx * dx + dy * dy
                } ?: return@detectTapGestures
                val p = Balance.PLOTS[plot]
                val dx = tap.x / size.width - p.x
                val dy = tap.y / size.height - p.y
                if (dx * dx + dy * dy < .055f * .055f) onPlotTap(plot)
            }
        },
    ) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF181227), Color(0xFF090711))))
        drawStars()
        drawFog(snapshot.wave, snapshot.worldTime)
        drawTowerSource(snapshot.wave, snapshot.worldTime)
        drawRoad()
        drawTorches(snapshot.worldTime)
        drawCore(snapshot.worldTime, snapshot.bossTelegraph, snapshot.shieldRemaining)
        drawPlots(snapshot)
        drawEnemies(snapshot)
        drawProjectiles(snapshot)
        drawEffects(snapshot)
    }
}

private fun DrawScope.line(start: Offset, end: Offset, color: Color, width: Float, cap: StrokeCap = StrokeCap.Butt) {
    drawLine(color, start, end, width, cap)
}

private fun DrawScope.pos(point: Point): Offset = Offset(point.x * size.width, point.y * size.height)
private fun DrawScope.pos(x: Float, y: Float): Offset = Offset(x * size.width, y * size.height)

private fun DrawScope.drawStars() {
    val stars = listOf(.08f to .12f, .21f to .08f, .31f to .20f, .49f to .08f, .76f to .11f, .89f to .25f, .15f to .32f, .58f to .26f, .94f to .46f)
    stars.forEachIndexed { index, pair -> drawCircle(Color(0x88D7C8F0), 1.2f + index % 2, pos(pair.first, pair.second)) }
}

private fun DrawScope.drawFog(wave: Int, time: Float) {
    val intensity = (.10f + (wave.coerceAtMost(300) / 300f) * .22f)
    val drift = sin(time * .18f) * size.width * .04f
    drawCircle(Color(0xFF5CA58A).copy(alpha = intensity), size.width * .25f, pos(.88f + drift / size.width, .22f))
    drawCircle(Color(0xFF5CA58A).copy(alpha = intensity * .7f), size.width * .20f, pos(.76f + drift / size.width, .54f))
    drawCircle(Color(0xFF49365F).copy(alpha = intensity), size.width * .28f, pos(.15f - drift / size.width, .76f))
}

private fun DrawScope.drawTowerSource(wave: Int, time: Float) {
    val p = pos(.92f, .16f)
    val pulse = 1f + sin(time * 2.2f) * .08f
    val scale = ((1f + (wave / 50) * .025f) * pulse).coerceAtMost(1.38f)
    val glow = 22f * scale + (wave % 20)
    drawCircle(Color(0x3328E59A), glow, p)
    drawCircle(Color(0x4428E59A), glow * .63f, p)
    val tower = Path().apply {
        moveTo(p.x - 34 * scale, p.y + 52 * scale); lineTo(p.x - 25 * scale, p.y - 42 * scale)
        lineTo(p.x - 8 * scale, p.y - 56 * scale); lineTo(p.x + 3 * scale, p.y - 44 * scale)
        lineTo(p.x + 24 * scale, p.y - 50 * scale); lineTo(p.x + 35 * scale, p.y + 52 * scale); close()
    }
    drawPath(tower, Color(0xFF321E44)); drawPath(tower, Color(0xFF6E365C), style = Stroke(2.5f))
    line(pos(.92f, .07f), pos(.92f, .22f), Color(0xFF98E36E), 5f, StrokeCap.Round)
    drawCircle(Color(0xFFB9FF83), 7f * scale, p.copy(y = p.y - 8 * scale))
    drawCircle(Color(0xFF1B122A), 2.5f, p.copy(y = p.y - 8 * scale))
}

private fun DrawScope.drawRoad() {
    val points = Balance.PATH.map(::pos)
    for (i in 0 until points.lastIndex) {
        line(points[i], points[i + 1], Color(0x552F283A), 42f, StrokeCap.Round)
        line(points[i], points[i + 1], Color(0xFF51404D), 30f, StrokeCap.Round)
        line(points[i], points[i + 1], Color(0xFF73555A), 22f, StrokeCap.Round)
    }
    points.forEach { drawCircle(Color(0x226B4F5A), 18f, it) }
}

private fun DrawScope.drawTorches(time: Float) {
    val torches = listOf(.77f to .10f, .61f to .27f, .30f to .23f, .20f to .53f, .52f to .67f, .76f to .83f)
    torches.forEachIndexed { index, (x, y) ->
        val p = pos(x, y); val flicker = 1f + sin(time * 8f + index) * .16f
        drawCircle(Color(0x33FF754C), 18f * flicker, p)
        line(p.copy(y = p.y + 8f), p.copy(y = p.y + 22f), Color(0xFF7D5B4E), 4f)
        drawCircle(Color(0xFFFFB347), 5f * flicker, p.copy(y = p.y - 3f))
        drawCircle(Color(0xFFFFE5A5), 2f, p.copy(y = p.y - 4f))
    }
}

private fun DrawScope.drawCore(time: Float, telegraph: Float, shield: Float) {
    val p = pos(Balance.PATH.last())
    val pulse = 1f + sin(time * 3f) * .08f
    if (shield > 0f) drawCircle(Color(0x5570D6FF), 58f + sin(time * 5f) * 4f, p, style = Stroke(4f))
    if (telegraph > 0f) {
        val warning = (1f - (telegraph / 2.1f).coerceIn(0f, 1f))
        drawCircle(Danger.copy(alpha = .20f + warning * .25f), 54f + warning * 18f, p, style = Stroke(4f))
    }
    drawCircle(Color(0x334C8DFF), 42f * pulse, p)
    drawCircle(Color(0xFF293C69), 27f * pulse, p)
    drawCircle(Color(0xFF69B6FF), 17f * pulse, p)
    drawCircle(Color(0xFFD9F4FF), 6f, p)
    line(p.copy(x = p.x - 34), p.copy(x = p.x + 34), Color(0xFF91CEFF), 3f)
    line(p.copy(y = p.y - 34), p.copy(y = p.y + 34), Color(0xFF91CEFF), 3f)
}

private fun DrawScope.drawPlots(snapshot: GameSnapshot) {
    Balance.PLOTS.forEachIndexed { index, point ->
        val center = pos(point)
        val selected = snapshot.selectedPlot == index
        drawCircle(if (selected) Color(0x5570D6FF) else Color(0x33241C34), 25f, center)
        drawCircle(if (selected) Color(0xFF70D6FF) else Color(0xFF6E536B), 20f, center, style = Stroke(2f))
        val tower = snapshot.towers.firstOrNull { it.plot == index }
        if (selected && tower != null) {
            drawCircle(tower.type.color.copy(alpha = .18f), Balance.towerRange(tower) * minOf(size.width, size.height), center, style = Stroke(2f))
        }
        if (tower == null) {
            line(center.copy(x = center.x - 7), center.copy(x = center.x + 7), Color(0xFFB9A4B7), 2f)
            line(center.copy(y = center.y - 7), center.copy(y = center.y + 7), Color(0xFFB9A4B7), 2f)
        } else {
            drawTower(tower, center, snapshot.worldTime, snapshot.skin)
            if (tower.webbed > 0f) {
                drawCircle(Color(0x99FFC1E3), 23f, center, style = Stroke(2f))
                line(center + Offset(-16f, -16f), center + Offset(16f, 16f), Color(0x99FFC1E3), 1.5f)
                line(center + Offset(16f, -16f), center + Offset(-16f, 16f), Color(0x99FFC1E3), 1.5f)
            }
        }
    }
}

private fun DrawScope.drawTower(tower: Tower, center: Offset, time: Float, skin: Int) {
    val tier = (tower.level - 1) / 10
    val detail = (tower.level - 1) % 10
    val color = if (skin == 1) when (tower.type) {
        TowerType.FROST -> Color(0xFFB7F2FF)
        TowerType.ARCANE -> Color(0xFFFFA6E8)
        TowerType.LIGHTNING -> Color(0xFFFFC857)
        else -> Color(0xFFFF8C69)
    } else tower.type.color
    val pulse = 1f + (tower.upgradePulse * .16f)
    val sway = SpriteAnimation.sample(SpriteState.IDLE, time, tower.id).tilt * 2f
    drawCircle(color.copy(alpha = .10f + tier * .012f), (23f + tier * 2f) * pulse, center)
    drawCircle(StoneDark, 16f * pulse, center.copy(y = center.y + 8f))
    round(color.copy(alpha = .75f), Rect(center.x - (8 + tier) * pulse, center.y - 13 * pulse, center.x + (8 + tier) * pulse, center.y + 12 * pulse), 4f)
    when (tower.type) {
        TowerType.ARCHER, TowerType.SKY_ARCHER -> {
            line(center.copy(x = center.x - 15), center.copy(x = center.x + 15, y = center.y + sway), color, 3f)
            drawArc(color, 205f, 130f, false, topLeft = Offset(center.x - 13, center.y - 15), size = androidx.compose.ui.geometry.Size(26f, 25f), style = Stroke(3f))
        }
        TowerType.CANNON -> { drawCircle(color, (10 + tier).toFloat(), center.copy(y = center.y - 8)); line(center.copy(y = center.y - 8), center.copy(x = center.x + 20, y = center.y - 17 + sway), color, 6f, StrokeCap.Round) }
        TowerType.FROST -> { drawCircle(Color(0xFFDBF8FF), (8 + tier).toFloat(), center.copy(y = center.y - 7)); repeat(4) { i -> line(center.copy(y = center.y - 8), center + Offset(cos(i * 1.57f) * 18, sin(i * 1.57f) * 18), color, 2f) } }
        TowerType.FIRE -> { val flame = Path().apply { moveTo(center.x, center.y - 22); quadraticTo(center.x - 14, center.y - 5, center.x, center.y + 3); quadraticTo(center.x + 14, center.y - 5, center.x, center.y - 22); close() }; drawPath(flame, color); drawCircle(Gold, 4f, center.copy(y = center.y - 9)) }
        TowerType.LIGHTNING -> { drawCircle(color, 7f + tier, center.copy(y = center.y - 9)); line(center.copy(x = center.x - 5, y = center.y - 16), center.copy(x = center.x + 4, y = center.y + 1), color, 4f); line(center.copy(x = center.x + 4, y = center.y + 1), center.copy(x = center.x - 5, y = center.y + 10), color, 4f) }
        TowerType.ARCANE -> { drawCircle(color, 8f + tier, center.copy(y = center.y - 8)); drawCircle(Color.White, 3f, center.copy(y = center.y - 8)); line(center.copy(x = center.x - 13, y = center.y + 4), center.copy(x = center.x + 13, y = center.y + 4), color, 3f) }
    }
    repeat(tier.coerceAtMost(6)) { i -> drawCircle(Gold.copy(alpha = .7f), 2.2f, center + Offset(-10f + i * 4f, 20f)) }
    repeat(detail / 3) { i -> drawCircle(Color.White.copy(alpha = .45f), 1.5f, center + Offset(-8f + i * 8f, -24f)) }
}

private fun DrawScope.drawEnemies(snapshot: GameSnapshot) {
    snapshot.enemies.forEach { enemy ->
        val base = pos(positionOf(enemy.progress))
        val bob = SpriteAnimation.sample(if (enemy.flying) SpriteState.MOVE else SpriteState.IDLE, snapshot.worldTime, enemy.id).bob * if (enemy.flying) 1.6f else 1f
        val p = base.copy(y = base.y + bob)
        val radius = when (enemy.type) { EnemyType.BOSS -> 24f; EnemyType.MINI_BOSS -> 18f; EnemyType.OGRE -> 15f; else -> 10f }
        if (enemy.hitFlash > 0f) drawCircle(Color.White.copy(alpha = .7f), radius + 5f, p)
        when (enemy.type) {
            EnemyType.BAT -> { drawCircle(Color(0xFF8E65D1), radius, p); line(p.copy(x = p.x - 8, y = p.y - 2), p.copy(x = p.x - 22, y = p.y - 12), Color(0xFFC19BFF), 5f); line(p.copy(x = p.x + 8, y = p.y - 2), p.copy(x = p.x + 22, y = p.y - 12), Color(0xFFC19BFF), 5f) }
            EnemyType.WOLF -> { drawOval(Color(0xFF5A477F), topLeft = Offset(p.x - 14, p.y - 8), size = androidx.compose.ui.geometry.Size(28f, 16f)); drawCircle(Color(0xFFB8A3FF), 3f, p.copy(x = p.x + 8, y = p.y - 2)) }
            EnemyType.SKELETON -> { drawCircle(Color(0xFFE5D6B8), radius, p); drawCircle(Color(0xFF2A1E2E), 3f, p.copy(x = p.x - 4, y = p.y - 2)); drawCircle(Color(0xFF2A1E2E), 3f, p.copy(x = p.x + 4, y = p.y - 2)); line(p.copy(y = p.y + 8), p.copy(y = p.y + 19), Color(0xFFE5D6B8), 5f) }
            EnemyType.OGRE -> { round(Color(0xFF6E806C), Rect(p.x - 15, p.y - 16, p.x + 15, p.y + 16), 8f); drawCircle(Color(0xFFFFC857), 3f, p.copy(x = p.x - 6, y = p.y - 4)); drawCircle(Color(0xFFFFC857), 3f, p.copy(x = p.x + 6, y = p.y - 4)) }
            EnemyType.SPIDER -> {
                drawCircle(Color(0xFFC65FA2), radius, p)
                repeat(4) { i ->
                    val y = -7f + i * 5f
                    line(p.copy(x = p.x - 7, y = p.y + y), p.copy(x = p.x - 22, y = p.y + y - 6), Color(0xFFEAA4D0), 2.5f)
                    line(p.copy(x = p.x + 7, y = p.y + y), p.copy(x = p.x + 22, y = p.y + y - 6), Color(0xFFEAA4D0), 2.5f)
                }
                drawCircle(Color.White, 2f, p.copy(x = p.x - 4, y = p.y - 2)); drawCircle(Color.White, 2f, p.copy(x = p.x + 4, y = p.y - 2))
            }
            EnemyType.WRAITH -> { val alpha = if (enemy.stealth) .22f else .68f; drawCircle(Color(0xFF7B5BA7).copy(alpha = alpha), radius + 4f, p); drawCircle(Color(0xFFD9B8FF).copy(alpha = alpha + .2f), 3f, p.copy(x = p.x + 5, y = p.y - 2)) }
            EnemyType.IMP -> { drawCircle(Color(0xFFE95645), radius, p); line(p.copy(x = p.x - 5, y = p.y - 7), p.copy(x = p.x - 11, y = p.y - 16), Color(0xFFFFB347), 3f); line(p.copy(x = p.x + 5, y = p.y - 7), p.copy(x = p.x + 11, y = p.y - 16), Color(0xFFFFB347), 3f) }
            EnemyType.MINI_BOSS, EnemyType.BOSS -> {
                val designs = listOf(Color(0xFFB52F5B), Color(0xFF6D4CC2), Color(0xFFCF553D), Color(0xFF2D8C9B), Color(0xFF7F3E5B), Color(0xFFE36A2E))
                val designColor = designs[enemy.bossDesign % designs.size]
                drawCircle(designColor.copy(alpha = .18f), radius + 10f, p)
                drawCircle(if (enemy.type == EnemyType.BOSS) when (enemy.bossPhase) { 1 -> designColor; 2 -> Color(0xFFCF553D); else -> Color(0xFFE36A2E) } else designColor, radius, p)
                drawCircle(Gold, 4f, p.copy(y = p.y - 3))
                if (enemy.bossDesign % 2 == 0) {
                    line(p.copy(x = p.x - radius, y = p.y - radius - 4), p.copy(x = p.x - 5, y = p.y - radius - 14), Gold, 3f)
                    line(p.copy(x = p.x + radius, y = p.y - radius - 4), p.copy(x = p.x + 5, y = p.y - radius - 14), Gold, 3f)
                } else {
                    line(p.copy(x = p.x - radius - 4), p.copy(x = p.x - radius - 16), Gold, 3f)
                    line(p.copy(x = p.x + radius + 4), p.copy(x = p.x + radius + 16), Gold, 3f)
                }
            }
            else -> {
                drawCircle(if (enemy.elite) Color(0xFFB9455F) else Acid, radius, p)
                drawCircle(Color(0xFF211828), 3f, p.copy(x = p.x + 4, y = p.y - 2))
                if (enemy.elite) { line(p.copy(x = p.x - 7, y = p.y - 7), p.copy(x = p.x - 13, y = p.y - 17), Danger, 2.5f); line(p.copy(x = p.x + 3, y = p.y - 7), p.copy(x = p.x + 9, y = p.y - 17), Danger, 2.5f) }
            }
        }
        round(Color(0xAA0A0710), Rect(p.x - radius, p.y - radius - 10, p.x + radius, p.y - radius - 6), 2f)
        round(if (enemy.type == EnemyType.BOSS) Danger else Color(0xFF7CE38B), Rect(p.x - radius, p.y - radius - 10, p.x - radius + 2 * radius * (enemy.hp / enemy.maxHp).coerceIn(0f, 1f), p.y - radius - 6), 2f)
    }
}

private fun DrawScope.drawProjectiles(snapshot: GameSnapshot) {
    snapshot.projectiles.forEach { projectile ->
        val p = Point(projectile.from.x + (projectile.to.x - projectile.from.x) * projectile.progress, projectile.from.y + (projectile.to.y - projectile.from.y) * projectile.progress)
        val screen = pos(p); val color = projectile.towerType.color
        line(pos(projectile.from), screen, color.copy(alpha = .28f), 2.5f, StrokeCap.Round)
        drawCircle(color.copy(alpha = .18f), 12f, screen)
        when (projectile.towerType) {
            TowerType.LIGHTNING -> {
                line(pos(projectile.from), screen, color, 3f, StrokeCap.Round)
                line(screen, pos(projectile.to), Color.White.copy(alpha = .7f), 2f, StrokeCap.Round)
            }
            TowerType.CANNON -> { drawCircle(color, 6f, screen); drawCircle(Gold, 2f, screen) }
            TowerType.FROST -> { drawLine(color, screen + Offset(-6f, 4f), screen + Offset(6f, -4f), 4f, StrokeCap.Round); drawCircle(Color.White, 2f, screen) }
            TowerType.FIRE -> { drawCircle(color, 5f, screen); drawCircle(Gold, 2f, screen) }
            TowerType.ARCANE -> { drawCircle(color, 5f, screen); drawCircle(Color.White, 2f, screen) }
            else -> { line(screen + Offset(-9f, 5f), screen + Offset(9f, -5f), color, 3f, StrokeCap.Round) }
        }
    }
}

private fun DrawScope.drawEffects(snapshot: GameSnapshot) {
    snapshot.particles.forEach { particle ->
        val p = pos(particle.at); val alpha = (1f - particle.age / .78f).coerceIn(0f, 1f)
        val angle = particle.id * .77f; val distance = particle.age * 55f
        drawCircle(particle.color.copy(alpha = alpha), (3f * particle.size) * alpha, p + Offset(cos(angle) * distance, sin(angle) * distance))
    }
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.CENTER }
        snapshot.floatingTexts.forEach { text ->
            val alpha = (1f - text.age / 1.35f).coerceIn(0f, 1f)
            paint.color = android.graphics.Color.argb((alpha * 255).toInt(), (text.color.red * 255).toInt(), (text.color.green * 255).toInt(), (text.color.blue * 255).toInt())
            val p = pos(text.at); canvas.nativeCanvas.drawText(text.text, p.x, p.y - text.age * 50f, paint)
        }
    }
}

private fun DrawScope.round(color: Color, rect: Rect, radius: Float) {
    drawRoundRect(color, topLeft = rect.topLeft, size = rect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius))
}

private fun positionOf(progress: Float): Point {
    val value = progress.coerceIn(0f, .9999f) * (Balance.PATH.size - 1)
    val index = value.toInt().coerceIn(0, Balance.PATH.size - 2)
    val t = value - index
    val a = Balance.PATH[index]; val b = Balance.PATH[index + 1]
    return Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}
