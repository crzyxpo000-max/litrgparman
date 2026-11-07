# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 28 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 112 | 11.2% |
| demir_taht | 66 | 6.6% |
| yukselis | 60 | 6.0% |
| kod_kirici | 209 | 20.9% |
| ozgurluk | 79 | 7.9% |
| vassal | 133 | 13.3% |
| yalniz_avci | 89 | 8.9% |
| hasat | 197 | 19.7% |
| dongu | 55 | 5.5% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (56) | 63.6% |
| dongu | 88 | dongu (55) | 62.5% |
| kod_kirici | 87 | kod_kirici (69) | 79.3% |
| koruyucu | 88 | hasat (46) | 31.8% |
| ozgurluk | 87 | ozgurluk (79) | 90.8% |
| rastgele | 300 | hasat (117) | — |
| vassal | 87 | vassal (81) | 93.1% |
| yalniz_avci | 87 | yalniz_avci (63) | 72.4% |
| yukselis | 88 | yukselis (50) | 56.8% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 10.2% ×0.79 · dongu 54.5% ×0.80 · kod_kirici 57.5% ×0.81 · koruyucu 31.8% ×0.77 · ozgurluk 12.6% ×0.80 · rastgele 40.7% ×0.77 · vassal 12.6% ×0.80 · yalniz_avci 36.8% ×0.82 · yukselis 9.1% ×0.75

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (26): `HasFlag(flag=lonca_hakimiyeti)` 26
- **dongu** (23): `HasFlag(flag=yanki_son_sirri)` 21 · `VarAtLeast(key=kule_kat, value=100)` 16
- **kod_kirici** (17): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 9 · `LacksFlag(flag=yanki_silindi)` 8 · `NpcAlive(npc=mert_kilic)` 1
- **koruyucu** (60): `HasFlag(flag=kadimler_yenildi)` 60 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **vassal** (1): `VarAtLeast(key=kadim_tehdit, value=70)` 1
- **yalniz_avci** (23): `HasFlag(flag=uye_yok)` 12 · `GradeAtLeast(grade=S)` 6 · `CompanionsAtMost(value=0)` 5
- **yukselis** (27): `QuestIs(quest=zincir_yukselenler, status=DONE)` 24 · `NpcAlive(npc=vahit)` 17

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 28 | 39 | E |  |
| 90 | 43 | 25 | 53 | 76 | D |  |
| 120 | 62 | 42 | 73 | 95 | D |  |
| 150 | 91 | 67 | 99 | 117 | D |  |
| 180 | 132 | 114 | 153 | 180 | C | 33 |
| 210 | 193 | 166 | 245 | 313 | B |  |
| 240 | 282 | 230 | 344 | 409 | B |  |
| 270 | 411 | 335 | 454 | 547 | A |  |
| 300 | 599 | 430 | 601 | 726 | A |  |
| 330 | 874 | 491 | 723 | 798 | A | 49 |
| 360 | 874 | 575 | 1083 | 1358 | S |  |
| 365 | 874 | 575 | 1083 | 1358 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 62.0% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 21.0%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.6 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×14.7 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×19.2 (en güçlü NPC ortalaması Sv. 86)
- **Kule:** ortalama en yüksek kat 92; 100'e ulaşan 51.5%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 13 | 47 |
| 90 | 0 | 29 | 66 |
| 120 | 0 | 25 | 51 |
| 150 | 0 | 7 | 37 |
| 180 | 0 | 12 | 92 |
| 210 | 26 | 64 | 91 |
| 240 | 0 | 62 | 92 |
| 270 | 1 | 56 | 92 |
| 300 | 0 | 80 | 96 |
| 330 | 0 | 26 | 62 |
| 360 | 4 | 64 | 92 |
| 365 | 4 | 64 | 92 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 283; yere düşülen (yazılmış yenilgi ya da bayılma) 10.8%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 351.
- Hiç görülmeyen olay: 8 / 460: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `bl6_kd_kamp_aras`, `bl8_kamp_aras`, `bl9_kamp_aras`, `darbogaz_17`, `hasat_vakti`.
- En nadir 10 olay: `bl13_kamp_elif` 1.2%, `bl15_kd_kamp_elif` 1.2%, `bl12_kd_kamp_elif` 1.8%, `bl9_kamp_elif` 2.0%, `bl8_kamp_elif` 2.2%, `bl7_kd_kamp_elif` 2.8%, `kaya_soguk_firin` 5.2%, `fener_yavrulari` 6.2%, `thessar_kirik_muhur` 7.5%, `p3_firtina_olcucu` 7.7%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `cekirdege_yol` 100.0%, `bl13_kd_bekci` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_siralama_zirvesi` 100.0%, `p3_ellinciler` 100.0%.
- Bitiş seviyesi: p10 575, p50 1083, p90 1358.
