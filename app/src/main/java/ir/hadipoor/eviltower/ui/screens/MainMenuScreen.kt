package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import ir.hadipoor.eviltower.ui.components.StoneCard
import ir.hadipoor.eviltower.ui.theme.Ember
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Night
import ir.hadipoor.eviltower.ui.theme.Panel

@Composable
fun MainMenuScreen(profile: ProfileData, vm: GameViewModel, onExit: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "menu-tower")
    val pulse by transition.animateFloat(.92f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "menu-pulse")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF241534), Night)))) {
        Canvas(Modifier.fillMaxSize()) { repeat(18) { i -> drawCircle(Color(0x225C4779), 2f + i % 3, androidx.compose.ui.geometry.Offset(size.width * ((i * 47 % 100) / 100f), size.height * ((i * 71 % 100) / 100f))) } }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Text("برج شیطانی", color = Gold, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("دفاع از هسته در برابر موج‌های بی‌پایان", color = Color(0xFFD0C2D8), fontSize = 14.sp, textAlign = TextAlign.Center)
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * .62f)
                drawCircle(Color(0x335CA58A), 82f * pulse, c); drawCircle(Color(0x445CA58A), 52f * pulse, c)
                val tower = androidx.compose.ui.graphics.Path().apply { moveTo(c.x - 56, c.y + 65); lineTo(c.x - 40, c.y - 48); lineTo(c.x, c.y - 82); lineTo(c.x + 40, c.y - 48); lineTo(c.x + 56, c.y + 65); close() }
                drawPath(tower, Color(0xFF442544)); drawPath(tower, Ember, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                drawCircle(Ember, 22f * pulse, c.copy(y = c.y - 6)); drawCircle(Color(0xFFFFEBD6), 5f, c.copy(y = c.y - 6))
            }
            Spacer(Modifier.height(4.dp))
            StoneCard(Modifier.fillMaxWidth()) {
                Text("آخرین هسته‌ی آزاد هنوز می‌تپد. برج شیطانی هرگز از فرستادن سپاه دست نمی‌کشد؛ هر موج را نگه دار، طلا جمع کن و برای سقوط اجتناب‌ناپذیر آماده شو.", color = Color(0xFFCFC3D9), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
            }
            Spacer(Modifier.height(12.dp))
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
