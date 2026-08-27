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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.Achievements
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.game.render.SvgPaths
import ir.hadipoor.eviltower.game.render.drawSvg
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.GemIcon
import ir.hadipoor.eviltower.ui.components.ScreenScaffold
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers

/** دستاوردها */
@Composable
fun AchievementsScreen(
    profile: PlayerProfile,
    persianDigits: Boolean,
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    ScreenScaffold(title = strings.achievements, onBack = onBack) { modifier ->
        LazyColumn(
            modifier = modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Achievements.all) { achievement ->
                val progress = achievement.progressOf(profile).coerceAtMost(achievement.goal)
                val done = progress >= achievement.goal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TowerPalette.DeepPurple.copy(alpha = 0.9f))
                        .border(
                            1.dp,
                            if (done) TowerPalette.Ember.copy(alpha = 0.8f) else TowerPalette.PurpleLight.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TowerPalette.Shadow.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.size(30.dp)) {
                            drawSvg(
                                SvgPaths.SKULL_MARK, 0f, 0f, size.width, size.height,
                                if (done) TowerPalette.Ember else TowerPalette.StoneDark,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            achievement.persianTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (done) TowerPalette.TextPrimary else TowerPalette.TextMuted,
                        )
                        Text(
                            achievement.persianDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = TowerPalette.TextMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress.toFloat() / achievement.goal.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp),
                            color = if (done) TowerPalette.Ember else TowerPalette.PurpleLight,
                            trackColor = TowerPalette.Purple,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${strings.progress}: ${PersianNumbers.format(progress, persianDigits)} / " +
                                PersianNumbers.format(achievement.goal, persianDigits),
                            style = MaterialTheme.typography.labelSmall,
                            color = TowerPalette.TextMuted,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GemIcon(Modifier.size(18.dp))
                        Text(
                            text = PersianNumbers.format(achievement.rewardGems, persianDigits),
                            style = MaterialTheme.typography.labelMedium,
                            color = TowerPalette.Gem,
                        )
                        Text(
                            text = if (done) strings.unlocked else strings.locked,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) TowerPalette.Ember else TowerPalette.StoneLight,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
