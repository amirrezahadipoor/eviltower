package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.ScreenScaffold
import ir.hadipoor.eviltower.ui.components.StonePanel
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** امتیازات برتر — local, offline high-score table (no account, no Play Games). */
@Composable
fun LeaderboardScreen(
    profile: PlayerProfile,
    persianDigits: Boolean,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val num = { value: Int -> PersianNumbers.format(value, persianDigits) }
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    ScreenScaffold(title = strings.leaderboard, onBack = onBack) { modifier ->
        Column(modifier.padding(horizontal = 14.dp)) {
            StonePanel(Modifier.fillMaxWidth()) {
                StatRow(strings.bestScore, num(profile.bestScore))
                StatRow(strings.bestFloor, num(profile.bestFloor))
                StatRow(strings.totalCoins, num(profile.totalCoins))
                StatRow(strings.totalRuns, num(profile.totalRuns))
                StatRow(strings.enemiesDefeated, num(profile.totalEnemies))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = strings.localScoresNote,
                style = MaterialTheme.typography.bodySmall,
                color = TowerPalette.TextMuted,
            )
            Spacer(Modifier.height(10.dp))

            if (profile.recentRuns.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        strings.noScores,
                        style = MaterialTheme.typography.titleMedium,
                        color = TowerPalette.StoneLight,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(profile.recentRuns) { index, run ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TowerPalette.DeepPurple.copy(alpha = 0.9f))
                                .border(
                                    1.dp,
                                    if (index == 0) TowerPalette.Ember.copy(alpha = 0.7f)
                                    else TowerPalette.PurpleLight.copy(alpha = 0.25f),
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (index == 0) TowerPalette.Ember else TowerPalette.Purple
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = num(index + 1),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (index == 0) TowerPalette.Shadow else TowerPalette.TextPrimary,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "${strings.score}: ${num(run.score)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TowerPalette.TextPrimary,
                                )
                                Text(
                                    text = "${strings.floor} ${num(run.floor)} • ${strings.coins} ${num(run.coins)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TowerPalette.TextMuted,
                                )
                            }
                            Text(
                                text = formatter.format(Date(run.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = TowerPalette.StoneLight,
                            )
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TowerPalette.TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TowerPalette.EmberSoft)
    }
}