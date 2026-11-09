# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 24 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 132 | 13.2% |
| demir_taht | 73 | 7.3% |
| yukselis | 68 | 6.8% |
| kod_kirici | 215 | 21.5% |
| ozgurluk | 71 | 7.1% |
| vassal | 123 | 12.3% |
| yalniz_avci | 78 | 7.8% |
| hasat | 188 | 18.8% |
| dongu | 52 | 5.2% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (54) | 61.4% |
| dongu | 88 | dongu (52) | 59.1% |
| kod_kirici | 87 | kod_kirici (67) | 77.0% |
| koruyucu | 88 | koruyucu (36) | 40.9% |
| ozgurluk | 87 | ozgurluk (71) | 81.6% |
| rastgele | 300 | hasat (118) | — |
| vassal | 87 | vassal (82) | 94.3% |
| yalniz_avci | 87 | yalniz_avci (56) | 64.4% |
| yukselis | 88 | yukselis (54) | 61.4% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 11.4% ×0.79 · dongu 60.2% ×0.82 · kod_kirici 50.6% ×0.82 · koruyucu 40.9% ×0.78 · ozgurluk 21.8% ×0.82 · rastgele 40.3% ×0.79 · vassal 8.0% ×0.81 · yalniz_avci 44.8% ×0.81 · yukselis 11.4% ×0.76

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (25): `HasFlag(flag=lonca_hakimiyeti)` 25
- **dongu** (20): `HasFlag(flag=yanki_son_sirri)` 20 · `VarAtLeast(key=kule_kat, value=100)` 10
- **kod_kirici** (15): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 12 · `LacksFlag(flag=yanki_silindi)` 2 · `NpcAlive(npc=mert_kilic)` 1
- **koruyucu** (52): `HasFlag(flag=kadimler_yenildi)` 52 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **ozgurluk** (7): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 7
- **vassal** (3): `VarAtLeast(key=kadim_tehdit, value=70)` 2 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (28): `CompanionsAtMost(value=0)` 13 · `HasFlag(flag=uye_yok)` 10 · `GradeAtLeast(grade=S)` 9
- **yukselis** (20): `QuestIs(quest=zincir_yukselenler, status=DONE)` 18 · `NpcAlive(npc=vahit)` 7

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 29 | 40 | E |  |
| 90 | 43 | 27 | 58 | 82 | D |  |
| 120 | 62 | 47 | 79 | 102 | D |  |
| 150 | 91 | 74 | 107 | 122 | C |  |
| 180 | 132 | 125 | 163 | 185 | C | 34 |
| 210 | 193 | 174 | 258 | 333 | B |  |
| 240 | 282 | 244 | 358 | 429 | B |  |
| 270 | 411 | 336 | 477 | 556 | A |  |
| 300 | 599 | 474 | 615 | 747 | A |  |
| 330 | 874 | 548 | 734 | 806 | A | 51 |
| 360 | 874 | 640 | 1099 | 1371 | S |  |
| 365 | 874 | 640 | 1099 | 1371 | S | 59 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 66.2% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 16.6%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.8 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.2 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×18.5 (en güçlü NPC ortalaması Sv. 96)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 65.6%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 15 | 52 |
| 90 | 0 | 32 | 70 |
| 120 | 0 | 28 | 55 |
| 150 | 0 | 10 | 41 |
| 180 | 0 | 18 | 92 |
| 210 | 29 | 66 | 91 |
| 240 | 0 | 59 | 92 |
| 270 | 6 | 63 | 95 |
| 300 | 4 | 84 | 96 |
| 330 | 0 | 24 | 62 |
| 360 | 2 | 67 | 90 |
| 365 | 2 | 67 | 90 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 289; yere düşülen (yazılmış yenilgi ya da bayılma) 10.5%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 389.
- Hiç görülmeyen olay: 5 / 519: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `kaya_soguk_firin` 4.8%, `fener_yavrulari` 6.5%, `sb14_mert_salon` 6.8%, `thessar_kirik_muhur` 7.4%, `bl15_us_beyaz_kapi` 7.8%, `p3_firtina_olcucu` 8.1%, `sb14_elif_kayip_plan` 9.2%, `sb6_deniz_balkon` 12.8%, `sb15_deniz_kalkan` 13.0%, `esik_itaat` 13.6%.
- En sık 10 olay: `son_karar` 100.0%, `cekirdege_yol` 100.0%, `kadim_istilasi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `perde4_acilis` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_heykel` 100.0%, `p3_siralama_zirvesi` 100.0%, `p3_ellinciler` 100.0%.
- Bitiş seviyesi: p10 640, p50 1099, p90 1371.
