# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 28 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 144 | 14.4% |
| demir_taht | 72 | 7.2% |
| yukselis | 66 | 6.6% |
| kod_kirici | 202 | 20.2% |
| ozgurluk | 79 | 7.9% |
| vassal | 106 | 10.6% |
| yalniz_avci | 81 | 8.1% |
| hasat | 190 | 19.0% |
| dongu | 60 | 6.0% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (59) | 67.0% |
| dongu | 88 | dongu (60) | 68.2% |
| kod_kirici | 87 | kod_kirici (67) | 77.0% |
| koruyucu | 88 | koruyucu (43) | 48.9% |
| ozgurluk | 87 | ozgurluk (79) | 90.8% |
| rastgele | 300 | hasat (115) | — |
| vassal | 87 | vassal (72) | 82.8% |
| yalniz_avci | 87 | yalniz_avci (60) | 69.0% |
| yukselis | 88 | yukselis (54) | 61.4% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 12.5% ×0.78 · dongu 56.8% ×0.82 · kod_kirici 48.3% ×0.81 · koruyucu 51.1% ×0.77 · ozgurluk 16.1% ×0.81 · rastgele 48.0% ×0.78 · vassal 17.2% ×0.81 · yalniz_avci 44.8% ×0.80 · yukselis 12.5% ×0.76

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (24): `HasFlag(flag=lonca_hakimiyeti)` 24
- **dongu** (20): `HasFlag(flag=yanki_son_sirri)` 18 · `VarAtLeast(key=kule_kat, value=100)` 12
- **kod_kirici** (17): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 9 · `LacksFlag(flag=yanki_silindi)` 8 · `NpcAlive(npc=mert_kilic)` 1
- **koruyucu** (43): `HasFlag(flag=kadimler_yenildi)` 43 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **vassal** (2): `VarAtLeast(key=kadim_tehdit, value=70)` 2
- **yalniz_avci** (23): `HasFlag(flag=uye_yok)` 10 · `GradeAtLeast(grade=S)` 7 · `CompanionsAtMost(value=0)` 6
- **yukselis** (28): `QuestIs(quest=zincir_yukselenler, status=DONE)` 19 · `NpcAlive(npc=vahit)` 17

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 20 | 7 | 9 | 11 | F | 10 |
| 60 | 29 | 15 | 29 | 40 | E |  |
| 90 | 43 | 27 | 58 | 82 | D |  |
| 120 | 62 | 47 | 79 | 102 | D |  |
| 150 | 91 | 73 | 107 | 122 | C |  |
| 180 | 132 | 125 | 163 | 184 | C | 34 |
| 210 | 193 | 174 | 258 | 332 | B |  |
| 240 | 282 | 248 | 363 | 438 | B |  |
| 270 | 411 | 344 | 473 | 555 | A |  |
| 300 | 599 | 464 | 607 | 738 | A |  |
| 330 | 874 | 539 | 728 | 800 | A | 51 |
| 360 | 874 | 655 | 1105 | 1375 | S |  |
| 365 | 874 | 655 | 1105 | 1375 | S | 60 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 65.7% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 16.7%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.8 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.1 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×18.5 (en güçlü NPC ortalaması Sv. 97)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 62.3%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 5 | 27 | 48 |
| 60 | 0 | 15 | 52 |
| 90 | 0 | 32 | 70 |
| 120 | 0 | 28 | 56 |
| 150 | 0 | 10 | 40 |
| 180 | 0 | 19 | 92 |
| 210 | 28 | 66 | 91 |
| 240 | 1 | 62 | 93 |
| 270 | 5 | 59 | 93 |
| 300 | 0 | 83 | 96 |
| 330 | 0 | 25 | 62 |
| 360 | 2 | 66 | 92 |
| 365 | 2 | 66 | 92 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 289; yere düşülen (yazılmış yenilgi ya da bayılma) 10.6%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 388.
- Hiç görülmeyen olay: 6 / 518: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `sb14_mert_salon` 5.3%, `kaya_soguk_firin` 6.0%, `fener_yavrulari` 6.5%, `bl15_us_beyaz_kapi` 6.7%, `thessar_kirik_muhur` 7.5%, `p3_firtina_olcucu` 8.2%, `sb14_elif_kayip_plan` 10.1%, `sb15_deniz_kalkan` 12.1%, `sb6_deniz_balkon` 12.8%, `esik_itaat` 13.6%.
- En sık 10 olay: `son_karar` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_heykel` 100.0%, `p3_korfez_leviatani` 100.0%, `p3_siralama_zirvesi` 100.0%, `p3_ellinciler` 100.0%, `p3_bayrak_altinda` 100.0%.
- Bitiş seviyesi: p10 655, p50 1105, p90 1375.
