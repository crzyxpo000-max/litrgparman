# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 29 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 143 | 14.3% |
| demir_taht | 73 | 7.3% |
| yukselis | 63 | 6.3% |
| kod_kirici | 205 | 20.5% |
| ozgurluk | 78 | 7.8% |
| vassal | 125 | 12.5% |
| yalniz_avci | 75 | 7.5% |
| hasat | 185 | 18.5% |
| dongu | 53 | 5.3% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (57) | 64.8% |
| dongu | 88 | dongu (53) | 60.2% |
| kod_kirici | 87 | kod_kirici (68) | 78.2% |
| koruyucu | 88 | koruyucu (42) | 47.7% |
| ozgurluk | 87 | ozgurluk (78) | 89.7% |
| rastgele | 300 | hasat (115) | — |
| vassal | 87 | vassal (81) | 93.1% |
| yalniz_avci | 87 | yalniz_avci (54) | 62.1% |
| yukselis | 88 | yukselis (51) | 58.0% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 11.4% ×0.78 · dongu 60.2% ×0.82 · kod_kirici 50.6% ×0.82 · koruyucu 47.7% ×0.78 · ozgurluk 21.8% ×0.82 · rastgele 41.3% ×0.79 · vassal 9.2% ×0.81 · yalniz_avci 42.5% ×0.81 · yukselis 10.2% ×0.75

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (24): `HasFlag(flag=lonca_hakimiyeti)` 24
- **dongu** (19): `HasFlag(flag=yanki_son_sirri)` 19 · `VarAtLeast(key=kule_kat, value=100)` 10
- **kod_kirici** (16): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 12 · `LacksFlag(flag=yanki_silindi)` 4 · `NpcAlive(npc=mert_kilic)` 1
- **koruyucu** (46): `HasFlag(flag=kadimler_yenildi)` 46 · `ReputationAtLeast(faction=siginak, value=50)` 1
- **vassal** (3): `VarAtLeast(key=kadim_tehdit, value=70)` 2 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (29): `CompanionsAtMost(value=0)` 15 · `HasFlag(flag=uye_yok)` 10 · `GradeAtLeast(grade=S)` 9
- **yukselis** (21): `QuestIs(quest=zincir_yukselenler, status=DONE)` 19 · `NpcAlive(npc=vahit)` 8

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
| 240 | 282 | 245 | 358 | 432 | B |  |
| 270 | 411 | 336 | 475 | 556 | A |  |
| 300 | 599 | 474 | 614 | 746 | A |  |
| 330 | 874 | 540 | 734 | 805 | A | 51 |
| 360 | 874 | 640 | 1098 | 1369 | S |  |
| 365 | 874 | 640 | 1098 | 1369 | S | 59 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 65.9% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 16.6%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×4.8 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.2 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×18.5 (en güçlü NPC ortalaması Sv. 96)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 64.4%.

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
| 240 | 0 | 59 | 92 |
| 270 | 5 | 62 | 95 |
| 300 | 4 | 84 | 96 |
| 330 | 1 | 24 | 62 |
| 360 | 4 | 67 | 91 |
| 365 | 4 | 67 | 91 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 289; yere düşülen (yazılmış yenilgi ya da bayılma) 10.5%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 389.
- Hiç görülmeyen olay: 6 / 519: `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `darbogaz_17`, `elif_yoldas_darbe`, `hasat_vakti`.
- En nadir 10 olay: `kaya_soguk_firin` 4.6%, `fener_yavrulari` 6.5%, `thessar_kirik_muhur` 7.4%, `sb14_mert_salon` 7.5%, `bl15_us_beyaz_kapi` 7.8%, `p3_firtina_olcucu` 7.9%, `sb14_elif_kayip_plan` 9.1%, `sb6_deniz_balkon` 12.8%, `sb15_deniz_kalkan` 12.8%, `esik_itaat` 13.6%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_son_kapi` 100.0%, `cekirdege_yol` 100.0%, `kadim_istilasi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `perde4_acilis` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%, `p3_heykel` 100.0%.
- Bitiş seviyesi: p10 640, p50 1098, p90 1369.
