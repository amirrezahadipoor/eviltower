package ir.hadipoor.eviltower.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ir.hadipoor.eviltower.data.RunResult
import ir.hadipoor.eviltower.monetization.AdManager
import ir.hadipoor.eviltower.monetization.AdPlacement
import ir.hadipoor.eviltower.monetization.BillingCatalog
import ir.hadipoor.eviltower.monetization.BillingManager
import ir.hadipoor.eviltower.monetization.PurchaseResult
import ir.hadipoor.eviltower.ui.screens.AchievementsScreen
import ir.hadipoor.eviltower.ui.screens.GameOverOverlay
import ir.hadipoor.eviltower.ui.screens.GameScreen
import ir.hadipoor.eviltower.ui.screens.LeaderboardScreen
import ir.hadipoor.eviltower.ui.screens.MainMenuScreen
import ir.hadipoor.eviltower.ui.screens.SettingsScreen
import ir.hadipoor.eviltower.ui.screens.ShopScreen
import ir.hadipoor.eviltower.ui.screens.SplashScreen
import ir.hadipoor.eviltower.ui.theme.EvilTowerTheme

enum class Screen { SPLASH, MENU, GAME, SHOP, SETTINGS, ACHIEVEMENTS, LEADERBOARD }

/**
 * Root composable: owns navigation between the screens and provides the Persian/English
 * string table plus the RTL layout direction to the whole tree.
 */
@Composable
fun EvilTowerApp(viewModel: GameViewModel, tiltX: State<Float>) {
    val settings by viewModel.settings.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val strings = remember(settings.language) { GameStrings.of(settings.language) }
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.SPLASH) }

    EvilTowerTheme(rtl = strings.isRtl) {
        CompositionLocalProvider(LocalStrings provides strings) {
            when (screen) {
                Screen.SPLASH -> SplashScreen { screen = Screen.MENU }

                Screen.MENU -> {
                    MainMenuScreen(
                        profile = profile,
                        persianDigits = strings.isRtl,
                        onPlay = {
                            viewModel.startRun()
                            screen = Screen.GAME
                        },
                        onShop = { screen = Screen.SHOP },
                        onAchievements = { screen = Screen.ACHIEVEMENTS },
                        onSettings = { screen = Screen.SETTINGS },
                        onLeaderboard = { screen = Screen.LEADERBOARD },
                        onExit = { (context as? Activity)?.finish() },
                    )
                }

                Screen.GAME -> {
                    val engine = viewModel.engine
                    if (engine == null) {
                        androidx.compose.runtime.LaunchedEffect(Unit) { screen = Screen.MENU }
                    } else {
                        val result: RunResult? = viewModel.lastResult
                        val finished = viewModel.runFinished
                        GameScreen(
                            engine = engine,
                            profile = profile,
                            settings = settings,
                            audio = viewModel.audio,
                            tiltX = tiltX,
                            onExitToMenu = {
                                viewModel.quitRun()
                                screen = Screen.MENU
                            },
                            onRestart = {
                                viewModel.startRun()
                            },
                            onFinished = { victory -> viewModel.finishRun(victory) },
                            overlay = if (finished) {
                                {
                                    GameOverOverlay(
                                        result = result,
                                        skinId = profile.selectedSkin,
                                        persianDigits = strings.isRtl,
                                        adsAvailable = viewModel.adsAvailable,
                                        canContinueWithAd = !viewModel.adRewardUsedThisRun,
                                        freshAchievements = viewModel.freshAchievements,
                                        onTryAgain = {
                                            viewModel.clearFreshAchievements()
                                            viewModel.startRun()
                                        },
                                        onMainMenu = {
                                            viewModel.clearFreshAchievements()
                                            viewModel.quitRun()
                                            screen = Screen.MENU
                                        },
                                        onWatchAdContinue = {
                                            val activity = context as? Activity ?: return@GameOverOverlay
                                            AdManager.provider.showRewarded(activity, AdPlacement.CONTINUE_RUN) { ok ->
                                                if (ok) viewModel.continueAfterAd()
                                            }
                                        },
                                        onWatchAdDoubleCoins = {
                                            val activity = context as? Activity ?: return@GameOverOverlay
                                            AdManager.provider.showRewarded(activity, AdPlacement.DOUBLE_COINS) { ok ->
                                                if (ok) viewModel.doubleCoinsAfterAd()
                                            }
                                        },
                                    )
                                }
                            } else null,
                        )
                    }
                }

                Screen.SHOP -> ShopScreen(
                    profile = profile,
                    persianDigits = strings.isRtl,
                    onBack = { screen = Screen.MENU },
                    onBuy = { entry -> viewModel.purchase(entry) },
                    onEquipSkin = viewModel::selectSkin,
                    onEquipTheme = viewModel::selectTheme,
                    onBuyGems = {
                        val activity = context as? ComponentActivity ?: return@ShopScreen
                        BillingManager.provider.purchase(activity, BillingCatalog.GEMS_MEDIUM) { result ->
                            if (result is PurchaseResult.Success) {
                                viewModel.grantPurchasedGems(result.product.gems)
                            }
                        }
                    },
                )

                Screen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onBack = { screen = Screen.MENU },
                    onMusic = viewModel::setMusicVolume,
                    onSfx = viewModel::setSfxVolume,
                    onControls = viewModel::setControls,
                    onVibration = viewModel::setVibration,
                    onLanguage = viewModel::setLanguage,
                    onScreenShake = viewModel::setScreenShake,
                    onReset = viewModel::resetProgress,
                )

                Screen.ACHIEVEMENTS -> AchievementsScreen(
                    profile = profile,
                    persianDigits = strings.isRtl,
                    onBack = { screen = Screen.MENU },
                )

                Screen.LEADERBOARD -> LeaderboardScreen(
                    profile = profile,
                    persianDigits = strings.isRtl,
                    onBack = { screen = Screen.MENU },
                )
            }

            BackHandler(enabled = screen != Screen.MENU && screen != Screen.SPLASH) {
                if (screen == Screen.GAME) viewModel.quitRun()
                screen = Screen.MENU
            }
        }
    }
}
