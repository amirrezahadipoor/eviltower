package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.game.engine.Balance
import ir.hadipoor.eviltower.game.model.EnginePhase
import ir.hadipoor.eviltower.game.model.GameSnapshot
import ir.hadipoor.eviltower.game.model.Tower
import ir.hadipoor.eviltower.game.model.TowerType
import ir.hadipoor.eviltower.ui.GameViewModel
import ir.hadipoor.eviltower.ui.fa
import ir.hadipoor.eviltower.ui.components.EvilButton
import ir.hadipoor.eviltower.ui.components.StatPill
import ir.hadipoor.eviltower.ui.theme.Danger
import ir.hadipoor.eviltower.ui.theme.Ember
import ir.hadipoor.eviltower.ui.theme.Frost
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Night
import ir.hadipoor.eviltower.ui.theme.Panel
import ir.hadipoor.eviltower.ui.theme.PanelLight
import kotlin.math.sin

@Composable
fun GameScreen(vm: GameViewModel) {
    val snapshot = vm.snapshot.value
    var mapScale by remember { mutableFloatStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    val mapTransform = rememberTransformableState { zoom, pan, _ ->
        mapScale = (mapScale * zoom).coerceIn(1f, 2.4f)
        mapOffset += pan
    }
    Box(Modifier.fillMaxSize().background(Night)) {
        Column(Modifier.fillMaxSize().graphicsLayer { translationX = sin(snapshot.worldTime * 40f) * snapshot.screenShake * 32f }) {
            GameTopBar(snapshot, vm)
            if (snapshot.bossName != null) BossBar(snapshot)
            GameCanvas(snapshot, onPlotTap = { vm.selectPlot(it) }, modifier = Modifier.weight(1f).fillMaxWidth().transformable(mapTransform).graphicsLayer {
                scaleX = mapScale; scaleY = mapScale; translationX = mapOffset.x; translationY = mapOffset.y
            })
            if (snapshot.message != null) {
                Box(Modifier.fillMaxWidth().background(Color(0xDD6D294F)).padding(7.dp), contentAlignment = Alignment.Center) {
                    Text(snapshot.message, color = Gold, fontWeight = FontWeight.Bold)
                }
            }
            BuildPanel(snapshot, vm)
        }
        if (snapshot.phase == EnginePhase.PAUSED) PauseOverlay(vm)
        if (snapshot.phase == EnginePhase.PREP && snapshot.prepRemaining > 0f) {
            Text("موج بعدی در ${fa(snapshot.prepRemaining.toInt() + 1)}", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).background(Color(0xCC171020), RoundedCornerShape(15.dp)).padding(horizontal = 18.dp, vertical = 10.dp))
        }
        if (snapshot.message?.startsWith("هشدار باس") == true) BossIntro(snapshot.message)
    }
}

@Composable
private fun BossIntro(message: String) {
    val transition = rememberInfiniteTransition(label = "boss-intro")
    val pulse by transition.animateFloat(.92f, 1.08f, infiniteRepeatable(tween(460), RepeatMode.Reverse), label = "boss-pulse")
    Column(Modifier.align(Alignment.Center).graphicsLayer { scaleX = pulse; scaleY = pulse }.background(Color(0xEE35152F), RoundedCornerShape(22.dp)).padding(horizontal = 28.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚔", color = Danger, fontSize = 38.sp)
        Text(message, color = Gold, fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text("حمله‌ی سنگین را بخوان و دفاع را بچین", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun GameTopBar(snapshot: GameSnapshot, vm: GameViewModel) {
    Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StatPill("سکه", fa(snapshot.gold), Gold, Modifier.weight(1f))
        StatPill("هسته", "${fa(snapshot.coreHp)} / ${fa(snapshot.coreMaxHp)}", if (snapshot.coreHp <= 5) Danger else Frost, Modifier.weight(1.15f))
        StatPill("موج", "${fa(snapshot.wave)} / ${fa(snapshot.bestWave)}", Ember, Modifier.weight(1.1f))
        EvilButton(if (snapshot.phase == EnginePhase.PAUSED) "ادامه" else "توقف", Modifier.width(70.dp), PanelLight) { vm.togglePause() }
    }
}

@Composable
private fun BossBar(snapshot: GameSnapshot) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF291526)).padding(horizontal = 16.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚔ ${snapshot.bossName}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFF4B1F36), RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth((snapshot.bossHp / snapshot.bossMaxHp).coerceIn(0f, 1f)).height(9.dp).background(Danger, RoundedCornerShape(5.dp)))
        }
    }
}

@Composable
private fun BuildPanel(snapshot: GameSnapshot, vm: GameViewModel) {
    val selected = snapshot.selectedPlot?.let { plot -> snapshot.towers.firstOrNull { it.plot == plot } }
    Column(Modifier.fillMaxWidth().background(Panel).padding(top = 7.dp, bottom = 10.dp)) {
        if (selected == null) {
            Text(if (snapshot.selectedPlot == null) "یک سکوی خالی را انتخاب کن" else "ساخت برج", color = Color(0xFFEADCF2), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TowerType.entries.forEach { type ->
                    val locked = type.unlockable && !vm.profile.value.arcaneUnlocked
                    EvilButton("${type.shortTitle}\n${fa(Balance.towerCost(type, 1))}", Modifier.width(91.dp).alpha(if (locked) .4f else 1f), type.color, enabled = !locked && snapshot.gold >= Balance.towerCost(type, 1)) { vm.build(type) }
                }
            }
        } else {
            TowerUpgradePanel(selected, snapshot, vm)
        }
        Text("پیشرفت موج: ${fa(snapshot.spawned)} / ${fa(snapshot.totalToSpawn)}", color = Color(0xFFBCAEC7), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            EvilButton("☄ توان آتش ${if (snapshot.abilityRemaining > 0) fa(snapshot.abilityRemaining.toInt()) else "آماده"}", Modifier.weight(1f), Color(0xFF9E3C3A), enabled = snapshot.abilityRemaining <= 0f) { vm.inferno() }
            Text("کشتار: ${fa(snapshot.enemiesDefeated)}  •  زنجیره: ${fa(snapshot.combo)}", color = Color(0xFFBCAEC7), fontSize = 12.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TowerUpgradePanel(tower: Tower, snapshot: GameSnapshot, vm: GameViewModel) {
    val maxed = tower.level >= Balance.MAX_TOWER_LEVEL
    val cost = if (maxed) 0 else Balance.upgradeCost(tower)
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(tower.type.title, color = tower.type.color, fontWeight = FontWeight.Bold)
            Text("سطح ${fa(tower.level)} / ${fa(100)}  •  آسیب ${fa(Balance.towerDamage(tower).toInt())}", color = Color(0xFFE8DDF0), fontSize = 12.sp)
            Text("برد ${fa((Balance.towerRange(tower) * 100).toInt())}%  •  سرعت ${fa(Balance.towerInterval(tower).format1())}", color = Color(0xFFBCAEC7), fontSize = 11.sp)
            if (tower.webbed > 0f) Text("در تار عنکبوت گیر افتاده: ${fa(tower.webbed.toInt())}", color = Color(0xFFFF9ACB), fontSize = 11.sp)
        }
        EvilButton(if (maxed) "کامل" else "ارتقا\n${fa(cost)}", Modifier.width(92.dp), tower.type.color, enabled = !maxed && snapshot.gold >= cost) { vm.upgrade() }
        EvilButton("فروش", Modifier.width(65.dp), Color(0xFF573044)) { vm.sell() }
    }
}

@Composable
private fun PauseOverlay(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Color(0xDD0B0812)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("بازی متوقف است", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            EvilButton("ادامه", Modifier.fillMaxWidth(), Ember) { vm.togglePause() }
            EvilButton("شروع دوباره", Modifier.fillMaxWidth(), PanelLight) { vm.restartRun() }
            EvilButton("تنظیمات", Modifier.fillMaxWidth(), PanelLight) { vm.open(ir.hadipoor.eviltower.ui.AppScreen.SETTINGS) }
            EvilButton("خروج به منو", Modifier.fillMaxWidth(), PanelLight) { vm.goMenu() }
        }
    }
}

private fun Float.format1(): String = "%.1f".format(this)
