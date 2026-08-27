package ir.hadipoor.eviltower.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.ui.theme.Ember
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Panel
import ir.hadipoor.eviltower.ui.theme.PanelLight

@Composable
fun StoneCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) { content() }
}

@Composable
fun EvilButton(text: String, modifier: Modifier = Modifier, color: Color = Ember, enabled: Boolean = true, onClick: () -> Unit) {
    var bounce by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (bounce) .95f else 1f, label = "button-bounce")
    LaunchedEffect(bounce) { if (bounce) { kotlinx.coroutines.delay(110); bounce = false } }
    Button(
        onClick = { bounce = true; onClick() }, enabled = enabled,
        modifier = modifier.height(52.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = PanelLight),
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) }
}

@Composable fun BackButton(onClick: () -> Unit) { EvilButton("بازگشت", Modifier.fillMaxWidth(), PanelLight, onClick = onClick) }

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gold)
        if (subtitle != null) { Spacer(Modifier.height(5.dp)); Text(subtitle, color = Color(0xFFCFC3D9), fontSize = 13.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color = Gold, modifier: Modifier = Modifier) {
    Box(modifier.background(PanelLight, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = Color(0xFFBDB1C8))
            Text(value, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
