# Üslup — ansiklopedik ve sistem metinleri eki (USLUP Parti 9–10)

Önce docs/qa/uslup_brief.md'yi oku; oradaki kurallar geçerli (Görev 2 "kronik" bu dosyalarda yok). Bu ek, olay sahnesi
olmayan metinler içindir: NPC dosyaları, izler, bilgiler, görevler, başarılar, unvanlar, düşmanlar ve bestiyer, eşyalar,
yetenekler, sınıflar, uyanışlar, Özler, meslekler, Günlük Görev şablonları, Sistem metinleri (`system.yaml`).

## Kurallar
- **Adlara dokunma:** eşya/düşman/yetenek/unvan/sınıf/Öz/meslek **`ad`** alanları, `kisa_ad`, birlik ve bölge adları
  arayüzde ve başka metinlerde kimlik gibi geçer — aynen kalır. Yalnız açıklama, anlatı, satır, bilgi, görüş metinleri.
- **Ansiklopedik metin (USLUP §4):** geniş zaman, sade, bilgi verici, kısa; "…'nın bir türüdür", "Bu, …", "kullanıcıya
  … sağlar" gibi çeviri kokusu yok. Sayılar, yüzdeler, süreler, stat adları/kısaltmaları (GÜÇ, ÇEV, DAY, ZEK, BİL, ALG,
  İRD, KAR) ve oyun terimleri (Kademe, Öz, Yol, Kapı, Kule, Dikkat, Yozlaşma…) **birebir** korunur.
- **Sistem sesi (`system.yaml`, bildirim/köşeli parantezli satırlar):** bilerek kuru, resmi, kısa; isim cümlesi ağırlıklı.
  Yalnız Türkçe hatası ve çeviri kokusu düzeltilir; süsleme yok. Yer tutucular (`{n}`, `{npc}`, `{grade}`, `{skill}` …)
  aynı sayıda ve aynı biçimde kalır; **yer tutucunun ardına ek getirme** (sayıya/isme göre değişir): "{n} XP kazanıldı"
  gibi eksiz kalıp kullan.
- **Savaş metinleri** (`savas.metin`, `duello_metin`, düşman saldırı satırları `{dusman}`/`{npc}`): dinamik isme ek getirme;
  ismi özne yap. Dövüş ritmi kısa olabilir.
- **NPC görüşleri/hafızaları** (`gorusler`, `guc_tepkileri`, `hafizalar` …): karakterin konuşma sesi (npcs/<id>.yaml
  `kisilik`, `degerler`; GDD "Ana karakterler"). Yankı küçük harf, tırnaksız.
- Uzunluk eskisinin %60–%160'ı; belirteç sayısı aynı; `en`'e dokunma (bu dosyalarda kronik yok).

## Çıktı
Dosya başına bir JSON: `tools/qa/is/p9/<dosya yolunun content/ sonrası, / yerine __>.json`
(ör. `content/enemies/72_kule.yaml` → `tools/qa/is/p9/enemies__72_kule.json`). Biçim ve denetim uslup_brief.md'deki gibi
(`python tools/qa/uslup_apply.py check <yaml> <json>` → 0 hata). Değişmeyen dosya için JSON yazma.

## Rapor (≤150 sözcük)
Dosya başına değişen `tr` sayısı, check sonucu, 2 önce→sonra örneği, emin olmadıkların.
