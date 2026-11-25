# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 33 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 183 | 18.3% |
| demir_taht | 81 | 8.1% |
| yukselis | 60 | 6.0% |
| kod_kirici | 177 | 17.7% |
| ozgurluk | 77 | 7.7% |
| vassal | 114 | 11.4% |
| yalniz_avci | 69 | 6.9% |
| hasat | 172 | 17.2% |
| dongu | 67 | 6.7% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (61) | 69.3% |
| dongu | 88 | dongu (67) | 76.1% |
| kod_kirici | 87 | kod_kirici (68) | 78.2% |
| koruyucu | 88 | koruyucu (66) | 75.0% |
| ozgurluk | 87 | ozgurluk (77) | 88.5% |
| rastgele | 300 | hasat (124) | — |
| vassal | 87 | vassal (83) | 95.4% |
| yalniz_avci | 87 | yalniz_avci (53) | 60.9% |
| yukselis | 88 | yukselis (50) | 56.8% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 15.9% ×0.80 · dongu 77.3% ×0.84 · kod_kirici 63.2% ×0.83 · koruyucu 81.8% ×0.87 · ozgurluk 12.6% ×0.91 · rastgele 52.0% ×0.79 · vassal 17.2% ×0.80 · yalniz_avci 59.8% ×0.79 · yukselis 22.7% ×0.75

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (19): `HasFlag(flag=lonca_hakimiyeti)` 19
- **dongu** (10): `HasFlag(flag=yanki_son_sirri)` 10 · `VarAtLeast(key=kule_kat, value=100)` 7
- **kod_kirici** (15): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 8 · `LacksFlag(flag=yanki_silindi)` 7
- **koruyucu** (18): `HasFlag(flag=kadimler_yenildi)` 16 · `ReputationAtLeast(faction=siginak, value=50)` 2
- **ozgurluk** (1): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 1
- **vassal** (1): `VarAtLeast(key=kadim_tehdit, value=70)` 1
- **yalniz_avci** (31): `CompanionsAtMost(value=0)` 16 · `GradeAtLeast(grade=S)` 15 · `HasFlag(flag=uye_yok)` 2
- **yukselis** (27): `QuestIs(quest=zincir_yukselenler, status=DONE)` 25 · `NpcAlive(npc=vahit)` 11

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 10 | 8 | 9 | 11 | F | 10 |
| 60 | 29 | 16 | 33 | 42 | E |  |
| 90 | 43 | 32 | 70 | 91 | D |  |
| 120 | 62 | 51 | 86 | 112 | D |  |
| 150 | 91 | 77 | 111 | 126 | C |  |
| 180 | 132 | 129 | 170 | 185 | C | 34 |
| 210 | 193 | 182 | 268 | 365 | B |  |
| 240 | 282 | 247 | 383 | 491 | B |  |
| 270 | 411 | 338 | 495 | 573 | A |  |
| 300 | 599 | 425 | 651 | 793 | A |  |
| 330 | 874 | 490 | 750 | 851 | A | 51 |
| 360 | 874 | 585 | 1128 | 1678 | S |  |
| 365 | 874 | 585 | 1128 | 1678 | S | 59 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 72.1% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 21.5%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×5.0 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.6 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×19.4 (en güçlü NPC ortalaması Sv. 95)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 75.7%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 6 | 26 | 55 |
| 60 | 0 | 25 | 66 |
| 90 | 0 | 48 | 80 |
| 120 | 0 | 28 | 51 |
| 150 | 0 | 5 | 32 |
| 180 | 0 | 15 | 47 |
| 210 | 31 | 70 | 92 |
| 240 | 10 | 62 | 92 |
| 270 | 12 | 61 | 94 |
| 300 | 0 | 75 | 92 |
| 330 | 1 | 13 | 29 |
| 360 | 22 | 70 | 88 |
| 365 | 22 | 70 | 88 |

### Öz Kristali ekonomisi

- **İsteğe bağlı ödeme seçenekleri (`oz_kristali_min`):** karşılaşılan 22221, yetersiz bakiyeyle kilitli 2519 (11.3%; geri dönüş eşiği <%50, DECISIONS #167).

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 4 | 10 | 16 |
| 60 | 7 | 29 | 71 |
| 120 | 50 | 141 | 203 |
| 180 | 183 | 309 | 373 |
| 270 | 362 | 682 | 809 |
| 365 | 511 | 986 | 1163 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 311; yere düşülen (yazılmış yenilgi ya da bayılma) 11.8%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 395.
- Hiç görülmeyen olay: 5 / 526: `atlama_meslek`, `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `hasat_vakti`. *(tamamı yapısal: `atlama_meslek` kapalı meslek sistemi, `bildirim_*` oturum dışı Android bildirimi, `hasat_vakti` Gün 365 yedek güvenlik ağı)*
- En nadir 10 olay: `kaya_soguk_firin` 4.6%, `thessar_kirik_muhur` 6.4%, `yozlasma_75` 7.5%, `yozlasma_50` 8.2%, `sb14_mert_salon` 8.6%, `bl15_us_beyaz_kapi` 8.7%, `p3_firtina_olcucu` 8.8%, `bl15_pk_kamp_aras` 8.9%, `bl15_tp_kamp_deniz` 12.7%, `sb14_elif_kayip_plan` 13.1%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `p4_siralama_zirvesi` 100.0%, `fraksiyon_son_pozisyon` 100.0%, `perde4_acilis` 100.0%, `bl13_kd_catilar` 100.0%, `bl13_kd_police` 100.0%, `bl12_kd_senet_donen` 100.0%, `bl12_kd_senet` 100.0%, `bl11_kule_dibi_can` 100.0%.
- Bitiş seviyesi: p10 585, p50 1128, p90 1678.
