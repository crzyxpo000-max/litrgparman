# İlerleme

Şu an: Aşama 2 — Sistem mekanikleri (motor tamam, OP MC retrofit + arayüz sürüyor)
Son commit: 6a0ad8e Arayüz: AMOLED (saf siyah) tema seçeneği
Uzak depo: https://github.com/crzyxpo000-max/litrgparman (origin/main)
Görev tanımı: `CLAUDE_CODE_GOREV.md`

## Ortam notları
- JDK: Android Studio JBR (Java 25) → `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (check.sh kendisi bulur).
- SDK: `local.properties` → `sdk.dir=C:/Users/alpar/AppData/Local/Android/Sdk` (git dışı).
- Emülatör: `Medium_Phone_API_36.0` (`emulator -avd Medium_Phone_API_36.0`), adb ile kurulum.
- OneDrive kilidi yüzünden lint ara sıra "dosya kullanılıyor" der: `./gradlew --stop` ve tekrar.

## Aşama 2 — Sistem mekanikleri  ⏳
Plan:
- [x] `system/Affinity.kt`: 8 eğilim (yakin_dovus, menzilli, buyu, gizlilik, koruma, cagirma, zanaat, karanlik); `egilim` etkisi, `egilim_min` koşulu; prolog `egilim_*` değişkenleri taşınır (egilim_algi → menzilli, egilim_yakin → yakin_dovus)
- [x] SaveCodec v2 + v1 geçişi (egilim_* değişkenleri → eğilim vektörü); eski kayıt testi
- [x] Motor üretimli "Sistem istemi" (prompt) kuyruğu: sınıf teklifi, evrim, meslek seçimi, yetenek evrimi, kaynaştırma; tam ekran kart olarak gösterilir
- [x] `content/classes.json`: 8 temel + 4 gizli + nadir sınıflar; seviye başı stat artışı, başlangıç yeteneği, E evrimi (2 seçenek)
- [x] Seviye 10 sınıf teklifi; kademe eşiğinde seviye durur, XP bir seviyelik tamponda birikir, Evrim Görevi istemi
- [x] Serbest stat dağıtımı (motor: `allocate`); [ ] Durum ekranında +/− ve onay
- [x] `content/skills.json` ≥30 yetenek: aktif/pasif, maliyet, bekleme, seviye 1–10, kullanım XP'si, 2 dallı evrim, kaynaştırma tarifleri, ceza yetenekleri
- [x] `titles.json` ≥15 unvan; "İlk" unvanları için dünya kaydı (`firstClaims`), itibar tavanı (Kardeş Katili)
- [x] Meslekler + `items.json` + envanter; Simyacı 5 tarif, Çanta ekranında üretim
- [x] Öz kavrayışı ve D kademesi yuvası (model), Yol alanı
- [x] Günlük Görev: `daily.json` şablonları, günlük üretim (Rng), ilerleme etkisi, ödül (+1 serbest puan), Ceza Bölgesi kancası (`@geri` dönüşlü sahne)
- [x] Sistem bildirim metinleri `content/system.json`'a taşınır (DECISIONS #7)
- [ ] **OP MC retrofit (DECISIONS #19):** Yankı bağı prologda (`yanki_bagi`), Günlük Görev yalnızca oyuncuda + 7 günlük seri bonusu, pasif sezgi satırları (`sezgiler`), gizli sınıf garantisi (üçüncü seçenek her zaman gizli), eğilime göre Benzersiz yetenek, kademeyle çarpan sınıf artışı, insanlık yüzdeliği, kendini açan sekmeler (`arayuz`)
- [ ] Arayüz: Yetenekler ekranı, seviye/evrim tam ekran kartı + animasyon + titreşim, Çanta
- [ ] Testler, check.sh, bağımsız inceleme, commit
Notlar (kaldığın yer):
- OP MC motor retrofiti YAZILDI ve testleri geçiyor: gizli sınıf garantisi + echoSuggestion, grantUniqueSkill (sınıf seçiminde), growthMultiplier, dailyReward (7 günlük seri), humanityTopPercent, unlockTab/`arayuz` + otomatik sekmeler, `sezgiler` (pasif sezgi), NoticeKind.ECHO, kayıtta `tabs`.
- EKSİK İÇERİK: (1) skills.json'a 8 Benzersiz yetenek (`nadirlik: benzersiz`, `egilim: <eğilim>`), (2) classes.json gizli sınıflara `egilimler` listesi, (3) prologa Yankı bağı (`yanki_bagi` bayrağı, `yanki` türü bozuk bildirim, "Günlük Görev yalnızca sende"), (4) prolog sahnelerine `sezgiler`, (5) validator: Benzersiz yetenek her eğilim için 1, gizli sınıfta `egilimler` zorunlu, (6) testler: Benzersiz yetenek, sekmeler, sezgi, yüzdelik.
- SONRA: arayüz (Yetenekler, Çanta, +/− dağıtım, tam ekran istem/seviye kartı, sezgi/ECHO stili, gizli sekmeler, yüzdelik, Günlük Görev paneli), check.sh, emülatör, bağımsız inceleme, Aşama 2 commit.
- Kullanıcı isteğiyle araya tema seçimi girdi (Sistem/Açık/Koyu/AMOLED), tamam ve GitHub'a gönderildi.

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
