# Hikaye akışı: teşhis (Hikaye yapısı değişikliği, Adım 1)

Amaç: hikayenin "oradan oraya atlaması"nı ölçmek. Ölçüm, bölüm yapısına geçişten önceki durumdur; Adım 2 sonunda aynı
ölçüm tekrarlanır ve öncesi/sonrası tablosu buraya eklenir.

## Ölçüm düzeni

- **Kayıt:** `./gradlew :tools:simulator:akis --args="20"` → `tools/qa/akis/iz/NN.jsonl` (git dışında). Simülatörün nüfus
  karışımıyla 20 oynanış yapılır: 6 rastgele, 14 hedefli tarz, 2'si Demir Mod. Her adımda şunlar kaydedilir:
  - olay başlangıcı: gün, perde, bölge, olay, **neden**, Yönetmen puanı (kartlarda), hedef ve olay gerilimi, önceki olay,
    arada kaç akış eylemi olduğu, savaştan hemen sonra mı geldiği, aktif görevler, NPC'ler.
  - **Neden** şunlardan biridir: `kart` (fırsat eli), `kesif`, `tohum`, `npc_esik`, `dikkat_esik`, `bildirim`, `ana_yay`, `kule`.
  - Ayrıca bölge değişimi (hangi eylemle), savaş başı/sonu ve gün değişimi.
- **Envanter:** `python tools/qa/akis/envanter.py` → `envanter.json`. Her olayın perde, bölge, gün, ana yay, tohum, giriş yeri,
  görevleri ve ektiği tohumlar.
- **Çözümleme:** `python tools/qa/akis/cozumle.py --json tools/qa/akis/once.json`.
- **Görev penceresi:** bir görevin (quests.yaml) iki ardışık olayı arasında en çok 3 gün varsa, aradaki süre "görev sürüyor"
  sayılır.
- **Ölçeğe dikkat:** 20 oynanışta 4.638 olay başlangıcı var (oynanış başına ~232, tekil ~200). Ölümden sonra kontrol noktasına
  dönülünce aynı olaylar yeniden başlar; en çok Kule hikaye katları tekrarlanır. Bu yüzden sayılar bir miktar şişkindir; oranlar
  değişmez.

## Sonuçlar (öncesi)

| | Sorun | 20 oynanış | Oynanış başına |
| --- | --- | ---: | ---: |
| a | Görevin ortasında ilgisiz olay | 332 | 16,6 |
| b | Konum uyuşmazlığı / geçişsiz seyahat | 1.490 | 74,5 |
| c | Gergin anın ortasına düşen tohum/NPC olayı | 370 | 18,5 |
| d | Görevi kesen gün bazlı ana hikaye olayı | 39 | 1,9 |
| e | Bağlamsız geçiş | 574 | 28,7 |

**a) Görev ortasında ilgisiz olay: 332.** Kaynakları: fırsat eli kartları 142, tohumlar 120, Dikkat eşikleri 20, Kule 19,
NPC eşikleri 18, keşif 13.
*Örnek (oynanış 0, Gün 18):* `istasyon_sinyali` görevi sürerken (Yankı'nın sinyali, Kırık İstasyon) `cete_hesabi` tohumu
geldi. Aynı gün içinde ormanda iki kart daha açıldı: `zehirli_bereket` ve `isaretli_agaclar`.

**b) Konum uyuşmazlığı ve geçişsiz seyahat: 1.490.** Bunun 1.035'i bölgesiz bir olayın giriş sahnesinin başka bir yerde
geçmesi, 103'ü bölgeli bir olayın girişinin başka yerde olması, 352'si de bölgenin bir olay etkisiyle (`bolge`) yolculuk
sahnesi olmadan değişmesi. Uyuşmazlığın nedene göre dağılımı: ana yay 513, tohum 477, kart 137.
*Örnek (oynanış 0, Gün 12):* oyuncu Kırık İstasyon'dayken `kirilma` ana yay olayı ormanda açıldı; olay bitince bölge
yolculuksuz `ogretici_orman`'a geçti. Gün 33'te ise `kaya_lonca_teklifi` oyuncuyu Liman'dan Sanayi'ye "ışınladı".

**c) Gergin anın ortasına düşen olay: 370.** Hepsi araya oyuncu eylemi girmeden, gerilimi 3–4 olan bir olayın hemen ardından
geldi: tohum 225, Dikkat eşiği 113, NPC eşiği 32.
*Örnek (oynanış 0, Gün 13):* `aras_sira_meydan` (gerilim 4, Aras'la meydan düellosu) biter bitmez `dikkat_25` (avcılar
seni fark etti) açıldı.

**d) Görevi kesen gün bazlı ana hikaye: 39.** En sık görülenler: `kirilma` (Gün 12) 8, `elif_baris_masasi` (Gün 333) 6,
`siginak_3_tahliye` (Gün 205) 5, `yukselenler_3_ayin` (Gün 220) 5.
*Örnek (oynanış 1, Gün 12):* `olu_avcinin_mesaji` görevi sürerken Kırılma ana yayı vadesi geldiği için zorla başladı.

**e) Bağlamsız geçiş: 574.** Bir olay bitince oyuncu eylemi olmadan ortak NPC'si, tohum bağı ya da görev bağı olmayan
başka bir olaya geçildi: tohum 321, Dikkat eşiği 130, ana yay 69, NPC eşiği 54.
*Örnek (oynanış 0, Gün 31):* `ogretici_bekci` → `perde2_donus` → `dikkat_50`. Perde 2'nin açılış sahnesi biter bitmez, iki
perde öncesinden kalan Dikkat eşiği olayı geldi.

## En çok soruna yol açan 3 neden

1. **Olayların yeri yok.**
   - 255 olayın 178'i bölgesiz. Bunların 105'inin giriş sahnesi belirli bir yerde geçiyor; 47 ana yay düğümünün hepsi de
     bölgesiz. Tohum olayları ayrıca bölge ve gün de tanımıyor (CLAUDE.md kuralı).
   - Sonuç: yerinde olmayan olay her yerde açılıyor. Olay bölgeyi değiştirince de oyuncu yolculuk sahnesi görmeden başka
     yere geçiyor (`bolge` etkisi).
   - Payı: **b**'nin tamamı ve **e**'nin bir kısmı.
2. **Zorunlu olaylar her akış girişinde, hiçbir bekçi olmadan geliyor.**
   - `WorldEngine.enterHub` her olaydan, savaştan ve yuvadan sonra `Director.forced`'a bakıyor. Vadesi gelmiş tohum, eşik ya
     da ana yay varsa onu hemen başlatıyor.
   - "Üs mü, görev mi, az önce doruk mu yaşandı" ayrımı yok; aynı anda birden çok tohumun vadesi geldiyse arka arkaya diziliyor.
   - Dikkat ve Yozlaşma eşikleri, eşiğin aşıldığı günle aynı güne tohumlanıyor: savaşın ya da doruğun içinde aşılıyor,
     hemen ardından açılıyor.
   - Payı: **c**'nin tamamı, **e**'nin büyük kısmı (tohum + eşik 505/574) ve **a**'nın yarısı.
3. **Görevler kilitsiz, ana hikaye takvime bağlı.**
   - Görev bir bayrak/durum kümesinden ibaret ve içindeyken Yönetmen fırsat eline her şeyi koyabiliyor (a: kart 142).
   - Ana yay düğümleri `ana_yay.gun` geldiği an "ilk fırsatta kesin" geliyor ve yürüyen görevi kesiyor.
   - Fraksiyon zincirlerinin düğümleri de (`siginak_3_tahliye`, `yukselenler_3_ayin`) gün bazlı.
   - Payı: **a**, **d**.

Ek gözlemler:
- Gerilim eğrisi (`desiredTension`) yalnız havuz kartlarını puanlıyor. Zorunlu olaylar eğriyi bozuyor; c'deki 370 olayın
  hepsi gerilim ≥ 3 olayın ardından geldi.
- Yolculuk eylemi tek satırlık bir bildirim (`yolculuk`); varış sahnesi yok. Ana yay olayları kendi açılışlarında yolculuğu
  anlatıyor, ama oyuncunun o an nerede olduğuna bakmıyor.

## Öncesi / sonrası

Ölçüm `tools/qa/akis/iz` (önce) ve `tools/qa/akis/iz_sonra` (sonra); her iki kayıt aynı 20 tohumla, aynı nüfus
karışımıyla üretildi. Çözümleme `python tools/qa/akis/cozumle.py <klasör>`.

| | Sorun | Önce | Sonra |
| --- | --- | ---: | ---: |
| a | Görevin içinde göreve ait olmayan olay | — (görev yoktu) | **0** |
| b | Konum uyuşmazlığı / geçişsiz seyahat | 1.490 | **0** |
| c | Gergin anın ortasına düşen tohum/NPC olayı | 370 | **0** |
| d | Görevin içinde göreve ait olmayan ana yay olayı | — (görev yoktu) | **0** |
| e | Bağlamsız geçiş | 574 | **0** |

Görev penceresi ölçümü (eski a ve d, quests.yaml görevlerinin iki olayı arasında en çok 3 gün):

| | Önce | Sonra |
| --- | ---: | ---: |
| a | 332 | 109 |
| d | 39 | 19 |

Yeni yapıda kalan değerler kesinti değil. Fraksiyon zincirleri artık ayrı yan görevler; iki zincir olayı arasındaki
günlerde üste olay olması tasarım gereği (Mass Effect 2 döngüsü). Görev içinde kesinti ölçen tanım (a ve d) ikisi için de
0 veriyor.

Bağımsız akış kontrolünün (ikinci ajan) bulduğu ve düzeltilen sorunlar: aynı gün tekrarlanan varış (725 → 0), "üsse
dön"ün başka bölgeye yan göreve atması (121 → 0), ana yay düğümünün tohumla 60 gün erken gelmesi (8/20 oynanış → 0:
`kaya_darbe_gecesi` artık Gün 292–293), tarihli operasyonda oyuncunun elinde olmayan taşma cezası (10 → 0), olay oyuncuyu
taşıdıktan sonra gelen varış sahnesi, eski ana yay darboğazlarının asıl günleri (`darbogaz_gun` 45/95/225). Açık kalan:
Perde 2–3'ün geç günlerinde havuz olayları tükendiği için serbest günler çoğunlukla keşif, Kapı ve Kule (olay değil savaş)
ile geçiyor; içerik hacmi işi, yapı işi değil.

Notlar:
- **Olay sayısı:** oynanış başına ~560 olay başlangıcı var, bunun ~320'si varış sahnesi. Görevin ilk sahnesi varıştır; ayrı
  olay sayılmaz. Tekil olay ortalaması 241 (önce ~200): üste ve görevde olaylar daha düzenli tükeniyor.
- **b:** konum kuralı (her olayın `konum`u), olayın `bolge` etkisinin görev başlatması ve bölüm açılırken üs bölgesine
  geçiş ile kapandı.
- **c, e:** zorunlu olay kilidi (DECISIONS #133), son tarih tetiklemesinin oyuncu eylemini beklemesi ve brifingin üsten
  ayrılan eylemden önce açılması ile kapandı.
  Operasyon kuyruğunun ardışık olayları aynı görevin parçası sayılır.

### Denge (1000 oynanış, `docs/BALANCE.md`)

| Ölçüt | Önce | Sonra |
| --- | ---: | ---: |
| Koruyucu / Demir Taht / Yükseliş / Kod Kırıcı | %12,7 / 7,2 / 6,3 / 17,2 | %10,3 / 7,9 / 6,7 / 20,7 |
| Özgürlük / Vasal / Yalnız Avcı / Hasat / Döngü | %7,7 / 12,6 / 11,2 / 18,5 / 6,6 | %7,1 / 12,8 / 9,4 / 19,1 / 6,0 |
| Hakimiyet payı | %78,9 | %63,4 (sınır %60) |
| Ezme payı | %26,8 | %22,0 |
| Oynanış başına savaş | 389 | 304 |
| Kule 100. kata ulaşan | %91,3 | %59,2 |
| Bitmeyen oynanış | 0 | 0 |

Her son ulaşılabilir, dağılım en çok 3,5 puan kaydı (Kod Kırıcı). Hakimiyet sınırın üstünde, ama payı daraldı. Nedeni, üste geçen
günlerin savaşsız olması ve her görevin bir yolculuk yuvası harcaması. Hakimiyet %60'ın altına düşerse önce simülatörün
görev süresine (`Simulator.MISSION_DAYS`) ve keşif savaş olasılığına bakılmalı.

## Boş serbest günler

Ölçüm: `./gradlew :tools:simulator:akis --args="20 tools/qa/akis/iz_sonra"` ve `python tools/qa/akis/serbest.py tools/qa/akis/iz_sonra --md`
(20 oynanış; kayıttaki `gunozet` satırları).

- **Serbest gün:** bölümün brifing, hazırlık ya da bitti evresinde geçen gün. Operasyon ve sonrası sayılmaz.
- **Havuzun tükendiği gün:** bölümün açılışından sayılır. O günden bölüm sonuna kadar üste fırsat eli hep boştur.
  Medyan, yalnız havuzu tükenen oynanışlardan alınır.
- **Salt savaş günü:** en az bir savaş ve hiç olay yok.
- **Ne olay ne savaş:** yalnız dinlenme, antrenman, yolculuk ya da kamp.

### Öncesi (zaman atlama yokken)

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 10.2 | 11 | 3/20 | %10 | %11 |
| 2 | 10.6 | 8 | 15/20 | %27 | %10 |
| 3 | 3.0 | 0 | 20/20 | %30 | %15 |
| 4 | 9.8 | 9 | 3/20 | %22 | %10 |
| 5 | 18.9 | 17 | 11/20 | %31 | %14 |
| 6 | 13.4 | 12 | 16/20 | %39 | %29 |
| 7 | 23.6 | 15 | 20/20 | %23 | %57 |
| 8 | 26.1 | 0 | 20/20 | %31 | %56 |
| 9 | 30.1 | 0 | 20/20 | %61 | %30 |
| 10 | 22.4 | 24 | 4/20 | %33 | %14 |
| 11 | 17.0 | 21 | 17/20 | %34 | %39 |
| 12 | 29.9 | 20 | 18/20 | %47 | %34 |
| 13 | 31.7 | 0 | 17/20 | %59 | %31 |
| 14 | 12.7 | 10 | 19/20 | %41 | %46 |
| 15 | 24.9 | 0 | 20/20 | %45 | %51 |
| 16 | 11.8 | 9 | 3/20 | %23 | %13 |
| 17 | 7.5 | 6 | 16/20 | %31 | %11 |
| 18 | 4.9 | 0 | 18/20 | %32 | %38 |

Salt savaş payı %35'i aşan bölümler: 6, 9, 12, 13, 14, 15. Havuz en erken Bölüm 3, 8, 9, 13, 15 ve 18'de tükeniyor
(açılış günü). Bu bölümlerin olayları ya operasyona bağlı ya da önceki bölümde tükenmiş.

### Sonrası (zaman atlama; DECISIONS #142)

"Atlanan gün": atlamayla geçen ve içinde olay ya da savaş olmayan günler. Havuzun tükendiği gün hesaplanırken atlanan
günler çıkarılır. Ölçüm iki simülatör davranışıyla yapıldı.

**Gerçekçi kullanım (simülatörün kalıcı ayarı):** üste el boşken %10 olasılıkla 1–3 gün atlanır. Denge testi (hakimiyet
%60+) bu ayarla geçiyor: 200 oynanışta atlamasız %62, bu ayarla %61.

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 11.3 | 10 | 3/20 | %15 | %9 | %0 |
| 2 | 11.2 | 9 | 14/20 | %28 | %10 | %2 |
| 3 | 3.0 | 0 | 20/20 | %20 | %33 | %2 |
| 4 | 9.7 | 12 | 4/20 | %15 | %8 | %0 |
| 5 | 17.4 | 19 | 14/20 | %28 | %12 | %1 |
| 6 | 13.7 | 12 | 12/20 | %39 | %26 | %2 |
| 7 | 23.9 | 16 | 20/20 | %15 | %47 | %16 |
| 8 | 25.9 | 16 | 20/20 | %27 | %42 | %18 |
| 9 | 30.0 | 0 | 19/20 | %50 | %27 | %9 |
| 10 | 23.1 | 24 | 6/20 | %27 | %18 | %0 |
| 11 | 21.1 | 20 | 19/20 | %33 | %32 | %9 |
| 12 | 30.0 | 14 | 20/20 | %42 | %32 | %6 |
| 13 | 31.8 | 0 | 19/20 | %49 | %30 | %6 |
| 14 | 12.7 | 10 | 16/20 | %38 | %36 | %11 |
| 15 | 25.1 | 0 | 20/20 | %37 | %38 | %18 |
| 16 | 11.8 | 10 | 4/20 | %19 | %14 | %0 |
| 17 | 7.9 | 4 | 15/20 | %28 | %12 | %3 |
| 18 | 4.7 | 0 | 18/20 | %34 | %34 | %4 |

Salt savaş payı %35'i aşan bölümler: 6, 9, 12, 13, 14, 15. Bu öncesiyle aynı liste.

**Yoğun kullanım (deneme):** el boşken %50 olasılıkla 1–5 gün atlanır. O zamanki kazanç sabit 20 XP/gündü.

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 11.2 | 10 | 4/20 | %10 | %8 | %11 |
| 2 | 11.2 | 9 | 6/20 | %25 | %5 | %11 |
| 3 | 3.0 | 1 | 17/20 | %30 | %8 | %18 |
| 4 | 9.8 | 15 | 1/20 | %18 | %7 | %2 |
| 5 | 18.3 | 18 | 8/20 | %25 | %11 | %5 |
| 6 | 13.6 | 11 | 12/20 | %31 | %22 | %12 |
| 7 | 23.9 | 7 | 17/20 | %12 | %19 | %51 |
| 8 | 26.2 | 15 | 18/20 | %18 | %15 | %54 |
| 9 | 30.1 | 1 | 19/20 | %33 | %15 | %44 |
| 10 | 22.8 | 24 | 2/20 | %35 | %11 | %2 |
| 11 | 20.1 | 19 | 13/20 | %23 | %21 | %27 |
| 12 | 30.0 | 18 | 19/20 | %39 | %14 | %28 |
| 13 | 31.6 | 0 | 18/20 | %37 | %18 | %35 |
| 14 | 12.4 | 10 | 7/20 | %23 | %21 | %40 |
| 15 | 25.0 | 0 | 19/20 | %23 | %17 | %55 |
| 16 | 12.1 | 10 | 4/20 | %24 | %11 | %4 |
| 17 | 7.9 | 6 | 15/20 | %25 | %10 | %6 |
| 18 | 4.5 | 2 | 19/20 | %36 | %12 | %14 |

Bu ayarla %35'i aşanlar 10, 12, 13, 18'e iner. Ama hakimiyet %43'e düşer: atlanan gün normal bir günden az güç verir,
oyuncu dünya seviyesinin gerisinde kalır. Kazanç yüzdeli XP'ye (Perde 1/2/3+: %25/%50/%100) çıkınca bile %20 atlamada
%58 oldu.

**Sonuç:** zaman atlama boş günü kısaltan bir kolaylık. Açığı kapatmaz; kapatırsa güç eğrisini bozar. Açık yeni olaylarla
kapanmalı. Öncelik salt savaş payı %35'i aşan 6, 9, 12, 13, 14, 15. Ardından 7 ve 8 gelir: havuzları her oynanışta
tükeniyor, "ne olay ne savaş" payları %40'ın üstünde.

### Sonrası (yeni üs olayları; DECISIONS #143)

Kullanıcının onayladığı 23 olay ve yazım sırasında eklenen 12 takip düğümü yazıldı (`events/57_serbest_p2.yaml`,
`73_serbest_p3a.yaml`, `74_serbest_p3b.yaml`): Bölüm 6, 9, 12, 13, 14 ve 15'te yoldaş/NPC sahneleri ve yan zincirler.
Hepsi üste geçiyor. Bir oynanışta ortalama 21'i oynanıyor (en az 15, en çok 28).

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 11.3 | 10 | 3/20 | %15 | %9 | %0 |
| 2 | 11.2 | 9 | 14/20 | %28 | %10 | %2 |
| 3 | 3.0 | 0 | 20/20 | %20 | %33 | %2 |
| 4 | 9.7 | 12 | 4/20 | %15 | %8 | %0 |
| 5 | 17.4 | 19 | 14/20 | %28 | %12 | %1 |
| 6 | 13.8 | 28 | 10/20 | %36 | %24 | %2 |
| 7 | 23.6 | 15 | 20/20 | %19 | %46 | %14 |
| 8 | 25.4 | 16 | 20/20 | %28 | %39 | %21 |
| 9 | 30.0 | 7 | 20/20 | %52 | %20 | %4 |
| 10 | 22.1 | 23 | 5/20 | %30 | %15 | %1 |
| 11 | 20.7 | 21 | 18/20 | %39 | %28 | %6 |
| 12 | 29.9 | 18 | 19/20 | %44 | %26 | %4 |
| 13 | 31.8 | 12 | 18/20 | %48 | %24 | %1 |
| 14 | 12.6 | 10 | 18/20 | %33 | %36 | %9 |
| 15 | 25.0 | 2 | 19/20 | %34 | %40 | %16 |
| 16 | 11.8 | 10 | 6/20 | %22 | %14 | %0 |
| 17 | 7.5 | 6 | 15/20 | %27 | %13 | %2 |
| 18 | 4.6 | 0 | 16/20 | %40 | %29 | %2 |

**Bulgu:** salt savaş payı neredeyse hiç değişmedi (Bölüm 9 %50 → %52, 12 %42 → %44, 13 %49 → %48). Havuzun tükendiği gün
ise ileri kaydı (Bölüm 9: 0 → 7, 13: 0 → 12, 6: 12 → 28). "Ne olay ne savaş" payı düştü (Bölüm 9: %27 → %20,
15: %38 → %40 gürültü içinde). Nedeni şu: salt savaş günlerinin **hepsi** görevde, keşifte geçiyor. 20 oynanışta Bölüm
9'un 313 salt savaş gününün 313'ü, Bölüm 13'ün 302'sinin 302'si görevde. Üs olayları yalnız üste oynanır; boş üs
günlerini doldurur ama keşif görevindeki savaş günlerine dokunamaz. Bu payı düşürmek için ya keşif bölgelerinde geçen
olaylar gerekir (bölgede `kesif` olayları; keşif eylemi %50 olasılıkla bölgenin olayını çeker), ya da simülatörün keşif
davranışı (görev süresi, keşifte savaş olasılığı) yeniden ele alınmalıdır. Karar kullanıcıda.

### Yeni ölçüt: hikayesiz gün (DECISIONS #144)

**Hikayesiz gün:** o gün hiçbir olayda ya da Sistem isteminde seçim yapılmamış. Varış sahnesi ve zaman atlama özeti
sayılmaz. Savaş, Ezme ile biten tek satırlık savaş da, hikaye anı değildir. Hedef: hiçbir bölümde serbest günlerin
%35'inden fazlası hikayesiz olmasın. "Salt savaş günü" tabloda kalır, ama artık hedef değil.

**Simülatör doğrulaması:** denge nüfusunun gün akışı, el boşalınca üsten çoğunlukla ayrılıyor. Serbest günlerin %89'u
görevde geçiyor. İnsan benzeri strateji (`Profile.human`, `:tools:simulator:akis --args="20 <klasör> insan"`) yalnız
ölçüm içindir: üste kartların %90'ını oynar, boş elde %40 antrenman, %10 dinlenme, %20 zaman atlama, kalanında keşif.
Görevdeki davranış ve görev süresi aynı kalır (kullanıcı kararı). Görevde geçen gün payı %83'e iniyor; fark küçük, çünkü
görev bir kez başlayınca süresi aynı. İnsan benzeri stratejiyle (20 oynanış):

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün | Hikayesiz gün |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 10.7 | 12 | 1/20 | %11 | %5 | %7 | %12 |
| 2 | 11.2 | 5 | 19/20 | %30 | %12 | %14 | %43 |
| 3 | 3.0 | 0 | 18/20 | %37 | %18 | %13 | %35 |
| 4 | 9.4 | 8 | 1/20 | %11 | %4 | %4 | %13 |
| 5 | 18.8 | 17 | 17/20 | %30 | %13 | %8 | %42 |
| 6 | 13.8 | 12 | 16/20 | %39 | %17 | %4 | %50 |
| 7 | 24.9 | 8 | 19/20 | %16 | %36 | %29 | %73 |
| 8 | 26.6 | 16 | 20/20 | %26 | %33 | %30 | %80 |
| 9 | 30.1 | 6 | 20/20 | %52 | %19 | %8 | %71 |
| 10 | 21.9 | 20 | 6/20 | %29 | %14 | %4 | %40 |
| 11 | 19.6 | 19 | 17/20 | %29 | %29 | %20 | %70 |
| 12 | 30.0 | 16 | 19/20 | %42 | %26 | %9 | %72 |
| 13 | 31.6 | 4 | 19/20 | %47 | %23 | %8 | %72 |
| 14 | 12.6 | 9 | 17/20 | %29 | %30 | %20 | %65 |
| 15 | 25.1 | 2 | 19/20 | %33 | %31 | %27 | %85 |
| 16 | 11.7 | 10 | 6/20 | %21 | %7 | %2 | %24 |
| 17 | 7.5 | 3 | 19/20 | %26 | %18 | %2 | %28 |
| 18 | 4.7 | 0 | 18/20 | %33 | %29 | %6 | %52 |

Bölge bazında açık `python tools/qa/akis/bolge_acik.py <klasör>` ile alınır. Açığın %40–60'ı Kule Dibi'nde: Kule
oradan tırmanılıyor ve her kat bir savaş günü. Hedefe inmek için oynanış başına yaklaşık 85 hikaye günü daha gerekiyor.

### Sonrası (128 bölge, üs ve kamp olayı; DECISIONS #145)

Kullanıcının onayladığı 105 bölge/üs olayı ve 23 saha kampı sahnesi yazıldı: Bölüm 5–15 ve 18, dosyalar
`events/58_bolge_p2a|b|c.yaml` ve `75_bolge_p3a|b|c|d.yaml`. Hepsinin TR/EN incelemesi yapıldı. İnsan benzeri strateji,
20 oynanış:

| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün | Hikayesiz gün |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 10.7 | 12 | 1/20 | %11 | %5 | %7 | %12 |
| 2 | 11.2 | 5 | 19/20 | %30 | %12 | %14 | %43 |
| 3 | 3.0 | 0 | 18/20 | %37 | %18 | %13 | %35 |
| 4 | 9.4 | 8 | 1/20 | %11 | %4 | %4 | %13 |
| 5 | 18.4 | 16 | 16/20 | %31 | %12 | %7 | %40 |
| 6 | 13.8 | 12 | 17/20 | %35 | %20 | %4 | %47 |
| 7 | 23.4 | 16 | 20/20 | %9 | %26 | %25 | %55 |
| 8 | 26.2 | 17 | 20/20 | %14 | %19 | %27 | %56 |
| 9 | 30.1 | 13 | 20/20 | %40 | %12 | %7 | %53 |
| 10 | 22.1 | 23 | 5/20 | %26 | %16 | %3 | %40 |
| 11 | 17.6 | 21 | 18/20 | %25 | %18 | %16 | %51 |
| 12 | 29.9 | 16 | 18/20 | %34 | %20 | %5 | %55 |
| 13 | 31.8 | 15 | 20/20 | %39 | %15 | %6 | %54 |
| 14 | 12.8 | 9 | 20/20 | %26 | %25 | %19 | %61 |
| 15 | 25.0 | 17 | 19/20 | %20 | %23 | %19 | %56 |
| 16 | 11.8 | 10 | 8/20 | %29 | %8 | %0 | %32 |
| 17 | 7.6 | 3 | 18/20 | %28 | %11 | %8 | %32 |
| 18 | 4.8 | 1 | 17/20 | %26 | %19 | %3 | %36 |

| Bölüm | Hikayesiz gün (önce → sonra) |
| ---: | --- |
| 7 | %73 → %55 |
| 8 | %80 → %56 |
| 9 | %71 → %53 |
| 11 | %70 → %51 |
| 12 | %72 → %55 |
| 13 | %72 → %54 |
| 14 | %65 → %61 |
| 15 | %85 → %56 |

**Hedefe ulaşılamadı.** Hikayesiz gün payı ortalama 18 puan düştü, ama 13 bölüm hâlâ %35'in üstünde. Kalan açık oynanış
başına yaklaşık 49 hikaye günü (önce 85). Bir oynanışta yeni olayların 96'sı oynanıyor, ama açığı ancak yaklaşık 36 gün
kapatıyor. Nedenler:
1. Olayların bir kısmı zaten hikaye anı olan günlere düşüyor: zincirler ve aynı gün gelen kartlar.
2. Kamp sahnelerinin yalnız yaklaşık 4'ü oynanıyor (23'ten). O göreve o yoldaşın katılmış olması gerekiyor.
3. Kule Dibi'nde tırmanılan her kat savaş günü olarak kalıyor.

**Denge (1000 oynanış):** ilk ölçümde 200 oynanışlık denge testi hakimiyeti %59 buldu. Yeni olay bir savaş yuvasının yerini
alıp daha az XP veriyordu. Yeni olayların `xp_seviye` ödülleri 1,5 katına çıkarıldı (560 etki; 5–12 → 8–18). Sonrası:
hakimiyet %62,0 (önce %63,4, sınır %60). Sonların payı en çok 1,3 puan kaydı (Demir Taht %7,9 → %6,6). Hepsi
%1–%40 içinde. Kule 100'e ulaşan %59,2 → %51,5. Bitmeyen oynanış 0.

### Kamp kuralı gevşetildi (DECISIONS #146)

**Teşhis:** kamp kaydına göre (`AkisIzi` `kamp`, 20 oynanış) keşifte oynanış başına yaklaşık 200 saha kampı kuruluyor.
- Bunların %52'sinde hiç yoldaş yok.
- Elif hiçbir oynanışta yoldaş olmuyor: 5 kamp sahnesi hiç oynanamıyordu.
- Aras ancak bazı oynanışlarda yoldaş.

**Yeni kural:** yoldaş olmayan bir müttefik de kampa gelir. Şartlar: tanışılmış, yaşıyor, gitmemiş, "hain" işareti yok, güven
≥ 10.

**Sonuç (insan benzeri, 20 oynanış):**
- Oynanan kamp sahnesi oynanış başına 3,4'ten 8,75'e çıktı; 23 sahnenin 22'si oynanıyor.
- Hikayesiz gün payı gürültü içinde kaldı (Bölüm 9 %53 → %52, 6 %47 → %44, 18 %36 → %50, küçük örnek). 23 sahne 11 bölüme
  dağılınca bölüm başına yarım gün ediyor.
- Denge (1000 oynanış): hakimiyet %62,2. Sonlar %1–%40 içinde (Kod Kırıcı %20,9 → %17,8, Hasat %19,7 → %21,1).

Kalan en büyük kaldıraç Kule katları.

### Kule hızlı tırmanış (DECISIONS #147)

Kademenin altındaki katlar tek yuvada geçiliyor; ödüller aynen veriliyor. İnsan benzeri strateji, 20 oynanış: hikayesiz gün
payı değişmedi (Bölüm 9 %52 → %51, 13 %54 → %56, 15 %56 → %58; gürültü içinde). Kule günleri kısaldı, ama boşalan yuvalar
keşfe ve savaşa gidiyor. Denge (1000 oynanış): hakimiyet %64,3, Kule 100'e ulaşan %63,4, sonlar %1–%40 içinde (Koruyucu
%14,2).

### Hızlı tırmanıştan sonra Kule'nin payı

İnsan benzeri strateji, 20 oynanış; günlük özetteki `kule` sayacı (Kule ya da hızlı tırmanış eylemi). Bölüm 5–18'de kalan
açık oynanış başına 42,3 gün. Bunun 24,7 günü Kule'ye çıkılan ve hikaye anı olmayan günler (%58). Öteki 17,6 gün keşif ve
Kapı günleri.

| Bölüm | Hikayesiz | Kule günü | Hikayesiz Kule günü | Açık (gün) |
| ---: | ---: | ---: | ---: | ---: |
| 7 | %53 | 5.2 | 2.4 | 4.3 |
| 8 | %53 | 5.8 | 2.2 | 4.7 |
| 9 | %51 | 7.5 | 3.2 | 4.7 |
| 11 | %55 | 4.3 | 2.3 | 3.2 |
| 12 | %55 | 7.5 | 3.8 | 6.1 |
| 13 | %56 | 7.8 | 4.6 | 6.8 |
| 14 | %63 | 1.9 | 1.2 | 3.5 |
| 15 | %58 | 4.8 | 2.4 | 5.7 |

### Elif denetimi

100 oynanış (denge nüfusu; `AkisIzi` bitiş kaydı). `npcs/elif_sari.yaml` `yoldas: true`. Tek katılma noktası
`elif_sahra_hastanesi` (Bölüm 1) son sahnesi: yoldaş yeri ve güven ≥ 12. Yoldaş olma oranı %6. Bu bilinçli bir seyreklik:
Elif'in yayı Konsey'de geçer ve hiçbir yay düğümü ya da sonucu yoldaşlık istemez.
- **Düğüm erişimi (oynanış başına):** `elif_sahra_hastanesi` %99, `siginak_1_konsey` %100, `elif_konsey_oylamasi` %100,
  `elif_gece_nobeti` %100, `elif_lonca_darbesi` %100, `elif_baris_masasi` %49, `elif_son_gece` %46.
- **Barış masası:** son iki düğüm Elif yaşıyorsa gelir. Demir Gece'de oynanışların %51'inde ölüyor (tasarım).
- **Aşk yayı** (`elif_ask`, güven ve bağlılık ≥ 30–35 + merhamet ≥ 10): %12. 12 oynanışın 10'unda Elif hiç yoldaş olmamış.
- **Barış sonucu:** `elif_baskan` %8 (8'in 6'sında yoldaş değil), `insan_ittifaki` %22. Diğer bayraklar: `elif_dostluk` %26,
  `elif_mesafe` %42, `elif_konsey_basi` %34.
- **Sonuç:** ulaşılamayan düğüm yok. En seyrek sonuçlar aşk yayı (%12) ve başkanlık (%8). İkisi de yoldaşlığa değil ilişki ve
  itibar eşiklerine bağlı. Daha önceki "Elif hiç yoldaş olmuyor" tespiti 20 oynanışlık insan benzeri örnekten geliyordu ve
  yanlıştı (doğrusu %6).

### Kule kat anları (DECISIONS #148)

55 olay ve 5 unvan: `events/98_kule_an_a|b|c.yaml`, `feats/98_kule_an.yaml`, `titles/98_kule_an.yaml`. Dağılım: kestirme,
tuzaklı hazine, yaralı rakip ve Sistem yorumu için bant başına birer varyasyon (4 × 10); Aras yarışı 10; ilk temizleyen 5.
Hepsinin TR/EN incelemesi yapıldı. İnsan benzeri strateji, 20 oynanış: oynanış başına yaklaşık 25 kat anı ve 4,9 ilk
temizleyen sahnesi. Hikayesiz gün payı:

| Bölüm | 128 olaydan önce | Kat anlarından önce | Kat anlarından sonra |
| ---: | ---: | ---: | ---: |
| 1 | %12 | %12 | %12 |
| 2 | %42 | %42 | %42 |
| 3 | %35 | %35 | %35 |
| 4 | %13 | %13 | %13 |
| 5 | %43 | %39 | %32 |
| 6 | %50 | %44 | %37 |
| 7 | %74 | %53 | %44 |
| 8 | %80 | %53 | %50 |
| 9 | %71 | %51 | %46 |
| 10 | %40 | %37 | %31 |
| 11 | %72 | %55 | %49 |
| 12 | %72 | %55 | %53 |
| 13 | %72 | %56 | %50 |
| 14 | %66 | %63 | %56 |
| 15 | %85 | %58 | %52 |
| 16 | %24 | %26 | %24 |
| 17 | %28 | %34 | %27 |
| 18 | %54 | %57 | %52 |

Uzun bölümler (7–9, 11–15) %44–56 arasında kaldı; kullanıcı bunu kabul etti, ek içerik yazılmadı. Denge (1000 oynanış):
hakimiyet %65,7; sonlar %1–%40 içinde (Koruyucu %14,4, Kod Kırıcı %20,2); Kule 100'e ulaşan %62,3.

### Elif: Demir Gece'deki ölümün çözümlemesi

100 oynanış, seçim kaydıyla (`AkisIzi` `secim`). Elif 51 oynanışta öldü:
- **Evde kalmak (30):** saf seçim. Yalnız Lonca itibarı ≥ 30 iken görünür.
- **İskele'ye koşmak (16):** Konsey kurtarılıyor, ardından iki seçenek de ölüm. Seçimden sonra kurtarma yolu yok ve olay
  bunu önceden göstermiyor.
- **Hastane ya da pusu savaşını kaybetmek (4):** Pala ve askerleri oyuncu seviyesine göre ölçekleniyor (`@oyuncu+2`). Güç
  sonucu değiştirmiyor; savaş şansa kalıyor.
- **Zar yok.** Uyarı yalnız hastane sahnesinde bir sezgi (oksijen tüpleri, ALG 200). Önceden hazırlık ancak
  `demir_gece_plani` bilgisiyle pusu seçeneği.

Değişiklik DECISIONS #150 ile uygulandı; sonuç aşağıda.

### Elif: Demir Gece değişikliğinden sonra (DECISIONS #150)

100 oynanış (denge nüfusu). Uyarısız Demir Gece: 0 (önce 20). Uyarı sahnesi %100. Hazırlık seçimleri: hiçbiri 35, Lonca'yı
izleme 29, fişek 25, nöbetçi 11. Elif 54 oynanışta öldü; ölüm oranı için hedef yok. Her ölümün nedeni:

| Ölüm | Oynanış | Neden |
| --- | ---: | --- |
| Evde kaldı | 37 (22 hazırlıksız, 11 fişekli, 4 nöbetçili) | Görünen seçim |
| İskele, sonra "Yine de koş" / Reis'i sırtla | 13 | Hazırlık ya da güç yok (fişek ÇEV ≥ 150, çatılar ÇEV ≥ 250); 2 oynanışta nöbetçi vardı ama ölüm seçeneği seçildi |
| İskele / hastane, savaş kaybı, son şans yok | 4 | Güç yok (gizli seviye farkı da DAY ≥ 250 de yok) |

Düğüm erişimi: sahra hastanesi %99, Konsey oylaması %100, gece nöbeti %100, uyarı %100, Demir Gece %100, barış masası
%46, son gece %45, ikinci yoldaşlık teklifi %94 (Demir Gece sonrası sürümü %0, çünkü Konsey sürümü önce geliyor). Aşk yayı
%16, başkanlık %6, insan ittifakı %24, Elif yoldaş %68, Konsey kurtarıldı %36, son şans (seviye açığa) %1.

Denge (1000 oynanış): hakimiyet %65,9; sonlar %1–%40 içinde (Kod Kırıcı %20,5, Hasat %18,5, Koruyucu %14,3).

