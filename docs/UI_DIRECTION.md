# Arayüz yönü: görsel kimlik önerileri

Durum: **A + C birleşimi ("Kırık pencere") seçildi ve uygulandı** (DECISIONS #147–#148). Tokenlar `ui/theme/Theme.kt`,
taslaklar `docs/ui/yonler.html` ve `docs/ui/a_c_birlesim.html`.

## Bugünkü durum (çıkış noktası)

- `ui/theme/Theme.kt`: Material 3 varsayılanına yakın lacivert/camgöbeği şema, dört tema (Sistem · Açık · Koyu · AMOLED).
- Yazı: anlatı `FontFamily.Serif`, Sistem `FontFamily.Monospace`. Cihazın yazı tipine bağlı, uygulamaya özgü bir yazı yok.
- Bildirim renkleri tür başına tek tek seçilmiş (`noticeColor`). Yankı mor (`EchoColor`) ve birleşik çizgiyle "bozuk".
- Kutlama kartları (`system/Overlays.kt`) işlevsel ama türler arasında hissi pek ayrışmıyor.
  Seviye atlama, evrim ve sınıf aynı çerçeveyi paylaşıyor.

Eksik olan bir **kimlik**. Oyunun özü iki sesin çatışması: soğuk, bürokratik **Sistem** ve ona bağlı, kırık, fısıldayan
**Yankı**. Üstüne bir de kahramanın sınırları aşan bir anomali olması (OP MC, DECISIONS #19) var. Aşağıdaki üç yönün her biri
bu üçünü farklı bir dille anlatıyor.

## Bütün yönler için ortak kurallar

- **Okunabilirlik önce gelir.** Uzun anlatı metni seriftir ve 17 sp / 1,6 satır aralığıyla başlar.
  `Settings.textSize` ile büyür. Satır uzunluğu tablette en çok ~70 karakter olur (ortalanmış sütun).
- **Kontrast.** Gövde metni WCAG AA'yı (4,5:1) karşılar. Vurgu renkleri metin olarak kullanılıyorsa AA'yı karşılar,
  karşılamıyorsa yalnız çizgi ve parıltıda kullanılır. AMOLED ve Açık tema da aynı tokenlardan türer.
- **Hareket.** Cihazda "animasyonları kaldır" açıksa (`ANIMATOR_DURATION_SCALE = 0`) bütün kutlamalar anında,
  hareketsiz gösterilir. Hiçbir bilgi yalnız hareketle verilmez.
- **TalkBack.** Bozuk Yankı yazısı ekran okuyucuya düz metin olarak gider (bugünkü gibi). Kutlama kartları
  `liveRegion` ile duyurulur. Dekoratif katmanlar (tarama çizgisi, parıltı) erişilebilirlik ağacının dışındadır.
- **Yazı tipleri** uygulamaya gömülür (`res/font/`), lisans dosyası `app/src/main/assets/licenses/` altına ve
  Ayarlar → Lisanslar'a girer. Çalışırken indirme yoktur. Aday yazıların hepsi SIL Open Font License 1.1'dir.
  Kurulumda Türkçe glifler (ı İ ş Ş ğ Ğ ç ö ü) tek tek doğrulanır. Desteklemeyen yazı elenir.
- **Büyük anlar** (her yönde ayrı ayrı tasarlanır):
  1. seviye atlama
  2. evrim (kademe atlama)
  3. Benzersiz yetenek
  4. Küresel Sıralama değişimi
  5. sınır kırılması: gizli/görünen seviye farkının açığa çıkması, Ezme, "imkânsız" bir eşiğin aşılması

---

## Yön A · "Soğuk Holografik Arayüz" (Sistem penceresi)

Oyuncu Sistem'in kendi arayüzünün içindedir. Ekran, gözünün önünde asılı duran bir pencere gibidir. Temiz, kesin,
biraz da tehditkâr.

**Renk.**
- Zemin derin gece mavisi: `#070B14`, yüzey `#0E1524`.
- Tek baskın vurgu: buzlu camgöbeği `#7FE7FF`, parıltıda `#38D6FF`.
- Uyarı kehribar `#FFB547`, tehlike kızıl `#FF5A5F`.
- Nadirlik ölçeği soğuktan sıcağa gider: gri → camgöbeği → menekşe → altın → kızıl → beyaz-altın (Benzersiz).
- Yankı bu soğuk düzende tek "yanlış" renktir: elektrik macentası `#FF3DCB`, hafif renk kaymasıyla (kırmızı/mavi kanal ayrılması).

**Yazı.**
- Anlatı: **Literata** (OFL, uzun okuma için tasarlanmış serif).
- Sistem: **JetBrains Mono** (OFL).
- Başlık ve sayılar: **Chakra Petch** (OFL; köşeleri kesik, "arayüz" hissi veren grotesk).
- Sistem başlıkları büyük harf ve geniş harf aralıklıdır (Türkçe yerel ayarla: İ).

**Sistem kartları.**
- Yarı saydam cam panel: 1 dp camgöbeği kenar, köşeler 45° kesik (yuvarlak değil).
- Sol üstte küçük bir köşe parantezi `⌜ ⌝`.
- Başlık satırı `[ SİSTEM ]` biçiminde. Altında ince bir tarama çizgisi dokusu durur (opaklık %4; metnin altında, okumayı bozmaz).
- Bildirim türü renkli kenar şeridiyle ve küçük bir sembolle ayrılır, renk tek başına taşımaz.

**Büyük anlar.**
- Seviye atlama: ekranın ortasında pencere "açılır" (yatay bir çizgi dikeyde genişler). Sayı eski değerden yenisine
  hızla döner. Stat satırları sırayla yanar.
- Evrim: ekran bir an kararır. Kademe harfi (E → D) eski çerçeveyi kırarak büyür, ardından kart altın kenara geçer.
  Hafif titreşim (haptic) eşlik eder.
- Benzersiz yetenek: kart beyaz-altın parlar. Etrafında `UNIQUE` yazısı dönen bir halka gibi akar.
  Kart, diğerlerinin aksine yuvarlak bir mühürle kapanır.
- Küresel Sıralama: liste satırı yerinden kayar, eski sıra soluk bir hayalet olarak arkada kalır. Yanında ↑ ve fark sayısı görünür.
- Sınır kırılması: arayüz **arızalanır**. Kenarlar titrer, bir an Yankı'nın macentası pencereye sızar, Sistem metni
  `HATA: ÖLÇÜM ARALIĞI DIŞINDA` der. Sonra pencere eskisinden parlak geri gelir.

**Hareket.**
- Kısa ve keskin: 120–220 ms, `FastOutSlowIn`. Paneller kayarak değil, "açılarak" (ölçek + kırpma) girer.
- Yankı satırları harf harf belirir.

**Güçlü yanı:** türün (Sistem pencereli LitRPG) okurlarına hemen tanıdık gelir. Tek vurgu rengi sayesinde okunabilirliği kolay korunur.
**Riski:** "mavi hologram" yaygındır. Kimliği kesik köşeler, tarama dokusu ve Yankı'nın arıza dili taşımalı.

---

## Yön B · "Mühürlü Kodeks" (karanlık grimoire)

Sistem bir kitaptır: eski, kadim, insanlıktan önce yazılmış bir kodeks. Oyuncu her seçimde ona bir satır ekler.
Kadimler, Kule ve Öz kristalleri bu dille çok iyi uyuşur.

**Renk.**
- Zemin isli mürekkep `#110E0C`, yüzey eskimiş deri `#1A1512`. Açık temada parşömen `#F3EBDD`.
- Vurgu yaldız `#D4A84B` ve kan kırmızısı mühür `#9E2B25`.
- Sistem soğuk bir istisna olarak solgun gümüş-mavi `#9FB7C9` yazar. Kitabın içine sonradan kazınmış yabancı bir yazı gibidir.
- Yankı kenar boşluğundaki el yazısı notlar gibidir: solgun menekşe `#B48CD9`, eğik.

**Yazı.**
- Anlatı: **EB Garamond** ya da **Cormorant Garamond** (OFL). Literata da yedek seçenek olarak durur.
  (Garamond küçük boyutta incedir; anlatıda en az 18 sp gerekir.)
- Başlık: **Cinzel** (OFL; Roma büyük harfleri, yalnız kısa başlıklarda).
- Sistem: **IBM Plex Mono** (OFL).

**Sistem kartları.**
- Sayfaya iliştirilmiş bir "levha": koyu zemin, çift ince yaldız çerçeve, köşelerde küçük süs.
- Başlık mühür biçiminde bir rozettir.
- Yankı satırı kenar notu gibi sağa kayık durur. Altı titrek bir çizgiyle çizilir.

**Büyük anlar.**
- Seviye atlama: sayfaya yaldızla bir rakam basılır. Mürekkep yayılarak belirir, ardından mum ışığı gibi titrek bir parıltı gelir.
- Evrim: kitap kapanır, kapakta yeni kademe mühürle basılır ve kitap yeniden açılır.
- Benzersiz yetenek: kan kırmızısı bir mühür kartı kapatır, mühür kırılarak açılır. Altında "Bu sayfa yalnız senindir."
- Küresel Sıralama: bir parşömen listesinde adın bir satır yukarı yazılır, eski satırın üstü çizilir.
- Sınır kırılması: kodeksin sayfası **yanar**. Kenarlardan kor ilerler, yazı yerinden kayar. Sistem'in gümüş yazısı
  okunmaz hale gelir ve yerine Yankı'nın el yazısı geçer.

**Hareket.**
- Yavaş ve ağır: 280–450 ms. Sayfa çevirme, mürekkep yayılma, mühür basma.
- Kutlamaların toplam süresi en çok ~1,2 sn, dokununca geçilir.

**Güçlü yanı:** mobil LitRPG'lerde az görülür, ayırt edicidir. Anlatıyı "roman okuma" hissine çeker.
**Riski:** OP MC'nin modern, sayısal, "istatistik" tarafıyla daha az uyuşur. Sistem pencereleri ve savaş ekranı fazla süslü
kaçabilir. Garamond'un küçük boyutta okunabilirliği dikkat ister.

---

## Yön C · "Kırık Terminal" (minimal, fosfor)

Sistem bir işletim sistemidir. Oyuncu onun kabuğunda çalışan bir süreçtir. Yankı ise bu sistemin içindeki bir **bozulmadır**.
En sade, en hızlı ve en "kod" hissi veren yön budur. OP MC'nin "kuralları kıran hata" fikrini doğrudan görselleştirir.

**Renk.**
- Zemin neredeyse siyah `#0A0D0A`. AMOLED'de saf siyah.
- Tek ana renk fosfor yeşili değil, okunabilirlik için **sıcak beyaz metin** `#E8E6DF`.
- Sistem vurgusu kehribar fosfor `#FFB000`.
- Durum renkleri yalnız sembol ve çizgide kullanılır: yeşil `#7BD88F` başarı, kızıl `#FF6B5B` tehlike.
- Yankı tek bozuk renktir: ters video (açık zemin üstüne koyu harf) ve macenta kayması `#FF4FD8`.

**Yazı.**
- Anlatı: **Source Serif 4** (OFL; ekranda çok net, geniş x-yüksekliği). Bu yönde bile anlatı mono olmaz, çünkü uzun metin
  mono yazıyla yorar.
- Sistem ve arayüz: **Martian Mono** ya da **JetBrains Mono** (OFL).
- Başlık yazısı yoktur. Hiyerarşiyi boyut, kalınlık ve `>` istemleri kurar.

**Sistem kartları.**
- Çerçevesiz bloklardır. Sol kenarda 2 dp renkli dikey çizgi, başta `>` istemi durur.
- Başlık `sys.notice --tür=seviye` gibi değil, okunur kalır: `> SİSTEM · SEVİYE`.
- Satırlar yazıyor gibi akar. Ayarlarda kapatılabilir, erişilebilirlikte kapalıdır.

**Büyük anlar.**
- Seviye atlama: ekranın üstünden bir "derleme" satırı geçer (`████████░░ 100%`). Ardından tek satır büyük kehribar
  sayı gelir, sonra stat farkları `+2 GÜÇ` biçiminde dökülür.
- Evrim: ekran bir an ters videoya döner. Kademe harfi büyük ve tek başına durur, ardından terminal yeniden başlar
  (`yeniden başlatılıyor…`).
- Benzersiz yetenek: kartın içinde başka hiçbir yerde kullanılmayan **beyaz zemin, siyah yazı** bloğu çıkar.
  Sistem kendi kuralını bozmuştur.
- Küresel Sıralama: liste `diff` gibi gösterilir. Eski satır `- 214 {oyuncu}` kızıl, yeni satır `+ 37 {oyuncu}` yeşil.
- Sınır kırılması: ekran gerçekten **çöker**. Satırlar kayar, karakterler rastgele simgelere döner (TalkBack'e düz metin gider),
  Sistem `beklenmeyen değer` der. Yankı'nın küçük harfli tek cümlesi temiz ekrana yazılır.

**Hareket.**
- En az hareket: 80–160 ms, doğrusal.
- Büyük anlarda "kesik kare" etkisi (kısa, adım adım geçişler) kullanılır. Pil ve performans dostudur.

**Güçlü yanı:** en okunur ve en hafif yöndür, küçük telefonda çok iyi çalışır. Yankı'nın "bozulma" kimliği en güçlü bu dilde parlar.
**Riski:** soğuk ve sade olduğu için dünya, Kadimler ve duygu sahneleri sıcaklık kaybedebilir. Dengeyi anlatı serifi ve kehribar sağlamalı.

---

## Karşılaştırma

| | A · Holografik | B · Kodeks | C · Kırık Terminal |
| --- | --- | --- | --- |
| Tür tanıdıklığı | yüksek | orta | orta |
| Ayırt edicilik | orta | yüksek | yüksek |
| Uzun metin okunurluğu | iyi | dikkat ister | çok iyi |
| Sistem ↔ Yankı zıtlığı | güçlü (arıza) | orta (el yazısı) | en güçlü (bozulma) |
| Kutlamaların etkisi | parlak, hızlı | törensel, yavaş | sert, çarpıcı |
| Küçük telefon / performans | iyi | orta | en iyi |
| Gömülecek yazı | Literata, JetBrains Mono, Chakra Petch | EB Garamond, Cinzel, IBM Plex Mono | Source Serif 4, Martian Mono |

**Önerim:** A'nın düzeni (cam panel, kesik köşe, tek soğuk vurgu) ile C'nin Yankı/arıza dili ve sınır kırılması
anlarının birleşimi. Sistem soğuk ve kusursuz görünür, Yankı ve kahramanın anomalisi bu kusursuzluğu bozar.
Bu birleşim oyunun üst kuralını (kahraman bir anomali, Yankı ona bağlı) en doğrudan anlatır. Tek bir yön ya da başka bir
birleşim de seçilebilir.
