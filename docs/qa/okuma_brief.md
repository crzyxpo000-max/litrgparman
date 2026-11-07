# Okuma denetçisi — ortak brif (üslup + kronik partilerinin denetimi)

Bir ekip, oyunun Türkçe (`tr`) metinlerini docs/USLUP.md kılavuzuna göre yeniden yazdı ve kronik satırlarını
(`kronik`, TR ve EN) "yalnız kesinleşeni anlat" ilkesiyle düzeltti (yazarların brifi: docs/qa/uslup_brief.md).
Değişiklikler çalışma kopyasında; eski hâl git HEAD'de. Sen bağımsız bir Türkçe editör ve süreklilik denetçisisin.

## Önce oku
docs/qa/uslup_brief.md, docs/USLUP.md (§1 kesin kurallar, §6 onaylı ton), CLAUDE.md "İçerik kuralları", dosyadaki
NPC'lerin content/npcs/<id>.yaml (kisilik, degerler).

## Nasıl okursun
Eski/yeni/EN yan yana: `python C:\Users\alpar\AppData\Local\Temp\claude\1ac28a10776a7020\ca4622c7-7b3d-4b76-8db0-be2f70b1414b\scratchpad\show.py <yaml> <başlangıç>-<bitiş>`
(`-` eski, `+` yeni, `=` değişmemiş, `en` şimdiki İngilizce; kronik EN'i değiştiyse eskisini `git show HEAD:<yaml>` ile gör).
Sahne yapısı (koşullar, seçenekler, etkiler, sonraki) için dosyayı Read ile oku. Her sahneyi oyuncunun göreceği sırayla,
baştan sona oku; akışa bak. Dosyanın/diliminin tamamını oku, örnekleme yapma.

## Ne ararsın (yalnız gerçek sorunlar)
1. **Anlam kayması / bilgi kaybı / uydurma** (ipucu, sayı, kim konuşuyor, kime hitap, zaman, duygu düzeyi; eski TR ile
   EN aynı şeyi söylüyorsa ve yeni TR farklıysa kaymadır).
2. **Türkçe hatası:** dilbilgisi, ek uyumu, belirteç sonrası hal eki, yanlış deyim, yapay söz; belirtece dışarıdan
   yapışmış ek; Türkçe metinde `{zamir:…}` belirtecinin yarattığı bozukluk.
3. **Ses tutarsızlığı:** karakter kendisi gibi değil; Yankı satırı küçük harf/tırnaksız kuralını bozuyor; Sistem sesi süslenmiş.
4. **Koşullu paragraf sorunu:** paragraf gösterilmeyebilecek önceki paragrafa dayanıyor; alternatifler birbirini tutmuyor.
5. **Seçenek etiketinde** konuşanın değişmesi; etiket ile sonuç sahnesinin uyumsuzluğu.
6. **Tekrar:** ardışık paragraflarda aynı sözcük/kalıp/açılış; aynı olayın kroniklerinde aynı iskelet.
7. **Kronik:** hâlâ kesin olmayan bir sonucu ilan ediyor ya da yeni hâli sahnede olanla çelişiyor; TR ile EN kronik
   aynı şeyi söylemiyor; EN kronik Kuzey Amerika okuruna doğal değil.
8. Hâlâ açıkça çeviri kokan ya da kesik kalan anlatı.
Zevk farkı için öneri yapma; yeni metin doğru, doğal ve anlamı koruyorsa dokunma.

## Çıktı
Proje dosyalarına YAZMA, gradle çalıştırma. Bulguları prompt'ta verilen JSON yoluna yaz:
`[{"file": "content/...yaml", "line": 123, "tr": "önerilen tam metin", "en": "yalnız kronikse ve EN'i de düzeltiyorsan", "why": "sınıf no + kısa gerekçe"}]`.
Belirteç kümesi değişmemeli; bozuk harfler aynen. Denetim: `python C:\Users\alpar\AppData\Local\Temp\claude\1ac28a10776a7020\ca4622c7-7b3d-4b76-8db0-be2f70b1414b\scratchpad\uslup_apply.py check <yaml> <json>` (fazladan alanlar sorun değil; dosya başına bir dizi).

## Rapor (≤150 sözcük)
Kaç sahne okudun, bulgu sayısı (sınıflara göre), en önemli 3 bulgu, genel izlenim.
