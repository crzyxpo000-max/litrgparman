# Tasarım ve uygulama kararları

GDD'nin açık bıraktığı ya da bilinçli olarak saptığımız noktalar. En yeni en üstte.

## Aşama 3 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 36 | Duruşlar: Tam saldırı (verilen ×1,3, alınan ×1,25), Ölçülü (×1, +5 Dayanım/tur), Savunmacı (×0,7, alınan ×0,6, +12 Dayanım/tur) | OP MC "duruş" isteği; GDD sayı vermiyor. | Aşama 9 simülatörü. |
| 37 | Efor 0–3: kademe başına +%12 hasar, +5 puan isabet, 8 Dayanım | "Dayanım taahhüdü". | Aşama 9. |
| 38 | Ezme: düşman daha düşük kademede ya da seviyesi oyuncunun yarısından az ve en az 5 aşağıda; boss asla ezilmez | "Fodder auto-resolves"; tutorial dövüşleri gerçek kalsın. | Aşama 6 içerik akışında. |
| 39 | Her gerçek savaşın son düşmanı infaz seçimiyle biter: Acımasız (Merhamet −3, İnsanlık −2), Temiz (Düzen +2), Bağışla (Merhamet +5; zeki düşman hatırlar, %50 XP) | OP MC: "gerçek seçimler ahlakidir". | — |
| 40 | Ölüm: önce İkinci Şans (kademe başına bir; %30 HP; bedel rastgele Benzersiz olmayan yetenek −2 seviye, yoksa bir unvan). Sonra yazılmış `kaybet` sahnesi varsa oraya (HP 1); yoksa Demir Mod → `son_hasat`, Normal → savaş başındaki kayıt noktası (`checkpoint` slotu) | GDD: "ölüm son kayda döndürür"; otomatik kayıt savaş ortasını tuttuğu için ayrı kayıt noktası. | — |
| 41 | Direnç: fiziksel 2·DAY, büyü 2·BİL; yetenek statı ZEK/BİL/İRD ise büyü sayılır | GDD "Direnç"i tanımlamıyor. | Aşama 9. |
| 42 | Zaaf ×1,5; Analiz zorluğu d20 + ALG ≥ 8 + seviye/2; OP MC gereği gerçek statlar her zaman görünür, Analiz zaafı açar | GDD: "Analiz zaafı açar". | — |
| 43 | Konuş: ikna d20 + KAR ≥ 12 + seviye/2 (düşman %30 altındaysa +4); tehdit, üst kademe ya da +5 seviye farkta kendiliğinden başarılı. Kaçan zeki düşman her kaçışta +2 seviye, statlar +%15, HP +%20 ile döner | GDD "zekalı düşmanlar oyuncuyu hatırlar". | Aşama 5 NPC hafızasıyla birleşebilir. |
| 44 | Ortam etiketleri: tavan, su, yanici, karanlik, dar_gecit; her biri savaşta bir kez | GDD örnekleri ("tavanı çökert, suya elektrik ver"). | Aşama 6'da yeni etiketler. |
| 45 | Kademe Kırma: XP ×3, unvan zinciri 1/3/10 zaferde (Kademe Kıran I–III), %25 ek Benzersiz yetenek şansı | OP MC. | Aşama 9. |
| 46 | Uyanışlar `content/awakenings.json`'da; koşullar savaş bağlamı değişkenlerini (`_savas_kazanildi`, `_savas_en_dusuk_can`, `_savas_kademe_farki`, `_savas_boss`, `_savas_ezme`) ve yeni `degisken_max` koşulunu kullanır | Yeniden kullanılabilir "uyanış tetikleyicisi". | Aşama 5: "birini büyük bedelle korumak" yoldaşlarla. |
| 47 | Yetenek gücü seviye başı +%5; her kullanım +1 yetenek XP'si | GDD "kullanıldıkça XP alır". | Aşama 9. |
| 48 | Düşman HP'si içerikte açıkça yazılır (formülle türetilmez) | Denge içerikte kolay ayarlanır. | — |
| 49 | Kanal Faresi XP'si 80: ilk öldürme oyuncuyu Seviye 2'ye taşır | Prologdaki seviye atlama anı korunur. | Aşama 6. |
| 50 | Boss savaşından kaçılamaz | Kapı kilitli; boss anının ağırlığı. | — |
| 51 | Prologdaki ilk fare artık gerçek savaş (`savas` etkisi → `ilk_kan`); unvan zaferden sonraki sahnede verilir | Savaş sistemini ilk dakikada tanıtır. | Aşama 6 yeni açılış. |
| 52 | Yazılmış `kaybet` sahnesi ölüm değildir: her iki modda İkinci Şans'tan önce gelir ve onu harcamaz; Demir Mod'da da kalıcı ölüm sayılmaz | Senaryolu yenilgiler (ör. ilk fare) oyuncunun tek İkinci Şans'ını yakmasın; Demir Mod ölümü haddini aşmaktan gelmeli. | — |
| 53 | İnisiyatif savaş başında bir kez atılır (oyuncu ↔ en hızlı düşman); sonra sıra oyuncu → düşmanlar | Mobilde okunabilir tur ritmi; GDD "sıra en yüksekten başlar". | Aşama 5: yoldaşlar gelince kişi başı sıra. |
| 54 | Durum hasarları (kanama, zehir, yanma) kademe çarpanı almaz; fırlatılan eşyalar alır | Yüzdelik/sabit tur hasarı kademe farkında anlamını yitirmesin. | Aşama 9. |
| 55 | Oyuncuya uygulanan durumlar uygulandıkları tur sonunda süre kaybetmez (`fresh`) | 1 turluk sersemleme gerçekten bir turu alsın (bağımsız inceleme). | — |
| 56 | Kaçan düşman bağışlanmış sayılmaz: XP yok; herkes kaçarsa `_savas_kazanildi = 0` | Uyanış koşulları kaçan düşmanla tetiklenmesin. | — |
| 57 | Kayıt noktası yalnızca aynı oyunun (tohum + ad) noktasıysa kullanılır; yeni oyunda silinir | Başka oyunun savaşına dönülmesin. | — |
| 58 | Analiz edilen "göz" zaafı her vuruşta hedeflenebilir; `goz`/`buz` etiketleri birkaç yeteneğe eklendi; `fiziksel` zaaf kaldırıldı | Erişilemeyen ya da her vuruşu ×1,5 yapan zaaflar. | — |
| 59 | İngilizce savaş metinlerinde düşman adı "the" ile ve cümle başında büyük harfle | "Sewer Rat staggers." doğal değil. | — |
| 60 | `korunan_muttefik` sayacı şimdilik artmaz; yoldaşlarla (Aşama 5) bir müttefiki korurken sayılacak | Tek başına Savun ile çiftlenmesin. | Aşama 5. |

## Aşama 2 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 19 | **Tasarım değişikliği (kullanıcı onaylı): OP MC.** GDD'ye "OP MC tasarımı" bölümü eklendi; diğer bölümlerin üstünde sayılır. Yankı bağı, yalnızca oyuncuda Günlük Görev, gizli bilgi, garantili Benzersiz yetenek + gizli sınıf, kademe başına 4 evreli güç döngüsü, Küresel Sıralama, Gizli/Açık Güç, Anomali Dikkati, Yozlaşma, Tanıklar, Kademe Kırma, Ordu, iki savaş modu vb. Kabul kriterleri görev dosyasında ilgili aşamalara dağıtıldı; bitmiş aşamaların işi (prolog) Aşama 2'ye retrofit olarak girdi. Çelişen GDD satırları (Günlük Görev, Ölüm, Gerilim uyumu, Yankı) güncellendi. | Kullanıcının tasarım güncellemesi. | — |
| 20 | Yetenek seviyesi için kullanım XP'si `4 · L` (1→10 toplam 180) | GDD sayı vermiyor. | Aşama 9 simülatörü. |
| 21 | Meslek teklifi seviye 5'te otomatik (ya da `meslek_teklifi` etkisiyle); ikinci dal (E) Aşama 7'ye kaldı | GDD zamanlamayı vermiyor. | Aşama 6 içerik akışında. |
| 22 | Günlük Görev antrenmanı şimdilik Daha fazla → Günlük Görev'den set set yapılır | Gün döngüsü (serbest eylemler) Aşama 4'te. | Aşama 4. |
| 23 | İhmal edilen Günlük Görevde Ceza Bölgesi olasılığı %50; kanca `ceza_bolgesi` sahnesi, dönüş `@geri` | GDD "risk" diyor. | Aşama 9 denge. |
| 24 | Kademe eşiğinde XP en fazla bir seviyelik tamponda birikir; fazlası yanar | Evrimden sonra onlarca seviye atlamayı önler; evrim anı bir seviye atlamayla ödüllenir. | Aşama 9. |
| 25 | Sınıf seçildiğinde geçmiş seviyeler için geriye dönük stat artışı yok | Basitlik; seviye 10'a kadar serbest puanlar var. | — |
| 26 | Evrimi tanımlı olmayan kademede "genel evrim": sınıf aynı, tüm statlar +1 | Oyun eşikte kilitlenmesin; D+ evrimleri Aşama 7. | Aşama 7. |
| 27 | Pasif yeteneklerde stat/taban/kat şimdilik yok sayılır; fırlatılan eşyaların (`hasar`, `durum`) etkisi yalnızca savaşta | Savaş Aşama 3. | Aşama 3. |
| 28 | Nadir sınıf "Çok Yönlü": tüm statlar ≥ 8 | İçerik varsayımı; dengeli oyuncuya ödül. | Aşama 9. |
| 29 | Kaynaştırma koşul değişkenleri `hareketli_atis`, `korunan_muttefik`, `arkadan_vurus` savaşta sayılacak | Savaş Aşama 3. | Aşama 3. |
| 30 | Yetenek evrimi Yetenekler ekranından değil, tam ekran Sistem istemiyle seçilir; "Şimdilik bekle" ile ertelenebilir, bir sonraki kademe evriminde yeniden sorulur | Tüm kalıcı seçimler aynı tam ekran an üzerinden gelir; erteleme, kaynaştırma için yeteneği maksimumda tutmayı sağlar. | — |
| 31 | Keşif / savaş / kapı türü Günlük Görev şablonları `gunluk_genis_gorevler` bayrağı gelene kadar çıkmaz | Bu görevleri ilerletecek gün döngüsü ve savaş Aşama 3–4'te; tamamlanamayan görev oyuncuyu haksız cezalandırırdı. | Aşama 4: bayrak gün döngüsüyle açılır. |
| 32 | Birden çok gün atlayan etki (`gun: 3`) Günlük Görev serisini koparır | Aradaki günlerde görev yapılmadı. | — |
| 33 | Simya malzemeleri henüz içerikte verilmiyor | Toplama/ganimet Aşama 3 (düşman ganimeti) ve Aşama 6 (Perde 1 olayları) işi. | Aşama 3/6. |
| 34 | Ceza Bölgesi ve "Temel Kondisyon" metinleri özgünleştirildi (bağımsız inceleme: ilham kaynağına fazla yakındı) | Telif kuralı: yalnızca tür kalıpları. | — |
| 35 | Kayıt yüklenince `StoryEngine.sanitize`: tanımsız içeriğe işaret eden istemler atılır, eksik Günlük Görev üretilir; arayüz beklenmeyen hataları yakalayıp kaydeder | Eski/bozuk kayıt oyunu çökertmesin. | — |

## Aşama 0 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 13 | Gradle 8.14.3 → **9.1.0**, AGP 8.7.3 → **8.13.2**, Kotlin 2.0.21 → **2.2.21** | Geliştirme makinesindeki tek JDK, Android Studio 2026.2'nin JBR'si (Java 25). Gradle 8.14 ve Kotlin 2.0 Java 25'te çalışmıyor. Java 25'i destekleyen en küçük Gradle 9.1; AGP 8.x çizgisinde kalındı (AGP 9 yerleşik Kotlin ile kırıcı değişiklik getiriyor). | Aşama 9: targetSdk yükseltilirken AGP 9'a geçiş değerlendirilir. |
| 14 | `scripts/check.sh` JAVA_HOME yoksa Android Studio JBR'sini kendisi bulur | Komut satırında Java kurulu olmayan makinelerde de çalışsın. | — |
| 15 | AAB'de dil bölmesi kapalı (`bundle.language.enableSplit = false`) | Dil uygulama içinden anında değişiyor; Play bölerse diğer dilin metinleri cihazda olmaz (lint: AppBundleLocaleChanges). | — |
| 16 | Prolog emülatörde (API 36, Medium Phone) iki dilde elle oynandı; Compose UI testi yazılmadı | Emülatör çalışıyor; görev dosyası UI testini yalnızca emülatör yoksa istiyor. | Aşama 4: başsız oynanış testi motor düzeyinde akışı kapsar. |
| 18 | Tema: Sistem · Açık · Koyu · **AMOLED** (kullanıcı isteği). AMOLED saf siyah arka plan/yüzey; alt çubuk ve kartlar çok koyu gri | AMOLED ekranda siyah piksel kapalı: pil tasarrufu, yüksek kontrast. Kartların seçilebilmesi için yüzey kapları #07–#15 aralığında. | Aşama 9: yüksek kontrast seçeneğiyle birleştirilebilir. |
| 17 | Proje OneDrive klasöründe; lint bazen "dosya başka işlem tarafından kullanılıyor" hatası veriyor | OneDrive build/ klasörünü kilitleyebiliyor. `./gradlew --stop` ve yeniden çalıştırma çözüyor. | Sorun sıklaşırsa projeyi OneDrive dışına taşımak önerilir. |

## Aşama 1 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 1 | Aşama 1 içeriği **JSON** (GDD: YAML) | Geliştirme ortamında kaml indirilemedi; JSON, kotlinx.serialization'ın `JsonElement` API'siyle eklentisiz okunuyor. Anahtarlar GDD'deki YAML örnekleriyle aynı. | Aşama 4: YAML'a (kaml) geçiş ya da JSON'da kalma kararı. Ayrıştırıcı tek dosyada (`ContentParser`), geçiş küçük. |
| 2 | Kayıt kodlayıcı **elle yazıldı** (`SaveCodec`), serialization derleyici eklentisi yok | Sürümlü, alan adları sabit, eski kayıtlar için geçiş kodu yazmak kolay. | Durum çok büyürse eklentiye geçilebilir; kayıt biçimi aynı kalmalı. |
| 3 | Testler **JUnit 4** (GDD: JUnit 5) | Android'de standart; motor bu ortamda çevrimdışı doğrulanabildi. | Gerek görülmüyor. |
| 4 | AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35 | Birbiriyle uyumu bilinen sürümler. | Aşama 9: Google Play'in o an istediği hedef API'ye yükselt (Android Studio yükseltme yardımcısı). |
| 5 | Otomatik kayıt **her seçimden sonra** + uygulama arka plana alınınca (GDD: gün sonunda + arka planda) | Telefonda uygulama her an kapanabilir; kayıt küçük ve ucuz. | — |
| 6 | Uygulama içi dil, yapılandırma bağlamı (`createConfigurationContext`) ile | Anında değişir, AppCompat gerektirmez, tüm Android sürümlerinde aynı çalışır. | Android 13+ sistem dil ayarıyla da eşitlemek istenirse `LocaleManager` eklenebilir. |
| 7 | Motorun ürettiği Sistem metinleri (XP, seviye atlama, unvan bildirimi) Kotlin'de `LText` olarak | Hikaye metni değil, arayüz düzeyinde kalıp metinler; iki dilli tutuluyor. | Aşama 2: `content/system.json`'a taşımayı değerlendir. |
| 8 | Prolog (5 sahne) **demo içeriği**dir | Motoru uçtan uca denemek için: koşullu paragraflar, kilitli seçenek, seviye atlama, unvan, tohum ipucu. | Aşama 6'da Öğretici Katman içeriğiyle değiştirilir/genişletilir. |
| 9 | Eğilimler şimdilik düz değişken (`egilim_algi`, `egilim_yakin`…) | Gizli Eğilim vektörü Aşama 2'nin işi; veriler şimdiden toplanıyor. | Aşama 2: `Affinity` modeline taşı, değişken adlarını eşle. |
| 10 | Deniz/Devon için cinsiyet belirtilmedi (TR'de nötr; EN metinde zamir kullanılmıyor) | Karakterin cinsiyeti GDD'de tanımlı değil. | Karakter yazımı netleşince GDD'ye eklenip EN zamirleri yazılacak. |
| 11 | İçerik ilk erişimde ana iş parçacığında yükleniyor | Şu an birkaç KB. | Aşama 4/6: içerik büyüyünce arka plana al ve açılış ekranı göster. |
| 12 | Gezinme ikonları material-icons-core'dan (Çanta için geçici olarak "Build") | Ek bağımlılık yok. | Aşama 9: özel ikon seti. |
