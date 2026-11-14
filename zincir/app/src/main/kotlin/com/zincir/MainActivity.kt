package com.zincir

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.bildirim.Bildirimler
import com.zincir.mantik.Bicim
import com.zincir.ui.AntrenmanEkrani
import com.zincir.ui.AyarlarEkrani
import com.zincir.ui.BeslenmeEkrani
import com.zincir.ui.BugunEkrani
import com.zincir.ui.EsneklikEkrani
import com.zincir.ui.GozdenGecirmeEkrani
import com.zincir.ui.IlerlemeEkrani
import com.zincir.ui.ZincirTema

/** Uygulamanın tek penceresi. Bütün ekranlar bunun içinde Compose ile çizilir. */
class MainActivity : ComponentActivity() {
    private val vm: AnaViewModel by viewModels()

    /** Telefonun dili ne olursa olsun arayüz Türkçe olsun (tarih seçici, sayı biçimi). */
    override fun attachBaseContext(newBase: Context) {
        val ayar = Configuration(newBase.resources.configuration)
        ayar.setLocale(Bicim.TR)
        super.attachBaseContext(newBase.createConfigurationContext(ayar))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AnaEkran(vm) }
    }

    override fun onResume() {
        super.onResume()
        ZincirUygulamasi.onPlanda = true
        Bildirimler.zamanlayiciBildirimleriniKaldir(this)
        vm.gunuYenile()
    }

    override fun onPause() {
        // Ekran kilitlenince ya da başka uygulamaya geçilince: zamanlayıcı bitişini artık bildirim haber verir.
        ZincirUygulamasi.onPlanda = false
        super.onPause()
    }
}

private val SEKMELER = listOf("Bugün", "Antrenman", "Beslenme", "Esneklik", "İlerleme")

/** Alt sekmelerin üstünde açılan tam ekranlar. */
private const val AYARLAR = "ayarlar"
private const val GOZDEN = "gozden"

@Composable
fun AnaEkran(vm: AnaViewModel) {
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    ZincirTema(ayarlar?.tema ?: 0) {
        var sekme by rememberSaveable { mutableIntStateOf(0) }
        var ustEkran by rememberSaveable { mutableStateOf<String?>(null) }

        // Geri tuşu: önce üst ekranı kapatır, sonra Bugün sekmesine döner, en son uygulamadan çıkar.
        BackHandler(enabled = ustEkran != null || sekme != 0) {
            if (ustEkran != null) ustEkran = null else sekme = 0
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { if (ustEkran == null) AltCubuk(sekme) { sekme = it } },
        ) { bosluk ->
            Box(Modifier.fillMaxSize().padding(bosluk).consumeWindowInsets(bosluk).imePadding()) {
                when (ustEkran) {
                    AYARLAR -> AyarlarEkrani(vm, onGeri = { ustEkran = null })
                    GOZDEN -> GozdenGecirmeEkrani(vm, onGeri = { ustEkran = null })
                    else -> when (sekme) {
                        0 -> BugunEkrani(vm, onAyarlar = { ustEkran = AYARLAR }, onSekme = { sekme = it })
                        1 -> AntrenmanEkrani(vm)
                        2 -> BeslenmeEkrani(vm)
                        3 -> EsneklikEkrani(vm)
                        else -> IlerlemeEkrani(vm, onGozden = { ustEkran = GOZDEN })
                    }
                }
            }
        }
    }
}

/** Alt sekme çubuğu: simge yok, yalnızca yazı. Seçili sekme kalın ve üstünde siyah çizgi. */
@Composable
private fun AltCubuk(secili: Int, onSec: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            SEKMELER.forEachIndexed { i, ad ->
                val sec = i == secili
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab) { onSec(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(3.dp)
                            .background(if (sec) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background)
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            ad, fontSize = 13.sp, maxLines = 1,
                            fontWeight = if (sec) FontWeight.Bold else FontWeight.Normal,
                            color = if (sec) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
