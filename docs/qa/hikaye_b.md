# QA Hikaye B — Sonlar, epilog kartları, NPC yay düğümleri

## 1. Genel değerlendirme

1. Sonlar güçlü ve birbirinden belirgin biçimde ayrışıyor. Her biri dünya durumuna göre dallanıyor (Kadim durumu, hayatta kalan oranı, Deniz, Elif, Yankı, Vahit'in hakikati) ve kendi Sistem kapanış bildirimiyle bitiyor. Deniz'in "tanımak" motifi (karar ekranında "seni hâlâ tanıyabilecek miyim?", Koruyucu'da "Hâlâ tanıyorum", Yükseliş'te "Seni tanımıyorum", Özgürlük'te "Seni tanıdım. Sonunda.") duygusal kapanışı taşıyor.
2. Döngü gizli son olarak ödüllendirici: kendi el yazınla gelen bozuk satır, prologa geri dönüş ve Yankı'nın "şimdi sıra sende" vedası yerinde. Hasat hem cezalandırıcı hem anlamlı: son haftanın seçimi, hayatta kalan oranı ve Yankı'nın "bazen yetmez" satırı kaybı kişisel kılıyor. Ancak Döngü'nün evren bilgisi New Game+ meta yapısıyla çelişiyor (P1).
3. 8 NPC düğümünde karakter sesi TR ve EN'de tutarlı: Kaya kuralcı asker, Elif kuru mizahlı hekim, Mert kaygılı hacker, Aras kibirli ama yaralı, Thessar muhasebeci ("Kayda geçiyorum"/"Entered"), Vahit acınası bir büyüklenme içinde, Yankı küçük harfle. Seçenekler gerçekten farklı tonlar ve sonuçlar doğuruyor. Sorun kroniklerde: bazıları sonra gelen sonucu önceden ilan ediyor ve sonuçla çelişebiliyor.
4. Vassal sonu Thessar düğümündeki seçimleri yansıtmıyor: "örnek" maddesinin silinmesi bayrak koymuyor. Kronikleri ayrıca, sözleşmedeki Defter Bekçisi rolü ve B kademe tavanı sonda hiç anılmıyor. Bu, seçimin önemini zayıflatan en somut kırık.
5. EN çok doğal ve Kuzey Amerika'ya oturuyor (Gunny, "Nobody's sure clapping is allowed"). Bir bozuk cümle ve bir dizi küçük pürüz var. TR'de Perde 2–4 henüz USLUP'a göre yazılmadığı için beklenen çeviri kokusu kalıpları duruyor (özellikle "X'i seçtiğinde", "Bu, …", kronik iskeleti tekrarı). P0 yok, P1: 6.

## 2. Bulgular

### 2.1 Sonlar ve son koşulları

**P1 · orta — Vassal sonu Thessar düğümündeki seçimleri görmüyor** (`thessar_vassal_teklifi_madde` → `son_vassal`)
- `madde` seçeneği ("örnek" silinsin, İRD 250 gösteriş) yalnız ahlak ve ilişki etkisi veriyor, bayrak koymuyor. Vassal sonu yine de "her kuşaktan bir avuç parlak çocuk" diye haraç anlatıyor ve Deniz "haraç listesindeki çocukları saklamaya başlıyor". Oyuncu tam bu maddeyi sildirmek için büyük bir gösteriş yaptı.
- Sözleşmenin 3. ve 4. maddeleri (B kademe tavanı; imzacının "Defter Bekçisi" olup Dünya'yı yönetmesi) sonda geçmiyor. Oysa OP MC açısından bu son "insanlığa tavan, sana taç" diye kurulmuştu (Deniz'in yoldaş satırı: "Sana teklif ettiği şey bir taç").
- Teklifte "her mevsim vel ödenir" deniyor; sonda "her yıl … onda biri" ve bildirimde "Yıllık haraç" var.
- Öneri: `madde` seçeneğine `bayrak: vassal_ornek_silindi` ekleyin. `son_vassal`'da çocuk haracı paragrafını ve Deniz paragrafını bu bayrağa göre ikiye ayırın. Bekçi/tavan için bir paragraf ekleyin ("İnsanlık B'de duruyor; sen Defter'in tek açık satırısın"). Mevsim ile yıl birleştirilmeli.

**P1 · orta — Döngü bilgisi New Game+ ile çelişiyor** (`son_dongu`, `yanki_son_sir` ysr_1 sezgisi ve ysr_sir)
- "Önceki altı döngünün altısı da burada hasatla kapandı" deniyor; sayaç da sabit "[DÖNGÜ 7]". Döngü yalnız `onceki_oyun ≥ 1` iken açılıyor, yani oyuncunun en az bir önceki oyunu var ve o oyun Koruyucu, Kod Kırıcı gibi bir sonla bitmiş olabilir. `onceki_oyun ≥ 2` paragrafı da "Bunu daha önce de yaptın" diyerek önceki oyunu döngü sayıyor. Oyuncunun kendi zafer geçmişi metinde "hasat" diye yazılıyor.
- Öneri: "altısı da hasatla kapandı" yerine "hiçbiri döngüyü kırmadı / hepsi bu pencereye geri döndü" deyin. Ya da `onceki_oyun ≥ 1` için sayaca ek bir satır koyun ("yedinci satırın altında silinmiş, senin el yazınla başka sonlar"). Böylece önceki sonlar döngü içinde yer bulur.

**P1 · orta — Son dağılımı: Yalnız Avcı en sık son, Koruyucu hedefe zor ulaşılıyor** (`endings.yaml`)
- Yalnız Avcı (%22) "bütün yoldaşlar öldü ya da gitti" diye trajik bir istisna olarak tasarlanmış. Yine de en sık görülen son ve tek koşulu `yoldas_sayisi_max: 0` + S kademe. Oyun boyu kurulan yoldaş yaylarını arka plana itiyor. Koruyucu tarzındaki botun yalnız %49'u hedefine varıyor; koşul 4 ayrı şartın birlikte tutmasını istiyor (merhamet 40, Sığınak 50, hayatta kalan 40, `kadimler_yenildi`).
- Öneri: Simülatörde Yalnız Avcı'nın "tek uygun son" olarak mı yoksa botun tercihiyle mi seçildiğine bakın. Tek uygun son olarak geliyorsa yoldaşların oyun ortasında neden sıfıra indiğini kontrol edin. Koruyucu için hangi şartın düştüğünü raporlatın. `son_karar` öncesinde (ör. Gün 340'larda) eksik şartı sezdiren bir sezgi ya da olay koymayı düşünün.

**P2 · orta — Demir Mod ölümü herkese "haddini aştın" diyor** (`son_hasat`)
- Yankı'nın "haddini aştın. biliyordum." satırı ve bildirimdeki "Haddini aşmak bir seçimdir." herkese çıkıyor. Ama gerçek ölüm hikaye savaşlarında ve bosslarda da var; zorunlu bir boss ölümünde bu suçlama haksız kalıyor.
- Öneri: Bu satırları yüksek Dikkat'e ya da açığa vurmaya bağlayın (`dikkat_min`). Diğer durumlar için nötr bir sürüm yazın (ör. "yetmedi. bu sefer yetmedi.").

**P2 · küçük — Perde 4'te "birkaç sabah kala"** (`son_hasat` perde 4 paragrafları)
- Perde 4 Gün 331'de başlıyor; Gün 331'deki bir ölüm Hasat'a 34 gün kala oluyor. Öneri: `gun_min: 355` ile ayırın ya da "son haftalar" deyin.

**P2 · küçük — Kod Kırıcı'da ölü paragraf** (`son_kod_kirici`)
- Son koşulu `bayrak_yok: yanki_silindi` olduğu için `[bayrak: yanki_silindi]` paragrafı ("Kodun ortasında tam bir satır genişliğinde bir boşluk") hiç görünmez; `bayrak_yok` paragrafının koşulu da zaten hep tutuyor. Öneri: Paragrafı silin ya da bilerek tutuluyorsa yorum ekleyin.

**P2 · küçük — Demir Taht'ta Kadim durumu dalı eksik** (`son_demir_taht`)
- Paragraflar `kadimler_yenildi`, `kadim_anlasmasi` ve `kiyikent_dustu` durumlarını ele alıyor. Filonun körfezde durduğu ama anlaşmanın da olmadığı durum (`son_karar_1`'in dördüncü dalı) hiç anılmıyor; Gün 365'te filo ne yapıyor belli değil. Öneri: "Filo tahtı görüp bekliyor; tek sesle konuşan bir dünyayla pazarlık sırası" gibi bir paragraf ekleyin.

**P2 · orta — "İlk kez / hiç kullanmadığı" şişkinliği ve çelişki** (`son_karar_1`, `son_karar_2`, `son_koruyucu`, `son_ozgurluk`)
- Aynı final dizisinde Sistem dört ayrı "ilki" yaşıyor: soru işareti ("bir kez bile kullanmadığı bir işaret"), sesle konuşma, "[ONAYLANDI]" ("bir kez bile kullanmadığı bir kelime") ve Özgürlük'te "Bir yıl boyunca sana tek bir soru sormadı". Sonuncusu karar ekranındaki soru işaretiyle ve `son_karar_2`'deki soruyla, ayrıca "Bir yuvaya bağlansın mı?" gibi Sistem istemleriyle çelişiyor. "[ONAYLANDI]" da daha önce kullanılmış: `80_perde4` "[ÇEKİRDEK ERİŞİMİ ONAYLANDI]", `53_…` "[Liste onaylandı]".
- Öneri: Koruyucu: "bir yıldır sana hiç yazmadığı bir kelime" deyin ya da kelimeyi "[KABUL]" yapın. Özgürlük: "ilk kez senden bir şey istiyor" kalsın, "tek bir soru sormadı" cümlesi çıksın.

**P2 · orta — Yankı'nın son sözü üç sonda yok** (`son_koruyucu`, `son_demir_taht`, `son_vassal`)
- Yükseliş, Kod Kırıcı, Özgürlük, Yalnız Avcı ve Hasat'ta `yanki_bagi` koşullu bir veda satırı var. En iyi sonda (Koruyucu) ve iki "bedelli" sonda yok; Yankı yalnız epilog kartında görünüyor. Ayrıca Kaya ve Aras hiçbir son sahnesinde yok (Demir Taht'taki Kaya hariç). Öneri: Bu üç sona birer `yanki_bagi` satırı ekleyin.

**P2 · küçük — Koruyucu'da Deniz "başka hiçbir şey söylemeden" konuşmayı sürdürüyor** (`son_koruyucu`)
- "Tanıdım seni," diyor, başka hiçbir şey söylemeden. "Hâlâ tanıyorum." EN'de de aynı: "and nothing else. 'I still do.'" Öneri: "diyor; bir süre susuyor. 'Hâlâ tanıyorum.'"

**P2 · küçük — Hasat'ta ≥%65 için abartı** (`son_hasat_gun`)
- "Hepsini yaşattın" / "You kept every one of them alive" ile %65 hayatta kalan oranı uyuşmuyor. Öneri: "Çoğunu yaşattın".

**P2 · orta — Arketip sırası son ile çelişebiliyor** (`endings.yaml arketipler`)
- Sıra tanrı → tiran → koruyucu → kurtarıcı. Demir Taht'a İnsanlık −30 ile −39 arasında ve merhamet ≥ 20 ile ulaşan bir hükümdar "Koruyucu" arketipini alıyor ("Gücünü bir kalkan gibi taşıdın"). Vassal'ı imzalayan biri "Kurtarıcı" alıyor ("Kurallar seni ezmeye çalıştığında onları eğdin, kırdın"). Ayrıca son karar öncesindeki seçenekler hep merhamet ekliyor. Öneri: Arketip koşullarına `son` ekleyin (ör. Demir Taht'ta koruyucu yerine tiran) ya da Vassal için ayrı bir açıklama yazın.

**Olumlu (not):** Son seçim ekranı (`son_karar_1/2`) iyi kurulmuş. Biriken dünya durumu dört eksende özetleniyor, isteğe bağlı veda yürüyüşü sokakları kişiselleştiriyor, yoldaş tepkileri her NPC'nin sesine uygun (Elif "Hekim tavsiyesi.", Aras "ömrüm boyunca unutturmam"). `hicbir_son` için Hasat'a giden üç seçenek de anlamlı ve son sahnede karşılığını buluyor.

### 2.2 Epilog kartları

**P2 · orta — Döngü epiloglarında kalıp tekrarı ve zaman tutarsızlığı** (`epilog.npc.*` ve `epilog.fraksiyon.*`, `son: dongu`)
- Aynı sonun kartlarında "kim yaptığını bilmedi" iskeleti dört kez geliyor: Elif "kimin bıraktığını hiç öğrenmedi", Sığınak "kimin kurduğunu kimse bilmeyecek", Lonca "kimin yazdığını kimse bilmeyecek", Sessiz Ağ "kaynağını bulmaları yıllar sürecek". NPC kartları geçmiş zamanda, fraksiyon kartları gelecek zamanda; bu da başlık yorumundaki "geçmiş zaman" kuralına uymuyor. Öneri: İki kartta farklı bir görüntü kullanın, zamanı tekleştirin.

**P2 · küçük — Mert'in ölüm kartında ton kayması** (`epilog.npc.mert_kilic[1]`)
- TR "Hata bende." ağırbaşlı; EN "My bad." gündelik ve bir ihanetin ölüm sayfasında hafif kalıyor. Öneri: "That's on me."

**P2 · küçük — Yükselenler Döngü kartında belirsiz özne** (`epilog.fraksiyon.yukselenler[0]`)
- EN "who it is will surprise them more than the preacher": "them" kimi kastediyor belli değil. Öneri: "…and the one asking will be more surprised than the preacher."

**Olumlu:** Kartlar her NPC'nin kaderini GDD tablosuna göre karşılıyor (Kaya "son asker olarak gömüldü", Elif "Daha az evrak.", Aras "Sen de hızlısın, ha?" geri dönüşü, Thessar "raporu anomalinin adında duruyordu"). Varsayılan satırlar her son için anlamlı.

### 2.3 NPC yay düğümleri

**P1 · küçük — Kronikler sonucu önceden ilan ediyor ya da yanlış sonucu yazıyor**
- `elif_konsey_oylamasi_ikna / zorlama`: Kronik "Konsey'in başına ihtiyar kaptan geçti" diyor, ama seçenek `…_karne` sahnesine gidiyor ve orada salonu çevirip Elif'i başkan yapmak mümkün. Kronik sonuçla çelişiyor.
- `vmask_hakli / kir` (savaş): Kronik "kanal ona kendi sesini verdi" diye yenilgiyi varsayıyor. Kazanan oyuncu bir de "Hasadın kanalını kırdın" kroniği alıyor; ikisi çelişiyor.
- `akd_kopru / sebep`: Kronik "o sebebi bir gün çok-evrenin kapısında buldu" diye Aras'ın geleceğini kesinleştiriyor. Aras sonra ihanet edip ölebilir (`ihanet_aras`).
- `mkt_kacamak / tehdit`: Kronik "sana bir daha bir şey de anlatmadı" diyor; Kod Kırıcı'da Mert yine "Gördüğün şeyi bana oku" diyor.
- `elif_konsey_oylamasi_son / uyu`: Kronik "Bütün {yer:kiyikent_liman} adına karar veren bir kadına" diyor; Rıza ya da oyuncu başkan olduysa yanlış.
- Öneri: Savaşa giden seçeneklerde kroniği nötr yazın ("…kırmaya kalktın") ve sonucu sonuç sahnesinin kroniğine bırakın. Geleceği ilan eden kronikleri ya koşullu yapın ya da "-ecekti" yerine o anki eyleme çekin.

**P1 · küçük — "yıllardır" bir yıldan kısa süren dünyada** (TR, kısmen EN)
- `53_yukselenler_vahit` 2481 sezgisi: "{npc:mert_kilic|in} yıllardır duyduğu o kısa, düzenli sayım" (EN "for months").
- 2542 kroniği: "sesin yıllardır bir yazıcıdan geldiğini" (EN de "for years"). Vahit aynı sahnede "Bir kez konuştu… Entegrasyon sabahı" diyor.
- 2188 kroniği: "Bir peygamberin yıllardır açık tuttuğu hattı" (EN "for years").
- `52_lonca_kaya` 2247 (`koz`): "yıllardır cebinde taşıdığın sahte ihbarı" (EN "for months").
- Öneri: "aylardır" yapın; EN 2542 ve 2188'i "for months" yapın. Ayrıca 2481'de TR Mert'in adını veriyor, EN "the radio crew" diyor. Mert'le tanışılmamışsa TR bir ismi erken veriyor; ikisini aynı yapın.

**P1 · küçük — EN bozuk cümle** (`deniz_son_secim_kalir_yarali`)
- "the space between you two is wider than it is." Karşılaştırmanın öbür ucu düşmüş; TR "ateşten geniş". Öneri: "…is wider than the fire."

**P2 · orta — Kaya darbe gecesinde zaman çizelgesi karışık** (`kaya_darbe_gecesi`)
- Olay Gün 292'de. Haritada "Şafak" yazıyor ve `devir` seçeneği "Bu gece kimse şafağa yürümüyor" diyor; bunlar darbenin ertesi sabah olacağını söylüyor. Oysa Kaya darbenin "geri sayım altmış beşi gösterdiği sabah" (≈Gün 300) olacağını söylüyor, `kacis` sahnesi de "Önünde birkaç gün var" diyor. EN kart adı ve on kronik "the night before the coup" diyor, oysa darbeye günler var. `ortak` kroniğinde "şehrin son kışına" geçiyor; mevsim başka yerde kurulmuyor.
- Öneri: Haritadaki yazıyı "65" yapın (ya da "Şafak · 65"). EN kartı "The coup plot" / "Coup night" yapın, kronik önekini "On the night of the coup plot" yapın ya da tamamen çeşitlendirin.

**P2 · küçük — Kaya düğümünde TR/EN pürüzleri**
- "Süngünün dipçiği çenene oturuyor": süngünün dipçiği olmaz; EN "The rifle butt". Öneri: "Tüfeğin dipçiği".
- EN "'Sixty-five morning,' he says." doğal değil. Öneri: "Morning of sixty-five."
- EN "'Weather,' he says. 'You always were.'" Kuzey Amerika okuru için anlaşılmaz. Öneri: "'Fair-weather,' he says. 'Always were.'"
- EN "When you come out of the mill {yer:sanayi} streets are empty": "the Ironworks" çözülünce virgül ve iyelik eksik kalıyor. Öneri: "When you come out of the mill, the streets of {yer:sanayi} are empty".

**P2 · küçük — Thessar: yerelleştirme kayması** (`thessar_vassal_teklifi_kes`)
- EN "in English, then Turkish": Port Harlow'daki Kuzey Amerika okuru için "Turkish" bir anda dışarıdan geliyor. TR'de de sıra "İngilizce, Türkçe". Öneri: TR "Türkçe, İngilizce, sonra…"; EN "in English, then Spanish, then…".

**P2 · küçük — Mert düğümü**
- `mkt_sozcu_dustu` kroniği TR "aracısının cesedinin başında" diyor; sahnede pelerinin içinden "hiçbir şey çıkmıyor". EN doğru yazmış ("empty cloak"). Öneri: "boş pelerininin başında".
- EN `mkt_ekip`: "Just, one second, it was inside the door." Öneri: "One second it just… was inside the door."

**P2 · küçük — Aras sezgisinde EN söz dizimi** (`akd_1` sezgi, ALG 60)
- "so he doesn't have to look at his own reflection, not the guardian": "not the guardian" yanlış yere düşmüş. Öneri: "He's looking at you, not the guardian, so he doesn't have to look at his own reflection."

**P2 · küçük — Diğer EN pürüzleri**
- `vmask_1`: "he laughed. Crying." Öneri: "he laughed until he cried."
- `son_kod_kirici`: "smile a smile you haven't seen in a year". Öneri: "gives you a smile you haven't seen in a year."
- `elif_konsey_oylamasi_1`: "We take care of whoever got decided about." Öneri: "We take care of the people decisions land on."
- `son_karar_2`: "Rule Earth with one hand", "tek elden"in birebir çevirisi. Öneri: "Rule Earth alone."

**P2 · küçük — Yankı düğümünde mekân tutarsızlığı** (`ysr_kayip`)
- Olay Çekirdek'te açılabildiği hâlde ("Çekirdek'in kalbinde") yenilgi metni "seni Kule'nin zeminine yapıştırıyor" diyor. Öneri: "zemine" deyin.

**P2 · küçük — Gösteriş klişesi tekrarı**
- "Kapıyı menteşesinden sök" türü örnekte üç kez geçiyor (`vmask_1 kir`, `kaya_darbe_gecesi_yenilgi kir`, `mkt_tehdit`te kırılan menteşe). GDD örneği bu kalıbı tanımlıyor, ama sık tekrar gösterişin şıklığını azaltıyor. Öneri: Birini farklı bir güç ifadesine çevirin.

**Olumlu:** Her düğümde 3–8 gerçek seçenek var. İlişki, stat ve bayrak kilitleri anlamlı (Kaya `vazgecir` güven ≥ 30, Yankı `dinle` güven ≥ 60 ile Kod Kırıcı koşulu aynı). Yoldaş tepkileri her NPC'nin değerlerinden geliyor. Aras'ın köprü ve intihar geçmişi incelikle verilmiş; Vahit'in hakikati `gercek_hasat` ile dünya gerçeğine bağlanıyor. Thessar'ın "Hasat bir kez öder; vassal sonsuza dek" satırı Vassal sonunun tezini taşıyor.

### 2.4 Türkçe üslup (Perde 2–4, henüz yeniden yazılmadı)

**P2 · orta — Son sahnelerinin açılış iskeleti aynı ve çeviri kokulu**
- "Kalkan olmayı seçtiğinde pencere…", "Tahtı seçtiğinde pencere…", "Geçidi seçtiğinde pencere…", "Yeni bir Entegrasyon seçtiğinde pencere…": dört son aynı kalıpla açılıyor ve "X'i seçmek" USLUP §5'teki kaçınılacaklar listesinde. Benzeri: "arkasından bakmamayı seçiyorsun" (Yükseliş), "bakmamayı seçtiğin için" (`vmask_kac_hakli`).
- "Bu," açılışları: "ve bu, korkunç olduğu kadar güzel" (Özgürlük), "Bu, odadaki her şeyden daha çok korkutuyor seni" (`mkt_1`).
- "bir" yığılması: "bir yıl boyunca bir kez bile kullanmadığı bir kelimeyi" (Koruyucu).
- Kronik iskeleti tekrarı: `kaya_darbe_gecesi`'nde on kronik "Darbe gecesinde …" ile, Yankı kroniklerinin çoğu "Kafandaki ses(i) …" ile, son kararın tümü "Son kararında …" ile başlıyor. Epilogdaki "en kritik 5 seçim" kartında aynı olaydan iki kronik yan yana gelebilir.
- Öneri: USLUP Parti 2+ sırasında bu dosyalara (`81_sonlar`, `52_…`, `53_…`, `54_…`, `55_…`, `56_…`, `endings.yaml`) öncelik verin. Son sahnelerinin açılışlarını her sona özgü bir imgeyle kurun.

## 3. Açılamayan örnekler

Yok. 9 sonun tamamı, `son_hasat`, `son_karar` (ilk sahne ve karar penceresi), epilog kartları (her NPC ve fraksiyon için ilk iki satır ve son satır; diğerleri yalnız koşul olarak) ve 8 yay düğümünün tamamı `tools/qa/olay.py` ile açıldı. Not: Araç bildirimlerin `kosul` alanını yazdırmıyor. `son_vassal`'daki üç "[SON: Vassal]" bildirimi bu yüzden tekrarlanmış gibi görünüyor; YAML'da koşulları doğru (ihanet yok / ihanet + Thessar ölü / ihanet + Thessar sağ).
