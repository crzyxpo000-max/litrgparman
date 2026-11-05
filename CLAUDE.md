# CLAUDE.md — Yükseliş Protokolü / Ascension Protocol

Android için tek oyunculu, metin tabanlı, iki dilli (TR / EN-NA) bir LitRPG.
Tasarımın tamamı: `docs/GDD.md` (Türkçe, mekanik ve hikayede ana kaynak) ve `docs/GDD.en.md`
(Kuzey Amerika yerelleştirmesi; İngilizce isim ve terimlerde ana kaynak). Her işten önce ikisini oku.
Verilen tasarım kararları: `docs/DECISIONS.md`. Belirsiz bir noktada varsayım yap ve oraya yaz.

## Durum

- **Aşama 0 (Doğrulama) ve Aşama 1 (Çekirdek): tamam.** Motor, kayıt, i18n, içerik doğrulayıcı, Compose iskeleti, 5 sahnelik prolog demosu; app emülatörde çalışıyor.
- **Tüm geliştirme planı ve kabul kriterleri:** `CLAUDE_CODE_GOREV.md`. Güncel durum: `docs/PROGRESS.md`.

## Komutlar

```bash
./gradlew :engine:test             # motor testleri (JUnit 4)
./gradlew :engine:validateContent  # content/ doğrulaması
./gradlew assembleDebug            # APK: app/build/outputs/apk/debug/
./gradlew installDebug             # bağlı telefona/emülatöre kur
./gradlew lint                     # Android lint (hata kalmamalı)
scripts/check.sh                   # test + validateContent + assembleDebug + lint; her aşamanın sonunda geçmeli
```

Sürümler: Gradle 9.1, AGP 8.13.2, Kotlin 2.2.21, JDK 25 (Android Studio JBR). `local.properties` git dışıdır.

## Mimari (değişmez kurallar)

- `engine/` saf Kotlin'dir; Android'e **asla** bağımlı olmaz. Tüm oyun mantığı burada.
- `app/` yalnızca gösterir: Compose ekranları + `GameViewModel`. Oyun kuralı app'e yazılmaz.
- `WorldState` tek doğruluk kaynağıdır ve değişmezdir; yalnızca `story/Effects.kt` içindeki etkilerle değişir.
- Tüm rastgelelik `core/Rng.kt` (SplitMix64) üzerinden; durumu `WorldState.rngState` içinde.
- Motor arayüze dili çözülmüş görünüm modelleri verir (`SceneView`, `StatusView`, `NoticeView`).
- Kayıt biçimi `save/SaveCodec.kt` içinde, sürümlü (`VERSION`). Alan eklerken varsayılan ver; alan
  silerken/yeniden adlandırırken `VERSION`'ı artır ve `decode` içinde geçiş yaz. Eski kayıtlar açılmalı.
- `kotlinx.serialization` **derleyici eklentisi kullanılmıyor**; JSON `JsonElement` API'siyle elle okunup yazılıyor.

## Paketler (engine)

| Paket | İçerik |
| --- | --- |
| `core` | `WorldState`, `PlayerState`, `Morality`, `GameClock`, `Rng` |
| `system` | `Stat`, `Stats`, `Grade`, `Progression` (GDD formülleri), `Rarity` |
| `i18n` | `Lang`, `LText`, `NameRegistry`, `TokenResolver`, `TurkishSuffix`, `EnglishGrammar` |
| `story` | İçerik modeli (`Scene`, `ChoiceDef`, `Condition`, `Effect`, `Notice`), `EffectApplier`, `StoryEngine` |
| `content` | `ContentParser` (JSON), `ContentLoader`, `ContentValidator` (+ `main`) |
| `save` | `SaveCodec`, `SaveManager` (auto + 3 slot, Demir Mod), `SaveStorage` |

## İçerik kuralları (content/)

- Yerleşim: `names.json`, `titles.json`, `story/*.json` (dosya adı sırasıyla yüklenir). APK'ya assets olarak girer.
- Anahtarlar Türkçe ASCII: `sahneler`, `paragraflar`, `bildirimler`, `secenekler`, `etiket`, `gerekli`,
  `goster`, `etkiler`, `sonraki`, `kosul`, `son`, `yer`.
- Oyuncuya görünen **her** metin `{ "tr": ..., "en": ... }`. İngilizce çeviri değil, Kuzey Amerika
  yerelleştirmesidir (birimler: mil/lb/°F; deyimler; kültürel referanslar). Eşleme tablosu GDD'de.
- Metne **asla isim yazma**; belirteç kullan:
  - `{npc:deniz_arman}` kısa ad (Deniz / Devon), `{npc:deniz_arman|tam}` tam ad
  - Türkçe hal ekleri: `|e` `|i` `|in` `|de` `|den` `|le` → Deniz'e, Kaya'nın, Aras'tan
  - İngilizce iyelik: `|pos` → Devon's
  - `{yer:kiyikent}`, `{oyuncu}` (oyuncu adı, ek alabilir: `{oyuncu|in}`)
- Etkiler: `bayrak`, `bayrak_kaldir`, `degisken`, `ahlak` (merhamet/duzen/insanlik), `itibar`, `xp`, `gun`, `stat`, `unvan`.
- Koşullar: `stat` (asgari değer), `bayrak`, `bayrak_yok`, `degisken_min`. Birden çok anahtar = VE.
- `gerekli` karşılanmazsa seçenek görünür ama kilitlidir ve "[ALG 6]" etiketi alır; `goster` karşılanmazsa gizlenir.
- İçerik değiştikten sonra `:engine:validateContent` ve `:engine:test` çalıştır (ContentFilesTest gerçek içeriği doğrular).

## Arayüz notları (app/)

- Arayüz metinleri: `res/values/strings.xml` (EN) ve `res/values-tr/strings.xml` (TR). Yeni metin = iki dosyaya birden.
- Uygulama içi dil `ProvideLanguage` (App.kt) ile seçilir; cihaz dilinden bağımsız ve anında değişir.
- Türkçe büyük harf dönüşümünde yerel ayar kullan (`uppercase(locale)`): i → İ.
- Türkçe metinde sayıya ek getirme ("Aşama 4'te" sayıya göre değişir); "Aşama %d ile" gibi eksiz kalıp kullan.
- Kayıt: her seçimden sonra ve `onStop`'ta otomatik; dosyalar `files/saves/*.json`, Auto Backup kapsamında.

## Test notları

- Motor testleri JUnit 4 ile; test adları Türkçe olabilir.
- Yeni her formül veya kural için test yaz; GDD'deki sayılar testlerde sabitlenir (`ProgressionTest`).
