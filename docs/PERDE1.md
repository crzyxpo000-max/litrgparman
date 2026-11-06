# Perde 1 — Öğretici Katman (Gün 0–30) içerik tasarımı

Aşama 6'nın ortak sözleşmesi. İçerik ajanları ve incelemeciler bunu okur. Kurallar: `CLAUDE.md` (içerik kuralları),
`CLAUDE_CODE_GOREV.md` (Aşama 6 kabul kriterleri, yazım üslubu), `docs/GDD.md` (OP MC tasarımı, Dinamik karakterler).
**Kullanıcı kuralı: hikaye kusursuz olmalı; içerikte asla kısaltma, tasarruf yok.**

## OP MC güç döngüsü (F kademesi)

| Evre | Gün | İçerik |
| --- | --- | --- |
| Mücadele | 1–11 | Hayatta kalma, gerçek savaşlar, kıt kaynak, ilk ahlak sınavları. Oyuncu henüz sıradan görünür. |
| Kırılma | 12 (ana yay `kirilma`) | Yazılmış uyanış anı: birini büyük bedelle korumak. Bayrak `kirilma_yasandi`, oyuncuya bir iz (bedelli mutasyon). |
| Hakimiyet | 12–24 | Oyuncu çevresini açıkça geride bırakır: gösteriş seçenekleri, Ezme, [Gizle]/[Açığa vur], fraksiyonlar onu ister. Olayların çoğu buradadır. |
| Yeni tavan | 24 (ana yay `yeni_tavan`) | Bir üst kademeden (E) bir şey ölçeği sıfırlar; ufukta insan olmayan bir gözlemci. |
| Final | 28 (ana yay `ogretici_bekci`) | Öğretici Bekçisi; Perde 1 sonu dünya durumunu anlatır. |

Hakimiyet olayları `kosul: { bayrak: "kirilma_yasandi" }` ya da `gun: { min: 12 }` ile açılır ve gösteriş (`tur: gosteris` + stat `gerekli`), `gizle`/`aciga_vur` seçenekleri içerir.

## Dünya durumu (Perde 1 sonunda en az 3 belirgin sonuç)

- **Hayatta kalan oranı:** değişken `hayatta_kalan` (yüzde, açılışta 50'ye ayarlanır). Bir grubu kurtarmak +2…+6, terk etmek/öldürmek −2…−8.
  Son: ≤ 35 düşük, 36–64 orta, ≥ 65 yüksek.
- **Deniz'in akıbeti:** yoldaş ve hayatta (`yoldas: deniz_arman`) · Yükselenler'e gitti (bayrak `deniz_yukselenlere_gitti`, fraksiyonu `yukselenler`) ·
  öldü (`npc_olu: deniz_arman`, finalde onu kurtarmak için) · hiç bulunamadı (bayrak yok `deniz_bulundu` ve `deniz_el_tutuldu`).
- **İlk fraksiyon eğilimi:** Öğretici'deki ilk kamplar: Sığınak (`siginak`, Elif'in sahra hastanesi), Demir Lonca'nın çekirdeği (`lonca`,
  Kaya'nın silahlı avcıları), Yükselenler (`yukselenler`, Vahit'in vaazları), Sessiz Ağ (`sessiz_ag`, Mert'in istasyondaki radyocuları).
  İtibar `itibar` etkisiyle; son sahne en yüksek olanı anlatır.

## Olay listesi (perde: [1]) — kim yazar

Her olay **en az 3 seçenek** (giriş sahnesinde), olayın içinde **en az bir koşullu** (`gerekli` ya da `goster`), **en az bir tohum ekleyen**
(`tohum`) ve **en az bir NPC ilişki eksenini değiştiren** (`iliski`) seçenek bulundurur. Önemli (`onemli: true`) olaylarda her seçenek
bir ahlak eksenini oynatır. Tohum yalnızca bu listedeki olaylara ya da kendi partinin `yalniz_tohum` olaylarına ekilir.

### Parti A — Yoldaş yayları (`events/10_yoldaslar.yaml`, bayraklar `flags/10_yoldaslar.yaml`)
| Olay | Gün | Bölge | Not |
| --- | --- | --- | --- |
| `deniz_bulusma` | 1–20 | orman | `bayrak_yok: deniz_el_tutuldu`; Deniz tazılarla; her yolda bulunur ve katılır (`yoldas_katil`), `deniz_bulundu`, görev `deniz_bul` tamam. |
| `deniz_hesap` | tohum | — | Beklediysen; yalan seçeneği `yalan: {npc: deniz_arman, konu: deniz_plan_yalani}`. |
| `kamp_ates_basi` | 3–30 | — | `yoldas: deniz_arman`; nefes olayı (gerilim 1), "eski hayat". |
| `deniz_kardes_ipucu` | 8–20 | istasyon | Deniz'in kız kardeşinin izi (bir duvara kazınmış not); Deniz yoldaşsa. |
| `deniz_son_secim` | ana yay 22 | — | `kosul: { yoldas: deniz_arman }`. Vahit'in vaizleri Deniz'e kardeşini bulma vaadiyle gelir. Sonuçlar: kalır · Yükselenler'e gider (`npc_fraksiyon: {deniz_arman: yukselenler}`, `npc_gitti`, bayrak `deniz_yukselenlere_gitti`) · kalır ama yaralı güven (iz). Gücün ona ne yaptığını tartışın ("canavara ya da tanrıya dönüşen arkadaş"). |
| `istasyon_sinyali` | 5–20 | istasyon | Yankı yayı 1. |
| `istasyon_sinyali_2` | tohum | — | Yankı yayı 2. |
| `yanki_bozuk_satir` | 12–26 | — | Yankı yayı 3: kendisinin Sistem'den koparılmış bir parça olduğunu ima eder; `bilgi: yanki_parcasi`. |
| `rakip_iz` | 4–14 | çarşı/istasyon | Aras'la tanışma. |
| `aras_ilk_yaris` | 9–22 | — | "İlk" unvanları yarışı; kim önce. |
| `aras_sira_meydan` | 15–27 | — | Sıralamada üst üste; düello seçeneği (`savas: {dusmanlar: ["npc:aras_kurt"], …}`). |

### Parti B — Orman ve genel (`events/20_orman.yaml`, `flags/20_orman.yaml`)
| Olay | Gün | Bölge | Not |
| --- | --- | --- | --- |
| `yarali_avci` | 2–30 | orman/istasyon | Yaralı bir avcı; ilaç/yardım/yağma. |
| `avci_haberi` | tohum | — | Avcının borcu. |
| `yabanci_geri_donus` | tohum | — | Açılışta bırakılan yabancı döner (açılış ekiyor; ad yok, "yabancı"). |
| `yabanci_minnet` | tohum | — | Açılışta kurtarılan yabancı iyiliği öder. |
| `kayip_cocuk` | 2–16 | orman | Sisin içinde kaybolmuş bir çocuk. |
| `zehirli_bereket` | 3–20 | orman | Bol yiyecek bulunur; paylaşmak/saklamak; [ALG] zehirli olanı görür. |
| `isaretli_agaclar` | 4–22 | orman | Ağaçlara kazınmış oklar: tuzak mı yardım mı. |
| `gece_baskini` | 6–26 | herhangi | Gece kampa saldırı; kimi koruyacaksın. |
| `siralama_gunu` | 6–10 | herhangi | Sistem ilk Küresel Sıralama'yı herkese ilan eder; gizlenmek/göze girmek. `bilgi: sirala_maske` verebilir. |
| `sahte_seviye` | 12–26 | çarşı | Seviyesini şişirip insanları sömüren biri; [ALG] gerçek seviyesini görürsün (hakimiyet). |

### Parti C — Çarşı, İstasyon ve ilk kamplar (`events/30_carsi_istasyon.yaml`, `flags/30_carsi_istasyon.yaml`)
| Olay | Gün | Bölge | Not |
| --- | --- | --- | --- |
| `carsi_mahsurlar` | 4–26 | çarşı | Kepenklerin ardında mahsur grup (`carsi_mahsurlari` görevi). |
| `cop_avcisi_pazar` | 3–30 | çarşı | Takas tezgâhı. |
| `carsi_cetesi` | 8–24 | çarşı | Hayatta kalanlardan "geçiş ücreti" alan çete. |
| `eczane` | 5–22 | çarşı | Son ilaçlar: Elif'in hastanesi mi, kendin mi, satmak mı. |
| `kayip_esya_ofisi` | 6–28 | çarşı | Ölülerin eşyaları ve pencereleri; bir sır. |
| `son_tren` | 7–25 | istasyon | Rayların üstünde hâlâ hareket eden bir vagon. |
| `su_baskini` | 10–26 | istasyon | Su basan tünelde mahsur kalanlar. |
| `olu_avci_penceresi` | 9–27 | herhangi | Ölen bir avcının açık kalmış Sistem penceresi; ganimet ve gizli satır. |
| `kaya_ilk_tanisma` | 5–18 | orman/çarşı | Kaya'nın silahlı avcıları (Lonca çekirdeği); `tanis: kaya_demir`. |
| `elif_sahra_hastanesi` | 4–18 | çarşı | Elif'in hastanesi (Sığınak çekirdeği); `tanis: elif_sari`. Elif yoldaş olabilir. |
| `vahit_vaaz` | 7–20 | herhangi | Vahit'in vaazı (Yükselenler çekirdeği); `tanis: vahit`. |
| `mert_sifreli_mesaj` | 8–22 | istasyon | Mert ve radyocuları (Sessiz Ağ çekirdeği); `tanis: mert_kilic`. Mert yoldaş olabilir. |

### Parti D — Ana yay ve zindanlar (`events/40_ana_yay.yaml`, `flags/40_ana_yay.yaml`, `awakenings.yaml`)
| Olay | Gün | Not |
| --- | --- | --- |
| `kirilma` | ana yay 12 | Yazılmış uyanış anı. Deniz yoldaşsa onu, değilse kayıp çocukları/yabancıları korumak; bedel: can (yazılmış `kaybet` sahnesi) ve kalıcı iz (`iz: kemik_kabuk` ya da `iz: yanki_damari` ya da `iz: kara_kan` seçime göre). Bayrak `kirilma_yasandi`. Yankı'nın en önemli anı. |
| `yeni_tavan` | ana yay 24 | E kademesi `kemik_tacli_geyik` ya da `kepenk_cenesi`: kaç / gizlen / Kademe Kırma dene. Sırtta insan olmayan bir gözlemci (ad verme; Kadimler'in habercisi). Bayrak `yeni_tavan_goruldu`. |
| `ogretici_bekci` | ana yay 28 | Final; yoldaşla seçenekler (Deniz'in kendini feda etmesi: `npc_ol: deniz_arman`). Son sahne `perde1_son`: hayatta kalan oranı, Deniz'in akıbeti, ilk fraksiyon eğilimi (koşullu paragraflar). Yenilgi sahnesi de. |
| `dikkat_25`, `dikkat_50` | tohum (motor) | Dikkat eşikleri: avcılar, Sistem düzeltmesi. |
| `zindan_metro` | 10–26, istasyon | Çok odalı kapı zindanı (3+ oda, her odada seçim; savaşlar `kazan` ile sonraki odaya; kaçış sahnesi). |
| `zindan_kasa` | 12–27, çarşı | İkinci çok odalı zindan (çarşının altındaki banka kasası). |

## Ortak yazım kuralları (özet)

- Türkçe: ikinci tekil şahıs, şimdiki zaman, edebi ama akıcı. İngilizce: Kuzey Amerika yerelleştirmesi, sert tür kurgusu sesi.
- Paragraf ≤ 3 cümle; sahne 2–5 paragraf; seçenek emir kipinde ≤ 60 karakter, her seçeneğin bir sonucu var.
- İsim yazma: `{npc:id}`, `{yer:id}`, `{oyuncu}`, Türkçe ekler `|e|i|in|de|den|le`, İngilizce `|pos`. İngilizce zamir:
  `{zamir:id|o}` he/she/they/it, `|onu`, `|onun`, `|kendi`, `|b` büyük harf; fiil uyumu `say{zamir:id|s}`, `{zamir:id|is}`.
  Deniz için İngilizce "they" (zamir belirteciyle). Türkçede zamir belirteci kullanma.
- Sistem bildirimleri kuru, kısa, yer yer alaycı; İngilizcede köşeli parantez.
- Sezgiler (`sezgiler`): stat koşullu "[ALG]" satırı ve Yankı'nın sesi (`kosul: {bayrak: yanki_bagi}`).
- Yoldaş satırı yazmak istersen sahneye `yoldas_tepkileri: [{npc, metin, kosul}]`.
- Düşmanlar yalnızca `content/enemies.yaml`'dakiler. Yeni unvan/eşya/yetenek/düşman ekleme; gerekirse raporda öner.
- Bayraklar kendi `flags/<parti>.yaml` dosyana (`bayraklar:` altında, açıklamalı). Görev gerekirse `quests/<parti>.yaml`, bilgi `knowledge/<parti>.yaml`.
- Telif: Defiance of the Fall, He Who Fights With Monsters, The Primal Hunter, Solo Leveling'den hiçbir metin, isim, kavram adı kopyalanmaz.
