# Arayüz incelemesi (Paket 5) — 2026-11-10

## Özet

1. Arayüz tutarlı ve kimlikli; büyük anlar (seviye, evrim, Benzersiz, sıralama, ihlal) gerçekten özel. Sorun düzende ve güvenlikte.
2. **Veri kaybı riski üç yerde:** kayıt varken Yeni oyun, dolu slota Kaydet ve Yükle onay sormuyor. Geri tuşu oyunun her yerinde uygulamadan çıkıyor.
3. **Hikaye metni ekranın ana unsuru değil:** 360dp telefonda anlatı ekranın yaklaşık %30'unu, seçenek paneli %38'ini alıyor; üs ekranının üst yarısı boş.
4. **Yazı ölçeği 2.0'da okunmuyor:** etiketli seçenekler harf harf kırılıyor, alt sekme adları ve savaş düğmeleri kesiliyor, anlatı 3 satıra iniyor.
5. Bölüm brifingi, bölüm sonu ve son ekranı sıradan sahne gibi geçiyor; dünya yüklemesi her açılışta yaklaşık 8 saniye.

**En çok fark yaratacak 3 düzeltme**

1. **Onay pencereleri + geri tuşu** (B1–B4): Yeni oyun, Kaydet (dolu slot), Yükle için onay; geri tuşu önce alt sayfayı/sekmeyi kapatsın, Hikaye'de "çıkmak için tekrar bas".
2. **Hikaye düzeni** (A1–A5): seçenekler sabit panel yerine metnin altında akışın devamı; 5'ten fazlası "Diğer seçenekler" altında; etiket seçeneğin üstünde ayrı satır; üste rutin eylemler ayrı çubuk.
3. **Büyük yazı dayanıklılığı** (C1–C4): etiket/seçenek dikey dizilim, sekme adları tek satır kısa biçim, savaş düğmeleri 2 sütun, başlık satır kırma.

## Yöntem ve kapsam

- Emülatör `Medium_Phone_API_36.0`; koşullar `wm size/density`, `font_scale`, uygulama ayarları (`shared_prefs`) ile kuruldu.
- Ekranlara gerçek kayıtlarla gidildi: `./gradlew :tools:simulator:fixtures` bir oynanışı simüle edip 61 anın kaydını üretir (ilk savaş, boss, infaz, sınıf teklifi, brifing, çok seçenekli sahne, son …); `tools/qa/ui/cap.py` kaydı yükler, metne göre dokunur, görüntü alır.
- Görüntüler `docs/ui-review/<ekran>__<dil>_<boyut>_<tema>_<yazı>.png` (yarı çözünürlük, 452 dosya). `_k1`, `_k2` aynı ekranın kaydırılmış hali.

| Etiket | Dil | Ekran | Tema | Yazı | Ekran sayısı |
| --- | --- | --- | --- | --- | ---: |
| `tr_small_dark_n` | TR | 360dp | koyu | normal | 103 (tam set) |
| `en_small_dark_n` | EN | 360dp | koyu | normal | 54 |
| `tr_small_light_n` | TR | 360dp | açık | normal | 54 |
| `tr_small_dark_f2` | TR | 360dp | koyu | ölçek 2.0 | 50 |
| `en_small_light_f2` | EN | 360dp | açık | ölçek 2.0 | 50 |
| `tr_tablet_dark_n` | TR | 800dp | koyu | normal | 54 |
| `en_tablet_light_f2` | EN | 800dp | açık | ölçek 2.0 | 54 |
| `tr_small_dark_f2xl` | TR | 360dp | koyu | 2.0 + "Çok büyük" | 26 |

Sınırlar: 16 koşulun tamamı değil, her eksenin en az iki kez göründüğü 8 koşul çekildi. TalkBack canlı dinlenmedi; erişilebilirlik ağacı
(`uiautomator dump`) üzerinden etiket ve hedef boyutu ölçüldü. Zafer kartı ve gerçek seviye atlama anı kayıtta tutulmadığı için büyük anlar
`demo_moment` ile çekildi. Yenilgi kartı ayrı görülmedi (yalnız yenilgi sahnesi).

## A. Hikaye/seçenek dengesi (yüksek öncelik)

### Ölçüm: sahne başına seçenek

3058 sahne tarandı (`tools/qa/ui` dışındaki betik: görünür seçenek = koşulsuz + en büyük `goster` grubu, en kötü durum).

| Görünür seçenek | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Sahne | 1007 | 1116 | 550 | 246 | 118 | 18 | 2 | 1 |

- Sahnelerin %95'i 1–4 seçenek; 5 ve üzeri 139 sahne, **6 ve üzeri 21 sahne**. Tanımda 6+ seçeneği olan 73 sahnenin 52'si `goster` dallarıyla 5'in altına iniyor.
- Üs ekranı (Gün 4): 4 fırsat kartı + operasyon düğmesi + Antrenman, Dinlen, Kapılar, Yolculuk = **9 dokunulabilir öğe**, hepsi tek panelde.
- Görev ekranı (Kule Dibi): 1 kart + Keşfet, Antrenman, Dinlen, Kule, Kapılar, Üsse dön = 7 öğe.

### Ölçüm: ekran payı (360×800dp, normal yazı)

| Bölge | Hikaye sahnesi | Üs |
| --- | ---: | ---: |
| Üst şerit (gün, HP/MP, bölüm çubuğu, "Önceki sahneler") | %23 | %14 |
| Anlatı | **%26–32** | %8 (2 satır) + **%27 boş** |
| Seçenek paneli (`ActionDock`, tavan %38) | **%38** | %38 |
| Alt sekmeler | %13 | %13 |

Yazı ölçeği 2.0'da anlatı 3 satıra iniyor (`sahne_8_secenek__tr_small_dark_f2`); panel içinde 8 seçenek için 3 kez kaydırmak gerekiyor.

### Bulgular

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| A1 | `sahne_3_secenek`, `sahne_8_secenek`, `sahne_uzun` (`tr_small_dark_n`) | Anlatı ekranın %30'u; metin panelin altında kesiliyor, okumak için dar bir pencerede kaydırılıyor. Seçenek paneli sabit ve %38. | P1 | Sabit paneli kaldır: seçenekler metnin altında aynı kaydırmada gelsin. İlk açılışta ekran metnin başında dursun; alt kenarda "seçenekler ↓" ipucu. | Orta (StoryScreen düzeni) |
| A2 | `sahne_8_secenek`, `_k1`, `_k2` | 8 seçenek 38'lik panelde 3 ekran; hangisinin son olduğu belli değil. | P1 | İlk 4–5 seçenek görünür; kalanı "Diğer seçenekler (3)" başlığı altında katlı. Kilitli seçenekler katlı kısma önce düşsün. | Küçük–orta |
| A3 | `sahne_8_secenek` | Uzun etiket ("[Canavar Çekirdeği Parçası]") seçenek metnini dar sütuna itiyor: 4 satır, kelime bölünmesi. | P1 | Etiket seçeneğin üstünde küçük ayrı satır; metin tam genişlik. Düğme tek satır, sığmazsa küçük yazıyla en çok 2 satır, dikey boşluk 10dp'den 6dp'ye. | Küçük |
| A4 | `us_erken`, `us_perde2`, `us_operasyon_b2` | Üs ekranının üst yarısı boş; kartlar, operasyon ve rutin eylemler tek panelde karışık, kartlar panel içinde kayıyor. | P1 | Kartlar (hikaye) sahne alanına, metnin altına; Antrenman, Dinlen, Keşfet, Kule, Kapılar, Yolculuk, Zaman atla ayrı ince bir eylem çubuğuna (alt sekmelerin üstü, yatay kaydırmalı çipler). Operasyon düğmesi kartların üstünde tek başına. | Orta (HubPanel) |
| A5 | `gorev_perde2`, `us_erken_k1` | Rutin eylemler iki farklı biçimde: hap düğme (Antrenman, Dinlen) ve kesik köşe çerçeve (Kapılar, Yolculuk, Kule); "Kapılar (3)" sarı. | P2 | Eylem çubuğunda tek biçim; renk yalnız tehlike/uyarı için. | Küçük |
| A6 | 21 sahne (liste aşağıda) | 6–8 görünür seçenek. | P2 | Kademelendirme: önce 3 yön ("Konuş / Güç kullan / Geç"), sonra alt seçenekler; ya da eşya takası seçeneklerini tek "Takas et…" altında topla. | İçerik, sahne başına küçük |

**6+ görünür seçenekli sahneler ve öneri**

| Sahne | Görünür | Öneri |
| --- | ---: | --- |
| `cop_avcisi_pazar_1` (30_carsi_istasyon) | 8 | 3 takas seçeneğini "Takas et…" alt sahnesine topla → 5 |
| `kaya_darbe_gecesi_1`, `kaya_son_savas_generali_1` (52_lonca_kaya) | 7 | Önce taraf seç (Kaya / Lonca / uzak dur), sonra yöntem |
| `balkon_1`, `kayiplar_1` (60_kiyikent_a) | 6 | Stat kilitli iki seçeneği "Diğer seçenekler"e bırakmak yeter (A2) |
| `tepe_besik_sinavi_1` (61_kiyikent_b) | 6 | Aynı |
| `bl9_kordon_1`, `bl9_kat_fiyati_1` (58_bolge_p2c) | 6 | Ödeme ve pazarlık seçeneklerini tek "Öde ya da pazarlık et" altında kademelendir |
| `kadimler_2_vergi_1` (55), `yuk2_defter` (53), `lonca_3_rakip_bastirma_1`, `kaya_lonca_teklifi_1` (52) | 6 | Fraksiyon sahneleri: önce tutum (kabul / pazarlık / ret), sonra ayrıntı |
| `siginak_3_tahliye_1`, `siginak_1_konsey_1` (51) | 6 | Aynı |
| `donus_kamplar`, `secim_1` (50_perde2_ana) | 6 | Kamp seçimi: 4 kamp + "kimse" + "sonra" zaten liste; kart ızgarası (2 sütun) olarak göster, kademelendirme gerekmez |
| `vahit_vaaz_1`, `su_baskini_1`, `mert_sifreli_mesaj_1`, `carsi_cetesi_1` (30), `zehirli_bereket_1` (20) | 6 | Perde 1: ilk saat sahneleri; kilitli seçenekleri katla, 4 görünür kalsın |

## B. Geri bildirim ve akış

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| B1 | `akis_yeni_oyun` | Kayıt varken "Entegrasyonu başlat" sormadan otomatik kaydın üstüne yeni oyun yazıyor (bir önceki kopya `auto.prev` kalıyor ama arayüzden erişilmiyor). | **P0** | Otomatik kayıt varsa onay: "Sim · Seviye 16 · Gün 60 kaydının üstüne yazılacak". Demir Mod'da ayrıca vurgula. | Küçük |
| B2 | `akis_kaydet_ustune` | Dolu slota Kaydet onaysız üstüne yazıyor. | **P0** | Dolu slotta onay penceresi (eski ve yeni özet satırı). | Küçük |
| B3 | `akis_yukle` | Yükle onaysız; o anki ilerleme yalnız otomatik kayıtta kalıyor, o da yüklenen oyunun ilk seçiminde eziliyor. | **P0** | Onay + yüklemeden önce mevcut durumu `auto.prev`'e al ve "Önceki oturum" satırı olarak göster. | Küçük–orta |
| B4 | (akış denetimi) | Geri tuşu Hikaye, Durum, Çanta ve savaşta doğrudan uygulamadan çıkıyor; yalnız Daha fazla alt sayfası geri alıyor. | P1 | Sıra: açık pencere/alt sayfa → başka sekmedeyse Hikaye → Hikaye'de "çıkmak için tekrar bas" (2 sn). | Küçük |
| B5 | `baslik__*` | Açılışta "Dünya yükleniyor…" yaklaşık 8 sn (etkinlik 1,6 sn'de açılıyor); Devam et bu sürede gri ve neden gri olduğu yalnız küçük satırda. | P1 | İçeriği arka planda yüklerken ilerleme çubuğu; ayrıştırılmış içeriği önbelleğe al (ikili/JSON) ya da bölüm bölüm yükle. Hedef < 3 sn. | Orta–büyük |
| B6 | `dahafazla_kayitlar` | "Oyun kaydedildi" bildirimi var ama slot satırı değişmiyorsa (aynı gün) fark edilmiyor; Yükle birincil renkte, Kaydet ikincil: yanlış vurgu. | P2 | Kaydedilen satırda 1 sn vurgu; Kaydet birincil, Yükle ikincil. | Küçük |
| B7 | `sekme_skills` | "Ayrıntı için yeteneğe uzun bas" yazıyor; tek dokunuş da açıyor. | P2 | Metni "dokun" yap. | Çok küçük |

Olumlu: dil değişimi anında ve her ekranda uygulanıyor (`akis_dil_en`, `akis_dil_en_hikaye`); seçenek dokunuşu, savaş düğmeleri ve sekmeler gecikmesiz.

## C. Okunabilirlik ve büyük yazı

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| C1 | `sahne_8_secenek__tr_small_dark_f2`, `__en_small_light_f2` | Ölçek 2.0'da etiketli seçenek metni harf harf kırılıyor ("Pas / dişle / rini / bir", "Tr / ad / e / yo"). | **P0** (bu ayarda seçenek okunmuyor) | A3'teki dikey dizilim (etiket üstte) bunu da çözer. | Küçük |
| C2 | `*_f2` tüm ekranlar | Alt sekme adları kesiliyor: "Hik…", "Dur…", "Yet…", "Ça…", "Da…". Normal yazıda da 5 sekmede "Yetenek…" ve "Daha fa…". | P1 | Sekme etiketine ölçek tavanı (en çok 1.3) + kısa adlar ("Yetenek", "Diğer"); seçili olmayan sekmede yalnız ikon. | Küçük |
| C3 | `savas_taktik__tr_small_dark_f2` | Savaş düğmeleri kesik ("Yetene", "Otomat"), Taktik çipi taşıyor, günlüğün ilk satırları görünmüyor. | P1 | Büyük yazıda 2 sütun düğme ızgarası; günlük alanına en az 4 satır. | Küçük–orta |
| C4 | `baslik__tr_small_dark_f2xl` | 2.0 + "Çok büyük": başlık "Yükse / liş / Proto / kolü" diye bölünüyor; Devam et ikinci ekranda. | P1 | Başlığa otomatik küçülme (tek kelime satırı bölünmesin); toplam ölçeğe tavan (2.0) ya da ayar ekranında uyarı. | Küçük |
| C5 | `us_erken__tr_small_dark_f2` | "Kalan eylem: 3 / 4" üç satıra kırılıyor, Tehlike ile çakışacak kadar sıkışık. | P2 | İki satıra ayır (eylem üstte, tehlike altta) büyük yazıda. | Küçük |
| C6 | `dahafazla_ayarlar` | Yazı boyutu seçeneğinde "Çok büy…" kesik. | P2 | 2×2 ızgara ya da "XL". | Çok küçük |
| C7 | `dahafazla_gunluk` | Günlük: başlık, görev adı ve açıklama aynı ağırlıkta tek blok; yeşil/turkuaz başlık ayrımı keyfi. | P2 | Görev başına kart; etkin/biten ayrımı; açıklama anlatı yazı tipiyle. | Orta |

Olumlu: Türkçe karakterler üç yazı tipinde de doğru (İ, ı, ğ, ş, â); anlatı satır uzunluğu 360dp'de 38–42 karakter, tablette 75–80 (rahat);
açık temada kontrast yeterli; tablette içerik ortalanmış sütunda kalıyor.

## D. Hiyerarşi ve ilk saat

İlk saatte (Gün 0–5) ekranda olan ama oyuncunun henüz bilmek zorunda olmadığı her şey:

| Öğe | Nerede | Kendini açan arayüze bağlanabilir mi |
| --- | --- | --- |
| MP çubuğu | Üst şerit, her ekran | Evet: ilk mana harcayan yetenekle |
| "Kademe F", "Sv. 1 · F" | Üst şerit, Durum | Evet: Kademe ilk Evrim Görevi duyurusuyla |
| "İnsanlığın ilk %28 dilimi" | Durum | Evet: sıralama açılınca (`showRank`) |
| Dayanım 105, XP sayıları | Durum | Kısmen: Dayanım ilk savaştan sonra |
| 8 statın tamamı + kısaltmalar | Durum | Hayır (çekirdek), ama ilk açılışta tek cümle açıklama |
| "Tehlike 15" | Üs | Evet: ilk kapı/taşma olayına kadar gizli |
| Gerilim noktaları (● / ●●) | Fırsat kartı | Evet ya da açıklama: Yardım dışında hiçbir yerde anlatılmıyor |
| "Görev · Öğretici Katman · Harabe Çarşı" alt satırı | Fırsat kartı | Hayır, ama "Görev" yerine "Yolculuk gerekir" daha açık |
| Günlük Görev karosu (0/3) | Daha fazla | Zaten Gün 1'de geliyor; deneme süresinde "deneme" etiketi |
| "Operasyon öncesi" kayıt satırı | Kayıtlar | Evet: ilk operasyon kaydına kadar gizli |
| Zar yüzdeleri (%70), [Gizle]/[Açığa vur] | Seçenekler | İpucu kartı var; yeterli |
| Analiz düğmesi | Savaş | Hayır; ilk savaşta 5 düğme uygun |
| Öz Kristali sayacı (0) | Çanta | Evet: ilk kristale kadar gizli |

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| D1 | `us_erken`, `erken_durum` | Yukarıdaki listeden bağlanabilecek 7 öğe ilk günden görünür. | P2 | `StatusView`'a `showMana`, `showGrade`, `showDanger`, `showCrystals` ekle. | Küçük–orta |
| D2 | `sekme_bag` | Çanta'daki eşyaların çoğu üretim malzemesi; açıklamalar kapatılan mesleklere atıf yapıyor ("Her simyacı işe bununla başlar", "Rün ustaları…"). Kullan düğmesi olmayan eşya neye yaradığını söylemiyor. | P1 | Açıklamaları güncelle; eşyada "takasta kullanılır / hikayede sorulur" satırı; kullanılabilirler üstte. | İçerik, küçük |
| D3 | `durum_ahlak` | Ahlak çubukları iki uçlu ölçeği tek yönlü dolgu gibi gösteriyor (+100, +85, +100 hepsi dolu turuncu). | P2 | Ortadan iki yöne dolan çubuk, uç adı kalın. | Küçük |
| D4 | `dahafazla_harita` | Harita bir metin listesi; "0 gün kaldı" süresi dolmuş kapıyı belirsiz bırakıyor. | P2 | "Taşmak üzere" etiketi; bölgeleri perdeye göre grupla, bulunduğun bölge üstte. | Küçük |
| D5 | `istem_evolution` | Evrim seçeneğinde iki stat satırı açıklamasız ("DAY +4, İRD +2" ve "DAY +3, GÜÇ +1 … / seviye"); üstte boşluk. | P2 | "Şimdi:" ve "Her seviye:" önekleri. | Çok küçük |

## E. Önemli anlar

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| E1 | `son`, `son_k1`–`_k4` | Son ekranı sıradan sahne kabuğunda: üstte "Bölüm 18 · 7 gün", HP/MP, altta sekmeler; epilog kartları uzun bir kaydırma. | P1 | Tam ekran son: sekmeler ve bölüm çubuğu gizli, son adı büyük başlık, epilog kartları sırayla (dokunarak ilerle), sonunda özet + Ana menü. | Orta |
| E2 | `brifing_1`, `brifing_4` | Bölüm brifingi sıradan sahne gibi başlıyor; bölüm adı yalnız üstteki ince çubukta. | P1 | Bölüm açılış kartı: "Bölüm 4 · Kıyıkent'e alış", hedef, son tarih; dokununca brifing. | Küçük–orta |
| E3 | `darbogaz_2` | Bölüm sonu (darboğaz) da sıradan sahne; bölümün bittiği anlaşılmıyor. | P1 | Bölüm kapanış kartı: yapılan seçimlerden 2–3 satır, geçen gün, sonraki bölümün adı. | Orta |
| E4 | `savas_boss` | Boss ile sıradan düşman arasındaki tek fark kırmızı çerçeve; faz bilgisi yalnız `Faz 3` küçük satırı. | P2 | Boss adı üstünde "BOSS" etiketi, faz çentikli can çubuğu, faz geçişinde tek satırlık an. | Küçük–orta |
| E5 | `yenilgi_sahnesi` | Yenilgi sahnesi sıradan; 1/165 can dışında işaret yok. | P2 | Sahne üstünde kırmızı ince "Yenildin" bandı. | Çok küçük |

Olumlu: `an_level`, `an_evolution`, `an_unique`, `an_rank`, `an_breach` tam ekran, renk ve tipografiyle ayrışıyor; Yankı satırları ihlalde çok iyi çalışıyor.

## F. Tutarlılık

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| F1 | `sekme_more`, `dahafazla_gunluk`, `dahafazla_gunluk_gorev` | "Günlük" (görev defteri) ve "Günlük Görev" yan yana iki karo; aynı sözcük iki ayrı şey. | P1 | Defter karosu "Görevler" olsun (CLAUDE.md de "Görevler" diyor). | Çok küçük |
| F2 | `savas_taktik`, `erken_durum` | DAY = Dayanıklılık (stat), DYN = Dayanım (savaş kaynağı), Durum'da "Dayanım: 105": üç yakın ad. | P1 | Savaş kaynağına başka ad ("Nefes" / EN "Stamina" kalır) ya da kısaltmayı kaldır. | Küçük (metin) |
| F3 | `savas_ilk` | Yoldaş satırı "Can 123/130", çubuklar "HP". | P2 | Tek terim (TR'de "Can"). | Çok küçük |
| F4 | `us_erken`, `dahafazla_iliskiler`, `donus_ozeti` | "Yoldaşlar" ve "Yanındakiler" aynı kişiler için. | P2 | "Yoldaşlar". | Çok küçük |
| F5 | `us_erken_k1`, `gorev_perde2` | Düğme biçimleri: hap, kesik köşe çerçeve, dolu kesik köşe; aynı ekranda üçü birden. | P2 | A5 ile birlikte. | Küçük |
| F6 | `baslik`, `dahafazla_kayitlar` | Etkin ama beklemede olan düğme (yüklenirken Devam et) ile devre dışı düğme (boş slot Yükle) aynı gri. | P2 | Yüklenirken düğmede ilerleme göstergesi. | Küçük |

Tutarlı olanlar: Sistem kartı çerçevesi, nadirlik renkleri (sınıf ve yetenek kartları), kesik köşe, Yankı'nın ters video satırı, pembe yoldaş satırı, TR/EN düzenleri.

## G. Erişilebilirlik

Erişilebilirlik ağacından ölçüldü (canlı TalkBack oturumu yapılmadı).

| # | Görüntü | Sorun | Önc. | Öneri | Büyüklük |
| --- | --- | --- | --- | --- | --- |
| G1 | `sahne_3_secenek`, `savas_infaz`, `sekme_more` | Etiketsiz dokunulabilir öğeler: (i) yardım düğmesi, "Önceki sahneler" oku, Daha fazla karolarının ikonları. | P1 | (i) düğmesine "Sembol açıklamaları" açıklaması; karolarda ikon + başlık tek düğüm (`semantics(mergeDescendants)`). | Küçük |
| G2 | `baslik`, `dahafazla_ayarlar` | Anahtarlar (Demir Mod, Daktilo, Titreşim, Hasar sayıları, Bildirimler) etiketlerinden ayrı düğüm: TalkBack "anahtar, kapalı" okur. | P1 | Satırı `toggleable` yap, etiketle birleştir. | Küçük |
| G3 | `us_erken_k1`, tüm sahneler | 48dp altı hedefler: Antrenman/Dinlen 38dp yüksek, bölüm çubuğu 33dp (dokunulabilir), (i) düğmesi görsel 18dp. | P2 | En az 48dp yükseklik; bölüm çubuğuna dokunma alanı dolgusu. | Küçük |
| G4 | (kod) | Hareket azaltma yalnız büyük an kartlarında uygulanıyor; daktilo efekti ve sahne geçişi sistem ayarını izlemiyor. | P2 | `Game.reduceMotion` açıkken daktilo kapalı başlasın. | Çok küçük |
| G5 | `savas_taktik` | Devre dışı "Konuş" her savaşta görünür; neden kapalı olduğu söylenmiyor. | P2 | Konuşulamayan düşmanda gizle. | Çok küçük |

## H. Performans

| Ölçüm | Değer |
| --- | --- |
| Etkinlik açılışı (`am start -W`) | 1,6 sn |
| "Dünya yükleniyor" (Devam et etkin olana kadar) | yaklaşık 8 sn (B5) |
| Son ekranı, 16 hızlı kaydırma | 592 kare, %3,9 takılma, p50 17 ms, p99 19 ms (akıcı) |

Hikaye listesi yalnız güncel sahneyi çizdiği için uzun liste sorunu yok. Gereksiz yeniden çizim ayrıca profillenmedi.

## I. Dil

- TR ve EN düzenleri aynı; EN'de sekme adları sığıyor, TR'de "Yetenekler" ve "Daha fazla" 5 sekmede kesiliyor (C2).
- Dil değişince Ayarlar, sekmeler, bölüm çubuğu ve sahne metni anında değişiyor.
- Uzun TR etiketler: "Entegrasyonu başlat" ve "Kaldığım yerden devam et" normal yazıda sığıyor; 2.0'da ikinci satıra geçiyor ama kesilmiyor.

## Öncelik özeti

| Öncelik | Bulgular |
| --- | --- |
| P0 | B1, B2, B3 (onaysız veri kaybı), C1 (ölçek 2.0'da seçenek okunmuyor) |
| P1 | A1–A4, B4, B5, C2–C4, D2, E1–E3, F1, F2, G1, G2 |
| P2 | A5, A6, B6, B7, C5–C7, D1, D3–D5, E4, E5, F3–F6, G3–G5 |

## Düzeltme sonrası (2026-11-10, DECISIONS #170–#172)

Seçilen kapsam: P0'lar (B1, B2, B3, C1) ve en çok fark yaratacak üç düzeltme (B1–B4, A1–A5, C1–C4). "Sonra" görüntüleri aynı adla,
etiketin sonunda `_sonra` ile: `docs/ui-review/<ekran>__<koşul>_sonra.png` (4 koşul: `tr_small_dark_n`, `tr_small_dark_f2`,
`en_small_light_f2`, `tr_tablet_dark_n`).

| Bulgu | Durum | Ne yapıldı | Önce | Sonra |
| --- | --- | --- | --- | --- |
| B1 | Düzeltildi | Kayıt varken "Entegrasyonu başlat" onay penceresi açar (silinecek kaydın özeti). | `akis_yeni_oyun__tr_small_dark_n` | `akis_yeni_oyun__tr_small_dark_n_sonra` |
| B2 | Düzeltildi | Dolu slota Kaydet onay ister; boş slot doğrudan kaydeder. | `akis_kaydet_ustune__tr_small_dark_n` | `akis_kaydet_ustune__tr_small_dark_n_sonra` |
| B3 | Düzeltildi (onay) | Yükle onay ister ("kaydedilmemiş ilerleme kaybolur"). "Önceki oturum" satırı eklenmedi. | `akis_yukle__tr_small_dark_n` | `akis_yukle__tr_small_dark_n_sonra` |
| B4 | Düzeltildi | Geri: alt sayfa → Hikaye sekmesi → "Çıkmak için bir kez daha geri bas" (2 sn içinde ikinci basış çıkar). | — | `akis_geri_ipucu__tr_small_dark_n_sonra` |
| A1 | Düzeltildi | Sabit seçenek paneli kalktı; seçenekler metnin altında aynı kaydırmada. 360dp'de anlatı payı %30 → %64 (kısa sahnede seçenekler aynı ekranda, uzun sahnede "seçenekler aşağıda ▾"). | `sahne_3_secenek__tr_small_dark_n`, `sahne_8_secenek__tr_small_dark_n` | aynı adlar `_sonra` |
| A2 | Düzeltildi | 5'ten fazla seçenek: ilk 4 (açık olanlar önce) + "Diğer seçenekler (n) ▾". | `sahne_8_secenek_k1__tr_small_dark_n` | `sahne_8_secenek_k1__tr_small_dark_n_sonra`, `sahne_8_secenek__tr_tablet_dark_n_sonra` |
| A3, C1 | Düzeltildi | Etiket üstte ayrı küçük satır, metin tam genişlik; tek satıra sığmayan metin bir boy küçülür, kesilmez. Ölçek 2.0'da harf harf kırılma yok. | `sahne_8_secenek__tr_small_dark_f2`, `__en_small_light_f2` | aynı adlar `_sonra` (`_k1`, `_k2` seçenekleri gösterir) |
| A4 | Düzeltildi | Üste fırsat kartları ve operasyon sahne metninin hemen altında; rutin eylemler alttaki tek satırlık çubukta (yana kayar). Boş üst yarı kalmadı. | `us_erken__tr_small_dark_n`, `us_operasyon_b2__tr_small_dark_n`, `us_perde2__tr_small_dark_n` | aynı adlar `_sonra` |
| A5 | Düzeltildi | Rutin eylemler tek biçim (kesik köşe çerçeve, 48dp); hap düğmeler ve sarı "Kapılar" kalktı. | `gorev_perde2__tr_small_dark_n` | `us_erken__tr_small_dark_n_sonra` |
| A6 | Yapılmadı | 21 sahnenin içerik düzeyinde kademelendirilmesi; A2 katlaması bu sahnelerde de çalışıyor. | | |
| C2 | Düzeltildi | Alt sekme etiketleri cihaz ölçeğinden bağımsız (tavan 1.0), küçük etiket stili: 5 sekmede "Yetenekler" ve "Daha fazla" tam. | `sekme_status__tr_small_dark_f2` | `sekme_status__tr_small_dark_f2_sonra` |
| C3 | Düzeltildi | Savaş düğmesi etiketleri ölçek tavanı 1.3: "Yetenek", "Otomatik" tam. Günlük alanı değişmedi. | `savas_taktik__tr_small_dark_f2` | `savas_taktik__tr_small_dark_f2_sonra` |
| C4 | Düzeltildi | Başlık logosu ölçek tavanı 1.3: sözcük bölünmüyor, Devam et ilk ekranda. | `baslik__tr_small_dark_f2`, `baslik__tr_small_dark_f2xl` | `baslik__tr_small_dark_f2_sonra` |

| B5 | Kısmen | İçerik dosyaları paralel ayrıştırılıyor: "Dünya yükleniyor" yaklaşık 8 sn → 4,4 sn (son ölçüm 4,3 / 4,2 / 4,3; emülatör, debug). Hedef 3 sn'nin altı sağlanmadı. | `baslik__tr_small_dark_n` | `baslik__tr_small_dark_n_sonra` |
| D2 | Düzeltildi | Mesleklere atıf yapan 4 açıklama yeniden yazıldı; kullanılabilir eşya üstte; diğerlerinde "Takasta ve hikaye seçimlerinde işe yarar". | `sekme_bag__tr_small_dark_n` | `sekme_bag__tr_small_dark_n_sonra` |
| E1 | Düzeltildi (kısmen) | Son ekranı tam ekran: üst şerit, bölüm çubuğu ve sekmeler gizli, son adı büyük başlık. Epilog kartları hâlâ tek kaydırma (sırayla açılan kartlar yapılmadı). | `son__tr_small_dark_n`, `son_k1`–`_k4` | aynı adlar `_sonra` |
| E2 | Düzeltildi | Brifingin ilk sahnesinde bölüm açılış kartı: numara, ad, hedef, son tarih. | `brifing_1__tr_small_dark_n`, `brifing_4__…` | aynı adlar `_sonra` |
| E3 | Düzeltildi (sade) | Darboğazın ilk sahnesinde kapanış kartı: bölüm adı ve gün. Seçim özeti ve sonraki bölümün adı yok (motor verisi gerekir). | `darbogaz_2__tr_small_dark_n` | `darbogaz_2__tr_small_dark_n_sonra` |
| F1 | Düzeltildi | Defter karosu TR'de "Görevler". | `sekme_more__tr_small_dark_n` | `sekme_more__tr_small_dark_n_sonra` |
| F2 | Düzeltildi | Savaş kaynağı TR'de "Nefes" (NFS); Dayanıklılık (DAY) ile karışmıyor. | `savas_taktik__tr_small_dark_n` | `savas_taktik__tr_small_dark_n_sonra` |
| G1 | Düzeltildi | Metni tamamlayan dokunuş etiketli ve yalnız metin yazılırken etkin; etiketsiz dokunulabilir öğe ölçümü hikaye, savaş, Ayarlar ve Daha fazla ekranlarında 0 (başlıkta ad alanı ve dil/tema düğmeleri sayımda kaldı). | — | `build/ui-dumps/p1_sonra.json` ölçümü |
| G2 | Düzeltildi | Ayarlar ve başlıktaki anahtar satırları tek düğüm ve tek dokunma hedefi (satırın tamamı). | `dahafazla_ayarlar__tr_small_dark_n` | `dahafazla_ayarlar__tr_small_dark_n_sonra` |

Doğrulama: `s_flow.py` (geri tuşu Hikaye ve Durum'da oyunda kalıyor; Kaydet/Yükle/Yeni oyun onay penceresi açıyor), `scripts/check.sh`.

Düzeltmeden sonra görülen ve açık kalanlar:
- Ölçek 2.0'da üste "Kalan eylem: 3 / 4" hâlâ üç satır (C5, kapsam dışı).
- Eylem çubuğunda 4'ten fazla eylem olduğunda son düğme kenarda yarım görünür; yana kaydırma ipucu olarak bırakıldı.
- Savaşta ölçek 2.0'da günlüğün ilk satırı üstte kesik başlıyor (en yeni satırlar görünür).
- Ölçek 2.0'da Durum alt sekmesi "Unvanlar" "Unvanl" diye kesiliyor (`sekme_status__tr_small_dark_f2_sonra`; yeni P2).
