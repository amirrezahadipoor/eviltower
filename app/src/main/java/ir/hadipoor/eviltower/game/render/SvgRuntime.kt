package ir.hadipoor.eviltower.game.render

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Xml
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser
import ir.hadipoor.eviltower.game.model.EnemyType
import ir.hadipoor.eviltower.game.model.TowerType

internal data class SvgShape(val path: Path, val fill: Color?, val stroke: Color?, val strokeWidth: Float)
internal data class SvgDocument(val shapes: List<SvgShape>)

/** Parses editable SVG assets once; later frames only draw cached vector paths. */
class SvgRuntime(private val context: Context) {
    private val cache = mutableMapOf<String, SvgDocument?>()

    fun prewarm(names: Iterable<String>) { names.forEach { load(it) } }

    @Synchronized
    internal fun load(asset: String): SvgDocument? {
        if (cache.containsKey(asset)) return cache[asset]
        val document = runCatching {
            val parser = Xml.newPullParser()
            context.assets.open("svg/$asset").use { input ->
                parser.setInput(input, "UTF-8")
                val shapes = mutableListOf<SvgShape>()
                var event = parser.eventType
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "path") {
                        parser.getAttributeValue(null, "d")?.let { data ->
                            val path = PathParser.createPathFromPathData(data).asComposePath()
                            shapes += SvgShape(
                                path,
                                parseColor(parser.getAttributeValue(null, "fill")),
                                parseColor(parser.getAttributeValue(null, "stroke")),
                                parser.getAttributeValue(null, "stroke-width")?.toFloatOrNull() ?: 0f,
                            )
                        }
                    } else if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "circle") {
                        val cx = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 50f
                        val cy = parser.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 50f
                        val radius = parser.getAttributeValue(null, "r")?.toFloatOrNull() ?: 1f
                        val path = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius)) }
                        shapes += SvgShape(path, parseColor(parser.getAttributeValue(null, "fill")), parseColor(parser.getAttributeValue(null, "stroke")), parser.getAttributeValue(null, "stroke-width")?.toFloatOrNull() ?: 0f)
                    }
                    event = parser.next()
                }
                SvgDocument(shapes)
            }
        }.getOrNull()
        cache[asset] = document
        return document
    }

    private fun parseColor(value: String?): Color? = value?.takeUnless { it == "none" }?.let { raw -> runCatching { Color(AndroidColor.parseColor(raw)) }.getOrNull() }
}

object SvgAssets {
    fun tower(type: TowerType, level: Int): String = VectorSpriteCatalog.towerAsset(type, level)
    fun enemy(type: EnemyType, variant: Int): String = VectorSpriteCatalog.enemyAsset(type, variant)
}

fun DrawScope.drawSvg(runtime: SvgRuntime, asset: String, center: androidx.compose.ui.geometry.Offset, pixelSize: Float, alpha: Float = 1f, rotation: Float = 0f) {
    val document = runtime.load(asset) ?: return
    val scale = pixelSize / 100f
    withTransform({
        translate(center.x - pixelSize / 2f, center.y - pixelSize / 2f)
        scale(scale, scale)
        rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(50f, 50f))
    }) {
        document.shapes.forEach { shape ->
            shape.fill?.let { drawPath(shape.path, it.copy(alpha = it.alpha * alpha)) }
            shape.stroke?.let { drawPath(shape.path, it.copy(alpha = it.alpha * alpha), style = Stroke(shape.strokeWidth.coerceAtLeast(1f))) }
        }
    }
}
