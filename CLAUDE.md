# CLAUDE.md — Yükseliş Protokolü / Ascension Protocol

Android için tek oyunculu, metin tabanlı, iki dilli (TR / EN-NA) bir LitRPG.
Tasarımın tamamı: `docs/GDD.md` (Türkçe, mekanik ve hikayede ana kaynak) ve `docs/GDD.en.md`
(Kuzey Amerika yerelleştirmesi; İngilizce isim ve terimlerde ana kaynak). Her işten önce ikisini oku.
Verilen tasarım kararları: `docs/DECISIONS.md`. Belirsiz bir noktada varsayım yap ve oraya yaz.

## Durum

- **Aşama 9 (denge ve yayın) bitti; imzalama anahtarı ve Play Console kullanıcıda.** Aşama 0–7 tamam. Aşama 7: Perde 2–4 ve sonlar (`docs/PERDE2_4.md` içerik sözleşmesi: perdeler, bölgeler, ortak bayraklar, son koşulları, partiler).
- Aşama 8 kaldırıldı. Aşama 6: Perde 1 dikey dilimi (`docs/PERDE1.md`; 48 olay, yeni açılış, zindanlar).
- Aşama 5 tamam. Aşama 5: NPC sistemi (`npc/`: 4 eksen, hafıza, gündem, eşikler, ölüm, yoldaşlar, düello,
  Küresel Sıralama, yoldaş görüşleri, güce tepkiler, bilgi günlüğü, yalanlar, izler, Ordu modeli).
- Aşama 0–4: Aşama 4: içerik YAML, olay havuzu, Hikaye Yönetmeni, tohumlar, gün döngüsü, bölgeler/kapılar,
  fraksiyon matrisi, bayrak kaydı, görevler, OP MC (Dikkat, Yozlaşma, gizli/görünen seviye, efsane, zar, gösteriş, fırsat eli).
- Aşama 3: savaş (`combat/`, 20 düşman, savaş ekranı).
- **Aşama 0, 1 ve 2:** Motor, kayıt (v2), i18n, içerik doğrulayıcı, Compose ekranları, prolog demosu;
  Sistem mekanikleri (eğilim, sınıf, kademe/evrim, yetenek, unvan, meslek, Öz, Günlük Görev) ve OP MC çekirdeği.
- **Yapay zeka anlatıcı yok** (DECISIONS #97): oyun tamamen çevrimdışı; tüm metin önceden yazılmış içerikten gelir. Aşama 8 kaldırıldı.
- **Tasarımın üst kuralı:** GDD'deki "OP MC tasarımı" bölümü (DECISIONS #19). Kahraman bir anomalidir; Yankı ona bağlıdır.
- **Tüm geliştirme planı ve kabul kriterleri:** `CLAUDE_CODE_GOREV.md`. Güncel durum: `docs/PROGRESS.md`.

## Ayrı uygulama: Novel EPUB (`scraper/`)

Oyundan bağımsız küçük bir Android uygulaması (webnovel → EPUB): `:scraper:core` (saf Kotlin/JVM) ve `:scraper:app` (Compose).
Oyun kuralları ona uygulanmaz; ayrıntı `scraper/README.md`. Komutlar: `./gradlew :scraper:core:test :scraper:app:assembleDebug`.

## Ayrı uygulama: Zincir (`zincir/`)

Oyundan bağımsız kişisel antrenman/beslenme takip uygulaması (Compose + Room, çevrimdışı, arayüz Türkçe). Kendi Gradle
projesidir; kök derlemeye dahil **değildir** ve oyun kuralları ona uygulanmaz. Ayrıntı `zincir/README.md`.
Komutlar: `cd zincir && ./gradlew test assembleDebug lintDebug` (`local.properties` içinde `sdk.dir` gerekir).
Saf kurallar `mantik/` (testli), tablolar `veri/`, ekranlar `ui/`; program verisi `BaslangicVerisi.kt` ile veritabanına yazılır.
Kurallar (kullanıcının): sıra 14 adımlık dairedir ve hafta hesabı kullanmaz, yalnızca "Adım" gün bitince kendiliğinden geçer, atlama
yalnızca Kardiyo/Adım'da; zincir uyarısı son ağırlık seansından 3/5 tam gün sonra; sayaç son 7 gün; haftalık ortalama/özet Pzt–Paz;
kayıtlar `LocalDate` (epochDay) ile; zamanlayıcılar `elapsedRealtime` farkıyla (dinlenme: tam alarm, germe: ön plan servisi);
grafik kütüphanesi eklenmez (Canvas); Health Connect okuması isteğe bağlıdır (`saglik/`, varsayılan kapalı, yalnızca doldurur:
adımda büyük olan, uyku boşsa; saat kalorisi yalnızca bilgi, kalori hedefine/hakkına dokunmaz). Tablo şeması değişirse `Veritabani.version` artırılıp migration yazılır (telefonda veri var).

## Komutlar

```bash
./gradlew :engine:test             # motor testleri (JUnit 4)
./gradlew :engine:validateContent  # content/ doğrulaması
./gradlew assembleDebug            # APK: app/build/outputs/apk/debug/
./gradlew installDebug             # bağlı telefona/emülatöre kur
./gradlew lint                     # Android lint (hata kalmamalı)
./gradlew :tools:simulator:run --args="1000"   # denge simülatörü → docs/BALANCE.md
./gradlew bundleRelease            # AAB (keystore.properties yoksa imzasız)
scripts/check.sh                   # test + validateContent + simülatör denge testi + assembleDebug + lint; her aşamanın sonunda geçmeli
```

Sürümler: Gradle 9.1, AGP 8.13.2, Kotlin 2.2.21, JDK 25 (Android Studio JBR). `local.properties` git dışıdır.

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
| `content` | `ContentParser` (YAML → `YamlTree`), `ContentLoader`, `ContentValidator` (+ `main`) |
| `combat` | `CombatModel` (düşman/uyanış tanımları, `CombatState`, duruş, durumlar), `CombatMath` (GDD formülleri), `CombatEngine` (tur, yapay zeka, fazlar, infaz, zafer, ölüm, Ezme) |
| `world` | `WorldModel` (olay/bölge/fraksiyon/başarı/görev tanımları, `RegionState`, `Gate`, `Seed`, `DayState`), `Director` (puanlama, zorunlu olaylar, fırsat eli), `WorldEngine` (gün döngüsü, akış eylemleri, kapılar) |
| `npc` | `NpcModel` (kişilik, ilişki, `NpcDef`, `NpcState`, hafıza, iz/bilgi/ordu/sıralama tanımları), `NpcEngine` (etkiler, günlük gündem, eşikler, tanık yorumu, görüşler, güce tepki, Küresel Sıralama, düello) |
| `save` | `SaveCodec` (v4: kronik), `SaveManager` (auto + 3 slot, Demir Mod), `SaveStorage` |

## İçerik kuralları (content/)

- Kayıt parçaları: `flags/`, `quests/`, `knowledge/`, `feats/` klasörlerindeki dosyalar ana dosyayla birleşir (aynı kimlik iki kez = hata).
- Olay dosyaları: `events/10_yoldaslar`, `20_orman`, `30_carsi_istasyon`, `40_ana_yay`, `90_esikler`.
- Yerleşim: `names.yaml`, `titles.yaml`, `system.yaml`, `classes.yaml`, `skills.yaml`, `items.yaml`,
  `professions.yaml`, `essences.yaml`, `daily.yaml`, `story/*.yaml` (dosya adı sırasıyla). APK'ya assets olarak girer.
- `story/90_sistem.yaml`: kesinti sahneleri (`ceza_bolgesi`); dönüş `"sonraki": "@geri"`.
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
  `egilim` {eğilim: n}, `yetenek` id, `yetenek_xp` {id: n}, `esya` {id: n}, `kredi` n, `oz_kristali` n, `oz` {öz: n},
  `gunluk` {antrenman|kesif|savas|kapi: n}, `meslek_teklifi` true, `can` n, `mana` n, `arayuz` yetenekler|canta|harita.
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
- Yeni etkiler: `tohum {olay, gecikme{min,max}, kosul}`, `dikkat n`, `gizle n`, `yozlasma n`, `aciga_vur true`, `efsane id`,
  `gorev {id: basla|tamam|basarisiz}`, `bolge id`, `tehlike {bolge: n}`. Yeni koşullar: `dikkat_min/max`, `yozlasma_min`,
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
- Sezgide `anlati: true`: Sistem/Yankı sesi olmayan koşullu anlatı satırı; paragrafların sonuna düz metin olarak eklenir.
- Koşul `lider_fraksiyon: f` (lonca/sessiz_ag/siginak/yukselenler arasında en yüksek itibar; eşik `itibar_min` ile), `npc_gitti: id`,
  `bayrak`/`bayrak_yok` liste alabilir (`bayrak_yok: [a, b]` = hiçbiri).
- Türkçe noktalama: İngilizce kalıbı ", ve" kullanma; Türkçede "ve"den önce virgül konmaz.
- Başarısız zar (`basarisiz`) sessizce `@akis`'e dönmesin; kısa bir başarısızlık sahnesi yaz. Tohum olayı bölge/gün tanımaz:
  belirli bir yeri anlatan açılışı koşullu yaz ya da bölgeye geliş satırı ekle.
- Sahnede `sezgiler`: [{`kosul`, `metin`}] → pasif Sistem sezgisi; stat koşullu satır "[ALG]" öneki alır, öneksiz satır Yankı'nın sesidir.
- Bildirim türleri: sistem, durum, analiz, seviye, unvan, gorev, uyari, yetenek, evrim, sinif, `yanki` (bozuk yazı).
- Eğilimler: yakin_dovus, menzilli, buyu, gizlilik, koruma, cagirma, zanaat, karanlik. Fraksiyonlar: siginak, lonca, yukselenler, sessiz_ag, kadimler.
- `gerekli` karşılanmazsa seçenek görünür ama kilitlidir ve "[ALG 6]" etiketi alır; `goster` karşılanmazsa gizlenir.
- İçerik değiştikten sonra `:engine:validateContent` ve `:engine:test` çalıştır (ContentFilesTest gerçek içeriği doğrular).

- **Aşama 7:** düşman başvurusu `"id@120"` (sabit seviye), `"id@dunya"`/`"@dunya+20"` (dünya seviyesi ±%, `Progression.worldLevel`),
  `"id@oyuncu+5"` (oyuncu seviyesi ±n); ölçekleme `combat/EnemyScaling.kt`. Perde 2+ bölge düşmanları ve kapılar dünya seviyesine ölçeklenir.
  Etkiler `xp_seviye n` (seviye XP'sinin %n'i; Perde 2+ ödüllerinde sabit `xp` yerine), `kule_kat n`, `yol id`. Koşullar `kademe_min`,
  `ordu_min` (birlik sayısı), `yoldas_sayisi_max`, `oz`, `yol`, `son_uygun`, `hicbir_son`, `son` (epilog). Seçenekte `kronik {tr,en}`
  (son ekranındaki kritik seçim kartı; önemli olayların seçeneklerinde zorunlu) ve `kronik_agirlik`.
- Bölgede `son_perde` (bölge bu perdeden sonra ölür), `kosul` (yolculuk), `ortam`. Kule `tower.yaml` (bantlar, `hikaye_katlari`;
  hikaye katı olayları `yalniz_tohum`). Sonlar `endings.yaml` (`sonlar`, `arketipler`, `epilog.npc`, `epilog.fraksiyon`; her son
  sahnesi bir sona bağlı olmalı; tam bir `yedek` son). Yollar `essences.yaml` `yollar`.
- Parça dosyalar: `enemies/`, `items/`, `awakenings/`, `titles/` (ayrıca `flags/`, `quests/`, `knowledge/`, `feats/`).
- Eşik olayları: Dikkat `dikkat_<n>_p<perde>` (eşikler her perdede yeniden kurulur; Perde 1'de `dikkat_<n>`), Yozlaşma `yozlasma_<n>` (bir kez).
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
- Kayıt: her seçimden ve her Sistem eyleminden sonra ve `onStop`'ta otomatik; dosyalar `files/saves/*.yaml`, Auto Backup kapsamında.
- Kendini açan arayüz: Yetenekler ve Çanta sekmeleri `StatusView.tabs` içinde yoksa gizlidir.
- Tam ekran kartlar `ui/system/Overlays.kt`: önce kutlama (seviye/evrim/sınıf), sonra bekleyen istem.
- Tema: Sistem · Açık · Koyu · AMOLED (saf siyah).
- Daha fazla: İlişkiler (4 eksen, durum, hatıralar, izler; dokununca ayrıntı) ve Küresel Sıralama; Günlük'te bilgi günlüğü.
  Hikaye'de yoldaş görüşleri, savaşta yoldaş satırları, Durum'da sıra/ordu/izler. Bildirim türü `yoldas` (pembe).
- Gün akışında Hikaye sekmesi `HubPanel` gösterir (fırsat eli, serbest eylemler, kapılar, yolculuk, kamp); Harita ve Günlük Daha fazla'da.
- Kaydırmayı azaltan düzen (DECISIONS #124–127): Hikaye yalnızca güncel sahne + alttaki sabit `ActionDock`; önceki sahneler alt
  sayfada. Daha fazla = karo ızgarası + alt sayfalar; Durum = 3 sekme. Semboller `components/Glossary.kt` (her ekranda (i)
  `HelpButton`, Daha fazla → Yardım); bir kerelik ipuçları `TipCard` (`Settings.seenTips`). Yazı boyutu `Settings.textSize`.
- **Diyalog/alt sayfa:** `AlertDialog` değil `AppAlertDialog` kullan; `ModalBottomSheet` içeriğini `Localized {}` ile sar
  (yoksa pencere cihaz dilinde ve ölçeksiz yazıyla açılır).
- Savaş varken Hikaye sekmesi `CombatScreen` gösterir; savaş başında Normal modda `checkpoint` kaydı yazılır.
- Release: R8 açık (`app/proguard-rules.pro`), imzalama `keystore.properties` (git dışı). compileSdk 37, targetSdk 36.
- Bildirimler: `data/Notifications.kt` (WorkManager, kanal, izin); Ayarlar'da anahtar; `ProvideLanguage` izin kaydını taşır.
- Gün akışında Kule Dibi'nde Kule düğmesi (`HubView.tower`). Son sahnesinde epilog kartları (`EndingView`: son, OP MC türü, NPC'ler,
  fraksiyonlar, en kritik 5 seçim). Bitirilen oyunlar ve Döngü (New Game+) `SettingsStore` metasında; başlıkta Yeni Oyun+ anahtarı.

## Test notları

- Motor testleri JUnit 4 ile; test adları Türkçe olabilir.
- Yeni her formül veya kural için test yaz; GDD'deki sayılar testlerde sabitlenir (`ProgressionTest`).
