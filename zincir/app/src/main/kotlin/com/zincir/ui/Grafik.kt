package com.zincir.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zincir.mantik.Bicim
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Grafikteki bir değer: [gun] yatay eksende tarih, [deger] dikey eksende. */
data class Nokta(val gun: Long, val deger: Double)

private val SOL = 42.dp   // dikey eksen yazıları için boşluk
private val SAG = 10.dp
private val UST = 10.dp
private val ALT = 24.dp   // tarih yazıları için boşluk

/** Dikey eksende en çok 5 aralık olacak şekilde yuvarlak bir adım seçer. */
private fun eksenAdimi(aralik: Double): Double =
    listOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0).firstOrNull { aralik / it <= 5 } ?: 200.0

/**
 * Tek eksenli, tek mürekkepli çizgi grafik.
 *  - [noktalar]: tek tek ölçümler (gri noktalar)
 *  - [cizgi]: işaretli kalın çizgi (haftalık ortalama ya da seans değerleri)
 *  - [bant]: hedef aralığı (açık gri şerit)
 * Grafiğe dokununca ya da parmak kaydırılınca o günün değerleri üstte yazılır.
 */
@Composable
fun CizgiGrafik(
    noktalar: List<Nokta>,
    cizgi: List<Nokta>,
    birim: String,
    modifier: Modifier = Modifier,
    noktaAdi: String? = null,
    cizgiAdi: String? = null,
    bant: ClosedFloatingPointRange<Double>? = null,
    bantAdi: String? = null,
) {
    val hepsi = noktalar + cizgi
    if (hepsi.isEmpty()) return

    val murekkep = MaterialTheme.colorScheme.onBackground
    val gri = MaterialTheme.colorScheme.onSurfaceVariant
    val izgara = MaterialTheme.colorScheme.outlineVariant
    val zemin = MaterialTheme.colorScheme.background
    val bantRengi = MaterialTheme.colorScheme.surfaceVariant
    val olcer = rememberTextMeasurer()
    val yaziStili = TextStyle(fontSize = 12.sp, color = gri)

    // Eksen sınırları.
    val xMin = hepsi.minOf { it.gun }
    val xMax = hepsi.maxOf { it.gun }
    val degerler = hepsi.map { it.deger } + listOfNotNull(bant?.start, bant?.endInclusive)
    val adim = eksenAdimi(degerler.max() - degerler.min())
    var yMin = floor(degerler.min() / adim) * adim
    var yMax = ceil(degerler.max() / adim) * adim
    if (yMax - yMin < adim) {
        yMin -= adim
        yMax += adim
    }
    val gunler = hepsi.map { it.gun }.distinct().sorted()
    var secili by remember(gunler) { mutableStateOf<Long?>(null) }

    Column(modifier.fillMaxWidth()) {
        // Açıklama satırı: hangi işaret ne anlama geliyor.
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (noktaAdi != null && noktalar.isNotEmpty()) Aciklama(noktaAdi) { Box(Modifier.size(8.dp).background(gri, CircleShape)) }
            if (cizgiAdi != null && cizgi.isNotEmpty()) Aciklama(cizgiAdi) { Box(Modifier.width(18.dp).height(3.dp).background(murekkep)) }
            if (bantAdi != null && bant != null) Aciklama(bantAdi) { Box(Modifier.width(18.dp).height(10.dp).background(bantRengi)) }
        }
        // Seçili günün değerleri (ya da ipucu).
        val s = secili
        val okuma = if (s == null) "Değeri görmek için grafiğe dokun." else buildString {
            append(Bicim.kisaTarih(s))
            noktalar.firstOrNull { it.gun == s }?.let { append(" · ${noktaAdi ?: "değer"} ${Bicim.ondalik(it.deger)} $birim") }
            cizgi.minByOrNull { abs(it.gun - s) }?.takeIf { abs(it.gun - s) <= 3 }
                ?.let { append(" · ${cizgiAdi ?: "değer"} ${Bicim.ondalik(it.deger)} $birim") }
        }
        Text(
            okuma, style = MaterialTheme.typography.bodySmall,
            color = if (s == null) gri else murekkep, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )

        Canvas(
            Modifier.fillMaxWidth().height(220.dp)
                .pointerInput(gunler) {
                    // Dokunulan yere en yakın günü seç.
                    fun sec(x: Float) {
                        val sol = SOL.toPx()
                        val genislik = size.width - sol - SAG.toPx()
                        val oran = ((x - sol) / genislik).coerceIn(0f, 1f)
                        val hedef = xMin + oran * (xMax - xMin)
                        secili = gunler.minByOrNull { abs(it - hedef) }
                    }
                    detectTapGestures { sec(it.x) }
                }
                .pointerInput(gunler) {
                    fun sec(x: Float) {
                        val sol = SOL.toPx()
                        val genislik = size.width - sol - SAG.toPx()
                        val oran = ((x - sol) / genislik).coerceIn(0f, 1f)
                        val hedef = xMin + oran * (xMax - xMin)
                        secili = gunler.minByOrNull { abs(it - hedef) }
                    }
                    detectHorizontalDragGestures { degisim, _ -> sec(degisim.position.x) }
                }
        ) {
            val sol = SOL.toPx()
            val ust = UST.toPx()
            val w = size.width - sol - SAG.toPx()
            val h = size.height - ust - ALT.toPx()
            fun px(gun: Long): Float = if (xMax == xMin) sol + w / 2 else sol + (gun - xMin).toFloat() / (xMax - xMin) * w
            fun py(deger: Double): Float = ust + ((yMax - deger) / (yMax - yMin)).toFloat() * h

            // Hedef bandı (en arkada).
            if (bant != null) {
                drawRect(bantRengi, topLeft = Offset(sol, py(bant.endInclusive)), size = Size(w, py(bant.start) - py(bant.endInclusive)))
            }
            // Yatay ızgara çizgileri ve dikey eksen yazıları.
            var t = yMin
            while (t <= yMax + 1e-9) {
                val y = py(t)
                drawLine(izgara, Offset(sol, y), Offset(sol + w, y), strokeWidth = 1.dp.toPx())
                val yazi = olcer.measure(Bicim.ondalik(t), yaziStili)
                drawText(yazi, topLeft = Offset(sol - yazi.size.width - 6.dp.toPx(), y - yazi.size.height / 2))
                t += adim
            }
            // Tarih yazıları: ilk ve son gün.
            val ilk = olcer.measure(Bicim.kisaTarih(xMin), yaziStili)
            drawText(ilk, topLeft = Offset(sol, ust + h + 6.dp.toPx()))
            if (xMax != xMin) {
                val son = olcer.measure(Bicim.kisaTarih(xMax), yaziStili)
                drawText(son, topLeft = Offset(sol + w - son.size.width, ust + h + 6.dp.toPx()))
            }
            // Seçili gün çizgisi.
            secili?.let { g -> drawLine(gri, Offset(px(g), ust), Offset(px(g), ust + h), strokeWidth = 1.dp.toPx()) }

            // Tek tek ölçümler: gri nokta, çevresinde zemin renginde ince halka (üst üste binince ayrışsın).
            noktalar.forEach { n ->
                val merkez = Offset(px(n.gun), py(n.deger))
                drawCircle(zemin, radius = 5.dp.toPx(), center = merkez)
                drawCircle(gri, radius = 3.5.dp.toPx(), center = merkez)
            }
            // Kalın çizgi ve işaretleri.
            if (cizgi.size >= 2) {
                val yol = Path()
                cizgi.forEachIndexed { i, n -> if (i == 0) yol.moveTo(px(n.gun), py(n.deger)) else yol.lineTo(px(n.gun), py(n.deger)) }
                drawPath(yol, murekkep, style = Stroke(width = 2.5.dp.toPx()))
            }
            cizgi.forEach { n ->
                val merkez = Offset(px(n.gun), py(n.deger))
                drawCircle(zemin, radius = 6.5.dp.toPx(), center = merkez)
                drawCircle(murekkep, radius = 4.5.dp.toPx(), center = merkez)
            }
        }
    }
}

@Composable
private fun Aciklama(ad: String, isaret: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        isaret()
        Text(ad, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
