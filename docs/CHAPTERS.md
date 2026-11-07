# Bölüm yapısı: taslak (Hikaye yapısı değişikliği, Adım 1)

**Durum: onay bekliyor.** Teşhis için bkz. `docs/STORY_FLOW.md`. Sınıflandırma programı `tools/qa/akis/siniflandir.py` →
`tools/qa/akis/siniflama.json` (her olay: bölüm(ler), rol, konum, emin olunamama nedeni).

**Değişmeyenler:** mekanikler, bayraklar, seçim sonuçları, NPC hafızası, OP MC sistemleri, son koşulları ve 365 günlük
takvim. Bu yapı yalnızca olayların **ne zaman** ve **nerede** geldiğini değiştirir.

## 1. Kurallar

### Takvim
- Her bölümün sabit bir **açılış günü** ve **son tarihi** vardır. Son tarih, operasyonun bugünkü ana yay günüdür; böylece dünya
  seviyesi, Kule ve denge eğrisi kaymaz.
- Açılıştan son tarihe kadar günler **serbesttir**. Oyuncu brifingden sonra operasyona istediği gün girer.
- Operasyonu erken bitirirse bölümün kalan günleri **Sonrası** olarak üste geçer: serbest günler, NPC sahneleri. Sonraki bölüm
  kendi açılış gününde başlar.

### Son tarih kaçarsa
- Oyun bitmez. **Taşma** yaşanır:
  - operasyon bölgesinin tehlikesi artar (`tehlike`);
  - `hayatta_kalan` −3;
  - bayrak `bolum_<n>_tasti` konur.
- Ertesi sabah operasyon kendiliğinden başlar. Giriş sahnesinde "geç kaldın" paragrafı vardır, darboğazda da taşmanın izi.
- Hikaye beklemez, ama oyuncunun gecikmesi dünyaya yazılır.

### Üs ve görev
- Oyun her an ya **üstedir** ya da bir **görevdedir**.
- **Üs:**
  - Perde 1'de Öğretici kampı (kamp ateşi), Perde 2–4'te Sığınak (Haven, `kiyikent_liman`).
  - Bölüm 5'ten (Kasaba Çekirdeği) sonra Çekirdek'i kim aldıysa üs sahneleri oranın sesiyle de anlatılır; üs olayları yerden
    bağımsızdır (konum `üs`).
  - **Yalnız** üste tetiklenenler: karakter sahneleri, tohum geri dönüşleri, NPC eşikleri, Dikkat ve Yozlaşma eşikleri,
    Küresel Sıralama duyuruları (`bildirim_*`), yoldaş tepkileri ve fırsat eli.
  - Bunlar görevden dönüşte, bir sonraki üs anında sıraya girer; arka arkaya en çok 1 zorunlu olay gelir, ardından en az bir
    oyuncu eylemi.
- **Görev:** ana operasyon, fraksiyon görevi, yan görev, keşif, Kapı ya da Kule tırmanışı.
  - Görev kapalı bir sahne dizisidir; içindeyken Yönetmen yalnız o göreve etiketli olayları seçer.
  - İlk sahne **yolculuk + varış** sahnesidir; son sahne üsse dönüştür.
  - Görev içinde gün ilerleyebilir; Günlük Görev ve takvim işlemeye devam eder.
- **Konum:**
  - Her olayın konumu vardır: bölge ya da `üs`.
  - Konumlu olay yalnız o bölgedeki bir görevin içinde gelir; `üs` olayı yalnız üste.
  - `bolge` etkisi artık "ışınlama" değildir; görevin hedef bölgesini belirtir.

### Bölüm ritmi
Brifing (üs) → Hazırlık (serbest günler) → Ana operasyon (kilitli görev) → Sonrası (üs: sonuçlar, tohumlar, sıralama
değişimi) → **Darboğaz**.

- Darboğaz herkesin geçtiği tek sahnedir. Metni dünya durumuna göre değişir: kim hayatta, kim küs (ilişki eşikleri), hangi
  fraksiyon önde (`lider_fraksiyon`), bölüm taştı mı, Dikkat.
- OP MC eşlemesi: brifing = yeni tavan, hazırlık = hakimiyet, operasyon = mücadele ve kırılma.

### Bölüm ilerlemesi
- Ana yay düğümleri gün yerine **bölüm ilerlemesine** bağlanır (`bolum` değişkeni + bölüm içi adım). Örnek: "Bölüm 6 operasyonu
  bitti" → `deniz_kardes_bulundu`.
- Koşullu ana yay düğümleri (fraksiyon zincirleri, NPC yayları; rol `us_yay`) bölüm penceresinde üste gelir, artık gün geldi diye
  zorla gelmez.
- Fraksiyon zincirlerinin düğümleri (`siginak_*`, `lonca_*`, `yukselenler_*`, `sessiz_ag_*`, `kadimler_*`) **fraksiyon
  görevi**dir (Mass Effect 2'deki sadakat görevleri gibi). Pencereleri bugünkü gün aralıklarından bölüm aralığına çevrilir.

## 2. Bölümler

**Gün sütunu:** bölüm açılış gününde brifingle başlar. **Son tarih**, operasyonun en geç başlayacağı gündür (bugünkü ana yay
günü). Parantez içi bölüm sonudur: darboğaz en geç o gün gelir ve sonraki bölüm ertesi gün açılır. Bölüm pencereleri
`siniflandir.py`'deki `CH` ile aynıdır.

**Darboğaz sütunu:** "yeni" = yazılacak sahne; mevcut olayın adı yazılıysa o olayın son sahnesi darboğaz olur ve eksik
dünya durumu paragrafları eklenir. **Değişen:** darboğaz metninin neye göre değiştiği.

### Perde 1 — Öğretici Katman (üs: Öğretici kampı)

| # | Başlık | Gün: açılış → **son tarih** (bölüm sonu) | Ana tehdit | Ana operasyon | Darboğaz · değişen | Üsse düşen NPC yay düğümleri |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | İlk Gece | 0 → **12** (12) | Kapalı orman, kıt kaynak; Yankı'nın bozuk sesi | `kirilma` (13 sahne, orman) | `kirilma` sonu · kim kurtarıldı, `hayatta_kalan`, Deniz bulundu mu | Deniz: `deniz_bulusma`, `kamp_ates_basi`, `deniz_kardes_ipucu` · Elif: `elif_sahra_hastanesi` · Kaya: `kaya_ilk_tanisma` · Mert: `mert_sifreli_mesaj` · Vahit: `vahit_vaaz` · Aras: `rakip_iz`, `aras_ilk_yaris` · Yankı: `istasyon_sinyali`, `yanki_bozuk_satir` |
| 2 | Hakimiyet | 13 → **24** (24) | E kademesinden bir şey ölçeği sıfırlıyor | `yeni_tavan` (orman); öncesinde, Deniz yoldaşsa `deniz_son_secim` | `yeni_tavan` sonu · Deniz'in akıbeti, ilk fraksiyon eğilimi, Dikkat | Aras: `aras_sira_meydan` · Deniz: `deniz_son_secim` |
| 3 | Bekçi | 25 → **28** (30) | Öğretici Bekçisi; çıkış kapısı | `ogretici_bekci` (Çıkış Kapısı) | `ogretici_bekci` sonu (32 koşullu paragrafla zaten dünya durumunu anlatıyor) | Yankı: `istasyon_sinyali_2` |

### Perde 2 — Entegre Dünya (üs: Sığınak, Kıyıkent Limanı)

| # | Başlık | Gün: açılış → **son tarih** (bölüm sonu) | Ana tehdit | Ana operasyon | Darboğaz · değişen | Üsse düşen NPC yay düğümleri |
| --- | --- | --- | --- | --- | --- | --- |
| 4 | Dönüş | 31 → **40** (45) | Kıyıkent'te dört kamp; Kule yükseliyor | Brifing `perde2_donus` (iskele varışı). Operasyon `kule_yukselisi` (Kule Dibi) | `fraksiyon_secimi` · seçilen fraksiyon, kimin küs olduğu | Kaya: `kaya_lonca_teklifi` · fraksiyon görevleri açılır: `lonca_1`, `sessiz_ag_1`, `yukselenler_1` |
| 5 | Kasaba Çekirdeği | 46 → **60** (66) | Eski Şehir'in kalbi için dört kamp yarışıyor | `kasaba_cekirdegi` (Eski Şehir) | **yeni** · Çekirdek kimin (`cekirdek_*`), kaybeden kampın tepkisi, Elif hayatta mı | Mert: `mert_kaynak_parcasi` · Vahit: `vahit_secilmisler` · `siginak_1_konsey` |
| 6 | İlk Taşma | 67 → **80** (95) | Kordon'da ilk kapı taşması | `ilk_tasma` (Kordon) | `deniz_kardes_bulundu` / `deniz_anma_kardes` · Deniz hayatta mı, Kordon kurtarıldı mı ya da düştü mü | Aras: `aras_kule_dovusu` · Elif: `elif_konsey_oylamasi` · Kaya: `kaya_sert_ders` · `lonca_2`, `sessiz_ag_2`, `yukselenler_2` |
| 7 | Yankı | 96 → **105** (122) | Yankı ilk kez dışarıda görünüyor | `yanki_ortaya_cikis` (Eski Şehir) | **yeni** · Yankı'ya güven, Yozlaşma, kimin tanık olduğu | Mert: `mert_takip_edildi` · Vahit: `vahit_mucize_sinavi` · `siginak_2_su_savaslari` |
| 8 | Sistem Gerçeği | 123 → **135** (150) | Sistem'in ne olduğuna dair ilk parça | `sistem_gercegi` (Kasaba Çekirdeği) | **yeni** · Sistem hakkında ne düşündüğün (bayraklar), lider fraksiyon | Elif: `elif_gece_nobeti` · Yankı: `yanki_kaynak_izi` · Deniz: `deniz_yukselen_rakip` (Yükselenler'deyse) |
| 9 | Kadim Gemisi | 151 → **178** (180) | Gökyüzünde Kadim gemisi | `perde2_son` (Liman, Kule) | `perde2_son` sonu · perde sonu dünya durumu | — |

### Perde 3 — Çok-evren (üs: Sığınak)

| # | Başlık | Gün: açılış → **son tarih** (bölüm sonu) | Ana tehdit | Ana operasyon | Darboğaz · değişen | Üsse düşen NPC yay düğümleri |
| --- | --- | --- | --- | --- | --- | --- |
| 10 | İniş | 181 → **200** (205) | Thessar indi; Kule'de çok-evren geçidi açılıyor | Brifing `thessar_inis`. Operasyon `cok_evren_kapisi` (Kule Dibi) | **yeni** · Thessar'a tutum, Aras rakip mi müttefik mi | Thessar: `thessar_ticaret_masasi` · Mert: `mert_kadim_teklifi` · Aras: `aras_cok_evren_kapisi` · Yankı: `yanki_son_sir` · `siginak_3_tahliye`, `kadimler_1`, `lonca_3`, `sessiz_ag_3` |
| 11 | İlk Dünya | 206 → **225** (230) | Başka bir dünyanın hasadı | Oyuncu seçer: Batık Takımada (`batik_*` 4 olay) ya da Pirinç Kervan (`kervan_*` 4 olay). Diğeri ve Yedi Taht yan görevdir. | `deniz_canavar_mi_tanri_mi` / `deniz_anma_ayna` · Deniz'in gözünde sen, hangi dünyayı kurtardın | Thessar: `thessar_vassal_teklifi` · `yukselenler_3_ayin`, `kadimler_2` |
| 12 | Yol | 231 → **260** (260) | Öz'ünü bir Yol'a bağlama zamanı | `yol_olusumu` (Kıyıkent) | `yol_olusumu` sonu · seçilen Yol, Yozlaşma | Vahit: `vahit_maske_duser` · Elif: `elif_lonca_darbesi` · Thessar: `thessar_ihanet_fisiltisi` · `kadimler_3`, `sessiz_ag_4` |
| 13 | Kapı Fırtınası | 261 → **292** (292) | Aynı gece üç bölgede kapı | `p3_kapi_firtinasi` (Sanayi, Liman) | **yeni** · hangi bölgeyi korudun, Kaya darbe yaptı mı (`kaya_darbe_gecesi`) | Kaya: `kaya_darbe_gecesi` · `siginak_4_son_siginak`, `yukselenler_4_kurban`, `kadimler_4` |
| 14 | Büyük İhanet | 293 → **300** (305) | İçeriden biri seni satıyor | `buyuk_ihanet` (Eski Şehir, Kasaba Çekirdeği, Kule, Tepe) | `buyuk_ihanet` sonu (47 koşullu paragraf) · kim ihanet etti, kim kaldı | Thessar: `thessar_kirik_muhur` |
| 15 | Filo | 306 → **320** (330) | Kadim filosu yolda | `perde3_son` (Liman, Kule) | `perde3_son` sonu · perde sonu dünya durumu | `lonca_4` |

### Perde 4 — Hasat (üs: Sığınak)

| # | Başlık | Gün: açılış → **son tarih** (bölüm sonu) | Ana tehdit | Ana operasyon | Darboğaz · değişen | Üsse düşen NPC yay düğümleri |
| --- | --- | --- | --- | --- | --- | --- |
| 16 | Hasat Başlıyor | 331 → **342** (342) | Kadim istilası | Brifing `perde4_acilis`; üste `fraksiyon_son_pozisyon`. Operasyon `kadim_istilasi` (Kıyıkent) | **yeni** · istila kazanıldı mı (`kadimler_yenildi`), kim düştü, ordu | Aras: `aras_s_kademe`, `aras_zirve_sabahi` · Elif: `elif_baris_masasi` · Kaya: `kaya_son_savas_generali` · Mert: `mert_kod_kirici`, `mert_son_log` · `siginak_5`, `yukselenler_5`, `kadimler_5`, `sessiz_ag_5` |
| 17 | Çekirdeğe Yol | 343 → **352** (352) | Sistem Çekirdeği | `cekirdege_yol` → `sistem_cekirdegi` (yol açıksa) | **yeni** · Çekirdeğe inildi mi, Yankı'nın durumu, yanında kim var | Deniz: `deniz_son_fedakarlik` · Thessar: `thessar_son_duello` · Vahit: `vahit_yukselis_kapisi` · `lonca_5_taht` |
| F | Son Karar | 353 → **358** (365) | Hasat vakti | `son_karar` | `hasat_vakti` + sonlar (değişmez) | Perde 4 epilog olayları (`p4_*`) üste |

**Fırsat eli:** perdenin bugünkü gün aralığına göre bölümlere dağılır. Perde 2'nin 64 olayı Bölüm 4–9'da, Perde 3'ün 42
olayı Bölüm 10–15'te.
- Yerle ilgisi olmayanlar (rol `us`, 120 olay) üste kalır.
- Konumlular yan görev olur: 59 savaşlı, 10 savaşsız. Bunlar bölgeye giden bir görevin içinde ya da "o bölgeye git" kartı
  olarak gelir.
- Kule hikaye katları (10) Kule tırmanışı görevinin içindedir; değişmez.

## 3. Sınıflandırma özeti

| Rol | Olay |
| --- | ---: |
| Üs olayı (karakter, eşik, tohum, bildirim) | 120 |
| Yan görev (konumlu, savaşlı ya da görev bağlı) | 59 |
| Üs yay düğümü (koşullu ana yay) | 21 |
| Ana operasyon | 19 |
| Darboğaz (mevcut) | 13 |
| Konumlu, savaşsız (üs mü, yan görev mi) | 10 |
| Kule hikaye katı | 10 |
| Brifing (mevcut) | 3 |

Programın emin olamadığı **124 olay**:

- **Birden çok bölüme yayılıyor (76).** Çoğu, perdenin tamamına açık yan görev. Önerilen karar: bölüm penceresi = ilk uygun
  bölüm + `gun_max`'a kadar; görev sırası kilitli olduğu için yayılmak sorun değil.
- **Olay içinde yer değişiyor (34).** Ör. `deniz_kardes_bulundu`: Liman → İskele → Kordon → Tepe. Görev içindeki yer
  değişimleri meşrudur; yalnız ara yolculuk satırı eksik olanlara bakılacak.
- **Farklı bölümlerden ekilen tohumlar (15).** Ör. `hastane_karantina`, `yozlasma_50`. Üs olayı oldukları için bölümden
  bağımsız kalabilirler; hatırlatma cümlesi ebeveyne göre seçilir.
- **Savaşlı ve konumlu koşullu ana yay (13).** Ör. `siginak_4_son_siginak`, `kaya_darbe_gecesi`. Bunlar fraksiyon görevi
  olarak kilitli görev olmalı, üs sahnesi değil.
- **Konumlu, savaşsız (10).** Ör. `eski_sehir_ekmek_kuyrugu`, `kordon_kapi_catlagi`.

Tam liste için `siniflama.json`'daki `neden` alanı dolu kayıtlara bakılabilir. Adım 2'de bunlara tek tek karar verilecek ve
kararlar DECISIONS'a yazılacak.

## 4. Yazılacak metinler (iki dil)

| Tür | Adet | Ölçek | Tahmini (TR sözcük; EN de aynı kadar) |
| --- | ---: | --- | ---: |
| **Brifing** (yeni tehdit, son tarih, "önceki bölümde") | 15 yeni (3'ü mevcut olaylara ek) | 3–5 paragraf, koşullu "önceki bölümde" satırları | ~2.500 |
| **Varış** (görevin ilk sahnesi) | 13 bölge × 2 (ilk varış / sonraki) + 17 operasyon girişine yolculuk satırı | 1–2 paragraf | ~2.200 |
| **Darboğaz** | 7 yeni sahne + 11 mevcut sona eksik dünya durumu paragrafları | yeni sahne 8–12 koşullu paragraf | ~4.500 |
| **Taşma** (son tarih kaçtı) | 17 "geç kaldın" paragrafı + bildirim | 1 paragraf | ~1.200 |
| **Hatırlatma** (tohumdan doğan olay) | 248 ebeveyn → tohum çifti (gecikme ≥ 3 gün; 297 çiftin) | 1 cümle; tohum etkisinde `hatirlatma` alanı | ~3.000 |
| **Uzun aradan sonra özet** | ~30 şablon (`system.yaml`) + mevcut `kronik` satırları | motor üretir: bölüm, hedef, kalan gün, yoldaşlar, son 3 kritik seçim | ~500 |
| **Arayüz** | ~12 dizgi (bölüm satırı, görevler ekranı başlıkları) | strings.xml TR/EN | — |

**Toplam:** ~14.000 TR + ~14.000 EN sözcük.
- Karşılaştırma: Parti 9–10 üslup işi bunun kabaca iki katıydı.
- En büyük kalem hatırlatmalar. Hepsi tek cümle ve programla listelenebilir; ebeveyn olayın kronik satırından taslak
  üretilip elden geçirilir.

### Kod işi (Adım 2)
- Motor:
  - `Chapter` modeli ve `chapters.yaml`;
  - `WorldState`'e `chapter`, `chapterStep` ve `mission` alanları;
  - `Director` için üs/görev süzgeci, konum kuralı ve tek-zorunlu kuralı;
  - son tarih ve taşma; ana yay düğümünü bölüme bağlama.
- Kayıt: `SaveCodec` v5 ve eski kayıt geçişi. Bölüm gün aralığından bulunur, yarım görev üste açılır.
- Yükleyici ve denetim: `ContentParser`, `ContentValidator`; olaylara `bolum`, `konum` ve `gorev` etiketleri programla eklenir.
- Simülatör: görev ve üs eylemleri.
- Arayüz:
  - bölüm satırı (`Bölüm N · Hedef · Kalan gün`);
  - Görevler ekranında bölüm hedefi ve yan görevler ayrı;
  - görev içindeyken akış paneli yerine görev ilerleyişi;
  - dönüş özeti kartı.
- **QA P1-21 (Günlük Görev) bu adıma katılır:** görev günleri ile serbest günler ayrılınca günlük eylem hakkı da değişiyor;
  ayrı düzeltip sonra yeniden yazmamak için.
