package ir.hadipoor.eviltower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.hadipoor.eviltower.ui.theme.EvilTowerTheme
import ir.hadipoor.eviltower.ui.theme.TowerPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EvilTowerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TowerPalette.Night),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("برج شیطانی")
                }
            }
        }
    }
}
