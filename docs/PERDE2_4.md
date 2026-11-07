# Perde 2–4 ve sonlar — içerik sözleşmesi (Aşama 7)

Aşama 7'nin ortak sözleşmesi. Her içerik ajanı ve incelemeci bunu baştan sona okur. Ayrıca oku: `CLAUDE.md` (içerik kuralları),
`CLAUDE_CODE_GOREV.md` (Aşama 7 kabul kriterleri, §6 yazım üslubu), `docs/GDD.md` (OP MC tasarımı, Dünya, Dinamik karakterler,
Sonlar), `docs/GDD.en.md` (İngilizce adlar), `docs/PERDE1.md` (Perde 1 dünya durumu). Şema örneği: `content/events/40_ana_yay.yaml`,
`content/events/30_carsi_istasyon.yaml`.

**Kullanıcı kuralı: hikaye kusursuz olmalı; içerikte asla kısaltma, tasarruf yok.** Her sahne tam kalitede, iki dilde, kendi sesiyle.

## 1. Büyük resim

Oyuncu Öğretici'den Kıyıkent'e (EN: Port Harlow) döner. 335 gün sonra (Gün 365) Dünya ya tam bir Katman olarak kabul edilir ya da
hasat edilir. Üç katmanlı çatışma: **dışarıda** Kadimler Gün 365'i bekler; **içeride** dört insan fraksiyonu insanlığın nasıl
hayatta kalacağı üzerine savaşır; **derinde** Sistem'in kendisi vardır (hasat aracı mı, kurtarma protokolü mü — Perde 2 seçimleri
hangisinin "doğru" olduğunu belirler; gizli gerçek: Sistem bir **döngüdür** ve Yankı önceki döngünün anomalisinden kalan bir parçadır).

Kahraman bir anomalidir (OP MC): her kademe **Mücadele → Kırılma → Hakimiyet → Yeni tavan** döngüsünü izler. Hakimiyet oyun
süresinin çoğudur: gösteriş seçenekleri (`tur: gosteris` + stat `gerekli`), [Gizle]/[Açığa vur], Ezme, fraksiyonların oyuncuyu
istemesi, rakiplerin kıskançlığı. Gerçek seçimler ahlakidir; oyuncu ne yaparsa yapsın güçlenir.

### Perdeler, kademeler, dünya seviyesi

| Perde | Gün | Kademe (beklenen) | Ana düğümler (ana yay günü) |
| --- | --- | --- | --- |
| 2 — Entegre Dünya | 31–180 | E → C | Dönüş (31) · Kule (40) · fraksiyon seçimi (45) · Kasaba Çekirdeği (60) · ilk kapı taşması (80) · Yankı'nın ortaya çıkışı (105) · Sistem gerçeğinin ilk parçası (135) · Kadim gemisi (178) |
| 3 — Çok-evren | 181–330 | C → A | Thessar'ın inişi (181) · çok-evren geçidi (200) · Yol oluşumu (260) · büyük ihanet (300) · filo yolda (320) |
| 4 — Hasat | 331–365 | A → S | Hasat başlıyor (331) · fraksiyonların son pozisyonu (336) · Kadim istilası (342) · Çekirdeğe yol (350) · Sistem Çekirdeği (352) · son karar (358) · Hasat vakti (365, güvenlik) |

**Dünya seviyesi** (Perde 2+ bölge düşmanları, kapılar; `@dunya`): Gün 31 ≈ 20 · 60 ≈ 29 · 80 ≈ 38 · 100 ≈ 48 · 120 ≈ 62 ·
150 ≈ 91 · 180 ≈ 132 (C) · 200 ≈ 170 · 230 ≈ 248 (B) · 260 ≈ 361 · 300 ≈ 600 (A) · 330+ ≈ 876 (S).
**Kule katı seviyesi** = 20 · 1,0385^kat: kat 10 ≈ 29 · 20 ≈ 43 · 30 ≈ 62 · 40 ≈ 91 · 50 ≈ 132 · 60 ≈ 193 · 70 ≈ 281 · 80 ≈ 410 · 90 ≈ 597 · 100 ≈ 876.

## 2. Motor özellikleri (Aşama 7'de eklendi)

- **Ölçekli düşman:** `"dusman_id@seviye"` (sabit), `"dusman_id@dunya"` / `"@dunya+20"` (dünya seviyesi ±%), `"dusman_id@oyuncu"` /
  `"@oyuncu+5"` (oyuncunun gerçek seviyesi ±n). Statlar, can, saldırılar ve XP seviyeyle ölçeklenir; kademe seviyeden gelir.
  - Kazanılması gereken hikaye savaşları ve bosslar: **`@oyuncu+N`** (her oyuncu için gerçek savaş; N = 0–8). Büyük savaşlarda
    mutlaka `kaybet` sahnesi yaz (yazılmış yenilgi, ölüm değildir).
  - Dünyaya bağlı tehditler (sokaktaki canavar, sıradan devriye): `@dunya` (Kule'yi hızlı tırmanan oyuncu bunları **ezer**: hakimiyet).
  - Kademe farkı hasarı ×2/×0,5'tir: oyuncunun kademesinin üstünde sabit seviyeli düşman ölümcüldür. "Yeni tavan" anı için bilinçli kullan.
- **Etkiler:** `xp_seviye: n` (oyuncunun bir sonraki seviye XP ihtiyacının %n'i; Perde 2+ olaylarında **sabit `xp` yerine bunu kullan**:
  küçük 10–25, orta 30–60, büyük 80–150, ana yay doruğu 150–300), `kule_kat: n`, `yol: id`, `oz: {öz: n}` (kavrayış; 100'de bağlama istemi).
- **Koşullar:** `kademe_min: C`, `ordu_min: n` (toplam birlik sayısı), `yoldas_sayisi_max: n`, `oz: id`, `yol: id`, `son_uygun: id`,
  `hicbir_son: true`, `son: id` (yalnızca epilogda).
- **Seçenek alanı `kronik`:** `{tr, en}` — son ekranındaki "önemli olan seçimler" kartında bu seçimin **uzun vadeli etkisi**
  (tek cümle, geçmiş zaman, epilog sesi: "Kordon'u bıraktın; o sahil bir daha insan sesi duymadı."). Önemli (`onemli: true`)
  olayların **her** seçeneğine yaz. İstersen `kronik_agirlik: n` (varsayılan: ahlak + itibar/2 + ölüm 25 + fraksiyon 12 + tohum 6 + 10).
- **Dikkat eşikleri** her perdede yeniden kurulur: olay `dikkat_<25|50|75|100>_p<perde>`. **Yozlaşma eşikleri** (bir kez): `yozlasma_<25|50|75|100>`.
- **Bölgeler** (`regions.yaml`, yer adları `names.yaml`): `kiyikent_liman` (Sığınak), `kordon`, `eski_sehir`, `sanayi` (Lonca),
  `tepe` (Yükselenler), `kule_dibi` (Kule; çok-evren geçidi), `batik_takimada`, `pirinc_kervan` (`cok_evren_acik`), `yedi_taht`
  (`yedi_taht_davet`), `sistem_cekirdegi` (`cekirdek_yolu_acik`). Öğretici bölgeleri Perde 1'de kapanır. Yer belirteci `{yer:kordon}` vb.;
  ayrıca `{yer:kule}`, `{yer:kasaba_cekirdegi}`, `{yer:kiyikent}`.
- **Kule:** Kule Dibi'nde "Kule" eylemi bir sonraki katı açar; her 10. kat `kule_kat_<n>` olayıdır (Parti L2).
- **Parça dosyalar:** düşmanlar `content/enemies/<parti>.yaml` (`dusmanlar:`), eşyalar `items/<parti>.yaml` (`esyalar:`),
  unvanlar `titles/<parti>.yaml` (düz eşlem, `titles.yaml` biçimi), uyanışlar `awakenings/<parti>.yaml` (`uyanislar:`),
  bayraklar `flags/<parti>.yaml`, görevler `quests/<parti>.yaml`, bilgiler `knowledge/<parti>.yaml`. Aynı kimlik iki kez = hata.

## 3. Sözleşme: ortak durum

Ortak bayraklar `content/flags/00_sozlesme.yaml`'da tanımlı (açıklamalarıyla oku); **yeniden tanımlama**. Ortak görevler
`content/quests/00_sozlesme.yaml`: `zincir_siginak`, `zincir_lonca`, `zincir_yukselenler`, `zincir_sessiz_ag`, `zincir_kadimler`.

**Değişkenler:**
- `hayatta_kalan` (yüzde, Perde 1'den gelir; ≤35 düşük, ≥65 yüksek): bir mahalleyi/grubu kurtarmak +2…+6, terk etmek −2…−8.
- `kadim_tehdit` (0–100): Kadimler'in Dünya üzerindeki eli. Dönüşte +20 (Parti A), Kadim gemisi +10; Kadimler'le ticaret, ihanet,
  düşen mahalleler +3…+10; Kadim güçlerini yenmek, Thessar'ı caydırmak −3…−10. Vassal sonu ≥ 70 ister.
- `kule_kat` (motor), `onceki_oyun` (motor; bitirilmiş oyun sayısı).
- Fraksiyon itibarı: `siginak`, `lonca`, `yukselenler`, `sessiz_ag`, `kadimler` (−100…+100).

**Perde 1'den gelen durum** (okumak serbest): `deniz_yukselenlere_gitti`, `npc_olu/npc_gitti/yoldas: deniz_arman`,
`deniz_kardese_soz_verildi`, `kardes_izi_bulundu`, `kardes_notu_saklandi`, `deniz_yankiyi_biliyor`, `kirilma_yasandi`, `yeni_tavan_goruldu`,
`aras_tanisildi`, `aras_yarisi_*`, `sinyal_cevaplandi`, `yanki_ile_konusuldu`, `metro_kurtarildi`, bilgi `yanki_parcasi`, `lider_fraksiyon` + itibar.
NPC'lerin Perde 1 sonundaki olası durumlarını `content/npcs/<id>.yaml` ve `content/events/*.yaml` içinde gör (Elif ve Mert yoldaş olabilir).

### Sonlar (Parti K2 yazar; koşullar sabittir, testler bunları sınar)

| Son | Kimlik | Koşul (`endings.yaml`) |
| --- | --- | --- |
| Koruyucu | `koruyucu` | `ahlak_min: {merhamet: 40}`, `itibar_min: {siginak: 50}`, `degisken_min: {hayatta_kalan: 40}`, `bayrak: kadimler_yenildi` |
| Demir Taht | `demir_taht` | `ahlak_min: {duzen: 50}`, `ahlak_max: {insanlik: -30}`, `bayrak: lonca_hakimiyeti` |
| Yükseliş | `yukselis` | `ahlak_max: {insanlik: -70}`, `gorev: {zincir_yukselenler: tamam}`, `npc_hayatta: vahit` |
| Kod Kırıcı | `kod_kirici` | `gorev: {zincir_sessiz_ag: tamam}`, `iliski_min: {yanki: {guven: 60}}`, `npc_hayatta: mert_kilic`, `bayrak_yok: yanki_silindi` |
| Özgürlük | `ozgurluk` | `ahlak_max: {duzen: -60}`, `ahlak_min: {insanlik: 50}`, `bayrak: sistem_cekirdegine_ulasildi` |
| Vassal | `vassal` | `iliski_min: {thessar: {baglilik: 50}}`, `degisken_min: {kadim_tehdit: 70}` |
| Yalnız Avcı | `yalniz_avci` | `yoldas_sayisi_max: 0`, `kademe_min: S`, `bayrak: uye_yok` (QA P1-13) |
| Hasat | `hasat` | yedek (`yedek: true`): hiçbir son tutmazsa; Demir Mod ölümü (`son_hasat`) da buraya bağlı |
| Döngü (gizli) | `dongu` | `degisken_min: {kule_kat: 100, onceki_oyun: 1}`, `bayrak: yanki_son_sirri`, `gizli: true` |

Ahlak eksenleri: `merhamet` (+Merhamet / −Acımasızlık), `duzen` (+Düzen / −Özgürlük), `insanlik` (+İnsanlık / −Güç, Sistem'e uyum).
"Güç ≥ 30" = `insanlik ≤ −30`; "Özgürlük ≥ 60" = `duzen ≤ −60`. **Her son en az bir tutarlı oyun tarzıyla ulaşılabilir olmalı**:
zincirini yazan parti, zincirin baştan sona tamamlanabilir olduğundan ve ilgili ahlak eksenini besleyen seçimler sunduğundan sorumludur.

### NPC kaderleri (GDD tablosu; her yay tüm kaderlere izin vermeli)

| NPC | Kaderler | Kader bayrakları / etkileri |
| --- | --- | --- |
| Deniz (`deniz_arman`) | sadık yoldaş · Yükselenler'e katılan trajik rakip · oyuncuyu kurtarmak için ölen kahraman | `yoldas`, `npc_fraksiyon: {deniz_arman: yukselenler}`, `ihanet_deniz`, `npc_ol` |
| Kaya (`kaya_demir`) | akıl hocası · oyuncunun devirdiği diktatör · son savaşta müttefik general | `kaya_devrildi` (+`lonca_hakimiyeti`), bayrak `kaya_general` (Parti B2) |
| Elif (`elif_sari`) | konsey başkanı ve barış anahtarı · Lonca darbesinde ölür · aşk yayı | `elif_baskan`, `npc_ol: elif_sari`, `elif_ask` (Parti B1) |
| Vahit (`vahit`) | sahtekar · gerçekten haklı · Yükseliş sonunun kapısı | `vahit_sahtekar`, `vahit_hakli`, `gorev zincir_yukselenler` |
| Yankı (`yanki`) | Sistem'in kaynağını açan rehber · oyuncuyu ele geçirmeye çalışan virüs · silinir | Güven ≥ 60, `yanki_ele_gecirdi`, `yanki_silindi` (+ `npc_ol: yanki`, `bayrak_kaldir: yanki_bagi`) |
| Aras (`aras_kurt`) | ebedi rakip · çok-evren yoldaşı · oyuncu yerine "S kademe insan" | işaret `rakip`, `aras_cok_evren_yoldasi` (+`yoldas_katil`), `aras_s_kademe` |
| Mert (`mert_kilic`) | Kod Kırıcı sonunun anahtarı · Kadimler'e satılan muhbir | `gorev zincir_sessiz_ag`, `mert_kadimlere_satti`, `ihanet_mert` |
| Thessar (`thessar`) | Vassal ortağı · kendi ırkına ihanet eden müttefik · son boss | Bağlılık ≥ 50, `ihanet_thessar`, `thessar_son_duello` savaşı (`npc:thessar`) |

## 4. Partiler (her parti kendi dosyalarına yazar)

Olay kimlikleri aşağıdaki gibidir (başka partiler bunlara tohum ekebilir). Gün aralıkları önerilir; `perde` alanını doldur. "AY n" =
`ana_yay: {gun: n}`. Her parti kendi düşmanlarını, unvanlarını, eşyalarını, bayraklarını, bilgilerini parça dosyalarına yazar.
NPC yaylarının sahibi parti, o NPC'nin `content/npcs/<id>.yaml` dosyasındaki `yay_dugumleri` listesini kendi düğümleriyle günceller
(listedeki kimliklerin hepsi olay olarak var olmalı; yoksa doğrulayıcı uyarır).

### Parti A — Perde 2 ana yay · `events/50_perde2_ana.yaml`, `flags/50_perde2_ana.yaml`
- **Perde 1 köprüsü:** `events/40_ana_yay.yaml` içindeki `perde1_son` ve `perde1_son_yenilgi` artık son değil: `son: true` ve "Bu
  sürümde hikâye burada duruyor" bildirimi kalkar; her birine tek seçenek ("Işığa yürü") eklenir: `gun: 3`, `bolge: kiyikent_liman`,
  `sonraki: "@akis"` (metin aynı kalır, Perde 2'ye bağlanır).
- `perde2_donus` (AY 31): Dönüş. Kıyıkent yan yatmış vapur, mavi paneller, canavar bölgeleri; dört kampın şehre yerleşmesi; Deniz'in
  akıbetine göre dallar. `perde2_basladi`, `kadim_tehdit` +20.
- `kule_yukselisi` (AY 40): Şehrin ortasında Kule yükselir. `kule_acik` (Kule eylemi açılır).
- `fraksiyon_secimi` (AY 45): dört fraksiyon oyuncuyu ister (OP MC: hepsi onu ister). `uye_*` bayraklarından biri.
- `kasaba_cekirdegi` (AY 60): Kasaba Çekirdeği kimin olacak: `cekirdek_*` bayraklarından biri; bölge kontrolü (`kontrol`).
- `ilk_tasma` (AY 80): Kordon'daki kapı taşar; büyük savaş + sivil seçimleri; `kordon_kurtarildi` ya da `kordon_dustu`, `hayatta_kalan`.
- `yanki_ortaya_cikis` (AY 105): Yankı başkalarına görünür (Çekirdek yayınında, pencerelerde); oyuncunun sırrı açığa çıkar. `yanki_gorundu`.
- `sistem_gercegi` (AY 135): Sistem gerçeğinin ilk parçası (Kule'nin bir katında ya da Çekirdek'te bir "başlık"). Perde 2 seçimlerine
  göre `gercek_hasat` **ya da** `gercek_kurtarma` (tam biri); bilgi `sistem_basligi` (kendi knowledge dosyan).
- `perde2_son` (AY 178): körfezin üstünde Kadim gemisi. `kadim_gemisi_goruldu`, `kadim_tehdit` +10. Perde 3'e kanca.
- `dikkat_50_p2`, `dikkat_75_p2`, `dikkat_100_p2` (`yalniz_tohum`, motor tohumlar): avcılar, Sistem "düzeltmeleri", ilk Kadim gözcüsü.
- Toplam ≥ 11 olay.

### Parti B1 — Sığınak zinciri + Elif · `events/51_siginak_elif.yaml`
- Zincir (`zincir_siginak`): `siginak_1_konsey` (P2, 50–110), `siginak_2_su_savaslari` (P2, 100–170), `siginak_3_tahliye` (P3, 190–260),
  `siginak_4_son_siginak` (P3, 250–320), `siginak_5_kalkan_yemini` (P4, 331–356; `gorev: {zincir_siginak: tamam}`). Merhamet, itibar ≥ eşikleri.
- Elif yayı: `elif_konsey_oylamasi` (P2), `elif_gece_nobeti` (P2–3; aşk yayı `elif_ask`), `elif_lonca_darbesi` (P3; Lonca güçlüyse ya
  da `ihanet_kaya` sonrasında; Elif ölebilir `npc_ol: elif_sari`), `elif_baris_masasi` (P4; `elif_baskan`, barış anahtarı).
- Koruyucu sonu bu zincirle ulaşılabilir olmalı (Sığınak itibarı ≥ 50, merhamet ≥ 40, hayatta kalan ≥ 40 seçimleri).

### Parti B2 — Demir Lonca zinciri + Kaya · `events/52_lonca_kaya.yaml`
- Zincir (`zincir_lonca`): `lonca_1_kapi_temizligi` (P2, 45–110), `lonca_2_rutbe` (P2, 100–170), `lonca_3_rakip_bastirma` (P3),
  `lonca_4_demir_yasa` (P3), `lonca_5_taht` (P4; `lonca_hakimiyeti` + `gorev: {zincir_lonca: tamam}`).
- Kaya yayı: `kaya_lonca_teklifi` (P2), `kaya_sert_ders` (P2), `kaya_darbe_gecesi` (P3; oyuncu Kaya'yı devirir → `kaya_devrildi`,
  `lonca_hakimiyeti`; ya da Kaya'nın Konsey'e darbesi → `ihanet_kaya` tohumu J'nin `buyuk_ihanet`ine), `kaya_son_savas_generali`
  (P4; `kaya_general`, K1'in `kadim_istilasi`nda ordu gücü).
- Demir Taht sonu (düzen ≥ 50, güç ≥ 30) bu zincirle ulaşılabilir olmalı.

### Parti C — Yükselenler zinciri + Vahit (+ Deniz'in Yükselenler dalı) · `events/53_yukselenler_vahit.yaml`
- Zincir (`zincir_yukselenler`): `yukselenler_1_cagri` (P2), `yukselenler_2_yakit_listesi` (P2), `yukselenler_3_ayin` (P3),
  `yukselenler_4_kurban` (P3), `yukselenler_5_mutlak_sadakat` (P4; `gorev: {zincir_yukselenler: tamam}`).
- Vahit yayı: `vahit_secilmisler` (P2), `vahit_mucize_sinavi` (P2–3), `vahit_maske_duser` (P3; `vahit_sahtekar` ya da `vahit_hakli`;
  Vahit ölebilir), `vahit_yukselis_kapisi` (P4; Yükseliş sonunun kapısı).
- `deniz_yukselen_rakip` (P2–3; `npc_fraksiyon: {deniz_arman: yukselenler}` ise): Deniz trajik rakip; `ihanet_deniz` tohumu J'ye.
- Yükseliş sonu (güç ≥ 70) bu zincirle ulaşılabilir olmalı.

### Parti D — Sessiz Ağ zinciri + Mert · `events/54_sessiz_ag_mert.yaml`
- Zincir (`zincir_sessiz_ag`): `sessiz_ag_1_frekans` (P2), `sessiz_ag_2_kaynak_avi` (P2), `sessiz_ag_3_kule_kodu` (P3),
  `sessiz_ag_4_cekirdek_haritasi` (P3), `sessiz_ag_5_arka_kapi` (P4; `gorev: {zincir_sessiz_ag: tamam}`).
- Mert yayı: `mert_kaynak_parcasi` (P2), `mert_takip_edildi` (P2–3), `mert_kadim_teklifi` (P3; `mert_kadimlere_satti` → `ihanet_mert`
  tohumu J'ye), `mert_kod_kirici` (P4; Kod Kırıcı anahtarı; Yankı'yla birlikte).
- Kod Kırıcı sonu (zincir + Yankı güveni ≥ 60 + Mert hayatta) ulaşılabilir olmalı; Yankı'ya güven kazandıran seçimler sun.

### Parti E — Kadimler zinciri + Thessar · `events/55_kadimler_thessar.yaml`
- Thessar yayı: `thessar_inis` (AY 181; `thessar_indi`, `tanis: thessar`), `thessar_ticaret_masasi` (P3; `yedi_taht_davet` verebilir),
  `thessar_vassal_teklifi` (P3), `thessar_ihanet_fisiltisi` (P3; Thessar kendi ırkına ihanete hazırlanır → `ihanet_thessar` tohumu J'ye),
  `thessar_son_duello` (P4; düşman Thessar ise son boss: `npc:thessar` ya da kendi Thessar boss tanımın).
- Zincir (`zincir_kadimler`): `kadimler_1_ticaret`, `kadimler_2_vergi`, `kadimler_3_muhbir` (P3), `kadimler_4_vassal_sozlesmesi` (P3–4),
  `kadimler_5_teslim` (P4; `gorev: {zincir_kadimler: tamam}`; `kadim_tehdit` yükselir). Vassal sonu (Thessar Bağlılık ≥ 50, tehdit ≥ 70).
- Kadim askerleri, muhafızlar ve Thessar'ın savaş biçimi: `enemies/55_kadimler.yaml` (K1 istilada da kullanır: kimlikleri raporla).

### Parti F — Deniz, Yankı, Aras · `events/56_deniz_yanki_aras.yaml`
- Deniz: `deniz_kardes_bulundu` (P2; kız kardeşin izi Perde 1'den; bulunur, ölü bulunur ya da Yükselenler'de; `deniz_kardes_bulundu`),
  `deniz_canavar_mi_tanri_mi` (P3; yayın kalbi: "arkadaşımın canavara ya da tanrıya dönüşmesini izlemek"), `deniz_son_fedakarlik`
  (P4; Deniz oyuncuyu kurtarmak için ölebilir ya da yaşar). Deniz ölü/gitmiş/Yükselenler'de ise ilgili dallar ya da yerine anma sahneleri.
- Yankı: `yanki_kaynak_izi` (P2–3), `yanki_son_sir` (P4; Çekirdek'te ya da Kule 100'de; Sistem döngüdür → `yanki_son_sirri`; Yankı'yı
  sil → `yanki_silindi`), `yozlasma_50`, `yozlasma_75` (`yalniz_tohum`; Yankı'nın virüs yolu → `yanki_ele_gecirdi`).
- Aras: `aras_kule_dovusu` (P2–3), `aras_cok_evren_kapisi` (P3; çok-evren yoldaşı olabilir), `aras_s_kademe` (P4; oyuncu S değilse ve
  Aras rakipse "S kademe insan" olur → `aras_s_kademe`).
- `deniz_arman.yaml`, `yanki.yaml`, `aras_kurt.yaml` `yay_dugumleri` listelerini güncelle.

### Parti G — Perde 2 Kıyıkent (Liman, Kordon, Eski Şehir) · `events/60_kiyikent_a.yaml`
- ≥ 13 olay, P2 (Gün 31–180), bölgeler `kiyikent_liman`, `kordon`, `eski_sehir`. Şehrin yeniden kurulduğu, canavarlarla yaşandığı
  Kıyıkent: karaborsa, kayıp yakınlar, kapı avcıları, deniz canavarları, Ege kıyısı (EN: Pasifik Kuzeybatı yerelleştirmesi), hakimiyet
  anları (oyuncuyu hafife alanlar, gösteriş). Bazıları `kesif` etiketiyle keşifte çıkar. 3–4 olay `kordon_dustu`/`kordon_kurtarildi`'yı okur.

### Parti H — Perde 2 Kıyıkent (Sanayi, Tepe, Kule Dibi) · `events/61_kiyikent_b.yaml`
- ≥ 13 olay, P2, bölgeler `sanayi`, `tepe`, `kule_dibi`. Lonca'nın sanayisi, Yükselenler'in tepesi, Kule'nin dibinde kurulan avcı
  pazarı; Kule'ye tırmanan insanlar, Küresel Sıralama'nın şehirdeki etkisi, Kasaba Çekirdeği'nin sahibine göre değişen sokaklar.

### Parti I — Çok-evren · `events/70_cok_evren.yaml`
- `cok_evren_kapisi` (AY 200): Kule'nin 50. katının arkasındaki geçit açılır; `cok_evren_acik`.
- **Batık Takımada** (`batik_takimada`): hasat edilmiş bir dünya; Hasat sonunun aynası. ≥ 4 olay + kendi bossu.
- **Pirinç Kervan** (`pirinc_kervan`): Kadimlere boyun eğmiş tüccar bir ırkın dünya-gemisi; Vassal sonunun aynası. ≥ 4 olay;
  `yedi_taht_davet` verebilir.
- **Yedi Taht** (`yedi_taht`): Kadim sarayı; hasatla tanrılaşmış bir ırkın siyaseti; Yükseliş/tanrı sonunun aynası. ≥ 4 olay.
- Her dünyanın kendi ırkı, adları ve siyaseti (özgün; telif kuralı). Toplam ≥ 13 olay. Dünyalar Perde 3–4'te açıktır.

### Parti J — Perde 3 ana yay ve Kıyıkent · `events/71_perde3.yaml`
- `yol_olusumu` (AY 260): Yol oluşumu; `essences.yaml` `yollar` listesinden seçim (`yol: id`, `oz: {a: 100, b: 100}`), `yol_olustu`.
- `buyuk_ihanet` (AY 300): büyük ihanet olayı; hain dünya durumuna göre (öncelik): `mert_kadimlere_satti` → Mert; Lonca güçlü ve
  `kaya_devrildi` yok → Kaya; Thessar Bağlılık ≥ 50 → Thessar kendi ırkına ihanet eder (iyi haber); Deniz Yükselenler'de → Deniz;
  aksi hâlde Aras. `ihanet_*`, `ihanet_onlendi`.
- `perde3_son` (AY 320): filo yolda; son 45 güne kanca. `dikkat_75_p3`, `dikkat_100_p3`.
- ≥ 10 Perde 3 Kıyıkent olayı (şehrin C–A kademe hayatı, ordu kurmak: `ordu_kazan`, Kule 50+ toplumu, Küresel Sıralama'nın tepesi).

### Parti K1 — Perde 4 · `events/80_perde4.yaml`
- `perde4_acilis` (AY 331; `hasat_basladi`), `fraksiyon_son_pozisyon` (AY 336), `kadim_istilasi` (AY 342; ordu `ordu_min`, Kaya general,
  Thessar; sonuç `kadimler_yenildi` / `kadim_anlasmasi` / `kiyikent_dustu`), `cekirdege_yol` (AY 350; `cekirdek_yolu_acik`),
  `sistem_cekirdegi` (AY 352; `sistem_cekirdegine_ulasildi`; F'nin `yanki_son_sir`ini tohumlar), `dikkat_100_p4`.
- ≥ 9 Perde 4 genel olayı (kuşatma altında son günler, vedalar, son kapılar).

### Parti K2 — Sonlar · `events/81_sonlar.yaml`, `content/endings.yaml`
- `son_karar` (AY 358): yalnızca uygun sonları gösterir (`goster: {son_uygun: id}`); hiçbiri yoksa (`hicbir_son: true`) Hasat.
  Seçim ilgili son sahnesine gider. `hasat_vakti` (AY 365, güvenlik): oyun bitmediyse Hasat.
- Son sahneleri (`son: true`): `son_koruyucu`, `son_demir_taht`, `son_yukselis`, `son_kod_kirici`, `son_ozgurluk`, `son_vassal`,
  `son_yalniz_avci`, `son_hasat_gun` (+ mevcut `story/90_sistem.yaml` `son_hasat`, Demir Mod ölümü; zenginleştirilebilir), `son_dongu`.
  Her son sahnesi 4–6 paragraf, dünya durumuna göre koşullu paragraflar.
- `endings.yaml`: `sonlar` (koşullar §3 tablosu), `arketipler` (OP MC türü: `tanri` (insanlık ≤ −40), `tiran` (merhamet ≤ −20, düzen ≥ 20),
  `koruyucu` (merhamet ≥ 20), `kurtarici` (koşulsuz son)), `epilog.npc` (8 ana NPC; kader başına satır; `son:` koşulu kullanılabilir;
  son satır koşulsuz), `epilog.fraksiyon` (5 fraksiyon; son satır koşulsuz).

### Parti L1 — Bestiyer · `enemies/90_bestiyer.yaml`
- ≥ 24 yeni düşman (tasarım seviyesi 20–40; ölçekleme yukarı taşır): her Kıyıkent bölgesine 3–4, her çok-evren dünyasına 3, Sistem
  Çekirdeği'ne 2; ayrıca Kule bantları için 8+. `regions.yaml` düşman listelerini kendi düşmanlarınla değiştirir (yalnızca bu parti).

### Parti L2 — Kule · `content/tower.yaml`, `events/72_kule.yaml`, `awakenings/72_kademeler.yaml`
- `tower.yaml`: `kule: { bolge: kule_dibi, kosul: { bayrak: kule_acik }, seviye: { taban: 20, carpan: 1.0385 }, kat_sayisi: 100,
  bantlar: [...], hikaye_katlari: { 10: kule_kat_10, …, 100: kule_kat_100 } }`. Bantlar 1–9, 11–19, … (10 bant), her bantta 3–5
  düşman (L1'in kimlikleri ya da kendi `enemies/72_kule.yaml`'ın) ve 4+ açılış satırı.
- `kule_kat_10` … `kule_kat_100`: 10 hikaye katı (`yalniz_tohum: true`, `perde` yok); her biri bir "yeni tavan" ya da Sistem sırrı;
  zaferde `kule_kat: n`. Kat 50 = çok-evren geçidinin arkası; kat 100 = Sistem'in döngü olduğuna dair en büyük ipucu (Döngü sonu).
- Kademe kırılmaları: `awakenings/72_kademeler.yaml` — E, D, C, B, A, S kademelerinin her biri için yazılmış bir uyanış anı.

## 5. Ortak yazım kuralları (özet; ayrıntı CLAUDE.md)

- Türkçe: ikinci tekil şahıs, şimdiki zaman, edebi ama akıcı; ", ve" yok. İngilizce: Kuzey Amerika yerelleştirmesi (mil, lb, °F, ABD/Kanada
  deyimleri), sert tür kurgusu sesi; çeviri değil. Paragraf ≤ 3 cümle; sahne 2–5 paragraf; seçenek emir kipinde ≤ 60 karakter.
- **Asla isim yazma:** `{npc:id}`, `{npc:id|tam}`, Türkçe ekler `|e|i|in|de|den|le`, İngilizce `|pos`; `{yer:id}`, `{oyuncu}`, `{fraksiyon:id}`;
  İngilizce zamir `{zamir:id|o|onu|onun|kendi|b}` + fiil uyumu `|s|is|has|does|was` (Türkçede kullanma). Deniz "they", Yankı "it".
- **Yankı'nın sözleri küçük harf ve tırnaksız**, anlatı `{npc:yanki}` ile atfeder. Sistem bildirimleri kuru, kısa, alaycı; İngilizcede köşeli parantez.
- Her olay: giriş sahnesinde **≥ 3 seçenek**; olayın içinde **≥ 1 koşullu** (`gerekli`/`goster`), **≥ 1 tohum eken**, **≥ 1 ilişki
  ekseni değiştiren** seçenek (istisna yalnızca `son_karar`, `hasat_vakti`). `onemli: true` olaylarda her seçenek bir ahlak ekseni oynatır
  ve `kronik` taşır. Başarısız zar kısa bir sahneye gider. Olayın son seçenekleri `"sonraki": "@akis"`.
- Tohumlar yalnızca **kendi olaylarına** ya da bu belgede kimliği verilen olaylara. Tohum olayı bölge/gün tanımaz: açılışı koşullu yaz.
- OP MC: Perde 2+ olaylarının çoğunda en az bir gösteriş (`tur: gosteris` + yüksek stat `gerekli`, ölçek: P2 30–80, P3 80–300,
  P4 300+) ya da [Gizle]/[Açığa vur] seçeneği; sezgiler (`sezgiler`: "[ALG]" satırı ve Yankı'nın sesi `kosul: {bayrak: yanki_bagi}`).
- Yoldaş satırı: sahneye `yoldas_tepkileri: [{npc, metin, kosul}]`.
- Telif: Defiance of the Fall, He Who Fights With Monsters, The Primal Hunter, Solo Leveling'den hiçbir metin, isim, kavram adı kopyalanmaz.
- Her parti bitince: `./gradlew :engine:validateContent -q` (yalnızca **kendi** dosyalarındaki hataları düzelt; başka partilerin yarım
  dosyalarından gelen hataları yok say), kısa bir rapor: olay kimlikleri, eklenen düşman/unvan/eşya/bayrak kimlikleri, başkalarını
  ilgilendiren tohumlar, açık sorular.
