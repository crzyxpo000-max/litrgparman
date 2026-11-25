# DEVIR — Yükseliş Protokolü / Ascension Protocol

Yazıldığı gün: 2026-11-25. Dal: `main`, son commit `b512faf`. `origin/main` ile eşit (itilmemiş commit yok).
Çalışma ağacı temiz; yalnız `art/*.jpg` (7 dosya) izlenmiyor, bilinçli olarak depoya girmedi.

Bu dosya bir özet ve yol haritasıdır. Ayrıntının kaynağı: `CLAUDE.md` (kurallar), `docs/PROGRESS.md` (kutu kutu durum),
`docs/DECISIONS.md` (#1–#178), `docs/UI_REVIEW.md`, `docs/INCELEME_AKIS.md`, `docs/STORY_FLOW.md`, `docs/BALANCE.md`.

## 1. Şu anki durum

- Oyun baştan sona oynanıyor: prolog, 18 bölüm (4 perde), 9 son, New Game+. Tamamen çevrimdışı, iki dilli (TR / EN-NA).
- Aşama 0–7 ve 9 bitti (Aşama 8 kaldırıldı). Üstüne "oyuncu gözüyle inceleme" ve beş paket yapıldı; hepsi commit'li ve itilmiş.
- `scripts/check.sh` son çalıştırmada geçti (test, validateContent, denge testi, assembleDebug, lint).
- Denge (1000 oynanış, `docs/BALANCE.md`): hakimiyet %70,6 (hedef %60+), sonlar %6,6–%17,7 (hedef %1–%40).
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

Bu dönemde eklenen araçlar:

- `./gradlew :tools:simulator:fixtures --args="build/ui-fixtures 11"`: bir oynanışı simüle edip 61 anın kaydını üretir (ilk savaş, boss, infaz, sınıf teklifi, brifing, çok seçenekli sahne, son).
- `tools/qa/ui/`: `cap.py` (emülatörde koşul kurar, kayıt yükler, metne göre dokunur, görüntü alır), `s_all.py` (tüm ekranlar, bir koşul), `s_flow.py` (akış denetimleri), `s_a11y.py` (dokunma hedefi ve etiket ölçümü), `s_p1.py`, `s_confirm.py`, `s_sheet.py`, `s_shrink.py`. Pillow gerekir (`python -m pip install pillow`).
- `docs/ui-review/`: yaklaşık 680 görüntü, yaklaşık 30 MB. Ad biçimi `<ekran>__<dil>_<boyut>_<tema>_<yazı>[_sonra].png`.

## 3. Yarım kalanlar ve bilinen açıklar

Öncelik sırasıyla. Hiçbiri derlemeyi ya da oynanışı engellemiyor.

**Arayüz (`docs/UI_REVIEW.md`)**

- **B5, kısmen:** "Dünya yükleniyor" emülatörde (debug) yaklaşık 8 sn'den 4,3 sn'ye indi; hedef 3 sn sağlanmadı. Release derlemesinde gerçek cihazda hiç ölçülmedi.
- **B3, kısmen:** Yükle onay soruyor ama "Önceki oturum" satırı yok; yüklenen oyunun ilk seçiminde eski otomatik kayıt eziliyor.
- **E1, kısmen:** son ekranı tam ekran oldu; epilog hâlâ tek uzun kaydırma (sırayla açılan kartlar yapılmadı).
- **E3, sade:** bölüm kapanış kartında yalnız ad ve gün var; "bu bölümde yaptıkların" ve sonraki bölümün adı için motor verisi gerekiyor.
- **A6, yapılmadı:** 6–8 görünür seçenekli 21 sahne içerik düzeyinde kademelendirilmedi (liste raporda). Katlama bu sahnelerde çalışıyor.
- **19 P2'nin tamamı açık** (A5 kapandı): C5–C7, D1, D3–D5, E4, E5, F3–F6, G3–G5, B6, B7.
- Düzeltme sonrası görülen üç küçük sorun (ölçek 2.0): üste "Kalan eylem" üç satır, Durum alt sekmesi "Unvanl" diye kesik, savaş günlüğünün ilk satırı kırpık.
- TalkBack gerçek okuyucuyla hiç dinlenmedi; etiketler yalnız erişilebilirlik ağacından ölçüldü.
- Zafer ve yenilgi kartları görüntülenmedi (kayıtta tutulmuyor); büyük anlar `demo_moment` ile çekildi.

**Denge ve oynanış**

- Perde 3–4'teki öteki `@oyuncu±n` yeniden deneme indirimleri o seviyelerde etkisiz; yalnız Vali savaşı yüzdeliye çevrildi (#177).
- Sv. 700'ün altında istilaya gelen oyuncu Vali'yi nadiren yeniyor; neden ölçülmedi. İstila emülatörde elle oynanmadı.
- Perde 2 açılış hikaye savaşlarında kayıp yaklaşık %50 (`eski_sehir_kapi_avcilari`, `liman_yan_yatmis_vapur`, `kordon_fener_baligi`); hedef %30.
- Kristal ekonomisi ölçülmüyor: ödeme seçeneklerinin ne sıklıkla kilitli kaldığı simülatörde yok (#167'nin yeniden bakma koşulu).
- Kabul edilmiş riskler: Bölüm 16–18 taşma satırları hiç tetiklenmiyor (#140); bedava yolculukla el tazeleme (#162).

**Kod temizliği (zararsız)**

- `NpcEngine` güce tepki koşulunda `state.corruption >= 20` artık ölü dal.
- Kapatılan sistemlerin metinleri ve alanları duruyor: `professions.yaml`, `system.yaml` içindeki `kredi` / `yozlasma` / `meslek_*`, `strings.xml` içindeki `currency_line`, `no_profession`, `help_corruption*`, `status_corruption`.
- `docs/PROGRESS.md` başındaki "Son commit" satırı eski (`fde916d` yazıyor).

## 4. Sıradaki adımlar (önerilen sıra)

1. **Yayın hazırlığı (kullanıcı):** keystore oluştur, `keystore.properties` yaz, `./gradlew bundleRelease`, Play Console kaydı. Mağaza görselleri için `art/*.jpg` karar bekliyor (depoya girecek mi).
2. **Release derlemesini gerçek cihazda dene:** açılış süresi (B5), R8 sonrası çökme var mı, TalkBack dinlemesi. Açılış hâlâ 3 sn'nin üstündeyse ayrıştırılmış içeriği ikili önbelleğe al (#175).
3. **Elle oynanış turu:** Bölüm 1–3 ve Kadim istilası baştan sona, emülatörde ya da cihazda. Paket 2–5 değişiklikleri simülatör ve ekran görüntüsüyle doğrulandı; uzun bir insan oturumuyla doğrulanmadı.
4. **Denge:** Perde 2 açılış savaşları (düşman başvuruları), Perde 3–4 yeniden deneme indirimlerini yüzdeliye çevir, simülatöre kristal bakiyesi ve kilitli ödeme oranı ölçümü ekle.
5. **Arayüz P2 turu:** önce ölçek 2.0'daki üç küçük sorun ve G3 (48dp hedefler), sonra D1 (ilk saatte gizlenebilecek 7 öğe), E4 (boss sunumu), C7 (Görevler sayfası).
6. **İçerik:** A6'daki 21 sahneyi kademelendir (`/olay-yaz` kurallarıyla, `tr-editor` ve `en-localizer` incelemesi).
7. **Temizlik:** kullanılmayan metinler, ölü dal, PROGRESS başlığı.

## 5. Çalışma düzeni ve ortam

- Kurallar `CLAUDE.md`'de; her paketin sonunda: `scripts/check.sh` geçer, simülatör 1000 oynanış (hakimiyet %60+, sonlar %1–%40), GDD / CLAUDE.md / DECISIONS / PROGRESS güncellenir, commit atılır.
- Kullanıcı tercihleri: hikaye kalitesinden kısılmaz; içerik ve QA işinde paralel alt ajan; aynı klasörde iki yazar olmaz (worktree); paket başında kısa plan ve onay.
- JDK: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. adb: `C:/Users/alpar/AppData/Local/Android/Sdk/platform-tools`. Emülatör: `Medium_Phone_API_36.0`.
- Betik ipuçları (Windows / Git Bash): uzun Python'u dosyaya yazıp `python -X utf8 dosya.py < /dev/null` ile çalıştır; CRLF dosyalarda satır sonunu koru (`chapters.yaml`, `events/08–09`, `docs/*.md` çoğu); `tools/qa/ui` koşuları ekran boyutunu değiştirir, iş bitince `wm size reset; wm density reset; settings put system font_scale 1.0`.
