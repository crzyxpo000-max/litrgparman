---
name: olay-yaz
description: Verilen bölüm ve konum için content/events altına şemaya uygun, iki dilli, belirteç kullanan yeni bir olay yazar; doğrular ve tr-editor ile en-localizer'a inceletir. Kullanım /olay-yaz <bölüm no> <konum> [konu]
argument-hint: <bölüm> <konum> [konu]
---

# /olay-yaz $ARGUMENTS

1. **Bağlamı oku.** `CLAUDE.md` (İçerik kuralları), `docs/USLUP.md`, `docs/CHAPTERS.md` (bölümün tehdidi ve gün aralığı),
   `content/chapters.yaml` (bölümün `acilis`/`son_tarih`/`bitis` günleri), şema örneği olarak aynı bölgedeki 1–2 olay.
   Konum `us` (üs) ya da bir bölge kimliğidir (`content/regions.yaml`); başka bölgede geçen olay üste görev kartı olur.
2. **Olayı yaz** (uygun `content/events/NN_*.yaml` dosyasına, yoksa yeni parça dosyası):
   - `id`, `konum: [..]`, `perde`, `gun {min,max}` bölümün aralığında, `agirlik`, `etiketler`, `gerilim` 1–4, `npc`, `kart {tr,en}`.
   - Giriş sahnesinde **en az 3 seçenek**, en az biri koşullu (`gerekli` ya da `goster`); en az biri tohum eksin
     (`tohum: {olay, gecikme, kosul, hatirlatma: {tr,en}}`) ve biri NPC ilişkisini değiştirsin (`iliski`).
   - Her metin `{ tr, en }`; isimler belirteçle (`{npc:..}`, `{yer:..}`, `{zamir:..}`); son seçenekler `"sonraki": "@akis"`.
   - Yeni bayrak varsa `content/flags/` altına açıklamasıyla ekle.
3. **Doğrula:** `./gradlew -q :engine:validateContent` (Windows'ta JAVA_HOME = Android Studio jbr). Hata kalmayana kadar düzelt.
4. **İnceletir:** `tr-editor` ve `en-localizer` alt ajanlarını yeni olayın dosya ve satır aralığıyla çalıştır (sırayla).
   Bulgularını uygula, yeniden doğrula.
5. **Rapor:** olay kimliği, dosya:satır, seçenekler ve koşulları, eklenen tohum/bayraklar, incelemeden uygulananlar.
