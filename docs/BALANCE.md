# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 20 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 124 | 12.4% |
| demir_taht | 72 | 7.2% |
| yukselis | 61 | 6.1% |
| kod_kirici | 174 | 17.4% |
| ozgurluk | 76 | 7.6% |
| vassal | 129 | 12.9% |
| yalniz_avci | 114 | 11.4% |
| hasat | 184 | 18.4% |
| dongu | 66 | 6.6% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (51) | 58.0% |
| dongu | 88 | dongu (66) | 75.0% |
| kod_kirici | 87 | kod_kirici (55) | 63.2% |
| koruyucu | 88 | hasat (37) | 42.0% |
| ozgurluk | 87 | ozgurluk (76) | 87.4% |
| rastgele | 300 | hasat (118) | — |
| vassal | 87 | vassal (79) | 90.8% |
| yalniz_avci | 87 | yalniz_avci (72) | 82.8% |
| yukselis | 88 | yukselis (44) | 50.0% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 14.8% ×0.91 · dongu 46.6% ×0.91 · kod_kirici 54.0% ×0.94 · koruyucu 44.3% ×0.90 · ozgurluk 11.5% ×0.93 · rastgele 39.3% ×0.93 · vassal 18.4% ×0.89 · yalniz_avci 47.1% ×0.93 · yukselis 11.4% ×0.90

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (22): `HasFlag(flag=lonca_hakimiyeti)` 22
- **dongu** (14): `HasFlag(flag=yanki_son_sirri)` 7 · `VarAtLeast(key=kule_kat, value=100)` 7
- **kod_kirici** (24): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 15 · `LacksFlag(flag=yanki_silindi)` 10
- **koruyucu** (49): `HasFlag(flag=kadimler_yenildi)` 49 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **ozgurluk** (2): `MoralityAtMost(axis=ORDER, value=-60)` 2
- **vassal** (5): `VarAtLeast(key=kadim_tehdit, value=70)` 5
- **yalniz_avci** (10): `GradeAtLeast(grade=S)` 5 · `CompanionsAtMost(value=0)` 4 · `HasFlag(flag=uye_yok)` 2
- **yukselis** (26): `QuestIs(quest=zincir_yukselenler, status=DONE)` 23 · `NpcAlive(npc=vahit)` 10

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 8 | 9 | 11 | F | 11 |
| 60 | 29 | 17 | 33 | 42 | E |  |
| 90 | 43 | 42 | 66 | 85 | D |  |
| 120 | 62 | 69 | 89 | 101 | D |  |
| 150 | 91 | 100 | 124 | 134 | C |  |
| 180 | 132 | 137 | 173 | 188 | C | 33 |
| 210 | 193 | 197 | 267 | 343 | B |  |
| 240 | 282 | 280 | 375 | 455 | B |  |
| 270 | 411 | 402 | 504 | 588 | A |  |
| 300 | 599 | 556 | 638 | 732 | A |  |
| 330 | 874 | 750 | 833 | 882 | S | 49 |
| 360 | 874 | 961 | 1237 | 1486 | S |  |
| 365 | 874 | 961 | 1237 | 1486 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 78.9% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 26.8%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×5.2 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×16.9 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×21.8 (en güçlü NPC ortalaması Sv. 87)
- **Kule:** ortalama en yüksek kat 96; 100'e ulaşan 91.3%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 2 | 12 | 21 |
| 60 | 0 | 15 | 46 |
| 90 | 0 | 41 | 72 |
| 120 | 2 | 34 | 60 |
| 150 | 0 | 25 | 59 |
| 180 | 0 | 17 | 69 |
| 210 | 31 | 74 | 93 |
| 240 | 25 | 72 | 93 |
| 270 | 27 | 77 | 94 |
| 300 | 0 | 30 | 84 |
| 330 | 0 | 20 | 62 |
| 360 | 29 | 73 | 83 |
| 365 | 29 | 73 | 83 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 390; yere düşülen (yazılmış yenilgi ya da bayılma) 8.2%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 206.
- Hiç görülmeyen olay: 4 / 255: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `hasat_vakti`.
- En nadir 10 olay: `p3_firtina_olcucu` 2.3%, `kaya_soguk_firin` 5.0%, `thessar_kirik_muhur` 6.9%, `esik_itaat` 11.7%, `fener_yavrulari` 12.1%, `deniz_yukselen_rakip` 13.8%, `deniz_kardes_cizimi` 17.6%, `deniz_anma_kardes` 18.0%, `pala_yargilanmasi` 18.7%, `esik_kacis` 19.2%.
- En sık 10 olay: `son_karar` 100.0%, `p4_son_gece_ates` 100.0%, `p4_yikik_kiyikent` 100.0%, `sistem_cekirdegi` 100.0%, `cekirdege_yol` 100.0%, `kadim_istilasi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `perde4_acilis` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%.
- Bitiş seviyesi: p10 961, p50 1237, p90 1486.
