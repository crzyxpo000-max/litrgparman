package com.zincir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.mantik.Bicim
import com.zincir.mantik.Kilo
import com.zincir.mantik.Program
import com.zincir.mantik.Tarti
import com.zincir.veri.HaftaGozden
import kotlinx.coroutines.launch

private val KONTROL_LISTESI = listOf(
    "Tartı ortalamasına ve önceki haftaya göre farka baktım.",
    "Ağırlık seansı sayısına baktım; eksikse nedenini not ettim.",
    "Ortalama kalori ve proteini kontrol ettim.",
    "Ayar önerisini okudum; gerekiyorsa hedefi Ayarlar'dan güncelledim.",
    "Gelecek haftanın uçuş programına baktım.",
    "Gelecek hafta için seans günlerini seçtim.",
    "Whey, muz ve fıstık stoğunu kontrol ettim.",
)

/** Haftalık gözden geçirme: 10 dakikalık kontrol listesi ve gelecek haftanın seans günleri. */
@Composable
fun GozdenGecirmeEkrani(vm: AnaViewModel, onGeri: () -> Unit) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val kilolar by vm.kilolar.collectAsStateWithLifecycle()
    val seanslar by vm.seanslar.collectAsStateWithLifecycle()
    val sonGunler by vm.sonGunler.collectAsStateWithLifecycle()
    val sonYemekler by vm.sonYemekler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()

    val haftaBasi = Program.haftaBasi(a.baslangicGunu, bugun)
    val buHafta = Program.haftaIndex(a.baslangicGunu, bugun)
    val kayit by remember(haftaBasi) { vm.dao.gozdenAkisi(haftaBasi) }.collectAsStateWithLifecycle(null)
    val g = kayit ?: HaftaGozden(haftaBasi)
    val isaretler = g.isaretler.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    val planGunleri = g.planGunleri.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    fun yaz(yeni: HaftaGozden) = kapsam.launch { vm.dao.gozdenYaz(yeni) }

    val haftalar = remember(kilolar, a.baslangicGunu) { Kilo.haftalikOrtalamalar(kilolar.map { Tarti(it.gun, it.deger) }, a.baslangicGunu) }

    Sayfa {
        MetinDugme("‹ Geri", onClick = onGeri)
        EkranBasligi(
            "Haftalık gözden geçirme",
            ust = "Hafta ${buHafta + 1} · ${Bicim.kisaTarih(haftaBasi)} – ${Bicim.kisaTarih(haftaBasi + 6)} · yaklaşık 10 dk",
        )

        Bolum("Bu haftanın özeti", ust = 20.dp)
        val ort = haftalar.firstOrNull { it.index == buHafta }
        OzetSatiri("Tartı ortalaması", ort?.let { "${Bicim.kilo(it.ortalama)} kg" } ?: "—")
        OzetSatiri("Önceki haftaya göre", Kilo.fark(haftalar, buHafta)?.let { "${Bicim.fark(it)} kg" } ?: "—")
        HaftaOzetiTablosu(haftaOzeti(haftaBasi, seanslar, sonGunler, sonYemekler, a.germeHedefDk), a)

        Bolum("Ayar önerisi")
        AyarOnerileri(a, haftalar, buHafta)

        Bolum("Kontrol listesi")
        KONTROL_LISTESI.forEachIndexed { i, madde ->
            OnaySatiri(madde, i in isaretler) { sec ->
                val yeni = if (sec) isaretler + i else isaretler - i
                yaz(g.copy(isaretler = yeni.sorted().joinToString(",")))
            }
            Cizgi()
        }

        Bolum("Gelecek hafta seans günleri")
        Text(
            "Uçuş programına göre ${a.haftalikSeansHedef} gün seç. Seçtiğin günler Bugün ekranında görünür.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Bosluk()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0L..6L).forEach { i ->
                val gun = haftaBasi + 7 + i
                val secili = gun in planGunleri
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp)
                        .background(if (secili) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(role = Role.Checkbox) {
                            val yeni = if (secili) planGunleri - gun else planGunleri + gun
                            yaz(g.copy(planGunleri = yeni.sorted().joinToString(",")))
                        }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    val renk = if (secili) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                    Text(Bicim.gunAdi(gun), style = MaterialTheme.typography.labelSmall, color = renk)
                    Text("${java.time.LocalDate.ofEpochDay(gun).dayOfMonth}", style = MaterialTheme.typography.titleLarge, color = renk)
                }
            }
        }
        Bosluk(8.dp)
        Text("${planGunleri.size} gün seçildi.", style = MaterialTheme.typography.titleSmall)

        Bolum("Not")
        // Yazı kutusu kendi durumunu tutar; her değişiklik veritabanına da yazılır.
        var notMetni by remember(haftaBasi, kayit == null) { mutableStateOf(g.notMetni) }
        OutlinedTextField(
            value = notMetni,
            onValueChange = {
                notMetni = it
                yaz(g.copy(notMetni = it))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RectangleShape,
            placeholder = { Text("Bu hafta ne işe yaradı, ne engel oldu?") },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Bosluk(20.dp)
        DoluDugme("Tamam", Modifier.fillMaxWidth(), onClick = onGeri)
    }
}
