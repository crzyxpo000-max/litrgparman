# Tasarım ve uygulama kararları

GDD'nin açık bıraktığı ya da bilinçli olarak saptığımız noktalar. En yeni en üstte.

## Aşama 7 — 2026-11-06

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 105 | **Ölçekli düşman başvurusu** `id@seviye`: statlar ve saldırı tabanları beklenen insan statı eğrisiyle (5 + Σ (4 · kademe çarpanı + 3)/8), can aynı eğrinin can formülüyle, XP `xpToNext(L) · max(0,1; 0,012·L) · tür` ile ölçeklenir (tür = tanımdaki XP'nin kendi seviyesine oranı/0,1, 0,5–3; boss ×3); kademe seviyeden | Perde 2–4'ün E→S düşmanları, Kule ve kapılar için yüzlerce stat bloğu yazmadan; seviye yükseldikçe zafer başına daha çok seviye (OP MC üstel büyüme). | Aşama 9 simülatörü. |
| 106 | **Dünya seviyesi** `20 · 1,0385^((gün−30)/3)`: Perde 2+ bölge düşmanları bu seviyede (tehlikeyle +%25'e kadar), kapılar ×1,15. Kule katı seviyesi aynı eğri: `20 · 1,0385^kat` (kat 50 ≈ 132 C, kat 100 ≈ 876 S) | Kule'yi beklenenden hızlı tırmanan oyuncu çevresini ezer (hakimiyet evresi); GDD kademe tablosuyla perdeler örtüşür (Gün 180 ≈ C, Gün 330 ≈ S). | Aşama 9. |
| 107 | **Kule**: `tower.yaml` (bantlar, kat seviyesi, hikaye katları); her 10. kat yazılmış `yalniz_tohum` olay, diğerleri bant düşmanlarından prosedürel savaş (her 5. kat tek, güçlü düşman). Zafer `kule_kat` +1, Öz Kristali; hikaye katı `kule_kat: n` etkisiyle ilerler | GDD "100 katlı dikey zindan, her 10 katta bir hikaye sırrı". | — |
| 108 | **Sonlar** `endings.yaml`: koşul, sahne(ler), yedek (Hasat) ve gizli (Döngü); koşul dili `son_uygun`, `hicbir_son`, `son`; son karar olayı yalnızca uygun sonları gösterir. Son ekranı: OP MC türü (ahlak koşullu arketip), NPC ve fraksiyon epilogları (ilk tutan satır), **kronik**: önemli olaylardaki seçimler ağırlıkla (ahlak + itibar/2 + ölüm 25 + fraksiyon/ayrılık 12 + tohum 6 + `kronik` metni 10; `kronik_agirlik` ile elle) saklanır, en ağır 5'i gösterilir | GDD son anlatımı (Fallout tarzı epilog kartları). | — |
| 109 | **New Game+**: cihazda bitirilmiş oyun sayısı ve Döngü sonu uygulama ayarlarında (meta) tutulur; yeni oyun `onceki_oyun` değişkenini ve NG+ için `yeni_dongu` bayrağını alır, her stat +3. Kayıt v4 (kronik) | GDD: Döngü "en az bir önceki bitirilmiş oyun" ister ve New Game+'ı açar. | Aşama 9. |
| 110 | Yeni koşullar `kademe_min`, `ordu_min` (birlik sayısı), `yoldas_sayisi_max`, `oz`, `yol`; yeni etkiler `xp_seviye` (seviye XP'sinin yüzdesi), `kule_kat`, `yol`. Bölgelerde `son_perde` (Öğretici Katman Perde 1'de kapanır; kapı doğuşu/taşması yalnızca canlı bölgede), `kosul` (yolculuk), `ortam`. Düşman, eşya, uyanış ve unvan dosyaları parçalanabilir (`enemies/`, `items/`, `awakenings/`, `titles/`) | Paralel içerik partileri birbirinin dosyasına yazmasın; Perde 2+ ödülleri her seviyede anlamlı kalsın. | — |

## Aşama 6 — 2026-11-06

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 98 | **Telif:** antrenman türü Günlük Görevler spor salonu sayı listesi değil, Öğretici dünyasına özgü görevler (su taşı, sınırı dolaş, buz gibi tünel); "Ceza Bölgesi" oyuncuya görünen her yerde "Durmuş Saat / The Stopped Hour" (iç kimlik `ceza_bolgesi` aynı); unvan "Saati Yeniden Kuran" | Bağımsız içerik incelemesi: "100 şınav, 100 mekik, 100 squat, 10 km" ve "Ceza Bölgesi" Solo Leveling'in imza mekaniğine fazla yakındı. | — |
| 99 | Perde 1 yapısı `docs/PERDE1.md`: Mücadele (1–11) → Kırılma (ana yay 12) → Hakimiyet (12–24) → Yeni tavan (ana yay 24) → Bekçi (28). Dünya durumu: `hayatta_kalan` (açılışta 50), Deniz'in akıbeti (yoldaş / Yükselenler / öldü / gitti / hiç bulunamadı), ilk fraksiyon eğilimi (`lider_fraksiyon` + itibar ≥ 15, yoksa yalnız kurt) | Görev dosyası: Perde 1 sonunda en az 3 belirgin dünya durumu ve OP MC güç döngüsü. | Aşama 7: Perde 2 açılışı bu durumları okur. |
| 100 | 48 Perde 1 olayı 4 paralel içerik partisiyle yazıldı (yoldaş yayları, orman/genel, çarşı/istasyon/kamplar, ana yay/zindanlar); her parti kendi `flags/`, `quests/`, `knowledge/` parçasına yazar (kayıt dosyaları parçalanabilir, aynı kimlik iki kez hata). Aşama 4'ün kısa örnek olayları tam kaliteyle yeniden yazıldı; eski metin `docs/eski/` altında | Kullanıcı kuralı: hikaye kusursuz, kısaltma yok; paralel yazımda dosya çakışması olmasın. | — |
| 101 | Yeni içerik kuralları: Yankı'nın sözleri küçük harf ve tırnaksız; NPC adları her yerde belirteçle (doğrulayıcı düz yazılmış adı uyarır, test hata sayar); sezgide `anlati: true` = düz anlatı satırı (Yankı rengi değil); `bayrak`/`bayrak_yok` listesi; `npc_gitti`; Türkçede ", ve" yok; başarısız zar kısa bir sahneye gider | Bağımsız içerik incelemesi bulguları (12 yüksek, 10 orta). | — |
| 102 | Yeni açılış: iskele → aktarımda Yankı bağı (kabul et / sor / [İRD 6] direnmeye çalış; bağ her yolda kurulur, Yankı ilişkisi ve hatırası seçime göre) → uyanış. İlk savaş kaybedilirse yazılmış yenilgi sahnesi; "ilk öldürme" bayrağı zaferde konur | Görev dosyası: "yeni açılış Yankı bağını anlatır"; inceleme H4. | — |
| 103 | Perde 1 testleri: kurallar (her olayda ≥ 3 giriş seçeneği, koşullu, tohum, ilişki), 500 başsız oynanış (5 oyun tarzı: rastgele, açığa vuran, gizleyen, merhametli, acımasız; serbest puan rastgele dağıtılır) çıkmazsız biter, tüm Perde 1 olaylarına ulaşılır, dünya durumu boyutlarının her biri ≥ 2–3 farklı sonuç verir | Görev dosyası kabul kriterleri. | Aşama 9 simülatörü. |
| 104 | Uygulama sürümü 0.6.0 (başlık ekranı "v0.6") | Eski "Phase 1" etiketi yanıltıcıydı. | Her aşama sonunda. |

## Tasarım değişikliği — 2026-11-06

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 97 | **Yapay zeka anlatıcı oyundan tamamen çıkarıldı (kullanıcı onaylı).** Narrator/ClaudeNarrator, vekil sunucu, anlatıcı modu düğmesi ve serbest metin eylemi yok; Aşama 8 kaldırıldı (numaralar değişmedi). Hikaye Yönetmeni, olay havuzu ve tohumlar aynen kalır; tüm metinler önceden yazılmış içerikten gelir. Uygulama internet izni istemez | API maliyeti ve internet bağımlılığı; oyun tamamen çevrimdışı çalışacak. | — |

## Aşama 5 — 2026-11-06

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 76 | NPC'ler `content/npcs/<id>.yaml` (dosya başına bir NPC); durum `WorldState.npcs` içinde, kayıt v3. Eski `iliski_<npc>` değişkenleri Güven eksenine (yarısı Bağlılığa) taşınır | GDD örnek NPC dosyası; tek sayılı ilişki dört eksene genişledi. | — |
| 77 | NPC büyümesi: günde 40 · hız / (1 + L/10) yüzde-seviye (hız 1: Gün 30 ≈ 8, 180 ≈ 29, 365 ≈ 45). Yoldaşken ve ihmal edilince (10 gün temassız) ×1,25 | "NPC'ler oyuncudan yavaş bir eğride büyür"; ihmal edilen NPC "başka yoldan" güçlenir. | Aşama 9 simülatörü. |
| 78 | Hafıza: her gün küçük hatıralar 1 solar; |ağırlık| ≥ 40 ve yalanlar kalıcı; en fazla 30 | GDD "küçükler solar, büyükler kalıcı". | — |
| 79 | Eşikler günlük adımda denetlenir, NPC başına bir kez (`esik_<tur>` işareti) ve bir olay tohumlar: NPC'ye özel `esik_<tur>_<npc>` yoksa genel `esik_<tur>` (odak NPC "@", `{npc:@}`, `{npc_hedef:@}`). Ayrılık eşiği yoldaş değilse olay yerine en yakın değerli fraksiyona geçiştir | Genel olaylar sistemi şimdiden çalıştırır; NPC'ye özel yazım Aşama 6–7. | Aşama 6–7 içerik. |
| 80 | Tanık yorumu: Güven Δ = Σ(Δeksen · değer)/50 · (0,5 + vicdan/10); acımasızlıkta Saygı Δ = −Δmerhamet · (hırs − 5)/5, Korku Δ = −Δmerhamet · (10 − cesaret)/20; eksen başına ±15; |Güven Δ| ≥ 6 olay hatırası | GDD: "vicdanlı güven kaybeder, hırslı saygı duyar". | Aşama 9. |
| 81 | Yoldaşlar savaşta oyuncudan hemen sonra rolüne göre davranır (saldırı/menzil vurur, koruma kalkan, şifa iyileştirir); düşmanlar %35 yoldaşa saldırır; savunan oyuncu düşük canlı yoldaşın darbesini üstüne çeker (`korunan_muttefik`, `_savas_korunan`). Yoldaş savaşta ölmez, düşer; can savaştan savaşa taşınmaz | Kalıcı ölüm rastgele zarla değil hikaye seçimiyle gelsin; mobilde okunur tur ritmi. | Aşama 9. |
| 82 | Düello/ihanet düşmanı `npc:<id>` (ya da `npc:@`): NPC'nin seviyesi ve savaş bloğundan türetilir; Ezme uygulanmaz; acımasız infaz NPC'yi öldürür, temiz infaz ezer, bağışlamak bağ kurar | "Ölüm kalıcıdır"; ahlaki karar oyuncunun. | — |
| 83 | Küresel Sıralama dünyaya görünen seviyeyle hesaplanır; oyuncu gerçek yerini de görür ("Yankı gerçek verini saklıyor"). Eşitlikte oyuncu geride. `sira`, `gercek_sira` değişkenleri; ilk 10/3/1'e tırmanınca fraksiyon tepkisi (ranking.yaml) | Gizli/Açık Güç ile Sıralama'nın tutarlı olması için kanon açıklaması. | Aşama 7. |
| 84 | Yoldaş kapasitesi 1 + KAR/10 + kademe (1–8) | GDD: "KAR ve kademeye bağlı". | Aşama 9. |
| 85 | Yoldaş görüşü yalnızca önemli olaylarda ve iki+ seçenekte; NPC değerlerine en uygun seçeneğin baskın ekseni için `gorusler` satırı; sahneye yazılmış `yoldas_tepkileri` önceliklidir. Güce tepki 7 günde bir (seviye farkı ≥ 5), açığa vurmada hemen | Her sahnede konuşan yoldaş gürültü olur. | Aşama 6 içerik. |
| 86 | Yalanlar: `yalan {npc, konu}`; her gün şüphecilik · %1,5 olasılıkla ortaya çıkar (Güven −20, Saygı −5, `esik_yalan`) | "Yalanlar hafızaya girer, sonra ortaya çıkabilir". | Aşama 9. |
| 87 | Yas Tutan: yoldaş öldükten sonra 3 gün savaş yoksa motor `yas_tutuldu` bayrağını koyar | Aşama 2'den kalan gizli sınıf koşulu. | — |
| 88 | Prologda el tutulursa Deniz baştan yoldaş; Öğretici'de bulunursa (her yolda) katılır. Deniz'in ilk savaşlara katılması prolog RNG akışını değiştirdi; tam akış testi Günlük Görev'i tamamlanmış sayar | Yoldaş sistemini ilk dakikadan göstermek. | Aşama 6 yeni açılış. |
| 89 | Ordu modeli: birlik → adet, güç = adet · güç · (1 + kademe) · (1 + seviye/50); `ordu_kazan` eğilime uygun birliği verir. İçerik Aşama 7 | OP MC Ordu; şimdilik model. | Aşama 7. |
| 90 | 7 NPC dosyası paralel içerik ajanıyla yazıldı (Deniz örnek şemayla elle); kişisel görev ödülü olarak her NPC'ye özgün bir unvan | Görev dosyası: içerikte paralel alt ajan. | Aşama 7 tam yaylar. |
| 91 | Ayrılık eşiği (bağımsız inceleme): GDD'deki "fark > 120" nötr oyuncuyu bile aşıyordu; artık **sapma** = uzaklık(değer, oyuncu) − uzaklık(değer, nötr) > 60. Yoldaş değilse yalnızca fraksiyonu olan NPC ve yalnızca farklı bir fraksiyona geçer | Deniz ilk gün "seninle yürüyemem" demesin; işaret boşa harcanmasın. | Aşama 9. |
| 92 | Kişisel görev, kaçış, itaat ve ayrılık eşikleri yalnızca yoldaşlarda; ihanet ve rakiplik savaş bloğu olan her tanıdıkta. Bedensiz NPC (Yankı) genel eşik almaz, yalnızca `esik_<tur>_yanki` yazılırsa; tanık yorumundan da muaf (ilişkisi yazılmış içerikle değişir) | Genel sahneler bedeni olan, yanında yürüyen birini anlatıyor. | Aşama 7 Yankı yayı. |
| 93 | Hedefler: süreli (`gun`) hedefler ilerler ve etkilerini uygular; süresiz hedefler kalıcı arka plan amacıdır (`{npc_hedef:@}`) | İnceleme: süresiz hedefler gündemi tıkıyordu. | — |
| 94 | Düello bire birdir (yoldaş katılmaz), Ezme yok; NPC adı artikelsiz kısa ad; NPC infazı için ayrı metinler (Öldür / Dövüşü bitir / Elini uzat). Akılsız düşmanın infazı tanıkları etkilemez | "The Ash Wolfe", "Spare it" ve her fare infazında Deniz'in güven kaybı. | — |
| 95 | İngilizce zamir belirteci `{zamir:id|o|onu|onun|kendi|b}` ve fiil uyumu (`|s`, `|is`, `|has`, `|does`, `|was`); zamir `names.yaml`'da (Deniz they, Yankı it). Geçmiş pasajlar kendi odak NPC'siyle çözülür (`HistoryEntry.focus`) | Genel sahnelerde cinsiyetli NPC'lere "they" deniyordu; "@" sonraki olaya sızıyordu. | — |
| 96 | Telif yakınlığı: "Gölge Asker" → "Kül Lejyoneri", "Avcı Soyu" → "Liman Rüzgârı"; `npc_gitti` etkisi (durum "gitti"); ölen NPC'nin kişisel görevi başarısız; v2 kaydında Deniz bayraklara göre yoldaş, avcılar kurulur | Bağımsız inceleme bulguları. | — |

## Aşama 4 — 2026-11-06

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 61 | **İçerik YAML (kaml 0.104) — DECISIONS #1 kapandı.** kaml'ın düğüm API'si okunur, genel ağaca (JsonElement) çevrilir; ayrıştırıcı mantığı aynı. JSON içerik okuyucusu kaldırıldı; kayıt dosyaları JSON kalır. Dönüşüm kayıpsız doğrulandı. | GDD: kaml; derleyici eklentisi gerekmez. | — |
| 62 | Olaylar `content/events/*.yaml`; her olay kendi sahnelerini taşır; `"@akis"` gün akışına döner | Olay = havuz kaydı + küçük sahne grafiği. | — |
| 63 | Yönetmen: gerilim 1–4; hedef = son gerilim + 1 + Dikkat/40 (iki ağır olaydan sonra 1); uyum 1/(1+|fark|); tazelik 0,6^çakışma (son 12 etiket); bağ 1 + ilişki/100 (`iliski_<npc>` değişkeni, Aşama 5'te NPC modeli); ana yay ×3, vadesi gelince zorunlu | GDD formülünü sayılaştırma. | Aşama 9 simülatörü. |
| 64 | Fraksiyon itibarı dalgası: A'ya d → B'ye d · ilişki(A,B)/100 · 0,3 | GDD "ilişki matrisi". | Aşama 9. |
| 65 | Gün: 2–4 yuva; serbest eylemler keşif, antrenman, kapı, dinlenme, yolculuk; olay kartı (fırsat eli, 3 kart) da bir yuva; akşam kamp (HP +%50, MP dolu) günü bitirir | GDD gün döngüsü + OP MC fırsat eli. | Aşama 6 içerik akışı. |
| 66 | Kapı: her sabah bölge başına `kapi_sansi · (1 + tehlike/100)`, 3–6 gün süre, en fazla 3 açık kapı; taşma tehlike +15 ve `kapi_tasma_sayisi`; kapatmak tehlike −10 ve Öz Kristali | GDD kapı taşması. | Aşama 9. |
| 67 | Tohum koşulu ham metin (JSON) olarak kayda girer, vadesinde ayrıştırılır; vadesi gelip koşulu tutmayan tohum kurur | Koşul DSL'i kod değil veri; kayıt taşınabilir. | — |
| 68 | Anomali Dikkati 0–100: beklenen seviyenin (1 + 0,4·gün) üstündeki her yeni seviye +1, açığa vurma 5 + 2·fark, tanıklı başarı +2, dinlenme −1, `gizle` −n; 25/50/75/100 eşiklerinde `dikkat_<n>` olayı tohumlanır. Yozlaşma: ceza yeteneği +10, `yozlasma` etkisi | OP MC ana gerilim ölçeği. | Aşama 9 (Dikkat zaman grafiği). |
| 69 | Görünen seviye ilk akış girişinde gerçek seviyeye eşitlenir, sonra yalnızca [Açığa vur] ile güncellenir | "Beni hafife aldılar" anları için fark birikmeli. | Aşama 5 (NPC tepkileri). |
| 70 | Zar: d20 + stat ≥ zorluk; olasılık %5'lik adımlarla gösterilir; tek seferlik başarısızlık `_zar_<sahne>_<seçenek>` bayrağıyla kilitlenir | OP MC beyaz/kırmızı kontroller. | — |
| 71 | Prolog sonu `demo_son` → `ilk_sabah` (gün akışına geçiş); Perde 1 bu sürümde `ogretici_bekci` ana yay olayıyla (Gün 28) biter | Yazılmış giriş dizisi + olay havuzu. | Aşama 6 yeni açılış. |
| 72 | Olay içeriği Aşama 4'te 14 örnekle sınırlı; içerik ajanı kullanım limitine takıldığı için elle ve kısa yazıldı | Motoru sınamaya yeter; hacim Aşama 6'da. | Aşama 6. |
| 73 | Zorunlu olay (ana yay, tohum, eşik) yuva harcamaz; eylemin yuvası zaten harcanmıştır | Bağımsız inceleme: çift yuva harcaması günü seçimsiz bitiriyordu. | — |
| 74 | Görülmüş, tekrarlanmayan olayın tohumu kurur | Sonsuza kadar kuyrukta kalmasın. | — |
| 75 | Bölge kontrolü `kontrol {bolge: fraksiyon}` etkisiyle değişir; NPC'lerin görünen seviyeye tepkisi, Yozlaşma eşik olayları ve Dikkat 75/100 içeriği Aşama 5–7'de | Bu aşamada model ve kancalar; içerik ve NPC tepkileri sonra. | Aşama 5 (NPC), 6–7 (içerik). |

## Aşama 3 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 36 | Duruşlar: Tam saldırı (verilen ×1,3, alınan ×1,25), Ölçülü (×1, +5 Dayanım/tur), Savunmacı (×0,7, alınan ×0,6, +12 Dayanım/tur) | OP MC "duruş" isteği; GDD sayı vermiyor. | Aşama 9 simülatörü. |
| 37 | Efor 0–3: kademe başına +%12 hasar, +5 puan isabet, 8 Dayanım | "Dayanım taahhüdü". | Aşama 9. |
| 38 | Ezme: düşman daha düşük kademede ya da seviyesi oyuncunun yarısından az ve en az 5 aşağıda; boss asla ezilmez | "Fodder auto-resolves"; tutorial dövüşleri gerçek kalsın. | Aşama 6 içerik akışında. |
| 39 | Her gerçek savaşın son düşmanı infaz seçimiyle biter: Acımasız (Merhamet −3, İnsanlık −2), Temiz (Düzen +2), Bağışla (Merhamet +5; zeki düşman hatırlar, %50 XP) | OP MC: "gerçek seçimler ahlakidir". | — |
| 40 | Ölüm: önce İkinci Şans (kademe başına bir; %30 HP; bedel rastgele Benzersiz olmayan yetenek −2 seviye, yoksa bir unvan). Sonra yazılmış `kaybet` sahnesi varsa oraya (HP 1); yoksa Demir Mod → `son_hasat`, Normal → savaş başındaki kayıt noktası (`checkpoint` slotu) | GDD: "ölüm son kayda döndürür"; otomatik kayıt savaş ortasını tuttuğu için ayrı kayıt noktası. | — |
| 41 | Direnç: fiziksel 2·DAY, büyü 2·BİL; yetenek statı ZEK/BİL/İRD ise büyü sayılır | GDD "Direnç"i tanımlamıyor. | Aşama 9. |
| 42 | Zaaf ×1,5; Analiz zorluğu d20 + ALG ≥ 8 + seviye/2; OP MC gereği gerçek statlar her zaman görünür, Analiz zaafı açar | GDD: "Analiz zaafı açar". | — |
| 43 | Konuş: ikna d20 + KAR ≥ 12 + seviye/2 (düşman %30 altındaysa +4); tehdit, üst kademe ya da +5 seviye farkta kendiliğinden başarılı. Kaçan zeki düşman her kaçışta +2 seviye, statlar +%15, HP +%20 ile döner | GDD "zekalı düşmanlar oyuncuyu hatırlar". | Aşama 5 NPC hafızasıyla birleşebilir. |
| 44 | Ortam etiketleri: tavan, su, yanici, karanlik, dar_gecit; her biri savaşta bir kez | GDD örnekleri ("tavanı çökert, suya elektrik ver"). | Aşama 6'da yeni etiketler. |
| 45 | Kademe Kırma: XP ×3, unvan zinciri 1/3/10 zaferde (Kademe Kıran I–III), %25 ek Benzersiz yetenek şansı | OP MC. | Aşama 9. |
| 46 | Uyanışlar `content/awakenings.json`'da; koşullar savaş bağlamı değişkenlerini (`_savas_kazanildi`, `_savas_en_dusuk_can`, `_savas_kademe_farki`, `_savas_boss`, `_savas_ezme`) ve yeni `degisken_max` koşulunu kullanır | Yeniden kullanılabilir "uyanış tetikleyicisi". | Aşama 5: "birini büyük bedelle korumak" yoldaşlarla. |
| 47 | Yetenek gücü seviye başı +%5; her kullanım +1 yetenek XP'si | GDD "kullanıldıkça XP alır". | Aşama 9. |
| 48 | Düşman HP'si içerikte açıkça yazılır (formülle türetilmez) | Denge içerikte kolay ayarlanır. | — |
| 49 | Kanal Faresi XP'si 80: ilk öldürme oyuncuyu Seviye 2'ye taşır | Prologdaki seviye atlama anı korunur. | Aşama 6. |
| 50 | Boss savaşından kaçılamaz | Kapı kilitli; boss anının ağırlığı. | — |
| 51 | Prologdaki ilk fare artık gerçek savaş (`savas` etkisi → `ilk_kan`); unvan zaferden sonraki sahnede verilir | Savaş sistemini ilk dakikada tanıtır. | Aşama 6 yeni açılış. |
| 52 | Yazılmış `kaybet` sahnesi ölüm değildir: her iki modda İkinci Şans'tan önce gelir ve onu harcamaz; Demir Mod'da da kalıcı ölüm sayılmaz | Senaryolu yenilgiler (ör. ilk fare) oyuncunun tek İkinci Şans'ını yakmasın; Demir Mod ölümü haddini aşmaktan gelmeli. | — |
| 53 | İnisiyatif savaş başında bir kez atılır (oyuncu ↔ en hızlı düşman); sonra sıra oyuncu → düşmanlar | Mobilde okunabilir tur ritmi; GDD "sıra en yüksekten başlar". | Aşama 5: yoldaşlar gelince kişi başı sıra. |
| 54 | Durum hasarları (kanama, zehir, yanma) kademe çarpanı almaz; fırlatılan eşyalar alır | Yüzdelik/sabit tur hasarı kademe farkında anlamını yitirmesin. | Aşama 9. |
| 55 | Oyuncuya uygulanan durumlar uygulandıkları tur sonunda süre kaybetmez (`fresh`) | 1 turluk sersemleme gerçekten bir turu alsın (bağımsız inceleme). | — |
| 56 | Kaçan düşman bağışlanmış sayılmaz: XP yok; herkes kaçarsa `_savas_kazanildi = 0` | Uyanış koşulları kaçan düşmanla tetiklenmesin. | — |
| 57 | Kayıt noktası yalnızca aynı oyunun (tohum + ad) noktasıysa kullanılır; yeni oyunda silinir | Başka oyunun savaşına dönülmesin. | — |
| 58 | Analiz edilen "göz" zaafı her vuruşta hedeflenebilir; `goz`/`buz` etiketleri birkaç yeteneğe eklendi; `fiziksel` zaaf kaldırıldı | Erişilemeyen ya da her vuruşu ×1,5 yapan zaaflar. | — |
| 59 | İngilizce savaş metinlerinde düşman adı "the" ile ve cümle başında büyük harfle | "Sewer Rat staggers." doğal değil. | — |
| 60 | `korunan_muttefik` sayacı şimdilik artmaz; yoldaşlarla (Aşama 5) bir müttefiki korurken sayılacak | Tek başına Savun ile çiftlenmesin. | Aşama 5. |

## Aşama 2 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 19 | **Tasarım değişikliği (kullanıcı onaylı): OP MC.** GDD'ye "OP MC tasarımı" bölümü eklendi; diğer bölümlerin üstünde sayılır. Yankı bağı, yalnızca oyuncuda Günlük Görev, gizli bilgi, garantili Benzersiz yetenek + gizli sınıf, kademe başına 4 evreli güç döngüsü, Küresel Sıralama, Gizli/Açık Güç, Anomali Dikkati, Yozlaşma, Tanıklar, Kademe Kırma, Ordu, iki savaş modu vb. Kabul kriterleri görev dosyasında ilgili aşamalara dağıtıldı; bitmiş aşamaların işi (prolog) Aşama 2'ye retrofit olarak girdi. Çelişen GDD satırları (Günlük Görev, Ölüm, Gerilim uyumu, Yankı) güncellendi. | Kullanıcının tasarım güncellemesi. | — |
| 20 | Yetenek seviyesi için kullanım XP'si `4 · L` (1→10 toplam 180) | GDD sayı vermiyor. | Aşama 9 simülatörü. |
| 21 | Meslek teklifi seviye 5'te otomatik (ya da `meslek_teklifi` etkisiyle); ikinci dal (E) Aşama 7'ye kaldı | GDD zamanlamayı vermiyor. | Aşama 6 içerik akışında. |
| 22 | Günlük Görev antrenmanı şimdilik Daha fazla → Günlük Görev'den set set yapılır | Gün döngüsü (serbest eylemler) Aşama 4'te. | Aşama 4. |
| 23 | İhmal edilen Günlük Görevde Ceza Bölgesi olasılığı %50; kanca `ceza_bolgesi` sahnesi, dönüş `@geri` | GDD "risk" diyor. | Aşama 9 denge. |
| 24 | Kademe eşiğinde XP en fazla bir seviyelik tamponda birikir; fazlası yanar | Evrimden sonra onlarca seviye atlamayı önler; evrim anı bir seviye atlamayla ödüllenir. | Aşama 9. |
| 25 | Sınıf seçildiğinde geçmiş seviyeler için geriye dönük stat artışı yok | Basitlik; seviye 10'a kadar serbest puanlar var. | — |
| 26 | Evrimi tanımlı olmayan kademede "genel evrim": sınıf aynı, tüm statlar +1 | Oyun eşikte kilitlenmesin; D+ evrimleri Aşama 7. | Aşama 7. |
| 27 | Pasif yeteneklerde stat/taban/kat şimdilik yok sayılır; fırlatılan eşyaların (`hasar`, `durum`) etkisi yalnızca savaşta | Savaş Aşama 3. | Aşama 3. |
| 28 | Nadir sınıf "Çok Yönlü": tüm statlar ≥ 8 | İçerik varsayımı; dengeli oyuncuya ödül. | Aşama 9. |
| 29 | Kaynaştırma koşul değişkenleri `hareketli_atis`, `korunan_muttefik`, `arkadan_vurus` savaşta sayılacak | Savaş Aşama 3. | Aşama 3. |
| 30 | Yetenek evrimi Yetenekler ekranından değil, tam ekran Sistem istemiyle seçilir; "Şimdilik bekle" ile ertelenebilir, bir sonraki kademe evriminde yeniden sorulur | Tüm kalıcı seçimler aynı tam ekran an üzerinden gelir; erteleme, kaynaştırma için yeteneği maksimumda tutmayı sağlar. | — |
| 31 | Keşif / savaş / kapı türü Günlük Görev şablonları `gunluk_genis_gorevler` bayrağı gelene kadar çıkmaz | Bu görevleri ilerletecek gün döngüsü ve savaş Aşama 3–4'te; tamamlanamayan görev oyuncuyu haksız cezalandırırdı. | Aşama 4: bayrak gün döngüsüyle açılır. |
| 32 | Birden çok gün atlayan etki (`gun: 3`) Günlük Görev serisini koparır | Aradaki günlerde görev yapılmadı. | — |
| 33 | Simya malzemeleri henüz içerikte verilmiyor | Toplama/ganimet Aşama 3 (düşman ganimeti) ve Aşama 6 (Perde 1 olayları) işi. | Aşama 3/6. |
| 34 | Ceza Bölgesi ve "Temel Kondisyon" metinleri özgünleştirildi (bağımsız inceleme: ilham kaynağına fazla yakındı) | Telif kuralı: yalnızca tür kalıpları. | — |
| 35 | Kayıt yüklenince `StoryEngine.sanitize`: tanımsız içeriğe işaret eden istemler atılır, eksik Günlük Görev üretilir; arayüz beklenmeyen hataları yakalayıp kaydeder | Eski/bozuk kayıt oyunu çökertmesin. | — |

## Aşama 0 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 13 | Gradle 8.14.3 → **9.1.0**, AGP 8.7.3 → **8.13.2**, Kotlin 2.0.21 → **2.2.21** | Geliştirme makinesindeki tek JDK, Android Studio 2026.2'nin JBR'si (Java 25). Gradle 8.14 ve Kotlin 2.0 Java 25'te çalışmıyor. Java 25'i destekleyen en küçük Gradle 9.1; AGP 8.x çizgisinde kalındı (AGP 9 yerleşik Kotlin ile kırıcı değişiklik getiriyor). | Aşama 9: targetSdk yükseltilirken AGP 9'a geçiş değerlendirilir. |
| 14 | `scripts/check.sh` JAVA_HOME yoksa Android Studio JBR'sini kendisi bulur | Komut satırında Java kurulu olmayan makinelerde de çalışsın. | — |
| 15 | AAB'de dil bölmesi kapalı (`bundle.language.enableSplit = false`) | Dil uygulama içinden anında değişiyor; Play bölerse diğer dilin metinleri cihazda olmaz (lint: AppBundleLocaleChanges). | — |
| 16 | Prolog emülatörde (API 36, Medium Phone) iki dilde elle oynandı; Compose UI testi yazılmadı | Emülatör çalışıyor; görev dosyası UI testini yalnızca emülatör yoksa istiyor. | Aşama 4: başsız oynanış testi motor düzeyinde akışı kapsar. |
| 18 | Tema: Sistem · Açık · Koyu · **AMOLED** (kullanıcı isteği). AMOLED saf siyah arka plan/yüzey; alt çubuk ve kartlar çok koyu gri | AMOLED ekranda siyah piksel kapalı: pil tasarrufu, yüksek kontrast. Kartların seçilebilmesi için yüzey kapları #07–#15 aralığında. | Aşama 9: yüksek kontrast seçeneğiyle birleştirilebilir. |
| 17 | Proje OneDrive klasöründe; lint bazen "dosya başka işlem tarafından kullanılıyor" hatası veriyor | OneDrive build/ klasörünü kilitleyebiliyor. `./gradlew --stop` ve yeniden çalıştırma çözüyor. | Sorun sıklaşırsa projeyi OneDrive dışına taşımak önerilir. |

## Aşama 1 — 2026-11-05

| # | Karar | Neden | Ne zaman yeniden bakılır |
| --- | --- | --- | --- |
| 1 | Aşama 1 içeriği **JSON** (GDD: YAML) | Geliştirme ortamında kaml indirilemedi; JSON, kotlinx.serialization'ın `JsonElement` API'siyle eklentisiz okunuyor. Anahtarlar GDD'deki YAML örnekleriyle aynı. | Aşama 4: YAML'a (kaml) geçiş ya da JSON'da kalma kararı. Ayrıştırıcı tek dosyada (`ContentParser`), geçiş küçük. |
| 2 | Kayıt kodlayıcı **elle yazıldı** (`SaveCodec`), serialization derleyici eklentisi yok | Sürümlü, alan adları sabit, eski kayıtlar için geçiş kodu yazmak kolay. | Durum çok büyürse eklentiye geçilebilir; kayıt biçimi aynı kalmalı. |
| 3 | Testler **JUnit 4** (GDD: JUnit 5) | Android'de standart; motor bu ortamda çevrimdışı doğrulanabildi. | Gerek görülmüyor. |
| 4 | AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35 | Birbiriyle uyumu bilinen sürümler. | Aşama 9: Google Play'in o an istediği hedef API'ye yükselt (Android Studio yükseltme yardımcısı). |
| 5 | Otomatik kayıt **her seçimden sonra** + uygulama arka plana alınınca (GDD: gün sonunda + arka planda) | Telefonda uygulama her an kapanabilir; kayıt küçük ve ucuz. | — |
| 6 | Uygulama içi dil, yapılandırma bağlamı (`createConfigurationContext`) ile | Anında değişir, AppCompat gerektirmez, tüm Android sürümlerinde aynı çalışır. | Android 13+ sistem dil ayarıyla da eşitlemek istenirse `LocaleManager` eklenebilir. |
| 7 | Motorun ürettiği Sistem metinleri (XP, seviye atlama, unvan bildirimi) Kotlin'de `LText` olarak | Hikaye metni değil, arayüz düzeyinde kalıp metinler; iki dilli tutuluyor. | Aşama 2: `content/system.json`'a taşımayı değerlendir. |
| 8 | Prolog (5 sahne) **demo içeriği**dir | Motoru uçtan uca denemek için: koşullu paragraflar, kilitli seçenek, seviye atlama, unvan, tohum ipucu. | Aşama 6'da Öğretici Katman içeriğiyle değiştirilir/genişletilir. |
| 9 | Eğilimler şimdilik düz değişken (`egilim_algi`, `egilim_yakin`…) | Gizli Eğilim vektörü Aşama 2'nin işi; veriler şimdiden toplanıyor. | Aşama 2: `Affinity` modeline taşı, değişken adlarını eşle. |
| 10 | Deniz/Devon için cinsiyet belirtilmedi (TR'de nötr; EN metinde zamir kullanılmıyor) | Karakterin cinsiyeti GDD'de tanımlı değil. | Karakter yazımı netleşince GDD'ye eklenip EN zamirleri yazılacak. |
| 11 | İçerik ilk erişimde ana iş parçacığında yükleniyor | Şu an birkaç KB. | Aşama 4/6: içerik büyüyünce arka plana al ve açılış ekranı göster. |
| 12 | Gezinme ikonları material-icons-core'dan (Çanta için geçici olarak "Build") | Ek bağımlılık yok. | Aşama 9: özel ikon seti. |
