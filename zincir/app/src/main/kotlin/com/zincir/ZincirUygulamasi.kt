package com.zincir

import android.app.Application
import com.zincir.bildirim.Bildirimler
import com.zincir.veri.Depo
import com.zincir.veri.Veritabani

/** Uygulama açılırken bir kez kurulur; veritabanını ve depoyu herkes buradan alır. */
class ZincirUygulamasi : Application() {
    val depo: Depo by lazy { Depo(Veritabani.ac(this)) }

    override fun onCreate() {
        super.onCreate()
        Bildirimler.kanalOlustur(this)
    }

    companion object {
        /** Uygulama ekranda ve açık mı? Zamanlayıcı bitince titreşimi ekran mı, bildirim mi yapacak buna göre seçilir. */
        @Volatile
        var onPlanda: Boolean = false
    }
}
