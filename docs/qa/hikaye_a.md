# QA — Hikaye ve oynanış değerlendirmesi (örneklem)

Kapsam: brief'te listelenen örnekler `tools/qa/olay.py` ile okundu (TR+EN); BALANCE.md, GDD (OP MC, Hikaye Yönetmeni, Ana karakterler), GDD.en (ilk 60 satır + eşleme tablosu), USLUP §0–§6. Ayrıca bulguları doğrulamak için birkaç dar grep/sed (motorun sezgi öneki, eşik kuralı, NPC kişilikleri, birkaç seçeneğin etkileri) yapıldı. Hiçbir proje dosyası değiştirilmedi.

## 1. Genel değerlendirme (5 satır)

1. Anlatı iskeleti güçlü: vapur/simit/martı, "tanıyamayacağım bir şeye dönüşme", kalkan, "her seferinde" gibi motifler prologdan sonlara kadar taşınıyor; Döngü sonu prologu kapatıyor. Sonlar dünya durumunu (hayatta_kalan, kadimler_yenildi, gerçek bayrakları, NPC'lerin akıbeti) gerçekten yansıtıyor.
2. İngilizce Kuzey Amerika yerelleştirmesi genel olarak çok iyi (soft pretzel, Fourth of July, Gunny, Skipper Reese, "Jesus. Oh, Jesus."). Çeviri kokusu az, kusurlar tek tük.
3. Türkçede açık bir kalite basamağı var: Perde 1 kılavuza göre temiz. Perde 2–4'te USLUP §0'da sayılan kalıpların neredeyse hepsi duruyor; kılavuzun örnek gösterdiği cümlelerin bazıları birebir orada.
4. Mekanik/metin uyuşmazlıkları var: ~95 sezgi satırında çift stat öneki görünüyor, Kademe Kırma seçeneğine erişilemiyor, bir eşik olayı yapısal olarak hiç tetiklenemiyor, Deniz'in ölümü seçimden önce ilan ediliyor, kronik satırları karşılığı olmayan sonuçlar vaat ediyor.
5. OP MC sayıları hedefte (hakimiyet %82). Hissedilen hakimiyet ise iki göstergeyle zayıflıyor: Ezme payı %10,7, Dikkat de Gün 210–300 arasında tavanda (p50 91–97) kalıyor. Perde 4'te zorunlu düğüm ve veda sahnesi yoğunluğu tekrara düşüyor.

**Sayım:** P0: 0 · P1: 13 · P2: 22

---

## 2. Bulgular

### A. OP MC hissi (Mücadele → Kırılma → Hakimiyet)

**A1 · P1 · küçük — `yeni_tavan_1` Kademe Kırma seçeneğine pratikte erişilemiyor.**
`[kir] "Kademe Kırma'yı dene: ilk sen vur"` seçeneğinin koşulu `seviye_min: 14`, olay Gün 24'te geliyor. BALANCE'a göre Gün 30'da oyuncu seviyesi p90 = 9. Bu yüzden GDD'nin "Yeni tavan → Kademe Kırma" anı (`yeni_tavan_kirma` sahnesi, `yeni_tavan_kirildi` bayrağı, `ogretici_bekci_1`'deki "Taçlı geyiği devirenin haberini herkes almış" paragrafı) hemen hiçbir oynanışta görülmüyor.
*Öneri:* Eşiği ~7–8'e indir ya da koşulu stat/iz tabanlı yap (ör. `iz: kemik_kabuk` veya `stat {str: N}`). Simülatörde `yeni_tavan_kirma` görülme oranını ölç.

**A2 · P1 · orta (doğrulanmalı) — Seviye 10 sınıf teklifi Perde 1'de çoğu oyuncuya gelmiyor.**
GDD Perde 1 düğümleri arasında "Seviye 10 sınıf teklifi" var. BALANCE'ta Gün 30 seviyeleri p50 8, p90 9; yani oyuncuların büyük çoğunluğu sınıfını Perde 2'de alıyor. Perde 1 metni sınıfsız bir kahramana göre yazılmışsa sorun yok. Ama GDD'deki "Mücadele kısa" ilkesi ve Perde 1 dikey dilimi sınıf anına göre kurgulandıysa ritim kayıyor.
*Öneri:* Perde 1 XP'sini %20–30 artır ya da sınıf eşiğini Perde 1 için 8'e çek. Simülatöre "ilk sınıf günü" metriği ekle.

**A3 · P1 · orta — Ezme payı %10,7; hissedilen hakimiyet, sayılardaki hakimiyetin gerisinde.**
Gün 330'da oyuncu ana NPC'lerin ×19,5'i, ama kazanılan savaşların ancak onda biri Ezme ile bitiyor. Bunun nedeni Perde 2+ düşmanlarının dünya seviyesine ölçeklenmesi (oyuncu dünya seviyesinin ancak ~1,1–1,5 katı). Hakimiyet, dünya seviyesine göre ölçüldüğü için %82 çıkıyor; oysa GDD'deki hakimiyet "çevresini açıkça geride bırakmak". Oyuncu her savaşı hâlâ taktik oynuyorsa OP MC'nin uzun evresi sayılarda var, oyunda yok.
*Öneri:* Akış (fırsat eli) düşmanlarında `@dunya` yerine `@dunya-20`…`-40` kullan ya da Ezme eşiğini gevşet. Ezme hedefi Perde 2 ~%30, Perde 3–4 ~%50 olsun; gerçek savaş boss ve hikaye düğümlerine kalsın.

**A4 · P1 · orta — Dikkat Gün 210–300 arasında tavanda (p50 91→97, p90 98).**
Dikkat "ana gerilim ölçeği". Üç ay boyunca tavanda kalınca [Gizle] seçeneklerinin mekanik anlamı kalmıyor, eşikler de çoktan tükenmiş oluyor. Gün 300'deki düşüş muhtemelen olay kaynaklı.
*Öneri:* Perde 3'te günlük doğal sönümü artır ya da eşik başına "boşaltma" ekle (eşik olayı bitince −20). Hedef: p50'nin 60–85 bandında dalgalanması.

**A5 · P2 · — Güç anları metinde başarılı.**
`kirilma_kirilis` (zaman dışına düşmek, cam/F harfi), `kirilma_kemik/damar/kara`'nın üç farklı ton ve bedeli, `donus_ezme`'de alkışlanmayan güç ("ne hissetmesi gerektiğini bilmiyor"), `gemi_bakis`. Tanık sahneleri (kalabalığın susması, diz çöken toplayıcı) türün tatminini veriyor. İleride yazılacak Ezme/gösteriş sahneleri için örnek olarak kullan.

### B. Seçimlerin önemi, kronik ve sonlar

**B1 · P1 · orta — Kronik satırları, hiçbir durumun karşılamadığı sonuçlar vaat ediyor.**
Ör. `gemi_kule` › `ezberle`: "aylar sonra o harfler bir kapıyı açtı". Seçenek hiçbir bayrak koymuyor (yalnızca ahlak +1, ZEK +2, itibar), yani "kapı" hiç açılmıyor. Benzerleri: `perde3_son_ordu` › `talim` "o ordu son savaşta bir kez bile dağılmadı" (yalnızca `ordu_kazan 30`); `ihanet_mert_konus` › `geri_cek` "hiçbiri o yıl Kadimler'in kapısını çalmadı" (kronik savaştan önce yazılıyor, savaş kaybedilse de duruyor). 352 okunmayan bayrakla birlikte düşünülünce son ekranındaki "en kritik 5 seçim" kartları gerçek olmayan sonuçlar anlatabiliyor.
*Öneri:* (a) Kronik metinleri yalnızca kesinleşmiş olanı anlatsın: "harfleri ezberledin; bir gün işe yarayacaklarını düşündün". (b) Ya da vaadi tutan bir bayrak + koşullu paragraf ekle (ör. `cekirdege_yol`'a `gemi_harfleri_ezberlendi` koşullu bir satır). (c) Savaşla sonuçlanan seçimlerde kroniği `kazan` sahnesine taşı. Aracı genişlet: "kronik gelecek zaman/sonuç vaat ediyor ama seçenek bayrak koymuyor" taraması.

**B2 · P1 · küçük — `bekci_deniz_hat` Deniz'in ölümünü seçim yapılmadan ilan ediyor.**
3. paragraf koşulsuz: "Yüzünü son görüşün bu oluyor." / "It's the last time you see their face." Seçeneklerden `vazgec` ("Hayır. Birlikte gireceğiz") Deniz'i yaşatıyor (`npc_ol` yok, `bekci_cokus`'a gidiyor). Bu dalda anlatıcı yalan söylemiş oluyor.
*Öneri:* Cümleyi `kos`/`saril` sonrasına (`bekci_cokus_deniz`) taşı ya da belirsizleştir: "O yüzü bir daha böyle görecek misin, bilmiyorsun."

**B3 · P1 · küçük (doğrulanmalı) — `son_yukselis`'te iki çelişkili Deniz paragrafı aynı anda görünebilir.**
`{yoldas: deniz_arman}` → "Seni tanımıyorum… dönüp yürüyor"; `{npc_fraksiyon: {deniz_arman: yukselenler}, npc_hayatta}` → "Seçilmişlerin en önünde {npc:deniz_arman} yükseliyor". `secim_deniz_ses` › `katil` dalında Deniz hem yoldaş hem Yükselenler üyesi olabilir.
*Öneri:* İkinci koşula `yoldas` yerine `bayrak_yok`/`yoldas` dışlaması ekle ya da birinciye `npc_fraksiyon` dışlaması koy.

**B4 · P2 · orta — Rastgele oyuncuların en sık sonu Yalnız Avcı (113/300 ≈ %38).**
Odaksız oyunun varsayılan sonu Hasat değil Yalnız Avcı. Bu, yoldaşların kolayca gittiğini/öldüğünü gösteriyor; Yalnız Avcı'nın "bilinçli yalnızlık" tonunu da sulandırıyor. Toplam payı %22,1 ile en yüksek son.
*Öneri:* Yoldaş ayrılma eşiklerini (`DEPARTURE_DISTANCE`) gözden geçir. Ya da Yalnız Avcı'ya "en az bir yoldaşı kendi kararınla bıraktın / ayrılık eşiği yaşandı" gibi bir koşul ekle.

**B5 · P2 · küçük — Vassal teklifi ile Vassal sonu farklı şartlar anlatıyor.**
`thessar_vassal_teklifi_1`: "hiçbir insan B kademesini geçmez", imzacı "Defter Bekçisi" olur. `son_vassal` ise Öz'ün onda biri, çocuklar ve sancak diyor; B tavanından ve oyuncunun Defter Bekçisi rolünden söz etmiyor.
*Öneri:* `son_vassal`'a B tavanı ve "Defter Bekçisi" unvanını işleyen bir cümle ekle (bildirime unvan satırı).

**B6 · P2 · küçük — Demir Mod ölüm sonu her ölümü "haddini aşmak" diye yorumluyor.**
`son_hasat`: "haddini aştın. biliyordum." ve "Haddini aşmak bir seçimdir." BALANCE'a göre ölümler hikaye savaşlarından ve bosslardan geliyor (haddini aşan tarz yok). Gün 12'de Sis Dokuyucu'ya ölen oyuncuya bu suçlama haksız düşüyor.
*Öneri:* Bu iki satırı `dikkat_min` (ör. 70) ya da `aciga_vur` sayısıyla koşulla. Düşük Dikkat için ayrı bir satır yaz ("yetmedi; bazen yetmez").

**B7 · P2 · küçük — Döngü sonu, oyuncunun önceki bitirişiyle çelişebilir.**
`son_dongu`: "Önceki altı döngünün altısı da burada hasatla kapandı". Koşul `onceki_oyun ≥ 1`; oyuncunun önceki oyunu Koruyucu ile bitmiş olabilir.
*Öneri:* `onceki_oyun` için ayrı bir paragraf: "…altısı hasatla kapandı; biri, senin bir önceki döngün, başka bir satır bıraktı."

**B8 · P2 — Olumlu:** `son_karar_2` yalnızca uygun sonları gösteriyor, `hicbir_son` dalında üç anlamlı Hasat tavrı var. Sonlar `kadimler_yenildi` × gerçek bayrağı × `hayatta_kalan` kombinasyonlarını ayrı ayrı yazıyor. Epilog kalitesi yüksek.

### C. Karakter tutarlılığı

**C1 · P1 · küçük — Vahit zaman çizgisi: "yıllardır".**
`vmask_1` sezgisi TR "{npc:mert_kilic|in} yıllardır duyduğu o kısa, düzenli sayım", EN ise "for months". `vmask_oda` › `bak_sahte` kroniği "sesin yıllardır bir yazıcıdan geldiğini gördün" / "for years". Olay Gün 245'te, Entegrasyon bir yıldan kısa süre önce.
*Öneri:* TR "aylardır" yap; kronikte "Entegrasyon'dan beri". Vahit'in Entegrasyon öncesi bir tarikatı varsa bunu GDD'ye yazıp metinde açıkça kur.

**C2 · P2 · küçük — Yankı'nın büyük/küçük harf disiplini Perde 2–4 ve eşik olaylarında kayıyor.**
Fısıltı içinde "Sistem" büyük harfle yazılıyor (`secim_yukselenler` sezgisi: "Sistem onunla konuşmuyor", `kirilma_1`: "Sistem bu açıklığı", `gecit_cocuk`: "Sistem'i yok"), oysa başka yerlerde "sistem" küçük. `esik_itaat`'ın `yanki_bagi` sezgisi tamamen cümle düzeninde: "Korku sadakate benzer. Uzaktan." EN'de de "the System"/"the system" karışık.
*Öneri:* Kural koy: Yankı'nın sözünde "sistem" küçük (TR+EN). `esik_itaat` sezgisini küçük harfe çevir. `tools/uslup` aracına "yanki_bagi koşullu sezgide büyük harfle başlayan cümle" kontrolü ekle.

**C3 · P2 · küçük — Yankı'nın döngü hafızası tutarlı ama terimler değil.**
`yol_olusumu_ani`'de "bir kez daha gördüm bunu" (tek önceki taşıyıcı), `ysr_sir`'de altı döngü. Bu kademeli hatırlama olarak okunabilir, sorun değil. Ama EN'de "carrier" (Perde 3) ile "host" (`ysr_1` kroniği) aynı kavram için karışık kullanılıyor; dünya sayısı da "yüzlerce dünya", "bin dünya", "bin dört yüz dünya" (`p4_yikik_1`) arasında dolaşıyor.
*Öneri:* Tek terim seç (carrier/taşıyıcı) ve tek sayı (ya da bilinçli olarak "bilinmeyen").

**C4 · P2 · küçük — `ihanet_kaya_1` kroniği her oyuncuya aynı geçmişi yüklüyor.**
"sana silahını ilk öğreten adamla yüz yüze geldin". Sığınak/Ağ oyuncusu için Kaya akıl hocası olmamış olabilir.
*Öneri:* `kaya_akil_hocasi` bayrağına göre iki kronik (kronik koşul alamıyorsa seçeneği ikiye böl).

**C5 · P2 · küçük — `son_koruyucu`: "başka hiçbir şey söylemeden" dedikten sonra Deniz ikinci bir cümle söylüyor.**
"Tanıdım seni," … başka hiçbir şey söylemeden. "Hâlâ tanıyorum." (EN aynı). Sözü sona al ya da "başka hiçbir şey" ifadesini çıkar.

**C6 · Olumlu:** Deniz (TR cinsiyetsiz, EN "they"; "pazarlık yok"; "Hep sen önce inerdin"), Kaya (asker dili: Başçavuş/Gunny), Elif (hekim kuruluğu, "Hekim tavsiyesi"/"Doctor's orders"), Mert ("Kaydediyorum", "sessizlik de veridir"), Aras ("Sen de hızlısın, ha?" Perde 2 → Perde 3 geri çağrısı), Thessar (kibar, iki sesli) iki dilde de aynı kişi. Mizah ve hitap iki dilde eşdeğer ayarlanmış.

### D. Türkçe üslup (Perde 1 yeniden yazımı / Perde 2–4)

**D1 · P1 · büyük — Perde 2–4 USLUP'tan geçmemiş; kılavuzun "kaçın" örnekleri birebir duruyor.**
Örneklemde görülenler:
- **"Bu, …":** `gemi_toplama` "Bu, sana yetiyor."; `yanki_sustur` "ve bu, beklediğinden çok daha soğuk"; `son_ozgurluk` "ve bu, korkunç olduğu kadar güzel".
- **Özne + virgül + "kesinleştirdi"** (USLUP §0'daki örneğin kendisi): `yol_olusumu_ani` › `kimdi` kroniği "o soru, … varlığını sonsuza dek kesinleştirdi".
- **"seçmek" kalıbı:** `gecit_esik` "Başka bir dünyanın kapısında beklemeyi seçtin"; `son_koruyucu` "Kalkan olmayı seçtiğinde"; `son_karar_2` kronikleri "kalkan olmayı seçtin".
- **Kalıp kronik** (§0.5'in örneği): `yol_olusumu_1` "Yolunu Kule'nin tepesinde kurdun; o kattan sonra…" ×4.
- **İngilizce ara cümle virgülleri:** `donus_1` "ve sonra, hepsi aynı anda, birini aramaya"; `yanki_1` "Hepsinin üstünde, buğu gibi, aynı satır"; `yanki_meydan` "Sonra bütün camlarda, aynı anda, tek bir kelime beliriyor … Ve camlar temizleniyor."
- **Calque:** `donus_vapur_yanki` "sadece söylüyorum" (*just saying*); `secim_1` "en çok da birbirlerine kaybetmeyi" (*least of all to each other*); `secim_siginak` "o günün senin günün olmamasını istiyorum".

Perde 1 (`kirilma`, `ogretici_bekci`, prolog) bu kalıplardan temizlenmiş: ulaçlı bağlama, -mış'lı sahne kurma, deyim kullanımı yerinde. Fark okurca hissediliyor ve Perde 2'nin ilk sahnesinde (`donus_1`) hemen başlıyor.
*Öneri:* USLUP Parti 2+ planını Perde 2 ana yayından (`50_perde2_ana`) başlat. Sıra: ana yay → `81_sonlar` → `80_perde4` → `71_perde3`, çünkü oyuncunun en çok gördüğü metin bunlar. `tools/uslup`'a "Bu, " cümle başı, ", aynı anda," ve "-meyi seçtin" kalıp sayaçlarını ekle.

**D2 · P1 · küçük — Türkçe dilbilgisi ve anlam hataları.**
- `bekci_cokus_deniz` › `yanki`: "{npc:yanki} tek söz etmeden sustur onu". Hal eki eksik ve sözdizimi bozuk. → "{npc:yanki|i} daha tek söz etmeden sustur".
- `yanki_meydan`: "bir sürü balık gibi". "bir sürü" = *a lot of*; kastedilen "balık sürüsü gibi". Anlam hatası.
- `gecit_korundu`: "kimse ona zorlamıyor" → "kimse onu zorlamıyor".
- `p4_yikik_1`, `p4_oncu_1`, `p4_acilis` kroniği, `55_kadimler_thessar`: "Hasatın" (5 yer). Cins isimde yumuşama gerekir: "hasadın". "Hasat'ın" özel ad kullanımı tutarlıysa kalabilir.
- `fraksiyon_son_pozisyon`: "steteskobu" → "stetoskobu".

**D3 · P2 · küçük — Kronik iskelet tekrarı (ölçüldü).**
2.348 kronik satırının %20'si, kendi olayındaki en sık iki sözcüklük açılışı paylaşıyor ("İhanet gecesi…" 21, "Darbe gecesinde…" 8/8). EN'de "On the night of" 39 kez geçiyor. Son ekranı 5 kart gösterdiği için risk orta: farklı olaylardan gelse de "X gecesi/X olduğunda …-dın;" iskeleti art arda dizilebilir.
*Öneri:* USLUP §2'deki "her satır başka iskelet" kuralını kronik partisinde uygula. Epilog seçicisine "aynı ilk iki sözcükle başlayan iki kartı yan yana koyma" kuralı ekle.

### E. İngilizce (Kuzey Amerika)

**E1 · P2 · küçük — Tek tük garip cümleler.**
- `elif_konsey_oylamasi_1`: "We take care of whoever got decided about." → "We take care of whoever the decisions land on."
- `akd_1` sezgisi: "…look at his own reflection, not the guardian." Sarkan ek → "He's watching you, not the guardian, so he won't have to look at his own reflection."
- `kirilma_kirilis`: "That's so much heavier than scared." Doğal ama TR'deki sadelikten daha duygusal; kabul edilebilir.

**E2 · P2 · küçük — EN metinlerde belirteç yerine düz yer adı.**
`donus_1` bildirimi "The Esplanade · Gate grade D…" (TR'de `{yer:kordon}`), `gemi_son` "The Esplanade's dark.", `perde3_son_1` "The Esplanade's relit lanterns", `son_karar_1` "The Ironworks stacks". `yanki_cekirdek`/`yanki_sonuc` kroniğinde "{npc:yanki}'s" (`|pos` yerine düz kesme işareti). Kural ihlali; ad değişirse kırılır.
*Öneri:* `{yer:kordon|pos}` / `{yer:sanayi}` / `{npc:yanki|pos}` kullan. Validator'ı EN tarafında da düz yer adı arayacak şekilde genişlet.

**E3 · P2 · küçük — Terim büyük/küçük harf tutarsızlığı.**
"Grade E", "A-Grade", "S grade" (`son_yalniz_avci`) ve "Gate grade D" bir arada. GDD.en "Grade" diyor; tek biçim seç ("Grade S", "an S-Grade hunter").

**E4 · Olumlu:** Birimler (50 ft, °F), kültür uyarlamaları, kısa vurucu diyalog ritmi ve "they" zamir sistemi doğal. Sistem sesindeki kuru mizah ("This is a courtesy reminder.", "We just wanted that on the record.") NA okuruna çok iyi oturuyor.

### F. Tempo ve tekrar

**F1 · P1 · orta — Perde 4: 27 günde 11 zorunlu/kesin düğüm ve art arda dört "veda turu".**
Gün 331–358 arasında `perde4_acilis`, `p4_kadim_oncusu`, `p4_siralama_zirvesi`, `fraksiyon_son_pozisyon`, `p4_son_yayin`, `kadim_istilasi`, `p4_yikik_kiyikent`, `p4_son_gece_ates`, `cekirdege_yol`, `sistem_cekirdegi`, `son_karar` var (simülatörde hepsi %100). Bunların dördü aynı iskelette ilerliyor: `perde3_son_gece` (çatıda çay) → `p4_son_gece_ates` (ateş başı) → `son_karar_veda` (şehir yürüyüşü) → `son_karar_2` (yoldaş satırları). Her birinde yoldaşlar aynı sırayla (Deniz, Elif, Aras, Mert) birer cümle söylüyor ve "bir yıl önce…" geri çağrısı tekrar ediyor. Fırsat eline ve oyuncu inisiyatifine yer kalmıyor.
*Öneri:* `p4_son_gece_ates` ile `son_karar_veda`'yı birleştir ya da biri koşullu olsun (ör. `son_veda_yuruyusu` yoksa ateş sahnesi). `p4_yikik_kiyikent` ve `p4_siralama_zirvesi`'ni fırsat eli kartına çevir (ağırlık yüksek, zorunlu değil). Yoldaş satırlarının sırasını ilişki puanına göre değiştir.

**F2 · P2 · küçük — Aynı imge/ifade art arda.**
`son_karar_1` "Sistem'in bir yıl boyunca bir kez bile kullanmadığı bir işaret", hemen ardından `son_koruyucu` "bir yıl boyunca bir kez bile kullanmadığı bir kelime". Ayrıca altın renk `p4_acilis_1`'de hasat rengi (paneller "olgun buğday" rengine sararıyor), `son_koruyucu`'da ise kurtuluş işareti (paneller altına dönüyor). Bilinçli bir ters çevirme değilse anlamı bulandırıyor.
*Öneri:* Birini değiştir. Koruyucu'da renk değişimini "maviden beyaza" yap ya da ters çevirmeyi tek cümleyle açık et.

**F3 · P2 · küçük — Paralel sahnelerde art arda aynı açılış.**
Perde 2 kronikleri "Dönüş günü…/Kadim gemisi göründüğünde…", Perde 3 kronikleri "Filonun göründüğü gece…". Bkz. D3.

### G. Tarama araçlarının işaretledikleri

**G1 · P2 · küçük — `buyuk_ihanet_kule` ¶7–8 (ve benzer çiftler) birebir aynı metin: oyuncuya görünen bir hata değil.**
Çiftler birbirini dışlayan koşullar taşıyor (`bayrak_yok: thessar_ihanete_hazir` ↔ `bayrak: [thessar_ihanete_hazir, ihanet_thessar]`); YAML'da VEYA bu yolla kurulmuş. Aynı kalıp `buyuk_ihanet_iz`'deki üç "boru sesi" paragrafında ve `kule_a/b/c` seçeneklerinde de var. Asıl risk bakım: biri düzeltilip diğeri unutulursa iki dalda farklı metin kalır.
*Öneri:* Koşul DSL'ine `herhangi: [...]` (VEYA) ekle ya da aracın bu çiftleri "dışlayan koşullu kopya" diye ayrı raporlamasını sağla. Kısa vadede çiftlerin üstüne `# OR-kopya` yorumu koy.

**G2 · P2 · küçük — `content/awakenings.yaml` › `son_nefes`, `kirilma_kemik_kabuk`, `kirilma_yanki_damari` (ve `kirilma_kara_kan`): düz "Yankı"/"Echo".**
Satırlar: `"Yankı fısıldıyor: \"Ölmeyi reddettin…\""`, `"Yankı: sen dayandın…"`, `"Yankı: artık birlikte görüyoruz…"`. DECISIONS #128 tırnağı ve büyük harfi kasıtlı sayıyor ama düz adı değil. Ayrıca `son_nefes`'in `yanki_bagi` koşulu yok: Yankı silindikten sonra (`yanki_silindi`) ilk kez tetiklenirse ölü bir ses konuşur. Kırılma uyanışları Perde 1'e bağlı olduğundan orada risk yok.
*Öneri:* `{npc:yanki}` belirtecine geç. Uyanış satırlarının belirteç çözümlemesinden geçtiğini doğrula; geçmiyorsa motorda `satirlar` için `resolver` çağrısı ekle. `son_nefes`'teki Yankı satırını `yanki_bagi` koşuluna bağla ya da satır başına koşul desteği yoksa satırı Sistem sesine çevir.

**G3 · P1 · küçük — `esik_itaat` yapısal olarak tetiklenemiyor (simülatörde %0'ın nedeni).**
Motor (`NpcEngine.thresholds`): yalnızca **yoldaşlarda** korku ≥ 60, cesaret ≤ 3 ve sadakat ≥ 6 ise OBEY, değilse FLEE. Yoldaşlar: Aras (cesaret 9), Deniz (7), Elif (6), Mert (cesaret 2, sadakat 5). Cesareti ≤ 3 olan tek yoldaş Mert, o da sadakat 6'nın altında. Ona `can_borcu` (+2 sadakat) veren bir etki yok; `korku_izi` (−2 cesaret) Deniz'i 5'e, Elif'i 4'e indiriyor, ikisi de eşiğin üstünde kalıyor. Metin de kendi içinde sorunlu: "Ne istersen" sahnesi kısa ve Yankı sezgisi büyük harfli (C2).
*Öneri:* Kural bilinçli olarak kalsın, ama kapıyı aç: (a) Mert'in sadakatini 6'ya çek, ya da bir olayda Mert'e `can_borcu` izini ver (kurtarma sahnesi); (b) ya da OBEY koşulunu `sadakat ≥ 6 VEYA bağlılık ≥ 40` yap. Ardından `esik_itaat_mert_kilic` yaz: Mert'in sesine uygun ("Ne dersen. Kaydetmeyeceğim bile.").

### H. Simülatörün en nadir 10 olayı: neden nadir?

| Olay | Pay | Koşul / kaynak | Yargı |
| --- | ---: | --- | --- |
| `p3_firtina_olcucu` | 2,0% | `yalniz_tohum`; tek tohum kaynağı (`71_perde3.yaml:4106`, tek bir seçenek) | Kasıtlı (seçim sonucu). %2 çok düşükse ikinci bir tohum kaynağı eklenebilir. |
| `kaya_soguk_firin` | 5,1% | Tohum koşulu `kaya_hapiste` + Kaya hayatta + Perde 3 | Kasıtlı; dar ama anlamlı dal. |
| `thessar_kirik_muhur` | 6,2% | 2 tohum; Thessar hayatta + `ihanet_thessar` yok + 12–20 gün gecikme | Kasıtlı. Gecikme Perde sonuna taşıyorsa kaybolabilir; Perde 4'e kayınca hâlâ uygun mu, kontrol et. |
| `fener_yavrulari` | 11,9% | Perde 2'de yumurta seçiminden tohum | Kasıtlı. |
| `deniz_yukselen_rakip` | 14,5% | Deniz Yükselenler'de ve hayatta | Kasıtlı: Deniz'in trajik rakip yolu. |
| `deniz_anma_kardes` | 17,1% | `npc_olu: deniz_arman`, ana_yay Gün 95 | Kasıtlı; Deniz'in ölüm oranını yansıtıyor. |
| `deniz_kardes_cizimi` | 17,4% | 6 tohum kaynağı, `kardes_*` bayrakları | Kasıtlı. |
| `deniz_anma_ayna` | 18,2% | `npc_olu: deniz_arman`, Gün 225 | Kasıtlı. |
| `pala_yargilanmasi` | 18,9% | 5 tohum, Elif hayatta | Kasıtlı. |
| `kadimler_5_teslim` | 19,8% | Kadimler zinciri başladı + `antlasma_nushasi` eşyası, Gün 331–356 | Kasıtlı (zincir sonu). |

Hiçbiri hata gibi görünmüyor; hepsi seçim ya da ölüm sonucu. Not: `deniz_anma_*` iki olayı ~%17–18 ile Deniz'in ölüm oranını gösteriyor; bu, GDD'deki "oyuncuyu kurtarmak için ölen kahraman" kaderinin sağlıklı bir payda olduğunu doğruluyor.

**En sık 10 (ilk sahneler):** her biri farklı mekân ve gerilimle açılıyor (ateş başı, enkaz, yayın, sıralama, öncü, çekirdek merdiveni, istila, son masa). İlk sahnelerde iskelet tekrarı yok; sorun bunların toplamı (F1). `sistem_cekirdegi` ve `cekirdege_yol`'da iki gün arayla neredeyse aynı yoldaş tepki seti var (F1'e dahil).

### I. Diğer P2 notları

- **P2 · küçük — Çift sezgi öneki.** Motor stat koşullu sezgilere öneki kendisi ekliyor (`StoryEngine.insightPrefix`). Perde 2–4 içeriğinde ~95 sezgi metni "[ALG] …"/"[KAR] …"/"[ZEK] …" ile başlıyor; oyuncu "[ALG] [ALG] …" görüyor (dosyalar: `50_perde2_ana` 25, `61_kiyikent_b` 26, `71_perde3` 22, `80_perde4` 18, `52_lonca_kaya` 2, diğer 2). `yol_olusumu_1`'de koşul `wis`, metin "[ALG]"/"[PER]": ekranda "[BİL] [ALG] …" çıkıyor. **Bu bulgu görünürlüğü nedeniyle P1 sayıldı (13 P1'den biri).** *Öneri:* Metin başındaki `[XXX] ` öneklerini toplu sil; validator'a "stat koşullu sezgi metni `[` ile başlamasın" kuralını ekle; bir arayüz ekran görüntüsüyle doğrula.
- **P2 · küçük —** `ilk_kan` › `pencereyi_oku_ilk` yabancının akıbetini sessizce düşürüyor: `yabanci_korundu` konmuyor, `ilk_gece`'de yabancıdan hiç söz edilmiyor. Yenilgi dalında (`ilk_canavar_yenilgi`) yabancı seni kurtarıyor, ama bunu hatırlatan bir tohum yok. Bir satır ya da bir tohum ekle.
- **P2 · küçük —** `gemi_bakis` koşulsuz olarak "Kule'nin yükseldiği sabah bir cepheyi tuttuğun zaman olduğu gibi" diyor; o sahne her oyuncu için gerçekleşmemiş olabilir. Koşullu yap.
- **P2 · küçük —** `deniz_son_secim_1`'de TR `{npc:vahit}`, EN `{npc:vahit|tam}`. Zararsız ama tutarsız.
- **P2 · küçük —** `elif_konsey_oylamasi_1` Deniz: "Kardeşiminki daha da düşüktü" (geçmiş zaman). Kardeş kayıp ama ölü değil; "düşük" (şimdiki) daha doğru.
- **P2 · küçük —** `p4_siralama_zirvesi` "senin adının üstünde … {npc:aras_kurt}. S kademe." BALANCE'ta oyuncu NPC'lerin ×20'si. Paragraf koşulluysa koşulun gerçekten nadir olduğundan emin ol, değilse inandırıcılık kırılıyor.
- **P2 · küçük —** "Demir Yemin / Iron Oath" (`perde3_son_ordu`) GDD eşleme tablosunda yok; Lonca'nın alt birimi ise ilk geçtiği yerde tanıt.

---

## 3. Okunamayan / araçla açılmayan örnekler

- **Kapsam notu:** Koordinatör 3. ve 4. maddeleri (sonlar, 8 NPC yay düğümü) başka bir ajana verdiğinde bu iki madde zaten okunmuştu. Onlardan çıkan bulgular (B3, B5–B7, C1, C3–C5, E1, F2, G2'nin bir kısmı) rapordan çıkarılmadı; öbür ajanın bulgularıyla çakışabilir, birleştirirken tekrarlar ayıklanmalı.

- Tüm örnekler `tools/qa/olay.py` ile açıldı; açılamayan olay ya da son olmadı. `--son hasat`, `son_hasat`'ı da içerdiği için `son_hasat` iki kez basıldı (zararsız).
- Araç **bildirimlerin `kosul` alanını göstermiyor**. Bu yüzden `ilk_sabah` ve `son_vassal`'daki "çift bildirim" görüntüsü ham YAML'dan kontrol edildi (koşullar birbirini dışlıyor, hata yok). Araca bildirim koşulu yazdırma eklenmeli.
- Uzun olaylar (`buyuk_ihanet`, 1071 satır; `son_karar`, 632; NPC yay düğümleri, 190–360) **kısmen** okundu: ilk sahneler, dallanma noktaları ve sonuç sahneleri okundu. `buyuk_ihanet`'in Kaya/Thessar/Deniz/Aras alt dalları ve `mert_kadim_teklifi`'nin son ~300 satırı ayrıntılı okunmadı.
- B3 (Yükseliş'te Deniz çakışması) ve A2 (sınıf teklifi zamanı) çıkarıma dayanıyor; simülatörde ya da tek bir kayıtla doğrulanmalı.
