package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.data.ProfileData
import ir.hadipoor.eviltower.ui.AppScreen
import ir.hadipoor.eviltower.ui.GameViewModel
import ir.hadipoor.eviltower.ui.fa
import ir.hadipoor.eviltower.ui.components.EvilButton
import ir.hadipoor.eviltower.ui.components.ScreenTitle
import ir.hadipoor.eviltower.ui.components.StatPill
import ir.hadipoor.eviltower.ui.theme.Ember
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Night
import ir.hadipoor.eviltower.ui.theme.Panel

@Composable
fun MainMenuScreen(profile: ProfileData, vm: GameViewModel, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF241534), Night)))) {
        Canvas(Modifier.fillMaxSize()) { repeat(18) { i -> drawCircle(Color(0x225C4779), 2f + i % 3, androidx.compose.ui.geometry.Offset(size.width * ((i * 47 % 100) / 100f), size.height * ((i * 71 % 100) / 100f))) } }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Text("برج شیطانی", color = Gold, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("دفاع از هسته در برابر موج‌های بی‌پایان", color = Color(0xFFD0C2D8), fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                StatPill("رکورد", fa(profile.bestWave), Gold)
                StatPill("سکه", fa(profile.metaCoins), Gold)
                StatPill("جواهر", fa(profile.gems), Color(0xFF70D6FF))
            }
            Spacer(Modifier.height(28.dp))
            EvilButton("شروع دفاع", Modifier.fillMaxWidth(), Ember) { vm.startRun() }
            Spacer(Modifier.height(10.dp))
            EvilButton("فروشگاه", Modifier.fillMaxWidth(), Panel) { vm.open(AppScreen.SHOP) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EvilButton("دستاوردها", Modifier.weight(1f), Panel) { vm.open(AppScreen.ACHIEVEMENTS) }
                EvilButton("رکوردها", Modifier.weight(1f), Panel) { vm.open(AppScreen.RECORDS) }
            }
            Spacer(Modifier.height(10.dp))
            EvilButton("تنظیمات", Modifier.fillMaxWidth(), Panel) { vm.open(AppScreen.SETTINGS) }
            Spacer(Modifier.height(10.dp))
            EvilButton("خروج", Modifier.fillMaxWidth(), Color(0xFF4A2636), onClick = onExit)
            Spacer(Modifier.height(25.dp))
            Text("یک جاده، یک هسته، و دشمنانی که هرگز تمام نمی‌شوند.", color = Color(0xFF8D7B9B), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
