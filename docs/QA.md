# Kalite Kontrol (QA) — çalışma defteri

Bu dosya QA görevinin hafızasıdır: oturum kapanırsa buradan devam edilir. Adımlar ve kurallar kullanıcının görev
tanımındadır (2026-11-07); özet:

- **Kaynak kuralları:** paralel alt ajan yok (en fazla bir tane, yalnız Adım 3 hikaye örneklemesi için); content/
  baştan sona okunmaz, tarama programları yazılır; büyük kaynaklar önce aranır, sonra ilgili bölüm okunur.
- **Adımlar:** 1 otomatik ölçüm → 2 hedefli kod/motor/arayüz incelemesi → 3 hikaye/oynanış örneklemesi (tek alt ajan) →
  4 P0/P1/P2 raporu ve **dur, kullanıcıya sor** → 5 onaydan sonra 5–10 maddelik düzeltme partileri.
- Her adımın sonunda bu dosya güncellenir, commit atılır, kullanıcıya "Adım N bitti, /clear yapabilirsin" denir.

## Durum

| Adım | Durum |
| --- | --- |
| 1 Otomatik ölçüm | **bitti** (2026-11-07) |
| 2 Kod ve motor incelemesi | bekliyor |
| 3 Hikaye ve oynanış örneklemesi | bekliyor |
| 4 Rapor | bekliyor |
| 5 Düzeltme | onay bekleyecek |

Not: QA başlarken Türkçe üslup işinin (docs/USLUP.md) Parti 0 ve 1'i bitti ve commit edildi (`798d990`); Parti 2–10
QA'dan sonraya kaldı. Ölçümler Parti 1 sonrası içerikle alındı.

## Araçlar (yeniden kullanılacak)

```bash
scripts/check.sh                                   # test + validateContent + simülatör testi + uslup testi + APK + lint
./gradlew :tools:simulator:run --args="1000"       # docs/BALANCE.md (artık hiç görülmeyen / en nadir / en sık olaylar da var)
./gradlew :tools:simulator:perf                    # içerik yükleme, kayıt boyutu ve kodlama süresi (masaüstü)
./gradlew :tools:tarama:run                        # içerik taraması → docs/TARAMA.md (kategori sayıları + ayrıntı)
```

`tools/tarama` (yeni): ham YAML ağacını dolaşır. Kategoriler: eksik çeviri, TR/EN uzunluk oranı (belirteçler hariç,
<0.55 ya da >2.0), düz yazılmış NPC adı (ad alanları hariç), İngilizcede metrik birim, 3 cümleyi aşan paragraf,
60 karakteri aşan seçenek, birebir ve neredeyse aynı metin (5 sözcüklük parçalarla Jaccard ≥0.6), sonucu olmayan
seçim/etkisiz seçenek, konulup hiç okunmayan bayrak (motor ve app dizgeleri de okuma sayılır), hiç tohumlanmayan
yalnız-tohum olayı (motor aileleri ve başka yerden anılan kimlikler hariç), kilitli seçeneği olmayan olay, ilişki/hafıza
etkisi taşımayan ana NPC olayı.

## Adım 1 — Otomatik ölçüm sonuçları

### check.sh

- Geçti (test, validateContent, simülatör DengeTest, uslup testi, assembleDebug, lint).
- Lint: **0 hata, 29 uyarı, 1 ipucu.** UnusedResources 11 · UseKtx 6 · PluralsCandidate 6 · ObsoleteSdkInt 3 ·
  StringFormatCount 1 (`help_free`: EN'de kaçışsız `40% of`; çalışma anında zararsız) · OldTargetApi 1 (targetSdk 36,
  compileSdk 37) · ModifierParameter 1 (`StoryScreen.kt:92`).
- Kotlin derleyici uyarısı: yok.

### Simülatör (1000 oynanış, 17 sn; ayrıntı docs/BALANCE.md)

- **Son dağılımı** (hedef %1–%40, sağlanıyor): yalnız avcı 22.1 · kod kırıcı 14.4 · hasat 12.4 · koruyucu 12.2 ·
  vassal 10.9 · özgürlük 7.9 · demir taht 7.2 · döngü 7.2 · yükseliş 5.7. Tarza göre hedefe ulaşma: koruyucu %48.9 ve
  yükseliş %55.7 en düşük; özgürlük ve vassal %90.8.
- **Hakimiyet payı** (Gün 31+ oyuncu ≥ dünya seviyesi): **%82.2** (hedef %60+). Ezme payı %10.7.
- **Oyuncu/NPC güç farkı** (oyuncu p50 ÷ ana NPC ortalaması): Gün 30 ×0.7 · Gün 180 ×5.1 · Gün 330 ×19.5 · Gün 365 ×22.5.
- **Seviye eğrisi:** Gün 30'da oyuncu p50 8 / dünya 20; Gün 120'de 85 / 62; sonra hep önde. Mücadele evresi kısa ve
  yalnız Perde 1'de; Gün 90 p10 (27) dünyanın (43) gerisinde, p50 (60) önünde — dağılım geniş.
- **Dikkat eğrisi** (p50): Gün 30 8 · 90 48 · 180 61 · 210 91 · 270 97 · 300 90 · 365 89. p90 Gün 180'den sonra 97–98'de
  tavana yapışık: Perde 3–4 boyunca Dikkat neredeyse sabit tavanda.
- **Ölüm oranları:** Normal modda oynanış başına **0.00** ölüm, Demir Mod'da ölümle Hasat **%0.0**. Simülatör bayılma
  kuralı ve otomatik savaşla hiç ölmüyor → ya içerik çok kolay ya simülatörün savaş yapay zekası/kaçış kuralı oyuncudan
  çok iyi. Adım 3'te yorumlanacak.
- **Çıkmaz/çöken oynanış:** 0.
- **Hiç görülmeyen olay:** 5/255 — `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip` (cihaz bildirimine dokununca gelir;
  simülatörde beklenen), `hasat_vakti` (Gün 365 güvenlik ağı; beklenen), **`esik_itaat`** (itaat eşiği hiç tetiklenmiyor →
  incelenecek).
- **En nadir 10:** p3_firtina_olcucu %2.0 · kaya_soguk_firin 5.1 · thessar_kirik_muhur 6.2 · fener_yavrulari 11.9 ·
  deniz_yukselen_rakip 14.5 · deniz_anma_kardes 17.1 · deniz_kardes_cizimi 17.4 · deniz_anma_ayna 18.2 ·
  pala_yargilanmasi 18.9 · kadimler_5_teslim 19.8.
- **En sık 10** (hepsi %100, Perde 4 ana yayı): son_karar, p4_son_gece_ates, p4_yikik_kiyikent, p4_son_yayin,
  p4_siralama_zirvesi, p4_kadim_oncusu, sistem_cekirdegi, cekirdege_yol, kadim_istilasi, fraksiyon_son_pozisyon.
- Kule: ortalama en yüksek kat 96, %91.8 zirvede.

### İçerik taraması (docs/TARAMA.md; 19.249 metin, 4.761 seçenek, 255 olay)

| Kategori | Sayı | Not |
| --- | ---: | --- |
| Eksik/boş çeviri | 0 | |
| TR/EN uzunluk oranı aşırı | 0 | EN/TR oranı p1 0.79 · p50 1.10 · p99 1.52 |
| Düz yazılmış NPC adı | 35 | awakenings 10, enemies 11, knowledge 4, system 4, essences 2, items 3, tower 1; çoğu "Yankı"/"Echo" (`awakenings.yaml` uyanış satırları: "Yankı fısıldıyor: \"…\"" — hem düz ad hem tırnaklı/büyük harfli Yankı sesi) |
| İngilizcede metrik birim | 0 | |
| 3 cümleyi aşan paragraf | 1.777 | dosya başlıklarındaki "paragraf en çok 3 cümle" kuralı; diyalog parçaları da cümle sayılıyor, kaba ölçü |
| 60 karakteri aşan seçenek | 31 | |
| Birebir aynı metin (TR) | 28 | çoğu zar başarı/başarısız ya da iki dalın ortak satırı; bazıları aynı sahnede art arda (ör. `71_perde3 buyuk_ihanet_kule` paragraf 7=8) |
| Neredeyse aynı metin | 18 | |
| Sonucu olmayan seçim / etkisiz seçenek | 0 / 0 | |
| Konulup hiç okunmayan bayrak | **352** | flags.yaml'da tanımlı ama hiçbir koşul, son, epilog ya da motor kodu okumuyor (ör. `aras_kule_kazanildi`, `dokuyucu_olduruldu`, `taht_egildi`); seçimin uzun vadeli izi yok |
| Hiç tohumlanmayan yalnız-tohum olayı | 0 | |
| Kilitli seçeneği olmayan olay | 48 | |
| İlişki/hafıza etkisi olmayan ana NPC olayı | 0 | |

### Performans

| Ölçü | Değer | Not |
| --- | --- | --- |
| Soğuk açılış (emülatör, debug APK, `am start -W`) | 1.555–1.798 ms | release + R8 ile daha düşük olmalı |
| İçerik yükleme | masaüstü JVM: ilk 708 ms, ısınmış 302 ms; **cihazda ilk "Devam et" dokunuşunda ana iş parçacığı 3,9 sn donuyor** (Choreographer 222 kare atladı, Davey 3.879 ms) | `YukselisApplication.engine` `by lazy`, ilk motor çağrısında ana iş parçacığında çözülüyor; ANR riski (düşük cihazda 5 sn) |
| Motor eylemleri | `choose`/`hubAction`/`combatAction` ana iş parçacığında; tam oynanış (≈580 savaş) masaüstünde 476 ms | eylem başı ms düzeyi; sorun değil |
| APK boyutu | debug 14,8 MB · release AAB 6,1 MB | content/ 6,6 MB ham YAML |
| Kayıt dosyası | Gün 180 savaş ortası 23 KB; kodlama 13,7 ms, çözme 12,3 ms (masaüstü) | kayıt IO kuyruğunda; sorun değil |

### Adım 1'in işaretledikleri (Adım 4'te sınıflanacak)

1. İlk motor kullanımında 3,9 sn ana iş parçacığı donması (içerik yüklemesi).
2. Simülatörde sıfır ölüm (Normal ve Demir) — zorluk ya da simülatör gerçekçiliği.
3. Dikkat Perde 3–4'te tavanda sabit (p90 97–98, p50 ~90): eşik ve tepki çeşitliliği kayboluyor olabilir.
4. `esik_itaat` hiç tetiklenmiyor.
5. 352 okunmayan bayrak: seçimlerin uzun vadeli sonucu yok.
6. `awakenings.yaml` ve birkaç ansiklopedik dosyada düz "Yankı/Echo" adı ve Yankı'nın tırnaklı/büyük harfli sesi;
   prologda da Yankı sözleri tırnaklı/büyük harfli. **Kullanıcı: kasıtlı, bırak (DECISIONS #128)** — yalnız düz yazılmış
   ad (belirteç yerine "Yankı") kalan sorun.
7. Oyun tarzı hedefe ulaşma: koruyucu %48.9, yükseliş %55.7.
8. Lint: `help_free` format uyarısı, 11 kullanılmayan kaynak, `ModifierParameter`.
