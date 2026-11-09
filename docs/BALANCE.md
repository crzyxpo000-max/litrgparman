# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 22 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 123 | 12.3% |
| demir_taht | 74 | 7.4% |
| yukselis | 61 | 6.1% |
| kod_kirici | 178 | 17.8% |
| ozgurluk | 76 | 7.6% |
| vassal | 123 | 12.3% |
| yalniz_avci | 92 | 9.2% |
| hasat | 205 | 20.5% |
| dongu | 68 | 6.8% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (52) | 59.1% |
| dongu | 88 | dongu (68) | 77.3% |
| kod_kirici | 87 | kod_kirici (62) | 71.3% |
| koruyucu | 88 | hasat (37) | 39.8% |
| ozgurluk | 87 | ozgurluk (76) | 87.4% |
| rastgele | 300 | hasat (133) | — |
| vassal | 87 | vassal (79) | 90.8% |
| yalniz_avci | 87 | yalniz_avci (58) | 66.7% |
| yukselis | 88 | yukselis (47) | 53.4% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 12.5% ×0.79 · dongu 56.8% ×0.82 · kod_kirici 51.7% ×0.80 · koruyucu 40.9% ×0.76 · ozgurluk 8.0% ×0.85 · rastgele 43.3% ×0.75 · vassal 19.5% ×0.81 · yalniz_avci 44.8% ×0.76 · yukselis 9.1% ×0.77

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (28): `HasFlag(flag=lonca_hakimiyeti)` 28
- **dongu** (14): `HasFlag(flag=yanki_son_sirri)` 14 · `VarAtLeast(key=kule_kat, value=100)` 8
- **kod_kirici** (18): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 12 · `LacksFlag(flag=yanki_silindi)` 8
- **koruyucu** (52): `HasFlag(flag=kadimler_yenildi)` 52
- **ozgurluk** (2): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 1 · `MoralityAtMost(axis=ORDER, value=-60)` 1
- **vassal** (3): `VarAtLeast(key=kadim_tehdit, value=70)` 3 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (26): `GradeAtLeast(grade=S)` 15 · `CompanionsAtMost(value=0)` 12 · `HasFlag(flag=uye_yok)` 3
- **yukselis** (35): `QuestIs(quest=zincir_yukselenler, status=DONE)` 34 · `NpcAlive(npc=vahit)` 14

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 10 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 16 | 31 | 40 | E |  |
| 90 | 43 | 30 | 64 | 86 | D |  |
| 120 | 62 | 49 | 83 | 106 | D |  |
| 150 | 91 | 73 | 109 | 121 | C |  |
| 180 | 132 | 126 | 168 | 185 | C | 34 |
| 210 | 193 | 181 | 268 | 342 | B |  |
| 240 | 282 | 247 | 366 | 440 | B |  |
| 270 | 411 | 329 | 476 | 550 | A |  |
| 300 | 599 | 440 | 623 | 737 | A |  |
| 330 | 874 | 490 | 729 | 800 | A | 52 |
| 360 | 874 | 575 | 1121 | 1385 | S |  |
| 365 | 874 | 575 | 1121 | 1385 | S | 60 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 68.7% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 18.5%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.9 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.0 (en güçlü NPC ortalaması Sv. 71)
  - Gün 365: ×18.9 (en güçlü NPC ortalaması Sv. 96)
- **Kule:** ortalama en yüksek kat 93; 100'e ulaşan 73.7%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 2 | 17 | 43 |
| 60 | 0 | 19 | 56 |
| 90 | 0 | 36 | 74 |
| 120 | 0 | 24 | 46 |
| 150 | 0 | 3 | 30 |
| 180 | 0 | 10 | 51 |
| 210 | 29 | 65 | 92 |
| 240 | 0 | 58 | 92 |
| 270 | 7 | 62 | 94 |
| 300 | 0 | 72 | 92 |
| 330 | 0 | 10 | 29 |
| 360 | 12 | 68 | 88 |
| 365 | 12 | 68 | 88 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 316; yere düşülen (yazılmış yenilgi ya da bayılma) 11.2%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 395.
- Hiç görülmeyen olay: 5 / 526: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `fener_yavrulari` 3.4%, `kaya_soguk_firin` 4.2%, `bl15_us_beyaz_kapi` 6.7%, `thessar_kirik_muhur` 7.0%, `sb14_mert_salon` 8.0%, `p3_firtina_olcucu` 8.8%, `bl15_pk_kamp_aras` 11.6%, `sb14_elif_kayip_plan` 13.0%, `bl15_tp_kamp_deniz` 13.0%, `esik_itaat` 13.3%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `p4_son_kapi` 100.0%, `cekirdege_yol` 100.0%, `kadim_istilasi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `perde4_acilis` 100.0%, `bl13_kd_bekci` 100.0%, `kule_kat_10` 100.0%.
- Bitiş seviyesi: p10 575, p50 1121, p90 1385.
