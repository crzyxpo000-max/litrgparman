# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 28 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 177 | 17.7% |
| demir_taht | 84 | 8.4% |
| yukselis | 66 | 6.6% |
| kod_kirici | 175 | 17.5% |
| ozgurluk | 76 | 7.6% |
| vassal | 105 | 10.5% |
| yalniz_avci | 82 | 8.2% |
| hasat | 167 | 16.7% |
| dongu | 68 | 6.8% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (68) | 77.3% |
| dongu | 88 | dongu (68) | 77.3% |
| kod_kirici | 87 | kod_kirici (63) | 72.4% |
| koruyucu | 88 | koruyucu (65) | 73.9% |
| ozgurluk | 87 | ozgurluk (76) | 87.4% |
| rastgele | 300 | hasat (115) | — |
| vassal | 87 | vassal (77) | 88.5% |
| yalniz_avci | 87 | yalniz_avci (56) | 64.4% |
| yukselis | 88 | yukselis (58) | 65.9% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 20.5% ×0.80 · dongu 67.0% ×0.82 · kod_kirici 66.7% ×0.79 · koruyucu 81.8% ×0.81 · ozgurluk 19.5% ×0.86 · rastgele 51.7% ×0.78 · vassal 19.5% ×0.80 · yalniz_avci 50.6% ×0.80 · yukselis 12.5% ×0.74

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (13): `HasFlag(flag=lonca_hakimiyeti)` 13
- **dongu** (13): `HasFlag(flag=yanki_son_sirri)` 11 · `VarAtLeast(key=kule_kat, value=100)` 9
- **kod_kirici** (19): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 11 · `LacksFlag(flag=yanki_silindi)` 7 · `NpcAlive(npc=mert_kilic)` 2
- **koruyucu** (18): `HasFlag(flag=kadimler_yenildi)` 16 · `ReputationAtLeast(faction=siginak, value=50)` 2
- **ozgurluk** (3): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 2 · `MoralityAtMost(axis=ORDER, value=-60)` 1
- **vassal** (3): `VarAtLeast(key=kadim_tehdit, value=70)` 3 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 2
- **yalniz_avci** (25): `CompanionsAtMost(value=0)` 14 · `GradeAtLeast(grade=S)` 12 · `HasFlag(flag=uye_yok)` 2
- **yukselis** (22): `QuestIs(quest=zincir_yukselenler, status=DONE)` 22 · `NpcAlive(npc=vahit)` 9

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 10 | 8 | 9 | 11 | F | 10 |
| 60 | 29 | 16 | 32 | 41 | E |  |
| 90 | 43 | 30 | 69 | 92 | D |  |
| 120 | 62 | 50 | 86 | 112 | D |  |
| 150 | 91 | 76 | 111 | 126 | C |  |
| 180 | 132 | 127 | 170 | 186 | C | 34 |
| 210 | 193 | 181 | 270 | 346 | B |  |
| 240 | 282 | 251 | 371 | 436 | B |  |
| 270 | 411 | 328 | 486 | 555 | A |  |
| 300 | 599 | 423 | 636 | 758 | A |  |
| 330 | 874 | 479 | 734 | 809 | A | 52 |
| 360 | 874 | 564 | 1135 | 1453 | S |  |
| 365 | 874 | 564 | 1135 | 1453 | S | 60 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 70.6% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 19.6%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×5.0 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.2 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×19.3 (en güçlü NPC ortalaması Sv. 97)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 73.4%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 6 | 26 | 55 |
| 60 | 0 | 23 | 62 |
| 90 | 0 | 44 | 81 |
| 120 | 0 | 26 | 52 |
| 150 | 0 | 6 | 33 |
| 180 | 0 | 15 | 50 |
| 210 | 34 | 71 | 92 |
| 240 | 13 | 59 | 90 |
| 270 | 7 | 65 | 95 |
| 300 | 1 | 75 | 92 |
| 330 | 0 | 12 | 29 |
| 360 | 20 | 72 | 89 |
| 365 | 20 | 72 | 89 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 318; yere düşülen (yazılmış yenilgi ya da bayılma) 11.9%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 394.
- Hiç görülmeyen olay: 6 / 526: `atlama_meslek`, `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `kaya_soguk_firin` 4.0%, `thessar_kirik_muhur` 5.7%, `fener_yavrulari` 6.9%, `yozlasma_50` 7.0%, `yozlasma_75` 7.0%, `bl15_us_beyaz_kapi` 7.6%, `p3_firtina_olcucu` 7.8%, `sb14_mert_salon` 8.3%, `bl15_pk_kamp_aras` 11.5%, `sb14_elif_kayip_plan` 12.1%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_son_kapi` 100.0%, `bl13_kd_police` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_kordon_geri` 100.0%, `p3_kadim_pazari` 100.0%, `p3_kademe_ailesi` 100.0%, `p3_donenler` 100.0%.
- Bitiş seviyesi: p10 564, p50 1135, p90 1453.
