# İlerleme

Şu an: Aşama 2 — Sistem mekanikleri (başlıyor)
Son commit: (Aşama 0)
Görev tanımı: `CLAUDE_CODE_GOREV.md`

## Ortam notları
- JDK: Android Studio JBR (Java 25) → `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (check.sh kendisi bulur).
- SDK: `local.properties` → `sdk.dir=C:/Users/alpar/AppData/Local/Android/Sdk` (git dışı).
- Emülatör: `Medium_Phone_API_36.0` (`emulator -avd Medium_Phone_API_36.0`), adb ile kurulum.
- OneDrive kilidi yüzünden lint ara sıra "dosya kullanılıyor" der: `./gradlew --stop` ve tekrar.

## Aşama 0 — Doğrulama  ✅ (2026-11-05)
- [x] `:engine:test`, `:engine:validateContent`, `assembleDebug` çalışıyor (app ilk denemede derlendi, düzeltme gerekmedi)
- [x] Gradle 9.1.0 / AGP 8.13.2 / Kotlin 2.2.21 (Java 25 için en küçük yükseltme, DECISIONS #13)
- [x] `./gradlew lint`: 0 hata (uyarılar: eski targetSdk, -v26 klasörü, kullanılmayan iki metin)
- [x] `scripts/check.sh`: test + validateContent + assembleDebug + lint
- [x] Emülatörde (API 36) prolog EN ve TR baştan sona oynandı: kilitli [ALG 6] seçeneği, seviye atlama, unvan, tohum ipucu, Durum ekranı, dil değiştirme
- [x] `docs/PROGRESS.md`

## Aşama 1 — Çekirdek  ✅ (2026-11-05)
- [x] Gradle projesi (engine + app), version catalog, wrapper
- [x] WorldState, Rng, GameClock, GDD formülleri, etkiler ve koşullar
- [x] i18n: {tr,en}, isim belirteçleri, Türkçe hal ekleri, İngilizce dilbilgisi
- [x] Sürümlü kayıt, auto + 3 slot, Demir Mod
- [x] İçerik ayrıştırıcı ve doğrulayıcı, 5 sahnelik prolog (TR + EN-NA)
- [x] Compose: başlık, Hikaye, Durum, Daha fazla ekranları
- [x] 38 motor testi
