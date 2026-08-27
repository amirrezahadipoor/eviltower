package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.AnimatedTowerLogo
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import kotlinx.coroutines.delay

/** اسپلش/بارگذاری — animated tower logo while the save file is read. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val strings = LocalStrings.current
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(progress, tween(400), label = "loading")

    LaunchedEffect(Unit) {
        repeat(10) {
            delay(90)
            progress = (it + 1) / 10f
        }
        delay(250)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TowerPalette.DeepPurple, TowerPalette.Night, TowerPalette.Shadow)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            AnimatedTowerLogo(Modifier.size(180.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayMedium,
                color = TowerPalette.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = TowerPalette.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = TowerPalette.Ember,
                trackColor = TowerPalette.Purple,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.loading,
                style = MaterialTheme.typography.bodySmall,
                color = TowerPalette.TextMuted,
            )
        }
    }
}
