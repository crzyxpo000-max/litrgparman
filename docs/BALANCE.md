# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 27 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 142 | 14.2% |
| demir_taht | 78 | 7.8% |
| yukselis | 57 | 5.7% |
| kod_kirici | 190 | 19.0% |
| ozgurluk | 82 | 8.2% |
| vassal | 119 | 11.9% |
| yalniz_avci | 88 | 8.8% |
| hasat | 183 | 18.3% |
| dongu | 61 | 6.1% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (63) | 71.6% |
| dongu | 88 | dongu (61) | 69.3% |
| kod_kirici | 87 | kod_kirici (65) | 74.7% |
| koruyucu | 88 | hasat (40) | 44.3% |
| ozgurluk | 87 | ozgurluk (81) | 93.1% |
| rastgele | 300 | hasat (108) | — |
| vassal | 87 | vassal (78) | 89.7% |
| yalniz_avci | 87 | yalniz_avci (57) | 65.5% |
| yukselis | 88 | yukselis (49) | 55.7% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 9.1% ×0.78 · dongu 46.6% ×0.81 · kod_kirici 50.6% ×0.82 · koruyucu 45.5% ×0.78 · ozgurluk 9.2% ×0.80 · rastgele 44.0% ×0.77 · vassal 10.3% ×0.82 · yalniz_avci 39.1% ×0.80 · yukselis 9.1% ×0.74

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (18): `HasFlag(flag=lonca_hakimiyeti)` 18
- **dongu** (21): `HasFlag(flag=yanki_son_sirri)` 20 · `VarAtLeast(key=kule_kat, value=100)` 15
- **kod_kirici** (15): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 8 · `LacksFlag(flag=yanki_silindi)` 8
- **koruyucu** (48): `HasFlag(flag=kadimler_yenildi)` 48 · `ReputationAtLeast(faction=siginak, value=50)` 3
- **vassal** (2): `VarAtLeast(key=kadim_tehdit, value=70)` 1 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (26): `HasFlag(flag=uye_yok)` 14 · `GradeAtLeast(grade=S)` 8 · `CompanionsAtMost(value=0)` 5
- **yukselis** (32): `QuestIs(quest=zincir_yukselenler, status=DONE)` 27 · `NpcAlive(npc=vahit)` 16

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 29 | 40 | E |  |
| 90 | 43 | 25 | 58 | 81 | D |  |
| 120 | 62 | 43 | 80 | 101 | D |  |
| 150 | 91 | 69 | 108 | 121 | C |  |
| 180 | 132 | 121 | 163 | 184 | C | 33 |
| 210 | 193 | 174 | 253 | 322 | B |  |
| 240 | 282 | 238 | 357 | 411 | B |  |
| 270 | 411 | 335 | 471 | 553 | A |  |
| 300 | 599 | 445 | 603 | 727 | A |  |
| 330 | 874 | 510 | 726 | 800 | A | 49 |
| 360 | 874 | 607 | 1097 | 1371 | S |  |
| 365 | 874 | 607 | 1097 | 1371 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 64.3% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 16.3%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.9 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×14.8 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×19.2 (en güçlü NPC ortalaması Sv. 86)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 63.4%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 15 | 51 |
| 90 | 0 | 33 | 70 |
| 120 | 0 | 27 | 55 |
| 150 | 0 | 10 | 40 |
| 180 | 0 | 15 | 92 |
| 210 | 27 | 64 | 92 |
| 240 | 0 | 59 | 92 |
| 270 | 3 | 60 | 93 |
| 300 | 0 | 84 | 96 |
| 330 | 0 | 24 | 62 |
| 360 | 2 | 66 | 91 |
| 365 | 2 | 66 | 91 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 291; yere düşülen (yazılmış yenilgi ya da bayılma) 11.0%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 357.
- Hiç görülmeyen olay: 5 / 461: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `hasat_vakti`.
- En nadir 10 olay: `bl12_kd_kamp_elif` 2.1%, `kaya_soguk_firin` 4.9%, `fener_yavrulari` 6.3%, `sb14_mert_salon` 6.9%, `thessar_kirik_muhur` 7.0%, `bl15_us_beyaz_kapi` 7.5%, `p3_firtina_olcucu` 8.1%, `sb14_elif_kayip_plan` 10.2%, `vapur_ilk_sefer` 13.5%, `esik_itaat` 13.5%.
- En sık 10 olay: `son_karar` 100.0%, `p4_veda_mektuplari` 100.0%, `cekirdege_yol` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_korfez_leviatani` 100.0%, `p3_siralama_zirvesi` 100.0%.
- Bitiş seviyesi: p10 607, p50 1097, p90 1371.
