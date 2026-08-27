package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.AnimatedTowerLogo
import ir.hadipoor.eviltower.ui.components.CurrencyChip
import ir.hadipoor.eviltower.ui.components.StonePanel
import ir.hadipoor.eviltower.ui.components.TowerButton
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers

/** منوی اصلی */
@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    persianDigits: Boolean,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onLeaderboard: () -> Unit,
    onExit: () -> Unit,
) {
    val strings = LocalStrings.current
    val num = { value: Int -> PersianNumbers.format(value, persianDigits) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TowerPalette.DeepPurple, TowerPalette.Night, TowerPalette.Shadow)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencyChip(num(profile.coins))
                CurrencyChip(num(profile.gems), gem = true)
            }

            Spacer(Modifier.height(10.dp))
            AnimatedTowerLogo(Modifier.size(150.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayMedium,
                color = TowerPalette.TextPrimary,
            )
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = TowerPalette.TextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            TowerButton(
                text = if (profile.bestFloor > 0) strings.continueClimb else strings.newGame,
                primary = true,
                icon = Icons.Filled.PlayArrow,
                onClick = onPlay,
            )
            Spacer(Modifier.height(10.dp))
            TowerButton(strings.shop, icon = Icons.Filled.ShoppingCart, onClick = onShop)
            Spacer(Modifier.height(10.dp))
            TowerButton(strings.achievements, icon = Icons.Filled.EmojiEvents, onClick = onAchievements)
            Spacer(Modifier.height(10.dp))
            TowerButton(strings.leaderboard, icon = Icons.Filled.Leaderboard, onClick = onLeaderboard)
            Spacer(Modifier.height(10.dp))
            TowerButton(strings.settings, icon = Icons.Filled.Settings, onClick = onSettings)
            Spacer(Modifier.height(10.dp))
            TowerButton(strings.exit, icon = Icons.Filled.ExitToApp, onClick = onExit)

            Spacer(Modifier.height(18.dp))
            StonePanel(Modifier.fillMaxWidth()) {
                Text(
                    text = strings.story,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TowerPalette.TextMuted,
                    textAlign = TextAlign.Justify,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${strings.bestFloor}: ${num(profile.bestFloor)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TowerPalette.EmberSoft,
                    )
                    Text(
                        text = "${strings.bestScore}: ${num(profile.bestScore)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TowerPalette.EmberSoft,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
