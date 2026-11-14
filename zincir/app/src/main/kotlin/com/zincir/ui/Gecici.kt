package com.zincir.ui

import androidx.compose.runtime.Composable
import com.zincir.AnaViewModel

// Geçici yer tutucular: her aşamada biri gerçek ekranla değiştirilecek.
@Composable fun BugunEkrani(vm: AnaViewModel, onAyarlar: () -> Unit, onSekme: (Int) -> Unit) = Sayfa { EkranBasligi("Bugün") }
@Composable fun AntrenmanEkrani(vm: AnaViewModel) = Sayfa { EkranBasligi("Antrenman") }
@Composable fun BeslenmeEkrani(vm: AnaViewModel) = Sayfa { EkranBasligi("Beslenme") }
@Composable fun EsneklikEkrani(vm: AnaViewModel) = Sayfa { EkranBasligi("Esneklik") }
@Composable fun IlerlemeEkrani(vm: AnaViewModel, onGozden: () -> Unit) = Sayfa { EkranBasligi("İlerleme") }
@Composable fun AyarlarEkrani(vm: AnaViewModel, onGeri: () -> Unit) = Sayfa { EkranBasligi("Ayarlar") }
@Composable fun GozdenGecirmeEkrani(vm: AnaViewModel, onGeri: () -> Unit) = Sayfa { EkranBasligi("Haftalık gözden geçirme") }
