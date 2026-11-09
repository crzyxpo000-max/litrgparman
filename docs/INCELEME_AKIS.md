# Oyuncu gözüyle inceleme: akış, hikaye, karmaşıklık (2026-11-09)

**Amaç:** oyunu baştan sona bir oyuncunun yaşayacağı sırayla ele almak; ritmin düştüğü yerleri, hikayenin kopuk
noktalarını ve fazla gelen sistemleri bulmak; sadeleştirme ve düzeltme önerilerini önceliklendirmek. **Bu belge yalnız
inceleme ve plandır; hiçbir kod ya da içerik değiştirilmedi.** Kullanıcı onaylamadan düzenleme yapılmayacak.

**Yöntem**
- Belgeler: CLAUDE.md, GDD (OP MC ve Arayüz), CHAPTERS, STORY_FLOW, DECISIONS #130–#150, PROGRESS, `chapters.yaml`,
  `events/07–09_bolumler_*`, `40_ana_yay`, `96_zaman_atlama`, örnek bölge dosyaları.
- Akış kaydı: `:tools:simulator:akis --args="10 tools/qa/akis/iz_inceleme insan"` (insan benzeri strateji), çözümleme
  `cozumle.py` ve `serbest.py`; oynanış 8'in (koruyucu) ilk 31 günü gün gün çıkarıldı.
- Emülatörde (Medium_Phone_API_36.0, Türkçe, koyu tema) prolog + Gün 1–2 + Bölüm 1 operasyonu elle oynandı
  (~45 ekran görüntüsü).
- İki alt ajan: `continuity-checker` (18 bölüm akışı) ve "yeni oyuncu" (GDD okumadan yalnız oyun içi metinlerle Bölüm 1).
  Tam raporlar ortak not klasöründe (`inceleme_akis_sureklilik.md`, `inceleme_akis_yeni_oyuncu.md`).

---

## 1. Oyuncunun yolculuğu

### 1.1 İlk 20 dakika (emülatörde yaşandı)

| Dakika | Ne oluyor | Oyuncunun hissi |
| --- | --- | --- |
| 0–3 | Prolog: vapur, Deniz, gökyüzü yarılıyor, Yankı bağı. | **Çok iyi.** "Herkese Sistem düştü. Sana ben düştüm." türün kancası. Üç seçenek de karakterli. |
| 1 | İlk seçenek ekranında ipucu kartı + kilitli `[İRD 6]` seçenek. | İlk iki dakikada stat kısaltması ve kilit; henüz stat ekranı görülmedi. |
| 3 | Uyanış: 3 Sistem kartı üst üste (Durum penceresi 30 gün · Gizli satır · Günlük Görev "Daha fazla → Günlük Görev"). | Kartlar alt panelin altında kalıyor; oyuncu seçenekleri metinden önce görüyor. Kurgu içinde menü yönlendirmesi tonu kırıyor. |
| 4–6 | İlk savaş (Kanal Faresi Sv. 3): 9 düğme + Taktik (Ölçülü · Efor 0) + DYN çubuğu + ipucu. HP 105 → 50. | **Bir fare için fazla kokpit.** Savaş günlüğü tekrarlı ("Yaraların yanıyor. Iskaladın." ×3). İnfaz seçimi (Merhamet −3 / Düzen +2 / Merhamet +5) güzel ama sayılar çıplak. |
| 6 | Zafer: 4 ayrı kart (Zafer · +212 XP · +4 Kredi · [Envanter verildi]) anlatının üstüne yığılıyor. Seviye kartı. | Ödül kartları anlatıyı ekrandan itiyor. |
| 7 | İlk gece → **Gün 1 sabahı kırmızı kart: "Günlük Görev tamamlanmadı. Ceza protokolü değerlendiriliyor."** | Oyuncu daha ne yapacağını öğrenmeden cezalandırılıyor (Gün 0 prologdu). Gün 2 sabahı aynı kart yine. |
| 7 | "Öğretici Katman'da 29 gün kaldı" + bölüm satırı "11 gün" + başlıkta "365 gün". | **Üç saat.** Hangisi ölüm, hangisi ceza belli değil. |
| 8 | Brifing 1: önce 3 Sistem kartı (Yeni görev Çıkış Kapısı · "Gün akışı başladı" · "Gün 1 · 2 yuva"), sonra metin. | "Gün akışı", "yuva" iç terim. |
| 9 | **Gün akışı paneli:** üst yarı boş ("Bugün ne yapacaksın?"), altta Kalan eylem/Tehlike, ipucu, büyük mavi **Operasyon** düğmesi, 1 kart, Antrenman/Dinlen, Antrenman/zaman atlama ⏩, Yolculuk →. | Hikayeden yönetim paneline düşüş. Operasyon düğmesi en büyük ve en parlak: oyuncu Gün 1'de ona basar. |
| 10–12 | Kart "Tanıdık bir ses" (Deniz): güzel sahne, ardından Sv. 2 ile **2 × Sv. 5 tazı** (Korku). Kazanıldı; 5 ödül kartı daha. | İkinci savaş da aynı infaz menüsü. |
| 13 | Yolculuk → Harabe Çarşı: varış sahnesi tek düğme ("Çarşıya gir"); varınca **Kalan eylem 0/2, "Hava kararıyor. Kamp kurma vakti."** | Yolculuk bir eylem harcıyor; 2 eylemlik günde yolculuk günü yiyor. Yolculuk penceresi şu an bulunulan bölgeyi de listeliyor. |
| 14 | Gün 2 Çarşı'da: el boş; Keşfet → "Keşif sakin geçti… +2 Kaya Tuzu, +10 XP". | **Hikayesiz an.** `30_carsi_istasyon.yaml`'da `kesif` etiketli olay yok: Perde 1'de Keşfet hiçbir zaman hikaye çekmiyor. |
| 15 | Üsse dön (bedava) → üste 2 kart var. Operasyon'a bas → onay yok, Sv. 3 ile **Sis Dokuyucu Sv. 8 + 2 tazı**. | Oyuncu Gün 2'de, seviye 3'le bölüm operasyonuna girebiliyor. |
| 16–19 | Yenilgi sahnesi (CAN 1, ağdaki çocuklar; çok iyi yazılmış) → **tam o anda "Durmuş Saat" ceza sahnesi** araya giriyor (Günlük Görev ihmali) → Kırılma uyanışı (cam, F harfi; üç yol) → savaş baştan, 1 HP ile zafer. | Doruk anının ortasına ceza kesintisi. Operasyon Gün 2'de bitti: kalan 10 gün "Sonrası". |

**Sayım:** ilk saatte oyuncu **~70 kavram/ekran/düğme** ile karşılaşıyor (yeni oyuncu raporu, numaralı liste); bunların
en az **25'i** ilk saatte gerekli değil (Dikkat, Yozlaşma, Kule, Öz, Kredi/Öz Kristali, meslek, izler, bilgi günlüğü,
Küresel Sıralama, ordu, Perde, Demir Mod, zaman atlama, Kapılar, Efor/Duruş…).

### 1.2 Bölüm 1–3 (simülatör + emülatör)

10 oynanışın ortalaması (insan benzeri strateji):

| Bölüm | Gün | Olay | Varış sahnesi | Savaş | Hikayesiz gün |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 12 | 26,6 | 7,3 | 11,1 | %13 |
| 2 | 12 | 17,4 | 7,2 | 10,4 | %41 |
| 3 | 4 | 3,4 | 1,0 | 3,0 | %40 |

- **Bölüm 1 boş değil, tıka basa dolu.** 12 güne 27 olay + 7 varış + 11 savaş; Gün 6 ve 12'de tek günde 5–6 olay ve
  3–4 savaş var (kayıt, oynanış 8). Oyuncu "yetişemiyorum" hissine kapılır. Operasyon ise Gün 2–4'te giriliyor; kalan
  8–10 gün "Sonrası/Bitti" evresinde kart tüketimi.
- **Bölüm 2** tersine: hazırlık günleri (14–20) neredeyse yalnız Keşfet/Kapı savaşı (hikayesiz gün %41); operasyon
  günü (21) ise 4 olay arka arkaya (Deniz'in son seçimi, Yeni tavan, darboğaz, bir tohum). Dengesiz.
- **Bölüm 3** 4 gün: brifing → 2 gün keşif → Bekçi. 10 oynanışın bir kısmında Bekçi savaşı ilk denemede kayıp (G27
  yenilgi, G30'a atlama = kayıt noktasından dönüş).
- Savaş kayıp oranı: Gün 0–9 %17, Gün 20–29 %27, **Gün 30–39 %44**, 40–49 %26. Perde 2 girişi (Kıyıkent) en sert
  nokta.

### 1.3 Bölüm 4–18 (özet)

Hikayesiz gün payı (10 oynanış): 4 %12 · 5 %33 · 6 %37 · 7 %45 · 8 %49 · 9 %46 · 10 %31 · 11 %44 · 12 %45 ·
13 %56 · 14 %57 · 15 %54 · 16 %25 · 17 %30 · 18 %41. Hedef %35'in altında yalnız 1, 4, 5, 10, 16, 17.

Ritmin düştüğü yerler:
- **Varış sahnesi enflasyonu.** Oynanış başına ~690 olay başlangıcının ~300'ü varış sahnesidir; Bölüm 6–8 ve 11'de
  bölüm başına 21–24 varış (olaydan fazla). İlk/sonraki ziyaret varyasyonu var ama ikinci paragraf hep aynı ve her
  seferinde bir düğme ister.
- **Uzun bölümler (7, 8, 9, 12, 13, 15: 25–32 gün)** tarihli operasyon bekliyor; hazırlıklar 1–2 gün yetiyor, kalan
  günler Keşfet/Kapı/Kule. Kat anları ve bölge olayları payı düşürdü ama %45–57 hikayesiz gün kaldı.
- **Tekrar eden savaşlar:** her Kule katı, her Kapı, her Keşfet aynı savaş ekranı ve aynı 3 seçenekli infaz menüsü.
  Ezme tek satırla bitiyor ama yine de bir yuva ve bir ekran.
- **Kesinti türleri:** (a) zaman atlama özetleri doğru yerde (üs) ama `cozumle.py` bunları konum uyumsuz sayıyor
  (308 b'); (b) kat anları savaştan hemen sonra geliyor (259 c; tasarım gereği, sorun değil); (c) 220/333/339/340. gün
  ana yay düğümleri (yukselenler_3_ayin, elif_baris_masasi, aras_s_kademe, yukselenler_5) fraksiyon yan görevinin
  ortasına düşüyor (14 d); (d) `elif_demir_gece_uyari` → doğrudan `vahit_maske_duser` bağlamsız geçiş (4/10 oynanış).
- **Zaman atlama** insan benzeri stratejide seyrek (bölüm başına 1–5), kazancı düşük; oyuncu için "beklemenin kısa
  yolu" ama kendini anlatmıyor (metin "kaçırdığın Günlük Görevler sayılmaz" muğlak).

---

## 2. Hikaye

### 2.1 Omurga okunurluğu
Perde 1–2 güçlü ve gerekçeli: ağ → Kemik Taç → Bekçi → Kıyıkent → Kule → Çekirdek → Kordon → Yankı konuşur →
Sistem gerçeği → gemi. Perde 3'ün ortası (11 dünya seçimi, 12 Yol) "sıra geldi" hissi veriyor; 12'nin gerekçesi yalnız
mekanik. Perde 4 (16–18) yeniden toparlıyor.

Başlıklar: 7 "Küçük Harfler" operasyondan sonra anlam kazanıyor; **8 "Başlık" anlaşılmaz** (hedef "merdiven" diyor,
brifing merdiveni anmıyor); 14 "Ortak Masa" en net.

### 2.2 Kopukluklar (continuity-checker; dosya:satır raporda)
Yüksek:
1. **Kıyıkent düşerse** (`kiyikent_dustu`) üs Kadim'in elinde ama darboğaz 16/17 ve brifing 17/18 hâlâ Liman'da
   geçiyor; Kaya'nın telsizi koşulsuz (`09_bolumler_p3_4.yaml:395`).
2. **Bölüm 11 darboğazı** (`deniz_canavar_mi_tanri_mi`) Batık Takımada / Pirinç Kervan seçimini hiç anmıyor; bölümün
   tek büyük seçimi kayboluyor.
3. **`darbogaz_17` ve brifing_18 "merdiven kapandı" satırı ulaşılamaz:** her seçenek `cekirdek_yolu_acik` koyuyor.
4. **Susturulmuş Yankı** (`yanki_susturuldu`) yalnız yapısal sahnelerde gözetiliyor; operasyonlarda ve havuzda konuşmaya
   devam ediyor; `yol_olusumu:30` ölü paragraf.
Orta: Bekçi bayrakları savaştan önce konuyor (yenilgide brifing_4 "Bekçi'yi geçtin" der); Bölüm 7 "kes/sor" sonuçlarına
brifing_8 ve darbogaz_7'de satır yok; `50:2415` "yüz beş gün" sabit sayı; brifing_5 kaçan oyuncuya da "kapısını tuttun".
Düşük: sabit gün sayıları (`08:166`, `09:358`, `50:2726`), Kordon akıbeti Bölüm 6 darboğazında yok, Elif `npc_hayatta`
eksikleri.

### 2.3 Zayıf ya da gereksiz düğümler
- `darbogaz_17` (ulaşılamaz).
- `perde2_son` (Bölüm 9): izleyip tepki veren sahne; iki hazırlığın (sığınak stoku, gözcü) sonucu küçük.
- Bölüm 12 `yol_olusumu`: hikaye gerekçesi yok.
- Bölüm 6 darboğazı bölümün olayına değil ayrı Deniz koluna bağlanıyor.
- Bölüm 18 brifingi yalnız atmosfer.
- Prolog/ilk sabahta Sistem'in menü yönlendirmeleri ("Ayrıntılar: Daha fazla → Günlük Görev").

### 2.4 Güçlü anlar (korunmalı)
Prolog Yankı bağı · Kırılma (zamanın dışına düşmek, camdaki F, üç yol) · `bekci_deniz_hat` ("Hat bende, çekirdek
sende") · `yanki_ortaya_cikis` (bülten küçük harfe döner) · `deniz_canavar_mi_tanri_mi` (yan yatmış vapurda simit) ·
`cekirdege_yol` (merdivenin her basamağı bir Sistem penceresi) · Zehirli bereket ("sabır dersini geçtin. öbür dersten
kaldın.") · Deniz'in "kafanda bir kiracı var" satırı.

---

## 3. Karmaşıklık envanteri

G = gerekli · S = sadeleştirilebilir · Ç = çıkarılabilir (ya da Perde 2+'ya ertelenebilir). "İlk saat" = oyuncu ilk
saatte görüyor mu.

| Sistem / ekran / kavram | İlk saat | Karar | Gerekçe |
| --- | :-: | :-: | --- |
| Sistem penceresi, Yankı satırı, sezgiler [ALG] | evet | **G** | Oyunun kimliği. Sezgi "[ALG]" ile kilit "[ALG 6]" aynı biçimde; ayrılmalı (S). |
| Seviye, XP, kademe, serbest stat puanı | evet | G | Türün omurgası. Kademe harfi ilk saatte açıklanmıyor (ipucu yeter). |
| Sınıf teklifi (Sv. 10), evrim, Benzersiz yetenek | hayır | G | Büyük anlar; kendi ekranı var. |
| Ahlak eksenleri (3) | evet | G | Sonları belirliyor. İnfaz menüsünde çıplak sayılar (S: sayıyı gizle, yönü söyle). |
| **İnfaz seçimi (her savaş sonunda 3 seçenek)** | evet | **S** | Her sıradan savaşta aynı menü. Yalnız boss/zeki düşman/ilk kez; sıradan düşmanda otomatik "temiz bitir". |
| **Savaş ekranı: 9 düğme + Taktik (Duruş + Efor) + Dayanım** | evet | **S** | İlk farede fazla. Yetenek/Konuş/Eşya kilitli gösteriliyor. Perde 1'de Saldır/Savun/Analiz/Kaç/Otomatik; Taktik ve Ortam Sv. 10'dan (ya da ilk 3 savaştan) sonra açılsın. |
| Ezme | hayır | G | Grind'i yok ediyor. Daha da ileri: çok altta kalan düşmanla savaş ekranı hiç açılmasın (S). |
| **Günlük Görev + 7'lik seri + Durmuş Saat cezası** | evet | **S/Ç** | Oyuncu Gün 1 ve 2 sabahı cezalandırılıyor; Durmuş Saat doruğun ortasına giriyor (emülatörde Kırılma yenilgi sahnesinin ortasında). Öneri: cezayı kaldır (Ç), seri bonusu kalsın; ya da ceza yalnız üste, operasyon dışında. |
| Fırsat eli (kartlar, ● gerilim) | evet | G | Ana oyun döngüsü. ● açıklamasız (S: ipucu). |
| Keşfet / Antrenman / Dinlen | evet | G | Perde 1'de Keşfet hikaye çekmiyor (`30_carsi_istasyon` `kesif` etiketi yok): ya olay etiketle ya Perde 1'de Keşfet'i kaldır. |
| **Yolculuk (ayrı eylem) + varış sahnesi** | evet | **S** | 2 eylemlik günde yolculuk günü yiyor; varış sahnesi oynanış başına ~300 kez. Öneri: ilk ziyarette sahne, sonrakilerde tek satır bildirim; yolculuk bir kart/görev kartıyla birleşsin (kart zaten oraya götürüyor). |
| Üs / görev / "Üsse dön" | evet | G | Yapı doğru; etiket renkleri (pembe "Keşif · …") açıklanmıyor. |
| Bölüm satırı, son tarih, taşma | evet | G | Ama **üç saat** (365 · 30 · bölüm). Öneri: başlıkta 365, oyunda yalnız bölüm satırı; "Öğretici 29 gün kaldı" satırını kaldır. |
| **Operasyon düğmesi (erken girilebilir bölümler)** | evet | **S** | Gün 1'de en parlak düğme; Sv. 3 ile Sv. 8 bossa onay almadan giriyor. Öneri: önerilen seviye/uyarı ("Hazır mısın? Kamp seviyen 3, tehdit 8") ya da en erken giriş günü (açılış + 3). |
| Hazırlık eylemleri (tarihli operasyon) | hayır | G | İyi fikir; Perde 2+. Bölüm 2 brifingi "Hazırlan" diyor ama hazırlık düğmesi yok (metin). |
| **Antrenman / zaman atlama** | evet | **S** | Gün 1'de görünmemeli (Bölüm 1 zaten dolu). Perde 2'den itibaren; metni netleştir. |
| Kapılar (n), kapı taşması | Perde 2 | G | Perde 1'de düğme yok, iyi. |
| **Kule, hızlı tırmanış, kat anları (55 olay)** | hayır | G/S | Kule özdeş; kat anları hikayesiz günü düşürdü. Hızlı tırmanış kalsın; tek tek kat düğmesi "ileri seçenek" olabilir. |
| Saha kampı sahneleri (24) | hayır | G | Oynanış başına ~9; maliyetsiz. |
| **Dikkat (Anomali) + eşik olayları** | Durum'da | G | OP MC gerilimi. Ama Perde 1 Gün 1'de Durum'da "Gizli ölçüler" görünmemeli; ilk eşik (25) gelene kadar gizle (S). |
| **Yozlaşma** | Durum'da | **S/Ç** | Yalnız karanlık yetenek ve Yankı yolu; 2 eşik olayı. Dikkat'le birleştirilebilir ya da yalnız karanlık eğilim açılınca görünsün. |
| **Gizli / görünen seviye, Gizle / Açığa vur, gösteriş** | Gün 12 | G | OP MC'nin kalbi. Sınır kırılması anı var. |
| Küresel Sıralama + bildirimler | Daha fazla | **S** | Gün 0'da "#6 / 6" görünüyor. `siralama_gunu` olayına kadar karo gizlensin. |
| **Ordu** | Durum/Perde 3 | **S/Ç** | `army.yaml`, `ordu_kazan` etkileri, hazırlıklarda +5/+10/+20 birlik, `ordu_min` koşulu. Oyuncu sayıyı görüyor, yönetmiyor. Ya bir "müttefik sayısı" satırına indir ya da Perde 4 istilasında tek sahne olarak kullan. |
| **İzler (oyuncu mutasyonu / NPC yarası)** | Kırılma'da | S | "İz: Yankı Damarı" güzel; İlişkiler'de ayrı liste gerekmez, hatıraların içinde dursun. |
| **Bilgi günlüğü + yalanlar** | Günlük | **S/Ç** | Diyalog açan bilgi az; yalan mekaniği oyuncuya hiç görünmüyor. Bilgiler Günlük'te kalsın, ayrı "bilgi günlüğü" başlığı kalksın. |
| **Meslek (Simyacı… 5 dal) + Çanta üretimi** | Çanta "Sv. 5'te teklif" | **Ç** | Simya malzemesi kaynağı hiç kurulmadı (PROGRESS Aşama 2 "bilinen eksik"); Keşfet'in verdiği Kaya Tuzu bunun için. Zaman atlama odağı da var. Çıkarılırsa Keşfet ganimeti, Çanta sekmesi ve Kredi büyük ölçüde sadeleşir. |
| **Kredi + Öz Kristali (iki para)** | "+4 Kredi" Gün 0 | **S** | Mağaza yok; Kredi yalnız birkaç seçenekte. Tek kaynağa in (Öz Kristali) ya da Kredi'yi kaldır. |
| Öz (D kademesi) + Yol (A) | Perde 2–3 | G | Bölüm 12'nin omurgası; iyi. "Yuva 0/0" Çanta'da Gün 0'da görünmesin. |
| İlişkiler: 4 eksen, hatıralar | Gün 0 "[BAĞ] Yankı · Saygı +10" | S | Eksen kartları her sahnede 1–3 tane. Öneri: sahne sonunda tek birleşik satır ("Deniz sana daha çok güveniyor"). |
| Yoldaş tepkileri, kamp, yoldaş kapasitesi | Gün 1 | G | — |
| Fraksiyon itibarı (5) + lider fraksiyon | Bölüm 1 | G | Matris oyuncuya görünmüyor, sorun değil. |
| Demir Mod | başlık | G | Tek anahtar; kalsın. |
| New Game+ / Döngü sonu | bitiş | G | Maliyetsiz. |
| Bildirimler (Android) | Ayarlar | G | Kapalı varsayılan. |
| Harita | Daha fazla | S | Perde 1'de 3 bölge; Yolculuk penceresi zaten liste. |
| Daha fazla karoları (8) | Gün 0 | S | Gün 0'da Küresel Sıralama, İlişkiler 0/1, Günlük 0 görev: boş karolar. İlgili sistem açılınca görünsün (kendini açan arayüz kuralı zaten var). |
| Yardım/sözlük (6 liste), ipucu kartları | her ekran | S | Oyuncu (i)'yi bilmiyor; ipucu kartı tek satır. İlk haftada 3–4 kavramı sahne içinde Sistem söylesin. |

**Toplam:** 70 kavramdan ~15'i çıkarılabilir ya da ertelenebilir, ~20'si sadeleştirilebilir; omurga (Sistem penceresi,
seviye/kademe, sınıf, ahlak, kartlar, bölüm/üs/görev, gizli seviye, Dikkat, yoldaşlar, Öz/Yol, sonlar) dokunulmadan kalır.

---

## 4. Öneriler (önceliklendirilmiş)

Etki: oyuncunun ilk saati ve 20 saatlik ritim. Maliyet: K (saatler) · O (1 gün) · B (2+ gün, içerik yazımı).

### 4.1 Çıkarma / erteleme

| # | Ne | Neden | Dosyalar | Etki/Maliyet | Risk |
| --- | --- | --- | --- | --- | --- |
| Ç1 | **Durmuş Saat cezasını kaldır**; Günlük Görev kaçırılınca yalnız seri sıfırlansın. Hiçbir cezalı/tohum sahnesi operasyon içinde açılmasın. | Gün 1–2'de haksız ceza; doruğun ortasına kesinti (emülatörde görüldü). GDD kuralı "ölüm haddini aşmaktan gelir" ile çelişmiyor. | `story/Effects.kt` (PENALTY_CHANCE), `StoryEngine.kt:695`, `story/90_sistem.yaml ceza_bolgesi`, `system.yaml gunluk_kacirildi` | Yüksek / K | Solo Leveling göndermesi kaybolur; DECISIONS #98 ile çelişir (kullanıcı kararı gerek). |
| Ç2 | **Meslek sistemini ve Çanta üretimini çıkar** (ya da New Game+'a sakla). | Malzeme ekonomisi kurulmamış; Keşfet ganimeti, iki para ve zaman atlama odağı bunun için var. | `professions.yaml`, `items.yaml` tarifler, `PlayerActions` üretim, `BagScreen`, `TimeSkip` meslek odağı, Sv. 5 teklif istemi | Orta / O | Zanaat eğilimi sınıf teklifine girdi veriyor; eğilim kalır, meslek gider. Kayıt geçişi (`VERSION`). |
| Ç3 | **Kredi'yi kaldır, tek kaynak Öz Kristali.** | Mağaza yok; "+3 Kredi" kartları gürültü. | `items.yaml`, seçeneklerde `kredi` etkisi (grep), `BagScreen currency_line`, SaveCodec | Orta / O | Birkaç seçenek kredi koşullu (`kredi` grep ile sayılacak). |
| Ç4 | **Yozlaşma'yı Dikkat'in içine kat** ya da yalnız karanlık eğilim/Yankı yolunda görünür yap. | İki ölçek, ikisi de "gerilim". Yozlaşma eşiği 2 olay. | `WorldState`, `Director` eşikler, `StatusScreen status_corruption`, `yozlasma_*` olaylar | Orta / O | Karanlık build'in kimliği zayıflar; "görünür yap" seçeneği daha güvenli. |
| Ç5 | **Ordu'yu sayaçtan çıkar**; istila bölümünde tek bir "yanında kaç kişi var" paragrafı. | Oyuncu yönetmiyor, yalnız sayı görüyor. | `army.yaml`, `ordu_kazan`/`ordu_min`, `StatusScreen status_army`, Bölüm 10/15/16 hazırlıkları | Düşük-orta / O | Yalnız Avcı/Demir Taht koşulları ordu kullanıyorsa gözden geçir. |
| Ç6 | **Bilgi günlüğü ve yalan mekaniğini arayüzden kaldır**; bilgiler Günlük'te hatıra gibi görünsün. | Oyuncuya görünmeyen sistem. | `MoreScreen` bilgi bölümü, `knowledge.yaml` kalır | Düşük / K | Yok. |
| Ç7 | Daha fazla'da **Küresel Sıralama, İlişkiler, Harita karolarını ilgili sistem açılınca göster**; Gün 0'da gizle. | Boş karolar ("#6 / 6", "0 / 1"). | `MoreScreen`, `StatusView.tabs` benzeri bayrak | Orta / K | Yok. |
| Ç8 | Prolog ve ilk sabahta **menü yönlendiren Sistem satırlarını kaldır** ("Daha fazla → Günlük Görev", "İpucu: Statlarını Durum sekmesinde…"). | Ton kırıyor; ipucu kartları zaten var. | `story/00_prolog.yaml:133,144` | Düşük / K | Yok. |
| Ç9 | "Öğretici Katman'da N gün kaldı" ve "Yeni görev: Çıkış Kapısı (30 gün)" satırlarını kaldır; **tek saat bölüm satırı**. | Üç saat. | `story/00_prolog.yaml ilk_sabah`, `quests.yaml ogretici_cikis`, `system.yaml` | Orta / K | Bekçi'nin 28. gün bağlamı brifing_3'te zaten anlatılıyor. |

### 4.2 Sadeleştirme

| # | Ne | Neden | Dosyalar | Etki/Maliyet | Risk |
| --- | --- | --- | --- | --- | --- |
| S1 | **Savaş ekranını kademeli aç:** Perde 1'de Saldır · Savun · Analiz · Kaç · Otomatik; Taktik (Duruş/Efor) ve Ortam ilk 3 savaştan ya da Sv. 10'dan sonra; Yetenek/Eşya/Konuş kilitliyse gizli. Dayanım çubuğu Taktik'le birlikte gelsin. | İlk farede 9 düğme. | `CombatScreen.kt`, `CombatEngine` (duruş varsayılan Ölçülü zaten) | Yüksek / O | Ortam bazı erken savaşlarda zaaf çözümü; kontrol et. |
| S2 | **İnfaz menüsü yalnız boss, zeki düşman ya da ilk 2 savaşta**; sıradan düşmanda otomatik "Temiz bitir" (ahlak etkisiz). | Her savaş aynı 3 seçenek. | `CombatEngine` infaz fazı, `system.yaml` | Yüksek / K | Merhamet/Düzen kazanç kaynağı azalır: `DengeTest` çalıştır. |
| S3 | **Ödül kartlarını birleştir:** "Zafer · +335 XP · +3 Kredi · Sis Postu" tek kart; `[BAĞ]` kartları sahne sonunda tek satır ("Deniz sana daha çok güveniyor"). | Anlatı ekrandan itiliyor; 4–5 kart üst üste. | `StoryScreen.kt` bildirim gruplama, `Views.kt NoticeView` | Yüksek / K-O | Erişilebilirlik duyurusu tek olur, iyi. |
| S4 | **Varış sahnesi yalnız ilk ziyarette**; sonrakilerde tek satır bildirim, düğme yok. Yolculuk eylem harcamasın ya da görev kartına basınca yolculuk otomatik olsun (zaten öyle), serbest Yolculuk düğmesi yalnız keşif için kalsın. | Oynanış başına ~300 varış; 2 eylemlik günde yolculuk günü yiyor. | `WorldEngine` varış kuralı, `events/07–09 varis_*` (`tekrar`), DECISIONS #131 yuva kuralı | Yüksek / O | Hakimiyet %60 sınırı (dönüş yuvası tartışması #131); simülatörle ölç. |
| S5 | **Operasyon girişine kapı:** erken girilebilir bölümlerde (1, 2, 3, 7, 8, 11, 12, 14) düğme açılış + 3. günden önce görünmesin ya da onay penceresi "Tehdit: Sv. 8 · Sen: Sv. 3" uyarısı versin. Düğmeyi panelin en altına, kartların altına al. | Oyuncu Gün 1–2'de Sv. 3 ile giriyor; 10 gün boş kalıyor. | `WorldEngine.operationChoices`, `HubPanel.kt` sıra, `chapters.yaml` (`erken_gun` alanı) | Yüksek / K-O | Tasarım kararı #135'i daraltır (kullanıcıya soru). |
| S6 | **Zaman atlama düğmesi Perde 2'den itibaren** ve yalnız el boşken. | Gün 1'de gereksiz; Bölüm 1 dolu. | `HubPanel.kt`, `WorldEngine` skip koşulu | Orta / K | Yok. |
| S7 | **Durum ekranı Perde 1'de üç sekme yerine tek sayfa**; "Gizli ölçüler" (Dikkat/Yozlaşma/görünen seviye) ilk Dikkat eşiği ya da ilk [Gizle] seçeneğinden sonra görünsün. | Kendini açan arayüz kuralı Durum'a da uygulanmamış. | `StatusScreen.kt`, `StatusView` | Orta / K | Yok. |
| S8 | **Sezgi ile kilit etiketini ayır:** sezgi satırı "[ALG]" öneki yerine yalnız turkuaz çizgi; kilit "[ALG 6]" kalsın. Yardım'da tek satırla açıkla. | Yeni oyuncu ikisini karıştırıyor. | `StoryScreen.kt` sezgi satırı, `system.yaml` | Düşük / K | Yok. |
| S9 | **"yuva" sözcüğünü tek anlama indir:** eylem için "eylem" (zaten panelde), Öz için "yuva" kalsın; `system.yaml` "Gün {day} · {n} yuva" → "{n} eylem". "Gün akışı başladı", "Anomali Dikkati" gibi iç terimleri oyuncu diline çevir. | Üç anlamlı sözcük. | `system.yaml:136,139,155`, `strings.xml` | Düşük / K | Yok. |
| S10 | **Bölüm hedefi tek metin:** `chapters.yaml hedef` ile brifing bildirimi ("Sisli Orman'daki yuva büyüyor…") aynı cümle olsun; bölüm satırında kısaltılmış hali. | İki farklı hedef metni. | `chapters.yaml`, `07_bolumler_p1.yaml:30` ve diğer brifingler | Düşük / K | Yok. |
| S11 | **Perde 1 Keşfet'e hikaye bağla:** `30_carsi_istasyon.yaml` ve `20_orman.yaml` olaylarının yarısına `kesif` etiketi; ya da Perde 1'de Keşfet düğmesi yerine bölge kartları. | Çarşı'da Keşfet yalnız Kaya Tuzu veriyor. | `events/20_orman.yaml`, `30_carsi_istasyon.yaml` (etiket) | Orta / K | Bölüm 1 zaten dolu: önce S4–S5 ile boşalt. |
| S12 | **Yolculuk penceresinden bulunulan bölgeyi çıkar.** | Bug. | `HubPanel.kt` / `HubView.travel` | Düşük / K | Yok. |
| S13 | **Bölüm 1 yoğunluğunu yay:** fırsat eli Perde 1'de günde en çok 1 kart + 1 tohum; Gün 6/12'deki 5–6 olay yığılması dağılsın. | 12 güne 27 olay. | `Director` el boyutu (perde 1), `WorldEngine` zorunlu olay kilidi | Orta / K-O | Olayların bir kısmı Bölüm 2'ye kayar; `Perde1Test` erişim oranı. |

### 4.3 Düzeltme / ekleme (hikaye)

| # | Ne | Dosyalar | Etki/Maliyet |
| --- | --- | --- | --- |
| D1 | Kıyıkent düştü dalı: darboğaz 16/17 ve brifing 17/18 için Tepe'de geçen 3–5 koşullu paragraf; `darbogaz_16:395` koşulu. | `09_bolumler_p3_4.yaml`, `80_perde4.yaml` | Yüksek / O |
| D2 | Bölüm 11 darboğazına Batık/Kervan sonucunu anan 2–4 paragraf. | `56_deniz_yanki_aras.yaml:1431–1730` | Yüksek / K |
| D3 | `cekirdek_yolu_acik` yenilgi/taşma dalında `bayrak_kaldir`; ya da `darbogaz_17`'yi kaldır. | `80_perde4.yaml:1418–1530`, `chapters.yaml:397` | Orta / K |
| D4 | `yanki_susturuldu` gözetimi: Yankı sezgi/satırlarına `bayrak_yok`, `50:2418` seçeneğine `bayrak_kaldir`. | `71_perde3.yaml`, `50_perde2_ana.yaml`, Yankı sezgileri (grep) | Orta / O |
| D5 | Bekçi yenilgi bayrağı + Deniz'in hatta ölümü brifing_4'te; Bölüm 7 `yanki_gorundu` yedek satırları; sabit gün sayılarını yumuşat. | `40_ana_yay.yaml`, `08_bolumler_p2.yaml`, `50:2415,2726`, `09:358` | Orta / K |
| D6 | Bölüm 8'i yeniden adlandır ("Çekirdeğin Altı"), brifing_8'e merdiven satırı; Bölüm 12'ye hikaye gerekçesi (Thessar ya da Yankı'nın bir cümlesi). | `chapters.yaml`, `08_bolumler_p2.yaml:141–160`, `71_perde3.yaml` | Orta / K |
| D7 | Perde 2 girişinin sertliği (Gün 30–39 savaş kaybı %44): `kiyikent_*` ilk bölge düşmanlarının seviyesi ya da Bölüm 4 kartlarının savaş oranı. | `regions.yaml`, `60_kiyikent_a.yaml`, `enemies/` | Orta / O (simülatörle) |
| D8 | Ana yay günleri 220/333/339/340 düğümleri fraksiyon yan görevini kesmesin: `Director.forced` yan görev içinde beklesin. | `WorldEngine`/`Director` | Düşük / K |

### 4.4 Önerilen sıra (onaylanırsa)
1. **İlk saat paketi (K–O):** Ç1, Ç7, Ç8, Ç9, S1, S2, S3, S5, S6, S7, S8, S9, S10, S12. Tek oturumda; sonra emülatörde ilk
   20 dakika yeniden oynanır.
2. **Ritim paketi (O):** S4, S13, S11, D7, D8; her adımdan sonra `serbest.py` ve `DengeTest`.
3. **Hikaye paketi (O):** D1–D6 (continuity raporu dosya:satır ile hazır).
4. **Sistem çıkarma paketi (O–B):** Ç2, Ç3, Ç4, Ç5, Ç6; kayıt sürümü artar, eski kayıt geçişi yazılır.

---

## 5. Kullanıcıya sorular

1. **Günlük Görev cezası (Durmuş Saat) kalksın mı?** Seçenekler: tamamen kaldır (seri bonusu kalır) · yalnız üste ve
   operasyon dışında · olduğu gibi kalsın.
2. **Meslek / üretim sistemi çıkarılsın mı?** Çıkarsa Kredi de gidebilir (tek kaynak Öz Kristali). Yoksa malzeme
   ekonomisi yazılması gerekir (B maliyet).
3. **Yozlaşma ayrı ölçek kalsın mı?** Dikkat'e katılsın / yalnız karanlık yolda görünsün / kalsın.
4. **Operasyona erken giriş:** en erken açılış + 3 gün mü, onay penceresi mi, serbest mi kalsın (DECISIONS #135'i
   daraltır)?
5. **İnfaz menüsü** yalnız boss/zeki düşmanda mı, her savaşta mı?
6. **Varış sahnesi** yalnız ilk ziyarette mi; yolculuk eylem harcamasın mı (hakimiyet %60 sınırı yeniden ölçülecek)?
7. **Ordu ve bilgi günlüğü** arayüzden kalksın mı (motor verisi kalır)?
8. **Sıra:** önce ilk saat paketi mi, önce hikaye kopuklukları mı?
