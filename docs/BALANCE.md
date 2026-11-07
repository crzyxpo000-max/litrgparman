# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 19 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 109 | 10.9% |
| demir_taht | 75 | 7.5% |
| yukselis | 67 | 6.7% |
| kod_kirici | 182 | 18.2% |
| ozgurluk | 77 | 7.7% |
| vassal | 120 | 12.0% |
| yalniz_avci | 111 | 11.1% |
| hasat | 193 | 19.3% |
| dongu | 66 | 6.6% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (57) | 64.8% |
| dongu | 88 | dongu (66) | 75.0% |
| kod_kirici | 87 | kod_kirici (66) | 75.9% |
| koruyucu | 88 | hasat (44) | 37.5% |
| ozgurluk | 87 | ozgurluk (77) | 88.5% |
| rastgele | 300 | hasat (125) | — |
| vassal | 87 | vassal (79) | 90.8% |
| yalniz_avci | 87 | yalniz_avci (71) | 81.6% |
| yukselis | 88 | yukselis (56) | 63.6% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 8.0% ×0.79 · dongu 50.0% ×0.83 · kod_kirici 56.3% ×0.81 · koruyucu 38.6% ×0.79 · ozgurluk 14.9% ×0.87 · rastgele 40.0% ×0.79 · vassal 17.2% ×0.79 · yalniz_avci 41.4% ×0.82 · yukselis 14.8% ×0.77

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (19): `HasFlag(flag=lonca_hakimiyeti)` 19
- **dongu** (12): `VarAtLeast(key=kule_kat, value=100)` 9 · `HasFlag(flag=yanki_son_sirri)` 9
- **kod_kirici** (14): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 9 · `LacksFlag(flag=yanki_silindi)` 6
- **koruyucu** (55): `HasFlag(flag=kadimler_yenildi)` 54 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **ozgurluk** (1): `MoralityAtMost(axis=ORDER, value=-60)` 1
- **vassal** (4): `VarAtLeast(key=kadim_tehdit, value=70)` 3 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (12): `GradeAtLeast(grade=S)` 5 · `CompanionsAtMost(value=0)` 4 · `HasFlag(flag=uye_yok)` 3
- **yukselis** (23): `QuestIs(quest=zincir_yukselenler, status=DONE)` 20 · `NpcAlive(npc=vahit)` 12

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 29 | 40 | E |  |
| 90 | 43 | 24 | 54 | 78 | D |  |
| 120 | 62 | 38 | 71 | 90 | D |  |
| 150 | 91 | 60 | 97 | 113 | D |  |
| 180 | 132 | 115 | 156 | 184 | C | 34 |
| 210 | 193 | 174 | 250 | 320 | B |  |
| 240 | 282 | 243 | 352 | 411 | B |  |
| 270 | 411 | 353 | 472 | 562 | A |  |
| 300 | 599 | 505 | 610 | 738 | A |  |
| 330 | 874 | 589 | 737 | 805 | A | 49 |
| 360 | 874 | 721 | 1131 | 1389 | S |  |
| 365 | 874 | 721 | 1131 | 1389 | S | 57 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 63.9% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 22.3%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.7 (en güçlü NPC ortalaması Sv. 43)
  - Gün 330: ×14.9 (en güçlü NPC ortalaması Sv. 61)
  - Gün 365: ×20.0 (en güçlü NPC ortalaması Sv. 88)
- **Kule:** ortalama en yüksek kat 93; 100'e ulaşan 59.4%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 4 | 25 | 46 |
| 60 | 0 | 16 | 49 |
| 90 | 0 | 28 | 64 |
| 120 | 0 | 20 | 44 |
| 150 | 0 | 1 | 34 |
| 180 | 0 | 6 | 68 |
| 210 | 23 | 66 | 92 |
| 240 | 0 | 62 | 92 |
| 270 | 0 | 59 | 95 |
| 300 | 0 | 84 | 96 |
| 330 | 0 | 17 | 62 |
| 360 | 5 | 69 | 88 |
| 365 | 5 | 69 | 88 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 311; yere düşülen (yazılmış yenilgi ya da bayılma) 9.9%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 242.
- Hiç görülmeyen olay: 5 / 293: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `hasat_vakti`.
- En nadir 10 olay: `p3_firtina_olcucu` 4.8%, `fener_yavrulari` 5.7%, `kaya_soguk_firin` 6.1%, `thessar_kirik_muhur` 6.7%, `esik_itaat` 13.4%, `vapur_ilk_sefer` 14.6%, `deniz_yukselen_rakip` 14.9%, `deniz_anma_kardes` 17.5%, `deniz_kardes_cizimi` 18.0%, `deniz_anma_ayna` 18.1%.
- En sık 10 olay: `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `p4_son_kapi` 100.0%, `perde4_acilis` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_heykel` 100.0%, `p3_kademe_ailesi` 100.0%.
- Bitiş seviyesi: p10 721, p50 1131, p90 1389.
