# Yükseliş Protokolü · Ascension Protocol

Android için metin tabanlı, seçimlere göre şekillenen bir LitRPG. Türkçe ve İngilizce (Kuzey Amerika).

**Durum:** Aşama 1 / 9 tamam: oyun motoru, kayıt sistemi, iki dil altyapısı, arayüz iskeleti ve 5 sahnelik oynanabilir prolog.

## Çalıştırmak

1. [Android Studio](https://developer.android.com/studio)'yu kur (Android SDK ve emülatör onunla gelir).
2. Android Studio → **Open** → bu klasörü seç. İlk açılışta Gradle bağımlılıkları indirilir (birkaç dakika).
   Android Studio Gradle ya da eklenti sürümünü yükseltmeyi önerirse kabul edebilirsin.
3. Üstteki cihaz menüsünden bir emülatör seç ya da telefonunu USB hata ayıklama ile bağla, ▶ **Run**'a bas.

Komut satırından:

```bash
./gradlew :engine:test          # motor testleri
./gradlew assembleDebug         # APK → app/build/outputs/apk/debug/app-debug.apk
```

## Klasörler

- `engine/` — saf Kotlin oyun motoru (Android'den bağımsız, testli)
- `app/` — Jetpack Compose arayüzü
- `content/` — oyun içeriği (sahneler, isimler, unvanlar; her metin TR + EN)
- `docs/` — tasarım dokümanı (`GDD.md`, `GDD.en.md`) ve kararlar (`DECISIONS.md`)
- `CLAUDE.md` — Claude Code ile geliştirmeye devam ederken okunacak proje kuralları

## Claude Code ile devam etmek

Klasörde Claude Code'u başlat ve şunu yaz:

```text
CLAUDE_CODE_GOREV.md dosyasını baştan sona oku ve uygula. Bana yalnızca dosyada yazan durma noktalarında soru sor.
```

Oturum yarıda kalırsa:

```text
CLAUDE_CODE_GOREV.md ve docs/PROGRESS.md'yi oku, kaldığın yerden devam et.
```
