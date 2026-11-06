# İlerleme

Şu an: Aşama 5 — NPC sistemi (sırada)
Son commit: 6d96bda Aşama 4: hikaye motoru
Uzak depo: https://github.com/crzyxpo000-max/litrgparman (origin/main)
Görev tanımı: `CLAUDE_CODE_GOREV.md`

## Ortam notları
- JDK: Android Studio JBR (Java 25) → `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (check.sh kendisi bulur).
- SDK: `local.properties` → `sdk.dir=C:/Users/alpar/AppData/Local/Android/Sdk` (git dışı).
- Emülatör: `Medium_Phone_API_36.0` (`emulator -avd Medium_Phone_API_36.0`), adb ile kurulum.
- OneDrive kilidi yüzünden lint ara sıra "dosya kullanılıyor" der: `./gradlew --stop` ve tekrar.

## Aşama 2 — Sistem mekanikleri  ✅ (2026-11-05)
Plan:
- [x] `system/Affinity.kt`: 8 eğilim (yakin_dovus, menzilli, buyu, gizlilik, koruma, cagirma, zanaat, karanlik); `egilim` etkisi, `egilim_min` koşulu; prolog `egilim_*` değişkenleri taşınır (egilim_algi → menzilli, egilim_yakin → yakin_dovus)
- [x] SaveCodec v2 + v1 geçişi (egilim_* değişkenleri → eğilim vektörü); eski kayıt testi
- [x] Motor üretimli "Sistem istemi" (prompt) kuyruğu: sınıf teklifi, evrim, meslek seçimi, yetenek evrimi, kaynaştırma; tam ekran kart olarak gösterilir
- [x] `content/classes.json`: 8 temel + 4 gizli + nadir sınıflar; seviye başı stat artışı, başlangıç yeteneği, E evrimi (2 seçenek)
- [x] Seviye 10 sınıf teklifi; kademe eşiğinde seviye durur, XP bir seviyelik tamponda birikir, Evrim Görevi istemi
- [x] Serbest stat dağıtımı (motor: `allocate`), Durum ekranında +/− ve onay
- [x] `content/skills.json` ≥30 yetenek: aktif/pasif, maliyet, bekleme, seviye 1–10, kullanım XP'si, 2 dallı evrim, kaynaştırma tarifleri, ceza yetenekleri
- [x] `titles.json` ≥15 unvan; "İlk" unvanları için dünya kaydı (`firstClaims`), itibar tavanı (Kardeş Katili)
- [x] Meslekler + `items.json` + envanter; Simyacı 5 tarif, Çanta ekranında üretim
- [x] Öz kavrayışı ve D kademesi yuvası (model), Yol alanı
- [x] Günlük Görev: `daily.json` şablonları, günlük üretim (Rng), ilerleme etkisi, ödül (+1 serbest puan), Ceza Bölgesi kancası (`@geri` dönüşlü sahne)
- [x] Sistem bildirim metinleri `content/system.json`'a taşınır (DECISIONS #7)
- [x] **OP MC retrofit (DECISIONS #19):** Yankı bağı prologda (`yanki_bagi`), Günlük Görev yalnızca oyuncuda + 7 günlük seri bonusu, pasif sezgi satırları (`sezgiler`), gizli sınıf garantisi (üçüncü seçenek her zaman gizli), eğilime göre Benzersiz yetenek, kademeyle çarpan sınıf artışı, insanlık yüzdeliği, kendini açan sekmeler (`arayuz`)
- [x] Arayüz: Yetenekler ekranı, seviye/evrim tam ekran kartı + animasyon + titreşim, Çanta
- [x] Testler, check.sh, bağımsız inceleme, commit
Notlar:
- 74 motor testi, check.sh geçiyor; emülatörde EN ve TR denendi (Yankı bağı, sezgiler, Günlük Görev, sınıf teklifi + Benzersiz yetenek, kutlama kartı, sekmelerin açılması, stat dağıtımı, üretim).
- Bağımsız inceleme yapıldı; bulguların hepsi düzeltildi (kaynaştırma/evrim çakışması, çift dokunuş, kayıt yarışı, sezgi türü, eski içerikle çökme, olay unvanları, ceza satırı, çoklu gün, doğrulayıcı boşlukları, içerik ve telif yakınlığı). DECISIONS #30–35.
- Bilinen eksik: simya malzemesi kaynağı (Aşama 3/6), cihazda Seviye 25 evrimi denenmedi (testle kapsanıyor).

## Aşama 4 — Hikaye motoru  ✅ (2026-11-06)
- [x] İçerik YAML'a (kaml) taşındı, JSON içerik okuyucusu kaldırıldı (kayıpsız doğrulandı)
- [x] Olay havuzu (`events/*.yaml`), Hikaye Yönetmeni (GDD puanı + gerilim eğrisi + Dikkat), zorunlu ana yay
- [x] Tohumlar (gecikme, koşul, kayıtta kuyruk), kuruma
- [x] Gün döngüsü (2–4 yuva, fırsat eli, keşif/antrenman/kapı/dinlenme/yolculuk, kamp)
- [x] Bölgeler (kontrol, tehlike, kapı doğuşu/taşması), fraksiyon ilişki matrisi
- [x] Bayrak kaydı (`flags.yaml`), görevler, tanıklı başarılar
- [x] validateContent: tanımsız bayrak (hata), set edilmeyen bayrak / ulaşılamayan tohum olayı / ahlak oynatmayan önemli seçim (uyarı), tohum hedefi
- [x] Prolog → `ilk_sabah` → gün akışı; Perde 1 sonu Gün 28 ana yay (Öğretici Bekçisi)
- [x] OP MC: Dikkat + Yozlaşma, gizli/görünen seviye, [Gizle]/[Açığa vur], gösteriş, görünür olasılıklı zar (beyaz/kırmızı), fırsat eli
- [x] Arayüz: gün akışı paneli, Harita, Günlük (görevler + efsane), Durum'da Dikkat/Yozlaşma/görünen seviye
- [x] Başsız oynanış: 200 rastgele oynanış çökmeden ve çıkmazsız biter
Notlar:
- 122 motor testi, check.sh geçiyor. 14 örnek olay (içerik ajanı limite takıldı; elle kısa yazıldı, hacim Aşama 6).
- Bağımsız inceleme (Sonnet, kullanım tasarrufu) bulguları düzeltildi: çift yuva, kurumayan tohum, akıştan açılan savaşın yüklenince düşmesi, Deniz kurtarma ödülünün kaçışta da verilmesi, doğrulayıcı uyarı kapsamı. DECISIONS #61–75.
- Bilinen eksik (Aşama 5–7): NPC'lerin görünen seviyeye tepkisi, Yozlaşma eşik olayları, Dikkat 75/100 olayları.
- **KULLANICI KURALI: Hikaye kusursuz olmalı; hikaye içeriğinde asla kaynak kısılmaz.** Aşama 4'ün 14 örnek olayı (`events/act1_ornek.yaml`) tasarruf için kısa yazıldı: Aşama 6'da tam kaliteyle yeniden yazılacak/genişletilecek (paralel içerik ajanları + bağımsız içerik incelemesi, tam model).

## Aşama 3 — Savaş  ✅ (2026-11-06)
Plan:
- [x] `combat/` paketi: `CombatState` (WorldState içinde, kayda girer), `Combatant`, düşman tanımı (`content/enemies.json`), ortam etiketleri
- [x] Sahne etkisi `savas`: düşmanlar, ortam, `kazan` / `kaybet` / `kac` sahneleri
- [x] GDD formülleri: inisiyatif, isabet (%5–95), hasar (direnç, ±%10), kritik şans/çarpan, kademe farkı ×2 / ×0,5 — testle sabitlenir
- [x] Tur: ana + küçük eylem; Saldır, Yetenek, Savun, Analiz, Ortam, Konuş, Kaç; bekleme süreleri, MP/Dayanım
- [x] 8 durum etkisi (kanama, zehir, yanma, sersemleme, korku, kör, hızlanma, kalkan)
- [x] Düşman kalıpları (agresif, temkinli, sürü, kaçan), zaaflar, boss fazları %70/%40/%15 + faz anlatısı
- [x] Kaçan zeki düşman hafızası (güçlenmiş dönüş)
- [x] OP MC: **Gerçek savaş** (duruş: tam saldırı / ölçülü / savunmacı + Dayanım taahhüdü, düzyazı + isteğe bağlı sayılar, **infaz** bitirişi) ve **Ezme** (çok altta kalan düşman tek satırla)
- [x] OP MC: **uyanış tetikleyicisi** (`content/awakenings.json`: koşul → gizli yetenek/evrim yolu); **Kademe Kırma** (büyük XP, "Kademe Kıran" unvan zinciri, Benzersiz şansı); gerçek stat/seviye görünür
- [x] Ölüm: kademe başına İkinci Şans (bedel), Demir Mod → Hasat; haddini aşma kuralı
- [x] Savaş sayaçları: `hareketli_atis`, `korunan_muttefik`, `arkadan_vurus`, `ilk_vurus_kritik_oldurme`, `kapi_boss_solo_ust5`
- [x] ≥15 düşman + Öğretici bossu (iki dilde)
- [x] Arayüz: eylem ızgarası, duruş seçimi, hedef seçimi, uzun basınca ayrıntı, savaş günlüğü, otomatik tur, infaz seçimi
- [x] Testler: formüller, aynı tohum = aynı savaş, kazanma oranı simülasyonu (ör. Seviye 5 vs Kanal Faresi)
Notlar:
- 102 motor testi, check.sh geçiyor. Simülasyon (İkinci Şans kapalı): Sv. 5 ↔ Kanal Faresi ≥ %95, Sv. 1 ↔ fare ≥ %75, Sv. 10 ↔ 3 Sis Tazısı %60+, Sv. 12 ↔ E kademe elit ≤ %20; Demir Mod'da sıradan savaştan kaçış ≥ %90.
- Emülatörde EN ve TR: prolog faresi gerçek savaş, infaz, seviye kartı, ganimet, Çanta sekmesinin açılması.
- Bağımsız inceleme bulguları düzeltildi (sersemleme süresi, kayıt noktası, yenilgi/İkinci Şans sırası, kaçan ≠ bağışlanan, bağlam değişkenleri, RNG akışı, otomatik tur iptali, bekleme süreleri, alan yeteneğinde efor, erişilemeyen zaaflar, EN artikel/dilbilgisi, TR zaman). DECISIONS #52–60.
- Bilinen eksik: kişi başı inisiyatif sırası ve yoldaşlar (Aşama 5).

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
