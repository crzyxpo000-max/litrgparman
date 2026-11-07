# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 19 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 103 | 10.3% |
| demir_taht | 79 | 7.9% |
| yukselis | 67 | 6.7% |
| kod_kirici | 207 | 20.7% |
| ozgurluk | 71 | 7.1% |
| vassal | 128 | 12.8% |
| yalniz_avci | 94 | 9.4% |
| hasat | 191 | 19.1% |
| dongu | 60 | 6.0% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (58) | 65.9% |
| dongu | 88 | dongu (60) | 68.2% |
| kod_kirici | 87 | kod_kirici (67) | 77.0% |
| koruyucu | 88 | hasat (45) | 31.8% |
| ozgurluk | 87 | ozgurluk (71) | 81.6% |
| rastgele | 300 | hasat (108) | — |
| vassal | 87 | vassal (82) | 94.3% |
| yalniz_avci | 87 | yalniz_avci (60) | 69.0% |
| yukselis | 88 | yukselis (54) | 61.4% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 6.8% ×0.78 · dongu 25.0% ×0.80 · kod_kirici 48.3% ×0.84 · koruyucu 33.0% ×0.74 · ozgurluk 10.3% ×0.81 · rastgele 41.0% ×0.81 · vassal 8.0% ×0.79 · yalniz_avci 28.7% ×0.79 · yukselis 12.5% ×0.79

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (23): `HasFlag(flag=lonca_hakimiyeti)` 23
- **dongu** (24): `HasFlag(flag=yanki_son_sirri)` 24 · `VarAtLeast(key=kule_kat, value=100)` 17
- **kod_kirici** (14): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 12 · `NpcAlive(npc=mert_kilic)` 1 · `LacksFlag(flag=yanki_silindi)` 1
- **koruyucu** (59): `HasFlag(flag=kadimler_yenildi)` 59
- **ozgurluk** (6): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 5 · `MoralityAtMost(axis=ORDER, value=-60)` 1
- **yalniz_avci** (23): `GradeAtLeast(grade=S)` 10 · `HasFlag(flag=uye_yok)` 7 · `CompanionsAtMost(value=0)` 6
- **yukselis** (24): `QuestIs(quest=zincir_yukselenler, status=DONE)` 22 · `NpcAlive(npc=vahit)` 13

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 30 | 39 | E |  |
| 90 | 43 | 24 | 54 | 77 | D |  |
| 120 | 62 | 38 | 72 | 90 | D |  |
| 150 | 91 | 57 | 98 | 113 | D |  |
| 180 | 132 | 114 | 156 | 183 | C | 33 |
| 210 | 193 | 171 | 251 | 319 | B |  |
| 240 | 282 | 242 | 347 | 409 | B |  |
| 270 | 411 | 323 | 467 | 551 | A |  |
| 300 | 599 | 439 | 610 | 738 | A |  |
| 330 | 874 | 522 | 737 | 810 | A | 50 |
| 360 | 874 | 649 | 1085 | 1355 | S |  |
| 365 | 874 | 649 | 1085 | 1355 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 63.4% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 22.0%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.7 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×14.8 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×19.2 (en güçlü NPC ortalaması Sv. 86)
- **Kule:** ortalama en yüksek kat 93; 100'e ulaşan 59.2%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 15 | 47 |
| 90 | 0 | 30 | 66 |
| 120 | 0 | 21 | 44 |
| 150 | 0 | 4 | 36 |
| 180 | 0 | 7 | 75 |
| 210 | 19 | 66 | 91 |
| 240 | 0 | 60 | 91 |
| 270 | 0 | 57 | 92 |
| 300 | 0 | 86 | 96 |
| 330 | 0 | 16 | 65 |
| 360 | 0 | 71 | 92 |
| 365 | 0 | 70 | 92 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 304; yere düşülen (yazılmış yenilgi ya da bayılma) 10.1%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 238.
- Hiç görülmeyen olay: 5 / 293: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `hasat_vakti`.
- En nadir 10 olay: `fener_yavrulari` 5.5%, `p3_firtina_olcucu` 5.7%, `kaya_soguk_firin` 6.1%, `thessar_kirik_muhur` 6.1%, `esik_itaat` 13.0%, `deniz_yukselen_rakip` 14.3%, `deniz_anma_kardes` 14.9%, `vapur_ilk_sefer` 15.1%, `deniz_anma_ayna` 15.6%, `deniz_kardes_cizimi` 17.1%.
- En sık 10 olay: `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `p4_son_kapi` 100.0%, `p4_kadim_oncusu` 100.0%, `kule_kat_10` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_kademe_borsasi` 100.0%, `p3_korfez_leviatani` 100.0%.
- Bitiş seviyesi: p10 649, p50 1085, p90 1355.
