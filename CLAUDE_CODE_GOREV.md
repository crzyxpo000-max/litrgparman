# Claude Code Görev Dosyası — Yükseliş Protokolü (Tam Geliştirme)

> **Kullanıcı için (bu kutuyu Claude Code yok sayabilir):**
> 1. Projeyi (yukselis-protokolu klasörü) bilgisayarına aç. Bu dosya klasörün kökünde olmalı.
> 2. Android Studio kurulu olsun (Android SDK onunla gelir). Terminalde klasöre gir ve `claude` yaz.
> 3. İlk mesaj olarak şunu yapıştır:
>    `CLAUDE_CODE_GOREV.md dosyasını baştan sona oku ve uygula. Bana yalnızca dosyada yazan durma noktalarında soru sor.`
> 4. İş çok uzun sürer ve birden fazla oturuma yayılabilir. Oturum biterse ya da durursa aynı klasörde yeniden başlat ve şunu yaz:
>    `CLAUDE_CODE_GOREV.md ve docs/PROGRESS.md'yi oku, kaldığın yerden devam et.`
> 5. Komut çalıştırma ve dosya düzenleme izinlerini sürekli onaylamak istemiyorsan Claude Code'un otomatik kabul ayarını aç.

---

## 1. Görevin

Sen bu projenin baş geliştiricisisin. Görevin, "Yükseliş Protokolü / Ascension Protocol" adlı Android LitRPG oyununu **Aşama 0'dan Aşama 9'a kadar kendi başına** geliştirmek ve yayına hazır hale getirmek. Oyun Türkçe ve İngilizce (Kuzey Amerika yerelleştirmesi) olacak. Her seçim hikayenin gidişatını ve sonunu değiştirebilmeli.

Aşama 1 (çekirdek) tamamlandı ama **Android uygulaması hiç derlenmedi**: internet erişimi olmayan bir ortamda yazıldı. Yalnızca `engine` modülü derlendi ve test edildi (38 test). İşe bunu doğrulayarak başla (Aşama 0).

## 2. Kaynaklar ve öncelik sırası

Çelişki olursa yukarıdaki kazanır:

1. Bu dosya (`CLAUDE_CODE_GOREV.md`): kapsam, sıra, kabul kriterleri.
2. `docs/GDD.md`: mekanik, hikaye, karakterler, sonlar için ana kaynak (Türkçe).
3. `docs/GDD.en.md`: İngilizce isimler, terimler ve yerelleştirme için ana kaynak.
4. `CLAUDE.md`: kod ve içerik kuralları. Her aşamada güncel tut.
5. `docs/DECISIONS.md`: verilmiş kararlar. Yenilerini buraya ekle.

Her oturumun başında 1, 4 ve `docs/PROGRESS.md`'yi oku. GDD'nin ilgili bölümünü, o bölüme ait aşamaya başlarken oku.

## 3. Çalışma döngüsü (her aşama için)

1. **Oku:** GDD'nin ilgili bölümlerini ve bu dosyadaki aşama tanımını oku.
2. **Planla:** 5–15 maddelik kısa bir planı `docs/PROGRESS.md`'deki aşama başlığının altına yaz.
3. **Uygula:** küçük adımlarla ilerle; her mantıklı birimden sonra derle ve test et.
4. **Doğrula:** `scripts/check.sh` geçmeden aşama bitmiş sayılmaz (Aşama 0'da yazılacak).
5. **Bağımsız inceleme:** aşamanın sonunda, işi yazmamış bir alt ajana (subagent) kodu ve içeriği GDD'ye ve bu dosyadaki kabul kriterlerine göre incelet. Bulduklarını düzelt.
6. **Kaydet:** `CLAUDE.md`, `docs/DECISIONS.md` ve `docs/PROGRESS.md`'yi güncelle, ardından `Aşama N: <özet>` başlıklı bir commit at.
7. **Devam et:** kullanıcıya sormadan bir sonraki aşamaya geç.

Uzun içerik işlerinde (Aşama 6 ve 7) içeriği 10'ar olaylık partiler halinde yaz. Her partiden sonra `validateContent` ve testleri çalıştır, sonra commit at. Birbirinden bağımsız içerik partilerini paralel alt ajanlara verebilirsin. Her alt ajana `CLAUDE.md`, isim tablosu ve şema örneği ver.

## 4. İlerleme dosyası: `docs/PROGRESS.md`

Kaldığın yerden devam edebilmenin tek yolu bu dosya. Biçimi:

```markdown
# İlerleme

Şu an: Aşama 3 — Savaş (devam ediyor)
Son commit: <kısa hash> <mesaj>

## Aşama 0 — Doğrulama  ✅
- [x] ...

## Aşama 3 — Savaş  ⏳
Plan:
- [x] Savaş durumu modeli
- [ ] Boss fazları
Notlar: <yarım kalan iş, bilinen sorunlar>
```

Bağlam dolmak üzereyse ya da oturum kapanacak gibiyse yarım kalan işi buraya not et ve commit at.

## 5. Ne zaman durup soracaksın

Yalnızca şu durumlarda dur ve kullanıcıya sor. Bunların dışında hiçbir şey sorma; makul varsayımı yap ve `DECISIONS.md`'ye yaz.

- Uygulama imzalama anahtarı (keystore) oluşturma ya da parolaları.
- Google Play Console'a yükleme ya da yayınlama.
- Para gerektiren herhangi bir hizmet.
- Geri alınamaz git işlemleri (force push, geçmişi yeniden yazma, dal silme).
- GDD'deki ana hikayeyi ya da bir sonu kökten değiştiren bir karar.

Durduğunda o ana kadarki işi commit et. Sorunu `PROGRESS.md`'ye yaz ve kullanıcıya tek, net bir soru sor. Soruyu beklemeden yapılabilecek başka iş varsa önce onu bitir.

## 6. Genel kurallar

### Kod
- `engine/` saf Kotlin kalır, Android'e bağımlı olmaz. Oyun mantığı `app/`'e sızmaz.
- `WorldState` değişmezdir ve yalnızca etkilerle değişir. Rastgelelik yalnızca `Rng` ile.
- Kayıt biçimini değiştirirken `SaveCodec.VERSION`'ı artır ve eski kayıtlar için geçiş yaz. Eski kayıtların açıldığını test et.
- GDD'deki her formül için birim testi yaz.
- Uyarısız derleme hedeflenir; lint hataları kalmaz.

### İçerik
- Oyuncuya görünen her metin `{tr, en}`; ikisi de dolu.
- İsimler asla metne yazılmaz, `{npc:...}`, `{yer:...}`, `{oyuncu}` belirteçleri kullanılır (`CLAUDE.md`'de söz dizimi).
- İngilizce metin çeviri değil, Kuzey Amerika yerelleştirmesidir. Mil, lb, °F, ABD/Kanada deyimleri ve kültürel referanslar kullanılır; hikaye ve mekanikler aynı kalır.
- **Telif:** ilham alınan romanlardan (Defiance of the Fall, He Who Fights With Monsters, The Primal Hunter, Solo Leveling) hiçbir metin, karakter, yer, sınıf ya da yetenek adı kopyalanmaz. Yalnızca genel tür kalıpları kullanılır.

### OP MC
- Oyun bir "OP MC" güç fantezisidir (GDD: OP MC tasarımı; o bölüm diğerlerinin üstündedir). Kahraman bir anomalidir; gerçek seçimler ahlakidir. Yalnızca mekanikler alınır; ilham kaynaklarından metin, isim, sınıf ya da yetenek kopyalanmaz.

### Yazım üslubu
- Türkçe anlatı ikinci tekil şahıs ve şimdiki zaman kullanır ("Gözlerini açıyorsun"); edebi ama akıcı.
- İngilizce anlatı ikinci şahıs ve şimdiki zamanda, doğal ve sert bir tür kurgusu sesiyle yazılır.
- Bir paragraf en fazla 3 cümle, bir sahne 2–5 paragraf olur.
- Seçenekler emir kipinde ve en fazla 60 karakterdir. Her seçeneğin görünür ya da gizli bir sonucu olur.
- Sistem bildirimleri kuru, kısa, yer yer alaycıdır. İngilizcede köşeli parantez üslubu kullanılır: `[Skill evolved: Shadow Step → Void Step]`.
- Kişilikler GDD'deki "Dinamik karakterler" bölümüne uyar; bir karakter iki dilde aynı kişidir.

## 7. Aşamalar ve kabul kriterleri

Her aşamanın sonunda `scripts/check.sh` geçer, `CLAUDE.md` ile `PROGRESS.md` güncellenir ve commit atılır. Bu üç madde her aşama için geçerlidir ve aşağıda tekrar edilmez.

### Aşama 0 — Ortam ve Aşama 1 doğrulaması
- [ ] `./gradlew :engine:test`, `:engine:validateContent` ve `assembleDebug` çalışır. `app` modülündeki derleme hataları davranış değiştirmeden düzeltilir.
- [ ] Gradle, AGP ya da Kotlin sürümü yükseltmek gerekirse en küçük yükseltme yapılır ve `DECISIONS.md`'ye yazılır.
- [ ] `./gradlew lint` hata vermez (uyarılar kabul).
- [ ] `scripts/check.sh` yazılır: test + validateContent + assembleDebug + lint. İleride eklenen kontroller de buraya girer.
- [ ] Uygulama emülatörde açılır, prolog iki dilde baştan sona oynanır. Emülatör bu ortamda çalışmıyorsa Robolectric ya da Compose UI testleriyle başlık → yeni oyun → ilk seçim akışı test edilir.
- [ ] `docs/PROGRESS.md` oluşturulur.

### Aşama 2 — Sistem mekanikleri
- [x] **Eğilim vektörü** (8 eğilim): `egilim_*` değişkenleri yeni modele taşınır ve `egilim` etkisi eklenir.
- [x] **Sınıflar:** `content/classes.*` dosyasında 8 temel sınıf (her eğilime bir) ve 4 gizli sınıf bulunur (Kapı Yiyen, Sistem Hatası, Yas Tutan, Avcı'nın Gözü). Her sınıfın açılma koşulu, seviye başı stat artışı ve başlangıç yeteneği tanımlıdır.
- [x] Seviye 10'da sınıf teklifi olayı açılır: en güçlü iki eğilimden birer sınıf + bir gizli ya da nadir sınıf.
- [x] **Serbest stat dağıtımı:** Durum ekranında +/− ve onay; onaylanan dağıtım geri alınamaz.
- [x] **Kademeler ve evrim:** kademe eşiğinde seviye kazancı durur ve Evrim Görevi olayı açılır. E kademesi evrimi her temel sınıf için 2 seçenekle gelir.
- [x] **Yetenekler:** model (aktif/pasif, maliyet, bekleme, seviye 1–10, kullanım XP'si, 2 dallı evrim, kaynaştırma tarifleri, ceza yetenekleri) ve en az 30 yetenek.
- [x] **Unvanlar:** en az 15 unvan. "İlk" unvanları dünya genelinde bir kez verilir ve bunun kaydı tutulur (Aşama 5'te rakipler de alabilecek).
- [x] **Meslekler:** model, seçim olayı, ayrı XP ve basit üretim (en az Simyacı, 5 tarif; Çanta ekranından).
- [x] **Öz ve Yol:** model ve D kademesindeki yuva; içerik Aşama 7'de.
- [x] **Günlük Görev:** şablonlardan günlük üretim, ilerleme etkisi, ödül ve Ceza Bölgesi olay kancası.
- [x] **Arayüz:** Yetenekler ekranı (liste, uzun basınca ayrıntı, evrim seçimi), seviye atlama ve evrim için tam ekran kart, animasyon ve titreşim.
- [x] Sistem bildirim metinleri `EffectApplier`'dan içerik dosyasına taşınır (DECISIONS #7).
- [x] **OP MC (GDD: OP MC tasarımı):**
  - [x] Yankı bağı kanon: prolog/açılışta oyuncunun penceresi bozulur, Yankı'ya bağlanır; bayrak `yanki_bagi`. (Aşama 1 prologu için retrofit.)
  - [x] Günlük Görev yalnızca oyuncuda; metinler bunu söyler. Seri bonusu: her 7 günlük seride +1 ek serbest puan.
  - [x] Gizli bilgi altyapısı: sahnelere stat koşullu **pasif sezgi** satırları (`sezgiler`), "[ALG]" önekiyle ayrı stilde gösterilir; gizli Sistem satırları.
  - [x] Garanti: seviye 10 teklifinin üçüncü seçeneği her zaman gizli sınıf ("Yankı'nın önerisi" geri dönüşü); oyun tarzına bağlı tek bir **Benzersiz** yetenek (her eğilim için bir tane) sınıf seçiminde verilir.
  - [x] Üstel büyüme: sınıfın seviye başı artışı kademeyle çarpılır (F ×1, E ×2, D ×3…); Durum ekranında **insanlık yüzdeliği** ("İlk %0,001").
  - [x] Kendini açan arayüz: Yetenekler ve Çanta sekmeleri Sistem verene kadar gizli; her biri kendi bildirimiyle açılır (`arayuz` etkisi + ilk yetenek/eşyada otomatik).
  - [x] Denge: her önemli seçim en az bir ahlak eksenini oynatır (içerik kuralı; Aşama 4'te doğrulayıcı uyarısı).

### Aşama 3 — Savaş
- [x] Savaş durumu `WorldState` içinde tutulur; savaşın ortasında kaydedip yükleyince aynı yerden devam edilir.
- [x] Sahne etkisi `savas`: düşman listesi, ortam etiketleri, kazanınca / kaybedince / kaçınca gidilecek sahneler.
- [x] GDD'deki inisiyatif, isabet, hasar ve kritik formülleri ile kademe farkı çarpanı.
- [x] Eylemler: Saldır, Yetenek, Savun, Analiz, Ortam, Konuş, Kaç; her turda bir ana + bir küçük eylem.
- [x] GDD'deki 8 durum etkisi.
- [x] Düşman davranış kalıpları (agresif, temkinli, sürü, kaçan), zaaflar ve boss fazları (%70 / %40 / %15).
- [x] Kaçan zeki düşmanlar hafızaya girer ve güçlenmiş olarak geri dönebilir.
- [x] Ölüm: kademe başına bir İkinci Şans; Demir Mod'da ölüm Hasat sonuna bağlanır.
- [x] `content/enemies.*` dosyasında en az 15 düşman ve 1 boss (Perde 1 için).
- [x] **Arayüz:** eylem ızgarası, hedef seçimi, uzun basınca ayrıntı kartı, savaş günlüğü, otomatik tur.
- [x] Testler: formüller; aynı tohumla aynı savaş; "Seviye 5 oyuncu ile Kanal Faresi" gibi örnek eşleşmelerde kazanma oranı beklenen aralıkta (simülasyonla).
- [x] **OP MC:**
  - [x] İki savaş modu: **Gerçek savaş** (denkler ve bosslar; her turda duruş: tam saldırı / ölçülü / savunmacı + Dayanım taahhüdü; düzyazı anlatım, sayılar isteğe bağlı; oyuncunun seçtiği **infaz** bitirişi) ve **Ezme** (çok altta kalan düşmanlar tek satırla otomatik çözülür).
  - [x] Yeniden kullanılabilir **uyanış tetikleyicisi**: koşullar (ör. %10 altı canla kazanmak, birini büyük bedelle korumak) gizli yetenek ya da evrim yolu açar; içerik dosyasında tanımlanır.
  - [x] **Kademe Kırma:** bir üst kademedeki düşmanı yenmek büyük XP, "Kademe Kıran" unvan zinciri ve Benzersiz yetenek şansı verir; kademe farkı hasar kuralı dünya için korunur.
  - [x] Düşmanların gerçek statları ve seviyesi oyuncuya görünür (Analiz gizli zaafı açar).
  - [x] Demir Mod ölümleri haddini aşmaktan gelir: rastgele savaşlar Ezme ya da kaçışla ölümsüz çözülebilir; simülasyon testi bunu doğrular.

### Aşama 4 — Hikaye motoru
- [x] İçerik biçimi kararı: GDD'ye uygun olarak **YAML'a (kaml) geç**. Mevcut JSON içerik dönüştürülür, anahtarlar aynı kalır ve JSON okuyucu kaldırılır. Bu, DECISIONS #1'i kapatır.
- [x] **Olay havuzu:** ön koşullar, ağırlık, etiketler, perde ve bölge kısıtları.
- [x] **Hikaye Yönetmeni:** GDD'deki puanlama formülü (ağırlık × gerilim uyumu × tazelik × kişisel bağ × ana yay önceliği) ve hedef gerilim eğrisi.
- [x] **Tohumlar:** `tohum` etkisi, gecikme aralığı, koşul ve kayıtla birlikte saklanan kuyruk.
- [x] **Gün döngüsü:** sabah Günlük Görev, gün içinde 2–4 olay yuvası ya da serbest eylem (keşif, antrenman, kapı, dinlenme), akşam kamp.
- [x] Bölge durumları: kontrol, tehlike ve açık kapılar; zamanında kapatılmayan kapı taşar.
- [x] Fraksiyon itibarı ve fraksiyonlar arası ilişki matrisi.
- [x] **Bayrak kaydı:** her bayrak `content/flags.*` dosyasında açıklamasıyla tanımlanır; tanımsız bayrak hata sayılır.
- [x] `validateContent` genişler: tanımsız bayrak, hiçbir yerde set edilmeyen bayrak (uyarı), ulaşılamayan olay, tohumun hedeflediği olayın varlığı.
- [x] Prolog sahneleri yeni sisteme taşınır (yazılmış giriş dizisi + olay havuzu).
- [x] **Arayüz:** Harita (bölgeler, tehlike, kapılar) ve Görevler ekranları.
- [x] **Başsız oynanış testi:** 200 rastgele oynanış hiç çökmeden ve çıkmaza girmeden ilerler (ileride simülatörün temeli).
- [x] **OP MC:**
  - [x] **Gizli ve Açık Güç:** gerçek seviye + görünen seviye; [Gizle] / [Açığa vur] seçenekleri; NPC'ler açığa vurulana kadar görünen seviyeye tepki verir.
  - [x] **Anomali Dikkati** (ana gerilim ölçeği) ve ayrı **Yozlaşma** ölçeği; eşikler avcı, Sistem "düzeltmesi", Kadim elçisi ve rakip olaylarını tetikler. Hikaye Yönetmeni gerilim uyumunda Dikkat'i kullanır.
  - [x] **Tanıklar ve Efsane:** NPC önünde yapılan başarılar söylentiye, unvana ve fraksiyona göre farklı okunan tepkilere dönüşür.
  - [x] **Gösteriş seçenekleri** ("[GÜÇ 40 · Önemsiz] …"); yeniden denenebilir (beyaz) ve tek seferlik (kırmızı) kontroller görünür olasılıkla, çoğunlukla sosyal/ahlaki kontrollerde.
  - [x] **Fırsat eli:** serbest zamanda 3 rastgele olay kartından biri seçilir.
  - [x] Doğrulayıcı: önemli seçeneklerin en az bir ahlak eksenini oynatması uyarısı.

### Aşama 5 — NPC sistemi
- [ ] GDD'deki model: 5 kişilik özelliği, değerler, 4 ilişki ekseni (Güven, Saygı, Korku, Bağlılık), solan hafıza, hedefler.
- [ ] Günlük gündem simülasyonu: her NPC her gün bir adım atar, seviye atlar ve fraksiyon değiştirebilir.
- [ ] GDD'deki eşik olayları: ihanet, kişisel görev, rakiplik, ayrılma.
- [ ] Ölüm kalıcıdır; ölen NPC'nin yay düğümleri kapanır.
- [ ] Yoldaş sistemi: kapasite KAR ve kademeye bağlı; yoldaşlar savaşa katılır.
- [ ] Rakipler (özellikle Aras) "İlk" unvanlarını oyuncudan önce kapabilir.
- [ ] 8 ana NPC'nin dosyaları ve her birinin yay düğümü listesi.
- [ ] **Arayüz:** İlişkiler ekranı; 4 eksen, son hatıralar ve durum görünür.
- [ ] **OP MC:**
  - [ ] **Küresel Sıralama:** oyuncu, Aras, Kaya ve prosedürel avcılar; NPC'ler ekran dışında tırmanır, Sistem değişiklikleri duyurur, loncalar/Yükselenler/rakipler sıraya tepki verir. Arayüzde sıralama ekranı.
  - [ ] **Yoldaş tepkileri:** büyük seçimlerden önce kişilik ve ilişkiye göre tek satırlık görüş; oyuncunun gücüne hayranlık / endişe / kıskançlık / bağımlılık. Deniz'in yayı "arkadaşımın canavara ya da tanrıya dönüşmesi".
  - [ ] **Bilgi günlüğü:** söylentiler, sırlar ve gerçek seviyeler diyalog açar; NPC'lere söylenen yalanlar hafızaya girer ve sonra ortaya çıkabilir.
  - [ ] **İzler:** oyuncuda bedelli mutasyon/soy uyanışı; NPC'lerde oyuncunun yaptıklarından kalan kalıcı özellik ve yaralar.
  - [ ] **Ordu** modeli: oyuncuyla ölçeklenen çağrılar/takipçiler (sınıfa göre gölge asker, canavar ya da lonca); içerik Aşama 7.
  - [ ] NPC'ler oyuncudan yavaş bir eğride büyür (formül testle sabitlenir).

### Aşama 6 — Dikey dilim: Perde 1 içeriği (iki dilde)
İçerik bütçesi (alt sınırlar):

| Tür | Adet |
| --- | --- |
| Olay (Perde 1, Gün 0–30) | 40 |
| Giriş dizisi (prolog yerine gerçek açılış) | 1 |
| Deniz, Yankı ve Aras yay düğümleri | Her biri 3 |
| Düşman / boss | 15 / 1 (Öğretici bossu) |
| Çok odalı kapı zindanı | 2 |
| Yetenek / sınıf / unvan | 30 / 8 + 4 / 15 |
| Günlük Görev şablonu | 10 |

- [x] Her olayda en az 3 seçenek bulunur: en az biri koşullu, biri tohum eken, biri bir NPC ilişki eksenini değiştiren.
- [x] Perde 1 sonunda en az 3 belirgin farklı dünya durumu oluşabilir: hayatta kalan oranı, Deniz'in akıbeti, ilk fraksiyon eğilimi.
- [x] Başsız oynanış testi Perde 1 için genişler: 500 oynanışta çıkmaz yoktur ve tüm Perde 1 olaylarına en az bir kez ulaşılır.
- [x] Bağımsız içerik incelemesi: bir alt ajan rastgele 10 olayı TR ve EN olarak okur; üslup, yerelleştirme ve tutarlılık sorunlarını raporlar, sorunlar düzeltilir.
- [x] **OP MC güç döngüsü:** Perde 1 (F kademesi) Mücadele → Kırılma (yazılmış uyanış anı) → Hakimiyet (oyun süresinin çoğu) → Yeni tavan sırasını izler; yeni açılış Yankı bağını anlatır.

### Aşama 7 — Perde 2–4 ve sonlar
İçerik bütçesi:

| Tür | Adet |
| --- | --- |
| Olay: Perde 2 / 3 / 4 | 60 / 45 / 25 |
| Fraksiyon görev zinciri (Sığınak, Lonca, Yükselenler, Sessiz Ağ, Kadimler) | Her biri 5 düğüm |
| 8 ana NPC'nin tam yayı | Her biri 3–5 düğüm, tüm olası kaderleriyle |
| Kule | 100 kat: prosedürel katlar + her 10. katta yazılmış hikaye katı |
| Çok-evren dünyası | 3 |
| Son | 8 + gizli Döngü |

- [ ] Sonlar GDD'deki koşullara göre belirlenir. Son ekranı epilog kartlarını gösterir: her NPC'nin akıbeti, fraksiyonlar ve en kritik 5 seçim.
- [ ] Döngü sonu New Game+'ı açar.
- [ ] Hacimden önce dallanma kapsamı gelir: her son, en az bir başsız oynanış stratejisiyle ulaşılabilir olmalı. Bunu kanıtlayan test yazılır.
- [ ] **OP MC:** her kademe (E→S) dört evreli döngüyü izler; Perde 3–4'te Ordu içeriği; sonlar oyuncunun ne tür bir OP MC olduğunu (koruyucu, tiran, tanrı, kurtarıcı) ahlak eksenlerinden okur.

### Aşama 8 — Kaldırıldı (yapay zeka anlatıcı oyundan çıkarıldı)

Oyun tamamen çevrimdışı çalışır; tüm metinler önceden yazılmış içerikten gelir (DECISIONS #97).

### Aşama 9 — Denge ve yayın hazırlığı
- [ ] `tools/simulator`: engine modülünü kullanarak rastgele ve sezgisel stratejilerle 1000 oynanış simüle eder. Son dağılımı, seviye eğrisi ve ölüm oranları `docs/BALANCE.md`'ye raporlanır, dengesizlikler düzeltilir. Hiçbir son %1'in altında ya da %40'ın üstünde kalmamalı (gizli son hariç).
- [ ] `targetSdk`, Google Play'in o an istediği seviyeye yükseltilir.
- [ ] Release derlemesi: R8 küçültme açık ve kuralları yazılmış. İmzalama `keystore.properties` dosyasından okunur (dosya git dışında tutulur). Anahtarı oluşturmak bir durma noktasıdır.
- [ ] Son uygulama simgesi (uyarlanabilir, vektör) ve monokrom sürümü.
- [ ] Mağaza metinleri: `fastlane/metadata/android/tr-TR` ve `en-US` (başlık, kısa ve uzun açıklama, yenilikler).
- [ ] Gizlilik politikası taslağı (`docs/PRIVACY.md`): Oyun hiçbir kişisel veriyi cihaz dışına göndermez; kayıtlar yalnızca cihazda ve Android otomatik yedeklemede tutulur.
- [ ] `./gradlew bundleRelease` imzasız olarak da çalışır; imzalı AAB, anahtar sağlanınca üretilir.
- [ ] **OP MC:**
  - [ ] Simülatör ayrıca raporlar: perde başına oyuncu/NPC güç farkı, oyun süresinin "hakimiyet" evresindeki payı (hedef %60+), zaman içindeki Dikkat düzeyi, son dağılımı.
  - [ ] **Bildirimler:** oturumlar arasında nadir, isteğe bağlı Android bildirimleri ("[Aras Kurt Küresel Sıralama'da seni geçti.]"); her biri küçük bir olay açar. Android 13+ bildirim izni usulüne uygun (açıklama + istek, reddedilince ısrar etmemek) istenir.

## 8. Bitiş

Tüm aşamalar bittiğinde (ya da bir durma noktasında beklerken) kullanıcıya kısa bir rapor ver:

- Hangi aşamalar tamam, hangileri bekliyor ve neden.
- `scripts/check.sh` sonucu ve test sayısı.
- Telefonda denemesi gereken 5 şey.
- Kullanıcının yapması gerekenler: keystore, Play Console.

Sonra `docs/PROGRESS.md`'yi "Tamam" olarak işaretle ve son commit'i at.
