package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.hadipoor.eviltower.ui.theme.Gold
import ir.hadipoor.eviltower.ui.theme.Ember

private fun DrawScope.line(start: Offset, end: Offset, color: Color, width: Float) = drawLine(color, start, end, width)

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(.8f, 1.12f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "pulse")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.align(Alignment.CenterHorizontally).fillMaxSize(.35f)) {
            val c = center; drawCircle(Ember.copy(alpha = .12f), 100f * pulse, c); drawCircle(Color(0xFF512B5D), 72f * pulse, c, style = Stroke(5f)); drawCircle(Gold, 25f, c)
            line(c.copy(y = c.y - 80), c.copy(y = c.y + 80), Ember, 6f)
            line(c.copy(x = c.x - 80), c.copy(x = c.x + 80), Ember, 6f)
        }
        Text("برج شیطانی", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("دفاع تا آخرین نفس", color = Color(0xFFBFB1CC), fontSize = 15.sp)
    }
}
