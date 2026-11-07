# İlerleme

Şu an: Kule hızlı tırmanış bitti; kat anları ölçümü ve Elif denetimi sürüyor. Bekleyen (kullanıcı): imzalama anahtarı (keystore) ve Google Play Console
Son commit: 0d67731 kamp kuralı (sonraki: hızlı tırmanış)
Uzak depo: https://github.com/crzyxpo000-max/litrgparman (origin/main)
Görev tanımı: `CLAUDE_CODE_GOREV.md`

## Ortam notları
- JDK: Android Studio JBR (Java 25) → `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (check.sh kendisi bulur).
- SDK: `local.properties` → `sdk.dir=C:/Users/alpar/AppData/Local/Android/Sdk` (git dışı).
- Emülatör: `Medium_Phone_API_36.0` (`emulator -avd Medium_Phone_API_36.0`), adb ile kurulum.
- OneDrive kilidi yüzünden lint ara sıra "dosya kullanılıyor" der: `./gradlew --stop` ve tekrar.

## Arayüz kimliği "Kırık pencere" (2026-11-07)
- [x] Üç yön önerisi ve taslak görseller (`docs/UI_DIRECTION.md`, `docs/ui/`); kullanıcı A + C birleşimini seçti
- [x] Tokenlar tek yerde (`ui/theme/Theme.kt`), gömülü OFL yazılar ve Ayarlar → Lisanslar, tek koyu tema (AMOLED birleşti)
- [x] Ekranlar: başlık, üst çubuk, bölüm satırı, Sistem kartı, Yankı satırı, seçenekler, Durum, savaş, istem kartları
- [x] Büyük anlar: seviye, evrim, sınıf, Benzersiz yetenek, Küresel Sıralama, sınır kırılması (motor: `Notice.moment/from/to`)
- [x] Emülatör görüntüleri: TR/EN, 360dp telefon ve 800dp tablet, en büyük yazı boyutu ve açık tema (DECISIONS #147–#148)
- [ ] TalkBack ile cihazda dinleme denemesi (ağaçta açıklamalar var; gerçek okuyucuyla denenmedi)

## Tarihli operasyonlar ve boş serbest günler (2026-11-07)
- [x] Tarihli operasyonlar sabit: geri sayım, hazırlık eylemleri (her tarihli bölüme 2–3), yan görev kesilmez, uyarı,
      operasyon öncesi kayıt; `HazirlikTest` (DECISIONS #141)
- [x] Ölçüm: bölüm başına serbest gün, havuzun tükendiği gün, salt savaş payı (`tools/qa/akis/serbest.py`, STORY_FLOW)
- [x] Antrenman / zaman atlama (`world/TimeSkip.kt`, `HubAction.Skip`, `events/96_zaman_atlama.yaml`, HubPanel
      diyaloğu, simülatör, `ZamanAtlamaTest`; DECISIONS #142)
- [x] Yeni üs olayları (onaylı 23 + 12 takip düğümü; 6, 9, 12, 13, 14, 15. bölümler; TR/EN inceleme; DECISIONS #143)
- [x] Kullanıcı kararı: bölge olayları. Ölçüt "hikayesiz gün" oldu, simülatöre insan benzeri strateji eklendi, motora saha kampı
      sahneleri (`kamp: true`) geldi; `SahaKampiTest` (DECISIONS #144)
- [x] 128 bölge/üs/kamp olayı (Bölüm 5–15, 18; TR/EN inceleme; DECISIONS #145). Hikayesiz gün ~18 puan düştü; hedef %35
      tutmadı (kalan açık oynanış başına ~49 gün). Denge: hakimiyet %62,0, son dağılımı en çok 1,3 puan kaydı
- [x] Kamp kuralı gevşetildi: güvenilen müttefik de kampa gelir (DECISIONS #146); kamp sahnesi 3,4 → 8,75/oynanış,
      hikayesiz gün gürültü içinde
- [x] Kule hızlı tırmanış: kademenin altındaki katlar tek yuvada, ödüller aynen; `KuleHizliTest` (DECISIONS #147).
      Hakimiyet %64,3; hikayesiz gün değişmedi
- [ ] Kule kat anları (ölçüm ve şablon önerisi) ve Elif denetimi: sürüyor

## Hikaye yapısı değişikliği — bölüm, üs ve görev  ✅ (2026-11-07)
- [x] Adım 1: olay sırası kaydı (`:tools:simulator:akis`), teşhis `docs/STORY_FLOW.md`, sınıflandırma, plan `docs/CHAPTERS.md`
- [x] Motor: `chapters.yaml` (17 bölüm + final), üs/görev (operasyon, yan, keşif), konum kuralı, zorunlu olay kilidi,
      son tarih ve taşma, tarihli operasyonlar, ana yay bölüme bağlı, tohum hatırlatması, dönüş özeti; kayıt v5 + v4 geçişi
- [x] İçerik: 255 olaya `konum`; brifing (18), darboğaz (9 yeni), varış (13 bölge); 252 tohum hatırlatması; TR/EN inceleme
- [x] Arayüz: bölüm satırı, operasyon / üsse dön düğmeleri, görev kartı bölgesi, Görevler'de bölüm hedefi, dönüş özeti
- [x] QA P1-21 (Günlük Görev) bu işle kapandı
- [x] Doğrulama: görev tabanlı a, b, c, d, e = 0 (öncesi 332 / 1.490 / 370 / 39 / 574); 1000 oynanış: her son
      ulaşılabilir, hakimiyet %63,9; DECISIONS #130–#139

## Aşama 9 — Denge ve yayın hazırlığı  ✅ (2026-11-06)
Plan:
- [x] `tools/simulator`: engine'i kullanan JVM aracı; 1000 oynanış (rastgele + sezgisel strateji karışımı), paralel
- [x] Rapor `docs/BALANCE.md`: son dağılımı, seviye eğrisi, ölüm oranları, perde başına oyuncu/NPC güç farkı, hakimiyet payı (hedef %60+), zaman içinde Dikkat
- [x] Denge düzeltmeleri: her son %1–%40 (Döngü hariç), seviye patlaması, ölüm döngüleri; simülatör testle sabitlenir
- [x] targetSdk 36, R8 küçültme + kurallar, imzalama `keystore.properties`'ten (git dışı), imzasız `bundleRelease`
- [x] Uyarlanabilir vektör simge + monokrom
- [x] `fastlane/metadata/android/{tr-TR,en-US}`, `docs/PRIVACY.md`
- [x] Bildirimler: isteğe bağlı, nadir; Android 13+ izin akışı (açıklama + istek, reddedilince ısrar yok); her biri küçük bir olay açar
- [x] Bağımsız inceleme, check.sh, commit; durma noktası: keystore ve Play Console (kullanıcı)
Notlar:
- Simülatör ve denge tamam: 1000 oynanışta her son %5–%22, her hedefli tarz kendi sonuna ≥ %49, hakimiyet %82, ölüm yalnızca hikaye savaşlarında. DECISIONS #116–120.
- Yayın: targetSdk 36 (compileSdk 37), R8, imzalama `keystore.properties` (git dışı; yoksa imzasız AAB), simge + monokrom, mağaza metinleri, `docs/PRIVACY.md`, isteğe bağlı bildirimler. DECISIONS #121–123. R8'li sürüm ve izin akışı emülatörde denendi.
- Bağımsız inceleme (yüksek bulgu yok; 11 orta/düşük) düzeltildi. 169 test, check.sh geçiyor.
- Bilinen eksik: simülatörde "haddini aşan" (her şeyi açığa vuran) bir oyun tarzı yok; Dikkat Perde 3'te yüksek (p50 ~90); bildirimin arka planda gösterimi emülatörde elle tetiklenmedi.

## Aşama 7 — Perde 2–4 ve sonlar  ✅ (2026-11-06)
Plan:
- [x] Motor: ölçekli düşman başvurusu (`id@seviye`: stat/can/saldırı/XP seviyeyle ölçeklenir), `xp_seviye` etkisi (seviyenin XP'sine oranla)
- [x] Motor: perde geçişi (Perde 1 sonu → Dönüş; içerik partisinde), bölgelerin perde aralığı ve giriş koşulu, Kıyıkent + 3 çok-evren dünyası + Sistem çekirdeği
- [x] Motor: Kule (100 kat; prosedürel katlar `tower.yaml`, her 10. kat yazılmış hikaye olayı), `kule_kat` değişkeni, Kule eylemi
- [x] Motor: sonlar (`endings.yaml`: koşul, sahne), `son_uygun` / `hicbir_son` koşulları, Gün 365 Hasat; kronik (önemli seçimler + `kronik` metni), epilog görünümü (NPC akıbetleri, fraksiyonlar, en kritik 5 seçim)
- [x] Motor: New Game+ (Döngü sonu; önceki bitirilmiş oyun sayısı `onceki_oyun`), kayıt v4
- [x] Arayüz: son ekranı + epilog kartları, Kule düğmesi, başlıkta New Game+
- [x] İçerik sözleşmesi `docs/PERDE2_4.md` (perde yapısı, bölgeler, bayraklar, son koşulları, partiler)
- [x] İçerik partileri (paralel): Perde 2 ana yay · fraksiyon zincirleri (5×5) · 8 NPC'nin tam yayları · Perde 2 genel · Perde 3 + çok-evren · Perde 4 + sonlar + epiloglar · Kule + düşmanlar
- [x] Testler: her son en az bir başsız stratejiyle ulaşılır; 365 günlük başsız oynanış çıkmazsız; Döngü → NG+
- [x] Bağımsız içerik ve kod incelemesi, düzeltmeler, check.sh, commit
Notlar:
- 165 motor testi, check.sh geçiyor. 252 olay toplam (Perde 2–4: ~190), 15 paralel içerik partisi + düzeltme ajanları. DECISIONS #105–115.
- Her son en az bir strateji profiliyle ulaşılıyor (`Perde2_4Test`); 12 rastgele 365 günlük oynanış çıkmazsız biter; Perde 2–4 olaylarının ~%97'sine ulaşılır.
- Bağımsız kod incelemesi (8 bulgu) ve içerik incelemesi (2 yüksek, 9 orta, 10+ düşük) düzeltildi.
- Bilinen eksik / Aşama 9: rastgele oynanışta Yalnız Avcı sık (12'de ~6; hedef ≤ %40), son günlerde seviye patlaması (bitişte Sv. 1000–1900), ara sıra ölüm döngüsü (Normal modda kayıt noktasıyla aşılıyor); `TurkishSuffix` bileşik yer adlarında (Kule Dibi'nde) yanlış ek verdiği için 35 yerde düz yazım kaldı; D+ sınıf evrimleri genel evrim (DECISIONS #26); emülatörde Perde 2–4 elle oynanmadı.

## Aşama 6 — Dikey dilim: Perde 1 içeriği  ✅ (2026-11-06)
- [x] Perde 1 tasarım belgesi `docs/PERDE1.md` (güç döngüsü, dünya durumu, olay listesi, partiler)
- [x] Yeni açılış: aktarımda Yankı bağı sahnesi, ilk savaş yenilgi sahnesi, hayatta kalan oranı
- [x] 48 Perde 1 olayı (4 paralel içerik partisi), Deniz/Yankı/Aras yay düğümleri (her biri ≥ 3), 4 ilk kamp (Lonca/Sığınak/Yükselenler/Sessiz Ağ), 2 çok odalı zindan, Kırılma (3 uyanış), Yeni tavan, Bekçi + dünya durumu epilogu
- [x] Bütçe: düşman 20 + boss, yetenek ≥ 30, sınıf 8 + 4, unvan ≥ 15, Günlük Görev şablonu 14
- [x] Perde1Test: içerik kuralları, 500 başsız oynanış (çıkmaz yok, tüm olaylara ulaşılır), dünya durumu çeşitliliği, düz yazılmış ad yasağı
- [x] Bağımsız içerik incelemesi (10 rastgele olay + açılış + final, TR/EN): 12 yüksek, 10 orta bulgu düzeltildi (telif: Günlük Görev seti ve Ceza Bölgesi; düz yazılmış adlar; dal tutarsızlıkları; final sözleşmesi)
- [x] Emülatörde yeni açılış (EN) oynandı: Yankı bağı sahnesi, bağ kartı, ilişki bildirimi
Notlar:
- 153 motor testi, check.sh geçiyor. DECISIONS #98–104.
- Bilinen eksik: Perde 2+ yay düğümleri (validateContent 25 uyarı, Aşama 7); ordu içeriği Aşama 7.

## Tasarım değişikliği — Yapay zeka anlatıcı kaldırıldı  ✅ (2026-11-06)
- [x] GDD (TR/EN), görev dosyası (Aşama 8 "Kaldırıldı", durma noktası ve gizlilik maddesi), CLAUDE.md, DECISIONS #97
- [x] Ayarlardaki anlatıcı düğmesi, `Settings.narratorMode` ve kaydı, ilgili metinler (iki dilde) kaldırıldı; kodda Narrator/proxy yoktu, internet izni yoktu

## Aşama 5 — NPC sistemi  ✅ (2026-11-06)
Plan:
- [x] `npc/` paketi: `NpcDef` (5 kişilik özelliği, değerler, başlangıç ilişkisi, hedefler, eğilim, savaş bloğu, yay düğümleri, eşik olayları, ses satırları), `NpcState` (4 eksen, solan hafıza, hedef ilerlemesi, seviye, fraksiyon, durum, izler)
- [x] İçerik: `npcs/*.yaml` (8 ana NPC), `knowledge.yaml` (bilgi günlüğü), `traits.yaml` (izler), `army.yaml` (ordu), `ranking.yaml` (prosedürel avcılar + sıra tepkileri); yeni etkiler/koşullar, `{npc:@}` odak belirteci
- [x] Günlük gündem: her NPC her gün bir adım (hedef, seviye eğrisi, ihmal bonusu, fraksiyon kayması); hafıza solması
- [x] Eşik olayları (ihanet tohumu 5–15 gün, kişisel görev, kaçış/itaat, rakip düellosu, ayrılma/fraksiyon değişimi) + genel eşik olayları (`events/90_esikler.yaml`), NPC'ye özel geçersiz kılma `esik_<tur>_<npc>`
- [x] Kalıcı ölüm: `npc_ol`; ölen NPC'nin olayları/tohumları kapanır
- [x] Yoldaşlar: kapasite (KAR + kademe), savaşa katılım (müttefik turu, hedef dağılımı, koruma sayacı), NPC'yle düello (`npc:` düşman)
- [x] OP MC: Küresel Sıralama (gerçek/görünen), Sistem duyuruları, fraksiyon tepkileri; rakiplerin "İlk" unvanlarını kapması
- [x] OP MC: yoldaş tepkileri (önemli sahnede görüş satırı; güce hayranlık/endişe/kıskançlık/bağımlılık), görünen seviyeye ve açığa vurmaya tepki
- [x] OP MC: bilgi günlüğü + yalanlar (hafızaya girer, şüpheciliğe göre ortaya çıkar), izler (oyuncu mutasyonu / NPC yarası), Ordu modeli, NPC büyüme eğrisi (testle sabit)
- [x] Kayıt v3: NPC durumları, sıralama, bilgi, izler, ordu; v2 `iliski_*` değişkenleri → ilişki eksenleri
- [x] Mevcut içerik `iliski_*` değişkenlerinden yeni `iliski` etkisine taşınır; doğrulayıcı NPC kontrolleri
- [x] Arayüz: İlişkiler ekranı (4 eksen, hatıralar, durum, izler), Sıralama ekranı, savaşta yoldaşlar, bilgi günlüğü
- [x] Testler, check.sh, bağımsız inceleme, commit
Notlar:
- 150 motor testi (NpcTest: büyüme eğrisi, eşikler, ölüm, yas, kapasite, yoldaşlı savaş determinizmi, düello ölümü, tanık yorumu, görüş, açığa vurma, sıralama, İlk unvan kapma, yalan, kayıt v3), check.sh geçiyor.
- 8 ana NPC dosyası (Deniz elle, diğer 7 paralel içerik ajanıyla), 8 genel eşik olayı, kişisel görev unvanları. DECISIONS #76–90.
- Bağımsız inceleme (15 bulgu) düzeltildi: ayrılık eşiği, eşiklerin kapsamı, hedef ilerlemesi, düello/infaz metinleri, ölü NPC konuşması, odak sızıntısı, zamirler, RNG akışı, tanık dengesi, kayıt geçişi, "gitti" durumu, telif yakınlığı. DECISIONS #91–96.
- Emülatörde: v2 kaydı açıldı (Deniz Güven 50+10 geçişi), İlişkiler ve Küresel Sıralama ekranları.
- Bilinen eksik (Aşama 6–7): NPC yay düğümü olayları henüz yazılmadı (validateContent uyarısı), NPC'ye özel eşik olayları, Ordu içeriği, Yankı'nın savaşsız ihanet yolu.

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
