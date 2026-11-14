package com.zincir.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room veritabanı. Tabloya sütun eklersen [version] sayısını artırman ve bir "migration" yazman gerekir;
 * yoksa uygulama eski veriyle açılamaz.
 */
@Database(
    entities = [
        Ayarlar::class, Hareket::class, Seans::class, SetKaydi::class, Ogun::class, Yiyecek::class,
        YemekKaydi::class, Germe::class, GunKaydi::class, Olcum::class, Kacamak::class, EgerPlan::class,
        HaftaGozden::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class Veritabani : RoomDatabase() {
    abstract fun dao(): TakipDao

    companion object {
        fun ac(context: Context): Veritabani =
            Room.databaseBuilder(context.applicationContext, Veritabani::class.java, "zincir.db").build()
    }
}
