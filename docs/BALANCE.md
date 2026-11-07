# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 24 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 119 | 11.9% |
| demir_taht | 80 | 8.0% |
| yukselis | 60 | 6.0% |
| kod_kirici | 178 | 17.8% |
| ozgurluk | 77 | 7.7% |
| vassal | 123 | 12.3% |
| yalniz_avci | 94 | 9.4% |
| hasat | 211 | 21.1% |
| dongu | 58 | 5.8% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (61) | 69.3% |
| dongu | 88 | dongu (58) | 65.9% |
| kod_kirici | 87 | kod_kirici (65) | 74.7% |
| koruyucu | 88 | hasat (45) | 37.5% |
| ozgurluk | 87 | ozgurluk (76) | 87.4% |
| rastgele | 300 | hasat (121) | — |
| vassal | 87 | vassal (78) | 89.7% |
| yalniz_avci | 87 | yalniz_avci (58) | 66.7% |
| yukselis | 88 | yukselis (50) | 56.8% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 12.5% ×0.79 · dongu 53.4% ×0.80 · kod_kirici 44.8% ×0.80 · koruyucu 40.9% ×0.77 · ozgurluk 10.3% ×0.81 · rastgele 40.0% ×0.77 · vassal 9.2% ×0.79 · yalniz_avci 39.1% ×0.80 · yukselis 14.8% ×0.74

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (22): `HasFlag(flag=lonca_hakimiyeti)` 22
- **dongu** (19): `HasFlag(flag=yanki_son_sirri)` 17 · `VarAtLeast(key=kule_kat, value=100)` 12
- **kod_kirici** (20): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 13 · `LacksFlag(flag=yanki_silindi)` 9 · `NpcAlive(npc=mert_kilic)` 1
- **koruyucu** (52): `HasFlag(flag=kadimler_yenildi)` 52 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **vassal** (4): `VarAtLeast(key=kadim_tehdit, value=70)` 3 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (28): `HasFlag(flag=uye_yok)` 12 · `CompanionsAtMost(value=0)` 10 · `GradeAtLeast(grade=S)` 8
- **yukselis** (30): `QuestIs(quest=zincir_yukselenler, status=DONE)` 25 · `NpcAlive(npc=vahit)` 16

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 28 | 39 | E |  |
| 90 | 43 | 25 | 53 | 76 | D |  |
| 120 | 62 | 42 | 73 | 96 | D |  |
| 150 | 91 | 66 | 99 | 117 | D |  |
| 180 | 132 | 114 | 154 | 182 | C | 33 |
| 210 | 193 | 165 | 246 | 312 | B |  |
| 240 | 282 | 232 | 344 | 409 | B |  |
| 270 | 411 | 331 | 457 | 549 | A |  |
| 300 | 599 | 419 | 602 | 734 | A |  |
| 330 | 874 | 468 | 724 | 801 | A | 49 |
| 360 | 874 | 570 | 1084 | 1348 | S |  |
| 365 | 874 | 570 | 1084 | 1348 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 62.2% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 21.1%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.6 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×14.6 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×19.2 (en güçlü NPC ortalaması Sv. 86)
- **Kule:** ortalama en yüksek kat 92; 100'e ulaşan 53.3%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 13 | 47 |
| 90 | 0 | 28 | 67 |
| 120 | 0 | 24 | 52 |
| 150 | 0 | 8 | 38 |
| 180 | 0 | 18 | 92 |
| 210 | 24 | 65 | 91 |
| 240 | 0 | 58 | 91 |
| 270 | 1 | 56 | 93 |
| 300 | 0 | 82 | 96 |
| 330 | 0 | 26 | 62 |
| 360 | 3 | 66 | 92 |
| 365 | 4 | 66 | 92 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 282; yere düşülen (yazılmış yenilgi ya da bayılma) 10.9%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 356.
- Hiç görülmeyen olay: 5 / 460: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `hasat_vakti`.
- En nadir 10 olay: `bl12_kd_kamp_elif` 2.0%, `kaya_soguk_firin` 4.9%, `fener_yavrulari` 6.1%, `sb14_mert_salon` 6.1%, `thessar_kirik_muhur` 6.7%, `bl15_us_beyaz_kapi` 7.8%, `p3_firtina_olcucu` 8.3%, `sb14_elif_kayip_plan` 9.2%, `esik_itaat` 13.4%, `vapur_ilk_sefer` 13.5%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_korfez_leviatani` 100.0%, `p3_siralama_zirvesi` 100.0%, `p3_ellinciler` 100.0%.
- Bitiş seviyesi: p10 570, p50 1084, p90 1348.
