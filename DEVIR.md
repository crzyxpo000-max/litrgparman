# DEVIR — Yükseliş Protokolü / Ascension Protocol

Yazıldığı gün: 2026-11-26. Dal: `main`, son commit `9f6da2c` (+ Paket 9).
Çalışma ağacı: `art/*.jpg` (7 dosya) izlenmiyor, bilinçli olarak depoya girmedi.

Bu dosya bir özet ve yol haritasıdır. Ayrıntının kaynağı: `CLAUDE.md` / `AGENTS.md` (kurallar), `docs/PROGRESS.md` (kutu kutu durum),
`docs/DECISIONS.md` (#1–#193), `docs/UI_REVIEW.md`, `docs/INCELEME_AKIS.md`, `docs/STORY_FLOW.md`, `docs/BALANCE.md`.

## 1. Şu anki durum

- Oyun baştan sona oynanıyor: prolog, 18 bölüm (4 perde), 9 son, New Game+, başlık Arşivi. Tamamen çevrimdışı, iki dilli (TR / EN-NA).
- Aşama 0–7 ve 9 bitti (Aşama 8 kaldırıldı). Üstüne "oyuncu gözüyle inceleme" ve **Paket 1–9** tamamlandı (`UI_REVIEW.md` P0/P1/arayüz P2 sıfırlandı).
- Tüm doğrulama adımları geçti (`:engine:validateContent`, `:engine:test`, `:tools:uslup:test`, `:tools:simulator:test`, `assembleDebug`, `lint`).
- Denge (1000 oynanış, `docs/BALANCE.md`): hakimiyet **%72,1** (hedef %60+), sonlar **%6,0–%18,3** (hedef %1–%40), kristal kilitlenme **%11,3** (hedef <%50).
- Yayın için bekleyen, kullanıcıda: imzalama anahtarı (`keystore.properties`) ve Google Play Console.

## 2. Yapılanlar (son dönem, eskiden yeniye)

| Commit | İş | Kararlar |
| --- | --- | --- |
| `8f4abe6` | Oyuncu gözüyle inceleme (`docs/INCELEME_AKIS.md`) ve **Paket 1: hikaye kopuklukları**. Susturulmuş Yankı kuralı (`WorldState.hasFlag`), Bölüm 7–11 düğüm düzeltmeleri. | #151–#155 |
| `c9cb51b` | **Paket 2: ilk saat.** Günlük Görev cezası kurallı (ilk 3 gün deneme, yalnız üs/kampta), infaz yalnız boss/zeki düşmanda, ödüller zafer kartında, operasyon onay penceresi, kendini açan arayüz, biten bölümün boş günleri kendiliğinden geçer. | #156–#160 |
| `fde916d` | **Paket 3: ritim.** Varış sahnesi yalnız ilk ziyarette, yolculuk eylem harcamaz, Perde 1'de el 2 kart, dünya seviyesi rampası (Gün 30–50), 6 yeni Perde 1 keşif olayı. | #161–#165 |
| `1286940` | **Paket 4: sistem çıkarma.** Meslek/üretim ve Kredi kapalı (kod duruyor, `Progression.*_ENABLED`), Öz Kristali tek para birimi (`oz_kristali_min` koşulu), Yozlaşma Dikkat'e katıldı, Ordu ve Bilgi günlüğü ilk kullanımda kalıcı açılır. | #166–#169 |
| `14ffe74` | **Paket 5: arayüz incelemesi** (`docs/UI_REVIEW.md`, 4 P0 · 18 P1 · 19 P2) ve seçilen düzeltmeler: onay pencereleri (yeni oyun, kaydet, yükle), geri tuşu, seçenekler metnin altında, "Diğer seçenekler" katlaması, üs eylem çubuğu, büyük yazı tavanları. | #170–#172 |
| `7b8d445` | **Paket 5, kalan P1'ler:** bölüm açılış/kapanış kartı, tam ekran son, TR terimler ("Nefes", "Görevler"), eşya açıklamaları, anahtar satırları, paralel içerik ayrıştırma. | #173–#176 |
| `9ba8b70` | **Final dengesi:** Vali savaşının destekleri yüzdeli (`@oyuncu-10%`), simülatör botu savaşın arkasındaki sahneyi görür. | #177–#178 |
| `b512faf` | README güncellendi. | — |
| `159c7f8` | **Paket 6: açılış hızı, kayıt güvenliği ve ilk saat arayüzü.** İkili içerik önbelleği (`content.bin`, soğuk yükleme 1570 → 61 ms), `onceki` oturum kaydı, 7 yeni kendini açan gösterge, `2.0x` yazı ölçeği ve 48dp hedef düzeltmeleri, `AGENTS.md`. | #179–#182 |
| `dd67b2f` | **Paket 7: savaş hissi, bosslar, final ve Arşiv.** Ekransız anında Ezme, boss faz çentikleri ve `★ Zaaf` vurgusu, hızlı otomatik savaş, kademeli epilog, bölüm kapanış özetleri, başlık Arşivi, katmanlı titreşim. | #183–#186 |
| `9f6da2c` | **Paket 8: denge, kristal telemetrisi ve yaşayan dünya.** Öz Kristali telemetrisi (%11,3 kilitlenme), Perde 2 açılış savaşları ve Perde 3–4 `@oyuncu±n%` ölçeklemeleri, `elif_yoldas_darbe` erişilebilirliği, zaman atlama bültenleri ve NG+ (`yeni_dongu`) Yankı fısıltıları. | #187–#190 |
| Paket 9 | **Paket 9: arayüz P2 kapanış turu.** Kayıtlar vurgusu (`B6`), Yetenekler ipucu (`B7`), kartlı Görevler (`C7`), ortadan iki yönlü Ahlak çubuğu (`D3`), perde gruplu Harita ve "Taşmak üzere" (`D4`), Evrim `Şimdi:` / `Her seviye:` (`D5`), Yenilgi bandı (`E5`), TR `Can`/`Yoldaşlar` (`F3`/`F4`), başlık yükleme göstergesi (`F6`). | #191–#193 |

Bu dönemde eklenen araçlar:

- `./gradlew :tools:simulator:fixtures --args="build/ui-fixtures 11"`: bir oynanışı simüle edip 61 anın kaydını üretir (ilk savaş, boss, infaz, sınıf teklifi, brifing, çok seçenekli sahne, son).
- `tools/qa/ui/`: `cap.py` (emülatörde koşul kurar, kayıt yükler, metne göre dokunur, görüntü alır), `s_all.py` (tüm ekranlar, bir koşul), `s_flow.py` (akış denetimleri), `s_a11y.py` (dokunma hedefi ve etiket ölçümü), `s_p1.py`, `s_confirm.py`, `s_sheet.py`, `s_shrink.py`. Pillow gerekir (`python -m pip install pillow`).
- `docs/ui-review/`: yaklaşık 680 görüntü, yaklaşık 30 MB. Ad biçimi `<ekran>__<dil>_<boyut>_<tema>_<yazı>[_sonra].png`.

## 3. Yarım kalanlar ve bilinen açıklar

Öncelik sırasıyla. Hiçbiri derlemeyi ya da oynanışı engellemiyor.

**Arayüz (`docs/UI_REVIEW.md`)**

- **B5, cihaz ölçümü:** `content.bin` ikili önbelleği (#179) masaüstünde soğuk yüklemeyi 1570 ms → 61 ms'ye indirdi; release derlemesinde gerçek Android cihazda henüz ölçülmedi.
- **A6, yapılmadı:** 6–8 görünür seçenekli 21 sahne içerik düzeyinde kademelendirilmedi (liste raporda). Katlama bu sahnelerde çalışıyor.
- `UI_REVIEW.md` kapsamındaki tüm P0, P1 ve arayüz P2 maddeleri (Paket 5–9) kapatıldı.
- TalkBack gerçek okuyucuyla hiç dinlenmedi; etiketler yalnız erişilebilirlik ağacından ölçüldü.

**Denge ve oynanış**

- Sv. 700'ün altında istilaya gelen oyuncu Vali'yi nadiren yeniyor; neden ölçülmedi. İstila emülatörde elle oynanmadı.
- Kristal ekonomisi (#187): erken oyunda kıtlık sağlıklı (Gün 30 p50: 10, Gün 60: 29; kilitlenme %11,3 < %50), ancak üretim kapalıyken (#166) geç oyunda bakiye birikiyor (Gün 180 p50: 309, Gün 365: 986).
- Kabul edilmiş riskler: Bölüm 16–18 taşma satırları hiç tetiklenmiyor (#140); bedava yolculukla el tazeleme (#162).

**Kod temizliği (zararsız)**

- Kapatılan sistemlerin metinleri ve alanları duruyor (`Progression.*_ENABLED` ile geri açılabilir): `professions.yaml`, `system.yaml` içindeki `kredi` / `yozlasma` / `meslek_*`, `strings.xml` içindeki `currency_line`, `no_profession`, `help_corruption*`, `status_corruption`.

## 4. Sıradaki adımlar (önerilen sıra)

1. **Yayın hazırlığı (kullanıcı):** keystore oluştur, `keystore.properties` yaz, `./gradlew bundleRelease`, Play Console kaydı. Mağaza görselleri için `art/*.jpg` karar bekliyor (depoya girecek mi).
2. **Release derlemesini gerçek cihazda dene:** `content.bin` ile açılış süresi, R8 sonrası çökme var mı, TalkBack dinlemesi.
3. **Elle oynanış turu:** Bölüm 1–3 ve Kadim istilası baştan sona, emülatörde ya da cihazda.
4. **İçerik (isteğe bağlı):** A6'daki 21 sahneyi kademelendir (`/olay-yaz` kurallarıyla, `tr-editor` ve `en-localizer` incelemesi); geç oyun için isteğe bağlı kristal harcama yerleri.

## 5. Çalışma düzeni ve ortam

- Kurallar `CLAUDE.md`'de; her paketin sonunda: `scripts/check.sh` geçer, simülatör 1000 oynanış (hakimiyet %60+, sonlar %1–%40), GDD / CLAUDE.md / DECISIONS / PROGRESS güncellenir, commit atılır.
- Kullanıcı tercihleri: hikaye kalitesinden kısılmaz; içerik ve QA işinde paralel alt ajan; aynı klasörde iki yazar olmaz (worktree); paket başında kısa plan ve onay.
- JDK: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. adb: `C:/Users/alpar/AppData/Local/Android/Sdk/platform-tools`. Emülatör: `Medium_Phone_API_36.0`.
- Betik ipuçları (Windows / Git Bash): uzun Python'u dosyaya yazıp `python -X utf8 dosya.py < /dev/null` ile çalıştır; CRLF dosyalarda satır sonunu koru (`chapters.yaml`, `events/08–09`, `docs/*.md` çoğu); `tools/qa/ui` koşuları ekran boyutunu değiştirir, iş bitince `wm size reset; wm density reset; settings put system font_scale 1.0`.
