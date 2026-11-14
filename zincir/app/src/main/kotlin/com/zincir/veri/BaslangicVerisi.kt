package com.zincir.veri

import java.time.LocalDate

/**
 * İlk açılışta veritabanına yazılan program. Buradaki her şey sonradan Ayarlar'dan düzenlenebilir;
 * uygulama bu dosyayı yalnızca veritabanı boşken okur.
 */
object BaslangicVerisi {
    val PROGRAM_BASLANGICI: Long = LocalDate.of(2026, 11, 23).toEpochDay()

    fun ayarlar() = Ayarlar(baslangicGunu = PROGRAM_BASLANGICI, siraGunu = PROGRAM_BASLANGICI)

    val hareketler = listOf(
        // Seans A
        Hareket(seans = "A", sira = 1, ad = "Leg press", alternatifAd = "Goblet squat", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 120),
        Hareket(seans = "A", sira = 2, ad = "Dumbbell bench press", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 120),
        Hareket(seans = "A", sira = 3, ad = "Seated cable row", alternatifAd = "Tek kol dumbbell row", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 90),
        Hareket(seans = "A", sira = 4, ad = "Dumbbell shoulder press", setSayisi = 2, tekrarMin = 10, tekrarMax = 12, dinlenmeSn = 90),
        Hareket(seans = "A", sira = 5, ad = "Leg curl", alternatifAd = "Tek bacak Romanian deadlift", setSayisi = 2, tekrarMin = 10, tekrarMax = 15, dinlenmeSn = 90),
        Hareket(seans = "A", sira = 6, ad = "Plank", setSayisi = 3, tekrarMin = 30, tekrarMax = 45, birim = "sn", dinlenmeSn = 60, agirlikli = false),
        // Seans B
        Hareket(seans = "B", sira = 1, ad = "Dumbbell Romanian deadlift", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 120),
        Hareket(seans = "B", sira = 2, ad = "Lat pulldown", alternatifAd = "Dumbbell pullover", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 120),
        Hareket(seans = "B", sira = 3, ad = "Incline dumbbell press", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 90),
        Hareket(seans = "B", sira = 4, ad = "Split squat", setSayisi = 2, tekrarMin = 10, tekrarMax = 10, ek = "her bacak", dinlenmeSn = 90),
        Hareket(seans = "B", sira = 5, ad = "Dumbbell curl + triceps pushdown", setSayisi = 2, tekrarMin = 12, tekrarMax = 12, ek = "her biri", dinlenmeSn = 60),
        Hareket(seans = "B", sira = 6, ad = "Dead bug", setSayisi = 3, tekrarMin = 10, tekrarMax = 12, dinlenmeSn = 60, agirlikli = false),
    )

    val ogunler = listOf(
        Ogun(sira = 1, ad = "Kahvaltı (otel büfesi)", kcal = 600, protein = 35,
            icerik = "3 yumurta (150 g) · yoğurt 200 g · 2 dilim ekmek (60 g) veya yulaf 40 g kuru · 1 meyve (150 g) · pişmiş sebze 100 g"),
        Ogun(sira = 2, ad = "Öğle", kcal = 680, protein = 55,
            icerik = "Izgara tavuk veya balık 170 g · sade pilav 150 g · dal 150 g · pişmiş sebze 150 g"),
        Ogun(sira = 3, ad = "Ara öğün", kcal = 250, protein = 25,
            icerik = "Whey 30 g · muz 120 g veya fıstık 30 g"),
        Ogun(sira = 4, ad = "Akşam", kcal = 650, protein = 45,
            icerik = "Tavuk veya balık 150 g ya da 3 yumurtalı köri · 1 roti (45 g) veya pilav 150 g · dal 150 g · pişmiş sebze 150 g"),
        Ogun(sira = 5, ad = "Ek pişirme yağı payı", kcal = 70, protein = 0, icerik = "8 g yağ", pay = true),
    )

    val yiyecekler = listOf(
        Yiyecek(ad = "Tavuk göğsü (pişmiş)", bazGram = 100.0, kcal = 165.0, protein = 31.0),
        Yiyecek(ad = "Tavuk but, derisiz", bazGram = 100.0, kcal = 190.0, protein = 25.0),
        Yiyecek(ad = "Izgara balık", bazGram = 100.0, kcal = 130.0, protein = 22.0),
        Yiyecek(ad = "Yumurta (1 adet)", bazGram = 50.0, kcal = 72.0, protein = 6.0),
        Yiyecek(ad = "Dal (pişmiş)", bazGram = 100.0, kcal = 100.0, protein = 6.0),
        Yiyecek(ad = "Sade pilav (pişmiş)", bazGram = 100.0, kcal = 130.0, protein = 3.0),
        Yiyecek(ad = "Roti (1 adet)", bazGram = 45.0, kcal = 120.0, protein = 4.0),
        Yiyecek(ad = "Ekmek (1 dilim)", bazGram = 30.0, kcal = 80.0, protein = 3.0),
        Yiyecek(ad = "Yulaf (kuru)", bazGram = 40.0, kcal = 150.0, protein = 5.0),
        Yiyecek(ad = "Sade yoğurt", bazGram = 100.0, kcal = 60.0, protein = 3.5),
        Yiyecek(ad = "Whey", bazGram = 30.0, kcal = 120.0, protein = 24.0),
        Yiyecek(ad = "Muz", bazGram = 120.0, kcal = 105.0, protein = 1.0),
        Yiyecek(ad = "Fıstık", bazGram = 30.0, kcal = 170.0, protein = 7.0),
        Yiyecek(ad = "Pişirme yağı", bazGram = 10.0, kcal = 90.0, protein = 0.0),
    )

    val germeler = listOf(
        Germe(sira = 1, ad = "Sırtüstü, havluyla düz bacak kaldırma", bolge = "arka bacak", ikiTaraf = true),
        Germe(sira = 2, ad = "Yarım diz çökme, kalçayı öne itme", bolge = "kalça önü", ikiTaraf = true),
        Germe(sira = 3, ad = "Sırtüstü, ayağı karşı dize koyup çekme", bolge = "kalça dışı", ikiTaraf = true),
        Germe(sira = 4, ad = "Duvara yaslanıp arka topuğu yere basma", bolge = "baldır", ikiTaraf = true),
        Germe(sira = 5, ad = "Kapı çerçevesinde ön kolları dayayıp öne adım", bolge = "göğüs ve ön omuz"),
        Germe(sira = 6, ad = "Dizler üstünde, kollar önde yere uzanma", bolge = "sırt ve bel"),
    )

    val egerPlanlar = listOf(
        EgerPlan(sira = 1, metin = "Eğer uçuştan döndüysem ve 6 saatten fazla uyuduysam, kahvaltıdan 90 dakika sonra salona inerim."),
        EgerPlan(sira = 2, metin = "Eğer canım tatlı isterse, önce whey ve bir meyve yerim, 20 dakika beklerim."),
        EgerPlan(sira = 3, metin = "Eğer bir öğünü bozarsam, sonraki öğün plandaki öğündür."),
        EgerPlan(sira = 4, metin = "Eğer tartı 2 hafta düşmezse bırakmam; ayar kuralını uygularım."),
    )

    // Aşağıdakiler düzenlenmeyen sabit listelerdir.
    val KARDIYO_TURLERI = listOf("Eğimli yürüyüş", "Bisiklet", "Eliptik")
    val KACAMAKLAR = listOf("Biryani", "Paratha", "Kızartma", "Şerbetli tatlı")
}
