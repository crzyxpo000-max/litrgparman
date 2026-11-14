package com.zincir.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zincir.mantik.Bicim
import kotlin.math.max

/* Ortak arayüz parçaları. Her ekran bunlardan kurulur; böylece görünüm her yerde aynı kalır. */

/** Sayfa kenar boşluğu. */
val Kenar = 20.dp

/** Kaydırılabilir sayfa gövdesi. */
@Composable
fun Sayfa(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Kenar).padding(top = 16.dp, bottom = 40.dp),
        content = content,
    )
}

/** Ekran başlığı: üstte küçük etiket, altında büyük başlık, sağda isteğe bağlı düğme. */
@Composable
fun EkranBasligi(baslik: String, ust: String? = null, sag: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            if (ust != null) Etiket(ust)
            Text(baslik, style = MaterialTheme.typography.headlineLarge)
        }
        if (sag != null) sag()
    }
}

/** BÜYÜK HARFLİ küçük gri etiket. */
@Composable
fun Etiket(metin: String, modifier: Modifier = Modifier, renk: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(metin.uppercase(Bicim.TR), style = MaterialTheme.typography.labelMedium, color = renk, modifier = modifier)
}

/** İnce ayırıcı çizgi. */
@Composable
fun Cizgi(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** Bölüm başı: siyah çizgi ve etiket. */
@Composable
fun Bolum(etiket: String, ust: Dp = 32.dp) {
    Spacer(Modifier.height(ust))
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(10.dp))
    Etiket(etiket, renk = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(10.dp))
}

@Composable
fun Bosluk(yukseklik: Dp = 12.dp) = Spacer(Modifier.height(yukseklik))

/** Ana eylem düğmesi: dolu siyah. */
@Composable
fun DoluDugme(metin: String, modifier: Modifier = Modifier, etkin: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = etkin, shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) { Text(metin, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

/** İkincil düğme: çerçeveli. */
@Composable
fun CizgiDugme(metin: String, modifier: Modifier = Modifier, etkin: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = etkin, shape = RectangleShape,
        border = BorderStroke(1.dp, if (etkin) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) { Text(metin, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

/** Üçüncül eylem: altı çizili metin. Dokunma alanı yine en az 48 dp. */
@Composable
fun MetinDugme(metin: String, modifier: Modifier = Modifier, renk: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Box(
        modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) { Text(metin, style = MaterialTheme.typography.titleSmall, color = renk, textDecoration = TextDecoration.Underline) }
}

/** Uyarı kutusu: uygulamadaki tek renkli öğe. */
@Composable
fun Uyari(baslik: String, metin: String? = null, modifier: Modifier = Modifier) {
    val renk = MaterialTheme.colorScheme.error
    Row(modifier.fillMaxWidth().border(2.dp, renk).padding(16.dp)) {
        Column {
            Text(baslik, style = MaterialTheme.typography.headlineSmall, color = renk)
            if (metin != null) {
                Spacer(Modifier.height(4.dp))
                Text(metin, style = MaterialTheme.typography.bodyMedium, color = renk)
            }
        }
    }
}

/** Sakin bilgi notu: solda kalın çizgi. */
@Composable
fun Not(metin: String, modifier: Modifier = Modifier, baslik: String? = null) {
    Row(modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onBackground))
        Column(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
            if (baslik != null) Text(baslik, style = MaterialTheme.typography.titleSmall)
            Text(metin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** İnce ilerleme çizgisi. [oran] 0–1. */
@Composable
fun IlerlemeCizgisi(oran: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
        Box(Modifier.fillMaxWidth(oran.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.onBackground))
    }
}

/** Günlük hedef satırı: ad, değer / hedef ve ilerleme çizgisi. */
@Composable
fun HedefSatiri(ad: String, deger: String, hedef: String, oran: Float, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(ad, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(deger, style = MaterialTheme.typography.headlineSmall)
            Text(" / $hedef", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        IlerlemeCizgisi(oran)
    }
}

/** 56 dp kare düğme (− ve + için). */
@Composable
fun KareDugme(metin: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(56.dp).border(1.dp, MaterialTheme.colorScheme.onBackground).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(metin, style = MaterialTheme.typography.headlineMedium) }
}

/** Sayı girişi: − [kutu] +. Kutuya elle de yazılabilir; virgül ve nokta kabul edilir. */
@Composable
fun SayiGirisi(
    deger: String, onDegis: (String) -> Unit, etiket: String, modifier: Modifier = Modifier,
    adim: Double = 1.0, ondalik: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        KareDugme("−") { onDegis(Bicim.giris(max(0.0, (Bicim.coz(deger) ?: 0.0) - adim))) }
        OutlinedTextField(
            value = deger,
            onValueChange = { yeni -> onDegis(yeni.filter { it.isDigit() || (ondalik && (it == ',' || it == '.')) }.take(6)) },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            singleLine = true, shape = RectangleShape,
            label = { Text(etiket) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = if (ondalik) KeyboardType.Decimal else KeyboardType.Number),
        )
        KareDugme("+") { onDegis(Bicim.giris((Bicim.coz(deger) ?: 0.0) + adim)) }
    }
}

/** Açma/kapama satırı. */
@Composable
fun AnahtarSatiri(metin: String, acik: Boolean, modifier: Modifier = Modifier, alt: String? = null, etkin: Boolean = true, onDegis: (Boolean) -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = etkin, role = Role.Switch) { onDegis(!acik) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(metin, style = MaterialTheme.typography.titleMedium)
            if (alt != null) Text(alt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = acik, onCheckedChange = null, enabled = etkin,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.onBackground, checkedThumbColor = MaterialTheme.colorScheme.background,
                uncheckedTrackColor = MaterialTheme.colorScheme.background, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/** Yan yana seçenek kutuları; seçili olan dolu siyah. */
@Composable
fun Secim(secenekler: List<String>, secili: String, modifier: Modifier = Modifier, onSec: (String) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        secenekler.forEach { s ->
            val sec = s == secili
            Box(
                Modifier.weight(1f).heightIn(min = 52.dp)
                    .background(if (sec) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(role = Role.RadioButton) { onSec(s) }.padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                    color = if (sec) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/** Tıklanabilir liste satırı: solda başlık ve açıklama, sağda değer. */
@Composable
fun ListeSatiri(baslik: String, modifier: Modifier = Modifier, alt: String? = null, sag: String? = null, onClick: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(baslik, style = MaterialTheme.typography.titleMedium)
                if (alt != null) Text(alt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (sag != null) {
                Text(sag, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp), textAlign = TextAlign.End)
            }
            if (onClick != null) Text("  ›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Cizgi()
    }
}

enum class AlanTuru { METIN, UZUN_METIN, TAM, ONDALIK, SECIM }

/** [FormDialog] içindeki tek giriş alanı. */
data class Alan(val etiket: String, val deger: String, val tur: AlanTuru = AlanTuru.METIN, val secenekler: List<String> = emptyList())

/**
 * Genel düzenleme penceresi. Alanların değerleri metin olarak [onKaydet]'e verilir.
 * [dogrula] bir hata metni dönerse pencere kapanmaz ve hata gösterilir.
 */
@Composable
fun FormDialog(
    baslik: String, alanlar: List<Alan>, onKapat: () -> Unit,
    aciklama: String? = null, onSil: (() -> Unit)? = null, kaydetMetni: String = "Kaydet",
    dogrula: (List<String>) -> String? = { null },
    onKaydet: (List<String>) -> Unit,
) {
    val degerler = remember { mutableStateListOf<String>().apply { addAll(alanlar.map { it.deger }) } }
    var hata by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onKapat, shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
        title = { Text(baslik, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (aciklama != null) Text(aciklama, style = MaterialTheme.typography.bodyMedium)
                alanlar.forEachIndexed { i, alan ->
                    if (alan.tur == AlanTuru.SECIM) {
                        Column {
                            Etiket(alan.etiket)
                            Spacer(Modifier.height(6.dp))
                            Secim(alan.secenekler, degerler[i]) { degerler[i] = it }
                        }
                    } else {
                        OutlinedTextField(
                            value = degerler[i], onValueChange = { degerler[i] = it; hata = null },
                            label = { Text(alan.etiket) }, shape = RectangleShape, modifier = Modifier.fillMaxWidth(),
                            singleLine = alan.tur != AlanTuru.UZUN_METIN,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                keyboardType = when (alan.tur) {
                                    AlanTuru.TAM -> KeyboardType.Number
                                    AlanTuru.ONDALIK -> KeyboardType.Decimal
                                    else -> KeyboardType.Text
                                },
                            ),
                        )
                    }
                }
                hata?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = dogrula(degerler.toList())
                if (h != null) hata = h else { onKaydet(degerler.toList()); onKapat() }
            }) { Text(kaydetMetni, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            Row {
                if (onSil != null) {
                    TextButton(onClick = { onSil(); onKapat() }) {
                        Text("Sil", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onKapat) { Text("Vazgeç", style = MaterialTheme.typography.labelLarge) }
            }
        },
    )
}

/** Evet/hayır onayı. */
@Composable
fun OnayDialog(baslik: String, metin: String, onayMetni: String, onKapat: () -> Unit, onOnay: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKapat, shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
        title = { Text(baslik, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(metin, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = { onOnay(); onKapat() }) { Text(onayMetni, style = MaterialTheme.typography.labelLarge) } },
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** Bu parça ekrandayken telefon ekranı kapanmaz. */
@Composable
fun EkraniAcikTut() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Uzun bir listeden tek seçim yapılan pencere. */
@Composable
fun ListeDialog(baslik: String, secenekler: List<String>, onKapat: () -> Unit, onSec: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onKapat, shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
        title = { Text(baslik, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                secenekler.forEach { s ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onSec(s); onKapat() }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Text(s, style = MaterialTheme.typography.titleMedium) }
                    Cizgi()
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** Köşeli onay kutusu satırı (kontrol listeleri için). */
@Composable
fun OnaySatiri(metin: String, isaretli: Boolean, modifier: Modifier = Modifier, onDegis: (Boolean) -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Checkbox) { onDegis(!isaretli) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp)
                .background(if (isaretli) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                .border(2.dp, MaterialTheme.colorScheme.onBackground),
            contentAlignment = Alignment.Center,
        ) { if (isaretli) Text("✓", color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.width(14.dp))
        Text(metin, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
