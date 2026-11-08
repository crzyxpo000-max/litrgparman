# Roman Arşivi (Kobo EPUB arşivleyici)

Bir romanın internetteki kopyalarını bulur, bölümlerini sırayla toplar ve Kobo için
`KitapAdi.kepub.epub` olarak telefonun **İndirilenler** klasörüne kaydeder.

Oyun projesinden (`../app`, `../engine`) tamamen bağımsız, ayrı bir Gradle projesidir;
oyunun `settings.gradle.kts`'ine ve `scripts/check.sh`'ye dahil değildir.

## Derleme

```bash
cd kobo-arsiv
./gradlew testDebugUnitTest   # birim testleri (EPUB yapısı, HTML sezgileri, depo akışı)
./gradlew assembleDebug       # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug        # bağlı telefona kur
./gradlew lintDebug
```

Android Studio'da `kobo-arsiv/` klasörünü ayrı bir proje olarak açın. `local.properties`
(`sdk.dir=…`) git dışıdır. En düşük Android sürümü 10 (API 29): İndirilenler'e izinsiz yazmak için
`MediaStore.Downloads` gerekir.

**Yazar adı:** `app/src/main/java/com/romanarsiv/domain/AppConfig.kt` → `AUTHOR_NAME`. Kendi adınızı yazın.

## Kullanım

1. Roman adını yazıp **Ara**'ya basın (DuckDuckGo, olmazsa Bing; "… novel oku" sorgusu da denenir).
   Yalnızca **roman siteleri** listelenir (bkz. "Roman sitesi filtresi"); elenenler "n sonuç gizlendi ·
   Göster" ile açılabilir. Kopyanın adresini biliyorsanız dizin ya da 1. bölüm URL'sini doğrudan arama
   kutusuna yapıştırın; arama ve filtre yapılmaz.
2. Listeden bir kaynağa dokunun. Bölümler tespit edilir ve **seçim ekranı** açılır:
   - bölümleri tek tek işaretleyin ya da **İlk – Son** alanlarına numara yazıp **Aralığı seç**'e basın
     (ör. 1–50 = ilk cilt); **Tümü / Hiçbiri / Tersine çevir** de var;
   - **Kitap adı** EPUB'ın içindeki başlıktır; **Dosya adı** İndirilenler'deki addır. Dosya adı kitap
     adından önerilir, elle değiştirince bağımsız kalır (**Öner** ile geri döner). Uzantı yazmazsanız
     `.kepub.epub` eklenir; `.epub` yazarsanız düz EPUB adı korunur.
3. **İndir**'e basın. Yalnızca seçilen bölümler indirilir; bitince Snackbar dosya adını gösterir.
   Seçim ekranı açık kalır, başka bir aralığı ayrı dosya olarak da indirebilirsiniz.
   **Arka planda çalışır:** tespit ve indirme sürerken uygulamadan çıkabilir (ya da **Arka plana al**),
   ekranı kapatabilirsiniz; ilerleme bildirimde görünür (**İptal** düğmesiyle). İş bitince bildirim gelir;
   tespit bittiyse bildirime dokununca seçim ekranı açılır. Site doğrulama isterse bildirim sizi çağırır.
4. Dosyayı USB ile Kobo'nun kök klasörüne kopyalayın. `.kepub.epub` uzantısı sayesinde Kobo kepub
   motorunu kullanır (sayfa sayımı, okuma istatistikleri).

Not: Sayfa bir bölümün kendisiyse ("sonraki" zinciri) bölüm listesi önceden bilinemez; tespit sırasında
bölümler sırayla okunur, bu yüzden bu adım dizin sayfasına göre daha uzun sürer.

## Mimari

```
com.romanarsiv
├── domain/            saf Kotlin: modeller, depo arayüzleri, kullanım durumları, AppConfig
│   ├── model/         NovelSource, ChapterRef, NovelChapter, ArchiveProgress, ArchiveResult
│   ├── repository/    NovelRepository, EpubStorage
│   ├── usecase/       SearchNovelUseCase, DetectChaptersUseCase, ArchiveNovelUseCase (channelFlow)
│   └── FileNames      dosya adı önerisi ve temizleme (.kepub.epub)
├── data/
│   ├── remote/        PageLoader (Jsoup, site başına istek aralığı, yeniden deneme),
│   │                  SearchProvider (DuckDuckGo HTML, Bing), ChapterExtractor (sezgiler), UrlUtils
│   ├── repository/    NovelRepositoryImpl (dizin modu / "sonraki" zinciri)
│   └── storage/       FileHelper (MediaStore → Download/…kepub.epub)
├── epub/              EpubGenerator (EPUB 3 + NCX, UTF-8, koboSpan)
├── background/        ArchiveJobs (uzun işler), ArchiveService (ön plan hizmeti), ArchiveNotifications
├── presentation/      MainViewModel (StateFlow: SearchState, DetectState, SelectionState, DownloadState)
│   └── ui/            MainScreen (arama), SelectionScreen (bölüm seçimi, adlar), Dialogs (doğrulama, çökme)
└── di/                AppContainer (elle bağımlılık enjeksiyonu)
```

### Bölüm tespiti

- **Dizin modu:** sayfada aynı siteye ait ≥2 bölüm bağlantısı varsa ("Bölüm 12", "12. Bölüm",
  "Chapter 12", `/bolum-12`, `<select>` seçenekleri). Liste en yeniden eskiye diziliyse ters çevrilir;
  dizin birden çok sayfaysa "sonraki" bağlantısıyla diğer dizin sayfaları da toplanır.
- **Zincir modu:** sayfa bir bölümün kendisiyse (uzun ana metin) `rel=next` / "Sonraki" / "İleri" /
  "Next" / `»` bağlantıları izlenir; döngü ve `MAX_CHAPTERS` sınırında durur.
- **Ana metin:** bilinen içerik seçicileri (`#chapter-content`, `.entry-content`, …), yoksa paragraf
  yoğunluğuna göre puanlama. Reklam, yorum, menü, betik ve gezinti bağlantıları atılır; yalnızca
  `p br em strong i b u h2–h4 blockquote hr` kalır. Tek `<p>` içinde `<br>` ile yazılmış bölümler
  paragraflara bölünür. İndirilemeyen bölümler atlanır ve sayısı bildirilir.

### EPUB

Harici kütüphane yok: `epublib-core` 2011'den beri güncellenmiyor ve Android'de xmlpull sınıf
çakışması çıkarıyor. Paket `java.util.zip` ile yazılır: sıkıştırılmamış `mimetype` ilk girdi,
`container.xml`, `content.opf` (EPUB 3, `dc:language tr`), `toc.ncx` + `nav.xhtml` (İçindekiler),
başlık sayfası ve her bölüm için bir XHTML. Tüm dosyalar UTF-8; Türkçe harfler varlığa çevrilmeden ham
kalır. Paragraflar `koboSpan` ile sarılır (`EpubGenerator(kepub = false)` ile kapatılabilir).

### 403 / bot koruması

Sayfalar önce Jsoup ile istenir. Site 403 ya da 503 döndürürse (Cloudflare, DDoS-Guard …) aynı sayfa
ekran dışı bir **WebView** (gerçek Chromium) ile açılır; doğrulama çoğunlukla kendiliğinden geçer.
Geçmezse tam ekran bir tarayıcı penceresi açılır: doğrulamayı geçip **Devam**'a dokunun. Alınan
çerezler ve WebView'in User-Agent'ı sonraki Jsoup isteklerinde kullanılır; site yine reddederse o
site için her sayfa WebView ile açılır (daha yavaş ama çalışır). Vazgeç'e basılırsa indirme durur.

### Roman sitesi filtresi (`domain/NovelSiteFilter`)

Sezgisel puan, eşik 2: bilinen roman/fanfik siteleri (Wattpad, Royal Road, Webnovel, AO3 …) +10;
alan adında `novel`, `wuxia`, `fanfic`, `fiction`, `hikaye` +4; adres yolunda `bolum`, `chapter`,
`novel`, `/oku` … +2; başlıkta `bölüm`, `chapter`, `novel`, `oku` … +2 (özette +1); başlıkta `izle`,
`dizi`, `fragman`, `fiyat` … −4 (özette −2). Ansiklopedi, kitap mağazası, sözlük, TV/video siteleri
doğrudan elenir. Bilinen roman siteleri listenin üstüne çıkar.

### Arka plan (`background/`)

`ArchiveJobs` uygulama ömrü boyunca yaşayan bir kapsamda koşar (ViewModel değil), böylece ekran
kapanınca iş durmaz. İş başlayınca `ArchiveService` (ön plan hizmeti, tür `dataSync`) açılır: süreci
canlı tutar, ilerleme bildirimini günceller, ekran kapalıyken ağ için kısmi uyanıklık kilidi (en çok
3 saat) tutar ve iş bitince kendini kapatır. Android 13+ bildirim izni ilk uzun işte istenir; izin
verilmese de iş sürer, yalnızca bildirim görünmez. Android 15'in dataSync sınırı (günde 6 saat)
dolarsa iş iptal edilir.

### Çökme kaydı

Yakalanmamış bir hata olursa yığın izi `files/son_cokme.txt`'ye yazılır ve bir sonraki açılışta
kopyalanabilir bir pencerede gösterilir.

**Not:** Android'in düzenli ifade motoru ICU'dur; `(?U)`, `(?u)`, `\p{Cntrl}` gibi JVM'de çalışan
söz dizimleri telefonda çöker. `RegexAndroidSafetyTest` kaynakta bunları arar.

## Bilinen sınırlar

- Giriş (oturum açma) isteyen sayfalar kazınamaz.
- Arama motorları otomatik istekleri zaman zaman doğrulama sayfasına yönlendirir; o durumda URL'yi
  doğrudan yapıştırın.
- Bir bölümü birden çok sayfaya bölen siteler için yalnızca ilk sayfa alınır.
