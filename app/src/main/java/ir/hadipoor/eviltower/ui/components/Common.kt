package ir.hadipoor.eviltower.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.game.render.SvgPaths
import ir.hadipoor.eviltower.game.render.drawSvg
import ir.hadipoor.eviltower.ui.theme.TowerPalette

/**
 * Reusable UI atoms of برج شیطانی.
 * All icons are the same animated vector paths the game itself renders ([SvgPaths]) — no PNGs.
 */

/** Generic animated SVG icon: draws a cached vector path with an optional live animation. */
@Composable
fun SvgIcon(
    pathData: String,
    color: Color,
    modifier: Modifier = Modifier.size(24.dp),
    secondaryPath: String? = null,
    secondaryColor: Color = Color.White,
    animation: IconAnimation = IconAnimation.None,
) {
    val transition = rememberInfiniteTransition(label = "icon")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animation.durationMs, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = if (animation == IconAnimation.Pulse) RepeatMode.Reverse else RepeatMode.Restart,
        ),
        label = "iconPhase",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (animation) {
            IconAnimation.None -> {
                drawSvg(pathData, 0f, 0f, w, h, color)
                secondaryPath?.let { drawSvg(it, 0f, 0f, w, h, secondaryColor) }
            }

            IconAnimation.Spin -> {
                // coin spin: horizontal squash around the centre
                val squash = kotlin.math.abs(kotlin.math.cos(t * Math.PI.toFloat()))
                val cw = w * (0.18f + 0.82f * squash)
                drawSvg(pathData, (w - cw) / 2f, 0f, cw, h, color)
                secondaryPath?.let { drawSvg(it, (w - cw) / 2f, 0f, cw, h, secondaryColor) }
            }

            IconAnimation.Pulse -> {
                val scale = 0.88f + 0.12f * t
                val pw = w * scale
                val ph = h * scale
                drawSvg(pathData, (w - pw) / 2f, (h - ph) / 2f, pw, ph, color)
                secondaryPath?.let { drawSvg(it, (w - pw) / 2f, (h - ph) / 2f, pw, ph, secondaryColor) }
            }

            IconAnimation.Rotate -> {
                drawSvg(pathData, 0f, 0f, w, h, color, rotation = t * 360f)
                secondaryPath?.let { drawSvg(it, 0f, 0f, w, h, secondaryColor, rotation = t * 360f) }
            }

            IconAnimation.Float -> {
                val dy = kotlin.math.sin(t * 2f * Math.PI.toFloat()) * h * 0.06f
                drawSvg(pathData, 0f, dy, w, h, color)
                secondaryPath?.let { drawSvg(it, 0f, dy, w, h, secondaryColor) }
            }
        }
    }
}

enum class IconAnimation(val durationMs: Int) {
    None(1000), Spin(1400), Pulse(700), Rotate(1200), Float(2200)
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier.size(22.dp)) = SvgIcon(
    pathData = SvgPaths.COIN_BODY,
    color = TowerPalette.Gold,
    secondaryPath = SvgPaths.COIN_MARK,
    secondaryColor = Color(0xFFB07A12),
    modifier = modifier,
    animation = IconAnimation.Spin,
)

@Composable
fun GemIcon(modifier: Modifier = Modifier.size(22.dp)) = SvgIcon(
    pathData = SvgPaths.GEM_BODY,
    color = TowerPalette.Gem,
    secondaryPath = SvgPaths.GEM_FACET,
    secondaryColor = Color(0xFFBFF3FF),
    modifier = modifier,
    animation = IconAnimation.Float,
)

@Composable
fun HeartIcon(filled: Boolean, modifier: Modifier = Modifier.size(24.dp)) = SvgIcon(
    pathData = SvgPaths.HEART_BODY,
    color = if (filled) TowerPalette.Blood else TowerPalette.StoneDark,
    modifier = modifier,
    animation = if (filled) IconAnimation.Pulse else IconAnimation.None,
)

/** Ornate stone panel used by every menu. */
@Composable
fun StonePanel(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(TowerPalette.Purple.copy(alpha = 0.92f), TowerPalette.DeepPurple.copy(alpha = 0.96f))
                )
            )
            .border(BorderStroke(1.dp, TowerPalette.PurpleLight.copy(alpha = 0.55f)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content,
    )
}

/** Big ember-lit menu button with a torch glow on the leading edge. */
@Composable
fun TowerButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val bg = if (primary) {
        Brush.horizontalGradient(listOf(TowerPalette.Ember, Color(0xFFB4460C)))
    } else {
        Brush.horizontalGradient(listOf(TowerPalette.Purple, TowerPalette.DeepPurple))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                BorderStroke(1.dp, if (primary) TowerPalette.Torch else TowerPalette.PurpleLight.copy(alpha = 0.6f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 13.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (primary) TowerPalette.Shadow else TowerPalette.EmberSoft,
                    modifier = Modifier.size(20.dp).padding(end = 0.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (primary) TowerPalette.Shadow else TowerPalette.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            )
        }
    }
}

/** Small currency chip (coins / gems) used in the top bar of every screen. */
@Composable
fun CurrencyChip(amount: String, gem: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(TowerPalette.Shadow.copy(alpha = 0.55f))
            .border(1.dp, TowerPalette.PurpleLight.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (gem) GemIcon(Modifier.size(18.dp)) else CoinIcon(Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = if (gem) TowerPalette.Gem else TowerPalette.Gold,
        )
    }
}

/** Animated tower logo used by the splash screen and the main menu. */
@Composable
fun AnimatedTowerLogo(modifier: Modifier = Modifier.size(140.dp)) {
    val transition = rememberInfiniteTransition(label = "logo")
    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glow",
    )
    val ember by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Restart),
        label = "ember",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // glow behind the tower
        drawCircle(
            brush = Brush.radialGradient(
                listOf(TowerPalette.Ember.copy(alpha = 0.25f * glow), Color.Transparent),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
        )
        drawSvg(SvgPaths.TOWER_LOGO, w * 0.18f, h * 0.08f, w * 0.64f, h * 0.84f, TowerPalette.Purple)
        drawSvg(
            SvgPaths.TOWER_LOGO, w * 0.18f, h * 0.08f, w * 0.64f, h * 0.84f,
            TowerPalette.PurpleLight, style = Stroke(width = 2f),
        )
        // window ember
        drawCircle(
            color = TowerPalette.Ember.copy(alpha = 0.35f + 0.6f * glow),
            radius = w * 0.05f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.6f),
        )
        // rising embers
        for (i in 0 until 8) {
            val phase = (ember + i / 8f) % 1f
            val ex = w * (0.3f + 0.4f * ((i * 37 % 10) / 10f))
            val ey = h * (0.95f - phase * 0.8f)
            drawCircle(
                color = TowerPalette.EmberSoft.copy(alpha = (1f - phase) * 0.7f),
                radius = w * 0.012f,
                center = androidx.compose.ui.geometry.Offset(ex, ey),
            )
        }
    }
}
