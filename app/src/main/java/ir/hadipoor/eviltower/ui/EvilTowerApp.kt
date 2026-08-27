package ir.hadipoor.eviltower.ui

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

val LocalPersian = staticCompositionLocalOf { true }

@Composable
fun EvilTowerApp(viewModel: GameViewModel) {
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current
    EvilTowerTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalPersian provides true) {
            Surface(modifier = Modifier.fillMaxSize(), color = Night) {
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
            }
        }
    }
}
