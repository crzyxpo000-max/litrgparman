# Kalite Kontrol (QA) — rapor ve çalışma defteri

## Özet

1. Oyun **sağlam ve bitirilebilir**: 1000 simülasyonda çöken ya da takılan oynanış yok, 9 sonun hepsi %5–%22 bandında, kayıtlar savaş ortasında bile kayıpsız ve belirlenimci, eski sürüm kayıtlar açılıyor; check.sh ve lint (0 hata) temiz.
2. Hikaye iskeleti, sonlar ve İngilizce yerelleştirme güçlü; NPC sesleri iki dilde tutarlı. Türkçede Perde 1 yeniden yazıldı, **Perde 2–4 hâlâ çeviri kokuyor**.
3. **En kritik 1 — İlk "Devam et"/"Yeni oyun" dokunuşunda ana iş parçacığı 3,9 sn donuyor** (içerik ana iş parçacığında yükleniyor): düşük cihazda ANR ("uygulama yanıt vermiyor") riski.
4. **En kritik 2 — OP MC hissi sayılarda var, oyunda zayıf:** oynanış başına 519 savaş, yalnız %10,7'si Ezme, ölüm riski sıfır; Perde 1'de oyuncuların %90'ı sınıf teklifini (Sv 10) ve Kademe Kırma'yı (Sv 14) hiç görmüyor; Dikkat Perde 3'te üç ay tavanda.
5. **En kritik 3 — Görünür metin/mekanik hataları:** 95 sezgide çift önek ("[ALG] [ALG] …"), erişilemeyen `esik_itaat`, seçimden önce ilan edilen Deniz ölümü, gerçekleşmeyen sonuçları anlatan kronik satırları (352 bayrak hiç okunmuyor).

Biçim: **[kimlik] açıklama** — *nerede* — öneri — büyüklük (küçük / orta / büyük). Kaynaklar: Adım 1–3 notları
(aşağıda), docs/TARAMA.md, docs/BALANCE.md, docs/qa/hikaye_a.md, docs/qa/hikaye_b.md.

## P0 — çökme, kayıt kaybı, ilerlemeyi engelleyen hata, ulaşılamayan son

- [x] **P0-1 İlk motor erişiminde 3,9 sn ana iş parçacığı donması (ANR riski).** — *app `YukselisApplication.engine` (`by lazy`), ilk `newGame`/`continueGame`* — Uygulama açılır açılmaz içeriği `Dispatchers.Default`'ta önceden yükle; yüklenene dek başlık düğmelerinde ilerleme göstergesi; motor çağrılarını ana iş parçacığından al. Ölç: Choreographer atlanan kare <10. — orta

Başka P0 bulunmadı: ulaşılamayan son yok, çıkmaz yok, kayıt kaybı yalnız bozuk dosyada (P1-20).

## P1 — denge, kırık dal, belirgin çeviri/yerelleştirme hatası, kötü arayüz deneyimi

**Mekanik / metin uyuşmazlıkları**
- [x] **P1-1 Çift sezgi öneki: ~95 sezgi ekranda "[ALG] [ALG] …" (bir yerde "[BİL] [ALG]").** — *content/events 50_perde2_ana 25, 61_kiyikent_b 26, 71_perde3 22, 80_perde4 18, diğer 4; motor `StoryEngine.insightPrefix`* — Metin başındaki `[XXX] ` öneklerini (TR+EN) toplu sil; validator'a "stat koşullu sezgi `[` ile başlamaz" kuralı. — küçük
- [x] **P1-2 `esik_itaat` hiç tetiklenemez.** — *NpcEngine.thresholds: yoldaş + korku ≥60 + cesaret ≤3 + sadakat ≥6; tek aday Mert (cesaret 2, sadakat 5)* — Koşulu `sadakat ≥6 ya da bağlılık ≥40` yap veya Mert'e özel `esik_itaat_mert_kilic`; simülatörde görüldüğünü doğrula. — küçük
- [x] **P1-3 Kademe Kırma seçeneğine pratikte erişilemiyor.** — *40_ana_yay `yeni_tavan_1` `kir`: `seviye_min: 14`, Gün 24; Gün 30 p90 Sv 9* — Eşiği 7–8'e indir ya da stat/iz koşuluna çevir; simülatörde `yeni_tavan_kirma` oranını ölç (hedef %15–30). — küçük
- [x] **P1-4 Deniz'in ölümü seçimden önce ilan ediliyor.** — *40_ana_yay `bekci_deniz_hat` ¶3 "Yüzünü son görüşün bu oluyor"; `vazgec` Deniz'i yaşatıyor* — Cümleyi ölüm dalına (`bekci_cokus_deniz`) taşı ya da belirsizleştir. — küçük
- [x] **P1-5 Kronik satırları gerçekleşmeyen ya da henüz belli olmayan sonuçları anlatıyor; 352 bayrak hiç okunmuyor.** — *ör. `gemi_kule›ezberle`, `perde3_son_ordu›talim`, `ihanet_mert_konus›geri_cek`, `elif_konsey_oylamasi›zorlama`, `vmask_hakli›kir`, `akd_kopru›sebep`, `mkt_kacamak›tehdit`; liste docs/TARAMA.md* — Kronikleri yalnız kesinleşeni anlatacak biçimde yeniden yaz ya da vaadi tutan bayrak + koşullu satır ekle; savaşa bağlı kroniği `kazan` sahnesine taşı; taramaya "kronik vaat ediyor ama bayrak yok" kuralı. — büyük
- [x] **P1-6 Vassal sonu Thessar düğümündeki seçimleri yansıtmıyor (B tavanı, Defter Bekçisi, "örnek" maddesi).** — *55_kadimler_thessar `thessar_vassal_teklifi_*` → 81_sonlar `son_vassal`* — Madde seçimine bayrak koy; sona koşullu paragraf ve unvan satırı. — orta
- [x] **P1-7 Döngü sonu New Game+ ile çelişiyor ("önceki altı döngünün altısı da hasatla kapandı").** — *`son_dongu`, `yanki_son_sir` (ysr_1 sezgisi, ysr_sir)* — `onceki_oyun ≥1` için ayrı paragraf: önceki döngü oyuncunun bitirdiği sonla anılsın. — küçük
- [x] **P1-8 `son_yukselis`'te çelişen iki Deniz paragrafı birlikte görünebilir (doğrulanmalı).** — *81_sonlar `son_yukselis`: `yoldas` ve `npc_fraksiyon: yukselenler` koşulları çakışıyor* — Koşulları dışlayıcı yap; bir test kaydıyla doğrula. — küçük

**Denge ve oynanış**
- [x] **P1-9 Seviye 10 sınıf teklifi Perde 1'de oyuncuların %90+'ına gelmiyor (GDD Perde 1 düğümü).** — *BALANCE Gün 30 p50 8 / p90 9; Perde 1 XP* — Perde 1 XP'sini %20–30 artır ya da Perde 1 düğüm listesini güncelle; simülatöre "ilk sınıf günü" ölçüsü. — orta
- [x] **P1-10 Hissedilen hakimiyet zayıf: Ezme %10,7; oyuncu dünya seviyesinin ancak 1,1–1,5 katı.** — *Perde 2+ akış düşmanları `@dunya`; Ezme eşiği `CombatMath.isCrush`* — Akış (fırsat eli/keşif) düşmanlarını `@dunya-20…-40`'a çek ya da Ezme eşiğini gevşet; hedef Ezme Perde 2 ~%30, Perde 3–4 ~%50; boss ve hikaye savaşları zor kalsın. — orta
- [x] **P1-11 Savaş sayısı yüksek, risk yok: oynanış başına 519 savaş, ölüm 0, İkinci Şans 0, yere düşme %6,4.** — *simülatör; gün akışı keşif savaşları* — Keşif savaşı sıklığını düşür (ya da savaşsız keşif sonuçlarını artır) ve hikaye/boss savaşlarını sertleştir; hedef: oynanış başına ~250 savaş, hikaye savaşlarında yere düşme %15–25. P1-10 ile birlikte ayarlanmalı. — orta
- [x] **P1-12 Dikkat Perde 3–4'te tavanda (Gün 210–300 p50 91–97) ve Yönetmen'in hedef gerilimi hep 4.** — *Dikkat dinamiği (`dikkat`, günlük sönüm), `Director.desiredTension` (+Dikkat/40)* — Perde 3'te doğal sönümü artır / eşik olayı sonrası boşaltma; `desiredTension`'da Dikkat katkısını sınırla; GDD'deki "üç ağır olaydan sonra kamp ateşi"ni uygula (kod iki diyor). Hedef p50 60–85 dalgalanma. — orta
- [x] **P1-13 Yalnız Avcı en sık son (%22; rastgele oyuncuda %38); Koruyucu tarzı hedefe %49, Yükseliş %56.** — *endings.yaml koşulları, yoldaş ayrılık eşiği `DEPARTURE_DISTANCE`* — Yalnız Avcı'ya "bilinçli yalnızlık" koşulu ekle (ayrılık eşiği yaşandı / yoldaş bırakıldı), Koruyucu'nun hangi koşulda düştüğünü simülatörde ayrıştır. — orta
- [x] **P1-14 Perde 4 temposu: 27 günde 11 zorunlu düğüm, art arda 4 "veda turu" aynı iskelette.** — *80_perde4, 81_sonlar: `perde3_son_gece`, `p4_son_gece_ates`, `son_karar_veda`, `son_karar_2`* — `p4_son_gece_ates` ile `son_karar_veda`'yı birleştir ya da birini koşullu yap; `p4_yikik_kiyikent` ve `p4_siralama_zirvesi`'ni fırsat eli kartına çevir; yoldaş satır sırası ilişkiye göre. — orta

**Türkçe ve İngilizce metin**
- [x] **P1-15 Perde 2–4 Türkçesi USLUP'tan geçmedi (kılavuzun "kaçın" örnekleri birebir duruyor).** — *docs/USLUP.md Parti 2–10* — Üslup partilerini sürdür; sıra önerisi: 50_perde2_ana → 81_sonlar → 80_perde4 → 71_perde3 → kalanlar. — büyük
- [x] **P1-16 Türkçe dil hataları.** — *`bekci_cokus_deniz›yanki` "{npc:yanki} tek söz etmeden sustur onu"; `yanki_meydan` "bir sürü balık gibi"; `gecit_korundu` "kimse ona zorlamıyor"; "Hasatın" (5 yer); `fraksiyon_son_pozisyon` "steteskobu"* — Tek tek düzelt (Parti 2 beklemeden). — küçük
- [x] **P1-17 Bir yıldan kısa süren dünyada "yıllardır".** — *53_yukselenler_vahit 2188/2481/2542 (`vmask_1`, `vmask_oda›bak_sahte`), 52_lonca_kaya 2247; TR/EN farklı ("for months")* — "aylardır / Entegrasyon'dan beri". — küçük
- [x] **P1-18 EN bozuk cümle.** — *`deniz_son_secim_kalir_yarali` ("wider than it is."; "the fire" düşmüş)* — Cümleyi onar. — küçük

**Arayüz ve kayıt**
- [x] **P1-19 360dp + yazı ×1,3: seçenek paneli ekranın yarısından fazlası, hikayeye ~4 satır kalıyor.** — *app `ActionDock` (StoryScreen)* — Dock'a en yüksek yükseklik (~%45) + kendi kaydırması; uzun etiketlerde yazı ölçeğini koru. — küçük
- [x] **P1-20 Bozuk otomatik kayıt sessizce kayboluyor; Demir Mod'da tek kayıt olduğundan oyun gider.** — *engine `SaveManager.load` null; app `FileSaveStorage.write` yedeksiz* — Yazmadan önce önceki kaydı `auto.prev.json`'a döndür; okuma başarısızsa yedekten aç ve kullanıcıya bildir. — küçük

## P2 — cilalama

**Arayüz / erişilebilirlik / performans**
- [x] P2-1 Tablette metin tam genişlik (~95 karakter/satır). — *App/StoryScreen* — İçeriği ~640dp ile sınırla ve ortala; geniş ekranda `NavigationRail`. — küçük
- [x] P2-2 Başlık ekranında ×1,3 yazıda "Yükseliş / Protokolü" satırları üst üste biniyor. — *başlık tipografisi lineHeight* — Satır yüksekliğini artır. — küçük
- [x] P2-3 Tıklanabilir kartlarda TalkBack eylem etiketi yok. — *ui/system/Overlays.kt:128, 158* — `onClickLabel` + `Role.Button`. — küçük
- [x] P2-4 Yapılandırma değişince (döndürme/yeniden boyut) daktilo efekti baştan başlıyor. — *StoryScreen `finished` durumu* — `rememberSaveable`. — küçük
- [x] P2-5 Her eylemde 12 görünüm modeli ana iş parçacığında kuruluyor (çoğu görünmüyor). — *GameViewModel.render* — Görünür sekmeninkini hemen, kalanları tembel/arka planda kur. — orta
- [x] P2-6 Lint uyarıları: `help_free` EN'de kaçışsız `40%`, 11 kullanılmayan kaynak, `ModifierParameter` (StoryScreen.kt:92), 6 PluralsCandidate. — küçük

**Motor / kod**
- [x] P2-7 `Director.arcPriority` ölü kod (ana yay olayları adaylardan zaten dışlı). — küçük
- [x] P2-8 Tohum koşulu her gün YAML'dan yeniden ayrıştırılıyor (`Director.seedConditionHolds`). — önbellek — küçük
- [x] P2-9 `sanitize` `dayState.currentEvent`'i denetlemiyor (içerikten silinen olayın ortasında kayıt). — küçük
- [x] P2-10 `render` içindeki görünüm üreticileri try/catch dışında (bir görünüm istisnası çökertir). — küçük
- [x] P2-11 Koşul DSL'inde VEYA yok; birebir kopya paragraflar (ör. `buyuk_ihanet_kule` ¶7=¶8) bakım riski. — `herhangi: [...]` koşulu — orta
- [x] P2-12 Belgeler: CLAUDE.md kayıtları `*.yaml` diyor (gerçekte `*.json`); simülatörün gereksiz `-Xss4m`'i. — küçük

**İçerik ve üslup**
- [x] P2-13 Düz "Yankı/Echo" adı (35 yer; awakenings, enemies, knowledge, system) ve `son_nefes` Yankı satırı `yanki_bagi` koşulsuz. — belirteç + koşul — küçük
- [x] P2-14 EN metinlerde belirteç yerine düz yer adı ("The Esplanade", "The Ironworks", "{npc:yanki}'s"). — `{yer:…}`/`|pos`; validator'ı EN'e genişlet — küçük
- [x] P2-15 Terim tutarlılığı: "Grade" yazımı (Grade E / A-Grade / S grade), carrier/host, dünya sayısı (yüzlerce/bin/bin dört yüz), Yankı fısıltısında "Sistem/sistem". — küçük
- [x] P2-16 `son_hasat` her Demir Mod ölümüne "haddini aştın" diyor. — Dikkat koşullu iki sürüm — küçük
- [x] P2-17 Sonlarda küçük tutarsızlıklar: Koruyucu'da "başka hiçbir şey söylemeden" sonrası konuşma, Kod Kırıcı'da ölü paragraf, Demir Taht'ta Kadim dalı eksik, Yankı'nın son sözü üç sonda yok, arketip sırası sonla çelişebiliyor, `son_karar_1`/`son_koruyucu` aynı imge art arda. — küçük–orta
- [ ] P2-18 Kronik açılış iskeleti tekrarı (%20 aynı ilk iki sözcük; EN "On the night of" ×39); Döngü epilog kartlarında kalıp. — üslup partilerinde + epilog seçicisine "aynı açılışı yan yana koyma" — orta
- [x] P2-19 Küçük süreklilik: `ilk_kan›pencereyi_oku_ilk` yabancıyı düşürüyor, `gemi_bakis` koşulsuz geri çağrı, Kaya darbe gecesi zaman çizelgesi, `ihanet_kaya_1` kroniği herkese akıl hocası Kaya, Thessar/Mert/Aras EN pürüzleri (docs/qa/hikaye_b.md 2.3). — küçük
- [ ] P2-20 3 cümleyi aşan paragraf 1.777, 60 karakteri aşan etiket 5 (TARAMA). — üslup partilerinde — orta
- [x] P2-21 `tools/qa/olay.py` bildirim koşullarını göstermiyor; taramaya kronik-vaat ve OR-kopya kuralları. — küçük

## Ek: Parti 1 (Türkçe üslup) önce → sonra örnekleri

> Kımıldadığını görmüyor bile. Elma bir anda senin elinde; onun çakısı hâlâ havada, boşluğu soyuyor.
> → Kıpırdadığını bile görmüyor. Elma göz açıp kapayıncaya kadar senin elinde; onun çakısıysa hâlâ havada, boşluğu soyup duruyor.

> Kapılar kapanıyor ve vagon tek bir ses çıkarmadan tünele geri dönüyor. Peron duvarındaki çentikler, birinin bunu günlerdir saydığını söylüyor: kırk dakikada bir.
> → Kapılar kapanınca vagon çıt çıkarmadan tünele geri dönüyor. Peron duvarına çentikler atılmış; biri günlerdir saymış: kırk dakikada bir.

> Haklıydın. Bu, beklediğin kadar iyi hissettirmiyor.
> → Haklı çıktın ama haklı çıkmak umduğun kadar iyi gelmiyor.

---

## Çalışma defteri

Bu dosya QA görevinin hafızasıdır: oturum kapanırsa buradan devam edilir. Adımlar ve kurallar kullanıcının görev
tanımındadır (2026-11-07); özet:

- **Kaynak kuralları (2026-11-07 güncellendi: kullanıcı "kaynak kullanımını artırabiliriz, çok yavaşız" dedi —
  bağımsız işler paralel alt ajanlara bölünebilir):** content/
  baştan sona okunmaz, tarama programları yazılır; büyük kaynaklar önce aranır, sonra ilgili bölüm okunur.
- **Adımlar:** 1 otomatik ölçüm → 2 hedefli kod/motor/arayüz incelemesi → 3 hikaye/oynanış örneklemesi (tek alt ajan) →
  4 P0/P1/P2 raporu ve **dur, kullanıcıya sor** → 5 onaydan sonra 5–10 maddelik düzeltme partileri.
- Her adımın sonunda bu dosya güncellenir, commit atılır, kullanıcıya "Adım N bitti, /clear yapabilirsin" denir.

## Durum

| Adım | Durum |
| --- | --- |
| 1 Otomatik ölçüm | **bitti** (2026-11-07) |
| 2 Kod ve motor incelemesi | **bitti** (2026-11-07) |
| 3 Hikaye ve oynanış örneklemesi | **bitti** (2026-11-07) |
| 4 Rapor | **bitti** (2026-11-07) — onay bekleniyor |
| 5 Düzeltme | **bitti** — P0 + tüm P1 (2026-11-07/08); P2'ler açık |

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

## Adım 2 — Kod, motor ve arayüz incelemesi

Okunan yerler: `save/SaveCodec.kt` (decode ve geçişler), `save/SaveManager.kt`, app `data/Storage.kt`,
`StoryEngine.sanitize`, `world/Director.kt` (tamamı), `combat/CombatMath.kt`, `GameViewModel` eylem ve render yolları,
arayüzde erişilebilirlik ve tıklanabilir öğe aramaları. Emülatörde 360dp + yazı ×1,3 ve tablet (800dp) ekran görüntüleri.

### Yeni test: `tools/simulator` `KayitVeBelirlenimTest` (geçti)

- 6 tam oynanışta **her savaş başında** (savaş ortası durum dahil) `SaveCodec.encode → decode` aynı `WorldState`'i veriyor.
- Aynı tohum + aynı tarz aynı oynanışı üretiyor (son, gün, seviye, görülen olaylar, seviye eğrisi).

### Bulgular

**Kayıt**
- Geçişler v1→v2→v3→v4 test edilmiş (`SystemMechanicsTest`, `Asama7MotorTest`); yazma atomik (geçici dosya + rename);
  bozuk kayıt `null` döner, çökmez; daha yeni sürüm kaydı reddedilir. İçerik değişince `sanitize` geçersiz istem, savaş,
  sahne, tohum ve el kartlarını temizliyor.
- **Bozuk/okunamayan otomatik kayıt sessizce kayboluyor:** `load` null döner, başlık ekranında "Devam et" görünmez; Demir
  Mod'da tek kayıt olduğu için oyun tamamen gider. Önceki otomatik kaydın yedeği (`auto.prev.json`) yok; `fsync` yok.
- `sanitize` `dayState.currentEvent`'i denetlemiyor (içerikten silinmiş olayın ortasında kayıt): `sceneId` düzeltilse de
  `currentEvent` eski kimlikte kalır. Düşük risk (yalnız içerik güncellemesinde).
- CLAUDE.md "dosyalar `files/saves/*.yaml`" diyor; gerçekte `*.json` (belge hatası).

**Hikaye Yönetmeni ve tohumlar**
- Puan formülü GDD ile aynı. Ama `arcPriority` **ölü kod**: ana yay olayları `candidates`'tan zaten dışlandığı için çarpan
  hep 1,0.
- GDD "üç ağır savaştan sonra kamp ateşi" diyor; kod **iki** ağır olaydan (gerilim ≥3) sonra nefes veriyor.
- `desiredTension = son + 1 + Dikkat/40`: Dikkat Perde 3–4'te ~90 olduğundan hedef gerilim hep 4'e kilitleniyor
  (Adım 1'deki Dikkat tavanıyla birleşince eğri düzleşiyor; nefes yalnız iki ağır olaydan sonra).
- `seedConditionHolds` tohum koşulunu her gün YAML'dan yeniden ayrıştırıyor (`forced` + `witheredSeeds`); önbellek yok.
- Vadesi gelen ama koşulu tutmayan tohum hemen kuruyor (bekleme yok) — tasarım gereği olabilir, belgede yok.

**Savaş formülleri** — GDD ile uyumlu (inisiyatif, isabet ±ölçek, hasar + zırh sabiti, kritik, kademe ×2); sapmalar
DECISIONS #112 vb. belgeli. Kritik şansına %75, kritik çarpanına ×3,5 tavanı GDD'de yok (zararsız).

**Çökme riski** — Motor istisnaları `GameViewModel` içinde yakalanıyor (`choose` runCatching, `hubAction`/`act`
RuntimeException). `error("Tanımsız …")` çağrıları validateContent'in denetlediği kimliklerde. `render` içindeki
görünüm üreticileri (`engine.view/status/journal…`) yakalanmıyor: bir görünüm istisnası uygulamayı çökertir (düşük
olasılık). Simülatörün `-Xss4m` ayarı gereksiz görünüyor (testler varsayılan yığında 200 oynanış geçiyor).

**Performans (arayüz)**
- **İlk motor erişiminde ana iş parçacığında içerik yükleme: 3,9 sn donma** (Adım 1). `YukselisApplication.engine`
  `lazy`; arka planda önceden ısıtılmalı (`Dispatchers.Default`) ve başlık ekranı hazır olana dek düğmeyi bekletmeli.
- Her eylemde `render` 12 görünüm modelini (günlük, harita, ilişkiler, sıralama, çanta…) ana iş parçacığında yeniden
  kuruyor; çoğu o an görünmüyor. Masaüstü ölçüsüyle eylem başı ms düzeyi; düşük cihazda kare kaçırabilir.
- `collectAsStateWithLifecycle` kullanılıyor; Kotlin 2.2 Compose derleyicisinde güçlü atlama açık — yeniden çizim
  sorunu görülmedi (ölçülmedi; Layout Inspector gerekir).

**Erişilebilirlik ve görünüm** (ekran görüntüleri: emülatör)
- 4 `Icon(contentDescription = null)` — hepsi yanında metin olan süs ikonları, sorun değil.
- `Overlays.kt:128/158` tıklanabilir kartlarda `onClickLabel`/rol yok: TalkBack "dokununca ne olur" söylemiyor.
- **360dp + yazı ×1,3:** alttaki `ActionDock` (3 seçenek) ekranın yarısından fazlasını kaplıyor; hikaye metnine ~4
  satır kalıyor. Dock'a en yüksek yükseklik (ör. ekranın %45'i) + kendi kaydırması gerekli.
- **Başlık ekranı ×1,3:** "Yükseliş / Protokolü" iki satırı üst üste biniyor (ş çengeli ü noktalarına değiyor): satır
  yüksekliği dar.
- **Tablet (800dp):** metin tam genişlikte, satırda ~95 karakter; okunabilirlik düşük. İçerik en çok ~640dp genişlikte
  ortalanmalı; büyük ekranda alt çubuk yerine `NavigationRail` düşünülebilir.
- Ekran yönü değişince (tablet testinde yapılandırma değişti) daktilo efekti baştan başlıyor; metin zaten okunmuşsa
  rahatsız edici (küçük).

## Adım 3 — Hikaye ve oynanış örneklemesi

İki alt ajan (kullanıcı hızlandırmayı onayladı): A açılış dizisi, perde ana düğümleri, simülatörün en nadir/en sık
olayları ve tarama işaretlerini; B sonları, epilog kartlarını ve 8 NPC'den birer yay düğümünü TR+EN okudu
(`tools/qa/olay.py`). Ham raporlar: **docs/qa/hikaye_a.md** (P0 0 · P1 13 · P2 22) ve **docs/qa/hikaye_b.md**
(P0 0 · P1 6). Örtüşenler Adım 4'te birleştirildi. Ana bulgular ana koordinatörce doğrulandı: çift stat öneki (motor
`insightPrefix` + 95 metin "[XXX]" ile başlıyor), Kademe Kırma `seviye_min: 14` (Gün 24; Gün 30 p90 = 9), Seviye 10
sınıf teklifi GDD'de Perde 1'de ama Gün 30 p90 = 9, `bekci_deniz_hat` 3. paragraf koşulsuz.

**Genel yargı:** İskelet, motifler ve sonlar güçlü; EN yerelleştirme çok iyi; NPC sesleri iki dilde tutarlı. Zayıf
yanlar: Perde 2–4 Türkçesi kılavuzdan geçmemiş, mekanik/metin uyuşmazlıkları (önek, erişilemeyen seçenek/eşik,
önceden ilan edilen ölüm, karşılıksız kronik vaatleri), hissedilen hakimiyet sayılardaki hakimiyetin gerisinde,
Perde 4 kendini tekrarlıyor.

**Savaş ve ilerleme (simülatörle):** Oynanış başına ortalama **519 savaş**; yalnız %10,7'si Ezme, %6,4'ünde oyuncu
yere düşüyor (yazılmış yenilgi/bayılma), İkinci Şans hiç kullanılmıyor, ölüm sıfır. Yani savaşlar ne tehlikeli ne de
ezici: uzun bir orta yol — grind riski. Mücadele evresi Gün ~1–60 (Gün 30'da oyuncu dünyanın 0,4 katı), Gün 90'dan
sonra p50 hep önde; Perde 2+ düşmanları dünya seviyesine ölçeklendiğinden oyuncu dünyanın ancak 1,1–1,5 katında
kalıyor, ezme hissi gelmiyor. İmkânsız bölüm yok (çıkmaz 0, tüm sonlar erişilebilir); sıkıcı bölüm riski Perde 3
(Dikkat tavanda, Yönetmen gerilimi 4'e kilitli) ve Perde 4 (11 zorunlu düğüm).

## Adım 5 — Düzeltme günlüğü

Onay (2026-11-07): **P0 + tüm P1.** Plan: (1) kod/arayüz/küçük içerik, (2) denge P1-9..13 simülatörle,
(3) yapısal hikaye P1-6, P1-7, P1-8, P1-14, (4) Perde 2–4 Türkçe üslup partileri (P1-15) — aynı geçişte kronik doğruluğu
(P1-5) ve dil hataları (P1-16, P1-17); ortak brif `docs/qa/uslup_brief.md`, uslup aracı `--kronik-en` ile.

**Parti 1 (kod + küçük içerik):**
- P0-1: İçerik `GameViewModel.warmUp()` ile ilk kare çizildikten sonra `Dispatchers.Default`'ta yükleniyor; başlık
  düğmeleri hazır olana dek kapalı ("Dünya yükleniyor…"). Emülatörde "Devam et" sonrası atlanan kare: 222 → 0. (Açılıştaki
  ~60 karelik ilk kare maliyeti debug Compose'un kendisi; yüklemeyle ilgisiz.)
- P1-1: 95 sezgiden "[ALG]/[PER]…" önekleri kaldırıldı; validator stat koşullu sezgide stat önekini hata sayıyor.
- P1-2: İtaat eşiği `sadakat ≥6 ya da bağlılık ≥ OBEY_BOND (40)`; test `korkak ama baglanmis yoldas itaat eder`.
- P1-3: Kademe Kırma `seviye_min` 14 → 6 (kayıp yazılmış yenilgi sahnesine gider).
- P1-4: `bekci_deniz_hat` ölüm ilanı belirsizleştirildi (TR+EN).
- P1-16 (Perde 1 kısmı): `bekci_cokus_deniz›yanki` etiketi düzeltildi; kalanlar üslup partilerinde.
- P1-18: `deniz_son_secim_kalir_yarali` EN cümlesi onarıldı.
- P1-19: `ActionDock` en yüksek yükseklik %50 → %38.
- P1-20: `SaveManager.autosave` önceki sağlam kaydı `auto.prev`'e döndürüyor; otomatik kayıt bozuksa ondan açılıyor;
  test `bozukOtomatikKayitOncekindenAcilir`.
- Not: Parti 1 (üslup) commit'inde bir test (`yeniOyunBaslangicSahnesindeBaslar`, prolog etiket metni) kırık kalmıştı —
  çıktı süzgecim kaçırdı; beklenen metin güncellendi.

**Parti 2 (denge; simülatör 1000 oynanış, önce → sonra):**

| Ölçü | Önce | Sonra |
| --- | --- | --- |
| Savaş / oynanış | 520 | 389 |
| Ezme payı | %10,4 | %26,9 |
| Hakimiyet payı | %81,3 | %79,0 |
| Gün 30 seviye p50 / p90 (Sv 10 sınıf teklifi) | 8 / 9 (~%10) | 9 / 11 (~%35) |
| Dikkat p50 Gün 210 / 240 / 270 | 91 / 94 / 97 | 69–78 bandı |
| Koruyucu tarzı Kadim istilası zaferi | %34 | %44 |
| Yalnız Avcı payı (en sık son) | %22,1 | %11,4 (en sık artık Kod Kırıcı %17,4) |
| Son aralığı | %5,7–%22,1 | %6,1–%18,4 |

- P1-9: Savaş zaferi XP'si Perde 1'de ×2 (`EffectApplier.ACT1_XP_FACTOR`). Gün 30'da oyuncuların ~üçte biri Sv 10'a
  ulaşıyor (önce ~%10). Tamamına çıkarmak Mücadele evresini fazla kısaltır; GDD Perde 1 tablosundaki "Seviye 10 sınıf
  teklifi" çoğu oyuncu için Perde 2 başına kayıyor — tasarım kararı olarak kabul edildi (kısmi).
- P1-10: Perde 2+ keşif düşmanları dünya seviyesinin ×0,8'i (`REGION_LEVEL_FACTOR`); kapılar ×1,15 kaldı.
- P1-11: Keşifte savaş tabanı 0,45 → 0,32 (`EXPLORE_FIGHT_CHANCE`); kayıp XP Perde 2+ zafer XP'si ×1,5 ile dengelendi.
  Hikaye/boss savaşları sertleştirilmedi (yere düşme %8,2; İkinci Şans hâlâ hiç kullanılmıyor) — açık kalan not.
- P1-12: Gece Dikkat sönümü `2 + Dikkat/35`; `desiredTension`'da Dikkat en çok +1; nefes GDD'deki gibi üç ağır olaydan
  sonra (test `uc agir olaydan sonra nefes`).
- P1-13: Kadim istilası valisi iki seviye aşağı; Yalnız Avcı'ya `bayrak: uye_yok` (fraksiyonsuz yalnız kurt; GDD ve
  PERDE2_4 tablosu güncellendi). Koruyucu tarzı hedefe %42 (önce %47) — sınır, finali kazanmak; kasıtlı zorluk.
- Gün 330'da oyuncu dünyanın ×0,9'u (önce ×1,09): seyrek savaşla geç oyun biraz yavaşladı; Gün 365 p50 seviye 1.239
  (önce 1.272). Hakimiyet hedefi (%60+) korunuyor.

**Parti 3 (yapısal hikaye) ve Parti 4 (Perde 2–4 Türkçe üslup + kronik doğruluğu):**
- P1-6/7/8: Vassal sonu Thessar maddelerini (örnek maddesi, B tavanı, Defter Bekçisi) hatırlıyor; Döngü sonu oyuncunun
  önceki yılını ayrı satır sayıyor; Yükseliş'te Deniz paragrafları çakışmıyor.
- P1-14: `p4_son_gece_ates` Perde 3 son gecesini yakınlarla geçirene gelmez (`p3_son_gece_yakinlar`); Perde 4 veda
  sahnelerinde yoldaş satırları ve "bir yıl önce" geri çağrısı çeşitlendi.
- P1-15/5/16/17: Perde 2–4'ün bütün olay dosyaları + sonlar + endings + eşik/bildirim/sistem + Kule (~13.000 `tr`)
  `docs/qa/uslup_brief.md` ile 33 yazar diliminde yeniden yazıldı; her dosyayı bağımsız bir okuyucu baştan sona okudu
  (`docs/qa/okuma_brief.md`), 199 bulgu süzülüp uygulandı. Kronikler yalnız kesinleşeni anlatıyor (TR+EN birlikte);
  "İhanet gecesi…/On the night of…" gibi tekrarlı iskeletler kırıldı; "yıllardır", "Hasatın", belirteç dışı ekler
  (`{yer:x|de}ki`), çift olumsuzluk vb. dil hataları giderildi. Ek içerik düzeltmeleri: Tepe Mahallesi belirteçle,
  sahtekâr Vahit çakışması, `kcam_sil` Yankı koşulu, karantina gemisi oğlan satırı, Koruyucu'da Deniz "başka hiçbir
  şey" çelişkisi (TR+EN).
- Kalan üslup işi: USLUP Parti 9–10 (NPC, eşya, yetenek, düşman, sistem metinleri; ansiklopedik). QA P1 kapsamı dışında.

### Tarama ve ölçüm: önce (Adım 1) → sonra (Adım 5 sonu)

| Kategori | Önce | Sonra | Not |
| --- | ---: | ---: | --- |
| Eksik/boş çeviri | 0 | 0 | |
| TR/EN uzunluk oranı aşırı | 0 | 0 | |
| Düz yazılmış NPC adı | 35 | 35 | P2-13 (awakenings, enemies, knowledge, system) |
| İngilizcede metrik birim | 0 | 0 | |
| 3 cümleyi aşan paragraf | 1.777 | 1.792 | üslup ulaçla bağlayınca bazı paragraflarda cümle sayısı arttı; P2-20 |
| 60 karakteri aşan seçenek | 31 | 7 | önce sayısı belirteçleri ham saydı; düzeltilmiş araçla önce ~5 |
| Birebir aynı metin (TR) | 28 | 25 | |
| Neredeyse aynı metin (TR) | 18 | 13 | |
| Konulup hiç okunmayan bayrak | 352 | 352 | P1-5 kroniklerin dürüstleştirilmesiyle kapandı; ölü bayraklar P2 |
| Kilitli seçeneği olmayan olay | 48 | 48 | |
| Üslup: noktalı virgül >1 / 3 -yor / kısa cümle dizisi | 216 / 303 / 1.616 | 15 / 62 / 1.262 | 19.254 `tr` |
| Üslup: "Bu, " / ", aynı anda" / "bir şekilde" / "fark ediyorsun" | 24 / 12 / 11 / 11 | 1 / 2 / 2 / 2 | |
| Hiç görülmeyen olay (simülatör) | 5 (esik_itaat dahil) | 4 (yalnız beklenenler) | P1-2 |
| Savaş / oynanış · Ezme · Hakimiyet | 520 · %10,4 · %81 | 389 · %26,8 · %79 | |
| Son aralığı | %5,7–%22,1 | %6,3–%18,5 | |
| İlk "Devam et" ana iş parçacığı donması | 3,9 sn (222 kare) | 0 kare | P0-1 |

**(Geçmiş) Duraklatma notu — tamamlandı:**
- Çalışma kopyasında COMMIT EDİLMEMİŞ: dalga 1 üslup+kronik uygulaması (50, 51, 52, 53, 54, 56; 3.665 tr, uslup `--kronik-en` 0 hata,
  validateContent temiz) + `80_perde4` `p4_son_gece_ates` koşulu + `flags/71_perde3.yaml` `p3_son_gece_yakinlar`.
  Eski hâl HEAD'de (okuma denetimi `tools/qa/show.py` HEAD'le karşılaştırır) — denetim bitmeden commit etme.
- Okuma denetimi bitenler: `tools/qa/is/R_51.json` (11), `R_52.json` (9), `R_56.json` (9) — süzüp `tools/qa/uslup_apply.py apply` ile uygula.
  Yarım kalan/yeniden başlatılacak: R_50, R_53, R_54 (`docs/qa/okuma_brief.md`).
- Sonra commit; ardından dalga 2 (60, 61, 70, 71+tower, 72; dilimler `tools/qa/dilim.py 6500 …`) ve dalga 3 (55, 80, 81,
  endings.yaml, 90_esikler+95+story/90_sistem) yazarları `docs/qa/uslup_brief.md` ile; her dalgaya okuma denetimi.
- Dalga 2 uygulandıktan sonra P1-14'ü bitir: `71_perde3` `perde3_son` › `yakinlar` seçeneğine `bayrak: "p3_son_gece_yakinlar"`.
- Açık P1'ler: P1-5, P1-15, P1-16, P1-17 (üslup dalgalarıyla), P1-14 (yukarıdaki adım). Sonda tarama önce/sonra tablosu.
- Not: eşzamanlı alt ajan sınırı 20.

**Parti 5 (P2, 2026-11-08):**
- Arayüz: P2-1 içerik en çok 680dp (tablet); P2-2 başlık satır yüksekliği; P2-3 tıklanabilir kartlarda `onClickLabel` +
  rol; P2-4 daktilo durumu `rememberSaveable`.
- Kod: P2-5 ölçüldü — `render` masaüstünde 0,32 ms, değişiklik gerekmedi; P2-7 ölü `arcPriority` kaldırıldı; P2-8 tohum
  koşulu önbelleği; P2-9 `sanitize` silinmiş olayı temizler (test); P2-10 `render` istisnası çökertmez; P2-11 `herhangi`
  (VEYA) koşulu (test) ve 12 kopya paragraf birleştirildi (71); uyanış satırları satır başı `kosul` alabiliyor
  (`son_nefes` Yankı satırı artık yalnız bağ varken).
- Lint (P2-6): 29 → 2 uyarı (kalan: `mipmap-anydpi-v26` — uyarlanabilir simge için gerekli; `OldTargetApi` — yayın
  kararı). P2-12: CLAUDE.md kayıt uzantısı, simülatörde `-Xss4m` yok.
- İçerik (iki ajan + düzeltmeler): P2-13 düz Yankı adı belirtece (awakenings, knowledge, system; tarama 35 → 21, kalanı
  "deniz/ash" gibi yanlış pozitif ve belirteç çözülmeyen `sira_gizli`); P2-14 EN'de 117, TR'de 104 düz yer adı belirtece
  (cümle başı "The Esplanade" ve "Kule Dibi'ne" gibi bileşik ekler — motor desteği olmadığından — düz kaldı); P2-15
  terimler (Grade yazımı, carrier, Yankı'da küçük "sistem", dünya sayısı kanonu DECISIONS #129); P2-16 Demir Mod ölüm
  sonu Dikkat'e göre iki sürüm; P2-17 Kod Kırıcı ölü paragrafı, Demir Taht Kadim dalı, üç sona Yankı'nın son sözü,
  arketip sırası (Demir Taht → Tiran, Vassal → Kurtarıcı/pazarlık); P2-19 prolog yabancısı, `gemi_bakis` koşulu
  (`kule_cephe_tutuldu`), EN pürüzleri; okuyucu notları: 55 Kaya/Elif paragrafı, 60 taşlı çanta, 60 oğlanın repliği.
  9 ölü bayrak sonlara/epiloğa bağlandı (352 → 343).
- P2-21: `olay.py` bildirim koşullarını gösteriyor; taramaya "kronik vaadi (sezgisel)" (23 aday, çoğu anlık sonuç) ve
  "dışlayan koşullu kopya" (7 → 0) kuralları.
- Açık: P2-18 (epilog seçicisinde aynı açılışlı kartları yan yana koymama — metinler çeşitlendirildi, kod kuralı yok),
  P2-20 (3 cümleyi aşan paragraf; kılavuzun ulaçla bağlama tercihiyle çelişiyor, kabul), 343 ölü bayrak, USLUP Parti 9–10.

## Sıradaki iş (plan, 2026-11-07)

**P1-21 · Günlük Görev: savaş/Kapı/keşif görevleri bir günde yapılamıyor.** `content/daily.yaml` geniş şablonlar
(`gunluk_genis_gorevler`, gün döngüsüne girişte açılır) 3–4 adım istiyor; gün başına 2–4 eylem hakkı var
(`Stats.kt` MIN_SLOTS/MAX_SLOTS), keşifte savaş şansı ~%32 (`WorldEngine` EXPLORE_FIGHT_CHANCE, P1-11 düşürdü),
Kapı şansı 0.15–0.35. Kaçırılan görev seriyi sıfırlıyor ve Ceza Bölgesi riski getiriyor (`Effects.kt` ~473–481).
1. Adım sayısını günün eylem hakkına bağla (`newDaily`: adim ≤ hak; savas/kapi 1–2, kesif 2'yi geçmesin).
2. Hikaye savaşları ve bosslar dahil kazanılan her savaş `savas` sayılsın; Kapı keşfi/temizliği ikisi de `kapi`.
3. O gün ulaşılamayan görevde (ör. bölgede Kapı yok, hak bitti) ceza verme; seri de kopmasın.
4. Antrenman ipucunu görünür yap: gün döngüsünde Günlük Görev panelinden "Serbest zaman → Antrenman"a kısayol.
5. Test (her şablon tek günde yapılabilir; ulaşılamayan görevde ceza yok) + simülatörde günlük tamamlama oranı
   (rapora satır ekle; hedef ≥ %70) → check.sh → commit.

Diğer açıklar: P2-18, P2-20 (kabul), 343 ölü bayrak, imzalı AAB (anahtar kullanıcıda).
