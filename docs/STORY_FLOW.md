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

Adım 2 doğrulamasında doldurulacak.

| | Sorun | Önce (20 oynanış) | Sonra |
| --- | --- | ---: | ---: |
| a | Görev ortasında ilgisiz olay | 332 | — |
| b | Konum uyuşmazlığı / geçişsiz seyahat | 1.490 | — |
| c | Gergin anın ortasına düşen olay | 370 | — |
| d | Görevi kesen gün bazlı ana hikaye | 39 | — |
| e | Bağlamsız geçiş | 574 | — |
