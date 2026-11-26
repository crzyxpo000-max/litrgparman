# CLAUDE.md — Yükseliş Protokolü / Ascension Protocol

Android için tek oyunculu, metin tabanlı, iki dilli (TR / EN-NA) bir LitRPG.
Tasarımın tamamı: `docs/GDD.md` (Türkçe, mekanik ve hikayede ana kaynak) ve `docs/GDD.en.md`
(Kuzey Amerika yerelleştirmesi; İngilizce isim ve terimlerde ana kaynak). Her işten önce ikisini oku.
Verilen tasarım kararları: `docs/DECISIONS.md`. Belirsiz bir noktada varsayım yap ve oraya yaz.

## Durum

- **Hikaye yapısı değişikliği (bölüm, üs/görev, konum, son tarih; `docs/CHAPTERS.md`, `docs/STORY_FLOW.md`, DECISIONS #130–#139) bitti.**
- **Aşama 9 (denge ve yayın) bitti; imzalama anahtarı ve Play Console kullanıcıda.** Aşama 0–7 tamam. Aşama 7: Perde 2–4 ve sonlar (`docs/PERDE2_4.md` içerik sözleşmesi: perdeler, bölgeler, ortak bayraklar, son koşulları, partiler).
- Oyuncu gözüyle inceleme (`docs/INCELEME_AKIS.md`) ve Paket 1–9 (`docs/UI_REVIEW.md`, DECISIONS #151–#196) tamamlandı; açık maddeler ve önerilen sıra `DEVIR.md`'de.
- Aşama 8 kaldırıldı. Aşama 6: Perde 1 dikey dilimi (`docs/PERDE1.md`; 48 olay, yeni açılış, zindanlar).
- Aşama 5 tamam. Aşama 5: NPC sistemi (`npc/`: 4 eksen, hafıza, gündem, eşikler, ölüm, yoldaşlar, düello,
  Küresel Sıralama, yoldaş görüşleri, güce tepkiler, bilgi günlüğü, yalanlar, izler, Ordu modeli).
- Aşama 0–4: Aşama 4: içerik YAML, olay havuzu, Hikaye Yönetmeni, tohumlar, gün döngüsü, bölgeler/kapılar,
  fraksiyon matrisi, bayrak kaydı, görevler, OP MC (Dikkat, gizli/görünen seviye, efsane, zar, gösteriş, fırsat eli).
- Aşama 3: savaş (`combat/`, 20 düşman, savaş ekranı).
- **Aşama 0, 1 ve 2:** Motor, kayıt (v2), i18n, içerik doğrulayıcı, Compose ekranları, prolog demosu;
  Sistem mekanikleri (eğilim, sınıf, kademe/evrim, yetenek, unvan, meslek, Öz, Günlük Görev) ve OP MC çekirdeği.
- **Yapay zeka anlatıcı yok** (DECISIONS #97): oyun tamamen çevrimdışı; tüm metin önceden yazılmış içerikten gelir. Aşama 8 kaldırıldı.
- **Tasarımın üst kuralı:** GDD'deki "OP MC tasarımı" bölümü (DECISIONS #19). Kahraman bir anomalidir; Yankı ona bağlıdır.
- **Tüm geliştirme planı ve kabul kriterleri:** `CLAUDE_CODE_GOREV.md`. Güncel durum: `docs/PROGRESS.md`.

## Komutlar

```bash
./gradlew :engine:test             # motor testleri (JUnit 4)
./gradlew :engine:validateContent  # content/ doğrulaması
./gradlew assembleDebug            # APK: app/build/outputs/apk/debug/
./gradlew installDebug             # bağlı telefona/emülatöre kur
./gradlew lint                     # Android lint (hata kalmamalı)
./gradlew :tools:simulator:run --args="1000"   # denge simülatörü → docs/BALANCE.md
./gradlew bundleRelease            # AAB (keystore.properties yoksa imzasız)
./gradlew :tools:uslup:run         # Türkçe üslup işi: HEAD'e göre yalnız tr değişti mi + üslup ölçümü (--ayrinti, --olcum, --ref)
./gradlew :tools:tarama:run        # içerik taraması → docs/TARAMA.md (QA; docs/QA.md)
./gradlew :tools:simulator:perf    # içerik yükleme / kayıt boyutu ölçümü
./gradlew :tools:simulator:akis --args="20 tools/qa/akis/iz_sonra"   # olay sırası kaydı; çözümleme: python tools/qa/akis/cozumle.py <klasör>
scripts/check.sh                   # test + validateContent + simülatör denge testi + assembleDebug + lint; her aşamanın sonunda geçmeli
```

Sürümler: Gradle 9.1, AGP 8.13.2, Kotlin 2.2.21, JDK 25 (Android Studio JBR). `local.properties` git dışıdır.

## Araçlar (.claude/)

- **Alt ajanlar** (`.claude/agents/`): `tr-editor` (Türkçe üslup, salt okur), `en-localizer` (Kuzey Amerika İngilizcesi ve
  GDD.en isimleri, salt okur), `continuity-checker` (bölüm akışı, bayraklar, NPC durumları; salt okur), `balance-analyst`
  (simülatörü çalıştırıp önceki BALANCE.md ile karşılaştırır). Rapor verirler; düzeltmeyi ana oturum yapar.
- **Yetenekler** (`.claude/skills/`): `/olay-yaz <bölüm> <konum> [konu]` (şemaya uygun iki dilli olay + doğrulama + iki
  inceleme), `/oyun-testi [ekran]` (installDebug + adb ekran görüntüleri + inceleme), `/denge [n]` (balance-analyst özeti),
  `/kalite` (check.sh + hata özeti ve öneriler).
- **Kancalar** (`.claude/settings.json`, betikler `.claude/hooks/`): `content/` altında bir YAML düzenlenince
  `:engine:validateContent` çalışır, hata varsa Claude'a bildirilir; oturum dururken kod/içerik değişmiş ama
  `docs/PROGRESS.md` değişmemişse bir kez hatırlatılır. Kancalar hızlıdır: tam derleme ya da test içermez.

## Mimari (değişmez kurallar)

- `engine/` saf Kotlin'dir; Android'e **asla** bağımlı olmaz. Tüm oyun mantığı burada.
- `app/` yalnızca gösterir: Compose ekranları + `GameViewModel`. Oyun kuralı app'e yazılmaz.
- `WorldState` tek doğruluk kaynağıdır ve değişmezdir; yalnızca `story/Effects.kt` içindeki etkilerle ve
  `story/PlayerActions.kt` eylemleriyle değişir. Geçersiz eylem `ActionRejected` fırlatır.
- **Sistem istemleri** (`SystemPrompt` kuyruğu): sınıf teklifi, evrim, meslek, yetenek evrimi, kaynaştırma, Öz bağlama.
  Motor tetikleyicilerle (`EffectApplier.checkTriggers`) ekler; istem bekliyorken hikaye seçimi yapılamaz.
- Motorun ürettiği Sistem metinleri `content/system.yaml`'dadır; anahtarlar ve yer tutucular `SystemTexts.REQUIRED`'da.
- Tüm rastgelelik `core/Rng.kt` (SplitMix64) üzerinden; durumu `WorldState.rngState` içinde.
- Motor arayüze dili çözülmüş görünüm modelleri verir (`SceneView`, `StatusView`, `NoticeView`).
- Kayıt biçimi `save/SaveCodec.kt` içinde, sürümlü (`VERSION`). Alan eklerken varsayılan ver; alan
  silerken/yeniden adlandırırken `VERSION`'ı artır ve `decode` içinde geçiş yaz. Eski kayıtlar açılmalı. Kayıtlar JSON'dur.
- `kotlinx.serialization` **derleyici eklentisi kullanılmıyor**; JSON `JsonElement` API'siyle elle okunup yazılıyor.

## Paketler (engine)

| Paket | İçerik |
| --- | --- |
| `core` | `WorldState`, `PlayerState`, `Morality`, `GameClock`, `Rng` |
| `system` | `Stat`, `Stats`, `Grade`, `Progression` (GDD formülleri + OP MC: büyüme çarpanı, yüzdelik), `Rarity`, `Affinity`, içerik tanımları (`Defs.kt`) |
| `i18n` | `Lang`, `LText`, `NameRegistry`, `TokenResolver`, `TurkishSuffix`, `EnglishGrammar` |
| `story` | İçerik modeli (`Model.kt`), `EffectApplier` (etkiler + tetikleyiciler), `PlayerActions` (istem, dağıtım, üretim, eşya, antrenman), `StoryEngine`, görünümler (`Views.kt`), `SystemTexts` |
| `content` | `ContentParser` (YAML → `YamlTree`), `ContentLoader`, `ContentCache` (ikili `content.bin` önbelleği), `ContentValidator` (+ `main`) |
| `combat` | `CombatModel` (düşman/uyanış tanımları, `CombatState`, duruş, durumlar), `CombatMath` (GDD formülleri), `CombatEngine` (tur, yapay zeka, fazlar, infaz, zafer, ölüm, Ezme) |
| `world` | `WorldModel` (olay/bölge/fraksiyon/başarı/görev tanımları, `RegionState`, `Gate`, `Seed`, `DayState`), `Chapters` (bölüm tablosu, evre, görev), `Director` (puanlama, konum kuralı, tohumlar, fırsat eli), `WorldEngine` (gün döngüsü, üs/görev akışı, bölüm ilerlemesi, son tarih ve taşma, akış eylemleri, kapılar) |
| `npc` | `NpcModel` (kişilik, ilişki, `NpcDef`, `NpcState`, hafıza, iz/bilgi/ordu/sıralama tanımları), `NpcEngine` (etkiler, günlük gündem, eşikler, tanık yorumu, görüşler, güce tepki, Küresel Sıralama, düello) |
| `save` | `SaveCodec` (v4: kronik), `SaveManager` (auto + 3 slot, Demir Mod), `SaveStorage` |

## İçerik kuralları (content/)

- Kayıt parçaları: `flags/`, `quests/`, `knowledge/`, `feats/` klasörlerindeki dosyalar ana dosyayla birleşir (aynı kimlik iki kez = hata).
- Olay dosyaları: `events/10_yoldaslar`, `20_orman`, `30_carsi_istasyon`, `40_ana_yay`, `90_esikler`.
- Yerleşim: `names.yaml`, `titles.yaml`, `system.yaml`, `classes.yaml`, `skills.yaml`, `items.yaml`,
  `professions.yaml`, `essences.yaml`, `daily.yaml`, `story/*.yaml` (dosya adı sırasıyla). APK'ya assets olarak girer.
- `story/90_sistem.yaml`: kesinti sahneleri (`ceza_bolgesi`); dönüş `"sonraki": "@geri"`.
- **Bölüm yapısı** (`chapters.yaml`; DECISIONS #130–#139): `uslar` (perde → üs bölgesi) ve `bolumler` (`no, perde, acilis,
  son_tarih, bitis, erken, baslik, hedef, tehdit, brifing[], operasyon[] ya da [{ad, bolge, olaylar}], darbogaz[],
  darbogaz_gun, tasma, tasma_etkiler, geri_sayim {tr,en} ({n}), hazirlik [{id, ad, aciklama, sonuc, bedel, kosul, operasyonda}]`).
  Tarihli (`erken: false`) operasyonun en az 2 hazırlığı olmalı; bayrak `hazirlik_<id>` (motor öneki), operasyon sahnesinde
  ek seçenek `goster: { bayrak: "hazirlik_<id>" }`. Kayıt yuvası `operasyon`: operasyon öncesi otomatik kayıt. Her olayın **`konum`** etiketi vardır: `us`, bölge kimlikleri ya da `*`; yeni olay
  yazarken zorunlu. Üste yalnız `us` ve üs bölgesindeki olaylar gelir; başka bölgede geçen olay görev kartı olur.
  Yapısal sahneler `events/07–09_bolumler_*.yaml`: `brifing_<n>`, `darbogaz_<n>`, `varis_<bölge>` (yalnız bölgeye ilk gidişte;
  `ziyaret_<bölge>` değişkeni sayar; sonraki gidişler tek satır bildirim, DECISIONS #161). Yolculuk eylem harcamaz (#162); Perde 1'de el 2 kart (#163). Tohum etkisinde **`hatirlatma {tr,en}`** (gecikme ≥ 3 ise yaz):
  doğan olayın ilk paragrafı olur. Koşullar `bolum`, `bolum_min`, `bolum_max`, `gorevde`, `gorev_turu: operasyon|yan|kesif`.
  Erken girilebilen operasyonda kesin gün sayısı yazma. Bayrak öneki `bolum_` motorundur (`bolum_<n>_tasti`).
  **Saha kampı sahnesi** (`kamp: true`, DECISIONS #144, #146): `konum` bölge(ler), `npc` kampa gelen kişi(ler) (yoldaş ya da
  güveni ≥ 10, tanışılmış, hain olmayan müttefik); keşif görevinde o bölgede gün sonu Kamp'ında oynanır (havuz/tohum/üs değil).
  **Kule kat anı** (DECISIONS #148): `etiketler: [kule_an, …]`, `konum: [kule_dibi]`, `yalniz_tohum`, `tekrar`, perde yok, bant
  `kosul: { degisken_min/max: { kule_kat } }`; ilk temizleyen `kule_ilk_<kat>` (15/35/55/75/95). Motor: `WorldEngine.towerMoment`. Ölçüt "hikayesiz gün": `tools/qa/akis/serbest.py`, `bolge_acik.py`.
- Belirteç kipleri: EN `{yer:x|b}` cümle başında büyük harf, `{fraksiyon:sessiz_ag|k}` "The"yi küçültür; EN'de "the
  {fraksiyon:siginak}" / "the {fraksiyon:lonca}". TR'de `names.yaml iyelik: true` olan bileşik ad n'li ek alır (Kule Dibi'ne).
- Sınıflar: her eğilime 1 `temel`; `gizli` sınıfların `kosul` + `egilimler` (Yankı'nın önerisi) alanı; temel/gizli
  sınıfa tam 2 `evrim` (kademe E). Yetenekler: her eğilime tam 1 `benzersiz` yetenek (`egilim` alanıyla).
- Anahtarlar Türkçe ASCII: `sahneler`, `paragraflar`, `bildirimler`, `secenekler`, `etiket`, `gerekli`,
  `goster`, `etkiler`, `sonraki`, `kosul`, `son`, `yer`.
- Oyuncuya görünen **her** metin `{ "tr": ..., "en": ... }`. İngilizce çeviri değil, Kuzey Amerika
  yerelleştirmesidir (birimler: mil/lb/°F; deyimler; kültürel referanslar). Eşleme tablosu GDD'de.
- Metne **asla isim yazma**; belirteç kullan:
  - `{npc:deniz_arman}` kısa ad (Deniz / Devon), `{npc:deniz_arman|tam}` tam ad
  - Türkçe hal ekleri: `|e` `|i` `|in` `|de` `|den` `|le` → Deniz'e, Kaya'nın, Aras'tan
  - İngilizce iyelik: `|pos` → Devon's
  - `{yer:kiyikent}`, `{oyuncu}` (oyuncu adı, ek alabilir: `{oyuncu|in}`)
- Etkiler: `bayrak`, `bayrak_kaldir`, `degisken`, `ahlak` (merhamet/duzen/insanlik), `itibar`, `xp`, `gun`, `stat`, `unvan`,
  `egilim` {eğilim: n}, `yetenek` id, `yetenek_xp` {id: n}, `esya` {id: n}, `oz_kristali` n (tek para birimi; `kredi` kaldırıldı), `oz` {öz: n},
  `gunluk` {antrenman|kesif|savas|kapi: n}, `can` n, `mana` n, `arayuz` yetenekler|canta|harita. (`meslek_teklifi` yazma: meslek kapalı.)
- Koşullar: `stat`, `bayrak`, `bayrak_yok`, `degisken_min`, `ahlak_min`, `ahlak_max`, `seviye_min`, `egilim_min`,
  `yetenek`, `unvan`, `sinif`, `esya`, `itibar_min`. Birden çok anahtar = VE.
- Savaş: seçenekte `{"savas": {"dusmanlar": [...], "ortam": [tavan|su|yanici|karanlik|dar_gecit], "kazan": sahne, "kaybet": sahne?, "kac": sahne?}}`;
  `sonraki` gerekmez. Düşmanlar `enemies.yaml` (`dusmanlar`), uyanışlar `awakenings.yaml` (`uyanislar`). Saldırı metninde `{dusman}` = düşman adı.
  Türkçe savaş metinlerinde dinamik isme ek getirme ("{target}'e" yok); ismi özne yap.
- Koşul `degisken_max` da var. Savaş sonrası bağlam değişkenleri: `_savas_kazanildi`, `_savas_en_dusuk_can`, `_savas_kademe_farki`, `_savas_boss`, `_savas_ezme`.
- `story/90_sistem.yaml`: `son_hasat` (Demir Mod ölümü).
- Olaylar `events/*.yaml` (`olaylar` listesi): `id, perde, bolge, gun{min,max}, kosul, agirlik, etiketler, gerilim 1–4, npc, onemli,
  yalniz_tohum, ana_yay{gun}, tekrar, kart, sahne, sahneler`. Olayın son seçenekleri `"sonraki": "@akis"`.
- Diğer dünya dosyaları: `regions.yaml` (`baslangic`, `bolgeler`), `factions.yaml` (ilişki matrisi), `feats.yaml` (tanıklı başarılar),
  `quests.yaml`, `flags.yaml` (**her bayrak burada tanımlı olmalı**; motorun koyduğu `yenildi_ uyanis_ olay_ dikkat_esik_ _red_ _zar_` önekleri hariç).
- Yeni etkiler: `tohum {olay, gecikme{min,max}, kosul}`, `dikkat n`, `gizle n`, `aciga_vur true`, `efsane id`,
  `gorev {id: basla|tamam|basarisiz}`, `bolge id`, `tehlike {bolge: n}`. Yeni koşullar: `dikkat_min/max`, `oz_kristali_min`,
  `gizli_fark_min`, `bolge`, `perde`, `gun_min/max`, `gorev {id: durum}`, `itibar_max`. Belirteç `{fraksiyon:id}`.
- Seçenek alanları: `tur: gizle|aciga_vur|gosteris`, `zar {stat, zorluk, tek, basarisiz, basarisiz_etkiler}`.
- `validateContent` uyarıları (⚠) derlemeyi durdurmaz: set edilmeyen bayrak, ulaşılamayan tohum olayı, ahlak oynatmayan önemli seçim.
- **NPC'ler (Aşama 5):** `npcs/<id>.yaml` (şema: `npcs/deniz_arman.yaml`): kisilik (cesaret/hirs/vicdan/sadakat/supheci),
  degerler, baslangic_iliski (guven/saygi/korku/baglilik), hedefler, egilim, seviye, hiz, fraksiyon, yoldas, siralama, bilinen,
  yaninda (koşul), savas {rol, stat, statlar, artis, saldiri, metin ({npc},{dusman}), duello_metin ({dusman})}, yay_dugumleri,
  ilk_unvanlar, gorusler (merhamet+ … insanlik-), guc_tepkileri (hayranlik/endise/kiskanclik/bagimlilik), hafizalar, kisisel_odul.
  Ayrıca `traits.yaml` (izler), `knowledge.yaml` (bilgiler), `army.yaml` (birlikler), `ranking.yaml` (avcılar, sıra tepkileri),
  `factions.yaml` `degerler`. Her olayın `npc` listesindeki NPC'nin dosyası olmalı; ölen NPC'nin olayları gelmez.
- NPC etkileri: `iliski {npc: {guven: n…}}`, `hafiza {npc, id, agirlik}`, `npc_ol`, `yoldas_katil`, `yoldas_ayril`, `npc_fraksiyon`,
  `tanis`, `npc_seviye`, `npc_iz`, `npc_isaret`, `iz` (oyuncu), `bilgi`, `yalan {npc, konu}`, `kisisel_gorev {npc: durum}`, `npc_odul`,
  `ordu {birlik: n}`, `ordu_kazan n`; `tohum` içinde `npc`. Koşullar: `npc_hayatta`, `npc_olu`, `yoldas`, `iliski_min/max`,
  `npc_fraksiyon`, `npc_isaret`, `npc_seviye_min`, `hafiza {npc: id}`, `bilgi`, `yoldas_yer`, `gorunen_seviye_min/max`, `iz`, `npc_savasabilir`.
- NPC kimliği `"@"` = olayın odak NPC'si (tohumun NPC'si ya da olayın ilk `npc`'si); metinde `{npc:@}`, `{npc_hedef:@}`.
- İngilizce zamir: `{zamir:<npc|@>|o|onu|onun|kendi}` (he/him/his/himself …, `|b` büyük harf) ve fiil uyumu `|s` (say/says),
  `|is`, `|has`, `|does`, `|was`; zamir `names.yaml` `zamir` alanından (Deniz "they"). Türkçe metinde kullanma (boş döner).
  `npc_gitti` etkisi NPC'yi "gitti" durumuna alır (eşikleri kapanır). Düello düşmanı `npc:<id>` / `npc:@`.
- Eşik olayları: genel `events/90_esikler.yaml` (`esik_ihanet|kisisel|kisisel_son|kacis|itaat|rakip|ayrilik|yalan`, `tekrar` + `yalniz_tohum`);
  NPC'ye özel sürüm `esik_<tur>_<npc>` yazılırsa onun yerine gelir. Sahnede `yoldas_tepkileri: [{npc, metin, kosul}]`.
- Sistem metni yer tutucusu olarak kimliksiz `{npc}` serbesttir (belirteç değil).
- **Yankı'nın sesi:** Yankı'nın kendi sözleri her yerde küçük harfle ve tırnaksız yazılır (fısıltı); anlatı "{npc:yanki}" ile atfeder.
  Yankı adı da her yerde belirteçle (`{npc:yanki|e}` → Yankı'ya). Doğrulayıcı düz yazılmış NPC adlarını uyarır.
  **Susturulmuş Yankı:** `yanki_susturuldu` (Bölüm 7, kapı kapalı tutulursa) ya da `yanki_silindi` varken motor `bayrak: yanki_bagi`
  koşulunu yanlış sayar (`WorldState.hasFlag`); Yankı'nın her sözü `bayrak: yanki_bagi` ile yazılır, ek koşul gerekmez.
  `bayrak_yok: yanki_bagi` paragrafı "kafanın içi sessiz" anlamına gelir. Bağın kurulup kurulmadığını sormak gerekirse bu iki bayrağa bak.
- Sezgide `anlati: true`: Sistem/Yankı sesi olmayan koşullu anlatı satırı; paragrafların sonuna düz metin olarak eklenir.
- Koşul `lider_fraksiyon: f` (lonca/sessiz_ag/siginak/yukselenler arasında en yüksek itibar; eşik `itibar_min` ile), `npc_gitti: id`,
  `bayrak`/`bayrak_yok` liste alabilir (`bayrak_yok: [a, b]` = hiçbiri). VEYA: `herhangi: [{…}, {…}]` (birini tutması yeter; kopya paragraf yazma).
- Türkçe noktalama: İngilizce kalıbı ", ve" kullanma; Türkçede "ve"den önce virgül konmaz.
- Başarısız zar (`basarisiz`) sessizce `@akis`'e dönmesin; kısa bir başarısızlık sahnesi yaz. Tohum olayı bölge/gün tanımaz:
  belirli bir yeri anlatan açılışı koşullu yaz ya da bölgeye geliş satırı ekle.
- Sahnede `sezgiler`: [{`kosul`, `metin`}] → pasif Sistem sezgisi; stat koşullu satır turkuaz çizgiyle gösterilir (önek yok; kilit etiketi "[ALG 6]" ile karışmasın), öneksiz/koşulsuz satır Yankı'nın sesidir (`InsightView.echo`).
- Bildirim türleri: sistem, durum, analiz, seviye, unvan, gorev, uyari, yetenek, evrim, sinif, `yanki` (bozuk yazı). Aynı `Notice.group` değerini taşıyan
  bildirimler (bir NPC'nin bir sahnedeki ilişki değişimleri) `StoryEngine.collapse` ile tek karta iner; ilişki satırı sayısızdır (`bag_*` metinleri).
  Savaş ödülleri (XP, Kredi, ganimet) zafer kartının satırlarıdır; infaz seçimi yalnız boss/zeki/NPC düellosunda; `savas_sayisi` değişkeni 3'te Taktik'i açar.
  Günlük Görev: ilk 3 gün deneme (`EffectApplier.PENALTY_GRACE_DAYS`); Ceza Bölgesi yalnız `WorldEngine.enterHub`'da (üs ya da keşif kampı), sahne kesmez.
  Biten bölümün boş günleri `restUntilNextChapter` ile geçer; zaman atlama Perde 2'den itibaren.
- Eğilimler: yakin_dovus, menzilli, buyu, gizlilik, koruma, cagirma, zanaat, karanlik. Fraksiyonlar: siginak, lonca, yukselenler, sessiz_ag, kadimler.
- `gerekli` karşılanmazsa seçenek görünür ama kilitlidir ve "[ALG 6]" etiketi alır; `goster` karşılanmazsa gizlenir.
- İçerik değiştikten sonra `:engine:validateContent` ve `:engine:test` çalıştır (ContentFilesTest gerçek içeriği doğrular).

- **Aşama 7:** düşman başvurusu `"id@120"` (sabit seviye), `"id@dunya"`/`"@dunya+20"` (dünya seviyesi ±%, `Progression.worldLevel`),
  `"id@oyuncu+5"` (oyuncu seviyesi ±n), `"id@oyuncu-10%"` (oyuncu seviyesinin ±%n'i, oyuncunun kademesinde; Perde 3–4'te destek ve
  yeniden deneme indirimi bununla yazılır, ±n seviye o seviyelerde etkisizdir, DECISIONS #177; ilk denemeyi yüzdeyle sertleştirme, #195); ölçekleme `combat/EnemyScaling.kt`. Perde 2+ bölge düşmanları ve kapılar dünya seviyesine ölçeklenir
  (`Progression.worldLevel`: Gün 30'da eğrinin yarısı, Gün 50'ye doğru tam eğri; DECISIONS #164).
  Etkiler `xp_seviye n` (seviye XP'sinin %n'i; Perde 2+ ödüllerinde sabit `xp` yerine), `kule_kat n`, `yol id`. Koşullar `kademe_min`,
  `ordu_min` (birlik sayısı), `yoldas_sayisi_max`, `oz`, `yol`, `son_uygun`, `hicbir_son`, `son` (epilog). Seçenekte `kronik {tr,en}`
  (son ekranındaki kritik seçim kartı; önemli olayların seçeneklerinde zorunlu) ve `kronik_agirlik`.
- Bölgede `son_perde` (bölge bu perdeden sonra ölür), `kosul` (yolculuk), `ortam`. Kule `tower.yaml` (bantlar, `hikaye_katlari`;
  hikaye katı olayları `yalniz_tohum`). Sonlar `endings.yaml` (`sonlar`, `arketipler`, `epilog.npc`, `epilog.fraksiyon`; her son
  sahnesi bir sona bağlı olmalı; tam bir `yedek` son). Yollar `essences.yaml` `yollar`.
- Parça dosyalar: `enemies/`, `items/`, `awakenings/`, `titles/` (ayrıca `flags/`, `quests/`, `knowledge/`, `feats/`).
- Eşik olayları: Dikkat `dikkat_<n>_p<perde>` (eşikler her perdede yeniden kurulur; Perde 1'de `dikkat_<n>`).
- **Paket 4 — kapatılan sistemler (DECISIONS #166–#169):** meslek/üretim ve Kredi kapalı (`Progression.PROFESSIONS_ENABLED/CREDITS_ENABLED`; kod durur,
  içerikte `kredi`/`meslek_teklifi` yazılmaz, Öz Kristali tek para birimi, ödeme seçeneği `gerekli: { oz_kristali_min: k }`). Yozlaşma yok
  (`CORRUPTION_ENABLED`): `yozlasma n` yazma, `dikkat n` yaz; ceza yetenekleri (`EffectApplier.PENALTY_ATTENTION`) ve izler (`traits.yaml dikkat`)
  Dikkat artırır; Yankı yay düğümleri `yozlasma_50/75` içerik tohumuyla `dikkat_min` koşuluna bağlı. Ordu ve Bilgi günlüğü ilk `ordu`/`bilgi`
  etkisiyle kalıcı açılır (`WorldState.UI_ARMY_VAR/UI_KNOWLEDGE_VAR`).
- New Game+: `StoryEngine.newGame(…, previousGames, newGamePlus)` → değişken `onceki_oyun`, bayrak `yeni_dongu`; meta uygulama ayarlarında.

- **Aşama 9:** `tools/simulator` (engine'i kullanan JVM aracı; `DengeTest` 200 oynanışla son dağılımı %1–%40, hakimiyet %60+ sınar).
  Gün akışından açılan savaşlarda yenilgi bayılmadır (1 can, akışa dönüş); gerçek ölüm hikaye savaşları ve bosslarda. Yenilgiden
  sonra yeniden deneme seçeneği canı yeniler ve her yenilgi sahnesinden savaşsız bir çıkış (`@akis`) olmalı.
  Bildirim olayları `events/95_bildirimler.yaml` (`bildirim_rakip|goz|kapi`, `story/Notifier.kt`).

## Arayüz notları (app/)

- Arayüz metinleri: `res/values/strings.xml` (EN) ve `res/values-tr/strings.xml` (TR). Yeni metin = iki dosyaya birden.
- Uygulama içi dil `ProvideLanguage` (App.kt) ile seçilir; cihaz dilinden bağımsız ve anında değişir.
- Türkçe büyük harf dönüşümünde yerel ayar kullan (`uppercase(locale)`): i → İ.
- Türkçe metinde sayıya ek getirme ("Aşama 4'te" sayıya göre değişir); "Aşama %d ile" gibi eksiz kalıp kullan.
- Kayıt: her seçimden ve her Sistem eyleminden sonra ve `onStop`'ta otomatik; dosyalar `files/saves/*.json`, Auto Backup kapsamında. Yükleme öncesi mevcut Normal mod oturumu `SaveManager.PREVIOUS_SESSION` (`onceki`) yuvasına alınır (DECISIONS #180).
- Kendini açan arayüz: Yetenekler ve Çanta sekmeleri `StatusView.tabs` içinde yoksa gizlidir. `StatusView.showHidden/showRank/showRelations/showMap/showArmy/showKnowledge/showMana/showGrade/showStamina/showDanger/showCrystals`
  ilgili sistem oyuna girene kadar göstergeleri gizler (DECISIONS #181); `CombatView.tacticsUnlocked` yanlışken savaş ekranı
  Saldır · Savun · Analiz · Kaç · Otomatik'e iner (Taktik, Ortam, Nefes yok), konuşulamayan düşmanda Konuş gizlidir (#182). Operasyon düğmesi onay penceresi açar (`OperationChoiceView`).
- Tam ekran kartlar `ui/system/Overlays.kt`: önce kutlama (seviye/evrim/sınıf), sonra bekleyen istem.
- Tema: Sistem · Açık · Koyu (koyu temanın zemini saf siyah; ayrı AMOLED seçeneği yok, DECISIONS #148).
- **Görsel kimlik "Kırık pencere"** (DECISIONS #147): renk/yazı/şekil/hareket tokenları yalnız `ui/theme/Theme.kt`'de
  (`Game.colors`, `GameType`, `GameShapes`, `GameMotion`, `LocalReduceMotion`). Ekranda sabit `Color(0x…)` yazma.
  Sistem penceresi `Modifier.systemWindow()`, Yankı satırı `EchoLine` (`ui/components/Window.kt`); `Button`/`OutlinedButton`
  bu paketten gelir (kesik köşe). Büyük anlar `ui/system/Overlays.kt` (motorda `Notice.moment/from/to`); hata ayıklama
  derlemesinde `adb shell am start -f 0x20000000 -n com.yukselis.protocol/.MainActivity --es demo_moment level|evolution|unique|rank|breach`.
- Daha fazla: İlişkiler (4 eksen, durum, hatıralar, izler; dokununca ayrıntı) ve Küresel Sıralama; Günlük'te bilgi günlüğü.
  Hikaye'de yoldaş görüşleri, savaşta yoldaş satırları, Durum'da sıra/ordu/izler. Bildirim türü `yoldas` (pembe).
- Gün akışında Hikaye sekmesi `HubPanel` gösterir (fırsat eli, serbest eylemler, kapılar, yolculuk, kamp); Harita ve Günlük Daha fazla'da.
  Bölüm yapısında: üste operasyon düğmesi ve görev kartları (başka bölge), görevde keşif/Kapı/Kule ve "Üsse dön"; Hikaye'nin
  üstünde her zaman `ChapterBar` ("Bölüm N · Hedef · Kalan gün"); Görevler'de bölüm hedefi yan görevlerden ayrı; dönüş özeti
  `UiState.recap` (2 günden uzun aradan sonra).
- Hikaye düzeni (DECISIONS #170; #124–127'nin sabit seçenek panelinin yerine): Hikaye yalnızca güncel sahne; **seçenekler ve fırsat
  kartları metnin altında aynı kaydırmada** (`Choices`/`ChoiceButton`: etiket üst satırda, 5'ten fazlası "Diğer seçenekler" altında;
  `HubStory`). Alttaki ince `ActionDock` yalnız rutin eylem çubuğu (`HubActionBar`), "metni göster" ve son düğmesi. Önceki sahneler alt
  sayfada. Daha fazla = karo ızgarası + alt sayfalar; Durum = 3 sekme. Semboller `components/Glossary.kt` (her ekranda (i)
  `HelpButton`, Daha fazla → Yardım); bir kerelik ipuçları `TipCard` (`Settings.seenTips`). Yazı boyutu `Settings.textSize`.
- **Türkçe üslup:** hikaye metni yazarken/düzeltirken `docs/USLUP.md` (kılavuz + parti planı ve ilerleme durumu).
- Geri alınamaz işlem onay ister (DECISIONS #171): kayıt varken Yeni oyun, dolu slota Kaydet, Yükle. Geri tuşu: alt sayfa → Hikaye sekmesi →
  "çıkmak için tekrar bas" (`GameScaffold`). Denetim etiketlerinde yazı ölçeği tavanı `CappedFontScale` (sekmeler 1.0, savaş düğmeleri ve
  logo 1.3; DECISIONS #172); okuma metnine tavan konmaz.
- Bölüm kartları ve son (DECISIONS #173): `brifing_<n>_1` sahnesinde açılış, `darbogaz_<n>_1` sahnesinde kapanış kartı (`ChapterCard`,
  sahne kimliğinden tanınır: bu adlandırmayı bozma); son ekranı tam ekrandır (üst şerit ve sekmeler gizli). TR terimler (#174): savaş kaynağı
  "Nefes" (NFS), defter karosu "Görevler"; "Dayanım" ve tek başına "Günlük" yazma. İçerik derleme zamanında ikili önbelleğe (`content.bin`, `:app:compileContentCache`, #179) derlenir; APK'da YAML yoktur, `src/main/assets`
  (lisans metinleri) pakette kalır (#196). Kuralların tek kaynağı bu dosyadır; `AGENTS.md` yalnız buraya yönlendirir.
- Arayüz incelemesi araçları: `:tools:simulator:fixtures` (anlık kayıt üretir), `tools/qa/ui/cap.py`, `s_all.py` (koşul matrisi görüntüleri →
  `docs/ui-review/`), rapor `docs/UI_REVIEW.md`.
- **Diyalog/alt sayfa:** `AlertDialog` değil `AppAlertDialog` kullan; `ModalBottomSheet` içeriğini `Localized {}` ile sar
  (yoksa pencere cihaz dilinde ve ölçeksiz yazıyla açılır).
- Savaş varken Hikaye sekmesi `CombatScreen` gösterir; savaş başında Normal modda `checkpoint` kaydı yazılır.
- Release: R8 açık (`app/proguard-rules.pro`), imzalama `keystore.properties` (git dışı). compileSdk 37, targetSdk 36.
- Bildirimler: `data/Notifications.kt` (WorkManager, kanal, izin); Ayarlar'da anahtar; `ProvideLanguage` izin kaydını taşır.
- Gün akışında Kule Dibi'nde Kule düğmesi (`HubView.tower`); kademenin altındaki katlar varsa "Hızlı tırmanış" düğmesi (`TowerView.fastTo`). Son sahnesinde adım adım epilog kartları (`EndingView`: son, OP MC türü, kritik 5 seçim, NPC'ler,
  fraksiyonlar; `Tümünü göster` seçeneğiyle, DECISIONS #185). Bölüm kapanış kartı (`ChapterLine.highlights`, `nextTitle`) o bölümdeki kritik seçimleri ve sonraki bölüm adını gösterir.
- Bitirilen oyunlar, Döngü (New Game+) ve kalıcı Arşiv (Sonlar, OP MC türleri, Gizli sınıflar, Benzersiz yetenekler; `StoryEngine.archive`) `SettingsStore` metasında; başlıkta Yeni Oyun+ anahtarı ve Arşiv sayfası (#186). Savaşta boss faz çentikleri, Analiz zaaf vurgusu, hızlı otomatik savaş ayarı (`Settings.fastCombat`), ekransız anında Ezme ve katmanlı titreşim (`Haptics.kt`, #183–#186).

## Test notları

- Motor testleri JUnit 4 ile; test adları Türkçe olabilir.
- Yeni her formül veya kural için test yaz; GDD'deki sayılar testlerde sabitlenir (`ProgressionTest`).
