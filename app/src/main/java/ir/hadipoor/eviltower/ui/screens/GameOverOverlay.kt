package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.RunResult
import ir.hadipoor.eviltower.game.render.RenderStyles
import ir.hadipoor.eviltower.game.render.SvgPaths
import ir.hadipoor.eviltower.game.render.drawSvg
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.CoinIcon
import ir.hadipoor.eviltower.ui.components.GemIcon
import ir.hadipoor.eviltower.ui.components.StonePanel
import ir.hadipoor.eviltower.ui.components.TowerButton
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers

/** پایان بازی / پیروزی — shown on top of the frozen game surface. */
@Composable
fun GameOverOverlay(
    result: RunResult?,
    skinId: String,
    persianDigits: Boolean,
    adsAvailable: Boolean,
    canContinueWithAd: Boolean,
    freshAchievements: List<String>,
    onTryAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onWatchAdContinue: () -> Unit,
    onWatchAdDoubleCoins: () -> Unit,
) {
    val strings = LocalStrings.current
    val num = { value: Int -> PersianNumbers.format(value, persianDigits) }
    val victory = result?.victory == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TowerPalette.Shadow.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        StonePanel(
            Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
        ) {
            FallingHero(skinId = skinId, victory = victory)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (victory) strings.victory else strings.gameOver,
                style = MaterialTheme.typography.headlineLarge,
                color = if (victory) TowerPalette.Torch else TowerPalette.Blood,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (victory) strings.victorySub else strings.gameOverSub,
                style = MaterialTheme.typography.bodyMedium,
                color = TowerPalette.TextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))

            ResultRow(strings.floorsClimbed, num(result?.floorsClimbed ?: 0))
            ResultRow(strings.score, num(result?.score ?: 0))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.coinsEarned,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TowerPalette.TextMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = num(result?.coinsKept ?: 0),
                        style = MaterialTheme.typography.titleMedium,
                        color = TowerPalette.Gold,
                    )
                }
            }
            if ((result?.gemsCollected ?: 0) > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.gems,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TowerPalette.TextMuted,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GemIcon(Modifier.size(18.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = num(result?.gemsCollected ?: 0),
                            style = MaterialTheme.typography.titleMedium,
                            color = TowerPalette.Gem,
                        )
                    }
                }
            }

            if (result?.isNewBestScore == true || result?.isNewBestFloor == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.newRecord,
                    style = MaterialTheme.typography.titleMedium,
                    color = TowerPalette.Ember,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            if (freshAchievements.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                freshAchievements.forEach { title ->
                    Text(
                        text = "🏆 $title",
                        style = MaterialTheme.typography.labelLarge,
                        color = TowerPalette.Torch,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (adsAvailable && canContinueWithAd && !victory) {
                TowerButton(strings.watchAdContinue, primary = true, onClick = onWatchAdContinue)
                Spacer(Modifier.height(8.dp))
            }
            if (adsAvailable && (result?.coinsKept ?: 0) > 0) {
                TowerButton(strings.doubleCoins, onClick = onWatchAdDoubleCoins)
                Spacer(Modifier.height(8.dp))
            }
            TowerButton(strings.tryAgain, primary = !adsAvailable, onClick = onTryAgain)
            Spacer(Modifier.height(8.dp))
            TowerButton(strings.mainMenu, onClick = onMainMenu)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TowerPalette.TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TowerPalette.TextPrimary)
    }
}

/** The signature "hero falls" animation, drawn with the same vector sprites as the game. */
@Composable
private fun FallingHero(skinId: String, victory: Boolean) {
    val style = RenderStyles.hero(skinId)
    val transition = rememberInfiniteTransition(label = "fall")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (victory) 2600 else 1500), RepeatMode.Restart),
        label = "fallPhase",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val w = size.width
        val h = size.height
        val heroW = w * 0.18f
        val heroH = h * 0.55f
        val x = w / 2f - heroW / 2f
        val y = if (victory) {
            h * 0.25f + kotlin.math.sin(t * 2f * Math.PI.toFloat()) * h * 0.05f
        } else {
            -heroH * 0.4f + t * (h + heroH * 0.6f)
        }
        val rotation = if (victory) 0f else t * 540f

        // falling embers / victory sparks
        for (i in 0 until 10) {
            val phase = (t + i / 10f) % 1f
            val ex = w * (0.15f + 0.7f * ((i * 53 % 17) / 17f))
            val ey = if (victory) h * (0.9f - phase * 0.8f) else h * phase
            drawCircle(
                color = TowerPalette.EmberSoft.copy(alpha = (1f - phase) * 0.55f),
                radius = w * 0.008f,
                center = androidx.compose.ui.geometry.Offset(ex, ey),
            )
        }

        drawSvg(SvgPaths.HERO_CAPE, x, y, heroW, heroH, style.cape, rotation = rotation)
        drawSvg(SvgPaths.HERO_TORSO, x, y, heroW, heroH * 0.8f, style.armor, rotation = rotation)
        drawSvg(SvgPaths.HERO_HEAD, x + heroW * 0.06f, y - heroH * 0.05f, heroW * 0.88f, heroH * 0.42f, style.armor, rotation = rotation)
        drawSvg(SvgPaths.HERO_VISOR, x + heroW * 0.06f, y - heroH * 0.05f, heroW * 0.88f, heroH * 0.42f, style.visor, rotation = rotation)
        if (victory) {
            drawSvg(
                SvgPaths.WINGS_BODY, x - heroW * 0.6f, y + heroH * 0.1f, heroW * 2.2f, heroH * 0.5f,
                TowerPalette.Torch, alpha = 0.8f,
            )
        }
    }
}
