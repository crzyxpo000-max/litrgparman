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

1. Roman adını yazıp **Ara**'ya basın (DuckDuckGo, olmazsa Bing). Kopyanın adresini biliyorsanız
   dizin ya da 1. bölüm URL'sini doğrudan arama kutusuna yapıştırın; arama yapılmaz.
2. Listeden bir kaynağa dokunun, kitap adını onaylayın.
3. İlerleme penceresi bölüm bölüm ilerler; bitince Snackbar dosya adını gösterir.
4. Dosyayı USB ile Kobo'nun kök klasörüne kopyalayın. `.kepub.epub` uzantısı sayesinde Kobo kepub
   motorunu kullanır (sayfa sayımı, okuma istatistikleri).

## Mimari

```
com.romanarsiv
├── domain/            saf Kotlin: modeller, depo arayüzleri, kullanım durumları, AppConfig
│   ├── model/         NovelSource, NovelChapter, ArchiveProgress, ArchiveResult
│   ├── repository/    NovelRepository, EpubStorage
│   └── usecase/       SearchNovelUseCase, ArchiveNovelUseCase (channelFlow ile ilerleme)
├── data/
│   ├── remote/        PageLoader (Jsoup, site başına istek aralığı, yeniden deneme),
│   │                  SearchProvider (DuckDuckGo HTML, Bing), ChapterExtractor (sezgiler), UrlUtils
│   ├── repository/    NovelRepositoryImpl (dizin modu / "sonraki" zinciri)
│   └── storage/       FileHelper (MediaStore → Download/…kepub.epub)
├── epub/              EpubGenerator (EPUB 3 + NCX, UTF-8, koboSpan)
├── presentation/      MainViewModel (StateFlow: SearchState, DownloadState; Channel: UiMessage)
│   └── ui/            MainScreen (Compose: arama, LazyColumn, onay, ilerleme penceresi, Snackbar)
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

## Bilinen sınırlar

- JavaScript ile içerik yükleyen siteler (Jsoup JS çalıştırmaz) ve giriş/Cloudflare doğrulaması
  isteyen sayfalar kazınamaz.
- Arama motorları otomatik istekleri zaman zaman doğrulama sayfasına yönlendirir; o durumda URL'yi
  doğrudan yapıştırın.
- Bir bölümü birden çok sayfaya bölen siteler için yalnızca ilk sayfa alınır.
