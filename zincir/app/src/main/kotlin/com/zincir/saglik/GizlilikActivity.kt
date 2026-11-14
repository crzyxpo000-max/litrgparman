package com.zincir.saglik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zincir.ui.Bosluk
import com.zincir.ui.DoluDugme
import com.zincir.ui.EkranBasligi
import com.zincir.ui.Sayfa
import com.zincir.ui.ZincirTema

/**
 * Health Connect, izin isteyen her uygulamadan "bu veriyi neden istiyorsun" sayfası ister.
 * Health Connect'teki "gizlilik politikası" bağlantısına dokununca bu sayfa açılır.
 */
class GizlilikActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ZincirTema(0) {
                Surface(Modifier.safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
                    Sayfa {
                        EkranBasligi("Sağlık verisi")
                        Bosluk()
                        Text(
                            "Zincir, Health Connect'ten yalnızca adım sayısını, uyku süresini ve aktif kaloriyi okur. " +
                                "Bu veriler günlük kaydını doldurmak için kullanılır ve telefondan dışarı çıkmaz: " +
                                "uygulamanın internet izni yoktur; hesap, sunucu, reklam ve analitik yoktur. " +
                                "Zincir, Health Connect'e hiçbir şey yazmaz. İzni istediğin an Health Connect'ten geri alabilirsin.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Bosluk(24.dp)
                        DoluDugme("Kapat", Modifier) { finish() }
                    }
                }
            }
        }
    }
}

