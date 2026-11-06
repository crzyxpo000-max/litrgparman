# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 21 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 122 | 12.2% |
| demir_taht | 72 | 7.2% |
| yukselis | 57 | 5.7% |
| kod_kirici | 144 | 14.4% |
| ozgurluk | 79 | 7.9% |
| vassal | 109 | 10.9% |
| yalniz_avci | 221 | 22.1% |
| hasat | 124 | 12.4% |
| dongu | 72 | 7.2% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (55) | 62.5% |
| dongu | 88 | dongu (72) | 81.8% |
| kod_kirici | 87 | kod_kirici (65) | 74.7% |
| koruyucu | 88 | koruyucu (43) | 48.9% |
| ozgurluk | 87 | ozgurluk (79) | 90.8% |
| rastgele | 300 | yalniz_avci (113) | — |
| vassal | 87 | vassal (79) | 90.8% |
| yalniz_avci | 87 | yalniz_avci (68) | 78.2% |
| yukselis | 88 | yukselis (49) | 55.7% |

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 6 | 8 | 9 | F | 11 |
| 60 | 29 | 14 | 26 | 37 | E |  |
| 90 | 43 | 27 | 60 | 71 | D |  |
| 120 | 62 | 60 | 85 | 89 | D |  |
| 150 | 91 | 106 | 120 | 127 | C |  |
| 180 | 132 | 156 | 171 | 178 | C | 33 |
| 210 | 193 | 224 | 254 | 285 | B |  |
| 240 | 282 | 322 | 356 | 392 | B |  |
| 270 | 411 | 471 | 508 | 545 | A |  |
| 300 | 599 | 633 | 705 | 735 | A |  |
| 330 | 874 | 829 | 957 | 1020 | S | 49 |
| 360 | 874 | 1095 | 1272 | 1429 | S |  |
| 365 | 874 | 1095 | 1272 | 1429 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 82.2% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 10.7%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.7 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×5.1 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×19.5 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×22.5 (en güçlü NPC ortalaması Sv. 86)
- **Kule:** ortalama en yüksek kat 96; 100'e ulaşan 91.8%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 0 | 8 | 19 |
| 60 | 0 | 6 | 43 |
| 90 | 0 | 48 | 82 |
| 120 | 14 | 58 | 84 |
| 150 | 8 | 59 | 87 |
| 180 | 5 | 61 | 97 |
| 210 | 49 | 91 | 98 |
| 240 | 67 | 94 | 98 |
| 270 | 83 | 97 | 98 |
| 300 | 41 | 90 | 98 |
| 330 | 32 | 78 | 94 |
| 360 | 77 | 89 | 94 |
| 365 | 77 | 89 | 94 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 205.
- Bitiş seviyesi: p10 1095, p50 1272, p90 1429.
