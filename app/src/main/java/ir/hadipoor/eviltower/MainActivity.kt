package ir.hadipoor.eviltower

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import ir.hadipoor.eviltower.monetization.BazaarBillingProvider
import ir.hadipoor.eviltower.ui.EvilTowerApp
import ir.hadipoor.eviltower.ui.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private var bazaarBilling: BazaarBillingProvider? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        bazaarBilling = BazaarBillingProvider(this)
        runCatching { bazaarBilling?.connect() }
        setContent { EvilTowerApp(viewModel) }
    }
    override fun onPause() {
        viewModel.audio.pauseMusic()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.profile.value.musicOn) viewModel.audio.startMusic()
    }

    override fun onDestroy() {
        runCatching { bazaarBilling?.disconnect() }
        bazaarBilling = null
        viewModel.release()
        super.onDestroy()
    }
}
