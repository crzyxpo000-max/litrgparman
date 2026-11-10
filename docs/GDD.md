# Yükseliş Protokolü — LitRPG Oyun Tasarım Dokümanı

Nov 5, 2026 · @arman

## Vizyon ve tasarım ilkeleri

Yükseliş Protokolü, Dünya'ya aniden inen bir "Sistem"in insanlığı seviye, sınıf ve yeteneklerle yeniden yazdığı, Android için tek oyunculu ve tamamen metin tabanlı bir LitRPG'dir. Oyuncu her seçimiyle hem karakterinin build'ini hem dünyanın kaderini şekillendirir; iki oynanış aynı bitmez.

**İlham kaynaklarından alınan mekanik DNA.** Hikaye ve karakterler tamamen özgündür; kaynaklardan yalnızca tür kalıpları alınır.

| Kaynak | Alınan mekanik fikir | Oyundaki karşılığı |
| --- | --- | --- |
| Defiance of the Fall | Dünya çapında entegrasyon olayı, ilk olma ödülleri, kavramsal güç yolu | Entegrasyon Günü, "İlk" unvanları, Öz sistemi |
| He Who Fights With Monsters | Kişilikli sistem arayüzü, ayrıntılı yetenek metinleri, ahlak ve kimlik ikilemleri | Alaycı Sistem bildirimleri, ahlak eksenli seçimler |
| The Primal Hunter | Davranışa göre açılan gizli sınıf ve yetenekler, kademe evrimi, meslek ile savaş sınıfının ayrılması | Gizli eğilim takibi, F→S kademeleri, ayrı Meslek ağacı |
| Solo Leveling | Zayıf başlayıp tek başına yükselen kahraman, kapılar ve zindanlar, günlük görev ve ceza, gölge ordusu benzeri yoldaşlar | Kapı zindanları, Günlük Görev, Çağrı/yoldaş sistemi |

**Tasarım ilkeleri**

1. **Seçim gerçekten önemli olmalı.** Her büyük seçim en az bir kalıcı dünya bayrağını değiştirir; sonuçlar bazen 10–30 oyun günü sonra ortaya çıkar.
2. **Build, oyuncunun davranışından doğar.** Sınıf menüden seçilmez; Sistem oyuncunun nasıl savaştığını ve neyi seçtiğini izler, teklifini buna göre yapar.
3. **Sayılar görünür ve tatmin edici olmalı.** Seviye atlama, yetenek evrimi ve unvan anları türün asıl ödülüdür; her birinin kendine ait bir ekran anı vardır.
4. **Dünya oyuncu olmadan da döner.** NPC'ler ve fraksiyonlar kendi hedeflerini izler; oyuncu bir kasabayı kurtarmazsa o kasaba düşebilir.
5. **İskelet el yazımı, ayrıntılar dinamik.** Ana hikaye düğümleri tasarlanır; aralarını olay motoru, Hikaye Yönetmeni ve tohumlar doldurur; tüm metin önceden yazılmış içerikten gelir.

**Hedef:** tek oynanış 15–25 saat, 8 son + 1 gizli son, en az 3 belirgin build yolu, Türkçe ve İngilizce tam dil desteği, platform Android (telefon ve tablet).

## OP MC tasarımı

Yükseliş Protokolü bir "OP MC" güç fantezisidir: kahraman zayıf başlar ama dünyanın kurallarının dışında büyür. Bu bölüm diğer tüm bölümlerin üstündedir; çelişkide bu bölüm geçerlidir. Mekanikler ilham alınan türden gelir; hiçbir metin, isim, sınıf ya da yetenek kopyalanmaz.

### Temel kural: Kahraman bir anomalidir

- **Herkes standart Sistem'i alır. Oyuncu bozuk ve ayrıcalıklı bir sürümünü alır.** Entegrasyon Günü'nde Sistem'in hatalı parçası Yankı oyuncuya bağlanır. Bu, prologda ve açılış dizisinde kanondur: aktarım sırasında oyuncunun penceresi bozulur, bir satır yalnızca ona görünür.
- **Günlük Görev yalnızca oyuncudadır.** Diğer insanlar böyle bir şeyin var olduğunu bilmez; bir NPC'ye anlatmak bir [Açığa vur] eylemidir.
- **Gizli bilgi:** oyuncu düşmanların gerçek statlarını, gerçek seviyeleri, görevlerin gizli ödüllerini ve Sistem'in gizli satırlarını görür. Yüksek statlar sahnelere yalnızca oyuncunun okuduğu **pasif sezgi** satırları ekler ("[ALG] Seviyesi sahte. Gerçek: 34.").
- **Garanti:** her oynanışta oyuncu bir **Benzersiz yetenek** ve bir **gizli sınıf** kazanır; hangileri olduğu oyun tarzına (Eğilim vektörü, ahlak, bayraklar) bağlıdır. Seviye 10 teklifinin üçüncü seçeneği her zaman gizli sınıftır; hiçbir gizli sınıfın koşulu sağlanmadıysa, en güçlü eğilime en yakın gizli sınıf "Yankı'nın önerisi" olarak gelir.

### Güç eğrisi: her kademe bir döngü

Her kademe (F→S) dört evreden oluşur ve içerik bu sırayı izler:

1. **Mücadele (kısa):** dünya yeni güçlendi; savaşlar gerçek ve taktiktir.
2. **Kırılma:** koşulla tetiklenen, elle yazılmış bir **uyanış anı** (ör. %10'un altında canla kazanmak, birini büyük bedelle korumak). Gizli bir yetenek ya da evrim yolu açar. Yeniden kullanılabilir **uyanış tetikleyicisi** mekaniğiyle kurulur.
3. **Hakimiyet (uzun, oyun süresinin çoğu):** oyuncu çevresini açıkça geride bırakır.
4. **Yeni tavan:** bir üst kademeden gelen bir şey ölçeği sıfırlar.

Oyuncunun stat büyümesi **üsteldir** (Günlük Görev serisi, Benzersiz yetenek, unvan bonusları, kademeyle büyüyen sınıf artışı); NPC'ler daha yavaş bir eğride büyür. Büyük sayılar eğlencenin parçasıdır: hasar sayıları, art arda seviye atlamalar ve Durum ekranında **insanlık yüzdeliği** ("İlk %0,001").

### Yeni sistemler

- **Küresel Sıralama:** insanlığın en güçlülerinin canlı listesi (oyuncu, Aras Kurt, Kaya Demir, prosedürel avcılar). NPC'ler ekran dışında tırmanır; Sistem değişiklikleri duyurur. Loncalar, Yükselenler ve rakipler oyuncunun sırasına tepki verir.
- **Gizli ve Açık Güç:** oyuncunun bir **gerçek seviyesi** ve bir **görünen seviyesi** vardır. [Gizle] seçenekleri Dikkat'i düşük tutar ve "beni hafife aldılar" anlarını açar; [Açığa vur] seçenekleri korku, saygı ve katılım teklifleri getirir ama Dikkat'i artırır. NPC'ler açığa vurulana kadar görünen seviyeye tepki verir.
- **Anomali Dikkati:** "kaybedebilirsin" yerine ana gerilim ölçeği; **tek ölçek** (Paket 4, DECISIONS #168: eski Yozlaşma ölçeği Dikkat'e katıldı; karanlık/lanetli güç kullanımı, ceza yetenekleri ve izler doğrudan Dikkat artırır). Hızlı büyüme ve açığa vurmalar onu artırır; eşikler avcıları, Sistem "düzeltmelerini", Kadim elçilerini ve rakipleri tetikler. Karanlık ve lanetli yetenekler için ayrı bir **Yozlaşma** ölçeği vardır.
- **Tanıklar ve Efsane:** NPC'lerin önünde yapılan başarılar söylenti olarak yayılır, unvanlara ve fraksiyon tepkilerine dönüşür. Aynı başarı her fraksiyonda farklı okunur (Sığınak bir katliamdan korkar, Demir Lonca hayranlık duyar).
- **Kademe Kırma:** kademe farkı hasar kuralı dünya için geçerli kalır; ama bir üst kademedeki düşmanı yenmek büyük XP, bir "Kademe Kıran" unvan zinciri ve Benzersiz yetenek şansı verir.
- **Ordu:** oyuncuyla birlikte ölçeklenen çağrılar ve takipçiler (sınıfa göre gölge askerler, evcilleştirilmiş canavarlar ya da bir lonca); güç fantezisi Perde 3–4'te büyük ölçekte çalışır.

### Yeniden ayarlanan mekanikler

- **Kendini açan arayüz:** Yetenekler, Çanta, Harita ve sonra Anomali / Kök Erişim sekmeleri Sistem onları verene kadar gizlidir; her biri kendi bildirimiyle gelir ("[Envanter verildi]").
- **Pasif Sistem sezgisi:** yüksek statlar sahnelere özel satırlar ekler (yukarıda).
- **Gösteriş seçenekleri:** yüksek statlar "[GÜÇ 40 · Önemsiz] Kapıyı menteşesinden sök" gibi sorunu tarzla çözen seçenekler gösterir. Yeniden denenebilir (beyaz) ve tek seferlik (kırmızı) kontroller görünür olasılıklarla kalır, ama çoğunlukla sosyal ve ahlaki kontrollerde kullanılır.
- **Fırsat eli:** serbest zamanda 3 rastgele olay kartı gösterilir (Perde 1'de 2; DECISIONS #163); oyuncu birini seçer.
- **İki savaş modu:**
  - **Gerçek savaş** (denkler ve bosslar): taktik; her turda duruş (tam saldırı / ölçülü / savunmacı) ve Nefes taahhüdü; düzyazıyla anlatılır, sayılar istenirse görünür; boss, zeki düşman ve NPC düellosu oyuncunun seçtiği bir **infaz** hamlesiyle biter, sıradan düşman tek satırla düşer (DECISIONS #157). Taktik (duruş, efor) ve Ortam ilk üç savaştan sonra açılır.
  - **Ezme:** oyuncunun açıkça üstün olduğu sıradan düşmanlar tek, şık bir satırla otomatik çözülür. Kasılma (grind) yoktur.
- **Yoldaş tepkileri:** büyük seçimlerden önce yoldaşlar kişiliklerine ve ilişkilerine göre tek satırlık görüş bildirir. Oyuncunun büyüyen gücüne de hayranlık, endişe, kıskançlık ya da bağımlılıkla tepki verirler. Deniz'in yayı "arkadaşımın bir canavara ya da tanrıya dönüşmesini izlemek" olur.
- **Bilgi günlüğü:** öğrenilen söylentiler, sırlar ve gerçek seviyeler diyalog seçeneklerini açar; NPC'lere söylenen yalanlar onların hafızasına girer ve sonra ortaya çıkabilir.
- **İzler:** oyuncuda bedelli mutasyonlar ve soy uyanışları; NPC'lerde oyuncunun onlara yaptıklarından kalan kalıcı özellikler ve yaralar.
- **Rakipler "İlk" unvanlarını oyuncudan önce kapabilir.**
- **Bildirimler:** oturumlar arasında nadir, isteğe bağlı Android bildirimleri ("[Aras Kurt Küresel Sıralama'da seni geçti.]", "[A kademesinden bir varlık seni fark etti.]"); her biri küçük bir olay açar. Bildirim izni usulüne uygun istenir.

### Denge kuralları

- **Gerçek seçimler ahlakidir.** Oyuncu ne yaparsa yapsın güçlenir; ahlak eksenleri onun nasıl bir OP MC olacağını belirler (koruyucu, tiran, tanrı, kurtarıcı). Her önemli seçim en az bir ekseni oynatır.
- **Demir Mod ölümleri haddini aşmaktan gelir** (her şeyi açığa vurup üst kademe varlıkları kışkırtmak), rastgele savaşlardan değil.
- **Simülatör** perde başına oyuncu/NPC güç farkını, oyun süresinin "hakimiyet" evresindeki payını (hedef %60+), zaman içindeki Dikkat düzeyini ve son dağılımını raporlar.

## Dünya ve olay örgüsü iskeleti

Oyun, Sistem'in Dünya'yı "Çok-evren Ağı"na kattığı Entegrasyon Günü başlar. İnsanlığın 365 günü vardır: sonunda Dünya ya tam bir Katman olarak kabul edilir ya da hasat edilir.

**Entegrasyon Günü (Gün 0).** Gökyüzünde mavi paneller açılır, herkes "Öğretici Katman"a ışınlanır. 30 günlük öğreticide ilk canavarlar, ilk sınıf teklifi ve ilk ahlak sınavları gelir. Öğreticiden sağ çıkan insanların oranı (`dunya.hayatta_kalan_orani`) sonraki perdelerde insanlığın gücünü belirler.

**Sistem.** Tarafsız görünen ama kendi gündemi olan bir yapı. Bildirimleri kuru, zaman zaman alaycıdır. Oyunun ana gizemi: Sistem'i kim yazdı ve neden? Cevap tek değildir; oyuncunun Perde 2'deki seçimleri hangi gerçeğin "doğru" olduğunu belirler.

**Mekanlar**

- **Öğretici Katman** — orman, harabe şehir, ilk zindan kapıları; Gün 0–30.
- **Entegre Dünya** — dönüşte oyuncunun şehri Kıyıkent (Ege kıyısında kurgusal bir liman şehri) ve çevresi canavar bölgelerine dönüşmüştür; Kasaba Çekirdekleri ele geçirilip yönetilebilir.
- **Kapılar** — E'den S'ye derecelendirilmiş zindan portalları. Zamanında kapatılmazsa "kapı taşması" olur ve canavarlar bölgeye yayılır.
- **Kule** — 100 katlı dikey zindan; her 10 katta bir hikaye sırrı açılır.
- **Çok-evren** — Perde 3'te açılan yabancı ırk dünyaları ve onların siyaseti.

**Fraksiyonlar**

| Fraksiyon | Ne ister | Oyuncudan beklediği |
| --- | --- | --- |
| Sığınak Konseyi | Sivilleri korumak, oy birliğiyle düzen | Kasaba savunması, kaynak paylaşımı |
| Demir Lonca | Güçlülerin yönettiği meritokrasi | Kapı temizliği, rakipleri bastırmak |
| Yükselenler | Sistem'e tapan, insanları "seçilmiş" ve "yakıt" diye ayıran tarikat | Ritüel, kurban, mutlak sadakat |
| Sessiz Ağ | Sistem'in kaynak koduna ulaşmak isteyen gizli araştırmacılar | Bilgi toplamak, riskli gizli görevler |
| Kadimler | Dünya'yı hasat etmek ya da vassal yapmak isteyen çok-evren ırkı | Ticaret, elçilik, ihanet |

**Ana çatışma üç katmanlıdır.** Dışarıda Kadimler Gün 365'i bekler. İçeride fraksiyonlar insanlığın nasıl hayatta kalacağı üzerine savaşır. Derinde Sistem'in kendisi vardır: oyuncu ilerledikçe onun bir hasat aracı mı yoksa bir kurtarma protokolü mü olduğunu keşfeder.

## Sistem mekanikleri

Karakter gücü beş katmandan oluşur: statlar, seviye ve kademe, sınıf, yetenekler, unvanlar. Bunların üstünde geç oyunda Öz (kavramsal güç) devreye girer.

### Statlar

| Stat | Kısaltma | Etkisi |
| --- | --- | --- |
| Güç | GÜÇ | Fiziksel hasar, taşıma, bazı kapıları zorla açma |
| Çeviklik | ÇEV | İnisiyatif, kaçınma, kritik şansı |
| Dayanıklılık | DAY | Can (HP), fiziksel direnç, Nefes (savaş kaynağı; eski adı Dayanım, DECISIONS #174) |
| Zeka | ZEK | Büyü hasarı, mana havuzu, bulmaca çözümleri |
| Bilgelik | BİL | Mana yenilenmesi, büyü direnci, Öz kavrayışı |
| Algı | ALG | İsabet, kritik hasar, gizli nesne/tuzak/yalan fark etme |
| İrade | İRD | Zihinsel direnç, korku etkileri, Öz kullanımı |
| Karizma | KAR | İkna, NPC ilişki kazancı, yoldaş kapasitesi |

Her seviyede sınıfa göre otomatik stat artışı + 3 serbest puan gelir. Türetilmiş değerler:

```latex
HP = 50 + 10 \cdot DAY + 5 \cdot L \qquad Mana = 30 + 8 \cdot ZEK + 3 \cdot BIL \qquad Dayanim = 40 + 5 \cdot DAY + 5 \cdot CEV
```

```latex
XP_{gerekli}(L) = \lfloor 100 \cdot L^{1.8} \rfloor \qquad XP_{kazanc} = XP_{taban} \cdot 1.15^{(L_{dusman} - L_{oyuncu})}
```

Seviye farkı bonusu, zayıfken güçlüyü yenmeyi türün ruhuna uygun biçimde çok ödüllendirir; çarpan 0,1 ile 5 arasında sınırlanır.

### Kademeler (grade) ve evrim

| Kademe | Seviye aralığı | Evrimde açılan |
| --- | --- | --- |
| F | 1–24 | Temel sınıf (Seviye 10'da) |
| E | 25–49 | İlk sınıf evrimi, Meslek ikinci dalı |
| D | 50–99 | Öz yuvası 1, yoldaş kapasitesi +1 |
| C | 100–199 | Öz birleştirme, Kule 50+ |
| B | 200–399 | Çok-evren seyahati |
| A | 400–799 | Yol (iki Öz'den oluşan özgün güç) |
| S | 800+ | Son perde yetkileri |

Her kademe eşiğinde seviye kazancı durur ve bir **Evrim Görevi** açılır. Evrim görevi bir anlatı olayıdır: oyuncu iki ya da üç evrim yolundan birini seçer; seçim sınıfın adını, statlarını ve bir yeni yeteneği belirler.

### Sınıf: davranıştan doğan teklif

Sınıf menüden seçilmez. Oyun boyunca her eylem gizli bir **Eğilim** vektörüne puan ekler: Yakın Dövüş, Menzilli, Büyü, Gizlilik, Koruma, Çağırma, Zanaat, Karanlık. Seviye 10'da Sistem üç sınıf teklif eder: en yüksek iki eğilimden birer sınıf + özel bayraklardan gelen bir nadir/gizli sınıf.

Gizli sınıf örnekleri:

- **Kapı Yiyen** — seviyesinin 5 üstündeki bir kapı bossunu tek başına yenmek.
- **Sistem Hatası** — üç glitch olayını bulup Yankı'yla konuşmak.
- **Yas Tutan** — bir yoldaşın ölümünden sonra 3 gün kimseyle savaşmadan yaşamak.
- **Avcı'nın Gözü** — 50 düşmanı ilk vuruşta kritikle öldürmek.

Sınıf ayrıca bir **Meslek** ağacından bağımsızdır: Simyacı, Demirci, Rün Ustası, Aşçı, Kartograf. Meslekler ayrı seviye alır ve ekipman, iksir, harita gibi kaynaklar üretir.
**Paket 4 (DECISIONS #166): Meslek/üretim sistemi kapalıdır.** Tanımlar ve kod durur (`professions.yaml`, `Progression.PROFESSIONS_ENABLED`), oyuncuya teklif gelmez, Çanta'da üretim yoktur.

### Yetenekler

- **Nadirlik:** Sıradan → Nadir → Epik → Efsanevi → Mitik → Benzersiz. Benzersiz yetenekler her oynanışta yalnızca bir kişide bulunur; NPC'ler de alabilir.
- **Kullanımla gelişme:** her yeteneğin 1–10 seviyesi vardır, kullanıldıkça XP alır.
- **Dallı evrim:** Seviye 10'da yetenek iki yoldan birine evrilir (örnek: *Gölge Adımı* → *Boşluk Adımı* ya da *Gölge Avı*).
- **Kaynaştırma:** iki maksimum seviye yetenek + bir koşul → yeni yetenek (örnek: *Alev Oku* + *Rüzgar Adımı* + 100 hareketli atış → *Kuyruklu Yıldız Atışı*).
- **Ceza yetenekleri:** karanlık seçimler güçlü ama yan etkili yetenekler verir (örnek: *Ruh Emme* — hasar verdiğinde iyileşirsin, Karizma kalıcı −1/kademe).

### Unvanlar, Öz ve Miras

- **Unvanlar** stat bonusu verir ve NPC tepkilerini değiştirir. "İlk" unvanları Defiance tarzıdır: dünyada bir şeyi ilk yapan alır (*İlk Kapı Kapatan*, *Öğreticinin İlk Kanı*). Negatif unvanlar da vardır (*Kardeş Katili*: Sığınak itibarı tavanı −50).
- **Öz (kavram):** D kademesinden itibaren oyuncu deneyimlerle Öz kavrar: Ateş, Uzay, Ölüm, Koruma, Yıkım, Zaman, Avlanma, Yankı. A kademesinde iki Öz birleşip oyuncuya özgü bir **Yol** olur. Yol, son perdedeki nihai yeteneği ve bazı sonların koşulunu belirler.
- **Miras:** gizli görevlerle ölü bir Kadim'in mirası alınabilir. Büyük güç verir ama bir bedeli vardır: mirası bırakan varlığın hafızası oyuncuyla konuşur ve kendi hedeflerini dayatır.

### Günlük Görev ve ekonomi

- **Günlük Görev:** her oyun günü Sistem dünyaya özgü bir görev verir (kampın suyunu taşı, sınırı dolaş, 3 kapı kapat; spor salonu sayı listeleri değil). Tamamlamak +1 serbest stat (her 7 günlük seri +1 daha); görmezden gelmek "Durmuş Saat"e ışınlanma riski (DECISIONS #98). İlk üç gün deneme süresidir (ceza ve seri kaybı yok); ceza yalnız gün sonunda görev tamamlanmadıysa işlenir ve Durmuş Saat yalnız üs ya da keşif kampı anında açılır, hiçbir sahneyi kesmez (DECISIONS #156). **Günlük Görev yalnızca oyuncudadır** (bkz. OP MC tasarımı).
- **Para:** yalnız **Öz Kristali** (evrim, yetenek kaynaştırma, hikayedeki ödemeler; koşul `oz_kristali_min`). Kredi kaldırıldı (Paket 4, DECISIONS #167): hikayedeki ödemeler kristale çevrildi (yaklaşık 25 Kredi = 1 kristal).
- **Ekipman:** 8 yuva, nadirlik renkleri, set bonusları (Meslek kapalı olduğu için yükseltme yok).

## Savaş sistemi

Savaş tur tabanlıdır ama metin tabanlı olmanın avantajını kullanır: oyuncu sadece "saldır" demez, ortamı ve düşmanın zaafını okuyup yaratıcı eylemler seçer.

**Tur akışı**

1. İnisiyatif atılır; sıra en yüksekten başlar.
2. Sıradaki taraf bir **ana eylem** + bir **küçük eylem** (eşya, pozisyon, kısa konuşma) yapar.
3. Durum etkileri işler, bekleme süreleri azalır.
4. Boss için faz eşiği kontrol edilir (HP %70 / %40 / %15).
5. Bir taraf düşene, kaçana ya da teslim olana kadar tekrarlanır.

**Ana eylemler:** Saldır · Yetenek · Savun · Analiz (ALG kontrolü; düşmanın statlarını ve zaafını açar) · Ortam (çevre etiketlerini kullan: tavanı çökert, suya elektrik ver) · Konuş (zekalı düşmanlarda ikna, tehdit, pazarlık) · Kaç.

**Formüller**

```latex
Inisiyatif = CEV + 0.5 \cdot ALG + d20
```

```latex
Isabet = 75\% + (ALG_{saldiran} - CEV_{savunan}) \cdot 1.5\% \quad (\%5 \text{ ile } \%95 \text{ arasi})
```

```latex
Hasar = (Taban_{yetenek} + Stat_{ana} \cdot Kat_{yetenek}) \cdot \frac{100}{100 + Direnc} \cdot (1 \pm 0.1)
```

```latex
Kritik_{sans} = 5\% + 0.2 \cdot CEV \qquad Kritik_{carpan} = 1.5 + 0.005 \cdot ALG
```

Kademe farkı ayrıca ezici bir çarpan uygular: her kademe farkı için hasar ×2 (üst kademe) ya da ×0,5 (alt kademe). Bu, türdeki "üst kademe birini neredeyse yenemezsin" hissini korur; zayıf oyuncuyu tuzak, ortam ve zaaf kullanmaya iter.

**Durum etkileri**

| Etki | Ne yapar | Tipik kaynak |
| --- | --- | --- |
| Kanama | Her tur maks. HP'nin %3'ü | Bıçak, pençe |
| Zehir | Her tur sabit hasar, iyileşme −50% | Örümcek, simya |
| Yanma | Her tur hasar, savunma −10% | Ateş büyüsü |
| Sersemleme | 1 tur eylem yok | Kritik darbe, kalkan vuruşu |
| Korku | İRD kontrolü; başarısızsa saldıramaz | Boss kükremesi |
| Kör | İsabet −40% | Duman, ışık |
| Hızlanma | Tur başına iki ana eylem | Yetenek, iksir |
| Kalkan | Belirli hasarı emer | Koruma yetenekleri |

**Düşman tasarımı.** Her düşmanın bir davranış kalıbı (agresif, temkinli, sürü, kaçan), bir zaafı ve bir "anlatı kancası" vardır. Bosslar faz geçişinde yeni yetenek açar ve kısa bir anlatı anı tetikler. Zekalı düşmanlar oyuncuyu hatırlar: kaçan bir düşman daha sonra güçlenmiş olarak geri gelebilir.

**Ölüm.** Normal modda Sistem her kademede bir kez **İkinci Şans** verir (bedeli: rastgele bir yeteneğin seviyesi −2 ya da bir unvan kaybı). Sonrasında ölüm son kayda döndürür. Demir Mod'da ölüm kalıcıdır ve oyun o noktadaki dünya durumuyla "Hasat" sonuna bağlanır. Ölümler rastgele savaşlardan değil haddini aşmaktan gelmelidir: oyuncunun çok altındaki düşmanlar **Ezme** moduyla çözülür, gerçek tehlike Dikkat'i yükseltip üst kademe varlıkları kışkırtmaktır. NPC'lerin ölümü her modda kalıcıdır.

## Dinamik hikaye motoru

Hikaye sabit bir dal ağacı değildir; tek bir **Dünya Durumu** üzerinde çalışan olaylardan oluşur. Her seçim durumu değiştirir, bir **Hikaye Yönetmeni** de bu duruma bakarak sıradaki olayı seçer. Böylece yüzlerce dalı elle yazmadan binlerce farklı gidişat oluşur.

&#91;embedded content: hikaye motoru döngüsü · 8 parça\]

NPC gündemleri ve olgunlaşan tohumlar her gün Dünya Durumu'na yazılır; Yönetmen yalnızca bu durumu okuyarak karar verir; oyuncunun okuduğu her metin önceden yazılmış içerikten gelir.

### Dünya Durumu (tek doğruluk kaynağı)

- **Bayraklar:** `deniz_kurtarildi`, `liman_mahallesi_dustu`, `yanki_ile_konusuldu` gibi evet/hayır değerleri.
- **Değişkenler:** `gun`, `hayatta_kalan_orani`, `kapi_taşma_sayisi`, `kadim_tehdit` (0–100).
- **Fraksiyon itibarı:** her fraksiyon için −100…+100, ayrıca fraksiyonlar arası ilişki matrisi.
- **Ahlak eksenleri** (oyuncunun eylemleriyle kayar, −100…+100):
  - Merhamet ↔ Acımasızlık
  - Düzen ↔ Özgürlük
  - İnsanlık ↔ Güç (Sistem'e uyum)
- **Bölge durumları:** her bölgenin kontrolü, tehlike seviyesi, açık kapıları.
- **Tohumlar:** ertelenmiş sonuçlar kuyruğu (aşağıda).

### Olay yapısı

Her olay bir veri dosyasıdır: ön koşullar, ağırlık, etiketler, metin ve seçenekler. Her seçeneğin anında etkileri ve isteğe bağlı tohumları vardır. Seçenekler koşullu olabilir: belirli bir stat, yetenek, unvan, eşya ya da ilişki eşiği yeni seçenek açar ("\[ALG 30\] Yalan söylediğini fark et").

### Tohum ve hasat (ertelenmiş sonuçlar)

Büyük seçimler anında değil, sonradan çarpar. Bir seçim kuyruğa bir tohum bırakır: "12–20 gün sonra, şu koşul hâlâ doğruysa, şu olayı tetikle". Örnek: öğreticide yaralı bir yabancıyı bırakıp kaçarsan, Gün 40'ta o kişi Yükselenler'in komutanı olarak karşına çıkar ve seni hatırlar.

### Hikaye Yönetmeni

Yönetmen her adımda uygun olayları filtreler, sonra puanlar:

```latex
Puan = Agirlik \cdot Gerilim_{uyumu} \cdot Tazelik \cdot Kisisel_{bag} \cdot Oncelik_{anayay}
```

- **Gerilim uyumu:** hedef gerilim eğrisine (yükselen, zirve, nefes alma) uyan olaylar öne çıkar; ana gerilim kaynağı Anomali Dikkati'dir. Üç ağır savaştan sonra bir kamp ateşi sahnesi gelir.
- **Tazelik:** yakın zamanda görülen etiketler cezalandırılır.
- **Kişisel bağ:** oyuncunun ilişkisi yüksek NPC'leri içeren olaylar öne çıkar; böylece hikaye oyuncunun önemsediği kişiler etrafında döner.
- **Ana yay önceliği:** zorunlu hikaye düğümleri zamanı gelince her zaman kazanır. Bölüm yapısında ana yay puanlanmaz;
  bölüm tablosu yönetir (aşağıda).

### Bölüm yapısı: üs ve görev

Ana hikaye kanıtlanmış bir yapıya oturur (ayrıntı `docs/CHAPTERS.md`, kararlar DECISIONS #130–#139):

- **Omurga (dallan ve birleş):** 17 bölüm + final (Perde 1'de 3, Perde 2 ve 3'te 6'şar, Perde 4'te 2). Her bölüm herkesin
  geçtiği bir **darboğaz** sahnesiyle biter; içeriği dünya durumuna göre değişir (kim hayatta, kim küs, hangi kamp önde).
- **Takvim (Persona):** her bölümün bir ana operasyonu ve son tarihi vardır. O güne kadarki günler serbesttir; operasyona
  girme zamanını oyuncu seçer (dünya olayları tarihlidir: gemi iner, istila başlar). Son tarih kaçarsa oyun bitmez; taşma
  yaşanır (tehlike, hayatta kalan oranı, kalıcı bayrak) ve operasyon yine başlar.
- **Döngü (Mass Effect 2):** **Üs** (Perde 1'de Öğretici kampı, sonra Sığınak): karakter sahneleri, tohum dönüşleri, NPC
  eşikleri, Küresel Sıralama duyuruları, yoldaş tepkileri ve fırsat eli yalnız burada. **Görev:** kapalı bir sahne dizisi;
  bir bölgeye ilk gidişte yolculuk ve varış sahnesiyle açılır, sonraki gidişlerde tek satırlık yolculuk bildirimiyle (DECISIONS #161); yolculuk eylem harcamaz (#162); içindeyken yalnız göreve ait olaylar gelir. Her olayın konumu vardır ve başka yerde
  tetiklenmez; başka bölgede geçen olay üste görev kartı olur.
- **Bölüm ritmi:** Brifing (yeni tehdit, son tarih, "önceki bölümde") → Hazırlık → Ana operasyon → Sonrası → Darboğaz.
  OP MC döngüsüyle eşleşir: brifing yeni tavan, hazırlık hakimiyet, operasyon mücadele ve kırılma.
- **Tarihli operasyonlar** (dünya olayları) erken girilemez; hedef satırı geri sayar ("Gemi inişine 6 gün"). Serbest
  günlerde **hazırlık** yapılır (keşif, tahkimat, müttefik toplama): her biri bir bayrak bırakır ve operasyonu değiştirir
  (daha az kayıp, ek seçenek ya da avantajlı başlangıç). Yan görev operasyon gününde kesilmez; operasyondan önce oyun
  ayrı bir yuvaya kaydedilir.
- **Operasyon onayı:** erken girilebilen operasyonun düğmesi bir onay penceresi açar: tehdit ve oyuncu seviyesi, hazırlıkların durumu, yoldaş görüşleri; fark üç seviye ve üstündeyse Sistem uyarısı. Açılış gününde ve büyük farkta düğme vurgusuzdur. Bölüm erken biter ve üste yapılacak bir şey kalmazsa kalan günler sonraki bölümün açılışına kadar kendiliğinden geçer (DECISIONS #158).
- **Antrenman / zaman atlama** (Perde 2'den itibaren): üste boş kalan günler bir odakla geçirilebilir: bir stat, kapı temizliği, meslek ya da
  bir yoldaşla vakit. Oyuncu 1 günden (hazırlıkta) operasyona 2 gün kalana kadar dilediği kadar gün seçer; sonrasında
  darboğaz gününe, bölüm bittiyse sonraki bölümün açılışına kadar. Günler tek tek geçer (NPC'ler, kapılar, son tarih).
  Kazanç Günlük Görevden ve normal oynanıştan azdır: 3 günde 1 stat (Günlük Görev günde 1 serbest puan); günde seviye
  XP'sinin Perde 1'de %25'i, Perde 2'de %50'si, Perde 3–4'te %100'ü (normal oynanış her perdede daha
  hızlı); kapıda tehlike −3/gün ve 3 günde 1 Öz Kristali; (meslek odağı Paket 4 ile kapalı); yoldaşla güven +1/gün (en çok +6)
  ve 2 günde 1 bağlılık (en çok +2). Atlanan günlerin Günlük Görevi
  cezasızdır (seri ne büyür ne kopar). %25 olasılıkla bir havuz olayı araya girer; yeni bir tohumun vakti gelince ya da
  ana yay düğümü uyunca atlama durur. Kesintisiz atlama odağın kısa özet sahnesiyle biter.
- Arayüzde her zaman "Bölüm N · Hedef · Kalan gün" satırı görünür; tohumdan doğan her olay bağlantıyı hatırlatan bir
  cümleyle başlar; uzun aradan sonra açılan oyunda kısa bir dönüş özeti gösterilir.

## Dinamik karakterler

NPC'ler senaryo kuklası değil, kendi hedefleri, korkuları ve hafızası olan ajanlardır. Oyuncu ne yaparsa yapsın her NPC oyun günü başına bir adım kendi gündemini ilerletir; ölüm, ihanet ve fraksiyon değiştirme tamamen kalıcıdır.

### NPC modeli

- **Kişilik:** 5 özellik, 0–10 arası — Cesaret, Hırs, Vicdan, Sadakat, Şüphecilik.
- **Değerler:** ahlak eksenlerinde nerede durduğu. Oyuncunun eylemleri bu değerlere göre yorumlanır: Vicdanı yüksek bir NPC, oyuncunun bir esiri öldürdüğünü görürse güveni düşer; hırsı yüksek biri aynı şeye saygı duyar.
- **İlişki eksenleri** (−100…+100): Güven, Saygı, Korku, Bağlılık. Tek bir "dostluk" puanı yerine dört eksen, "senden korkuyor ama sana saygı duyuyor" gibi zengin durumlar üretir.
- **Hedefler:** öncelikli bir liste ("kız kardeşini bul", "Lonca'nın liderliğini al"). Hedef tamamlanınca ya da imkansızlaşınca yenisi seçilir.
- **Hafıza:** oyuncuyla ilgili önemli olayların listesi, her biri bir duygusal ağırlıkla. Küçük olaylar zamanla solar, büyükler (hayat kurtarmak, ihanet) kalıcıdır. Diyaloglar ve olay koşulları bu hafızadan beslenir.
- **Kendi gelişimi:** NPC'ler de seviye atlar, yetenek kazanır, kademe evrimi geçirir. Oyuncu bir NPC'yi ihmal ederse o NPC başka bir yoldan güçlenir.

### Eşik olayları

| Koşul | Sonuç |
| --- | --- |
| Güven < −40 ve Hırs ≥ 7 | İhanet tohumu ekilir; 5–15 gün içinde ihanet olayı |
| Bağlılık ≥ 70 | Kişisel görev açılır; bitince ortak yetenek ya da unvan |
| Korku ≥ 60 ve Cesaret ≤ 3 | Kaçar ya da oyuncuya körü körüne itaat eder |
| Saygı ≥ 60, Güven ≤ 0 | Rakip olur: oyuncuyu düelloya çağırır |
| Değerler ile oyuncu ekseni arası fark > 120 | Fraksiyon değiştirir ya da gruptan ayrılır |

### Ana karakterler

| Karakter | Rol | Olası kaderler |
| --- | --- | --- |
| Deniz Arman | Öğreticide birlikte uyandığın eski arkadaş; Koruma eğilimli | Sadık yoldaş · Yükselenler'e katılan trajik rakip · Oyuncuyu kurtarmak için ölen kahraman |
| Kaya Demir | Demir Lonca lideri, eski asker | Akıl hocası · Oyuncunun devirdiği diktatör · Son savaşta müttefik general |
| Elif Sarı | Sığınak Konseyi'nin hekim lideri | Konsey başkanı ve barış sonu anahtarı · Lonca darbesinde ölür · Oyuncuyla aşk yayı |
| Vahit | Yükselenler'in peygamberi; Sistem'le konuştuğunu söyler | Sahtekar çıkar · Gerçekten haklı çıkar · Oyuncunun ellerinde Yükseliş sonunun kapısı |
| Yankı | Sistem'in hatalı bir parçası; Entegrasyon Günü oyuncuya bağlanır ve ona ayrıcalıklı, bozuk bir Sistem verir; yalnızca oyuncuya fısıldayan ses. Oyuncu onu Bölüm 7'de susturabilir; susturulmuş ya da silinmiş Yankı hiçbir sahnede konuşmaz (DECISIONS #151) | Sistem'in kaynağını açan rehber · Oyuncuyu ele geçirmeye çalışan virüs · Susturulur · Silinir |
| Aras Kurt | Oyuncuyla aynı hızda yükselen gizemli rakip | Ebedi rakip · Çok-evren yoldaşı · Oyuncu yerine "S kademe insan" olan kişi |
| Mert Kılıç | Sessiz Ağ'ın araştırmacısı | Kod Kırıcı sonunun anahtarı · Kadimler'e satılan muhbir |
| Elçi Thessar | Kadimler'in Dünya elçisi | Vassal sonu ortağı · Kendi ırkına ihanet eden müttefik · Son boss |

Her ana karakterin 3–5 "yay düğümü" vardır; hangi düğümlerin tetikleneceği ilişki eksenlerine ve dünya durumuna bağlıdır. İkincil NPC'ler (tüccar, kasaba muhtarı, kapı avcıları) aynı modeli daha basit şekilde kullanır ve prosedürel üretilebilir.

## Yapı: perdeler ve sonlar

Oyun dört perdeden oluşur ve Gün 365'te bir sona bağlanır. Son, tek bir seçimle değil, oyun boyunca biriken dünya durumuyla belirlenir; son perdedeki büyük karar yalnızca uygun sonlar arasında seçim yaptırır.

### Perdeler

| Perde | Gün | Kademe | Ana düğümler |
| --- | --- | --- | --- |
| 1 — Öğretici | 0–30 | F | Uyanış, ilk öldürme, Seviye 10 sınıf teklifi, Deniz ile karşılaşma, Öğretici bossu, ilk "İlk" unvanı |
| 2 — Entegre Dünya | 31–180 | E–C | Dönüş, fraksiyon seçimi, Kasaba Çekirdeği, ilk kapı taşması, Yankı'nın ortaya çıkışı, Sistem gerçeğinin ilk parçası |
| 3 — Çok-evren | 181–330 | C–A | Kadim elçisi, Kule 50+, çok-evren seyahati, Yol oluşumu, büyük ihanet olayı |
| 4 — Hasat | 331–365 | A–S | Fraksiyonların son pozisyonu, Kadim istilası, Sistem'in çekirdeği, son karar |

Perde geçişleri zaman ve olay temellidir: oyuncu yavaş ilerlerse dünya beklemez, Gün 180 gelince Perde 3 olayları zaten başlar.

### Sonlar

| Son | Özet | Ana koşullar |
| --- | --- | --- |
| Koruyucu | Dünya tam Katman olarak kabul edilir, oyuncu insanlığın kalkanı olur | Merhamet ≥ 40, Sığınak itibarı ≥ 50, hayatta kalan oranı ≥ %40, Kadimler yenilmiş |
| Demir Taht | Oyuncu Dünya'nın mutlak hükümdarı olur | Düzen ≥ 50, Güç ≥ 30, Lonca kontrolü veya Kaya'nın devrilmesi |
| Yükseliş | Oyuncu Sistem'le birleşip tanrılaşır, insanlık yakıt olur | Güç ≥ 70, Yükselenler zinciri tamam, Vahit hayatta |
| Kod Kırıcı | Oyuncu Sistem'i yeniden yazar, kuralları kendisi belirler | Sessiz Ağ zinciri tamam, Yankı güveni ≥ 60, Mert hayatta |
| Özgürlük | Sistem yok edilir; tüm seviyeler sıfırlanır, insanlık zayıf ama özgür | Özgürlük ≥ 60, İnsanlık ≥ 50, Sistem çekirdeğine ulaşılmış |
| Vassal | Kadimlerle anlaşma: Dünya hayatta ama boyunduruk altında | Thessar ile Bağlılık ≥ 50, kadim\_tehdit ≥ 70 |
| Yalnız Avcı | Tüm yoldaşlar ölmüş ya da gitmiş; oyuncu çok-evrene tek başına çıkar | Aktif yoldaş = 0, oyuncu S kademe, hiçbir fraksiyona katılmamış (`uye_yok`) |
| Hasat | Kötü son: insanlık yeterince güçlenemez, Dünya hasat edilir | Gün 365'te hiçbir son koşulu sağlanmamış ya da Demir Mod ölümü |
| Döngü (gizli) | Oyuncu Sistem'in bir döngü olduğunu öğrenir ve yeni bir Entegrasyon başlatır; New Game+ açılır | Tüm Kule katları + Yankı'nın son sırrı + en az bir önceki bitirilmiş oyun |

**Son anlatımı.** Son ekranı yalnızca kahramanı değil, dünyayı anlatır: her ana NPC'nin akıbeti, her fraksiyonun durumu, oyuncunun en kritik 5 seçimi ve bunların uzun vadeli etkisi (Fallout tarzı epilog kartları).

## Arayüz ve oyuncu deneyimi

Oyun Android telefonda dikey ekranda, tek elle oynanacak şekilde tasarlanır ve bir LitRPG romanının sayfasını okuyormuş hissi verir: anlatı düzyazı olarak akar, Sistem bildirimleri ise kendi kartlarında renk, titreşim ve kısa animasyonla gelir. Tablette metin sütunu ortalanır, yan panelde durum özeti açık kalır.

**Görsel kimlik: "Kırık pencere".** Sistem kusursuz bir cam penceredir: kesik köşeler, tek bir buz mavisi, soluk tarama çizgileri; başlıklar ve sayılar köşeli bir yazıyla, Sistem'in sesi tek aralıklı yazıyla, anlatı uzun okuma için yapılmış bir serifle yazılır. Kahraman bu pencerenin ölçemediği bir hatadır ve Yankı o hatanın sesidir: Yankı'nın satırları pencerenin içinde değil, ters renkli ve kaymış bir şerit olarak araya sızar. Büyük anların her biri kendi ekranındadır: seviye atlarken pencere açılır ve sayı sayar, evrimde yeni kademe harfi eskisinin yerine oturur, Benzersiz yetenekte pencere beyaz-altına döner, Küresel Sıralama'da eski sıranın üstü çizilir. Sınır kırıldığında (gerçek seviye açığa çıktığında ya da bir savaş Ezmeyle bittiğinde) ekran bir an çöker ve Sistem `görünen` ile `gerçek` arasındaki farkı yazmak zorunda kalır. Koyu temanın zemini saf siyahtır.

**Örnek ekran (telefon, Hikaye sekmesi)**

```text
┌──────────────────────────────────┐
│ Gün 14 · Harabe Çarşı        ☰   │
│ HP ████████░░ 182/240            │
│ MP ██████░░░░  60/95             │
├──────────────────────────────────┤
│ Yıkık kubbenin altından ince bir │
│ tıkırtı geliyor. Deniz kalkanını │
│ kaldırıyor, ama sen tıkırtının   │
│ tavandan geldiğini fark ediyorsun│
│                                  │
│ ┌─ SİSTEM ─────────────────────┐ │
│ │ [Algı] kontrolü başarılı.    │ │
│ │ Gizli düşman: Kubbe Örümceği │ │
│ │ Sv. 17 · Nadir · Zaaf: Ateş  │ │
│ └──────────────────────────────┘ │
├──────────────────────────────────┤
│ [ Deniz'i uyar, geri çekil     ] │
│ [ Alev Oku ile tavanı hedefle  ] │
│ [ ALG 25 · Taşıyıcı ipi kes    ] │
│ [ Sessiz kal, Deniz'i yem yap  ] │
├──────────────────────────────────┤
│ Hikaye  Durum  Yetenek  Çanta  ⋯ │
└──────────────────────────────────┘
```

**Sistem bildirim türleri:** seviye atlama, yetenek kazanımı/evrimi, unvan, görev, uyarı, gizli koşul ipucu ve Yankı'nın "bozuk" bildirimleri (glitchli yazı, karışık harfler). Nadirlik renkleri: gri, yeşil, mavi, mor, turuncu, kırmızı, altın. Önemli anlarda kısa titreşim ve isteğe bağlı ses efekti çalar.

**Gezinme.** Terminal komutları yerine alt gezinme çubuğu kullanılır: Hikaye · Durum (statlar, kademe, unvanlar, Öz) · Yetenekler · Çanta (envanter, ekipman) · Daha fazla (harita, Günlük Görev, ilişkiler, görevler, kayıt, ayarlar). Savaşta seçenek listesinin yerini eylem ızgarası (Saldır, Yetenek, Savun, Analiz, Ortam, Konuş, Kaç) ve hedef seçimi alır. Bir yeteneğe ya da düşmana uzun basmak ayrıntı kartını açar.

**Hız, ritim ve erişilebilirlik.** Daktilo efekti açılıp kapanabilir; ekrana dokunmak metni anında tamamlar. Seviye atlama ve evrim anlarında tam ekran kart ve kısa animasyon kullanılır. Uzun savaşlar için "otomatik tur" vardır. Yazı boyutu, açık/koyu tema, yüksek kontrast ve TalkBack (ekran okuyucu) desteklenir.

**Dil desteği (Türkçe / İngilizce).** Oyun iki dilde de baştan sona oynanabilir; iki sürüm aynı içeriği, aynı seçimleri ve aynı sonları paylaşır.

- Dil ilk açılışta cihaz diline göre seçilir, Ayarlar'dan oyun ortasında değiştirilir. Kayıt dosyası dilden bağımsızdır, çünkü dünya durumu yalnızca kimlik (id) tutar.
- Arayüz metinleri Android kaynaklarındadır (`values/strings.xml` İngilizce, `values-tr/strings.xml` Türkçe); hikaye içeriğinde her metin alanı `{tr, en}` olarak tutulur.
- Stat kısaltmaları dile göre gösterilir: GÜÇ/STR, ÇEV/AGI, DAY/END, ZEK/INT, BİL/WIS, ALG/PER, İRD/WIL, KAR/CHA.
- **İngilizce sürüm çeviri değil, Kuzey Amerika için tam kültürel yerelleştirmedir.** Kişi, mekan ve fraksiyon adları, ölçü birimleri (km → mil, kg → lb, °C → °F), mizah, deyimler ve kültürel referanslar değişir. Hikaye, seçimler ve mekanikler aynı kalır.
- İçerik metinleri isim yazmaz, belirteç kullanır (`{npc:deniz_arman}`); böylece aynı olay Türkçede "Deniz", İngilizcede "Devon" der.
- İngilizce metin türün doğal İngilizcesiyle yazılır; Sistem bildirimleri köşeli parantezli LitRPG üslubunu korur ("\[Skill evolved: Shadow Step → Void Step\]").
- Türkçe ekler (Deniz'e, Kaya'nın, Yankı'yla) ünlü uyumuna göre eki seçen bir yardımcıyla üretilir; İngilizce için a/an ve çoğul yardımcıları vardır.

**Yerelleştirme eşleme tablosu (TR → EN, Kuzey Amerika).** Tam liste İngilizce sekmededir; ana isimler:

| Tür | Türkçe | İngilizce (Kuzey Amerika) |
| --- | --- | --- |
| Oyun adı | Yükseliş Protokolü | Ascension Protocol |
| Şehir | Kıyıkent (Ege kıyısı, kurgusal) | Port Harlow (Pasifik Kuzeybatı, kurgusal) |
| Karakter | Deniz Arman | Devon Hale |
| Karakter | Kaya Demir | Marcus Stone |
| Karakter | Elif Sarı | Ellie Sawyer |
| Karakter | Vahit | Brother Vance |
| Karakter | Aras Kurt | Ash Wolfe |
| Karakter | Mert Kılıç | Miles Kincaid |
| Karakter | Yankı | Echo |
| Karakter | Elçi Thessar | Envoy Thessar (yabancı ırk, değişmez) |
| Fraksiyon | Sığınak Konseyi · Demir Lonca · Yükselenler · Sessiz Ağ · Kadimler | Haven Council · Iron Guild · The Ascended · The Quiet Network · The Ancients |
| Mekan | Öğretici Katman · Harabe Çarşı · Kasaba Çekirdeği · Kule | Tutorial Layer · Dead Mall · Town Core · The Spire |
| Kavram | Sistem · Kademe · Öz · Yol | the System · Grade · Essence · Path |
| Kavram | Günlük Görev · İkinci Şans · Demir Mod | Daily Quest · Second Chance · Ironman Mode |
| Birim | 10 km koşu · 100 şınav | 6-mile run · 100 push-ups |

**Kayıt.** Her oyun günü sonunda ve uygulama arka plana alındığında otomatik kayıt + 3 manuel slot; Demir Mod'da tek otomatik slot. Kayıt, uygulamanın kendi depolamasında JSON olarak tutulur ve Android otomatik yedeklemesiyle Google hesabına yedeklenir. Dosya tüm dünya durumunu, tohum kuyruğunu, NPC hafızalarını ve RNG tohumunu içerir; aynı kayıt aynı sonucu üretir.

## Teknik mimari ve veri şeması

Öneri: **Kotlin + Jetpack Compose** (yerel Android arayüzü, Material 3) + **kotlinx.serialization** ve **kaml** (kayıt ve YAML içerik) + **JUnit 5** (testler). Minimum Android 8.0 (API 26), hedef Google Play'in istediği güncel API seviyesi. Oyun motoru Android'den bağımsız, saf bir Kotlin modülüdür; Compose yalnızca onu gösterir. Kod ile içerik ayrıdır: yeni olay, NPC, yetenek ya da düşman eklemek kod yazmadan YAML dosyası eklemekle olur.

**Klasör yapısı**

```text
yukselis-protokolu/
├── CLAUDE.md                  # Claude Code için kalıcı proje kuralları
├── docs/GDD.md                # Türkçe tasarım dokümanı (ana kaynak)
├── docs/GDD.en.md             # İngilizce (Kuzey Amerika) tasarım dokümanı
├── engine/                    # saf Kotlin modülü, Android bağımlılığı yok
│   └── src/main/kotlin/…/     # core/ (state, rng, clock, save) · system/ · combat/
│                              # story/ · npc/ · i18n/
├── app/                       # Android uygulaması (Jetpack Compose)
│   └── src/main/
│       ├── kotlin/…/ui/       # story/ status/ skills/ inventory/ combat/ settings/ + ViewModel'ler
│       ├── res/values/        # strings.xml (İngilizce)
│       └── res/values-tr/     # strings.xml (Türkçe)
├── content/                   # events/ npcs/ skills/ classes/ titles/ enemies/ items/ regions/
│                              # (her metin alanı tr + en; derlemede doğrulanıp assets'e kopyalanır)
└── tools/simulator/           # otomatik oynanış ve denge simülatörü (JVM)
```

**Örnek olay dosyası**

```yaml
id: ogretici_yarali_yabanci
perde: 1
on_kosullar:
  gun: { min: 3, max: 20 }
  bolge: harabe_carsi
  bayrak_yok: [yabanci_karsilasildi]
agirlik: 5
etiketler: [ahlak, karsilasma, gerilim_orta]
metin:
  tr: "Çöken bir duvarın altından bir el uzanıyor. {npc:deniz_arman} sana dönüyor: 'Sürü geliyor!'"
  en: "A hand claws out from under a collapsed wall. {npc:deniz_arman} spins toward you: 'The pack's coming!'"
secenekler:
  - metin:
      tr: Duvarı kaldırıp yabancıyı kurtar
      en: Lift the wall and pull the stranger free
    kosul: { stat: { str: 18 } }
    etkiler:
      ahlak: { merhamet: +10 }
      bayrak: [yabanci_kurtarildi]
      npc_olustur: { sablon: yabanci_kapi_avcisi, iliski: { guven: 30 } }
  - metin:
      tr: Onu bırak ve kaç
      en: Leave them and run
    etkiler:
      ahlak: { merhamet: -15 }
      bayrak: [yabanci_birakildi]
    tohum:
      olay: yukselen_komutan_intikam
      gecikme_gun: { min: 12, max: 20 }
      kosul: { bayrak: [yabanci_birakildi] }
```

**Örnek NPC dosyası**

```yaml
id: deniz_arman
ad: { tr: Deniz Arman, en: Devon Hale }
kisa_ad: { tr: Deniz, en: Devon }
kisilik: { cesaret: 7, hirs: 4, vicdan: 8, sadakat: 9, supheci: 3 }
degerler: { merhamet: 60, duzen: 10, insanlik: 70 }
baslangic_iliski: { guven: 50, saygi: 20, korku: 0, baglilik: 40 }
hedefler:
  - { id: kardesini_bul, oncelik: 10 }
  - { id: oyuncuyu_koru, oncelik: 7 }
egilim: { koruma: 8, yakin_dovus: 5 }
yay_dugumleri: [deniz_ilk_kavga, deniz_kardes_ipucu, deniz_yukselenler_teklifi, deniz_son_secim]
```

**Mimari kurallar**

- Oyun motoru (`engine`) saf Kotlin'dir ve Android'e bağımlı değildir: JUnit ile bilgisayarda test edilir, ileride iOS ya da PC'ye taşınabilir.
- Tüm rastgelelik tek, tohumlanmış bir RNG'den geçer; testler ve kayıtlar tekrarlanabilir olur.
- Dünya durumu tek, değişmez bir veri sınıfıdır; olaylar onu yalnızca etki fonksiyonlarıyla değiştirir. Arayüz bu durumu bir ViewModel üzerinden StateFlow olarak dinler.
- Koşul dili (`on_kosullar`, `kosul`) küçük, güvenli bir DSL'dir; betik çalıştırma yoktur.
- İçerik metinleri isim yazmaz, belirteç kullanır: `{npc:deniz_arman}`, `{yer:harabe_carsi}`, Türkçe ek için `{npc:deniz_arman|e}` → "Deniz'e" (İngilizcede "Devon").
- `validateContent` Gradle görevi derlemeden önce tüm YAML dosyalarını kontrol eder: tanımsız bayrak, ulaşılamayan olay, bozuk referans, tanımsız isim belirteci, eksik ya da boş çeviri.

**Çok dil mimarisi.** Arayüz metinleri Android kaynaklarındadır (`values/strings.xml`, `values-tr/strings.xml`); içerikte oyuncuya görünen her alan bir `{tr, en}` sözlüğüdür. `t()` yardımcısı seçili dildeki metni döndürür, eksik çeviride diğer dile düşer ve uyarı yazar. Dünya durumu, kayıt, koşullar ve olay mantığı dilden tamamen bağımsızdır; stat ve kavram kimlikleri koddaki İngilizce adlardır (`str`, `agi`, `essence`).

## Geliştirme yol haritası ve Claude Code komutu

Oyunu tek seferde değil, 9 aşamada ve her aşamada oynanabilir bir sürümle geliştir. En önemli kilometre taşı Aşama 6: Öğretici Katman'ın baştan sona oynanabilir olduğu dikey dilim. Mekanikler orada test edilmeden diğer perdelerin içeriğine geçme.

**Nasıl kullanılır**

1. Bilgisayarına Android Studio'yu (Android SDK ve emülatör onunla gelir) ve Claude Code'u kur.
2. Bu dokümanın Türkçe sekmesini `docs/GDD.md`, İngilizce sekmesini `docs/GDD.en.md` adıyla Markdown olarak dışa aktar ve boş bir proje klasörüne koy.
3. Klasörde `git init` çalıştır, Claude Code'u başlat ve aşağıdaki ana komutu yapıştır. Önce plan sunacak; onayla.
4. Her aşama bitince uygulamayı emülatörde ya da USB hata ayıklama ile kendi telefonunda dene, notlarını ver, sonra bir sonraki aşama komutuna geç. Aşamalar arasında `/clear` ile bağlamı temizlemek uzun projede kaliteyi korur; kalıcı bilgi CLAUDE.md ve docs/ içinde durur.

**Ana komut (ilk mesaj)**

```text
Sen deneyimli bir Android oyun geliştiricisi ve anlatı tasarımcısısın. Bu repoda "Yükseliş Protokolü" (İngilizce adı: Ascension Protocol) adlı, Android için, tek oyunculu, metin tabanlı bir LitRPG oyunu geliştireceğiz. Defiance of the Fall, He Who Fights With Monsters, The Primal Hunter ve Solo Leveling tarzı mekanikler kullanıyoruz ama hikaye ve karakterler tamamen özgün. Tasarımın tamamı docs/GDD.md (Türkçe, ana kaynak) ve docs/GDD.en.md (Kuzey Amerika için yerelleştirilmiş İngilizce sürüm) dosyalarında. Her işe başlamadan önce ikisini de oku ve tasarıma sadık kal. Mekanik ya da hikaye çelişkisinde docs/GDD.md, İngilizce isim ve terimlerde docs/GDD.en.md geçerlidir.

TEMEL HEDEF: Her seçim hikayenin gidişatını ve sonunu değiştirebilmeli. Hikaye sabit bir dal ağacı değil; tek bir Dünya Durumu, koşullu olaylar, ertelenmiş sonuçlar (tohumlar), bir Hikaye Yönetmeni ve kendi hedefleri olan NPC'lerle dinamik olarak oluşmalı.

TEKNİK KURALLAR
- Kotlin, Gradle (Kotlin DSL), Jetpack Compose + Material 3, kotlinx.serialization, kaml (YAML), kotlinx.coroutines, JUnit 5. Minimum Android 8.0 (API 26), hedef Google Play'in istediği güncel API seviyesi.
- İki ana Gradle modülü: engine (saf Kotlin, Android bağımlılığı yok, tüm oyun mantığı) ve app (Compose arayüzü, ViewModel'ler, kayıt dosyası erişimi). Arayüz motorun durumunu StateFlow ile dinlesin; oyun mantığı asla app modülüne sızmasın.
- Kod ve içerik ayrı: olaylar, NPC'ler, yetenekler, sınıflar, unvanlar, düşmanlar, eşyalar ve bölgeler content/ altında YAML; derlemede doğrulanıp assets'e kopyalansın. Koda hikaye metni gömme.
- Tüm rastgelelik tek, tohumlanmış bir RNG'den geçsin. Dünya durumu tek, değişmez bir veri sınıfı olsun; yalnızca etki fonksiyonlarıyla değişsin. Koşullar için küçük ve güvenli bir DSL yaz, betik çalıştırma kullanma.
- İKİ DİL: Türkçe ve İngilizce. Arayüz metinleri res/values/strings.xml (İngilizce) ve res/values-tr/strings.xml (Türkçe). İçerikte oyuncuya görünen her alan {tr, en} sözlüğü. Dil ilk açılışta cihaz diline göre seçilsin, Ayarlar'dan oyun ortasında değişsin; kayıt dosyası dilden bağımsız olsun.
- İngilizce sürüm çeviri değil, Kuzey Amerika için tam kültürel yerelleştirme: kişi, mekan ve fraksiyon adları, birimler (mil, lb, °F), mizah, deyimler ve kültürel referanslar değişir; mekanikler ve hikaye aynı kalır. Eşlemeler GDD'deki yerelleştirme tablosunda ve docs/GDD.en.md'de.
- İçerik metinleri asla isim yazmasın, belirteç kullansın: {npc:deniz_arman}, {yer:harabe_carsi}, Türkçe ek için {npc:deniz_arman|e}. Belirteçleri seçili dile göre çözen ve Türkçe ünlü uyumuna göre eki seçen bir yardımcı yaz; İngilizce için a/an ve çoğul yardımcıları ekle.
- Klasör yapısı GDD'deki "Teknik mimari" bölümündeki gibi olsun.
- Belirsiz bir tasarım kararında GDD'ye en uygun varsayımı yap ve docs/DECISIONS.md dosyasına kısaca yaz; beni gereksiz sorularla durdurma.
- Her aşamanın sonunda: ./gradlew test ve ./gradlew assembleDebug geçsin, uygulama emülatörde açılsın, CLAUDE.md'yi güncelle (mimari, komutlar, kurallar), anlamlı bir git commit at ve telefonda neyi test etmem gerektiğini 5 maddede söyle.

AŞAMALAR
1. Çekirdek: Gradle projesi (engine + app), CLAUDE.md, dünya durumu modeli, RNG, oyun saati (gün/perde), kayıt/yükleme (uygulama depolamasında JSON, 3 slot + otomatik kayıt, arka plana alınınca kayıt, Android otomatik yedekleme, Demir Mod), i18n altyapısı (strings.xml, {tr, en} içerik, isim belirteçleri, ek yardımcıları, dil seçimi), Compose iskeleti: Hikaye ekranı (akan metin, daktilo efekti, dokununca tamamlama, seçenek butonları), Sistem bildirim kartları ve nadirlik renkleri, alt gezinme (Hikaye, Durum, Yetenekler, Çanta, Daha fazla), açık/koyu tema.
2. Sistem mekanikleri: 8 stat ve türetilmiş değerler, XP/seviye formülleri, F→S kademeleri ve Evrim Görevleri, gizli Eğilim vektörü, Seviye 10 sınıf teklifi (2 eğilim + 1 gizli sınıf), Meslek ağacı, yetenekler (nadirlik, kullanımla seviye, dallı evrim, kaynaştırma, ceza yetenekleri), unvanlar ve "İlk" unvanları, Öz ve Yol, Günlük Görev ve ceza. Durum ve Yetenekler ekranları; seviye atlama ve evrim için tam ekran kart, animasyon ve titreşim.
3. Savaş: tur akışı, GDD'deki inisiyatif/isabet/hasar/kritik formülleri, kademe farkı çarpanı, durum etkileri, Analiz/Ortam/Konuş/Kaç eylemleri, düşman davranış kalıpları, boss fazları, kaçan düşmanların geri dönmesi, İkinci Şans. Savaş ekranı: eylem ızgarası, hedef seçimi, uzun basınca ayrıntı kartı.
4. Hikaye motoru: olay yükleyici, koşul DSL'i, koşullu seçenekler ([ALG 25] gibi), etkiler, tohum kuyruğu, Hikaye Yönetmeni puanlaması (gerilim eğrisi, tazelik, kişisel bağ, ana yay önceliği), fraksiyon itibarı ve fraksiyonlar arası ilişki matrisi, 3 ahlak ekseni, bölge durumları ve kapı taşmaları, validateContent Gradle görevi (tanımsız bayrak, ulaşılamayan olay, bozuk referans, tanımsız isim belirteci, eksik ya da boş çeviri).
5. NPC sistemi: kişilik, değerler, 4 ilişki ekseni (Güven, Saygı, Korku, Bağlılık), solan hafıza, hedefler ve günlük gündem simülasyonu, GDD'deki eşik olayları (ihanet, kişisel görev, rakiplik, ayrılma), NPC seviye atlama, kalıcı ölüm. İlişkiler ekranı.
6. Dikey dilim, Perde 1 içeriği iki dilde: Öğretici Katman'ın tamamı (Gün 0–30). En az 40 olay, Deniz, Yankı ve Aras'ın ilk yay düğümleri, 15 düşman, 1 boss, 30 yetenek, 8 temel + 4 gizli sınıf, 15 unvan. Oyun telefonda baştan sona, iki dilde de oynanabilir olmalı.
7. Perde 2–4 ve sonlar: kalan fraksiyonlar, karakter yayları, Kule, çok-evren, 8 son + gizli Döngü sonu, son ekranı ve epilog kartları (her NPC'nin akıbeti, fraksiyonlar, en kritik 5 seçim).
9. Denge ve yayın hazırlığı: tools/simulator içinde, engine modülünü kullanarak rastgele ve sezgisel stratejilerle 1000 oynanışı simüle eden bir JVM aracı yaz; son dağılımını, seviye eğrisini ve ölüm oranlarını raporla, dengesizlikleri düzelt. Ardından imzalı release derlemesi (AAB), uygulama simgesi ve Google Play mağaza metinlerini (TR ve EN-NA) hazırla.

ŞİMDİ: docs/GDD.md ve docs/GDD.en.md'yi baştan sona oku. Önce tüm mimarinin ve 9 aşamanın planını çıkar, bana onaylat. Onaydan sonra yalnızca Aşama 1'i uygula ve dur.
```

**Sonraki aşamalar için kısa komut**

```text
docs/GDD.md, docs/GDD.en.md, CLAUDE.md ve docs/DECISIONS.md'yi oku. Aşama <N>'i GDD'ye sadık kalarak uygula. Önce kısa bir plan göster, sonra uygula. Bitince ./gradlew test ve ./gradlew assembleDebug çalıştır, CLAUDE.md'yi güncelle, commit at ve test etmem gereken 5 şeyi söyle. Oyun testimden notlarım: <notların>
```

**İçerik üretimi için komut** (Aşama 6 ve 7'de hız kazandırır)

```text
content/ altındaki şemaya ve GDD'deki karakterlerin kişiliklerine uyarak <bölge/perde> için 10 yeni olay yaz. Her metin alanını hem Türkçe hem İngilizce doldur; İngilizce sürüm çeviri gibi değil, Kuzey Amerika okuyucusu için kendi başına doğal okunsun (birimler, deyimler, kültürel referanslar). Metne isim yazma; GDD'deki {npc:...} ve {yer:...} belirteçlerini kullan. Her olayda en az 3 seçenek olsun; en az biri koşullu (stat, yetenek, unvan ya da ilişki), en az biri bir tohum eken ve en az biri bir NPC ilişki eksenini değiştiren seçenek olsun. Bitince validate komutunu çalıştır ve ulaşılamayan olay kalmadığını doğrula.
```
