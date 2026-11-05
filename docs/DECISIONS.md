# Tasarım ve uygulama kararları

GDD'nin açık bıraktığı ya da bilinçli olarak saptığımız noktalar. En yeni en üstte.

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
