# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 25 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 135 | 13.5% |
| demir_taht | 68 | 6.8% |
| yukselis | 71 | 7.1% |
| kod_kirici | 193 | 19.3% |
| ozgurluk | 77 | 7.7% |
| vassal | 115 | 11.5% |
| yalniz_avci | 86 | 8.6% |
| hasat | 196 | 19.6% |
| dongu | 59 | 5.9% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (50) | 56.8% |
| dongu | 88 | dongu (59) | 67.0% |
| kod_kirici | 87 | kod_kirici (68) | 78.2% |
| koruyucu | 88 | koruyucu (40) | 45.5% |
| ozgurluk | 87 | ozgurluk (77) | 88.5% |
| rastgele | 300 | hasat (127) | — |
| vassal | 87 | vassal (75) | 86.2% |
| yalniz_avci | 87 | yalniz_avci (58) | 66.7% |
| yukselis | 88 | yukselis (52) | 59.1% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 15.9% ×0.78 · dongu 48.9% ×0.80 · kod_kirici 49.4% ×0.82 · koruyucu 47.7% ×0.75 · ozgurluk 14.9% ×0.83 · rastgele 41.7% ×0.78 · vassal 13.8% ×0.79 · yalniz_avci 39.1% ×0.77 · yukselis 11.4% ×0.76

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (34): `HasFlag(flag=lonca_hakimiyeti)` 34
- **dongu** (16): `HasFlag(flag=yanki_son_sirri)` 16 · `VarAtLeast(key=kule_kat, value=100)` 9
- **kod_kirici** (15): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 9 · `LacksFlag(flag=yanki_silindi)` 8
- **koruyucu** (47): `HasFlag(flag=kadimler_yenildi)` 46 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **ozgurluk** (3): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 3
- **vassal** (3): `VarAtLeast(key=kadim_tehdit, value=70)` 3 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (23): `GradeAtLeast(grade=S)` 13 · `CompanionsAtMost(value=0)` 12 · `HasFlag(flag=uye_yok)` 2
- **yukselis** (25): `QuestIs(quest=zincir_yukselenler, status=DONE)` 22 · `NpcAlive(npc=vahit)` 12

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 10 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 30 | 40 | E |  |
| 90 | 43 | 29 | 65 | 86 | D |  |
| 120 | 62 | 47 | 83 | 106 | D |  |
| 150 | 91 | 73 | 110 | 121 | C |  |
| 180 | 132 | 128 | 169 | 185 | C | 34 |
| 210 | 193 | 182 | 268 | 350 | B |  |
| 240 | 282 | 248 | 365 | 437 | B |  |
| 270 | 411 | 334 | 477 | 552 | A |  |
| 300 | 599 | 444 | 618 | 735 | A |  |
| 330 | 874 | 491 | 730 | 800 | A | 51 |
| 360 | 874 | 605 | 1109 | 1389 | S |  |
| 365 | 874 | 605 | 1109 | 1389 | S | 60 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 68.4% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 18.6%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.9 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.1 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×18.7 (en güçlü NPC ortalaması Sv. 97)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 74.0%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 24 | 50 |
| 60 | 0 | 15 | 58 |
| 90 | 0 | 41 | 77 |
| 120 | 0 | 24 | 50 |
| 150 | 0 | 7 | 36 |
| 180 | 0 | 12 | 47 |
| 210 | 32 | 69 | 93 |
| 240 | 11 | 59 | 92 |
| 270 | 5 | 63 | 94 |
| 300 | 0 | 72 | 92 |
| 330 | 0 | 12 | 29 |
| 360 | 22 | 70 | 89 |
| 365 | 21 | 70 | 89 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 317; yere düşülen (yazılmış yenilgi ya da bayılma) 11.7%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 394.
- Hiç görülmeyen olay: 6 / 526: `atlama_meslek`, `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `kaya_soguk_firin` 4.5%, `fener_yavrulari` 4.9%, `yozlasma_50` 6.5%, `p3_firtina_olcucu` 7.0%, `thessar_kirik_muhur` 7.3%, `bl15_us_beyaz_kapi` 8.2%, `bl15_pk_kamp_aras` 9.5%, `sb14_mert_salon` 9.7%, `yozlasma_75` 10.4%, `sb14_elif_kayip_plan` 11.4%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `p4_son_kapi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_heykel` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_siralama_zirvesi` 100.0%.
- Bitiş seviyesi: p10 605, p50 1109, p90 1389.
