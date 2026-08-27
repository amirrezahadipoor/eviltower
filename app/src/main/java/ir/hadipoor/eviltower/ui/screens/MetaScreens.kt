package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.data.ProfileData
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.ui.AppScreen
import ir.hadipoor.eviltower.ui.GameViewModel
import ir.hadipoor.eviltower.ui.fa
import ir.hadipoor.eviltower.ui.components.BackButton
import ir.hadipoor.eviltower.ui.components.EvilButton
import ir.hadipoor.eviltower.ui.components.ScreenTitle
import ir.hadipoor.eviltower.ui.components.StatPill
import ir.hadipoor.eviltower.ui.components.StoneCard
import ir.hadipoor.eviltower.ui.theme.Ember
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Panel
import ir.hadipoor.eviltower.ui.theme.PanelLight

@Composable
private fun MetaFrame(title: String, vm: GameViewModel, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0812)).padding(horizontal = 18.dp)) {
        ScreenTitle(title)
        content()
        Spacer(Modifier.height(12.dp))
        BackButton { vm.goMenu() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ShopScreen(profile: ProfileData, vm: GameViewModel) = MetaFrame("فروشگاه", vm) {
    Text("سکه‌ی دائمی: ${fa(profile.metaCoins)}     جواهر: ${fa(profile.gems)}", color = Gold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    StoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("برج جادوی اهریمنی", color = Color(0xFFE68CFF), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("نفوذ به زره و آسیب بسیار زیاد برای فاز بی‌پایان.", color = Color(0xFFD0C2D8))
            EvilButton(if (profile.arcaneUnlocked) "باز شده" else "باز کردن — ${fa(850)} سکه", Modifier.fillMaxWidth(), Color(0xFF733F88), enabled = !profile.arcaneUnlocked && profile.metaCoins >= 850) { vm.buyArcane() }
        }
    }
    Spacer(Modifier.height(12.dp))
    StoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("کیسه‌ی شروع", color = Gold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("در هر دفاع ۸۰ سکه‌ی اولیه‌ی بیشتر دریافت کن. خریدها دائمی هستند.", color = Color(0xFFD0C2D8))
            EvilButton("خرید ۵۰۰ سکه — سطح فعلی ${fa(profile.startingGoldBonus / 80)}", Modifier.fillMaxWidth(), Color(0xFF805B31), enabled = profile.metaCoins >= 500) { vm.buyGoldBonus() }
        }
    }
}

@Composable
fun AchievementsScreen(profile: ProfileData, vm: GameViewModel) = MetaFrame("دستاوردها", vm) {
    val list = listOf(
        Triple("رسیدن به موج ۵۰", profile.bestWave >= 50, "بهترین موج: ${fa(profile.bestWave)}"),
        Triple("دفاع جاودانه", profile.bestWave >= 300, "ورود به فاز بی‌پایان"),
        Triple("یک برج سطح ۱۰۰", profile.towerLevel100, "در یک دور، یک برج را تا آخر ارتقا بده"),
        Triple("شکارچی باس‌ها", profile.totalBosses >= 100, "شکست ${fa(profile.totalBosses)} باس"),
        Triple("صدای هسته", profile.totalEnemies >= 1000, "شکست ${fa(profile.totalEnemies)} دشمن"),
    )
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(list) { (title, done, detail) ->
            StoneCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "✓" else "◇", color = if (done) Gold else Color(0xFF756680), fontSize = 26.sp, modifier = Modifier.size(38.dp))
                    Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(detail, color = Color(0xFFBCAEC7), fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun RecordsScreen(profile: ProfileData, vm: GameViewModel) = MetaFrame("رکوردهای من", vm) {
    StoneCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("بهترین موج", color = Color(0xFFBCAEC7)); Text(fa(profile.bestWave), color = Gold, fontSize = 42.sp, fontWeight = FontWeight.Black) } }
    Spacer(Modifier.height(12.dp))
    Text("آخرین دفاع‌ها", color = Color.White, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    if (profile.history.isEmpty()) Text("هنوز رکوردی ثبت نشده است.", color = Color(0xFF9B8AA7), modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center)
    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(profile.history) { record -> StoneCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("موج ${fa(record.wave)}", color = Gold, fontWeight = FontWeight.Bold); Text(record.date, color = Color(0xFFBCAEC7)) } } }
    }
}

@Composable
fun SettingsScreen(profile: ProfileData, vm: GameViewModel) = MetaFrame("تنظیمات", vm) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SettingRow("صدای بازی", "افکت‌های تیر، ضربه و ارتقا", profile.soundOn) { vm.setSound(it) }
        SettingRow("موسیقی", "موسیقی زمینه‌ی تاریک", profile.musicOn) { vm.setMusic(it) }
        SettingRow("لرزش", "بازخورد ضربه به هسته", profile.vibrationOn) { vm.setVibration(it) }
        SettingRow("گرافیک سبک", "برای گوشی‌های ضعیف‌تر", profile.lowGraphics) { vm.setLowGraphics(it) }
        VolumeRow("صدای افکت", profile.soundVolume, vm::setSoundVolume)
        VolumeRow("صدای موسیقی", profile.musicVolume, vm::setMusicVolume)
        StoneCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("زبان", color = Color.White, fontWeight = FontWeight.Bold); Text("فارسی (پیش‌فرض) • رابط راست‌به‌چپ", color = Color(0xFFBCAEC7), modifier = Modifier.padding(top = 5.dp)) } }
    }
}

@Composable
private fun VolumeRow(title: String, value: Float, onChange: (Float) -> Unit) {
    StoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${fa((value * 100).toInt())}%", color = Gold, fontSize = 12.sp)
            }
            Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    StoneCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFFBCAEC7), fontSize = 12.sp) }; Switch(checked, onCheckedChange = onChange) } }
}

@Composable
fun ResultScreen(snapshot: GameSnapshot, profile: ProfileData, vm: GameViewModel) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0812)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp)); Text("نتیجه‌ی دفاع", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("هسته نابود شد", color = Color(0xFFFF6B6B), fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        StoneCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ResultLine("موج طی‌شده", fa(snapshot.bestWave), Gold)
            ResultLine("بهترین رکورد", fa(profile.bestWave), Ember)
            ResultLine("دشمنان شکست‌خورده", fa(snapshot.enemiesDefeated), Color.White)
            ResultLine("سکه‌ی به‌دست‌آمده", fa(snapshot.goldEarned), Gold)
            ResultLine("مدت دفاع", "${fa(snapshot.runSeconds)} ثانیه", Color.White)
        } }
        Spacer(Modifier.height(22.dp))
        if (snapshot.bestWave >= profile.bestWave && snapshot.bestWave > 0) Text("رکورد جدید!", color = Gold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        EvilButton("دوباره تلاش کن", Modifier.fillMaxWidth(), Ember) { vm.startRun() }
        Spacer(Modifier.height(10.dp)); EvilButton("منوی اصلی", Modifier.fillMaxWidth(), PanelLight) { vm.goMenu() }
    }
}

@Composable
private fun ResultLine(label: String, value: String, color: Color) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color(0xFFBCAEC7)); Text(value, color = color, fontWeight = FontWeight.Bold) } }
