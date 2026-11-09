# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 27 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 122 | 12.2% |
| demir_taht | 76 | 7.6% |
| yukselis | 55 | 5.5% |
| kod_kirici | 205 | 20.5% |
| ozgurluk | 80 | 8.0% |
| vassal | 133 | 13.3% |
| yalniz_avci | 85 | 8.5% |
| hasat | 186 | 18.6% |
| dongu | 58 | 5.8% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (53) | 60.2% |
| dongu | 88 | dongu (58) | 65.9% |
| kod_kirici | 87 | kod_kirici (65) | 74.7% |
| koruyucu | 88 | hasat (41) | 31.8% |
| ozgurluk | 87 | ozgurluk (80) | 92.0% |
| rastgele | 300 | hasat (109) | — |
| vassal | 87 | vassal (76) | 87.4% |
| yalniz_avci | 87 | yalniz_avci (52) | 59.8% |
| yukselis | 88 | yukselis (45) | 51.1% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 14.8% ×0.76 · dongu 42.0% ×0.79 · kod_kirici 49.4% ×0.77 · koruyucu 33.0% ×0.74 · ozgurluk 10.3% ×0.79 · rastgele 44.3% ×0.75 · vassal 10.3% ×0.79 · yalniz_avci 46.0% ×0.76 · yukselis 10.2% ×0.76

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (26): `HasFlag(flag=lonca_hakimiyeti)` 26
- **dongu** (23): `HasFlag(flag=yanki_son_sirri)` 23 · `VarAtLeast(key=kule_kat, value=100)` 18
- **kod_kirici** (17): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 11 · `LacksFlag(flag=yanki_silindi)` 7
- **koruyucu** (60): `HasFlag(flag=kadimler_yenildi)` 59 · `ReputationAtLeast(faction=siginak, value=50)` 3
- **ozgurluk** (3): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 3
- **vassal** (3): `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 2 · `VarAtLeast(key=kadim_tehdit, value=70)` 1
- **yalniz_avci** (28): `GradeAtLeast(grade=S)` 15 · `CompanionsAtMost(value=0)` 13 · `HasFlag(flag=uye_yok)` 6
- **yukselis** (32): `QuestIs(quest=zincir_yukselenler, status=DONE)` 29 · `NpcAlive(npc=vahit)` 15

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 14 | 27 | 40 | E |  |
| 90 | 43 | 25 | 56 | 81 | D |  |
| 120 | 62 | 42 | 75 | 100 | D |  |
| 150 | 91 | 63 | 99 | 117 | D |  |
| 180 | 132 | 116 | 155 | 180 | C | 34 |
| 210 | 193 | 169 | 251 | 321 | B |  |
| 240 | 282 | 229 | 350 | 411 | B |  |
| 270 | 411 | 318 | 459 | 548 | A |  |
| 300 | 599 | 419 | 596 | 730 | A |  |
| 330 | 874 | 447 | 710 | 796 | A | 51 |
| 360 | 874 | 574 | 1077 | 1351 | S |  |
| 365 | 874 | 574 | 1077 | 1351 | S | 59 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 62.4% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 15.3%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.6 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×13.8 (en güçlü NPC ortalaması Sv. 69)
  - Gün 365: ×18.4 (en güçlü NPC ortalaması Sv. 95)
- **Kule:** ortalama en yüksek kat 93; 100'e ulaşan 56.5%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 1 | 22 | 47 |
| 60 | 0 | 12 | 50 |
| 90 | 0 | 31 | 70 |
| 120 | 0 | 20 | 46 |
| 150 | 0 | 2 | 31 |
| 180 | 0 | 9 | 89 |
| 210 | 22 | 66 | 92 |
| 240 | 0 | 56 | 92 |
| 270 | 1 | 58 | 93 |
| 300 | 0 | 74 | 96 |
| 330 | 0 | 11 | 29 |
| 360 | 1 | 65 | 92 |
| 365 | 1 | 65 | 92 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 287; yere düşülen (yazılmış yenilgi ya da bayılma) 11.5%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 383.
- Hiç görülmeyen olay: 5 / 519: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `kaya_soguk_firin` 4.9%, `fener_yavrulari` 5.4%, `sb14_mert_salon` 6.2%, `thessar_kirik_muhur` 6.8%, `p3_firtina_olcucu` 7.1%, `bl15_us_beyaz_kapi` 8.3%, `sb14_elif_kayip_plan` 8.7%, `sb6_deniz_balkon` 11.9%, `bl15_pk_kamp_aras` 12.3%, `sb15_deniz_kalkan` 13.7%.
- En sık 10 olay: `son_karar` 100.0%, `perde4_acilis` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_heykel` 100.0%, `p3_kademe_borsasi` 100.0%, `p3_siralama_zirvesi` 100.0%, `p3_ellinciler` 100.0%.
- Bitiş seviyesi: p10 574, p50 1077, p90 1351.
