# Türkçe Üslup Kılavuzu ve Dil Elden Geçirme Planı

Bu belge iki şey içerir: (1) oyunun Türkçe metinlerinin nasıl yazılacağı, (2) mevcut ~220.000 sözcüklük Türkçe metnin
bu kılavuza göre yeniden yazılması işinin planı ve ilerleme durumu. Yeni bir oturum bu dosyayı okuyarak işe kaldığı
yerden devam edebilmelidir.

## 0. Sorun (kullanıcı geri bildirimi, 2026-11-07)

> "Hikayenin Türkçesi hiç akıcı ve doğal gelmiyor, edebi hissettirmiyor."

Teşhis: Metin dilbilgisi açısından doğru, imgeleri iyi; ama **Türkçe düşünülerek değil, İngilizce bir metnin çevirisi
gibi** kurulmuş. Belirtiler:

1. **İngilizce sözdizimi kalıpları.** "Bu, korkudan çok daha ağır." · "o soru, … kesinleştirdi" (öznenin ardından
   virgül) · "neredeyse komik derecede kolay" (*almost comically easy*) · "etrafında kurmak" (*build around*) ·
   "sana kilitli" (*locked on you*) · "…, aynı anda." (cümle sonuna eklenmiş zarf) · "kuru bir sesle:".
2. **Kesik, İngiliz gerilim romanı ritmi.** "Üç adım. Dört." · "Acelesi yok. Hiç olmadı." · "Sessizlik." Arka arkaya
   sıralanan iki üç sözcüklük cümleler. Türkçe düzyazı ulaçlarla (-ip, -ken, -ince, -dıkça, -erek) bağlanarak akar.
3. **Fiil sonu tekdüzeliği.** Neredeyse her cümle "-yor" ile bitiyor; kulak bunu tekrar olarak duyuyor.
4. **"bir" fazlalığı** (İngilizce *a/an* karşılığı), **noktalı virgül bolluğu**, "Sonra / artık / yalnızca / sadece"
   tekrarı, gereksiz "senin".
5. **Kalıp tekrarı.** Kronik satırlarında aynı iskelet art arda: "Yolunu X'in etrafında kurdun; o günden sonra …".

## 1. Değişmeyecekler (kesin kurallar)

Bu iş **yalnızca Türkçe dil ve üslup** işidir. Hikaye, olaylar, seçimler, mekanik ve İngilizce metin aynen kalır.

- **Yalnızca `tr` değerleri değişir.** `en` metinleri, kimlikler (`id`), anahtarlar, koşullar, etkiler, `sonraki`,
  sıralar ve yorumlar dokunulmadan kalır. Liste öğesi sayısı aynı kalır (her paragrafın kendi `kosul`'u olabilir:
  **paragraf birleştirme, bölme, silme yok**).
- **Anlam ve bilgi korunur.** Bir cümledeki her bilgi (ipucu, sayı, isim, tohum ima eden ayrıntı, sezgi ipucu,
  zarın neyi sınadığı) yeni metinde de olmalı. Bilgi eklenmez; yeni olay, yeni karakter özelliği, yeni dünya bilgisi
  uydurulmaz. Sahnenin duygusu ve gerilim düzeyi aynı kalır.
- **Belirteçler aynen korunur** ve aynı sayıda kalır: `{npc:deniz_arman}`, `{npc:@}`, `{npc_hedef:@}`, `{oyuncu}`,
  `{yer:…}`, `{fraksiyon:…}`, `{dusman}`, Sistem yer tutucuları (`{n}`, `{npc}`, `{grade}` …). Cümle yeniden
  kurulunca **hal eki belirteci değişebilir** (`|e |i |in |de |den |le`): ad artık yönelme hâlindeyse `{npc:x|e}`
  yaz. Belirtecin dışına ek yapıştırma (`{npc:x}'ye` YOK). Savaş metinlerinde dinamik isme ek gelmez; ismi özne yap.
- **Metne düz isim yazılmaz.** "Deniz", "Yankı", "Kıyıkent" yerine belirteç (validator uyarır).
- **Yankı'nın sesi:** kendi sözleri **küçük harfle ve tırnaksız** (fısıltı), anlatı onu `{npc:yanki}` ile atfeder.
  Bozuk harfler (`s̷aklanmayı`, `█████`) olduğu gibi kalır.
- **Sistem sesi:** köşeli parantezli, kuru, resmi, isim cümlesi ağırlıklı ("Hayatta kalma olasılığı: düşük."). Bu ses
  *bilerek* soğuk ve kısadır; anlatı kuralları ona uygulanmaz. Yalnızca Türkçe hataları ve çeviri kokusu düzeltilir.
- **Sezgiler:** stat koşullu satırlar anlatıcının/Sistem'in gözlemidir; `yanki_bagi` gibi koşullu öneksizler Yankı'dır
  (küçük harf).
- **Terimler sabit** (GDD eşleme tablosu): Sistem, Kademe, Öz, Yol, Kapı, Kule, Dikkat, Yozlaşma, Entegrasyon,
  Öğretici Katman, Harabe Çarşı, Kasaba Çekirdeği, Sığınak Konseyi, Demir Lonca, Yükselenler, Sessiz Ağ, Kadimler,
  stat adları ve kısaltmaları (GÜÇ, ÇEV, DAY, ZEK, BİL, ALG, İRD, KAR). Yetenek, eşya, unvan, düşman adları değişmez
  (başka yerde kimlikle eşleşir, arayüzde görünür).
- **YAML güvenliği:** Değerler çift tırnaklı tek satır; içteki tırnak `\"`. Ters bölü, iki nokta vb. bozulmasın.
- **Teknik Türkçe kuralları** (CLAUDE.md): "ve"den önce virgül yok; sayıya ek getirme kalıbı kullanma.

## 2. Anlatıcı ve zaman

- **İkinci tekil şahıs, şimdiki zaman korunur** ("Açıklığa varıyorsun"). Oyun kitaplarının alışılmış sesi ve
  etkileşimin sesi budur. Ama "-yor" tekdüzeliği kırılır:
  - **Durum bildiren -mış/-miş:** "Sis toprağa çökmüş." · "Kapı aralık kalmış." (sahne kurarken çok doğal)
  - **Geniş zaman:** genellemeler, alışkanlıklar, bilgece cümleler: "Kan kokusu yalnız seni çekmez."
  - **İsim cümlesi:** "Ok kılıfı boş." · "Ortalık sessiz."
  - **Ulaçla bağlama:** iki "-yor" cümlesini birine indir: "Kapıyı itip içeri giriyorsun."
  - **Devrik cümle** ara sıra, vurgu için ve konuşmada: "Gelmiyor kimse."
  - Bir paragrafta art arda üç cümle aynı ekle bitmesin.
- **Kronik ve son kartları** geçmiş zaman ("-dın"): her satır başka bir iskeletle kurulur.

## 3. Ritim ve cümle

- Türkçe cümle fiille biter ve yan cümleleri sıfat-fiil / ulaçla önüne alır. İngilizce gibi virgülle uzayan
  ek cümlecikler ("…, eli titreyerek, gözleri yerde.") yerine **yükleme bağlanan** yapılar kur.
- **Kısa cümle yalnızca vurgu için.** Bir sahnede en çok bir iki kez. "Üç adım. Dört." → "Üç adım, dört adım…"
- **Noktalı virgül** paragraf başına en çok bir. Çoğu zaman nokta, "ama", "oysa", "derken", ulaç ya da iki nokta
  daha doğaldır.
- **"Bu, …" ile başlayan açıklama cümlesi** neredeyse her zaman kaldırılabilir ya da önceki cümleye bağlanabilir.
- **"bir"** yalnızca gerçekten "herhangi bir / tek bir" anlamı varsa. "yaşlı bir kadın" doğal; "Bir çocuk sesi." →
  "bir çocuğun sesi"; "Bir çığlık yükseliyor." → "Derken bir çığlık kopuyor."
- **Gereksiz iyelik ve zamir:** "senin", "onun", "kendi" Türkçede ekten zaten anlaşılır. "Senin pencerenin
  kenarları" → "Pencerenin kenarları".
- Tekrar eden bağlaçlar ("Sonra", "Artık", "Yalnızca", "Sadece", "Ama") aynı paragrafta bir kez.

## 4. Sözcük seçimi

- **Canlı ve somut fiil:** "yükseliyor" (çığlık) → "kopuyor"; "bakıyor" yerine yerine göre "süzüyor, dikiliyor,
  gözünü ayırmıyor"; "gidiyor" yerine "uzaklaşıyor, sıvışıyor, yürüyüp gidiyor".
- **Deyim ve ikileme** Türkçenin akışını verir, ölçülü kullan: "ağır ağır", "titreşip duruyor", "ha bire",
  "gözü dönmüş", "can havliyle", "dili tutulmuş". Her paragrafa serpiştirme; yerini bulsun.
- **Ses yansımaları:** gıcırdamak, çıtırdamak, hışırdamak, uğuldamak; İngilizcedeki "a sound of …" kalıbı yerine.
- **Kayıt:** Anlatı çağdaş edebi Türkçe (Sait Faik'in yalınlığı, Yusuf Atılgan'ın gerilimi, Hakan Günday'ın
  sertliği arası). Osmanlıca süs yok; aşırı öztürkçe de yok. Bilim-kurgu/sistem sözcükleri yerinde kalır.
- **Diyalog konuşma Türkçesi:** insanlar "Bana öyle bakma" der, "Bana o şekilde bakmayı bırak" demez. Karakterin
  yaşı, mesleği, sınıfı sesine yansır (bkz. `content/npcs/<id>.yaml` `kisilik`, `degerler` ve GDD karakter bölümü).
  Ağız taklidi yok; "biliyon mu" gibi yazım yok. Hitaplar Türkçe: "abi, hocam, kızım, yavrum, bak hele" vb. yerinde.
- **Seçenek etiketleri:** kısa emir kipi ("Bacağını sar, onu sırtına al"); konuşma seçenekleri tırnak içinde ve
  karakterin ağzına yakışır biçimde.
- **Ansiklopedik metinler** (bestiyer, eşya, yetenek, unvan açıklamaları): geniş zaman, sade, bilgi verici; yine de
  çeviri kokusu ("…'nın bir türüdür") olmasın.

## 5. Çeviri kokusu sözlüğü (kaçınılacak → tercih)

| Kaçın | Tercih |
| --- | --- |
| Bu, X'ten daha ağır. | … X'ten de ağır bir şey. / önceki cümleye bağla |
| neredeyse komik derecede kolay | insanı güldürecek kadar kolay |
| X'in etrafında kurmak | X üzerine kurmak |
| sana kilitli (gözler) | sana dikilmiş |
| …, aynı anda. | hem … hem … / aynı anda … (başa) |
| kuru bir sesle: | kuru kuru: / sesi kupkuru: |
| bir şekilde | nasılsa, bir yolunu bulup |
| X yapmayı seçiyorsun | X'i yapıyorsun / X'e karar veriyorsun |
| … olduğunu fark ediyorsun | … olduğunu görüyorsun / ancak o zaman anlıyorsun |
| hiç olmadı | hiçbir zaman olmamış |
| fazla hızlı (cevap) | biraz fazla çabuk |
| muhtemelen | herhalde / büyük ihtimalle |
| Sadece … | Yalnız … / yalnızca … (dönüşümlü, az) |
| -mak zorunda kalmak (her yerde) | -mak düşüyor, -mak gerekiyor, yerine göre |
| o, … (özne + virgül) | virgülü kaldır, yüklemi yaklaştır |
| tebrikler. bu sefer. | aferin. bu seferlik. |

## 6. Önce → sonra örnekleri (kullanıcıya gösterilip onay alınan ton)

> Sis bu sabah yere yakın ve ağır; ağaçların arasında, rüzgâr yokken titreyen bembeyaz iplikler gerili. Sonra bir çığlık yükseliyor. Bir çocuk sesi.
>
> → Sis bu sabah toprağa çökmüş, ağır ağır sürünüyor. Ağaçların arasına gerilmiş bembeyaz iplikler, ortada rüzgâr yokken titreşip duruyor. Derken bir çığlık kopuyor; bir çocuğun sesi.

> Hesap basit ve korkunç: hepsini aynı anda koruyamazsın. Ama denemezsen biri ölecek; belki de hepsi.
>
> → Hesabı basit, sonucu korkunç: hepsini birden koruyamazsın. Ama denemezsen içlerinden biri ölecek, belki de hepsi.

> Dokuyucu yavaşça onlara dönüyor. Acelesi yok. Hiç olmadı.
>
> → Dokuyucu ağır ağır onlara dönüyor. Hiç acelesi yok; hiçbir zaman olmamış.

> Üç adım. Dört. Sis seni yutuyor ve bacakların hayatta kalmanın en eski dilini konuşuyor: koş.
>
> → Üç adım, dört adım… Sis seni içine çekerken bacakların canlıların bildiği en eski sözü tekrarlıyor: kaç.

> {npc:deniz_arman|in} gözleri donmuş anın içinden sana kilitli. Korkmuyor. Sana güveniyor; bu, korkudan çok daha ağır.
>
> → {npc:deniz_arman|in} gözleri, donup kalmış anın içinden sana dikilmiş. O bakışta korku yok, güven var. Korkudan da ağır bir şey.

> Sessizlik. Kaburgalarının üstünde, eğildiğinde gıcırdayan ikinci bir iskelet var artık. Nefes almak daha zor; darbe almak neredeyse komik derecede kolay.
>
> → Ortalık sessizleşiyor. Kaburgalarının üstünde, eğildikçe gıcırdayan ikinci bir iskelet taşıyorsun artık. Nefes almak zorlaştı; darbe yemekse insanı güldürecek kadar kolay.

> Kadın fotoğrafı göğsüne bastırıp gülüyor ve ağlıyor, aynı anda.
>
> → Kadın fotoğrafı göğsüne bastırmış; hem gülüyor hem ağlıyor.

> (kronik) Yolunu Ateş'in etrafında kurdun; o günden sonra yanındakiler hep biraz daha sıcak, karşındakiler hep biraz daha korkak oldu.
>
> → Yolunu Ateş üzerine kurdun. O günden sonra yanında duranlar biraz daha ısındı, karşına dikilenler biraz daha ürktü.

> (kronik) Önceki taşıyıcının izini sordun; o soru, senden önce bu yolu yürüyen birinin varlığını sonsuza dek kesinleştirdi.
>
> → Senden önceki taşıyıcının izini sordun. O sorudan sonra bu yolu senden önce birinin yürüdüğünden bir daha hiç şüphe edemedin.

> (Yankı) tesadüf, diyor ses, fazla hızlı. muhtemelen tesadüf. hadi. ateşler bekliyor.
>
> → tesadüf, diyor ses; biraz fazla çabuk. herhalde tesadüf. hadi, ateşler bekliyor.

## 7. İş planı

### 7.1 Önce: doğrulama aracı (Parti 0)

Elle 19.000 metni denetlemek mümkün değil. Yeniden yazmaya başlamadan bir araç yaz (ör. `tools/uslup` JVM modülü ya da
`engine` testi; içerik ayrıştırıcı `content/ContentParser` hazır). Bir dosyanın git `HEAD` sürümüyle çalışma kopyasını
karşılaştırır ve şunları **hata** sayar:

- Ağaç yapısı farklı (anahtar, liste uzunluğu, `tr` dışındaki herhangi bir değer — `en` dahil — değişmiş).
- Bir `tr` değerindeki belirteç kümesi (`\{[^}]*\}` ile yakala, **ek kısmı hariç** `{npc:x|e}` → `npc:x`) değişmiş.
- Yankı satırı (eski metin küçük harfle başlıyor ve `{npc:yanki}` atıflı bağlamda) büyük harfe dönmüş.
- Boş ya da eskisinin %60'ından kısa / %160'ından uzun `tr` (bilgi kaybı ya da şişme kuşkusu → uyarı).

Ve **uyarı** olarak üslup ölçümleri: paragraf başına noktalı virgül > 1, art arda 3 "-yor" sonu, "Bu, " ile başlayan
cümle, "etrafında kur", "komik derecede", ", aynı anda". Araç dosya başına özet basar; her parti sonunda koşulur.

**Parti 0 notu (2026-11-07):** Araç `tools/uslup` (JVM modülü, `engine`'in `YamlTree`'sini kullanır; testler
`CheckerTest`, `scripts/check.sh` içinde). Kullanım:

```bash
./gradlew :tools:uslup:run                          # HEAD'e göre değişen content/ dosyaları; hata varsa çıkış kodu 1
./gradlew :tools:uslup:run --args="--ayrinti"        # uyarılar dahil her bulgu, yeri ve yeni metniyle
./gradlew :tools:uslup:run --args="--olcum"          # karşılaştırmasız üslup ölçümü (tüm content/ ya da verilen dosyalar)
./gradlew :tools:uslup:run --args="--ref HEAD~1 content/events/20_orman.yaml"
```

Hatalar: satır sayısı ya da `tr: "…"` maskelendikten sonra herhangi bir satır değişmiş (en, anahtar, yorum, biçim);
ağaçta anahtar/liste uzunluğu/`tr` dışı değer farkı; belirteç çoklu kümesi (ek hariç) değişmiş; bozuk Yankı yazısı
(birleşen işaretli ya da `█` içeren sözcükler) değişmiş; küçük harfle başlayan (belirteçle başlamayan) satır büyük
harfe dönmüş; boş `tr`; yeni ya da silinmiş dosya. Uyarılar: uzunluk eskisinin %60'ının altında ya da %160'ının
üstünde (eski ≥20 karakterse); büyük harfli satır küçük harfe dönmüş; üslup kuralları. Üslup kuralları değer
başına sayılır ve dosya başına "eski → yeni" basılır. `-yor` ve noktalı virgül kuralları köşeli parantezle başlayan
Sistem satırlarına uygulanmaz. §7.1'deki listeye ek olarak "art arda kısa cümle" (en çok 3 sözcüklük iki cümle üst
üste: §0.2'deki kesik ritim) ve sözlükten ", ve", "bir şekilde", "muhtemelen", "-mayı seçiyorsun", "fark ediyorsun".
Paragraflar arası "-yor" tekrarı ölçülmez (her `tr` ayrı değerlendirilir); okuma denetimi bunu yakalar.

Başlangıç ölçümü (19.249 `tr`): noktalı virgül >1: 216 · art arda 3 -yor: 303 · art arda kısa cümle: 1.616 ·
"Bu, ": 24 · "etrafında kur": 9 · ", aynı anda": 12 · "bir şekilde": 11 · "fark ediyorsun": 11 · "muhtemelen": 3 ·
", ve": 3 · "komik derecede": 1 · "-mayı seçiyorsun": 1. Bunlar sıfıra indirilecek hedef değil (Yankı fısıltısı ve
diyalog kısa olabilir); partiden partiye düşüşü gösterir.

### 7.2 Partiler (sıra: oyuncunun karşılaştığı sıra)

Sözcük sayıları yaklaşık (tek satırlık `tr` değerleri). Büyük dosyalar olay sınırlarından bölünür; bir ajan ~5.000
sözcükten fazlasını tek seferde almaz.

| Parti | Dosyalar | ~Sözcük | Durum |
| --- | --- | --- | --- |
| 0 | Doğrulama aracı (`tools/uslup`) | — | **bitti** |
| 1 | `story/00_prolog`, `events/10_yoldaslar`, `20_orman`, `30_carsi_istasyon`, `40_ana_yay` (Perde 1) | 27.600 | **bitti** |
| 2 | `50_perde2_ana`, `56_deniz_yanki_aras` | 23.800 | **bitti** |
| 3 | `51_siginak_elif`, `52_lonca_kaya`, `53_yukselenler_vahit` | 30.900 | **bitti** |
| 4 | `54_sessiz_ag_mert`, `55_kadimler_thessar`, `90_esikler`, `95_bildirimler` | 19.500 | **bitti** |
| 5 | `60_kiyikent_a`, `61_kiyikent_b` | 24.700 | **bitti** |
| 6 | `70_cok_evren`, `71_perde3` | 24.700 | **bitti** |
| 7 | `72_kule`, `tower.yaml` | 11.700 | **bitti** |
| 8 | `80_perde4`, `81_sonlar`, `endings.yaml`, `story/90_sistem` | 19.300 | **bitti** |
| 9 | `npcs/*`, `traits`, `knowledge*`, `ranking`, `army`, `quests*`, `feats*`, `regions`, `factions` | ~6.000 | bekliyor |
| 10 | `enemies*` (bestiyer dahil), `items*`, `skills`, `classes`, `titles*`, `awakenings*`, `essences`, `professions`, `daily`, `system.yaml` | ~25.000 | bekliyor |

**Parti 1 notu (2026-11-07):** 1.774 `tr` değeri yeniden yazıldı (9 dilim ajanı; dilimler çıktıyı `{satır, tr}` JSON'u
olarak verdi, birleştirme tek elden yapıldı). Ardından 5 bağımsız okuyucu dosyaların tamamını eski/yeni/EN yan yana
okudu: 39 bulgu (anlam kayması 13, Türkçe hatası 15, tekrar 9, etiket 2) süzülüp uygulandı. Üslup ölçümü (5 dosya):
noktalı virgül >1 19→0 · art arda 3 -yor 19→2 · art arda kısa cümle 214→177 · "Bu, " 7→0. Açık kalan noktalar:
prologda {npc:yanki|in} sözleri tırnaklı ve büyük harfli (63, 66, 98, 100, 102, 172, 267, 300; DECISIONS #101 ile
kullanıcı: kasıtlı, DECISIONS #128); `30_carsi_istasyon` 1643'te TR/EN anlamı özgün metinde ayrışıyor; 3618'in EN'inde
açılış tırnağı eksik (EN bu işin dışında).

**Parti 2–8 notu (2026-11-07/08, QA P1-15 ile birlikte):** Perde 2–4 olayları, sonlar, endings, eşikler, bildirimler,
sistem sahneleri ve Kule (~13.000 `tr`) 33 yazar dilimi + her dosyaya bir okuyucu ile yeniden yazıldı (199 okuma bulgusu
uygulandı). Aynı geçişte kronikler "yalnız kesinleşeni anlat" ilkesiyle TR+EN düzeltildi (`tools/uslup --kronik-en`).
Briefler: `docs/qa/uslup_brief.md`, `docs/qa/okuma_brief.md`; araçlar `tools/qa/`. Tüm içerik ölçümü (19.254 `tr`):
noktalı virgül >1 216→15 · art arda 3 -yor 303→62 · kısa cümle dizisi 1.616→1.262 · "Bu, " 24→1. Kalan: Parti 9–10.

### 7.3 Parti akışı

1. Parti dosyalarını olay sınırlarından ≤5.000 sözcüklük dilimlere böl.
2. Her dilim için bir alt ajan: bu kılavuzu, `CLAUDE.md` içerik kurallarını ve ilgili NPC dosyalarını okur; yalnızca
   kendi dilimindeki `tr` değerlerini yeniden yazar. Aynı dosyaya iki ajan aynı anda yazmaz (dilimler ayrı dosyaya
   değil, sırayla ya da önce geçici dosyaya yazılıp birleştirilir).
3. Doğrulama aracı + `./gradlew :engine:validateContent :engine:test`. Hata varsa düzelt.
4. **Okuma denetimi:** her dilimden rastgele en az 15 sahneyi baştan sona oku (yalnız tek cümle değil, sahne akışı);
   eski/yeni farkını (`git diff --word-diff`) gözden geçir. Anlam kayması, uydurulmuş bilgi, ses tutarsızlığı varsa
   ajana geri gönder ya da düzelt.
5. Partiyi commit et ("Üslup: Parti N — …"), bu tablodaki **Durum**'u güncelle, partinin üstüne kısa not düş.
6. **Parti 1 bitince durup kullanıcıya örnek göster** (APK + birkaç sahnenin önce/sonrası); onay gelirse devam.
7. Her partiden sonra kullanıcıya `/clear` öner; yeni oturum bu dosyadan devam eder.

### 7.4 Notlar

- İngilizce metin bu işte değişmez. İngilizcede de benzer bir gözden geçirme istenirse ayrı iş olarak planlanır.
- Simülatör ve denge testleri metinden etkilenmez; yine de `scripts/check.sh` her parti sonunda koşulur.
