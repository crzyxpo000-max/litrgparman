# Üslup + kronik dilim ajanı — ortak brif (QA P1-15 + P1-5)

Bu brif, Perde 2–4 Türkçe metinlerini yeniden yazan dilim ajanları içindir. Her ajan kendi dilimini (dosya + satır
aralığı) ve çıktı yolunu prompt'unda alır. Tasarım bağlamı: docs/USLUP.md (Parti 0–1 bitti; bu iş Parti 2–8).

## Önce oku (atlama)
1. **docs/USLUP.md'nin tamamı**: §1 kesin kurallar, §2–§5 üslup, §6 kullanıcının onayladığı önce→sonra tonu (ölçüt).
2. **CLAUDE.md "İçerik kuralları"**: belirteçler, hal ekleri (`{npc:x|e}`), Yankı'nın küçük harfli tırnaksız sesi,
   "ve"den önce virgül yok, sayıya ek getirmeme. DECISIONS #128: prolog ve uyanışlardaki tırnaklı Yankı kasıtlı.
3. Diliminde geçen NPC'lerin `content/npcs/<id>.yaml` dosyalarından `kisilik`, `degerler`, `hedefler` ve
   docs/GDD.md "### Ana karakterler" bölümü (~299. satır). Karakterin sesi diyaloğa yansımalı.
4. Dilimini baştan sona oku (Read offset/limit); sahne akışını, koşullu paragrafları, seçenekleri ve etkileri anla.
   Perde 1'in yeniden yazılmış hâlinden ton örneği için `python tools/qa/olay.py kirilma --tr` çıktısına göz at.

## Görev 1 — Türkçe üslup (tüm `tr` değerleri)
- Her `tr` değerini USLUP kılavuzuna göre çağdaş, akıcı, edebi Türkçeyle yeniden yaz: İngilizce sözdizimi kalıpları,
  kesik gerilim romanı ritmi, "-yor" tekdüzeliği, "bir" fazlalığı, noktalı virgül bolluğu, "Bu, …" açıklamaları,
  "X yapmayı seçtin", özne + virgül, ", aynı anda", calque'lar ("sadece söylüyorum") gitmeli. Yüzeysel sözcük
  değişikliği yetmez; cümleyi Türkçe kur. İyi olan cümleyi zorla değiştirme.
- **Anlam ve bilgi korunur** (ipucu, sayı, kimin konuştuğu, kime hitap edildiği, gerilim düzeyi). Bilgi ekleme yok.
- **Her paragraf kendi başına durmalı** (çoğunun `kosul`'u var); alternatif koşullu paragraflar birbirinin yerine geçer.
  Koşulsuz ardışık paragraflar bir akış gibi okunsun; aynı açılış ve aynı ekle biten cümle tekrarını kır.
- Belirteçler aynı sayıda (ek kısmı değişebilir); belirtecin dışına ek yapıştırma; düz isim yazma; bozuk harfler aynen.
- Yankı satırları küçük harfle başlar, tırnaksızdır (Yankı'nın sözünde "sistem" de küçük). Sistem sesi (köşeli
  parantezli bildirim satırları) kuru kalır; yalnız Türkçe hatası ve çeviri kokusu düzeltilir.
- Stat koşullu sezgi metni `[ALG]` gibi bir önekle **başlamaz** (motor ekler; validator hata verir).
- Seçenek etiketleri kısa emir kipi; konuşma seçenekleri tırnak içinde, karakterin ağzına yakışır; ekranda ~60
  karakteri geçmesin. Uzunluk eskisinin %60–%160'ı.
- Dünyada Entegrasyon'dan bu yana **bir yıldan az** geçti: "yıllardır" yalnız Entegrasyon öncesine uzanan bir şey için
  doğru; değilse "aylardır / Entegrasyon'dan beri".
- Türkçe dil hatalarını düzelt (ör. "Hasatın" → "hasadın" ya da özel ad olarak tutarlı "Hasat'ın"; "bir sürü balık
  gibi" → "balık sürüsü gibi"; "kimse ona zorlamıyor" → "kimse onu zorlamıyor"; "steteskop" → "stetoskop").

## Görev 2 — Kronik doğruluğu (seçeneklerdeki `kronik` alanı; `tr` VE `en`)
Kronik satırı son ekranındaki "en kritik seçimler" kartıdır; geçmiş zamanla yazılır. **Yalnız seçimin anında kesinleşeni
anlatmalı.** Bir kronik şunlardan birini yapıyorsa iki dilde birlikte yeniden yaz:
- Seçimden sonra gelen ve kesin olmayan bir sonucu ilan ediyor (savaş kaybedilebilir, kişi sonra ölebilir/dönebilir,
  "o ordu bir kez bile dağılmadı", "aylar sonra o harfler bir kapıyı açtı", "hiçbiri o yıl … çalmadı"). Seçeneğin
  `etkiler`/`savas`/`sonraki`'sine bak: vaat edilen şeyi hiçbir bayrak, koşul ya da sahne karşılamıyorsa vaat yanlıştır.
- Oyuncuya her yolda doğru olmayan bir geçmiş yüklüyor (ör. herkese "sana silahını ilk öğreten adam").
Düzeltme: kararı ve onun anlık anlamını anlat ("harfleri ezberledin; bir gün işe yarayacaklarını umdun").
Kronik iskeletleri çeşitlensin: aynı olayın kronikleri aynı iki sözcükle ("İhanet gecesi…", "On the night of…")
başlamasın. **İngilizce** yalnız kronik satırlarında değişebilir; Kuzey Amerika okuruna doğal, kısa, belirteçler aynı.

## Yasaklar
- **Proje dosyalarına yazma.** Yalnız kendi JSON çıktını (ve istersen scratchpad'de taslak) yaz. Gradle çalıştırma.
- `tr` dışında hiçbir şeyi değiştirme; tek istisna kronik satırlarının `en`'i. Anahtar, koşul, etki, sıra, satır sayısı
  aynı kalır; paragraf birleştirme/bölme/silme yok.

## Çıktı
UTF-8 JSON dizi: `[{"line": 123, "tr": "yeni metin"}, {"line": 456, "tr": "…", "en": "…"}]`. `line` = dosyadaki 1
tabanlı satır numarası (Read çıktısındaki). Yalnız değişen satırlar; `en` yalnız kronik satırlarında. Çift tırnak `\"`.
Denetim (0 hata olmalı): `python C:\Users\alpar\AppData\Local\Temp\claude\1ac28a10776a7020\ca4622c7-7b3d-4b76-8db0-be2f70b1414b\scratchpad\uslup_apply.py check <yaml> <json>`.
Uyarıları gözden geçir (Yankı/diyalogda kısa cümle kabul edilebilir; anlatıda art arda -yor ve kesik ritim düzelmeli).
Sonra yeni metni sahne sahne yeniden oku: akıyor mu, bilgi tam mı, kronikler dürüst mü?

## Rapor (≤200 sözcük)
Kaç `tr` ve kaç kronik (`en` dahil) değişti, check sonucu, 2 önce→sonra örneği, vaadi düzeltilen kroniklerin kimlikleri,
emin olmadığın noktalar.
