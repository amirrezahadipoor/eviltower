package ir.hadipoor.eviltower.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.staticCompositionLocalOf
import ir.hadipoor.eviltower.ui.screens.AchievementsScreen
import ir.hadipoor.eviltower.ui.screens.GameScreen
import ir.hadipoor.eviltower.ui.screens.MainMenuScreen
import ir.hadipoor.eviltower.ui.screens.RecordsScreen
import ir.hadipoor.eviltower.ui.screens.ResultScreen
import ir.hadipoor.eviltower.ui.screens.SettingsScreen
import ir.hadipoor.eviltower.ui.screens.ShopScreen
import ir.hadipoor.eviltower.ui.screens.SplashScreen
import ir.hadipoor.eviltower.ui.theme.EvilTowerTheme
import ir.hadipoor.eviltower.ui.theme.Night
import ir.hadipoor.eviltower.ui.theme.Gold

val LocalPersian = staticCompositionLocalOf { true }

@Composable
fun EvilTowerApp(viewModel: GameViewModel) {
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current
    val notice = viewModel.notice.value
    LaunchedEffect(notice) { if (notice != null) { kotlinx.coroutines.delay(2200); viewModel.clearNotice() } }
    EvilTowerTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalPersian provides true) {
            Surface(modifier = Modifier.fillMaxSize(), color = Night) {
                Box(Modifier.fillMaxSize()) {
                    when (viewModel.screen.value) {
                        AppScreen.SPLASH -> SplashScreen()
                        AppScreen.MENU -> MainMenuScreen(profile, viewModel, onExit = { (context as? Activity)?.finish() })
                        AppScreen.GAME -> GameScreen(viewModel)
                        AppScreen.SHOP -> ShopScreen(profile, viewModel)
                        AppScreen.ACHIEVEMENTS -> AchievementsScreen(profile, viewModel)
                        AppScreen.RECORDS -> RecordsScreen(profile, viewModel)
                        AppScreen.SETTINGS -> SettingsScreen(profile, viewModel)
                        AppScreen.RESULT -> ResultScreen(viewModel.snapshot.value, profile, viewModel)
                    }
                    if (notice != null) Text(notice, color = Color(0xFF180D20), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).background(Gold, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 11.dp))
                }
            }
        }
    }
}
